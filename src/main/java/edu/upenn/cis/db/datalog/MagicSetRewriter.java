package edu.upenn.cis.db.datalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.util.QueryBuilder.TermAndBoost;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.helper.Util;

public class MagicSetRewriter {
	private static DatalogProgram rewrittenProgram;
	private static DatalogProgram program;
	
	private static int indexOfadornedWithInterpretedAtoms = 0;
	private static int supplementRelationIndex = 0;

	private static HashMap<Atom, ArrayList<Boolean>> adornedRelsMap = new HashMap<Atom, ArrayList<Boolean>>();
	private static HashMap<Atom, HashSet<Atom>> adornedRelToInterpretedAtomsMap = new HashMap<Atom, HashSet<Atom>>();
	private static HashMap<Atom, Atom> adornedAtomToBaseAtomMap = new HashMap<Atom, Atom>();
	
	private static ArrayList<Boolean> getAdornment(Atom atom, HashSet<String> vars) {
		ArrayList<Boolean> adornment = new ArrayList<Boolean>();
		
		for (Term t : atom.getTerms()) {
			String var = t.getVar();
			adornment.add(vars.contains(var));
		}		return adornment;
	}
	
	private static String getAdornedRelName(String name, ArrayList<Boolean> adornment) {
		String relName = name + "_";
		for (Boolean b : adornment) {
			relName += (b == true) ? "b" : "f";
		}
		return relName;
	}
	
