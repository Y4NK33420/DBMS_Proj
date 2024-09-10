package edu.upenn.cis.db.datalog.rewriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.graphtrans.Config;

public class DatalogQueryRewriter_old {
	final static Logger logger = LogManager.getLogger(DatalogQueryRewriter_old.class);
	private static int predIdx = 0;
	private static DatalogProgram program; /* given program */
	private static DatalogProgram rewrittenProgram;
	private static int varIdx = 0;

	/**
	 * Select atoms to be rewritten as a subquery. 
	 * It pushes down interpreted atoms to the subquery, if any.
	 */
	private static Set<Atom> getAtomsInSubquery(Atom pickedAtom, Set<Atom> atoms) {
		Set<Atom> atomsInSubquery = new LinkedHashSet<Atom>();
		Set<String> boundVars = new LinkedHashSet<String>();
		boundVars.addAll(getVars(pickedAtom));

		while (true) {
			int boundVarsCount = boundVars.size();

			// 0: UDFs
			for (Atom a : atoms) {
				if (a.getRelName().startsWith(Config.relname_gennewid) == true) {
					for (int i = 0; i < a.getTerms().size()-1; i++) {
						boundVars.add(a.getTerms().get(i).getVar());
					}
				}
			}

			// 1. populate bound variables
			for (Atom a : atoms) {
				// e.g., E(a,c), c = 10
				if (a.isInterpreted() == true && a.getTerms().get(1).isConstant() == true) {
					String v = a.getTerms().get(0).getVar();
					if (boundVars.contains(v) == true) {
						atomsInSubquery.add(a); //.add(v);
					}
				} else if (program.getEDBs().contains(a.getRelName()) == true) {
					for (String v : getVars(a)) {
						if (boundVars.contains(v) == true) {
							boundVars.addAll(a.getVars());
							break;	
						}
					}
				}
			}

			// 2. populate interpreted atoms that contain any bound variables
			for (Atom a : atoms) {
//				if (a.isNegated() == true) continue;
				for (Term t : a.getTerms()) {
					if (t.isVariable() == true && boundVars.contains(t.getVar()) == true) {
						boundVars.addAll(a.getVars());
						atomsInSubquery.add(a);
						break;
					}
				}
			}

			// 3. populate vars in selected atoms
			Set<String> varsInSelectedAtoms = getVars(atomsInSubquery);
			for (Atom a : atoms) {
				if (a.isInterpreted() == false) continue;
				if (a.getPredicate().equals(Config.predOpEq) == true
						|| a.getPredicate().equals(Config.predOpGe) == true
						|| a.getPredicate().equals(Config.predOpGt) == true
						|| a.getPredicate().equals(Config.predOpLe) == true
						|| a.getPredicate().equals(Config.predOpLt) == true
						) {
					String v = a.getTerms().get(0).getVar();
					if (varsInSelectedAtoms.contains(v) == true) {
						atomsInSubquery.add(a);
					}
				}
			}
			if (boundVars.size() == boundVarsCount) {
				break;
			}
		}

		atomsInSubquery.add(pickedAtom);

		return atomsInSubquery;
	}

