package edu.upenn.cis.db.datalog.rewriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.graphtrans.Config;

public class Unfolder {

	public static void unfoldDisjunctiveQuery(HashSet<String> headVars, HashSet<Atom> rwBody, Atom pickedAtom, List<DatalogClause> rules) {
		// 1. Set up the head with a new name
		Atom unfoldHeadAtom = null;
		try {
			unfoldHeadAtom = (Atom)pickedAtom.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		unfoldHeadAtom.setPredicate(new Predicate(Helper.getNewPred() + "_DISJUNCTIVE_UNFOLDED_" + pickedAtom.getRelName()));

		Set<Atom> atomsInSubquery = Helper.getAtomsInSubquery(pickedAtom, rwBody); // push down to subquery
		Set<Atom> nonRelatedAtoms = new LinkedHashSet<Atom>();

		nonRelatedAtoms.addAll(rwBody);
		nonRelatedAtoms.removeAll(atomsInSubquery);
		
		Set<String> candidateHeadVars = new LinkedHashSet<String>(); //getVars(pickedAtom);
		Set<String> varsInRelatedAtoms = Helper.getVars(atomsInSubquery);
		Set<String> varsInNonRelatedAtoms = Helper.getVars(nonRelatedAtoms);

		// initialize with pickedAtom's vars - possibly redundant
		for (String v : pickedAtom.getVars()) {
			candidateHeadVars.add(v);
		}
		
		for (String v : headVars) {
			if (varsInRelatedAtoms.contains(v) == true) { // && v.equals("_") == false) {
				candidateHeadVars.add(v);
			}
		}

		for (String v : varsInRelatedAtoms) {
			if (varsInNonRelatedAtoms.contains(v) == true) { //&& v.equals("_") == false) {
				candidateHeadVars.add(v);
			}
		}

		HashSet<Atom> atomsToDeleteFromNonRelatedAtoms = new HashSet<Atom>();
		for (Atom b : nonRelatedAtoms) {
			if (b.isInterpreted() == true && b.getTerms().get(1).isConstant() == true) {
				if (b.getTerms().get(0).isVariable() == true) {
					String var = b.getTerms().get(0).getVar();
					if (varsInRelatedAtoms.contains(var) == false && headVars.contains(var) == false) {
						atomsToDeleteFromNonRelatedAtoms.add(b);						
					}
				}
			}
		}
		rwBody.removeAll(atomsToDeleteFromNonRelatedAtoms);
		nonRelatedAtoms.removeAll(atomsToDeleteFromNonRelatedAtoms);

		ArrayList<Term> terms = Helper.getTermsIncludeAllVars(candidateHeadVars);
		unfoldHeadAtom.setTerms(terms);
		
		// 2. With the unfolding head, create body per each rule.
		int numOfEmpty = 0;
		for (int i = 0; i < rules.size(); i++) {
			DatalogClause r = new DatalogClause();
			ArrayList<Atom> ar = rules.get(i).getHead().getAtomBodyStrWithInterpretedAtoms("");
			r.setHead(ar.get(0));
			for (Atom a : rules.get(i).getBody()) {
				r.addAtomToBody(a);
			}
			for (int j = 1; j < ar.size(); j++) {
				r.addAtomToBody(ar.get(j));
			}

			Set<Atom> unfolding = unfoldAtom(pickedAtom, r);

			if (unfolding.isEmpty() == true) {
				numOfEmpty++;
				continue;
//				throw new IllegalArgumentException("Unfolding is empty");
			}
			unfolding.addAll(atomsInSubquery);
			unfolding.remove(pickedAtom);
			Set<Atom> reUnfolding;

			reUnfolding = Rewriter.getRewrittenBody(unfoldHeadAtom.getVars(), unfolding);
			if (reUnfolding.isEmpty() == true) {
				numOfEmpty++;
				continue;
			}						
			Rewriter.getProgram().getEDBs().add(unfoldHeadAtom.getPredicate().getRelName());
			DatalogClause c5 = new DatalogClause(unfoldHeadAtom, reUnfolding);
			c5.addDesc("UNFOLD_DISJUNCTIVE");
			Rewriter.getRewrittenProgram().addRule(c5);
		}	

		// 3. if empty, return empty.
		if (numOfEmpty == rules.size()) {
			rwBody.clear();
		} else {
			rwBody.add(unfoldHeadAtom);
			rwBody.removeAll(atomsInSubquery);
	
			Helper.addAtomsToAtomSet(nonRelatedAtoms, rwBody);
		}
		Helper.integrityCheckBody(rwBody);
	}
	
	

	/**
	 * An atom of a rule is unfolded by its rule and 
	 * the unfolding is inserted to the original rule 
	 *  
	 * @param a An atom in a rule _c_, to unfold 
	 * @param body A rule to unfold
	 * @return Unfolding
	 */
	public static Set<Atom> unfoldAtom(Atom unfoldingHead, DatalogClause rule) {
		HashMap<String, HashSet<String>> varInRuleToUnfoldingMap = new HashMap<String, HashSet<String>>();

		// 1. prepare variable mapping
		Atom h = rule.getHead();
		for (int i = 0; i < h.getTerms().size(); i++) {
			Term t = h.getTerms().get(i);
			String varInUnfolding = unfoldingHead.getTerms().get(i).getVar();
			
			if (t.isConstant() == true) {
				throw new IllegalArgumentException("Rule head should not have constant");	
			}
			if (varInUnfolding.equals("_") == true) {
				throw new IllegalArgumentException("Unfolding head should not have _");
			}
			
			if (varInRuleToUnfoldingMap.containsKey(t.getVar()) == false) {
				varInRuleToUnfoldingMap.put(t.getVar(), new LinkedHashSet<String>());
			}
			varInRuleToUnfoldingMap.get(t.getVar()).add(varInUnfolding);
		}
		
		// 1a. assign new variable names for variables in the body that do not appear in the head
		for (String var : Helper.getVars(rule.getBody())) {
			if (var.equals("_") == true) continue;
			if (varInRuleToUnfoldingMap.containsKey(var) == false) {
				varInRuleToUnfoldingMap.put(var, new LinkedHashSet<String>());
				varInRuleToUnfoldingMap.get(var).add(Helper.getNewVar());
			}
		}

		// 2. unfold an atom with the rule
		HashSet<Atom> unfolding = new LinkedHashSet<Atom>();
		HashSet<Pair<String, String>> equalityVarsSet = new HashSet<Pair<String, String>>(); 

		for (int i = 0; i < rule.getBody().size(); i++) {
			Atom b = null;
			try {
				b = (Atom)(rule.getBody().get(i)).clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (Term t : b.getTerms()) {
				if (t.isConstant() == true) continue;
				if (t.getVar().equals("_")) continue;
				if (varInRuleToUnfoldingMap.containsKey(t.getVar()) == true) {
					 // substitute to the first variable if multiple exists
					int index = 0;
					String baseVar = null;
					for (String newVar : varInRuleToUnfoldingMap.get(t.getVar())) {
						if (index == 0) {
							t.setVar(newVar);
							baseVar = newVar;
						} else {
							equalityVarsSet.add(Pair.of(baseVar, newVar));							
						}
						index++;
					}
				} else {
					throw new IllegalArgumentException("No variable substitution is available.");
				}
			}

//			// handle "U" = "S"
//			if (b.isInterpreted() == true && b.getRelName().equals("=") == true) {
//				if (b.getTerms().get(0).isConstant() == true && b.getTerms().get(1).isConstant() == true) {
//					if (b.getTerms().get(0).toString().equals(b.getTerms().get(1).toString()) == false) {
//						unfolding.clear();
//					}
//				}
//			}
			unfolding.add(b);
		}
		
		if (unfolding.isEmpty() == false) {		
			for (Pair<String, String> vars : equalityVarsSet) {
				Atom b2 = new Atom(Config.predOpEq);
				b2.appendTerm(new Term(vars.getLeft(), true));
				b2.appendTerm(new Term(vars.getRight(), true));
				unfolding.add(b2);
			}
	
			if (Helper.integrityCheckBody(unfolding) == false) {
				unfolding.clear();
			} 
		}

		return unfolding;
	}
	
	public static void unfoldSingleQuery(Atom pickedAtom, HashSet<Atom> rwBody, DatalogClause rule) {
		Set<Atom> unfolding = unfoldAtom(pickedAtom, rule);
		if (unfolding.isEmpty() == true) {
			rwBody.clear();
		} else {
			rwBody.remove(pickedAtom);
			Helper.addAtomsToAtomSet(unfolding, rwBody);
		}
		Helper.integrityCheckBody(rwBody);
	}
}
