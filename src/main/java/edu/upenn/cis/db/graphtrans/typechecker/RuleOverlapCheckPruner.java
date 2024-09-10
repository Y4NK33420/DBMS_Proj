package edu.upenn.cis.db.graphtrans.typechecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.logicblox.connect.BloxCommand.Relation;
import com.logicblox.connect.ConnectBlox.Response;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogParser;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.datalog.simpleengine.IntegerSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleDatalogEngine;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Tuple;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.Config.PATTERN_TYPE;
import edu.upenn.cis.db.graphtrans.Config.RULE_TYPE;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.catalog.Schema;
import edu.upenn.cis.db.graphtrans.catalog.SchemaEdge;
import edu.upenn.cis.db.graphtrans.catalog.SchemaNode;
import edu.upenn.cis.db.graphtrans.datastructure.Egd;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.helper.Util;
import edu.upenn.cis.db.logicblox.LogicBlox;

/**
 * Rule pruning class for transrules with graph schemas and EGDs.
 * 
 * Our goal is to find
 * 1. a set of EGDs interacting with each rule, and  
 * 2. all pairs of rules that interact with each other
 * @author sbnet21
 *
 */
public class RuleOverlapCheckPruner {
	final static Logger logger = LogManager.getLogger(RuleOverlapCheckPruner.class);

	private static HashMap<Pair<Integer, Integer>, HashSet<Integer>> ruleEgdsMap;
	private static ArrayList<Triple<Integer,Integer,Integer>> rulePairsList;
	
	private static String viewName;
	private static boolean printLog = false;
	
	private static SimpleDatalogEngine engine;
	
	/**
	 * Color the schema graph with the given pattern.  
	 * @param atomList pattern given as a list of atoms
	 * @param type rule type
	 * @param subtype pattern type (none/before, affected, after)
	 * @param id rule id
	 * @return logic string
	 */
	private static String getLogicForColoring(ArrayList<Atom> atomList, RULE_TYPE type, PATTERN_TYPE subtype, int id) {
		ArrayList<Atom> atomListNoProp = new ArrayList<Atom>(); // Without relations for property
		String bodyStr = "";
		for (int i = 0; i < atomList.size(); i++) {
			if (atomList.get(i).getPredicate().equals(Config.predN)) {
				atomListNoProp.add(atomList.get(i));
				if (bodyStr.contentEquals("") == false) {
					bodyStr += ", ";
				}
				bodyStr += Config.relname_node_schema + "(" + atomList.get(i).getAtomBodyStr() + ")";
			} else if (atomList.get(i).getPredicate().equals(Config.predE)) {
				atomListNoProp.add(atomList.get(i));
				if (bodyStr.contentEquals("") == false) {
					bodyStr += ", ";
				}
				bodyStr += Config.relname_edge_schema + "(" + atomList.get(i).getAtomBodyStr() + ")";
			}
		}

		String headStr = "";
		for (int i = 0; i < atomListNoProp.size(); i++) {
			if (atomList.get(i).getPredicate().equals(Config.predN)) {
				if (headStr.contentEquals("") == false) {
					headStr += ", "; 
				}
				headStr += Config.relname_coloring + "(\"" + type + "\", " + subtype + ", " + id + ", \"n\", "+ atomListNoProp.get(i).getTerms().get(0) + ")";
			} else if (atomList.get(i).getPredicate().equals(Config.predE)) {
				if (headStr.contentEquals("") == false) {
					headStr += ", "; 
				}
				headStr += Config.relname_coloring + "(\"" + type + "\", " + subtype + ", " + id + ", \"e\", " + atomListNoProp.get(i).getTerms().get(0) + ")";
			}
		}
		return headStr + " <- " + bodyStr + ".\n";
	}