	private static Atom getNewHeadForSubqueryOfBoundAtoms(Set<String> headVars, Atom a, 
			Set<Atom> relatedAtoms, Set<Atom> rwBody) {
		/**
		 * Among vars in relatedAtoms, if in atom, headVars, or other atoms  
		 */

		Atom headAtom = null;
		try {
			ArrayList<Term> terms = new ArrayList<Term>();
			headAtom = (Atom)a.clone();

			Set<String> vars1 = getVars(relatedAtoms);
			Set<String> vars2 = new LinkedHashSet<String>();
			Set<String> varsInNonRelatedAtoms = new LinkedHashSet<String>();

			vars2.addAll(headVars);
			vars2.addAll(getVars(a));

			for (Atom b : rwBody) {
				if (b.equals(a) == true) continue;
				if (relatedAtoms.contains(b) == true) {
					vars2.addAll(getVars(b));
				}
			}

			for (Atom b : rwBody) {
				if (relatedAtoms.contains(b) == false) {
					varsInNonRelatedAtoms.addAll(b.getVars());
				}
			}

			for (String v : vars1) {
				if (v.contentEquals("_") == false) {
					terms.add(new Term(v, true));
				}
			}
			
			for (String v : headVars) {
				if (vars2.contains(v) == true) {
					terms.add(new Term(v, true));
				}
			}
				
//			for (String var : vars1) { 
//				if (vars2.contains(var) == true) {
//					if (var.contentEquals("_") == false) {
//						if (headVars.contains(var) == true || varsInNonRelatedAtoms.contains(var) == true) {
//							terms.add(new Term(var, true));
//						}
//					}
//				}
//			}
			if (terms.size() == 0) {
				throw new IllegalArgumentException("terms.size == 0");
			}

			headAtom.setTerms(terms);
			headAtom.setNegated(false);
			headAtom.getPredicate().setRelName(getNewPred() + "_NEG_SUBQUERY_" + a.getPredicate().getRelName());
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return headAtom;
	}

	private static String getNewPred() {
		return "R_" + (predIdx++);
	}

	public static String getNewVar() {
		return "_v" + (varIdx++); 
	}

	/**
	 * Take a datalog program and a query, and return a rewritten query with additional (intermediate) rules.
	 */
	private static HashSet<Atom> getRewrittenBody(HashSet<String> headVars, Set<Atom> body, String headName) {
		HashSet<Atom> rwBody = new LinkedHashSet<Atom>(body);
		HashSet<Atom> rwBodyIn = new LinkedHashSet<Atom>(body);
		
		handleAllPositiveIDBs(headVars, rwBody);
//		System.out.println("[getRewrittenBody] after handleAllPositiveIDBs\n\t\theadName: " + headName + " headVars: " + headVars + "\n\t\trwBodyIn: " + rwBodyIn + "\n\t\trwBodyOut: " + rwBody);
		
		if (rwBody.isEmpty() == false) {
			handleAllUDFs(headVars, rwBody);
//			System.out.println("[getRewrittenBody] after handleAllUDFs\n\t\theadVars: " + headVars + "\n\t\trwBodyIn: " + rwBodyIn + "\n\t\trwBodyOut: " + rwBody);

			//			handleAllRecursiveIDBs(headVars, rwBody);
			handleAllNegativeIDBs(headVars, rwBody);
//			System.out.println("[getRewrittenBody] after handleAllNegativeIDBs\n\t\theadVars: " + headVars + "\n\t\trwBodyIn: " + rwBodyIn + "\n\t\trwBodyOut: " + rwBody);


			integrityCheckBody(rwBody);
		}		
		
//		System.out.println("[getRewrittenBody] Finishing.\n\t\theadVars: " + headVars + "\n\t\trwBodyIn: " + rwBodyIn + "\n\t\trwBodyOut: " + rwBody);
		return rwBody;
	}

	private static boolean integrityCheckBody(HashSet<Atom> rwBody) {
		// integrity check
//		System.out.println("[integrityCheckBody] rwBody: " + rwBody);
		for (Atom a : rwBody) {
			if (a.getRelName().startsWith(Config.relname_gennewid + "_") == true) {
				HashSet<String> varsInUDFNotCovered = new LinkedHashSet<String>();
				varsInUDFNotCovered.addAll(a.getVars());
				varsInUDFNotCovered.remove(a.getTerms().get(a.getTerms().size()-1).getVar());
				for (Atom b : rwBody) {
					if (a == b) continue;
					if (b.isInterpreted() == true 
							&& (b.getRelName().equals("=") == true || b.getTerms().get(1).isConstant() == false)) {
						continue;
					}
					if (b.isNegated() == true) continue;
					varsInUDFNotCovered.removeAll(b.getVars());
				}
				if (varsInUDFNotCovered.isEmpty() == false) {
					throw new IllegalArgumentException("varsInUDFNotCovered: " + varsInUDFNotCovered);
				}
			}
		}

		HashMap<String, String> varToConstantMap = new HashMap<String, String>();
		for (Atom a : rwBody) {
			if (a.isInterpreted() == true && a.getRelName().equals("=") == true 
					&& a.getTerms().get(1).isConstant() == true) {
				String var = a.getTerms().get(0).getVar();
				String val = a.getTerms().get(1).toString();

				//				System.out.println("varToConstantMap: " + varToConstantMap + " a: " + a);

				if (varToConstantMap.containsKey(var) == true) {
					String valInMap = varToConstantMap.get(var);
					if (valInMap.equals(val) == false) {
						//	throw new IllegalArgumentException("Atom: a " + a + " varToConstantMap: " + varToConstantMap);
						return false;
					}
				}
				varToConstantMap.put(var,  val);
			}
		}
		
		normalizeAtomSet(rwBody);
		
		return true;

	}


//	private static void handleAllRecursiveIDBs(Set<String> headVars, Set<Atom> rwBody) {
//		// TODO Auto-generated method stub
//		Atom recursiveAtom = selectRecursiveIDBAtom(rwBody);
//		Set<Atom> toBeNewIDB = new LinkedHashSet<Atom>();
//
//		// Create a view V1 for bound IDBs and EDBs and predicates
//		for (Atom a : rwBody) {
//			if (a.isNegated() == true) continue;
//			if (program.getUDFs().contains(a.getPredicate().getRelName()) == true) continue;
//			if (a.getRelName().startsWith("REC_") == true) continue; // recursion
//			toBeNewIDB.add(a);
//		}
//		rwBody.removeAll(toBeNewIDB);
//
//		String newviewName = "NEWVIEW_" + (newviewid++);
//		DatalogClause c = new DatalogClause();
//		c.getBody().addAll(toBeNewIDB);
//		HashSet<String> vars = new LinkedHashSet<String>();
//
//		for (Atom a : toBeNewIDB) {
//			vars.addAll(a.getVars());
//		}
//		Atom newHead = new Atom(newviewName, vars);
//		c.setHead(newHead);
//
//		rwBody.add(newHead);
//
//		rewrittenProgram.addEDB(newviewName);
//		rewrittenProgram.addRule(c);
//
//		// create a new named recursion R' with newHead S and recursion R.
//		// unfolding R and put S to each body and return head R'
//	}

	private static ArrayList<Term> getTermsIncludeAllVars(Atom head, Set<Atom> atoms) {
		ArrayList<Term> terms = new ArrayList<Term>();

		Set<String> vars = new LinkedHashSet<String>();
		if (head != null) {
			for (Term t : head.getTerms()) {
				vars.add(t.toString());
			}
		}
		for (Atom a : atoms) {
			if (a.isNegated() == true) continue;
			for (Term t : a.getTerms()) {
				if (t.isVariable() == true) {
					vars.add(t.toString());
				}
			}
		}
		for (String v : vars) {
			if (v.contentEquals("_") == false) {
				terms.add(new Term(v, true));
			}
		}

		return terms;
	}

	private static ArrayList<Term> getTermsIncludeAllVars(Set<String> vars) {
		ArrayList<Term> terms = new ArrayList<Term>();
		for (String v : vars) {
			if (v.contentEquals("_") == false) {
				terms.add(new Term(v, true));
			} else {
				throw new IllegalArgumentException("variable is _.");
			}
		}
		if (terms.size() == 0) {
			throw new IllegalArgumentException("terms.size == 0");
		}
		return terms;
	}

	/**
	 * Return a set of variables in the atom.
	 * @param head
	 * @return
	 */
	private static HashSet<String> getVars(Atom head) {
		// TODO Auto-generated method stub
		HashSet<String> vars = new LinkedHashSet<String>();
		for (Term t : head.getTerms()) {
			if (t.isVariable() == true) {
				vars.add(t.toString());
			}
		}
		return vars;
	}

	private static HashSet<String> getVars(Set<Atom> atoms) {
		// TODO Auto-generated method stub
		HashSet<String> vars = new LinkedHashSet<String>();
		for (Atom a : atoms) {
			for (Term t : a.getTerms()) {
				if (t.isVariable() == true) {
					vars.add(t.toString());
				}
			}
		}
		return vars;
	}

	private static void handleAllUDFs(Set<String> headVars, Set<Atom> rwBody) {
		/*
	 	1. headVars <- E, U, Others
	 	2. Check if positive atoms (E) can determine UDF
	 	2. Create a rule: construct, newid, E' <- E (and insert)
	 	3. The head var of E' should be those in headVars and Others, and newid (replace E, U with E')
		 */

		HashSet<Atom> UDFs = selectUDFAtom(rwBody);
		HashSet<String> boundVars = new LinkedHashSet<String>();
		for (Atom b : rwBody) {
			if (program.getEDBs().contains(b.getRelName()) == true 
					|| rewrittenProgram.getEDBs().contains(b.getRelName()) == true) {
				boundVars.addAll(b.getVars());
			}
		}

//		System.out.println("[handleAllUDFs] UDFs to Handle: " + UDFs);
//		System.out.println("[handleAllUDFs] rwBody: " + rwBody);

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
//				System.out.println("\t[###### u is UDF] udf u: " + u + " a: " + a + " a.isNegated: " + a.isNegated() + " a==u?" + (a==u));

				if (a == u) continue;
				if (a.isNegated() == true) continue;
				if (program.getEDBs().contains(a.getRelName()) == false
						&& rewrittenProgram.getEDBs().contains(a.getRelName()) == false
						&& a.isInterpreted() == false) continue;

				//			System.out.println("\t[######] passed u: " + u + " a: " + a);

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

			Atom a3 = new Atom(getNewPred() + "_" + u.getRelName() + "_HANDLE");
			for (String v : varsInPosAtoms) {
				a3.appendTerm(new Term(v, true));
			}	
			c.addAtomToHeads(a1);
			c.addAtomToHeads(a2);
			c.addDesc("UDF_" + newVar);

//			System.out.println("rewrittenProgram.getEDBToGenIdMap(): " + rewrittenProgram.getEDBToGenIdMap());
		}

//		System.out.println("234234 posAtoms: " + posAtoms);
		for (Atom a : posAtoms) {
			if (UDFs.contains(a) == false) {
				c.addAtomToBody(a);
			}
		}

		if (c.getBody().size() > 0) {
			boolean willAddToProgram = true;
			for (Atom a : c.getBody()) {
				if (rewrittenProgram.getEDBToGenIdMap().containsKey(a.getRelName()) == true) {
					willAddToProgram = false;
				}
			}
	
			if (willAddToProgram == true) {
				rewrittenProgram.addRule(c);
			}
		}
	}



