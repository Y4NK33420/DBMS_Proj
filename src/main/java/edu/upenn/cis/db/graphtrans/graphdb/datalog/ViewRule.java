package edu.upenn.cis.db.graphtrans.graphdb.datalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.parser.LabelRegExParser;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.helper.Util;
import org.apache.commons.lang3.tuple.Pair;


/**
 * Generate Datalog rules from a view definition
 * 
 * @author sbnet21
 *
 */
public class ViewRule {
	final static Logger logger = LogManager.getLogger(ViewRule.class);

	private static int rid = 0;
	private static String viewName;
	private static String baseName;

	private static String MAP;
	private static String DMAP;
	private static String NDA;
	private static String NDD;
	private static String EDA;
	private static String EDD;
	private static String N0;
	private static String E0;
	private static String NP0;
	private static String EP0;
	private static String N1;
	private static String E1;
	private static String NP1;
	private static String EP1;

	private static HashSet<String> pathVariables;
	private static HashSet<String> availableRels;
	private static DatalogProgram program;

	private static Atom matchHeadAtom;
//	private static HashSet<String> matchVarsToBeUsedInF;
	
	private static int recursionId = 0;

	private static void initialize(String base, String view) {
		baseName = base;
		viewName = view;

		MAP = Config.relname_mapping + "_" + viewName;
		DMAP = Config.relname_default_mapping + "_" + viewName;
		NDA = Config.relname_node + "_" + Config.relname_added + "_" + viewName;
		NDD = Config.relname_node + "_" + Config.relname_deleted + "_" + viewName;
		EDA = Config.relname_edge + "_" + Config.relname_added + "_" + viewName;
		EDD = Config.relname_edge + "_" + Config.relname_deleted + "_" + viewName;
		N0 = Config.relname_node + "_" + baseName;
		E0 = Config.relname_edge + "_" + baseName;
		N1 = Config.relname_node + "_" + viewName;
		E1 = Config.relname_edge + "_" + viewName;
		NP0 = Config.relname_nodeprop + "_" + baseName;
		EP0 = Config.relname_edgeprop + "_" + baseName;
		NP1 = Config.relname_nodeprop + "_" + viewName;
		EP1 = Config.relname_edgeprop + "_" + viewName;		
		availableRels = new HashSet<String>();
//		matchVarsToBeUsedInF = new LinkedHashSet<String>();
		pathVariables = new HashSet<String>();
	}
	
//	public static HashSet<String> getMatchVarsToBeUsedInF() {
//		return matchVarsToBeUsedInF;
//	}

	private static boolean checkIncludedAtom(TransRule transRule, Atom a, boolean useWhereClause) {
		if (a.getPredicate().isInterpreted() == true && useWhereClause == false) {
			if (transRule.getVarsInWhereClause().contains(a.getTerms().get(0).getVar()) == true) {
				return false;
			}
		}
		return true;
	}

	public static void insertCatalogView(Store store, String name, String base, String type, String query, long level) {
		ArrayList<SimpleTerm> args = new ArrayList<SimpleTerm>();
		args.add(new StringSimpleTerm(name));
		args.add(new StringSimpleTerm(base));
		args.add(new StringSimpleTerm(type));
		args.add(new StringSimpleTerm(Util.addSlashes(query)));
		args.add(new LongSimpleTerm(level));
		store.addTuple(Config.relname_catalog_view, args);
	}

	public static void addViewRuleToProgram(DatalogProgram p, 
			TransRuleList transRuleList, boolean isAllIncluded, boolean usewhereClause) {
		addViewRuleToProgram(p, transRuleList, isAllIncluded, -1, usewhereClause);
	}

