package edu.upenn.cis.db.graphtrans.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.logicblox.connect.BloxCommand.Relation;
import com.logicblox.connect.ConnectBlox.Response;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.Config.PATTERN_TYPE;
import edu.upenn.cis.db.graphtrans.Config.RULE_TYPE;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.Egd;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
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
public class RulePruner {
	final static Logger logger = LogManager.getLogger(RulePruner.class);

	private static HashMap<Pair<Integer, Integer>, HashSet<Integer>> ruleEgdsMap;
	private static ArrayList<Triple<Integer,Integer,Integer>> rulePairsList;
	
	private static String viewName;
	private static boolean printLog = true;
	
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
		if (GraphTransServer.getEgdList().size() == 0) {
			return;
		}
		
		StringBuilder logic = new StringBuilder();
		for (int i = 0; i < GraphTransServer.getEgdList().size(); i++) {
			Egd egd = GraphTransServer.getEgdList().get(i);
			logic.append(getLogicForColoring(egd.getLhs(), RULE_TYPE.EGD, PATTERN_TYPE.NONE, i));
		}
		String blk_name = "color_egd";
		LogicBlox.runRemoveBlock(Config.getWorkspace(), blk_name);
//		int tc = Util.startTimer();
		LogicBlox.runAddBlock(Config.getWorkspace(), blk_name, logic.toString());
//		System.out.println("[ELAPSED TIME] tc: " + Util.getElapsedTime(tc));
		if (printLog == true) 
			System.out.println("[RulePruner] coloringGraphSchemaWithEgds " + logic);
	}

	/**
	 * Color the schema graph with the pattern of transrules. 
	 */
	private static void coloringGraphSchemaWithRules() {
		StringBuilder logic = new StringBuilder();
		for (int i = 0; i < GraphTransServer.getTransRuleList(0).getNumTransRuleList(); i++) {
			TransRule tr = GraphTransServer.getTransRuleList(0).getTransRule(i);
			logic.append(getLogicForColoring(tr.getPatternBefore(), RULE_TYPE.TRANSRULE, PATTERN_TYPE.BEFORE, i));
			logic.append(getLogicForColoring(tr.getPatternAffected(), RULE_TYPE.TRANSRULE, PATTERN_TYPE.AFFECTED, i));
			logic.append(getLogicForColoring(tr.getPatternAfter(), RULE_TYPE.TRANSRULE, PATTERN_TYPE.AFTER, i));
		}
		String blk_name = "color_rule";
		LogicBlox.runRemoveBlock(Config.getWorkspace(), blk_name);


//		int tc = Util.startTimer();
		LogicBlox.runAddBlock(Config.getWorkspace(), blk_name, logic.toString());
//		System.out.println("[ELAPSED TIME] tc2: " + Util.getElapsedTime(tc));

		if (printLog == true) 
			System.out.println("[RulePruner] coloringGraphSchemaWithRules " + logic);
		
	}    

	/**
	 * Populate the list of relevant transrule and EGD pairs. 
	 * Represented by a relation (ruleId, ruleSubType, egdId). 
	 */
	private static void populateRelatedEgdsOfRule() {
		coloringGraphSchemaWithEgds();
		coloringGraphSchemaWithRules();

		String query = "_(egd_id, rule_id, rule_subtype) <- " +
				Config.relname_coloring + "(\"" + Config.RULE_TYPE.EGD + "\",0,egd_id,c,d), " +
				Config.relname_coloring + "(\"" + Config.RULE_TYPE.TRANSRULE + "\",rule_subtype,rule_id,c,d).";

		Response res = LogicBlox.runExecBlock(Config.getWorkspace(), query, true);
		Relation rel = res.getTransaction().getCommand(0).getExec().getReturnLocal(0);

		if (printLog == true) 
			System.out.println("[RulePruner] populateRelatedEgdsOfRule " + query);
		
		int rows = 0;
		if (rel.getColumnCount() > 0) {
			rows = rel.getColumn(0).getInt64Column().getValuesCount();
		}

		for (int i = 0; i < rows; i++) {
			int v1 = (int)rel.getColumn(0).getInt64Column().getValues(i); // egd_id
			int v2 = (int)rel.getColumn(1).getInt64Column().getValues(i); // rule_id
			int v3 = (int)rel.getColumn(2).getInt64Column().getValues(i); // rule_subtype
			
			if (ruleEgdsMap.containsKey(Pair.of(v2,v3)) == false) {
				ruleEgdsMap.put(Pair.of(v2, v3), new HashSet<Integer>());
			}
			ruleEgdsMap.get(Pair.of(v2,v3)).add(v1);
		}
	}

	/**
	 * Populate the list of relevant transrule pairs.
	 */
	private static void populateRelatedRulePairs() {
		String query = "_(rule_id1, rule_id2, rule_subtype) <- " +
				Config.relname_coloring + "(\""+Config.RULE_TYPE.TRANSRULE + "\",0,rule_id1,c,d), " +
				Config.relname_coloring + "(\""+Config.RULE_TYPE.TRANSRULE + "\",rule_subtype,rule_id2,c,d), " +
				"rule_id1 != rule_id2, rule_subtype > 0.";

		if (printLog == true) 
			System.out.println("[RulePruner] populateRelatedRulePairs " + query);
		
		Response res = LogicBlox.runExecBlock(Config.getWorkspace(), query, true);
		Relation rel = res.getTransaction().getCommand(0).getExec().getReturnLocal(0);

		int rows = rel.getColumn(0).getInt64Column().getValuesCount();
		for (int i = 0; i < rows; i++) {
			int v1 = (int)rel.getColumn(0).getInt64Column().getValues(i);
			int v2 = (int)rel.getColumn(1).getInt64Column().getValues(i);
			int v3 = (int)rel.getColumn(2).getInt64Column().getValues(i);

			rulePairsList.add(Triple.of(v1, v2, v3)); // rule(v1,0) vs. rule(v2,v3)
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
		
		populateRelatedEgdsOfRule();
		populateRelatedRulePairs();
	}
}