	private static HashSet<Atom> selectUDFAtom(Set<Atom> rwBody) {
		HashSet<Atom> selected = new HashSet<Atom>();
		for (Atom a : rwBody) {
			if (a.getRelName().startsWith(Config.relname_gennewid + "_") == true) {
				selected.add(a);
			}
		}		
		return selected;
	}

	private static void handleAllNegativeIDBs(Set<String> headVars, HashSet<Atom> rwBody) {
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

//			System.out.println("[handleAllNegativeIDBs] pickedNegAtom: " + a + " rwBody: " + rwBody + " headVars: " + headVars);

			// [C1] Select a set of related atoms of the negated atom and create a sub query.
			Set<Atom> relatedAtoms = selectRelatedAtoms(headVars, a, rwBody);
			Set<Atom> nonRelatedAtoms = new LinkedHashSet<Atom>(rwBody);
			nonRelatedAtoms.removeAll(relatedAtoms);
			nonRelatedAtoms.remove(a);
			
			Atom headAtom = getNewHeadForSubqueryOfBoundAtoms(headVars, a, relatedAtoms, rwBody); // A'

			Set<String> candidateHeadVars = new LinkedHashSet<String>();
			Set<String> varsInRelatedAtoms = getVars(relatedAtoms);
			Set<String> varsInNonRelatedAtoms = getVars(nonRelatedAtoms);


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
//			System.out.println("[handleAllNegativeIDBs:2] headAtom: " + headAtom + " relatedAtoms: " + relatedAtoms + " nonRelatedAtoms: " + nonRelatedAtoms);
//			System.out.println("[handleAllNegativeIDBs:3] varsInRelatedAtoms: " + varsInRelatedAtoms + " varsInNonRelatedAtoms: "
//					+ varsInNonRelatedAtoms + " candidateHeadVars: " + candidateHeadVars);


			headAtom.setTerms(getTermsIncludeAllVars(candidateHeadVars));
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
				posHeadAtom.getPredicate().setRelName(getNewPred() + "_NEG_POS_" + a.getPredicate().getRelName());
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
			HashSet<Atom> unfolding = getRewrittenBody(getVars(posAtoms), posAtoms, posHeadAtom.getRelName());
			if (unfolding.isEmpty() == true) { // B has nothing, so !B can be pruned.
				rwBody.add(headAtom);
				continue;
			}
			posHeadAtom.setTerms(getTermsIncludeAllVars(null, unfolding));

			rewrittenProgram.getEDBs().add(posHeadAtom.getRelName());
			DatalogClause c4 = new DatalogClause(posHeadAtom, unfolding);
			c4.addDesc("NEGHANDLE2_POS4NEG");
			rewrittenProgram.addRule(c4);

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
			negHeadAtom.getPredicate().setRelName(getNewPred() + "_NEG_HANDLED_" + a.getPredicate().getRelName());

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
			negHeadAtom.setTerms(getTermsIncludeAllVars(candidateHeadVars));

			DatalogClause negatedAtomHandlingRule = new DatalogClause(negHeadAtom, negAtoms);
			negatedAtomHandlingRule.addDesc("NEGHANDLE3_FINAL");
			rewrittenProgram.addRule(negatedAtomHandlingRule);

			if (Config.isSubQueryPruningEnabled() == true) {
				rewrittenProgram.runRule(negHeadAtom.getPredicate().getRelName());
				ret = rewrittenProgram.checkZeroPred(negHeadAtom.getPredicate().getRelName());
			}
			rwBody.add(negHeadAtom);
		}
		normalizeAtomSet(rwBody);
	}

	private static void unfoldSingleQuery(Atom pickedAtom, HashSet<Atom> rwBody, DatalogClause rule) {
		Set<Atom> unfolding = unfoldAtom(pickedAtom, rule);
		if (unfolding.isEmpty() == true) {
			rwBody.clear();
		} else {
			rwBody.remove(pickedAtom);
			addAtomsToAtomSet(unfolding, rwBody);
		}
		integrityCheckBody(rwBody);
	}

	private static void unfoldDisjunctiveQuery(HashSet<String> headVars, HashSet<Atom> rwBody, Atom pickedAtom, List<DatalogClause> rules) {
//		System.out.println("\t\t[unfoldDisjunctiveQuery:Before(rand: " + int_random + ")] pickedAtom: " + pickedAtom
//				+ "\n\t\t\trules: " + rules + "\n\t\t\trwBody: " + rwBody + " headVars: " + headVars);

		// 1. Set up the head with a new name
		Atom unfoldHeadAtom = null;
		try {
			unfoldHeadAtom = (Atom)pickedAtom.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		unfoldHeadAtom.setPredicate(new Predicate(getNewPred() + "_DISJUNCTIVE_UNFOLDED_" + pickedAtom.getRelName()));

		Set<Atom> atomsInSubquery = getAtomsInSubquery(pickedAtom, rwBody); // push down to subquery
		Set<Atom> nonRelatedAtoms = new LinkedHashSet<Atom>();

		nonRelatedAtoms.addAll(rwBody);
		nonRelatedAtoms.removeAll(atomsInSubquery);
		
		Set<String> candidateHeadVars = new LinkedHashSet<String>(); //getVars(pickedAtom);
		Set<String> varsInRelatedAtoms = getVars(atomsInSubquery);
		Set<String> varsInNonRelatedAtoms = getVars(nonRelatedAtoms);

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

		if (candidateHeadVars.isEmpty() == true) {
//			throw new IllegalArgumentException(
//					"candidateHeadVars is empty headVars: " + headVars);
			for (String v : varsInRelatedAtoms) { // FIXME: 
				candidateHeadVars.add(v);
				break;
			}
		}
		
		ArrayList<Term> terms = getTermsIncludeAllVars(candidateHeadVars);
		unfoldHeadAtom.setTerms(terms);
		
//		System.out.println("\n[unfoldDisjunctiveQuery] \t\tatomsToDeleteFromNonRelatedAtoms: " + atomsToDeleteFromNonRelatedAtoms);
//		System.out.println("[unfoldDisjunctiveQuery] \t\trwBody: " + rwBody);
//		System.out.println("[unfoldDisjunctiveQuery] \t\tcandidateHeadVars: " + candidateHeadVars);
//		System.out.println("[unfoldDisjunctiveQuery] \t\theadVars: " + headVars);
//		System.out.println("[unfoldDisjunctiveQuery] \t\tpickedAtom: " + pickedAtom);
//		System.out.println("[unfoldDisjunctiveQuery] unfoldHeadAtom: " + unfoldHeadAtom);
//		System.out.println("[unfoldDisjunctiveQuery] \t\tatomsInSubquery: " + atomsInSubquery);
//		System.out.println("[unfoldDisjunctiveQuery] \t\tnonRelatedAtoms: " + nonRelatedAtoms);
		
		
		// 2. With the unfolding head, create body per each rule.
		int numOfEmpty = 0;
		for (int i = 0; i < rules.size(); i++) {
//			System.out.println("\t[unfoldDisjunctiveQuery:(rand: " + int_random + ")] headVars: " + headVars + " pickedAtom: " + pickedAtom + " Rule[" + i + "] to apply: " + rules.get(i) + " rwBody: " + rwBody);
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

			reUnfolding = getRewrittenBody(unfoldHeadAtom.getVars(), unfolding, unfoldHeadAtom.getRelName());
			if (reUnfolding.isEmpty() == true) {
				numOfEmpty++;
				continue;
			}						
			program.getEDBs().add(unfoldHeadAtom.getPredicate().getRelName());
			DatalogClause c5 = new DatalogClause(unfoldHeadAtom, reUnfolding);
			c5.addDesc("UNFOLD_DISJUNCTIVE");
			rewrittenProgram.addRule(c5);
		}	

		// 3. if empty, return empty.
		if (numOfEmpty == rules.size()) {
			rwBody.clear();
		} else {
			rwBody.add(unfoldHeadAtom);
			rwBody.removeAll(atomsInSubquery);
	
			addAtomsToAtomSet(nonRelatedAtoms, rwBody);
		}
		integrityCheckBody(rwBody);
		
//		System.out.println("\t\t[unfoldDisjunctiveQuery:After(rand: " + int_random + ")] headVars: " + headVars + " pickedAtom: " + pickedAtom + " rwBody: " + rwBody);
	}
	
	private static void normalizeAtomSet(HashSet<Atom> body) {
		HashSet<Atom> atomToDelete = new LinkedHashSet<Atom>();
		for (Atom a : body) {
			if (a.getTerms().get(0).toString().equals("U") == true) {
				if (a.getTerms().get(1).toString().equals("U") == true) {
					throw new IllegalArgumentException("[ERROR] a: " + a);
				}
			}
			if (a.isInterpreted() == true && a.getRelName().equals("=") == true) {
				if (a.getTerms().get(0).isConstant() == true && a.getTerms().get(1).isConstant() == true) {
					if (a.getTerms().get(0).toString().equals(a.getTerms().get(1).toString()) == true) {
						atomToDelete.add(a);
					} else {
						body.clear();
						return;
					}
				}
			}
		}
		body.removeAll(atomToDelete);
				
	}
	
	private static boolean isSameAtom(Atom a, Atom b) {
		if (a.getTerms().size() != a.getTerms().size()) return false;
		if (a.isInterpreted() != b.isInterpreted()) return false;
		if (a.isNegated() != b.isNegated()) return false;
		if (a.getRelName().equals(b.getRelName()) == false) return false;
		
		for (int i = 0; i < a.getTerms().size(); i++) {
			Term t1 = a.getTerms().get(i);
			Term t2 = b.getTerms().get(i);
			
			if (t1.isConstant() != t2.isConstant()) return false;
			if (t1.isConstant() == true && t1.toString().equals(t2.toString()) == false) return false;
			if (t1.isConstant() == false && t1.getVar().equals(t2.getVar()) == false) return false;
		}
		return true;
	}
	
	private static void addAtomToAtomSet(Atom a, HashSet<Atom> atoms) {
		for (Atom b : atoms) {
			if (isSameAtom(a, b) == true) {
				return;
			}
		}
		atoms.add(a);
	}
	
	private static void addAtomsToAtomSet(Set<Atom> atomsIn, Set<Atom> atoms) {
		for (Atom a : atomsIn) {
			for (Atom b : atoms) {
				if (isSameAtom(a, b) == true) {
					return;
				}
			}
			atoms.add(a);
		}
	}
	

	/*
	 * headVars: the variables to be kept
	 * rwBody: the body of the rule being processed
	 */
	private static void handleAllPositiveIDBs(HashSet<String> headVars, HashSet<Atom> rwBody) {
		while(true) {
			Atom pickedAtom = selectPositiveIDBAtom(rwBody);
			if (pickedAtom == null) { // all atoms are unfolded
				break;
			}
//			System.out.println("[01-handleAllPositiveIDBs] pickedAtom: " + pickedAtom + " isNeg: " + pickedAtom.isNegated() + " from rwBody: " + rwBody);

			List<DatalogClause> rules = program.getRules(pickedAtom.getRelName());
			if (rules == null) {
				throw new IllegalArgumentException("[ERROR] No rule found for pred: " + pickedAtom.getRelName());
			}

			if (rules.size() == 1) { // not a disjunctive query
				unfoldSingleQuery(pickedAtom, rwBody, rules.get(0)); 
			} else { // create a disjunctive query
				unfoldDisjunctiveQuery(headVars, rwBody, pickedAtom, rules);
			}
		}
		if (integrityCheckBody(rwBody) == false) {
			rwBody.clear();
		}
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
			if (program.getEDBs().contains(a.getRelName()) == true) continue;
			if (program.getUDFs().contains(a.getRelName()) == true) continue;
			if (rewrittenProgram.getEDBs().contains(a.getRelName()) == true) continue;
			selectedAtom = a;
			break;
		}

		//FIXME: check
//		if (selectedAtom != null) {
//			Set<String> vars = getVars(selectedAtom);
//			boolean isValid = false;
//			if (selectedAtom != null) {
//				for (Atom a : atoms) {
//					if (a.isNegated() == true) continue;
//					for (Term t : a.getTerms()) {
//						if (t.isVariable() == true) {
//							if (vars.contains(t.toString()) == true) {
//								isValid = true;
//								break;
//							}
//						}
//					}
//					if (isValid == true) break;
//				}
//				if (isValid == false) {
//					throw new IllegalArgumentException("[ERROR] selectNegativeIDBAtom incorrectly.");				
//				}
//			}
//		}

		return selectedAtom;
	}

	private static Atom selectPositiveIDBAtom(Set<Atom> atoms) {
		Atom selectedAtom = null;

		for (Atom a : atoms) {
			if (a.isNegated() == true) continue;
			if (program.getEDBs().contains(a.getPredicate().getRelName()) == true) continue;
			if (program.getUDFs().contains(a.getPredicate().getRelName()) == true) continue;
			if (rewrittenProgram.getEDBs().contains(a.getPredicate().getRelName()) == true) continue;
			if (a.isInterpreted() == true) continue;
			if (program.getRules(a.getPredicate().getRelName()) == null) {
				throw new IllegalArgumentException("atom a: " + a + " has no rule.");
//				continue;
			}
			selectedAtom = a;
			break;
		}

//		if (selectedAtom == null) {
//			for (Atom a : atoms) {
//				if (a.isNegated() == true) continue;
//				if (program.getEDBs().contains(a.getPredicate().getRelName()) == true) continue;
//				if (program.getUDFs().contains(a.getPredicate().getRelName()) == true) continue;
//				if (a.isInterpreted() == true) continue;
//				if (program.getRules(a.getPredicate().getRelName()) == null) continue;
//				selectedAtom = a;
//			}
//		}
		logger.debug("[selectPositiveIDBAtom] selectedAtom: " + selectedAtom);

		return selectedAtom;
	}

	/**
	 * Return a set of related atoms.
	 */
	private static Set<Atom> selectRelatedAtoms(Set<String> headVars, Atom a, Set<Atom> rwBody) {
		Set<Atom> atoms = new LinkedHashSet<Atom>();
		Set<String> boundVars = getVars(a);
		boundVars.addAll(headVars);

		// found all (transitively) associated atoms from negated atom and headVars 
		int numOfAtoms = 0;
		while(true) {
			for (Atom ar : rwBody) {
				if (ar.isNegated() == true) continue;
				for (Term t : ar.getTerms()) {
					if (t.isVariable() == false) continue;
					if (boundVars.contains(t.toString()) == true) {
						boundVars.addAll(ar.getVars());
						atoms.add(ar);
						break;
					}
				}
			}
			// check if we found all atoms to include
			if (numOfAtoms == atoms.size()) {
				break;
			}
			numOfAtoms = atoms.size();
		}
		if (atoms.size() == 0) {
			throw new IllegalArgumentException("ERROR... EMPTY RELATED ATOMS atoms: " + atoms);
		}
		//		System.out.println("[END-selectRelatedAtoms] headVars: " + headVars + " a: " + a + " rwBody: " + rwBody + " atoms: " + atoms);
		return atoms;
	}

	/**
	 * An atom of a rule is unfolded by its rule and 
	 * the unfolding is inserted to the original rule 
	 *  
	 * @param a An atom in a rule _c_, to unfold 
	 * @param body A rule to unfold
	 * @return Unfolding
	 */
	private static Set<Atom> unfoldAtom(Atom a, DatalogClause rule) {
		/*
		 1. bound vars (in a)
		 2. unbound vars should be replaced unique vars
		 */
		HashSet<String> boundVars = a.getVars();
		HashSet<String> varsInBody = new HashSet<String>();
		HashMap<String, String> varInRuleToUnfoldingMap = new HashMap<String, String>();
		HashMap<String, HashSet<String>> varInIDBToVarsInHead = new HashMap<String, HashSet<String>>();

		Atom h = rule.getHead();
		for (int i = 0; i < h.getTerms().size(); i++) {
			Term t = h.getTerms().get(i);

			String var = a.getTerms().get(i).getVar();
			if (var.equals("_") == false) {
				varInRuleToUnfoldingMap.put(t.getVar(), var);
			}

			// To handle this case: 
			// 		atom: V(r1,r2), rule: V(a,a) <- R(a,a) => rewriting: V(r1,r2) <- R(r1,r1), r1=r2
			if (varInIDBToVarsInHead.containsKey(t.getVar()) == false) {
				varInIDBToVarsInHead.put(t.getVar(), new HashSet<String>());
			}
			varInIDBToVarsInHead.get(t.getVar()).add(var);
		}

		// assign new variable names for variables in the body that do not appear in the head
		for (Atom b : rule.getBody()) {
			varsInBody.addAll(b.getVars());
		}
		for (String var : varsInBody) {
			if (var.equals("_") == true) continue;
			if (varInRuleToUnfoldingMap.containsKey(var) == false) {
				varInRuleToUnfoldingMap.put(var, getNewVar());
			}
		}

		// unfold an atom with the rule
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
//			System.out.println("[unfoldAtom] b before: " + b);
			for (Term t : b.getTerms()) {
				if (t.isVariable() == false) continue;
				if (t.getVar().equals("_")) continue;
				if (varInRuleToUnfoldingMap.containsKey(t.getVar()) == true) {
					// TODO: this can be a problem with integer constant
					String newVar = varInRuleToUnfoldingMap.get(t.getVar());
					if (newVar.startsWith("\"") == true) {
						t.setConstant(varInRuleToUnfoldingMap.get(t.getVar()));											
					} else {
						t.setVar(varInRuleToUnfoldingMap.get(t.getVar()));											
					}
				} else {
					throw new IllegalArgumentException("No variable substitution is available.");
				}
			}

			for (String v : varInIDBToVarsInHead.keySet()) {
				if (varInIDBToVarsInHead.get(v).size() > 1) {
					String usedVar = varInRuleToUnfoldingMap.get(v);
					for (String v2 : varInIDBToVarsInHead.get(v)) {
						if (v2.endsWith(usedVar) == false) {
							equalityVarsSet.add(Pair.of(usedVar, v2));
						}
					}
				}
			}

			// handle "U" = "S"
			if (b.isInterpreted() == true && b.getRelName().equals("=") == true) {
//				System.out.println("code  398712 b: " + b + " const1: "
//						+ b.getTerms().get(0).isConstant() + " const2: " + b.getTerms().get(1).isConstant());
//				System.out.println("code  398712 base b: " + rule.getBody().get(i) + " const1: "
//						+ rule.getBody().get(i).getTerms().get(0).isConstant() + " const2: " + rule.getBody().get(i).getTerms().get(1).isConstant());
//				System.out.println("code 398712 varInRuleToUnfoldingMap: " + varInRuleToUnfoldingMap);
				
				if (b.getTerms().get(0).isConstant() == true && b.getTerms().get(1).isConstant() == true) {
					if (b.getTerms().get(0).toString().equals(b.getTerms().get(1).toString()) == false) {
						unfolding.clear();
						return unfolding;
					}
				}
			}
//			System.out.println("[unfoldAtom] b after: " + b);
			unfolding.add(b);
		}

		for (Pair<String, String> vars : equalityVarsSet) {
			Atom b2 = new Atom(Config.predOpEq);
			b2.appendTerm(new Term(vars.getLeft(), true));
			b2.appendTerm(new Term(vars.getRight(), true));
			unfolding.add(b2);
		}