	/**
	 * Color the schema graph with the pattern (LHS) of EGDs.
	 */
	private static void coloringGraphSchemaWithEgds() {
		int tid = Util.startTimer();
		ArrayList<String> rules = new ArrayList<String>();
		if (GraphTransServer.getEgdList().size() == 0) {
			return;
		}
		
		StringBuilder logic = new StringBuilder();
		for (int i = 0; i < GraphTransServer.getEgdList().size(); i++) {
			Egd egd = GraphTransServer.getEgdList().get(i);
			String rule = getLogicForColoring(egd.getLhs(), RULE_TYPE.EGD, PATTERN_TYPE.NONE, i);
			rules.add(rule);
			logic.append(rule);			
		}
//		String blk_name = "color_egd";
//		LogicBlox.runRemoveBlock(Config.getWorkspace(), blk_name);
////		int tc = Util.startTimer();
//		LogicBlox.runAddBlock(Config.getWorkspace(), blk_name, logic.toString());
////		System.out.println("[ELAPSED TIME] tc: " + Util.getElapsedTime(tc));
		
//		System.out.println("timeB-0-1: " + Util.getElapsedTime(tid));
		if (printLog == true) 
			System.out.println("[RulePruner] coloringGraphSchemaWithEgds " + logic);

		for (String rule : rules) {
//			System.out.println("timeB-0-2: " + Util.getElapsedTime(tid));

			DatalogProgram program = new DatalogProgram();
			DatalogParser parser = new DatalogParser(program);
			DatalogClause c = parser.ParseQuery(rule);
//			System.out.println("timeB-0-3: " + Util.getElapsedTime(tid));
			
//			Util.getElapsedTime(tid);
//			System.out.println("c: " + c);
			edu.upenn.cis.db.datalog.simpleengine.Relation result = engine.executeQuery(c);
//			System.out.println("timeB-0-4: " + Util.getElapsedTime(tid));
		}
//		System.out.println("timeB-0-5: " + Util.getElapsedTime(tid));
//		System.out.println("[TCTC] engine: " + engine);
	
	
	}

	/**
	 * Color the schema graph with the pattern of transrules. 
	 */
	private static void coloringGraphSchemaWithRules() {
		int tid = Util.startTimer();
		
		ArrayList<String> rules = new ArrayList<String>();
		StringBuilder logic = new StringBuilder();
		for (int i = 0; i < GraphTransServer.getTransRuleList(0).getNumTransRuleList(); i++) {
			TransRule tr = GraphTransServer.getTransRuleList(0).getTransRule(i);
			rules.add(getLogicForColoring(tr.getPatternBefore(), RULE_TYPE.TRANSRULE, PATTERN_TYPE.BEFORE, i));
			rules.add(getLogicForColoring(tr.getPatternAffected(), RULE_TYPE.TRANSRULE, PATTERN_TYPE.AFFECTED, i));
			rules.add(getLogicForColoring(tr.getPatternAfter(), RULE_TYPE.TRANSRULE, PATTERN_TYPE.AFTER, i));
			
			for (String rule : rules) {
				logic.append(rule);
			}
		}
//		String blk_name = "color_rule";
//		LogicBlox.runRemoveBlock(Config.getWorkspace(), blk_name);
//
//
////		int tc = Util.startTimer();
//		LogicBlox.runAddBlock(Config.getWorkspace(), blk_name, logic.toString());
////		System.out.println("[ELAPSED TIME] tc2: " + Util.getElapsedTime(tc));
//		System.out.println("timeB-1-1: " + Util.getElapsedTime(tid));

		if (printLog == true) 
			System.out.println("[RulePruner] coloringGraphSchemaWithRules " + logic);

		for (String rule : rules) {
			DatalogProgram program = new DatalogProgram();
			DatalogParser parser = new DatalogParser(program);
			DatalogClause c = parser.ParseQuery(rule);
			
			edu.upenn.cis.db.datalog.simpleengine.Relation result = engine.executeQuery(c);
		}

//		System.out.println("timeB-1-2: " + Util.getElapsedTime(tid));

//		System.out.println("[TTSS] engine: " + engine);
	}    

