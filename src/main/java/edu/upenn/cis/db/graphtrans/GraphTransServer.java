package edu.upenn.cis.db.graphtrans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;

import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.graphtrans.datastructure.Egd;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.Neo4jGraph;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.OverlayViewNeo4jGraph;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.UpdatedViewNeo4jGraph;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.postgres.PostgresStore;
import edu.upenn.cis.db.graphtrans.store.simpledatalog.SimpleDatalogStore;

public class GraphTransServer {
	private static ArrayList<TransRuleList> transRuleListList;
	private static HashMap<String, TransRuleList> transRuleLists;
	private static ArrayList<Egd> egdList = new ArrayList<Egd>();
	private static DatalogProgram program;
	private static HashMap<String, Pair<HashSet<String>, HashSet<String>>> indexList; // viewName, Pair<NodeSet, EdgeSet>
	private static HashMap<String, ArrayList<String>> sIndexMap;
	private static ArrayList<DatalogClause> indexes;
	
	private static Store store = null;
	private static Store baseStore = null;
	
	public static void initialize() {
//		System.out.println("[GraphTransServer] Initialize()...");
		if (Config.isPostgresEnabled() == true) {
			store = new PostgresStore();
		}
		baseStore = new SimpleDatalogStore();
		baseStore.connect();
		program = new DatalogProgram();
		transRuleListList = new ArrayList<TransRuleList>();
		transRuleLists = new HashMap<String, TransRuleList>();
		indexList = new HashMap<String, Pair<HashSet<String>, HashSet<String>>>();
		sIndexMap = new HashMap<String, ArrayList<String>>();
		indexes = new ArrayList<DatalogClause>();
	}
	
	public static int getNumOfTransRuleListList() {
		return transRuleListList.size();
	}
	
	public static void setBaseStore(Store s) {
		baseStore = s;
	}
	
	public static Store getBaseStore() {
		return baseStore;
	}
	
	public static void setStore(Store s) {
		store = s;
	}
	
	public static Store getStore() {
		return store;
	}
	
	public static void clear() {
		transRuleListList.clear();
		egdList.clear();
		transRuleLists.clear();
		indexList.clear();
		sIndexMap.clear();
		indexes.clear();
	}
	
	public static ArrayList<DatalogClause> getIndexes() {
		return indexes;
	}
	
	public static void addSIndexMap(String viewName, String query) {
		if (sIndexMap.containsKey(viewName) == false) {
			sIndexMap.put(viewName, new ArrayList<String>());
		}
		sIndexMap.get(viewName).add(query);
	}
	
	public static HashMap<String, ArrayList<String>> getSIndexMap() {
		return sIndexMap;
	}
	
	public static void addIndexLabel(String viewName, boolean isNode, String label) {
		if (indexList.containsKey(viewName) == false) {
			indexList.put(viewName, Pair.of(new HashSet<String>(), new HashSet<String>()));
		} 
		if (isNode == true) {
			indexList.get(viewName).getLeft().add(label);
		} else {
			indexList.get(viewName).getRight().add(label);
		}
	}
	
	public static HashMap<String, Pair<HashSet<String>, HashSet<String>>> getIndexList() {
		return indexList;
	}
	
	public static TransRuleList getTransRuleList(String viewName) {
		return transRuleLists.get(viewName);
	}

	public static TransRuleList getTransRuleList(int index) {
		return transRuleListList.get(index);
	}

	public static ArrayList<Egd> getEgdList() {
		return egdList;
	}
		
	public static void addTransRuleList(TransRuleList t) {
		String viewName = t.getViewName();
		transRuleLists.put(viewName, t);
		transRuleListList.add(t); // FIXME: remove later
	}
	
	public static int getNumTransRuleList() {
		return transRuleListList.size();
	}

	public static DatalogProgram getProgram() {
		return program;
	}
}