	public static void addMatchRule(TransRuleList rules, int index, boolean useWhereClause) {
//		matchVarsToBeUsedInF.clear();
		TransRule tr = rules.getTransRuleList().get(index);
		HashSet<String> headVars = new LinkedHashSet<String>();
		for (Atom a : tr.getPatternMatch()) {
			if (a.isNegated() == true) continue;
			
			if (useWhereClause == false) {
				if (a.getPredicate().getRelName().contentEquals(Config.relname_edgeprop) == true ||
						a.getPredicate().getRelName().contentEquals(Config.relname_nodeprop) == true) {
					continue;
				}
				if (a.isInterpreted() == true && tr.getVarsInWhereClause().contains(a.getTerms().get(0).getVar()) == true) {
					continue;
				}
			}
			headVars.addAll(a.getVars());
			
			if (a.getPredicate().getRelName().contentEquals(Config.relname_edge) == true) {
				String labels = Util.removeQuotes(a.getTerms().get(3).toString());
				Pair<Boolean, String> regExBody = LabelRegExParser.Parse(labels);
				
//				System.out.println("CODE 140987124 - regExBody: " + regExBody);
				
				if (regExBody.getLeft() == true) { // Label is A
					pathVariables.add(a.getTerms().get(0).getVar());
				}
			}			
		}
//		System.out.println("CODE 140987124 - headVars: " + headVars);

		String headRel = Config.relname_match + "_" + rules.getViewName() + "_" + index;
		DatalogClause c = new DatalogClause();
		headVars.removeAll(pathVariables);
		Atom head = new Atom(headRel, headVars);
		c.setHead(head);
		c.addAtomToHeads(head);
		
		for (int j = 0; j < tr.getPatternMatch().size(); j++) {
			Atom a = tr.getPatternMatch().get(j);
			if (checkIncludedAtom(tr, a, useWhereClause) == false) {
				continue;
			}
			if (useWhereClause == false) {
				if (a.getPredicate().getRelName().contentEquals(Config.relname_nodeprop) == true ||
						a.getPredicate().getRelName().contentEquals(Config.relname_edgeprop) == true) {
					continue;
				}
			}

			Atom b = null;
			if (a.isInterpreted() == false) {
				String relName = a.getPredicate().getRelName() + "_" + rules.getBaseName();
				try {
					b = (Atom)a.clone();
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				b.getPredicate().setRelName(relName);

				if (a.equals(b) == true) {
					System.out.println("[OMG] a.equals(b) is true");
				}				 

				if (a.getPredicate().equals(b.getPredicate()) == true) {
					System.out.println("[OMG] a.getPredicate().equals(b.getPredicate()) is True");
				}
				
//				System.out.println("CODE 214124 - here add Edge or Node atom b : " + b);
				if (a.getPredicate().getRelName().contentEquals(Config.relname_edge) == true) {
					String labels = Util.removeQuotes(b.getTerms().get(3).toString());
//					System.out.println("CODE 21412 - labels: " + labels);
					Pair<Boolean, String> regExBody = LabelRegExParser.Parse(labels);
					
					if (regExBody.getLeft() == false) { // Label is A
						c.getBody().add(b);
					} else { // Label is (AB)*
						String recursionRel = "REC_" + (recursionId++);
						String realLabels = regExBody.getRight();
						DatalogClause recursion1 = new DatalogClause();
						DatalogClause recursion2 = new DatalogClause();
						
						Atom recursionRepeat = new Atom(recursionRel, "s", "n0");

						recursion2.addAtomToBody(recursionRepeat);
						
						String src = b.getTerms().get(1).getVar();
						String dst = b.getTerms().get(2).getVar();
						
//						System.out.println("CODE 21412 - real labels: " + realLabels + " src: " + src + " dst: " + dst);

						for (int r = 0; r < realLabels.length(); r++) {
							boolean isLast = (r + 1 == realLabels.length()) ? true : false;
							
							System.out.println(realLabels.charAt(r));
							ArrayList<Atom> atomsToAdd = new ArrayList<Atom>();
							
							atomsToAdd.add(new Atom("N_g", "n" + r, "l" + r));
							atomsToAdd.add(new Atom("E_g", "e" + r, "n" + r, "n" + (r + 1), "\"" + realLabels.charAt(r) + "\""));

							for (Atom atom : atomsToAdd) {
								recursion1.addAtomToBody(atom);
								recursion2.addAtomToBody(atom);
							}
						}
						Atom recursionHead1 = new Atom(recursionRel, "n0", "n" + realLabels.length());
						Atom recursionHead2 = new Atom(recursionRel, "s", "n" + realLabels.length());
						recursion1.setHead(recursionHead1);
						recursion2.setHead(recursionHead2);

//						System.out.println("rec1: " + recursion1);
//						System.out.println("rec2: " + recursion2);
						
						program.addRule(recursion1);
						program.addRule(recursion2);

						Atom newAtom = new Atom(recursionRel, src, dst);
						c.getBody().add(newAtom);
						
						continue; // not to add path variable to Vars
					}
				} else {
					c.getBody().add(b);
				}
			} else {
				c.getBody().add(a);
			}
		}
		matchHeadAtom = c.getHead();
		c.addDesc("match rule");
		program.addRule(c);
		
		ArrayList<Integer> indexSet = null;

		for (int i = 0; i < c.getHead().getTerms().size(); i++) {
			indexSet = new ArrayList<Integer>();
			indexSet.add(i);
			program.addIndexSet(matchHeadAtom.getRelName(), indexSet);
		}
	}
	
	

	public static Atom getNewIdAtom(ArrayList<String> skolemArgs, String newVar) {
		String skolemName = Util.removeQuotes(skolemArgs.get(0));
		String udf = Config.relname_gennewid + "_MAP_" + viewName + "_" + skolemName;
		Atom a = new Atom(new Predicate(udf)); // len

		for (int i = 1; i < skolemArgs.size(); i++){
			String v = skolemArgs.get(i);
			a.getTerms().add(new Term(v, true));
		}
		a.getTerms().add(new Term(newVar, true));
		program.getUDFs().add(udf);
		program.getEDBs().add(udf);

		return a;
	}

	public static void addMapRules(TransRuleList rules, int index, boolean useWhereClause) {
		TransRule tr = rules.getTransRuleList().get(index);
		
//		System.out.println("code 34978 getMapFromToMap => " + tr.getMapFromToMap());
//		System.out.println("code 34978 getSkolemFunctionMap => " + tr.getSkolemFunctionMap());
		
		ArrayList<DatalogClause> cs = new ArrayList<DatalogClause>();
		
		// Node variables can be mapped to node variables, while edge variables can be mapped to edge variables. 
		// However, this can introduce awkward cases below, but we allow it.
		// CONSTRUCT (b)-[x]->(b) MATCH (a:A)-[x:X]->(b:B)
		//
		//	for each atom in CONSTRUCT
		//		if the atom is in MATCH
		//			add to N', E' if no default rules 
		//		else
		//			add to N', E' as newVar 
		// 	for each atom in MATCH
		//		if atom's id is not in CONSTRUCT
		//			add to N-, E- 
		// CONSTRUCT, ADD, DELETE
		// edge reassign?
		
		for (Atom a : tr.getPatternConstruct()) {
			DatalogClause c_gennewid_map = new DatalogClause();

			int labelIndex = 1;
			if (a.getRelName().startsWith("E") == true) {
				labelIndex = 3;
			}
			String var = a.getTerms().get(0).getVar();
			String label = a.getTerms().get(labelIndex).toString();
			Atom b4 = null;
			Atom b5 = null;
			try {
				b4 = (Atom)a.clone();
				
				b5 = new Atom(Config.predOpEq);
				b5.appendTerm(new Term(var + "_label", true));
				b5.appendTerm(new Term(label, false));
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			b4.getPredicate().setRelName(b4.getRelName() + "_" + rules.getViewName());
			b4.getTerms().set(labelIndex, new Term(var + "_label", true));
			
			c_gennewid_map.addAtomToHeads(b4);
			c_gennewid_map.addAtomToBody(b5);
			c_gennewid_map.addAtomToBody(matchHeadAtom);

			for (int i = 0; i < a.getTerms().size()-1; i++) {
				String dstVar = a.getTerms().get(i).getVar();
				if (tr.getSkolemFunctionMap().containsKey(dstVar) == true) {
					ArrayList<String> srcVars = tr.getSkolemFunctionMap().get(dstVar);
					Atom a1 = new Atom(Config.relname_gennewid + "_MAP_" + viewName + "_" + Util.removeQuotes(srcVars.get(0)));
					for (int j = 1; j < srcVars.size(); j++) {
						String srcVar = srcVars.get(j);
						a1.appendTerm(new Term(srcVar, true));					
					}
					a1.appendTerm(new Term(dstVar, true));
					
					program.getUDFs().add(a1.getRelName());
					program.getEDBs().add(a1.getRelName());
					
					c_gennewid_map.addAtomToBody(a1);					
				}
			}
//					Atom b4 = null;
//					Atom b5 = null;
//					
//					int labelIndex = 1;
//					if (a.getRelName().startsWith("E") == true) {
//						labelIndex = 3;
//					}
//					String label = a.getTerms().get(labelIndex).toString();
//					try {
//						b4 = (Atom)a.clone();
//						b4.getPredicate().setRelName(b4.getRelName() + "_" + rules.getViewName());
//						b4.getTerms().set(labelIndex, new Term(var + "_label", true));
//	
//						b5 = new Atom(Config.predOpEq);
//						b5.appendTerm(new Term(var + "_label", true));
//						b5.appendTerm(new Term(label, false));
//					} catch (CloneNotSupportedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					c_gennewid_map.addAtomToHeads(b4);
//					c_gennewid_map.addAtomToBody(b5);
//				}
//			} 
			cs.add(c_gennewid_map);			
		}
		
		for (String dstVar : tr.getSkolemFunctionMap().keySet()) {
			DatalogClause c_gennewid_const = new DatalogClause();
			c_gennewid_const.addAtomToBody(matchHeadAtom);

			ArrayList<String> srcVars = tr.getSkolemFunctionMap().get(dstVar);
//			System.out.println("dstVar: " + dstVar + " srcVars: " + srcVars);
			
			String skolemName = Util.removeQuotes(srcVars.get(0));
			Atom a1 = new Atom(Config.relname_gennewid + "_CONST_" + viewName + "_" + skolemName);
			for (int i = 1; i < srcVars.size(); i++) {
				String srcVar = srcVars.get(i);
				a1.appendTerm(new Term(srcVar, true));					
			}
			a1.appendTerm(new Term(dstVar, true));
			c_gennewid_const.addAtomToHeads(a1);
			
			a1 = new Atom(Config.relname_gennewid + "_" + viewName);
			a1.appendTerm(new Term(dstVar, true));
			c_gennewid_const.addAtomToHeads(a1);
			
			program.addConstructorForLB(viewName, skolemName, srcVars.size()-1);

			cs.add(c_gennewid_const);
		}
	
//		for (DatalogClause c : cs) {
//			System.out.println("gennewid c: " + c);
//		}
		
		for (DatalogClause c : cs) {
			program.addRule(c);
		}

		
//		System.exit(0);
		

//			if (rules.isDefaultMap() == false) {
//				// without default rules, add to N', E'
//				c2.addAtomToBody(matchHeadAtom);
//			
//				Atom b4 = null;
//				int labelIndex = 1;
//				if (a.getRelName().startsWith("E") == true) {
//					labelIndex = 3;
//				}
//				String label = a.getTerms().get(labelIndex).toString();
//				String var = a.getTerms().get(0).getVar();
//				try {
//					b4 = (Atom)a.clone();
//					b4.getPredicate().setRelName(b4.getRelName() + "_" + rules.getViewName());
//					b4.getTerms().set(labelIndex, new Term(var + "_label", true));
//					
//					Atom b5 = new Atom(Config.predOpEq);
//					b5.appendTerm(new Term(var + "_label", true));
//					b5.appendTerm(new Term(label, false));
//					c2.addAtomToBody(b5);
//				} catch (CloneNotSupportedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				c2.addAtomToHeads(b4);
//				System.out.println("code 320423111 - c2: " + c2);		
//				c2.addDesc("atom in construct added");
//				cs.add(c2);
//			}
//		}
//		
//
//		if (rules.isDefaultMap() == false) {
//			//FIXE: same thing from below
//			for (Atom b2 : tr.getPatternConstruct()) {
//				if (tr.getNewEdgeVars().contains(b2.getTerms().get(0).getVar().toString()) == true ||
//						tr.getNewNodeVars().contains(b2.getTerms().get(0).getVar().toString()) == true) {
//					continue;
//				}
//
//				DatalogClause c2 = new DatalogClause();
//			
//				c2.addAtomToBody(matchHeadAtom);
////				c2.addAtomToBody(newIdAtom);
//	
//				Atom b4 = null;
//				int labelIndex = 1;
//				if (b2.getRelName().startsWith("E") == true) {
//					labelIndex = 3;
//				}
//				String label = b2.getTerms().get(labelIndex).toString();
//				String var = b2.getTerms().get(0).getVar();
//				try {
//					b4 = (Atom)b2.clone();
//					b4.getPredicate().setRelName(b4.getRelName() + "_" + rules.getViewName());
//					b4.getTerms().set(labelIndex, new Term(var + "_label", true));
//					
//					Atom b5 = new Atom(Config.predOpEq);
//					b5.appendTerm(new Term(var + "_label", true));
//					b5.appendTerm(new Term(label, false));
//					c2.addAtomToBody(b5);
//				} catch (CloneNotSupportedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				c2.addAtomToHeads(b4);
//				System.out.println("code 320423sdfsdf - c2: " + c2);		
//				c2.addDesc("atom in construct added");
//				cs.add(c2);
//			}
//		}	
//		
//		for (Atom b2 : tr.getPatternConstruct()) {
//			DatalogClause c2 = new DatalogClause();
//			c2.addAtomToBody(matchHeadAtom);
//			c2.addAtomToBody(newIdAtom);
//
//			for (int i = 0; i < b2.getTerms().size()-1; i++) {
//				String var = b2.getTerms().get(i).getVar();
//				if (tr.getNewEdgeVars().contains(var) == true || tr.getNewNodeVars().contains(var) == true ) { // new variable
//					Atom b4 = null;
//					int labelIndex = 1;
//					if (b2.getRelName().startsWith("E") == true) {
//						labelIndex = 3;
//					}
//					String label = b2.getTerms().get(labelIndex).toString();
//					
//					try {
//						b4 = (Atom)b2.clone();
//						b4.getPredicate().setRelName(b4.getRelName() + "_" + rules.getViewName());
//						b4.getTerms().set(labelIndex, new Term(dstVar + "_label", true));
//						
//						Atom b5 = new Atom(Config.predOpEq);
//						b5.appendTerm(new Term(dstVar + "_label", true));
//						b5.appendTerm(new Term(label, false));
//						c2.addAtomToBody(b5);
//					} catch (CloneNotSupportedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					c2.addAtomToHeads(b4);
//				}
//			}
//			System.out.println("code 32042314124 - c2: " + c2);		
//			c2.addDesc("map - added in construct");
//			cs.add(c2);
//		}	
//		
//		
//		
//		for (String dstVar : tr.getSkolemFunctionMap().keySet()) {
//			ArrayList<String> srcVars = null;
//			if (tr.getMapFromToMap().containsKey(dstVar) == true) {
//				srcVars = tr.getMapFromToMap().get(dstVar);
//			}
//			DatalogClause c = null;
//			Atom head = null;
//			Atom b = null;
//			
////			for (int i = 0; i < tr.getSkolemFunctionMap().get(dstVar).size(); i++) {
////				if (i == 0) continue;
////				matchVarsToBeUsedInF.add(tr.getSkolemFunctionMap().get(dstVar).get(i));
////			}
////			String skolemName = Util.removeQuotes(tr.getSkolemFunctionMap().get(dstVar).get(0));
//			Atom newIdAtom = getNewIdAtom(tr.getSkolemFunctionMap().get(dstVar), dstVar);
//			
////			System.out.println("skolemName: " + skolemName + " matchVarsToBeUsedInF: " + matchVarsToBeUsedInF);
//
//			if (rules.isDefaultMap() == true && srcVars != null) { // MAP FROM ... TO ...
//				for (String srcVar : srcVars) {
//					c = new DatalogClause();
//					String srcVarLabel = null;
//					String srcLabel = null;
//	
//					for (Atom a : tr.getPatternMatch()) {
//						if (checkIncludedAtom(tr, a, useWhereClause) == false) {
//							continue;
//						}
//						if (a.getTerms().get(0).getVar().contentEquals(srcVar) == true) {
//							if (a.getTerms().get(1).isConstant() == true) {
//								String label = a.getTerms().get(1).toString();
//								srcVarLabel = "__label";
//								srcLabel = label;
//							} else {
//								srcLabel = a.getTerms().get(0).toString() + "_label";
//								srcVarLabel = a.getTerms().get(0).toString() + "_label";
//							}
//							break;
//						}
//					}
//					if (srcVarLabel == null) {
//						throw new IllegalArgumentException("srcVarLabel is null. srcVar: " + srcVar + ");//, transRule.getMapMap(): " + tr.getMapMap());
//					}
//	
//					head = new Atom(MAP);
//					head.appendTerm(new Term(srcVar, true));
//					head.appendTerm(new Term(dstVar, true));
//					c.addAtomToHeads(head);
//					c.getBody().add(matchHeadAtom);
//					c.getBody().add(newIdAtom);
//					c.addDesc("map - rule");
//					
//					availableRels.add("MAP");
//
//					cs.add(c);
//				}
//			}
//			
//			System.out.println("code 324 => c: " + c);
//			// add new node
//			for (Atom b2 : tr.getPatternConstruct()) {
//				DatalogClause c2 = new DatalogClause();
//				c2.addAtomToBody(matchHeadAtom);
//				c2.addAtomToBody(newIdAtom);
//
//				for (int i = 0; i < b2.getTerms().size()-1; i++) {
//					String var = b2.getTerms().get(i).getVar();
//					if (tr.getNewEdgeVars().contains(var) == true || tr.getNewNodeVars().contains(var) == true ) { // new variable
//						Atom b4 = null;
//						int labelIndex = 1;
//						if (b2.getRelName().startsWith("E") == true) {
//							labelIndex = 3;
//						}
//						String label = b2.getTerms().get(labelIndex).toString();
//						
//						try {
//							b4 = (Atom)b2.clone();
//							b4.getPredicate().setRelName(b4.getRelName() + "_" + rules.getViewName());
//							b4.getTerms().set(labelIndex, new Term(dstVar + "_label", true));
//							
//							Atom b5 = new Atom(Config.predOpEq);
//							b5.appendTerm(new Term(dstVar + "_label", true));
//							b5.appendTerm(new Term(label, false));
//							c2.addAtomToBody(b5);
//						} catch (CloneNotSupportedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						c2.addAtomToHeads(b4);
//					}
//				}
//				System.out.println("code 32042314124 - c2: " + c2);		
//				c2.addDesc("map - added in construct");
//				cs.add(c2);
//			}	
//			availableRels.add("DMAP");
//
////			System.out.println("code 3241 matchVarsToBeUsedInF: " + matchVarsToBeUsedInF);
//			String skolemName = Util.removeQuotes(tr.getSkolemFunctionMap().get(dstVar).get(0));
//			int sizeOfVarsInConstructAtom = tr.getSkolemFunctionMap().get(dstVar).size()-1;
//				program.addConstructorForLB(viewName, skolemName, sizeOfVarsInConstructAtom);
//
//				DatalogClause c1 = new DatalogClause();
//				Atom a1 = new Atom(Config.relname_gennewid + "_CONST_" + viewName + "_" + Util.removeQuotes(skolemName));
//				for (int i = 1; i < tr.getSkolemFunctionMap().get(dstVar).size(); i++) {
//					String v = tr.getSkolemFunctionMap().get(dstVar).get(i);
//					a1.appendTerm(new Term(v, true));					
//				}
//				a1.appendTerm(new Term("_v", true));
//				c1.addAtomToHeads(a1);
//				a1 = new Atom(Config.relname_gennewid + "_" + viewName);
//				a1.appendTerm(new Term("_v", true));
//				c1.addAtomToHeads(a1);
//				c1.addAtomToBody(matchHeadAtom);
//				program.addRule(c1);
//		}	
//		
//		System.out.println("code 14870912 cs: " + cs);
//		
	
	}

	public enum OPTION {
		ADD, REMOVE
	};

	public static void addAddRemoveRules(TransRuleList rules, int index, boolean useWhereClause) {
		TransRule tr = rules.getTransRuleList().get(index);
		DatalogClause c = new DatalogClause();
		
		for (String var : tr.getNodeVarsToDelete()) {
			Atom b2 = new Atom(NDD, var);
			c.addAtomToHeads(b2);
			c.addDesc("N-[" + var + "]");
			availableRels.add("NDD");
		}
		
		for (String var : tr.getEdgeVarsToDelete()) {
			Atom b2 = new Atom(EDD, var);
			c.addAtomToHeads(b2);
			c.addDesc("E-[" + var + "]");
			availableRels.add("EDD");
		}
		
		if (c.getHeads().size() > 0) {
			c.addAtomToBody(matchHeadAtom);
			program.addRule(c);
		}
	}	


	public static void addViewRules() {
		DatalogClause c = new DatalogClause();
		c.addAtomToHeads(new Atom(DMAP, "id", "id"));
		c.addAtomToHeads(new Atom(N1, "id", "label"));
		c.addAtomToBody(new Atom(N0, "id", "label"));
		if (availableRels.contains("MAP") == true) {
			c.addAtomToBody(new Atom(false, MAP, "id", "_"));
		}
		if (availableRels.contains("NDD") == true) {
			c.addAtomToBody(new Atom(false, NDD, "id"));
		}		
		c.addDesc("default rule - n' and dmap");
		program.addRule(c);
		availableRels.add("DMAP");

		if (availableRels.contains("MAP") == true) {
			c = new DatalogClause();
			c.addAtomToHeads(new Atom(DMAP, "src", "dst"));
			c.addAtomToBody(new Atom(MAP, "src", "dst"));
			if (availableRels.contains("NDD") == true) {
				c.addAtomToBody(new Atom(false, NDD, "id"));
			}
			c.addDesc("default rule - copy map to dmap");
			program.addRule(c);
		} 
		c = new DatalogClause();
		c.addAtomToHeads(new Atom(E1, "id", "src2", "dst2", "label"));
		c.addAtomToBody(new Atom(E0, "id", "src", "dst", "label"));
		c.addAtomToBody(new Atom(DMAP, "src", "src2"));
		c.addAtomToBody(new Atom(DMAP, "dst", "dst2"));
		if (availableRels.contains("EDD") == true) {
			c.addAtomToBody(new Atom(false, EDD, "id"));
		}
		c.addDesc("default rule - e'");		
		program.addRule(c);
	}

	public static void addViewRulesForSingleRule(TransRuleList rules, boolean isAllIncluded, int index, boolean useWhereClause) {		
		//		System.out.println("[addViewRulesForSingleRule] index: " + index + " useWhereClause: " + useWhereClause);
		String type = rules.getViewType();
		
		if (type.equals("materialized") == true) {
			addMatchRule(rules, index, useWhereClause);
			addMapRules(rules, index, useWhereClause);
			addAddRemoveRules(rules, index, useWhereClause);
			if (rules.getNumTransRuleList() == index + 1) {
//				addPropertyRules();
			}
		} else if (isAllIncluded == true) {
			addMatchRule(rules, index, useWhereClause);
			addMapRules(rules, index, useWhereClause);
			addAddRemoveRules(rules, index, useWhereClause);
//			addPropertyRules();
		} else {
			if (type.equals("asr") == true) {
				addMatchRule(rules, index, useWhereClause);
			} else if (type.equals("hybrid") == true) {
				addMatchRule(rules, index, useWhereClause);
				addMapRules(rules, index, useWhereClause);
			}
		}
	}

	private static void addPropertyRules() {
		// TODO Auto-generated method stub
		DatalogClause c = new DatalogClause();
		Atom head;
		Atom b;
		
		if (availableRels.contains("MAP") == true) {
			head = new Atom(NP1);
			head.appendTerm(new Term("dst", true));
			head.appendTerm(new Term("key", true));
			head.appendTerm(new Term("value", true));
			c.addAtomToHeads(head);
			b = new Atom(NP0);
			b.appendTerm(new Term("src", true));
			b.appendTerm(new Term("key", true));
			b.appendTerm(new Term("value", true));
			c.addAtomToBody(b);
			b = new Atom(MAP);
			b.appendTerm(new Term("src", true));
			b.appendTerm(new Term("_", true));
			b.appendTerm(new Term("dst", true));
			b.appendTerm(new Term("_", true));
			c.addAtomToBody(b);
			program.addRule(c);
		}

		c = new DatalogClause();
		head = new Atom(NP1);
		head.appendTerm(new Term("n", true));
		head.appendTerm(new Term("key", true));
		head.appendTerm(new Term("value", true));
		c.addAtomToHeads(head);
		b = new Atom(NP0);
		b.appendTerm(new Term("n", true));
		b.appendTerm(new Term("key", true));
		b.appendTerm(new Term("value", true));
		c.addAtomToBody(b);
		if (availableRels.contains("NDD") == true) {
			b = new Atom(NDD);
			b.setNegated(true);
			b.appendTerm(new Term("n", true));
			c.addAtomToBody(b);
		}
		program.addRule(c);
		
		availableRels.add("NP1");
		availableRels.add("EP1");
	}

	/**
	 * @param p
	 * @param rules
	 * @param isAllIncluded True if all rules should be included (e.g., for rewriting)
	 * @param indexOfRule
	 * @param useWhereClause
	 */
	public static void addViewRuleToProgram(DatalogProgram p, TransRuleList rules, 
			boolean isAllIncluded, int indexOfRule, boolean useWhereClause) {
//		System.out.println("[addViewRuleToProgram] type: " + rules.getViewType() + " isAllInc: " + isAllIncluded);

		rid = 0;
		program = p;
		initialize(rules.getBaseName(), rules.getViewName());

		if (indexOfRule >= 0) { // for single rule only
			addViewRulesForSingleRule(rules, isAllIncluded, indexOfRule, useWhereClause);
		} else { // for all rules
			for (int i = 0; i < rules.getTransRuleList().size(); i++) {
				addViewRulesForSingleRule(rules, isAllIncluded, i, useWhereClause);
			}
		}

		if (rules.isDefaultMap() == true) {
			if (rules.getViewType().equals("materialized") == true) {
				addViewRules();
			} else {
				if ((isAllIncluded == true && rules.getViewType().contentEquals("materialized") == false) ||
						(isAllIncluded == false && rules.getViewType().contentEquals("materialized") == true)) {
					addViewRules();
				}		
			}
		}
	}
}

