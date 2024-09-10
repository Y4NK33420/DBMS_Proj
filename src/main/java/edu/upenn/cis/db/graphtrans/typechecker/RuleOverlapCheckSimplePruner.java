package edu.upenn.cis.db.graphtrans.typechecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

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
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
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
public class RuleOverlapCheckSimplePruner {
	final static Logger logger = LogManager.getLogger(RuleOverlapCheckSimplePruner.class);

	private static HashMap<String, Integer> nodeLabelDicEncoding = new HashMap<>();
	private static int nobelLabelDicEncodingIndex = 0;
	private static HashMap<Integer, Long> encodedTransrule = new HashMap<>();
	private static ArrayList<Long> encodedEgds = new ArrayList<Long>();

	private static HashMap<Integer, HashSet<Integer>> ruleEgdsMap = new HashMap<>();
	private static ArrayList<Pair<Integer,Integer>> newRulePairsList = new ArrayList<Pair<Integer,Integer>>();

	private static String viewName;

	private static long getEncodedGraph(ArrayList<Atom> atoms) {
		long encodedGraph = 0;

		for (int j = 0; j < atoms.size(); j++) {
//			System.out.println("atoms: " + atoms);
			if (atoms.get(j).getPredicate().equals(Config.predN)) {
				if (atoms.get(j).getTerms().get(1).isConstant() == true) {
					String label = Util.removeQuotes(atoms.get(j).getTerms().get(1).toString());
					if (nodeLabelDicEncoding.containsKey(label) == true) {
						int index = nodeLabelDicEncoding.get(label);
						encodedGraph = encodedGraph | (1 << index);
//						System.out.println("add [" + label + "] w/ index[" + index + "] encodedGraph[" + encodedGraph + "]");
					} else {
						System.out.println("atoms: " + atoms);
						System.out.println("nodeLabelDicEncoding: " + nodeLabelDicEncoding);
						throw new IllegalArgumentException("Node label[" + label + "]");
					}
				} else {
					encodedGraph = -1L;
					break;
				}
			}
		}
		return encodedGraph;
	}

	/**
	 * Color the schema graph with the pattern (LHS) of EGDs.
	 */
	private static void coloringGraphSchemaWithEgds() {
		for (int i = 0; i < GraphTransServer.getEgdList().size(); i++) {
			Egd egd = GraphTransServer.getEgdList().get(i);

			encodedEgds.add(getEncodedGraph(egd.getLhs()));
		}
//		System.out.println("encodedEgds: " + encodedEgds);
	}

	/**
	 * Color the schema graph with the pattern of transrules. 
	 */
	private static void coloringGraphSchemaWithRules() {
		ArrayList<String> rules = new ArrayList<String>();
		StringBuilder logic = new StringBuilder();
		
		TransRuleList transrules = GraphTransServer.getTransRuleList(viewName);
		for (int i = 0; i < transrules.getNumTransRuleList(); i++) {
			TransRule tr = transrules.getTransRule(i);
			encodedTransrule.put(i,  getEncodedGraph(tr.getPatternBefore()));
//			encodedTransrule.put(Pair.of(i, PATTERN_TYPE.AFFECTED), getEncodedGraph(tr.getPatternAffected()));
//			encodedTransrule.put(Pair.of(i, PATTERN_TYPE.AFTER), getEncodedGraph(tr.getPatternAfter()));
		}
//		System.out.println("encodedTransrule: " + encodedTransrule);
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

		for (int i = 0; i < encodedEgds.size(); i++) {
			for (Entry<Integer, Long> entry : encodedTransrule.entrySet()) {
				long encodedGraph = entry.getValue();
				if ((encodedEgds.get(i) | encodedGraph) != 0) {
					int key = entry.getKey();
					long value = entry.getValue();

					if (ruleEgdsMap.containsKey(key) == false) {
						ruleEgdsMap.put(key, new HashSet<Integer>());
					}
					ruleEgdsMap.get(key).add(i);
				}
			}
		}
//		System.out.println("ruleEgdsMap: " + ruleEgdsMap);
	}

	/**
	 * Populate the list of relevant transrule pairs.
	 */
	private static void populateRelatedRulePairs() {		
//		System.out.println("encodedTransrule: " + encodedTransrule);
		
		for (Entry<Integer, Long> entry1 : encodedTransrule.entrySet()) {
			for (Entry<Integer, Long> entry2 : encodedTransrule.entrySet()) {
				long encodedGraph1 = entry1.getValue();
				long encodedGraph2 = entry2.getValue();
				
				int id1 = entry1.getKey();
				int id2 = entry2.getKey();
				
				if (id1 < id2) {
					continue;
				}
//				System.out.println("id1: " + id1 + " sub1: " + sub1 + " WITH id2: " + id2 + " sub2: " + sub2);
//				System.out.println(encodedGraph1 + " vs " + encodedGraph2 + " resultBool: " + ((encodedGraph1 & encodedGraph2) != 0));

				if ((encodedGraph1 & encodedGraph2) != 0) {
//					System.out.println("(" + id1 + ",0) vs ("+id2+","+sub2+") encodedGraph1: " + encodedGraph1 + " encodedGraph2: " + encodedGraph2);
					newRulePairsList.add(Pair.of(id1, id2)); // rule(v1,0) vs. rule(v2,v3)
				}
			}
		}
//		System.out.println("newRulePairsList: " + newRulePairsList);
	}

	private static void loadCatalog() {
		// TODO Auto-generated method stub
		nodeLabelDicEncoding = new HashMap<String, Integer>();
		nobelLabelDicEncodingIndex = 0;
		
		for (SchemaNode s : Schema.getSchemaNodes()) {
			if (nodeLabelDicEncoding.containsKey(s.getLabel()) == false) {
				nodeLabelDicEncoding.put(s.getLabel(), nobelLabelDicEncodingIndex++);
			}
		}
	}

	/**
	 * Prune the set of transrules and EGDs to be used to check well-behavedness.
	 * This pruner simply checks whether two graph pairs have nodes with the same label.
	 * If not, they don't need to be compared.
	 * @return true if no interference, false, otherwise.
	 */
	public static void prune(String name, 
			ArrayList<Pair<Integer, Integer>> rulePairs,
			HashMap<Integer, HashSet<Integer>> ruleEgds) {
		viewName = name;
		newRulePairsList = rulePairs;
		ruleEgdsMap = ruleEgds;
		
		encodedTransrule.clear();
		encodedEgds.clear(); 

//		System.out.println("prune name: " + name + " encodedTransrule: " + encodedTransrule);
		//		engine = new SimpleDatalogEngine<SimpleTerm>();

		int tid = Util.startTimer();
		loadCatalog();
//		System.out.println("nodeLabelDicEncoding: " + nodeLabelDicEncoding);
//				System.out.println("timeA: " + Util.getElapsedTime(tid));
		populateRelatedEgdsOfRule();
//				System.out.println("timeB: " + Util.getElapsedTime(tid));
		populateRelatedRulePairs();
//				System.out.println("timeC: " + Util.getElapsedTime(tid));

		System.out.println("[RuleOverCheckSimplePruner] pruned newRulePairsList count: " + newRulePairsList.size());

	}
}