	/**
	 * Populate the list of relevant transrule and EGD pairs. 
	 * Represented by a relation (ruleId, ruleSubType, egdId). 
	 */
	private static void populateRelatedEgdsOfRule() {
		int tid = Util.startTimer();
		coloringGraphSchemaWithEgds();
//		System.out.println("timeB-1: " + Util.getElapsedTime(tid));
		coloringGraphSchemaWithRules();
//		System.out.println("timeB-2: " + Util.getElapsedTime(tid));
		
//		System.out.println(engine.getRelation("COLOR"));
//		System.exit(0);

		String query = "_(egd_id, rule_id, rule_subtype) <- " +
				Config.relname_coloring + "(\"" + Config.RULE_TYPE.EGD + "\",0,egd_id,c,d), " +
				Config.relname_coloring + "(\"" + Config.RULE_TYPE.TRANSRULE + "\",rule_subtype,rule_id,c,d).";

//		Response res = LogicBlox.runExecBlock(Config.getWorkspace(), query, true);
//		Relation rel = res.getTransaction().getCommand(0).getExec().getReturnLocal(0);

		if (printLog == true) 
			System.out.println("[RulePruner] populateRelatedEgdsOfRule " + query);

		
//		System.out.println("[populateRelatedEgdsOfRule] engine: " + engine);
		DatalogProgram program = new DatalogProgram();
		DatalogParser parser = new DatalogParser(program);		
		DatalogClause c = parser.ParseQuery(query);
//		System.out.println("timeB-3: " + Util.getElapsedTime(tid));

//		System.out.println("c: " + c);
		edu.upenn.cis.db.datalog.simpleengine.Relation<SimpleTerm> result = engine.executeQuery(c);
//		System.out.println("timeB-4: " + Util.getElapsedTime(tid));
		
		for (Tuple<SimpleTerm> t : result.getTuples()) {
//			Tuple<SimpleTerm> t = result.getTuples().get(i);
			
			int v1 = t.getTuple().get(0).getInt(); // egd_id
			int v2 = t.getTuple().get(1).getInt(); // rule_id
			int v3 = t.getTuple().get(2).getInt(); // rule_subtype
			
			if (ruleEgdsMap.containsKey(Pair.of(v2,v3)) == false) {
				ruleEgdsMap.put(Pair.of(v2, v3), new HashSet<Integer>());
			}
			ruleEgdsMap.get(Pair.of(v2,v3)).add(v1);
		}
		
//		System.out.println("timeB-5: " + Util.getElapsedTime(tid));
//		System.out.println("[populateRelatedEgdsOfRule] result: " + result);
			
//		int rows = 0;
//		if (rel.getColumnCount() > 0) {
//			rows = rel.getColumn(0).getInt64Column().getValuesCount();
//		}
//
//		for (int i = 0; i < rows; i++) {
//			int v1 = (int)rel.getColumn(0).getInt64Column().getValues(i); // egd_id
//			int v2 = (int)rel.getColumn(1).getInt64Column().getValues(i); // rule_id
//			int v3 = (int)rel.getColumn(2).getInt64Column().getValues(i); // rule_subtype
//			
//			if (ruleEgdsMap.containsKey(Pair.of(v2,v3)) == false) {
//				ruleEgdsMap.put(Pair.of(v2, v3), new HashSet<Integer>());
//			}
//			ruleEgdsMap.get(Pair.of(v2,v3)).add(v1);
//		}
//		System.out.println("rel: " + rel);
//		System.out.println("ruleEgdsMap: " + ruleEgdsMap);
	}

