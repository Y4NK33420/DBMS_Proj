package edu.upenn.cis.db.datalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.ConjunctiveQuery.Type;
import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.graphdb.datalog.BaseRuleGen;
import edu.upenn.cis.db.graphtrans.graphdb.datalog.ViewRule;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.helper.Util;

/**
 * @author sbnet21
 *
 * Create SSR and rules 
 */
public class SSR {
	private static ArrayList<DatalogClause> creationRules;
	private static ArrayList<DatalogClause> rewritingRules;
	
	private static HashMap<String, Integer> varToIndex;
	private static HashMap<Integer, String> indexToVar;
	private static int currentIndexForHashMap;
	
	private static String workspace = "ssr";
	
	private static HashMap<Integer, String> idToVarMap;
	
	private static Store store = null;
	private static final int START_INDEX = 200000;
	
	private static int rid = 0;
	
	private static int getIndexForVar(String var) {
		if (varToIndex.containsKey(var) == false) {
			varToIndex.put(var, currentIndexForHashMap);
			indexToVar.put(currentIndexForHashMap, var);
			currentIndexForHashMap++;
		}
		return varToIndex.get(var);
	}
	
	/**
	 * @param tr
	 * 
	 * Create SSR using TransRuleList
	 */
	public static void populateSSRRulesForAll(TransRuleList tr) {
		creationRules = new ArrayList<DatalogClause>();
		rewritingRules = new ArrayList<DatalogClause>();
				
		DatalogClause c = null;
		
		if (tr.isDefaultMap() == false) {
			DatalogClause ssrCreation = new DatalogClause();
			DatalogClause ssrRewriting = new DatalogClause();

			HashSet<String> ssrHeadVars = new LinkedHashSet<String>();
			
			for (Atom a : tr.getTransRuleList().get(0).getPatternMatch()) {
				Atom b = null;
				Atom b2 = null;
				try {
					b = (Atom)a.clone();
					b2 = (Atom)a.clone();
					if (b.isInterpreted() == false) {
						b.getPredicate().setRelName(b.getRelName() + "_" + tr.getBaseName());
						b2.getPredicate().setRelName(b2.getRelName() + "_" + tr.getViewName());
					}
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ssrHeadVars.addAll(b.getVars());
				ssrCreation.addAtomToBody(b);
				ssrRewriting.addAtomToBody(b2);
			}
			Atom ssrHeadAtom = new Atom("INDEX_" + tr.getViewName());
			for (String v : ssrHeadVars) {
				ssrHeadAtom.appendTerm(new Term(v, true));
			}
			ssrCreation.addAtomToHeads(ssrHeadAtom);
			ssrRewriting.addAtomToHeads(ssrHeadAtom);
			
			System.out.println("ssrCreation: " + ssrCreation);
			System.out.println("ssrRewriting: " + ssrRewriting);

			creationRules.add(ssrCreation);
			rewritingRules.add(ssrRewriting);
//			throw new NotImplementedException("SSR for non Default Rules");
			/*
			 	creation rule => INDEX(...) <- pattern query
			 	rewriting rule => pattern query => INDEX(...)
			 */
//			System.out.println("[populateSSRRulesForAll] tr.size(): " + tr.getTransRuleList().size() + " i: " + i + " rule : " + rule);
		} else {
			for (int i = 0; i < tr.getTransRuleList().size(); i++) {
				TransRule rule = tr.getTransRuleList().get(i);

				for (String v : rule.getNewNodeVars()) {
	//				System.out.println("newNode: " + v);
					createSSRForNewVariable(tr, i, v, true);
				}
				
				for (String v :rule.getNewEdgeVars()) {
	//				System.out.println("newEdge: " + v);
					createSSRForNewVariable(tr, i, v, false);
				}
			}
		}
	}

	public static void populateSSRRulesForAll_old(TransRuleList tr) {
		rid = 0;
		creationRules = new ArrayList<DatalogClause>();
		rewritingRules = new ArrayList<DatalogClause>();
		idToVarMap = new HashMap<Integer, String>();
		
//		System.out.println("[SSR] populateSSRRulesForAll tr: " + tr.getTransRuleList().size());
		
		for (int i = 0; i < tr.getTransRuleList().size(); i++) {
			SSRHelper.init();
			store = GraphTransServer.getBaseStore();
//			if (store.listDatabases().contains(workspace) == false) {
				store.createDatabase(workspace);
				store.useDatabase(workspace);
//			}

			// 1. Populate Schema Graph (used for coveredness test)
			SSRHelper.populateSchemaGraph();
//			store.debug();
//			System.out.println("74 store: " + store.getListRelationStr(workspace));

			
			// 2. Populate SSR rules (creation, rewriting)
			currentIndexForHashMap = START_INDEX;
			indexToVar = new HashMap<Integer, String>();
			varToIndex = new HashMap<String, Integer>();
			
			ArrayList<Atom> interpretedAtomsToBeAdded = new ArrayList<Atom>();			
			populateMatchGraph(tr, i, interpretedAtomsToBeAdded);
			populateSSRRules(tr, i, interpretedAtomsToBeAdded);


			store.deleteDatabase(workspace);
			
		}
	}
	
	private static void populateMatchGraph(TransRuleList tr, int index, ArrayList<Atom> interpretedAtomsToBeAdded) {
		String baseName = tr.getBaseName();
	
		// 1. Create Schema in DB
		ArrayList<Predicate> preds = BaseRuleGen.getBaseGraphRuleBaseEDB(false);

//		System.out.println("preds: " + preds);
		for (Predicate pred : preds) {
			store.createSchema(workspace, pred);
		}
		Predicate pred = new Predicate(Config.relname_mapping + Config.relname_base_postfix);
		pred.setArgNames("from", "from_l", "to", "to_l");
		pred.setTypes(Type.Integer, Type.String, Type.Integer, Type.String);
		store.createSchema(workspace, pred);
		
//		System.out.println("store: " + store.getListRelationStr(workspace));
	
		// 2. Populate Match Graph
		TransRule rule = tr.getTransRuleList().get(index);
//		System.out.println("rule: " + rule.getPatternMatch());
		ArrayList<Atom> match = rule.getPatternMatch();
		
		for (Atom a : match) {
			if (a.getPredicate().getRelName().contentEquals(Config.relname_nodeprop) == true ||
				a.getPredicate().getRelName().contentEquals(Config.relname_edgeprop) == true) {
				continue;
			}
			ArrayList<SimpleTerm> t = new ArrayList<SimpleTerm>();
			if (a.getPredicate().isInterpreted() == false) {
				for (int k = 0; k < a.getTerms().size(); k++) {
					Term t1 = a.getTerms().get(k);
					if (t1.isVariable() == true) {
						int idx = getIndexForVar(t1.getVar());
						t.add(new LongSimpleTerm(idx));
						idToVarMap.put(idx, t1.getVar());
					} else {
						t.add(new StringSimpleTerm(Util.removeQuotes(t1.toString())));
					}
				}
				String rel = a.getRelName() + "_" + baseName;
				store.addTuple(rel, t);
			} else if (a.getPredicate().isInterpreted() == true) {
				if (rule.getVarsInWhereClause().contains(a.getTerms().get(0).getVar()) == false) {
					interpretedAtomsToBeAdded.add(a);
				}
			}
		}

		// 3. Run transformation on match graph
		DatalogProgram p = new DatalogProgram();
//		ViewRule.addViewRuleToProgram(p, tr, true, false);
		ViewRule.addViewRuleToProgram(p, tr, true, index, false);
//		System.out.println("145 ppp: " + p);
		store.createView(p, tr);
	}
	
	private static String getVarByIdOrLabel(TransRule tr, int varId, String label) {
		String var = null;
		if (idToVarMap.containsKey(varId) == true) {
			var = idToVarMap.get(varId);
		} else {
			if (tr.getMetaLabelToVarMap().containsKey(label) == true) {
				var = tr.getMetaLabelToVarMap().get(label);
				idToVarMap.put(varId, tr.getMetaLabelToVarMap().get(label));
			} else {
				throw new IllegalArgumentException("varId: " + varId + " label: " + label + " idToVarMap: " + idToVarMap + " tr.getMetaLabelToVarMap(): " + tr.getMetaLabelToVarMap());
			}
		}
		return var;
	}
	
	private static boolean isDuplicatedCandidateSubgraph(ArrayList<Atom> as1, ArrayList<Atom> as2) {
		HashSet<String> vars1 = new LinkedHashSet<String>();
		HashSet<String> vars2 = new LinkedHashSet<String>();
		
//		System.out.println("as1: " + as1);
		for (Atom a : as1) {
//			System.out.println("a from as1: " + a);
			vars1.add(a.getTerms().get(0).getVar());
		}
		for (Atom a : as2) {
			vars2.add(a.getTerms().get(0).getVar());
		}
		boolean isDuplicated = true;
		if (vars1.size() == vars2.size()) {
			for (String v : vars1) {
				if (vars2.contains(v) == false) {
					isDuplicated = false;
					break;
				}
			}
		} else {
			isDuplicated = false;
		}
		return isDuplicated;
	}
	
	private static void createSSRForNewVariable(TransRuleList rules, int index, String newVar, boolean isNode) {
//		System.out.println("[createSSRForNewNode] index: " + index + " newNodeVar: " + newVar  + " isNode: " + isNode);
		TransRule tr = rules.getTransRuleList().get(index);
		HashSet<String> headVarsSSR = new LinkedHashSet<String>();
		
		String skolemName = null;
		for (int i = 0; i < tr.getSkolemFunctionMap().get(newVar).size(); i++) {
			String v2 = tr.getSkolemFunctionMap().get(newVar).get(i);
			if (i == 0) {
				skolemName = Util.removeQuotes(v2);
			}
		}
		String headRelSSR = "INDEX_" + rules.getViewName() + "_" + index;

		// #. Create a rule for creating SSR
		Atom atomForConst = null;
		if (Config.isLogicBlox() == true) {
			DatalogClause constRule = new DatalogClause();
			DatalogClause creationRule = new DatalogClause();
			
			for (Atom a : tr.getPatternMatch()) {
				Atom b = null;
				try {
					b = (Atom)a.clone();
					if (b.isInterpreted() == false) {
						b.getPredicate().setRelName(b.getRelName() + "_" + rules.getBaseName());
					}
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				constRule.addAtomToBody(b);
			}
			atomForConst = new Atom(headRelSSR + "_CONST");

			for (int i = 0; i < tr.getSkolemFunctionMap().get(newVar).size(); i++) {
				String v2 = tr.getSkolemFunctionMap().get(newVar).get(i);
				if (i > 0) {
					atomForConst.appendTerm(new Term(v2, true));
				}
			}
			creationRule.addAtomToHeads(new Atom(headRelSSR, headVarsSSR));
			
			int rid_current = rid;
//			for (String var : tr.getMetaSet()) {
			rid_current++;
			Atom b = new Atom(Config.relname_gennewid + "_CONST_" + rules.getViewName() + "_" + Util.removeQuotes(skolemName));
			
			for (int i = 0; i < tr.getSkolemFunctionMap().get(newVar).size(); i++) {
				String v2 = tr.getSkolemFunctionMap().get(newVar).get(i);
				if (i > 0) {
					b.appendTerm(new Term(v2, true));					
				}
			}
			b.appendTerm(new Term(newVar, true));
			constRule.addAtomToHeads(b);

			b = new Atom(Config.relname_gennewid + "_" + rules.getViewName());
			b.appendTerm(new Term(newVar, true));
			constRule.addAtomToHeads(b);
//			}
			constRule.addAtomToHeads(atomForConst);
//			System.out.println("***constRule: " + constRule);

			creationRules.add(constRule);
		} 
		
		DatalogClause creationRule = new DatalogClause();
		
		if (atomForConst == null) {
			for (Atom a : tr.getPatternMatch()) {
				Atom b = null;
				try {
					b = (Atom)a.clone();
					if (b.isInterpreted() == false) {
						b.getPredicate().setRelName(b.getRelName() + "_" + rules.getBaseName());
					}
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				creationRule.addAtomToBody(b);
			}
		} else {
			creationRule.addAtomToBody(atomForConst);
		}
		
		creationRule.addAtomToHeads(new Atom(headRelSSR, headVarsSSR));
		Atom b = new Atom(Config.relname_gennewid + "_MAP_" + rules.getViewName() + "_" + Util.removeQuotes(skolemName));
		for (int i = 0; i < tr.getSkolemFunctionMap().get(newVar).size(); i++) {
			String v2 = tr.getSkolemFunctionMap().get(newVar).get(i);
			if (i > 0) {
				b.appendTerm(new Term(v2, true));					
			}
		}	
		b.appendTerm(new Term(newVar, true));
		creationRule.addAtomToBody(b);
		creationRule.getHeads().get(0).appendTerm(new Term(newVar, true));
		headVarsSSR.add(newVar);
//		System.out.println("**** creationRule: " + creationRule);
		
		creationRules.add(creationRule);	

		// #. Create a rule for rewriting
		DatalogClause c = new DatalogClause();

		HashSet<String> endpoints = new LinkedHashSet<String>();
		
		for (Atom b2 : tr.getPatternConstruct()) {
			if (b2.getTerms().get(0).getVar().contentEquals(newVar) == true) {
				if (b2.getTerms().size() == 4) {
					endpoints.add(b2.getTerms().get(1).getVar());
					endpoints.add(b2.getTerms().get(2).getVar());
				}
				Atom b3 = null;
				try {
					b3 = (Atom)b2.clone();
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				b3.getPredicate().setRelName(b3.getRelName() + "_" + rules.getViewName());
				c.addAtomToBody(b3);
				c.addAtomToHeads(creationRule.getHead());
			}
		}
		
//		System.out.println("2471982 endpoints: " + endpoints);
		for (String endpoint : endpoints) {
			for (Atom b2 : tr.getPatternConstruct()) {
				if (b2.getTerms().get(0).getVar().contentEquals(endpoint) == true) {
					Atom b3 = null;
					try {
						b3 = (Atom)b2.clone();
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					b3.getPredicate().setRelName(b3.getRelName() + "_" + rules.getViewName());
					c.addAtomToBody(b3);
					creationRule.getHeads().get(0).getTerms().add(new Term(endpoint, true));
//					c.addAtomToHeads(creationRule.getHead());
				}
			}
		}
		rewritingRules.add(c);

//		System.exit(0);
		
	}
	
	
	
	private static void populateSSRRules(TransRuleList rules, int index, ArrayList<Atom> interpretedAtomsToBeAdded) {
		String headRelSSR = "INDEX_" + rules.getViewName() + "_" + index;

		TransRule tr = rules.getTransRuleList().get(index);
		SSRHelper.populateSchemasByRule(tr, rules.getViewName());
		
		ArrayList<Atom> outputPattern = new ArrayList<Atom>();	
		ArrayList<ArrayList<Atom>> candidateSubgraphs = new ArrayList<ArrayList<Atom>>();
		
		HashSet<String> headVarsSSR = new LinkedHashSet<String>();
		for (int i = 0; i < tr.getPatternMatch().size(); i++) {
			Atom a = tr.getPatternMatch().get(i);
			headVarsSSR.addAll(a.getVars());
		}
		
		// #. Create a rule for creating SSR
		Atom atomForConst = null;
		if (Config.isLogicBlox() == true) {
			DatalogClause constRule = new DatalogClause();
			DatalogClause creationRule = new DatalogClause();
			
			for (Atom a : tr.getPatternMatch()) {
				Atom b = null;
				try {
					b = (Atom)a.clone();
					if (b.isInterpreted() == false) {
						b.getPredicate().setRelName(b.getRelName() + "_" + rules.getBaseName());
					}
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				constRule.addAtomToBody(b);
			}
			atomForConst = new Atom(headRelSSR + "_CONST");
			
			for (String v : headVarsSSR) {
				atomForConst.appendTerm(new Term(v, true));
			}
			creationRule.addAtomToHeads(new Atom(headRelSSR, headVarsSSR));
			
//			int rid = 0;
//			System.out.println("tr.getMetaSet(): " + tr.getMetaSet());
			
			int rid_current = rid;
			HashSet<String> newNodes = new LinkedHashSet<String>();
			newNodes.addAll(tr.getNewEdgeVars());
			newNodes.addAll(tr.getNewNodeVars());
			for (String var : newNodes) {
				rid_current++;
				Atom b = new Atom(Config.relname_gennewid + "_CONST_" + rules.getViewName() + "_" + rid_current);
				for (int i = 1; i < tr.getSkolemFunctionMap().get(var).size(); i++) {
					String v = tr.getSkolemFunctionMap().get(var).get(i);
					b.appendTerm(new Term(v, true));					
				}
				b.appendTerm(new Term(var, true));
				constRule.addAtomToHeads(b);

				b = new Atom(Config.relname_gennewid + "_" + rules.getViewName());
				b.appendTerm(new Term(var, true));
				constRule.addAtomToHeads(b);
			}
			constRule.addAtomToHeads(atomForConst);
			System.out.println("***constRule: " + constRule);

			creationRules.add(constRule);
		} 

		
		DatalogClause creationRule = new DatalogClause();
		
		if (atomForConst == null) {
			for (Atom a : tr.getPatternMatch()) {
				Atom b = null;
				try {
					b = (Atom)a.clone();
					if (b.isInterpreted() == false) {
						b.getPredicate().setRelName(b.getRelName() + "_" + rules.getBaseName());
					}
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				creationRule.addAtomToBody(b);
			}
		} else {
			creationRule.addAtomToBody(atomForConst);
		}
		
		creationRule.addAtomToHeads(new Atom(headRelSSR, headVarsSSR));
//		int rid = 0;
		HashSet<String> newNodes = new LinkedHashSet<String>();
		newNodes.addAll(tr.getNewEdgeVars());
		newNodes.addAll(tr.getNewNodeVars());
		for (String var : newNodes) {
			rid++;
			Atom b = new Atom(Config.relname_gennewid + "_MAP_" + rules.getViewName() + "_" + rid);
			b.appendTerm(new Term(var, true));
			creationRule.addAtomToBody(b);
			creationRule.getHeads().get(0).appendTerm(new Term(var, true));
			headVarsSSR.add(var);
		}
//		System.out.println("creationRule: " + creationRule);
		
		creationRules.add(creationRule);
			
		// #. Populate Nodes in the output pattern
//		store.debug();
		DatalogClause c = new DatalogClause();
		c.setHead(new Atom("_", "n", "l"));
		c.addAtomToBody(new Atom("N_" + rules.getViewName(), "n", "l"));
		
		StoreResultSet rs = store.getQueryResult(c);
		
//		System.out.println("285 rs(N_" + rules.getViewName() + "): " + rs.getResultSet());
		
		int cols = rs.getColumns().size();
		int rows = (cols == 0) ? 0 : rs.getResultSet().size();
		
		Predicate predViewN = new Predicate("N_" + rules.getViewName());
		
		for (int i = 0; i < rows; i++) {
			int varId = (int)rs.getResultSet().get(i).getTuple().get(0).getLong();
			String label = rs.getResultSet().get(i).getTuple().get(1).getString();
			
//			System.out.println("i: " + i + " varId: " + varId + " label: " + label);
			
			String var = getVarByIdOrLabel(tr, varId, label);
			Atom a = new Atom(predViewN);
			a.appendTerm(new Term(var, true));
			a.appendTerm(new Term(Util.addQuotes(label), false));
			outputPattern.add(a);		
		}
		
		// #. Populate Edges in the output pattern
		c = new DatalogClause();
		c.setHead(new Atom("_", "n", "f", "t", "l"));
		c.addAtomToBody(new Atom("E_" + rules.getViewName(), "n", "f", "t", "l"));
		
		rs = store.getQueryResult(c);
//		System.out.println("311 rs(E_" + rules.getViewName() + "): " + rs.getResultSet());
		
		cols = rs.getColumns().size();
		rows = (cols == 0) ? 0 : rs.getResultSet().size();
	
		Predicate predViewE = new Predicate("E_" + rules.getViewName());
		for (int i = 0; i < rows; i++) {
			Atom a = new Atom(predViewE);
			String label = rs.getResultSet().get(i).getTuple().get(3).getString();
			for (int j = 0; j < 3; j++) {
				int varId = (int)rs.getResultSet().get(i).getTuple().get(j).getLong();
				String var = getVarByIdOrLabel(tr, varId, label);
				a.appendTerm(new Term(var, true));
			}
			a.appendTerm(new Term(Util.addQuotes(label), false));
			outputPattern.add(a);		
		}		
//		System.out.println("[getSSRRule] outputPattern: " + outputPattern);
		
		// #. Create a rule for rewriting
		DatalogClause rewritingRule = new DatalogClause();
		
		
		HashSet<String> outputPatternVars = new LinkedHashSet<String>();
		for (Atom a : outputPattern) {
			rewritingRule.addAtomToBody(a);
			outputPatternVars.addAll(a.getVars());
		}
		for (Atom a : interpretedAtomsToBeAdded) {
			rewritingRule.addAtomToBody(a);
		}
		
		ArrayList<String> headVarsRewriting = SSRHelper.getHeadVarsProjected(headVarsSSR, outputPatternVars);
		rewritingRule.addAtomToHeads(new Atom(headRelSSR, headVarsRewriting));

		// # Populate candidate subgraphs (nodes of new labels, incident edges, edge with new labels
		ArrayList<DatalogClause> cs = new ArrayList<DatalogClause>();
		Atom b = null;
		
		boolean existOutN = false;
		boolean existOutE = false;
		
		for (String label : tr.getMetaNodeLabel()) {
			c = new DatalogClause();
			c.addAtomToHeads(new Atom("OUT_N", "n1", "l1"));
			c.addAtomToBody(new Atom ("N_" + rules.getViewName(), "n1", "l1"));
			b = new Atom(Config.predOpEq);
			b.appendTerm(new Term("l1", true));
			b.appendTerm(new Term(Util.addQuotes(label), false));
			c.addAtomToBody(b);
			cs.add(c);

			c = new DatalogClause();
			c.addAtomToHeads(new Atom("OUT_E", "e1", "l", "n1", "l1", "n2", "l2"));
			c.addAtomToBody(new Atom ("N_" + rules.getViewName(), "n1", "l1"));
			c.addAtomToBody(new Atom("N_" + rules.getViewName(), "n2", "l2"));
			c.addAtomToBody(new Atom("E_" + rules.getViewName(), "e1", "n1", "n2", "l"));
			b = new Atom(Config.predOpEq);
			b.appendTerm(new Term("l1", true));
			b.appendTerm(new Term(Util.addQuotes(label), false));
			c.addAtomToBody(b);
			cs.add(c);
			
			c.addAtomToHeads(new Atom("OUT_E", "e1", "l", "n1", "l1", "n2", "l2"));
			c.addAtomToBody(new Atom ("N_" + rules.getViewName(), "n1", "l1"));
			c.addAtomToBody(new Atom("N_" + rules.getViewName(), "n2", "l2"));
			c.addAtomToBody(new Atom("E_" + rules.getViewName(), "e1", "n1", "n2", "l"));
			b = new Atom(Config.predOpEq);
			b.appendTerm(new Term("l2", true));
			b.appendTerm(new Term(Util.addQuotes(label), false));
			c.addAtomToBody(b);
			cs.add(c);
			
			existOutN = true;
			existOutE = true;
		}
		
		for (String label : tr.getMetaEdgeLabel()) {
			c = new DatalogClause();
			c.addAtomToHeads(new Atom("OUT_E", "e1", "l", "n1", "l1", "n2", "l2"));
			c.addAtomToBody(new Atom("N_" + rules.getViewName(), "n1", "l1"));
			c.addAtomToBody(new Atom("N_" + rules.getViewName(), "n2", "l2"));
			c.addAtomToBody(new Atom("E_" + rules.getViewName(), "e1", "n1", "n2", "l"));
			b = new Atom(Config.predOpEq);
			b.appendTerm(new Term("l", true));
			b.appendTerm(new Term(Util.addQuotes(label), false));
			c.addAtomToBody(b);

			cs.add(c);		
			
			existOutE = true;
		}	
		store.createView(workspace, cs, true);
				
		// # Retrieve OUT_N
		
		HashMap<String, Atom> nodeToAtom = new HashMap<String, Atom>();
		if (existOutN == true) {
			c = new DatalogClause();
			c.addAtomToHeads(new Atom("_", "n1", "l1"));
			c.addAtomToBody(new Atom("OUT_N", "n1", "l1"));
			rs = store.getQueryResult(c);
//			System.out.println("413 OUT_N rs: " + rs.getResultSet());

			for (int i = 0; i < rs.getResultSet().size(); i++) {
				ArrayList<Atom> as = new ArrayList<Atom>();
				int varId = (int)rs.getResultSet().get(i).getTuple().get(0).getLong();
				String label = rs.getResultSet().get(i).getTuple().get(1).getString();
	
				String var = getVarByIdOrLabel(tr, varId, label);
				Atom a = new Atom(predViewN);
				a.appendTerm(new Term(var, true));
				a.appendTerm(new Term(Util.addQuotes(label), false));
				
				nodeToAtom.put(var, a);
//				System.out.println("===>add a: " + a);
				as.add(a);
				
				if (isDuplicatedCandidateSubgraph(as, outputPattern) == false) {
					candidateSubgraphs.add(as);
				}
			}
		}
		
		if (existOutE == true) {
			// # Retrieve OUT_E
			c = new DatalogClause();
			c.addAtomToHeads(new Atom("_", "e1", "l", "n1", "l1", "n2", "l2"));
			c.addAtomToBody(new Atom("OUT_E", "e1", "l", "n1", "l1", "n2", "l2"));
			rs = store.getQueryResult(c);
			
//			System.out.println("441 OUT_E rs: " + rs.getResultSet());
			for (int i = 0; i < rs.getResultSet().size(); i++) {
				ArrayList<Atom> as = new ArrayList<Atom>();
				Atom a = new Atom(predViewE);
				String label = rs.getResultSet().get(i).getTuple().get(1).getString();
				HashSet<String> nodeVars = new LinkedHashSet<String>();
				for (int j = 0; j < 3; j++) {
					int varId = (int)rs.getResultSet().get(i).getTuple().get(j*2).getLong();
					String nodeLabel = rs.getResultSet().get(i).getTuple().get(j*2+1).getString();
					
//					System.out.println("i: " + i + " j: " + j + " varId: " + varId + " nodeLabel: " + nodeLabel);
					String var = getVarByIdOrLabel(tr, varId, nodeLabel);
					a.appendTerm(new Term(var, true));
					if (j > 0) {
						nodeVars.add(var);
						if (nodeToAtom.containsKey(var) == false) {
							Atom a2 = new Atom(predViewN);
							a2.appendTerm(new Term(var, true));
							a2.appendTerm(new Term(Util.addQuotes(nodeLabel), false));
							nodeToAtom.put(var, a2);
						}
					}	
				}
				a.appendTerm(new Term(Util.addQuotes(label), false));
	
//				System.out.println("nodeVars: " + nodeVars);
//				System.out.println("nodeToAtom: " + nodeToAtom);
				for (String v : nodeVars) {
					if (nodeToAtom.containsKey(v) == true) {
						as.add(nodeToAtom.get(v));
					}
				}
				as.add(a);
//				System.out.println("487 as: " + as);

				if (isDuplicatedCandidateSubgraph(as, outputPattern) == false) {
					candidateSubgraphs.add(as);
				}
			}
		}
		// #. populate rules for properties 
		/*
		 INDEX_v0_0_NP(nid,key,value)
        	<- NP_g(var, key, value) INDEX_v0_0(var,.....)  if var is node both in input/output pattern (kept node)
        	<- NP_g(src, key, value) INDEX_v0_0(src,...dst,.....), nid=dst if src(IN)->dst(OUT) (merged node by mapping)
		INDEX_v0_0_EP(eid,key,value)
        	<- EP_g(var, key, value) INDEX_v0_0(var,.....), eid=var  if var is edge in input/output pattern
		 */
		
//		System.out.println("outputPatternVars: " + outputPatternVars);

		for(Map.Entry e : tr.getMapMap().entrySet()) {
			Atom a = (Atom)e.getKey();
			String dst = a.getTerms().get(0).getVar();
            HashSet<String> srcSet = (HashSet<String>)e.getValue();
            for(String src : srcSet) {
        		DatalogClause ruleForSSRProp = new DatalogClause();

        		Atom headForSSRProp = new Atom(headRelSSR + "_" + Config.relname_nodeprop);
        		headForSSRProp.appendTerm(new Term(dst, true));
        		headForSSRProp.appendTerm(new Term("key", true));
        		headForSSRProp.appendTerm(new Term("value", true));
        		ruleForSSRProp.addAtomToHeads(headForSSRProp);

            	Atom d = new Atom(new Predicate(Config.relname_nodeprop + "_" + rules.getBaseName()));
            	d.appendTerm(new Term(src, true));
            	d.appendTerm(new Term("key", true));
            	d.appendTerm(new Term("value", true));
            	ruleForSSRProp.addAtomToBody(d);
            	ruleForSSRProp.addAtomToBody(creationRule.getHead());
            	
            	creationRules.add(ruleForSSRProp);
            }
		}
		
		// #. Coveredness Testing
//		System.out.println("index: " + index + " outputPattern: " + outputPattern);
//		System.out.println("index: " + index + " rewritingRule: " + rewritingRule);

		if (isCoveringIndex(outputPattern) == true) {
//			System.out.println("index: " + index + " => true");
			rewritingRules.add(rewritingRule);
		} else {
//			System.out.println("index: " + index + " => false");
		}
		
//		System.out.println("candidateSubgraphs: "  +candidateSubgraphs);
		for (int i = candidateSubgraphs.size() -1; i >= 0; i--) { // make decreasing order on the length
//			System.out.println("candidate: " + candidateSubgraphs.get(i));
			if (isCoveringIndex(candidateSubgraphs.get(i)) == true) {
//				System.out.println("===> true");
				outputPatternVars = new HashSet<String>();
				for (Atom a : candidateSubgraphs.get(i)) {
					outputPatternVars.addAll(a.getVars());
				}
				ArrayList<String> vars = SSRHelper.getHeadVarsProjected(headVarsSSR, outputPatternVars);
				DatalogClause candidate = new DatalogClause();
				candidate.addAtomToHeads(new Atom(headRelSSR, vars));
				candidate.setBody(candidateSubgraphs.get(i));

//				System.out.println("headVarsSSR: " + headVarsSSR + " outputPatternVars: " + outputPatternVars + " vars: " +vars);
				
				rewritingRules.add(candidate);
			} else {
//				System.out.println("===> false");
			}
		}
		
//		System.out.println("populateSSRRules rule idx: " + index + " count: " + rules.getNumTransRuleList());
	}
	
	/**
	 * @return datalog rules to create SSR -- caller should execute these rules 
	 */
	public static ArrayList<DatalogClause> getRulesForCreation() {	
		return creationRules; 
	}

	/**
	 * @return datalog rules to be used query rewriting -- will be used in query rewriting using SSR
	 */
	public static ArrayList<DatalogClause> getRulesForRewriting() {	
		return rewritingRules; 
	}	
	
	/**
	 * @param q1
	 * @return True iif SSR is the output pattern's covering index
	 */
	private static boolean isCoveringIndex(ArrayList<Atom> p) {
		return SSRHelper.testCoverednessOnSchemas(p);
	}	
}