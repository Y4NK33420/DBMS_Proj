package edu.upenn.cis.db.datalog.rewriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.graphtrans.Config;

public class Helper {
	private static int predIdx = 0;
	private static int varIdx = 0;

	public static String getNewPred() {
		return "R_" + (predIdx++);
	}

	public static String getNewVar() {
		return "_v" + (varIdx++); 
	}

	public static HashSet<String> getVars(Set<Atom> atoms) {
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
	
	public static HashSet<String> getVars(ArrayList<Atom> atoms) {
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

	public static void addAtomToAtomSet(Atom a, HashSet<Atom> atoms) {
		for (Atom b : atoms) {
			if (isSameAtom(a, b) == true) {
				return;
			}
		}
		atoms.add(a);
	}	

	public static void addAtomsToAtomSet(Set<Atom> atomsIn, Set<Atom> atoms) {
		for (Atom a : atomsIn) {
			for (Atom b : atoms) {
				if (isSameAtom(a, b) == true) {
					return;
				}
			}
			atoms.add(a);
		}
	}
	
	public static boolean isSameAtom(Atom a, Atom b) {
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

	public static ArrayList<Term> getTermsIncludeAllVars(Atom head, Set<Atom> atoms) {
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

	public static ArrayList<Term> getTermsIncludeAllVars(Set<String> vars) {
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
	
	public static void normalizeAtomSet(HashSet<Atom> body) {
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
	
	public static boolean integrityCheckBody(HashSet<Atom> rwBody) {
		// integrity check
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

	
	/**
	 * Select atoms to be rewritten as a subquery. 
	 * It pushes down interpreted atoms to the subquery, if any.
	 */
	public static Set<Atom> getAtomsInSubquery(Atom pickedAtom, Set<Atom> atoms) {
		Set<Atom> atomsInSubquery = new LinkedHashSet<Atom>();
		Set<String> boundVars = new LinkedHashSet<String>();
		boundVars.addAll(pickedAtom.getVars());

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
				} else if (Rewriter.getProgram().getEDBs().contains(a.getRelName()) == true) {
					for (String v : a.getVars()) {
						if (boundVars.contains(v) == true) {
							boundVars.addAll(a.getVars());
							break;	
						}
					}
				}
			}

			// 2. populate interpreted atoms that contain any bound variables
			for (Atom a : atoms) {
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
	
	public static Atom getNewHeadForSubqueryOfBoundAtoms(Set<String> headVars, Atom a, 
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
			vars2.addAll(a.getVars());

			for (Atom b : rwBody) {
				if (b.equals(a) == true) continue;
				if (relatedAtoms.contains(b) == true) {
					vars2.addAll(b.getVars());
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
	
	/**
	 * Return a set of related atoms.
	 */
	public static Set<Atom> selectRelatedAtoms(Set<String> headVars, Atom a, Set<Atom> rwBody) {
		Set<Atom> atoms = new LinkedHashSet<Atom>();
		Set<String> boundVars = a.getVars();
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
		return atoms;
	}
	

	
	
}