//		System.out.println("\t\t[UNFOLDING] " + unfolding);

		if (integrityCheckBody(unfolding) == false) {
			unfolding.clear();
		} 
		
		normalizeAtomSet(unfolding);

		return unfolding;
	}

	private static String viewName = null;

	/**
	 * Get a datalog program of the rewritten queries (entry point)
	 */
	public static DatalogProgram getProgramForRewrittenQuery(DatalogProgram p, DatalogClause q, String _viewName) {		
		// 1. prepare 
		viewName = _viewName;

		if (q.getHeads().size() > 1) {
			throw new IllegalArgumentException("Query should have 1 head.");
		}
		program = p;
		rewrittenProgram = new DatalogProgram();

		HashSet<String> headVars = new LinkedHashSet<String>();
		for (Atom h : q.getHeads()) { //FIXME: multiple heads?
			headVars.addAll(getVars(h));
		}
		Set<Atom> body = new LinkedHashSet<Atom>();
		body.addAll(q.getBody());

		// 2. compute rewriting body
		HashSet<Atom> rwBody = getRewrittenBody(headVars, body, Config.relname_query);

		// 3. compute head
		if (rwBody.isEmpty() == false) {
			Atom newHead = new Atom(new Predicate(Config.relname_query));
			for (String v : headVars) {
				newHead.appendTerm(new Term(v, true));
			}
			DatalogClause c7 = new DatalogClause(newHead, rwBody);
			c7.addDesc("QUERY");
			rewrittenProgram.addRule(c7);			
		} else {
			throw new IllegalArgumentException("rwBody is empty");
		}

		return rewrittenProgram;
	}	
}
