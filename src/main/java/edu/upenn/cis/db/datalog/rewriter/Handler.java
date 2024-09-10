package edu.upenn.cis.db.datalog.rewriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.graphtrans.Config;

public class Handler {
	public static void handleAllPositiveIDBs(HashSet<String> headVars, HashSet<Atom> rwBody) {
		while(true) {
			Atom pickedAtom = selectPositiveIDBAtom(rwBody);
			if (pickedAtom == null) { // all atoms are unfolded
				break;
			}

			List<DatalogClause> rules = Rewriter.getProgram().getRules(pickedAtom.getRelName());
			if (rules == null) {
				throw new IllegalArgumentException("[ERROR] No rule found for pred: " + pickedAtom.getRelName());
			}

			if (rules.size() == 1) { // not a disjunctive query
				Unfolder.unfoldSingleQuery(pickedAtom, rwBody, rules.get(0)); 
			} else { // create a disjunctive query
				Unfolder.unfoldDisjunctiveQuery(headVars, rwBody, pickedAtom, rules);
			}
		}
		if (Helper.integrityCheckBody(rwBody) == false) {
			rwBody.clear();
		}
	}
	
	public static void handleAllNegativeIDBs(Set<String> headVars, HashSet<Atom> rwBody) {
		/**
		 * A(a,b),!B(a,b),C(c,d),a=1 // given a body of atoms. 
		 * 		When we handle !B(a,b), A(a,b),a=1 are called determined atoms that constraint (a part of) B(a,b).
		 * [C1] Related R'(a,b)<-A(a,b),a=1 // generate a subquery from a set of determined atoms,
		 * [C2] Related B'(a,b)<-R'(a,b),B(a,b) // creating a subquery from constraint atom of the selected negated atom
		 * [C3] Rewritten to R'(a,b),!B'(a,b),C(c,d) // rewritten atoms
		 */
		while(true) {
			Atom a = selectNegativeIDBAtom(rwBody);
			if (a == null) {
				break;
			}

			// [C1] Select a set of related atoms of the negated atom and create a sub query.
			Set<Atom> relatedAtoms = Helper.selectRelatedAtoms(headVars, a, rwBody);
			Set<Atom> nonRelatedAtoms = new LinkedHashSet<Atom>(rwBody);
			nonRelatedAtoms.removeAll(relatedAtoms);
			nonRelatedAtoms.remove(a);
			
			Atom headAtom = Helper.getNewHeadForSubqueryOfBoundAtoms(headVars, a, relatedAtoms, rwBody); // A'

			Set<String> candidateHeadVars = new LinkedHashSet<String>();
			Set<String> varsInRelatedAtoms = Helper.getVars(relatedAtoms);
			Set<String> varsInNonRelatedAtoms = Helper.getVars(nonRelatedAtoms);


			for (String v : a.getVars()) {	// FIXME: possibly redundant 
				candidateHeadVars.add(v);
			}
			
			for (String v : headVars) {
				if (varsInRelatedAtoms.contains(v) == true) {
					candidateHeadVars.add(v);
				}
			}
			for (String v : varsInRelatedAtoms) {
				if (varsInNonRelatedAtoms.contains(v) == true) {
					candidateHeadVars.add(v);
				}
			}
			for (String v : a.getVars()) {
				if (varsInRelatedAtoms.contains(v) == true) {
					candidateHeadVars.add(v);
				}
			}

			DatalogProgram rewrittenProgram = Rewriter.getRewrittenProgram();
			headAtom.setTerms(Helper.getTermsIncludeAllVars(candidateHeadVars));
			rewrittenProgram.getEDBs().add(headAtom.getPredicate().getRelName());
			
			DatalogClause headSubQueryClause = new DatalogClause(headAtom, relatedAtoms);
			headSubQueryClause.addDesc("NEGHANDLE1_SUBQUERY");
			rewrittenProgram.addRule(headSubQueryClause);

			boolean ret = false; 
			if (Config.isSubQueryPruningEnabled() == true) {
				rewrittenProgram.runRule(headAtom.getPredicate().getRelName());
				ret = rewrittenProgram.checkZeroPred(headAtom.getPredicate().getRelName());
			}
			if (ret == true) { // positive atoms has 0-size
				rwBody.clear();
				return;
			}			
			rwBody.remove(a);
			rwBody.removeAll(relatedAtoms);

			// [C2] subquery with positive atom
			Atom posHeadAtom = null; 
			try {
				posHeadAtom = (Atom)a.clone();
				posHeadAtom.setNegated(false);
				posHeadAtom.getPredicate().setRelName(Helper.getNewPred() + "_NEG_POS_" + a.getPredicate().getRelName());
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Atom negatedAtomWithoutTheNegation = null; // B (of !B)
			try {
				negatedAtomWithoutTheNegation = (Atom)a.clone();
				negatedAtomWithoutTheNegation.setNegated(false);
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Set<Atom> posAtoms = new LinkedHashSet<Atom>();
			posAtoms.add(negatedAtomWithoutTheNegation);
			posAtoms.add(headAtom);

			//a.getVars()
			HashSet<Atom> unfolding = Rewriter.getRewrittenBody(Helper.getVars(posAtoms), posAtoms);
			if (unfolding.isEmpty() == true) { // B has nothing, so !B can be pruned.
				rwBody.add(headAtom);
				continue;
			}
			posHeadAtom.setTerms(Helper.getTermsIncludeAllVars(null, unfolding));

			rewrittenProgram.getEDBs().add(posHeadAtom.getRelName());
			rewrittenProgram.addRule(new DatalogClause(posHeadAtom, unfolding), "NEGHANDLE2_POS4NEG");

			ret = false;
			if (Config.isSubQueryPruningEnabled() == true) {
				rewrittenProgram.runRule(posHeadAtom.getPredicate().getRelName());
				ret = rewrittenProgram.checkZeroPred(posHeadAtom.getPredicate().getRelName());
			}

			// [C3] rewrite original query with negation
			Atom negHeadAtom = null; // B'
			try {
				negHeadAtom = (Atom)posHeadAtom.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			negHeadAtom.getPredicate().setRelName(Helper.getNewPred() + "_NEG_HANDLED_" + a.getPredicate().getRelName());

			Set<Atom> negAtoms = new LinkedHashSet<Atom>();
			negAtoms.add(headAtom); // !B'
			Atom negPosHeadAtom = null;
			try {
				negPosHeadAtom = (Atom)posHeadAtom.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			negPosHeadAtom.setNegated(true);
			negAtoms.add(negPosHeadAtom); // A'

			rewrittenProgram.getEDBs().add(negHeadAtom.getPredicate().getRelName());
			negHeadAtom.setTerms(Helper.getTermsIncludeAllVars(candidateHeadVars));

			rewrittenProgram.addRule(new DatalogClause(negHeadAtom, negAtoms), "NEGHANDLE3_FINAL");

			if (Config.isSubQueryPruningEnabled() == true) {
				rewrittenProgram.runRule(negHeadAtom.getPredicate().getRelName());
				ret = rewrittenProgram.checkZeroPred(negHeadAtom.getPredicate().getRelName());
			}
			rwBody.add(negHeadAtom);
		}
		Helper.normalizeAtomSet(rwBody);
	}

	public static void handleAllUDFs(Set<String> headVars, Set<Atom> rwBody) {
		/*
	 	1. headVars <- E, U, Others
	 	2. Check if positive atoms (E) can determine UDF
	 	2. Create a rule: construct, newid, E' <- E (and insert)
	 	3. The head var of E' should be those in headVars and Others, and newid (replace E, U with E')
		 */

		HashSet<Atom> UDFs = selectUDFAtoms(rwBody);
		HashSet<String> boundVars = new LinkedHashSet<String>();
		for (Atom b : rwBody) {
			if (Rewriter.getProgram().getEDBs().contains(b.getRelName()) == true 
					|| Rewriter.getRewrittenProgram().getEDBs().contains(b.getRelName()) == true) {
				boundVars.addAll(b.getVars());
			}
		}

		DatalogClause c = new DatalogClause();

		HashSet<String> processedUDFvars = new LinkedHashSet<String>();
		
		Set<Atom> posAtoms = new LinkedHashSet<Atom>();
		
		for (Atom u : UDFs) {
			Set<String> varsInPosAtoms = new LinkedHashSet<String>();
			ArrayList<String> varsInUDFAtom = new ArrayList<String>();
			for (String v : u.getVars()) {
				varsInUDFAtom.add(v);
			}

			for (Atom a : rwBody) {
				if (a == u) continue;
				if (a.isNegated() == true) continue;
				if (Rewriter.getProgram().getEDBs().contains(a.getRelName()) == false
						&& Rewriter.getRewrittenProgram().getEDBs().contains(a.getRelName()) == false
						&& a.isInterpreted() == false) continue;

				varsInPosAtoms.addAll(a.getVars());
				posAtoms.add(a);
			}

			String generatedId = u.getTerms().get(u.getTerms().size()-1).getVar();

			if (processedUDFvars.contains(generatedId) == true) {
				// the gennewid variable is already processed
				continue;
			}
			processedUDFvars.add(generatedId);

			Atom a1 = null;
			try {
				a1 = (Atom)u.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String constStr = u.getRelName();
			constStr = constStr.replace("_MAP_", "_CONST_");
			a1.getPredicate().setRelName(constStr);

			String newVar = varsInUDFAtom.get(varsInUDFAtom.size()-1) + "_newobj";
			a1.getTerms().get(a1.getTerms().size()-1).setVar(newVar);

			String constVarStr = u.getRelName().replace("_MAP_", "_");
			int posLastUnderbar = constVarStr.lastIndexOf("_");
			constVarStr = constVarStr.substring(0, posLastUnderbar);
			Atom a2 = new Atom(constVarStr); //Config.relname_gennewid + "_v0");
			a2.appendTerm(new Term(newVar, true));

			Atom a3 = new Atom(Helper.getNewPred() + "_" + u.getRelName() + "_HANDLE");
			for (String v : varsInPosAtoms) {
				a3.appendTerm(new Term(v, true));
			}	
			c.addAtomToHeads(a1);
			c.addAtomToHeads(a2);
			c.addDesc("UDF_" + newVar);
		}

		for (Atom a : posAtoms) {
			if (UDFs.contains(a) == false) {
				c.addAtomToBody(a);
			}
		}

		if (c.getBody().size() > 0) {
			boolean willAddToProgram = true;
			for (Atom a : c.getBody()) {
				if (Rewriter.getRewrittenProgram().getEDBToGenIdMap().containsKey(a.getRelName()) == true) {
					willAddToProgram = false;
				}
			}
	
			if (willAddToProgram == true) {
				Rewriter.getRewrittenProgram().addRule(c);
			}
		}
	}

	private static Atom selectPositiveIDBAtom(Set<Atom> atoms) {
		Atom selectedAtom = null;
	
		for (Atom a : atoms) {
			if (a.isNegated() == true) continue;
			if (Rewriter.getProgram().getEDBs().contains(a.getPredicate().getRelName()) == true) continue;
			if (Rewriter.getProgram().getUDFs().contains(a.getPredicate().getRelName()) == true) continue;
			if (Rewriter.getRewrittenProgram().getEDBs().contains(a.getPredicate().getRelName()) == true) continue;
			if (a.isInterpreted() == true) continue;
			if (Rewriter.getProgram().getRules(a.getPredicate().getRelName()) == null) {
				throw new IllegalArgumentException("atom a: " + a + " has no rule.");
			}
			selectedAtom = a;
			break;
		}
		return selectedAtom;
	}

	/**
	 * Select a negated IDB atom to process. 
	 * A candidate atom is a negated atom that is neither materialized nor interpreted atom.
	 * This method is called only when there is no positive atom that can be unfolded. 
	 * There exists at least one variable that appears in a positive atom.   
	 */
	private static Atom selectNegativeIDBAtom(Set<Atom> atoms) {
		Atom selectedAtom = null;
		
		for (Atom a : atoms) {
			if (a.isNegated() == false) continue;
			if (a.isInterpreted() == true) continue;
			if (Rewriter.getProgram().getEDBs().contains(a.getRelName()) == true) continue;
			if (Rewriter.getProgram().getUDFs().contains(a.getRelName()) == true) continue;
			if (Rewriter.getRewrittenProgram().getEDBs().contains(a.getRelName()) == true) continue;
			selectedAtom = a;
			break;
		}
		return selectedAtom;
	}

	private static HashSet<Atom> selectUDFAtoms(Set<Atom> rwBody) {
		HashSet<Atom> selected = new HashSet<Atom>();
		for (Atom a : rwBody) {
			if (a.getRelName().startsWith(Config.relname_gennewid + "_") == true) {
				selected.add(a);
			}
		}		
		return selected;
	}
}