	private static Atom getAdornedRel(Atom atom, ArrayList<Boolean> adornment) {
		Atom newAtom = null;
		try {
			newAtom = (Atom)atom.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String relName = getAdornedRelName(atom.getRelName(), adornment);
		newAtom.setBaseName(atom.getRelName());
		newAtom.getPredicate().setRelName(relName);
		newAtom.getAdornment().addAll(adornment);
		
		return newAtom;
	}
	
	private static HashSet<Atom> constructInputRelation(HashSet<String> boundVars, ArrayList<Atom> body) {
		// 0. Constraint Normalization
		HashSet<Atom> interpretedAtoms = new HashSet<Atom>();
		for (Atom a : body) {
			if (a.isInterpreted() == true) {
				if (a.getPredicate().getRelName().equals("=") == true) {
					boundVars.add(a.getTerms().get(0).getVar());
				}
				interpretedAtoms.add(a);
			}		
		}
//		System.out.println("[constructInputRelation] boundVars: " + boundVars + " interpretedAtoms: " + interpretedAtoms);
		return interpretedAtoms;
	}

	private static HashSet<Atom> getRelatedInterpretedAtoms(Atom a, HashSet<Atom> interpretedAtoms, HashSet<String> boundVars, boolean equalityOnly) {
		HashSet<Atom> relatedInterpretedAtoms = new HashSet<Atom>();
		
		for (Atom b : interpretedAtoms) {
			// TODO: equality only?
			HashSet<String> vars = new HashSet<String>();
			vars.addAll(b.getVars());
			vars.retainAll(a.getVars());
			
			if (b.getVars().size() == vars.size()) {
				relatedInterpretedAtoms.add(b);
			}
		}
//		System.out.println("[getRelatedInterpretedAtoms] a: " + a + " relatedInterpretedAtoms: " + relatedInterpretedAtoms);
		return relatedInterpretedAtoms;
	}
	
	/*
	 * This function returns the atom that be on the next in ordered atom
	 */
	private static Atom pickCandidateAtom(Atom initAtom, HashSet<String> boundVars, ArrayList<Atom> atomsToBeOrdered, HashSet<Atom> interpretedAtoms) {
//		System.out.println("[pickCandidateAtom] boundVars: " + boundVars + " atomsToBeOrdered: " + atomsToBeOrdered + " interpretedAtoms: " + interpretedAtoms);
		Atom candidateAtom = null;
		
		int numberOfBoundVars = 0;

		// Priority 1 - EDB with bound vars
		int reasonForPicked = -1;
		for (int i = 0; i < 6; i++) {
			int maxOfBoundVarsSofar = 0; // pick atom with the most bound vars
			for (Atom a : atomsToBeOrdered) {
				System.out.println("[pickCandidateAtom} Check atom a: " + a + " programEDB: " + program.getEDBs().contains(a.getRelName()) + " negated?: " + a.isNegated() + " i: " + i);
				boolean isAtomToBeChecked = false;
				if (a.getRelName().startsWith(Config.relname_gennewid + "_") == true ) {
					continue;
				}
				
				if (i == 0) {
					if (a.isInterpreted() == true) {
						isAtomToBeChecked = true;
					}
				} else if (i == 1) { // EDB with = 
					if (program.getEDBs().contains(a.getRelName()) == true && a.isNegated() == false) {
						isAtomToBeChecked = true;
					}
				} else if (i == 2) { // EDB with > (comparison)
//					if (program.getEDBs().contains(a.getRelName()) == true && a.isNegated() == false) {
//						isAtomToBeChecked = true;
//					}
				} else if (i == 3) { // IDB with =  
					if (program.getEDBs().contains(a.getRelName()) == false && a.isNegated() == false) {
						isAtomToBeChecked = true;
					}					
				} else if (i == 4) { // IDB with >
//					if (program.getEDBs().contains(a.getRelName()) == false && a.isNegated() == false) {
//						isAtomToBeChecked = true;
//					}					
				} else if (i == 5) { // EDB with negation
					if (program.getEDBs().contains(a.getRelName()) == true && a.isNegated() == true) {
						isAtomToBeChecked = true;
					}
				} else if (i == 6) { // IDB with negation
					if (program.getEDBs().contains(a.getRelName()) == false && a.isNegated() == true) {
						isAtomToBeChecked = true;
					}
				}
				
				HashSet<String> boundVarsToCheck = new HashSet<String>();
				boundVarsToCheck.addAll(boundVars);
				if (initAtom != null) { 
					boundVarsToCheck.addAll(initAtom.getVars());
				}
				if (isAtomToBeChecked == true) {
					HashSet<Atom> relatedInterpretedAtoms = getRelatedInterpretedAtoms(a, interpretedAtoms, boundVars, false);
					
					for (Atom b : relatedInterpretedAtoms) {
						boundVarsToCheck.addAll(b.getVars());
					}

					HashSet<String> boundVarsInAtom = new HashSet<String>();
					boundVarsInAtom.addAll(a.getVars());
					boundVarsInAtom.retainAll(boundVarsToCheck);
					
					if (boundVarsInAtom.size() > maxOfBoundVarsSofar) {
						maxOfBoundVarsSofar = boundVarsInAtom.size();
						candidateAtom = a;
						reasonForPicked= i;
					}
				}
			}
			if (candidateAtom != null) {
				break;
			}
		}
		
		if (candidateAtom == null) { // pick non genid
			for (Atom a : atomsToBeOrdered) {
				if (a.getRelName().startsWith(Config.relname_gennewid + "_") == false) {
					candidateAtom = a;
					reasonForPicked = 1024;
					break;
				}
			}
		}

		if (candidateAtom == null) {
			for (Atom a : atomsToBeOrdered) {
				if (a.getRelName().startsWith(Config.relname_gennewid + "_") == true ) {
					candidateAtom = a;
					reasonForPicked =  1023;
					break;
				}
			}
		}
		
		System.out.println("[pickCandidateAtom:42342] Found a candidate atom: " + candidateAtom + " reasonForPicked: " + reasonForPicked);
		
		return candidateAtom;
	}
	
	private static ArrayList<Atom> orderBody(Atom head, Atom initAtom, ArrayList<Atom> body, 
			HashSet<String> boundVars, HashSet<Atom> interpretedAtoms, HashMap<Atom, HashSet<Atom>> atomToInterpretedAtomsMap) {
		ArrayList<Atom> orderedBody = new ArrayList<Atom>();

		ArrayList<Atom> atomsToBeOrdered = new ArrayList<Atom>();
		HashSet<Atom> interpretedAtomsToBeChecked = new HashSet<Atom>();
		
		for (Atom a : body) {
			if (a.isInterpreted() == false) {
				atomsToBeOrdered.add(a);
			}
		}
		HashSet<String> boundVarsToCheck = new HashSet<String>();
		boundVarsToCheck.addAll(boundVars);
		if (initAtom != null) {
			boundVarsToCheck.addAll(initAtom.getVars());
		}

		while(atomsToBeOrdered.size() > 0) {
			Atom candidateAtom = pickCandidateAtom(initAtom, boundVarsToCheck, atomsToBeOrdered, interpretedAtoms);
			boundVarsToCheck.addAll(candidateAtom.getVars());
			orderedBody.add(candidateAtom);
			atomsToBeOrdered.remove(candidateAtom);
			HashSet<Atom> relatedInterpretedAtoms = getRelatedInterpretedAtoms(candidateAtom, interpretedAtoms, boundVarsToCheck, false);
			interpretedAtoms.removeAll(relatedInterpretedAtoms);
			
			atomToInterpretedAtomsMap.put(candidateAtom, relatedInterpretedAtoms);
		}
		
		// integrity check
		if (atomsToBeOrdered.size() > 0) {
			throw new IllegalArgumentException("atomsToBeOrdered should be empty [" + atomsToBeOrdered + "]");
		}
		
		// remaining interpreted atoms
		if (interpretedAtoms.size() > 0) {
			for (Atom a : interpretedAtoms) {
				orderedBody.add(a);
			}
//			throw new IllegalArgumentException("interpretedAtoms should be empty [" + interpretedAtoms + "]");
		}
		System.out.println("[orderBody:32423] head: " + head + " body: " + body);
		System.out.println("[orderBody:32423] orderedBody: " + orderedBody + " atomToInterpretedAtomsMap: " + atomToInterpretedAtomsMap);		
		return orderedBody;
	}
	
	private static ArrayList<Atom> adornBody(Atom initAtom, ArrayList<Atom> orderedBody, HashSet<String> boundVars,
			HashMap<Atom, HashSet<Atom>> atomToInterpretedAtomsMap) {
		ArrayList<Atom> adornedBody = new ArrayList<Atom>();	
		System.out.println("Adding boundVars [" + boundVars + "] with initAtom: " + initAtom + " orderedBody: " + orderedBody);
		if (initAtom != null) {
			boundVars.addAll(initAtom.getVars());
		}

		for (Atom a : orderedBody) {
//			System.out.println("Adorning orderedBody a [" + a + "] with initAtom: " + initAtom);
			if (a.getRelName().startsWith(Config.relname_gennewid + "_") == true) {
				adornedBody.add(a);
				continue;
			}			
			
//			if (a.isInterpreted() == true) {
//				throw new IllegalArgumentException("orderedBody should not have intepreted atom a: " + a);
//			}
			Atom newAtom = null;
			try {
				newAtom = (Atom)a.clone();;
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String relName = newAtom.getRelName();
			Atom atomToAdd = null;
			
			if (program.getEDBs().contains(relName) == true ||
				rewrittenProgram.getEDBs().contains(relName) == true || 
//				a.isInterpreted() == true && 
				newAtom.isInterpreted() == true ) {
				atomToAdd = newAtom;
				
				boundVars.addAll(atomToAdd.getVars());

				if (atomToInterpretedAtomsMap.containsKey(a) == true) {
					adornedBody.addAll(atomToInterpretedAtomsMap.get(a));
				}
				adornedBody.add(atomToAdd);
				boundVars.addAll(atomToAdd.getVars());
			} else { // IDB
				// In: boundVars, IDB Out: adorned boolean[]
				// TODO: if it has related interpreted atoms. it should create a non-general adorned rels.

				ArrayList<Boolean> adornment = getAdornment(newAtom, boundVars);

				if (atomToInterpretedAtomsMap.get(a).size() == 0) {
					System.out.println("[code 3423] a: " + a + " has no related interpreted atoms.");
				} else {
					HashSet<Atom> relatedInterpretedAtoms = atomToInterpretedAtomsMap.get(a);
					
					// handle constant interpreted atom (b=3) 
					for (int i = 0; i < newAtom.getTerms().size(); i++) {
						Term t = newAtom.getTerms().get(i);
						String v = t.getVar();
						for (Atom b : relatedInterpretedAtoms) {
							if (b.getRelName().equals("=") == true && b.getTerms().get(0).getVar().equals(v)
									&& b.getTerms().get(1).isConstant() == true) {
								adornment.set(i, true);
							}
						}
					}						
	
				}
//				System.out.println("EDB213: " + rewrittenProgram.getEDBs());
				Atom adornedAtom = getAdornedRel(newAtom, adornment);
				adornedAtomToBaseAtomMap.put(adornedAtom, a);
//				System.out.println("EDB214: " + rewrittenProgram.getEDBs());
				System.out.println("[code 2131] adornedAtom: " + adornedAtom + " newAtom: " + newAtom);
				System.out.println("[code 3423] a: " + a + " has related interpreted atoms. atomToInterpretedAtomsMap.get(a): " + atomToInterpretedAtomsMap.get(a));
				adornedAtom.getPredicate().setRelName(adornedAtom.getRelName());
//				System.out.println("EDB215: " + rewrittenProgram.getEDBs());
				adornedRelToInterpretedAtomsMap.put(adornedAtom, atomToInterpretedAtomsMap.get(a));
//				System.out.println("EDB216: " + rewrittenProgram.getEDBs());
				atomToAdd = adornedAtom;
//				rewrittenProgram.getEDBs().add(atomToAdd.getRelName());
								
				adornedBody.add(atomToAdd);
				boundVars.addAll(atomToAdd.getVars());
			}
		}
		System.out.println("[adornBody:324234] adornedBody: " + adornedBody);	
		
		return adornedBody;
	}
	
	private static void decomposeBody(Atom head, Atom initAtom, ArrayList<Atom> adornedBody, 
			HashMap<Atom, HashSet<Atom>> atomToInterpretedAtomsMap, boolean isBaseQuery) {

		Util.console_logln("[ruleDecomposition] head: " + head + " initAtom: " + initAtom + " adornedBody: " + adornedBody 
				+ " atomToInterpretedAtomsMap: " + atomToInterpretedAtomsMap + " isBaseQuery: " + isBaseQuery, 1);
		
//		System.out.println("rewritten: " + rewrittenProgram);

		ArrayList<DatalogClause> cs = new ArrayList<DatalogClause>();	
		HashMap<String, Pair<Atom, HashSet<Atom>>> adornedAtomMap = new HashMap<String, Pair<Atom, HashSet<Atom>>>();
		
		HashSet<Atom> supplementRelationBody = new LinkedHashSet<Atom>();

		
		HashSet<String> boundVars = new LinkedHashSet<String>(); 
		if (initAtom != null) {
			adornedBody.add(0, initAtom);
		}
		Atom decomposedRelationHead = null;
		for (int i = 0; i < adornedBody.size(); i++) {
			Atom a = adornedBody.get(i);
			System.out.println("Atom a: " + a + " isIDBInEDB: " + rewrittenProgram.getEDBs().contains(a.getRelName()) + " rewrittenProgram.getEDBs(): " + rewrittenProgram.getEDBs());
			if (program.getEDBs().contains(a.getRelName()) == true
					|| rewrittenProgram.getEDBs().contains(a.getRelName()) == true) {
				supplementRelationBody.add(a);
				if (atomToInterpretedAtomsMap.containsKey(a) == true) {
					supplementRelationBody.addAll(atomToInterpretedAtomsMap.get(a));
				}
				boundVars.addAll(a.getVars());
			} else if (a.isInterpreted() == true || a.getRelName().startsWith("InSup_") == true) {
				supplementRelationBody.add(a);
				boundVars.addAll(a.getVars());
			} else if (a.getRelName().startsWith(Config.relname_gennewid + "_")) { 
				System.out.println("[PROCESS ] GEN[" + a + " => add two rules with supplementRelationBody: " + supplementRelationBody + " index i: " + i + " of size: " + adornedBody.size());
				DatalogClause c_genid_map = new DatalogClause();
				c_genid_map.addDesc("GEN1-map");
				DatalogClause c_genid_const = new DatalogClause();
				c_genid_const.addDesc("GEN2-const");
//				c1.getBody().add(initAtom);
//				c1.getBody().addAll(adornedBody);
//				c1.getBody().remove(a);
				Atom gen_map = new Atom(Config.relname_gennewid + "_MAP");
				Atom gen_const = new Atom(Config.relname_gennewid + "_CONST");
				for (Term t : a.getTerms()) {
					gen_map.appendTerm(new Term(t.getVar(), true));
					gen_const.appendTerm(new Term(t.getVar(), true));
				}
				HashSet<String> vars = new HashSet<String>();
				vars.addAll(a.getVars());
				for (Atom b : supplementRelationBody) {
					vars.removeAll(b.getVars());
				}
				if (vars.size() != 1) {
					throw new IllegalArgumentException("vars size should be 1 vars: " + vars);
				}
				
				if (i + 1 < adornedBody.size()) {
					decomposedRelationHead = new Atom("DECOM_" + supplementRelationIndex); // + a.getRelName() + "_"
					supplementRelationIndex++;
	
					System.out.println("[HERE:32423] decomposedRelationHead: " + decomposedRelationHead);

					c_genid_map.addAtomToHeads(decomposedRelationHead);
					
					HashSet<String> varsToKeepInDecomposeRelationHead = new LinkedHashSet<String>();		
					HashSet<String> varsDecomposedRelationHead = new LinkedHashSet<String>();
					varsToKeepInDecomposeRelationHead.addAll(head.getVars());

					for (int j = i + 1; j < adornedBody.size(); j++) {
						Atom b = adornedBody.get(j);
						varsToKeepInDecomposeRelationHead.addAll(b.getVars());
						
						if (b.isInterpreted() == true) {
							supplementRelationBody.add(b);
						}
					}
					varsDecomposedRelationHead.addAll(boundVars);
					varsDecomposedRelationHead.addAll(a.getVars());
					varsDecomposedRelationHead.retainAll(varsToKeepInDecomposeRelationHead);
					
					for (String v : varsToKeepInDecomposeRelationHead) {
						decomposedRelationHead.appendTerm(new Term(v, true));
					}
				} else {
					c_genid_map.addAtomToHeads(head);
				}
				for (Atom b : supplementRelationBody) {
					c_genid_map.addAtomToBody(b);
					c_genid_const.addAtomToBody(b);
				}
				c_genid_map.addAtomToBody(gen_map);
//				c1.addAtomToBody(a);
//				c2.getBody().addAll(adornedBody);
//				c2.getBody().remove(a);
////				c1.addAtomToHeads(c.getHead());
//				c.getBody().add(initAtom);
				c_genid_const.addAtomToHeads(gen_const);
				c_genid_const.addAtomToHeads(new Atom(Config.relname_gennewid, vars));
				cs.add(c_genid_const);
				cs.add(c_genid_map);	
				System.out.println("cs c_genid_const: " + c_genid_const);
				System.out.println("cs c_genid_map: " + c_genid_map);
				
				supplementRelationBody.clear();
				supplementRelationBody.add(decomposedRelationHead);
			} else { // adorned IDB
				HashSet<Atom> relatedInterpretedAtoms = atomToInterpretedAtomsMap.get(adornedAtomToBaseAtomMap.get(a)); 

				System.out.println("[process adorned Atom] a: " + a + " boundVars: " + boundVars 
						+ " relatedInterpretedAtoms: " + relatedInterpretedAtoms);

				boolean addornedRelAdded = false;
				if (relatedInterpretedAtoms != null) {
					for (Atom b : relatedInterpretedAtoms) {
						if (b.getRelName().equals("=") == false || b.getTerms().get(1).isConstant() == false) {
							System.out.println("a: " + a + " should be non-general adorned IDB due to b: " + b);
							
							a.getPredicate().setRelName(a.getRelName() + "_" + (indexOfadornedWithInterpretedAtoms++));
							break;
						}
					}
				} else {
					System.out.println("relatedInterpretedAtoms is null for a: " + a);
				}
				addAdornedRelToQueue(a, a.getAdornment());
				
				
				Atom supplementRelationHead = new Atom("SUP_" + supplementRelationIndex); // + a.getRelName()
				Atom inputSupplementRelationHead = new Atom("InSup_" + a.getRelName());
				
				decomposedRelationHead = new Atom("DECOM_" + supplementRelationIndex); // + a.getRelName() + "_"
				
				supplementRelationIndex++;

				// supRel keeps variabes appears (1) in the head or (2) in the following atoms in the body
				HashSet<String> supplementRelationHeadVars = new LinkedHashSet<String>();
				HashSet<String> decomposedRelationHeadVars = new LinkedHashSet<String>();

				HashSet<String> varsToKeepInSupplementRelationHead = new LinkedHashSet<String>();		
				varsToKeepInSupplementRelationHead.addAll(head.getVars());

				for (int j = i + 1; j < adornedBody.size(); j++) {
					Atom b = adornedBody.get(j);
					varsToKeepInSupplementRelationHead.addAll(b.getVars());
				}
				System.out.println("a: " + a + " supplementRelationHeadVars: " + supplementRelationHeadVars + " boundVars: " + boundVars + " varsToKeepInSupplementRelationHead: " + varsToKeepInSupplementRelationHead);

				supplementRelationHeadVars.addAll(boundVars);
//				supplementRelationHeadVars.addAll(a.getVars());
				supplementRelationHeadVars.retainAll(varsToKeepInSupplementRelationHead);

				System.out.println("a: " + a + " supplementRelationHeadVars2: " + supplementRelationHeadVars + " boundVars: " + boundVars + " varsToKeepInSupplementRelationHead: " + varsToKeepInSupplementRelationHead);
				System.out.println("inputSupplementRelationHead: " + inputSupplementRelationHead + " a: " + a + " boundVars: " + boundVars);
				
				for (int j = 0; j < a.getAdornment().size(); j++) {
					if (a.getAdornment().get(j) == true) {
						inputSupplementRelationHead.getTerms().add(new Term(a.getTerms().get(j).getVar(), true));
						supplementRelationHead.getTerms().add(new Term(a.getTerms().get(j).getVar(), true));
					}
				}
				
//				for (Term t : a.getTerms()) {
//					System.out.println("t: " + t + " boundVars: " + boundVars);
//					if (boundVars.contains(t.getVar()) == true) {
//						inputSupplementRelationHead.getTerms().add(new Term(t.getVar(), true));
//					}
//				}
				System.out.println("inputSupplementRelationHead: " + inputSupplementRelationHead);
				
				
				boundVars.addAll(a.getVars());
				decomposedRelationHeadVars.addAll(supplementRelationHeadVars);
				decomposedRelationHeadVars.addAll(a.getVars());
				decomposedRelationHeadVars.retainAll(varsToKeepInSupplementRelationHead);
				
				for (String v : supplementRelationHeadVars) {
					supplementRelationHead.getTerms().add(new Term(v, true));
				}
				for (String v : decomposedRelationHeadVars) {
					decomposedRelationHead.getTerms().add(new Term(v, true));
				}
				
				// InSupRel keeps variables to be used in the adorned IDB (same # as # of b's in the adornment)
//				for (int j = 0; j < a.getTerms().size(); j++) {
//					if (a.getAdornment().get(j) == true) {
//						inputSupplementRelationHead.getTerms().add(a.getTerms().get(j));
//					}
//				}			
				
//				if (supplementRelationHeadVars.size() == 0) {
//					throw new IllegalArgumentException("supplementRelationHeadVars should not be empty.");					
//				}
				
				DatalogClause supplementRule = new DatalogClause();
				DatalogClause decomposedRule = new DatalogClause();
				
//				if (initAtom != null) {
//					decomposedRule.addAtomToBody(initAtom);			
//				}				

				supplementRule.addAtomToHeads(supplementRelationHead);
				
//				System.out.println("inputSupplementRelationHead: " + inputSupplementRelationHead + " a: " + a + " boundVars: " + boundVars);
				if (inputSupplementRelationHead.getTerms().size() > 0) {
					supplementRule.addAtomToHeads(inputSupplementRelationHead);
				}
				if (i + 1 < adornedBody.size() ) {
//					if (initAtom != null) {
//						decomposedRule.getBody().add(0, initAtom);
//						decomposedRule.addDesc("initAtom_added");
//					}
					decomposedRule.addAtomToHeads(decomposedRelationHead);
					decomposedRule.addDesc("INTER_i[" + i + "]");
				} else {
					System.out.println("******FINAL BEFORE... a: " + a + " inputSupplementRelationHead: " + inputSupplementRelationHead);
//					decomposedRule.addAtomToHeads(inputSupplementRelationHead);
//					decomposedRule.addDesc("AddInSupToHead@FINAL");
					decomposedRule.addAtomToHeads(head);
					decomposedRule.addDesc("FINAL");
				}
				
				for (Atom b : supplementRelationBody) {
					supplementRule.addAtomToBody(b);
				}
				if (supplementRule.getBody().size() > 0) {
					decomposedRule.addAtomToBody(supplementRelationHead);
				}
				if (relatedInterpretedAtoms != null) {
					for (Atom b : relatedInterpretedAtoms) {
						if (b.getRelName().equals("=") == true && b.getTerms().get(1).isConstant() == true) {
							decomposedRule.addAtomToBody(b);
							DatalogClause rulePopulateInSup = new DatalogClause();
							Atom inSupHead = new Atom("InSup_" + a.getRelName());
							for (int j = 0; j < a.getAdornment().size(); j++) {
								if (a.getAdornment().get(j) == true) {
									inSupHead.getTerms().add(a.getTerms().get(j));
								}
							}
							rulePopulateInSup.addAtomToHeads(inSupHead);
							rulePopulateInSup.addAtomToBody(b);
							rulePopulateInSup.addDesc("INPUT_CONSTANT");
//							rewrittenProgram.addEDB(inSupHead.getRelName());
							cs.add(rulePopulateInSup);
							System.out.println("[214124] rulePopulateInSup: " + rulePopulateInSup);	
							
							atomToInterpretedAtomsMap.get(adornedAtomToBaseAtomMap.get(a)).remove(b);
						}
					}		
				}
//				if (decomposedRule.getBody().size() > 0) {
					decomposedRule.addAtomToBody(a);
//				}
				
				supplementRule.addDesc("SUPPLEMENT(" + a.getRelName() + ")");
				
				System.out.println("Check for adornedBody.size(): " + adornedBody.size() + " i: " + i + " adornedBody: " + adornedBody);

				if (i + 1 < adornedBody.size()) {
					System.out.println("Check for InSupFortheNextAtom a: " + a + " i: " + i);
					Atom nextAdornedAtom = adornedBody.get(i+1);
					Atom inSupForNextAdornedAtom = new Atom("InSup_" + nextAdornedAtom.getRelName());

					for (int j = 0; j < nextAdornedAtom.getAdornment().size(); j++) {
						if (nextAdornedAtom.getAdornment().get(j) == true) {
							inSupForNextAdornedAtom.getTerms().add(nextAdornedAtom.getTerms().get(j));
						}
					}
					
//					rewrittenProgram.addEDB(inSupForNextAdornedAtom.getRelName());
					supplementRule.addAtomToHeads(inSupForNextAdornedAtom);	
					System.out.println("InSupForNextAtom: " + inSupForNextAdornedAtom);					

					supplementRule.addDesc("InSupForNextAtom(" + inSupForNextAdornedAtom.getRelName() + ")");					
				}
				
				decomposedRule.addDesc("DECOMPOSED");
				
				System.out.println("supplementRule: " + supplementRule + " current a: " + a);
				System.out.println("decomposedRule: " + decomposedRule + " current a: " + a);
				
				supplementRelationBody.clear();
				supplementRelationBody.add(decomposedRelationHead);
				
				if (supplementRule.getBody().size() > 0) {
//					for (Atom c : supplementRule.getHeads()) {
//						rewrittenProgram.addEDB(c.getRelName());
//					}
					cs.add(supplementRule);
				}
				cs.add(decomposedRule);
//				rewrittenProgram.addEDB(decomposedRule.getHead().getRelName());
			}
		}
		if (supplementRelationBody.size() >= 1) { // flush
			if (supplementRelationBody.size() == 1 && supplementRelationBody.contains(decomposedRelationHead) == true) {
				// do nothing
			} else {
				DatalogClause decomposedRule = new DatalogClause();	
				decomposedRule.addAtomToHeads(head);
				
				System.out.println("[supplementRelationBody=empty head: " + head + " initAtom: "+ initAtom + " decomposedRule: " + decomposedRule);
				
	//			if (initAtom != null) {
	//				decomposedRule.addAtomToBody(initAtom);
	//			}
				
				for (Atom a : supplementRelationBody) {
					decomposedRule.addAtomToBody(a);
				}
				decomposedRule.addDesc("No Sup body");
				cs.add(decomposedRule);
	
				System.out.println("[supplementRelationBody - shoule be flusehd out] supplementRelationBody: " + supplementRelationBody);
			}
		}
		
		for (DatalogClause c : cs) {
			Util.console_logln("[******addedRules] c: " + c, 3);
			c.addDesc("CODE8");
			rewrittenProgram.addRule(c);
		}
		
		System.out.println("[decomposeBody] done with head: " + head);
	}
	
	private static void rewriteMagicSet(Atom head, Atom initAtom, ArrayList<Atom> body, boolean isBaseQuery) {
//		if (rewrittenProgram.getEDBs().contains(head.getRelName()) == true) {
//			return;
//		}
		Util.console_logln("[rewriteMagicSet} head: "+ head + " initAtom: " + initAtom + " body: " + body , 3);
		HashSet<String> boundVars = new LinkedHashSet<String>();
		 HashMap<Atom, HashSet<Atom>> atomToInterpretedAtomsMap = new  HashMap<Atom, HashSet<Atom>>();
		 
		HashSet<Atom> interpretedAtoms = constructInputRelation(boundVars, body);
//		System.out.println("EDBs1: " + rewrittenProgram.getEDBs());
		ArrayList<Atom> orderedBody = orderBody(head, initAtom, body, boundVars, interpretedAtoms, atomToInterpretedAtomsMap);
//		System.out.println("EDBs2: " + rewrittenProgram.getEDBs());
		ArrayList<Atom> adornedBody = adornBody(initAtom, orderedBody, boundVars, atomToInterpretedAtomsMap);
//		System.out.println("EDBs3: " + rewrittenProgram.getEDBs());
		decomposeBody(head, initAtom, adornedBody, atomToInterpretedAtomsMap, isBaseQuery);
		
		rewrittenProgram.addEDB(head.getRelName());
	}
	
	public static void addAdornedRelToQueue(Atom adornedAtom, ArrayList<Boolean> adornment) {
		if (rewrittenProgram.getEDBs().contains(adornedAtom.getRelName()) == false) {
			adornedRelsMap.put(adornedAtom, adornment);
		}
	}
	
	public static DatalogProgram rewrite(DatalogProgram p, DatalogClause c) {
		Config.initialize();
		
		System.out.println("[MST-rewrite] c: " + c);
		
		adornedRelsMap = new LinkedHashMap<Atom, ArrayList<Boolean>>();
		
		program = p;
		rewrittenProgram = new DatalogProgram(); // Retrieve the answer using the same head of c
		
//		System.out.println("rewrittenProgram: " + rewrittenProgram);
		//new Atom("Sup_" + c.getHead().getRelName() +"_0"), 
		rewriteMagicSet(c.getHead(), null, c.getBody(), true);
		
		while (adornedRelToInterpretedAtomsMap.size() > 0) {
			HashSet<Atom> adornedAtoms = new HashSet<Atom>();
			adornedAtoms.addAll(adornedRelToInterpretedAtomsMap.keySet());
			for (Atom a : adornedAtoms) {
				HashSet<Atom> interpretedAtoms = adornedRelToInterpretedAtomsMap.get(a);
				String baseIDBName = a.getBaseName();
				
				List<DatalogClause> rules = program.getRules(baseIDBName);

				
				System.out.println("TODO: a : " + a + " baseIDBName: " + baseIDBName + " map: " + adornedRelsMap.get(a) 
				+ " relatedInterp: " + interpretedAtoms + " rules.size(): " + rules.size());
				
				
				for (int i = 0; i < rules.size(); i++) {
					DatalogClause rule = rules.get(i);
					System.out.println("[rewrite] Do w/ rule i: " + i + " rule: " + rule);
					Atom newHead = null;
					try {
						newHead = (Atom) rule.getHead().clone();
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					newHead.getPredicate().setRelName(a.getRelName());

					Atom newInSup = new Atom("InSup_" + a.getRelName()); //
					System.out.println("[HERE12312] newHead: "+ newHead + " newInSup: " + newInSup);
					boolean hasBoundVar = false;
					for (int j = 0; j < newHead.getTerms().size(); j++) {
						if (a.getAdornment().get(j) == true) {
							newInSup.getTerms().add(newHead.getTerms().get(j));
						}
						if (a.getAdornment().get(j) == true) {
							hasBoundVar = true;
						}
					}
					if (hasBoundVar == false) {
						newInSup = null;
					}
					System.out.println("[124124124] newHead: "+ newHead + " newInSup: " + newInSup);
					
					ArrayList<Atom> newBody = new ArrayList<Atom>();
					
					System.out.println("[variableSubstitution] a: " + a + " head: " + rule.getHeads());
					HashMap<String, String> varMapping = new HashMap<String, String>();
					for (int k = 0; k < a.getTerms().size(); k++) {
						String from, to;
						from = a.getTerms().get(k).getVar();
						to = rule.getHead().getTerms().get(k).getVar();
						varMapping.put(from,  to);
					}
					
					for (Atom b : interpretedAtoms) {
//							if (b.getRelName().equals("=") == false || b.getTerms().get(1).isConstant() == false) {
							Atom b2 = null;
							try {
								b2 = (Atom)b.clone();
							} catch (CloneNotSupportedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							for (int kk = 0; kk < 2; kk++) {
								if (b2.getTerms().get(kk).isVariable() == true) {
									b2.getTerms().set(kk, new Term(varMapping.get(b2.getTerms().get(kk).getVar()), true));
								}
							}
							System.out.println("interpreted var substitution: " + b + " => " + b2);
							newBody.add(b2);
//							}
					}
					
					newBody.addAll(rule.getBody());
					
					System.out.println("[rewrite] call rewriteMagicSet with newHead: "+ newHead + " newInSup: " + newInSup);					
					rewriteMagicSet(newHead, newInSup, newBody, false);
				}				
				
			}
			for (Atom a: adornedAtoms) {
				adornedRelToInterpretedAtomsMap.remove(a);
			}
			System.out.println("[NOW] adornedRelToInterpretedAtomsMap: " + adornedRelToInterpretedAtomsMap);
		}
//		System.exit(0);
		
		
//		while(adornedRelsQueue.isEmpty() == false) {
//			Atom adornedAtom = adornedRelsQueue.poll();
//			int position = adornedAtom.getRelName().indexOf("_");
////			System.out.println("adornedRelName: " + adornedRelName + " position: " + position);
//			String relName = adornedAtom.getRelName().substring(0, position); 
//			ArrayList<Boolean> adornment = adornedRelsMap.get(adornedAtom);
//
//			List<DatalogClause> rules = program.getRules(relName);
////			System.out.println("Looking for rules of relName: " + relName);
//			
//			if (rules == null) {
//				throw new IllegalArgumentException("rules is NULL relName: " + relName + " program: " + program);
//			}
//			
//			HashSet<Atom> relatedInterpretedAtoms = adornedRelToInterpretedAtomsMap.get(adornedAtom);
//			System.out.println("adornedAtom: " + adornedAtom + " relatedInterpretedAtoms: " + relatedInterpretedAtoms);
//			
//			for (DatalogClause rule : rules) {
////				System.out.println("to MST rule: " + rule);
//				
//				Atom newHead = null;
//				try {
//					newHead = (Atom) rule.getHead().clone();
//				} catch (CloneNotSupportedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				newHead.getPredicate().setRelName(adornedAtom.getRelName());
//				
//				Atom newInSup = new Atom("InSup_" + adornedAtom.getRelName()); //
////				System.out.println("newHead: "+ newHead + " newInSup: " + newInSup);
//				boolean hasBoundVar = false;
//				for (int j = 0; j < newHead.getTerms().size(); j++) {
//					if (adornment.get(j) == true) {
//						newInSup.getTerms().add(newHead.getTerms().get(j));
//					}
//					if (adornment.get(j) == true) {
//						hasBoundVar = true;
//					}
//				}
//				if (hasBoundVar == false) {
//					newInSup = null;
//				}
//				
//				ArrayList<Atom> newBody = new ArrayList<Atom>();
//				
//				if (adornedRelToInterpretedAtomsMap.containsKey(adornedAtom) == true) {
//					for (Atom b : adornedRelToInterpretedAtomsMap.get(adornedAtom)) {
//						if (b.getRelName().equals("=") == false || b.getTerms().get(1).isConstant() == false) {
//							newBody.add(b);
//						}
//					}
//				}
//				
//				
//				newBody.addAll(rule.getBody());
//				rewriteMagicSet(newHead, newInSup, newBody, false);
//			}
//		}
		
		return rewrittenProgram;	
	}
		
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("[MagicSetRewriter]");
		
		DatalogProgram p = new DatalogProgram();
		DatalogParser parser = new DatalogParser(p);
		
		// TODO Auto-generated method stub
		Config.initialize();
		
//		
//		String rule = "R(a,b) -> int(a), int(b)."
//				+ "S(a,b) -> int(a), int(b)."
//				+ "V1(a,b) <- R(a,c), S(c,b).\n"
//				+ "V2(a,b) <- R(a,c), R(c,b).";
//		p.addEDB("R");
//		p.addEDB("S");
//		DatalogClause q = parser.ParseQuery("Q(b) <- a=1, V2(a,c), R(a,c), V2(c,b).");
//	
//		
		
//		String rule = "T(a,b) <- E(a,b).\n"
////				+ "T(a,b) <- T(a,c), E(c,b).";		
//				+ "T(a,b) <- E(a,c), T(c,b).";		
//		
//		p.addEDB("E");
//		DatalogClause q = parser.ParseQuery("Q(b) <- a=1, T(a,b).");

		

//		String rule = "// positive\n"
//				+ "V1(a,b) <- R(a,c),S(c,b).\n"
//				+ "V2(a,b) <- T(a,c),T(c,b).\n";		
//
//		p.addEDB("R");
//		p.addEDB("S");
//		p.addEDB("T");
//		DatalogClause q = parser.ParseQuery("Q(b) <- a=1, V1(a,b), V2(b,c).");
//
		
//		String rule = "// non constant bound vars - positive\n"
//		+ "V1(a,b) <- R(a,c),S(c,b).\n"
//		+ "V2(a,b) <- T(a,c),T(c,b).\n";		
//
//		p.addEDB("R");
//		p.addEDB("S");
//		p.addEDB("T");
//		DatalogClause q = parser.ParseQuery("Q(d, b) <- b < 3, s < 10, V1(a,b), V2(b,c), R(s,d).");
//

		
		
//		String rule = "// negation\n"
//		+ "V1(a,b) <- R(a,c),S(c,b).\n"
//		+ "V2(a,b) <- T(a,c),T(c,b).\n";		
//
//		p.addEDB("R");
//		p.addEDB("S");
//		DatalogClause q = parser.ParseQuery("Q(b) <- a=1, V1(a,b), !V2(b,c).");

		
//		String rule = "// negation with recursion\n"
//		+ "V1(a,b) <- R(a,c),S(c,b).\n"
//		+ "V2(a,b,h) <- T(a,c),T(c,b), GEN_1(a,b,h).\n"
//		+ "L(a,b) <- E(a,b).\n"
//		+ "L(a,b) <- L(a,c), E(c,b).\n";
//
//		p.addEDB("GEN_1");
//		p.addEDB("R");
//		p.addEDB("S");
//		p.addEDB("T");
//		p.addEDB("E");
//		// variables in negated atom should appear in some positive IDBs/EDBs
////		DatalogClause q = parser.ParseQuery("Q(b) <- a=1, V1(a,b), !L(b,c), V2(b,c), b>2, c>10");
//		DatalogClause q = parser.ParseQuery("Q(b,h) <- a=1, V1(a,b), V2(b,c,h), b>2, c>10, b>c");
//		
//		
		
		String rule = "MATCH(a,b,c,x,y) <- N(a,al), N(b,bl), N(c,cl), E(x,a,b,xl), E(y,b,c,yl), al=890,bl=891,cl=892,xl=990,yl=991.\n"
				+ "N1(a,l), MAP(a,h) <- l = 999, MATCH(a,b,c,x,y), " + Config.relname_gennewid + "_1(a,b,c,x,y,h).\n"//, GEN_1(a,b,c,x,y,h).\n"
//				+ "N1(b,l), MAP(b,h) <- l = 888, MATCH(a,b,c,x,y), " + Config.relname_gennewid + "_2(a,b,c,x,y,h).\n"//, GEN_1(a,b,c,x,y,h).\n"
//				+ "DMAP(s,d) <- MAP(s,d).\n"
				+ "DMAP(n,n) <- N(n,l), !MAP(n,t1).\n"
				+ "N1(a,l) <- N(a,l).\n"
				+ "E1(e,s1,d1,l) <- E(e,s,d,l), DMAP(s,s1), DMAP(d,d1).\n"
//				+ "MAP(b,h) <- MATCH(a,b,c,x,y), GEN_1(a,b,c,x,y,h).\n"
				;

//		p.addEDB("GEN_1");
		p.addEDB("N");
		p.addEDB("E");

		// variables in negated atom should appear in some positive IDBs/EDBs
//		DatalogClause q = parser.ParseQuery("Q(b) <- a=1, V1(a,b), !L(b,c), V2(b,c), b>2, c>10");
//		DatalogClause q = parser.ParseQuery("Q(a,l) <- N1(a,l).");
		DatalogClause q = parser.ParseQuery("Q(e,s,d,l) <- E1(e,s,d,l).");
//		DatalogClause q = parser.ParseQuery("Q(s,d) <- DMAP(s,d).");
		

//		String rule = ""
//				+ "V1(a,b) <- R(a,c), S(c,b).\n"
//				+ "V2(c,d) <- V1(c,e), V1(c,d).\n"
//				;
//		p.addEDB("R");
//		p.addEDB("S");
//		DatalogClause q = parser.ParseQuery("Q(b) <- a<10, V1(a,c), V2(c,b).");
//		DatalogClause q = parser.ParseQuery("Q(b) <- a<10, V2(a,b).");
//		DatalogClause q = parser.ParseQuery("Q(b) <- a<10, V1(a,b), !S(b,c).");
	
	
		parser.ParseAndAddRules(rule);
		System.out.println("program p : " + p);
		
//		System.out.println("q: " + q);
		
		DatalogProgram rewrittenProgram = MagicSetRewriter.rewrite(p, q);
		
		System.out.println("[main] rewrittenProgram:\n" + rewrittenProgram);
				
		
//		Atom a = new Atom("V1"); 
//		Boolean[] ad = {true, true};
//		List<Boolean> adornment = Arrays.asList(ad);
//		defineAdornedRelation(a, adornment);

	}

}