	/**
	 * Populate the list of relevant transrule pairs.
	 */
	private static void populateRelatedRulePairs() {
		int tid = Util.startTimer();
		String query = "_(rule_id1, rule_id2, rule_subtype) <- " +
				Config.relname_coloring + "(\""+Config.RULE_TYPE.TRANSRULE + "\",0,rule_id1,c,d), " +
				Config.relname_coloring + "(\""+Config.RULE_TYPE.TRANSRULE + "\",rule_subtype,rule_id2,c,d), " +
				"rule_id1 != rule_id2, rule_subtype > 0.";

		if (printLog == true) 
			System.out.println("[RulePruner] populateRelatedRulePairs " + query);
		
//		System.out.println("timeC-1: " + Util.getElapsedTime(tid));

//		Response res = LogicBlox.runExecBlock(Config.getWorkspace(), query, true);
//		Relation rel = res.getTransaction().getCommand(0).getExec().getReturnLocal(0);

		DatalogProgram program = new DatalogProgram();
		DatalogParser parser = new DatalogParser(program);		
		DatalogClause c = parser.ParseQuery(query);
//		System.out.println("[c]: " + c);
		edu.upenn.cis.db.datalog.simpleengine.Relation<SimpleTerm> result = engine.executeQuery(c);
		
//		System.out.println("[prune] result: " + result);

//		System.out.println("timeC-2: " + Util.getElapsedTime(tid));

		for (Tuple<SimpleTerm> t : result.getTuples()) {
//			Tuple<SimpleTerm> t = result.getTuples().get(i);
//			
			int v1 = t.getTuple().get(0).getInt(); // egd_id
			int v2 = t.getTuple().get(1).getInt(); // rule_id
			int v3 = t.getTuple().get(2).getInt(); // rule_subtype

			rulePairsList.add(Triple.of(v1, v2, v3)); // rule(v1,0) vs. rule(v2,v3)
		}
//		System.out.println("timeC-3: " + Util.getElapsedTime(tid));

//		System.out.println("rulePairsList: " + rulePairsList);
		
//		int rows = rel.getColumn(0).getInt64Column().getValuesCount();
//		for (int i = 0; i < rows; i++) {
//			int v1 = (int)rel.getColumn(0).getInt64Column().getValues(i);
//			int v2 = (int)rel.getColumn(1).getInt64Column().getValues(i);
//			int v3 = (int)rel.getColumn(2).getInt64Column().getValues(i);
//
//			rulePairsList.add(Triple.of(v1, v2, v3)); // rule(v1,0) vs. rule(v2,v3)
//		}
	}

	private static void loadCatalog() {
		// TODO Auto-generated method stub
		int id = 1; 
		HashMap<String, Integer> nodeToId = new HashMap<String, Integer>();
		
		for (SchemaNode s : Schema.getSchemaNodes()) {
			nodeToId.put(s.getLabel(), id);
			engine.insertTuple("N_schema", new Tuple<SimpleTerm>(Arrays.asList(
					new LongSimpleTerm(id++), new StringSimpleTerm(s.getLabel()))));
		}
		for (SchemaEdge s : Schema.getSchemaEdges()) {
			int fromId = nodeToId.get(s.getFrom());
			int toId = nodeToId.get(s.getTo());
			engine.insertTuple("E_schema", new Tuple<SimpleTerm>(Arrays.asList(
					new LongSimpleTerm(id++), new LongSimpleTerm(fromId)
					, new LongSimpleTerm(toId), new StringSimpleTerm(s.getLabel()))));
		}
	}

	/**
	 * Prune the set of transrules and EGDs to be used to check well-behavedness.
	 * @return true if no interference, false, otherwise.
	 */
	public static void prune(String name, 
			ArrayList<Triple<Integer, Integer, Integer>> rulePairs,
			HashMap<Pair<Integer, Integer>, HashSet<Integer>> ruleEgds) {
		viewName = name;
		rulePairsList = rulePairs;
		ruleEgdsMap = ruleEgds;
		
		engine = new SimpleDatalogEngine<SimpleTerm>();

		int tid = Util.startTimer();
		loadCatalog();
//		System.out.println("timeA: " + Util.getElapsedTime(tid));
		populateRelatedEgdsOfRule();
//		System.out.println("timeB: " + Util.getElapsedTime(tid));
		populateRelatedRulePairs();
//		System.out.println("timeC: " + Util.getElapsedTime(tid));
		
	}
}
