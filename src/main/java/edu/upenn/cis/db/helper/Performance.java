package edu.upenn.cis.db.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;

public class Performance {
	private static String datetime;
	
	private static String desc;
	private static String graph;

	private static String platform; // "lb", "pg", "neo4j"
	private static String viewType; // "mv", "hv", "vv"

	private static long s_graph;
	private static long s_selectivity;
	private static long s_schema;
	private static long s_egd;

	private static boolean b_sequential;
	private static boolean b_prune_typecheck;
	private static boolean b_prune_subquery;
	private static boolean b_use_substitute_index;

	// time
	private static List<Long> t_createView;
	private static long t_createIndex;

	private static List<Boolean> b_typecheckInput; // level, output
	private static List<Boolean> b_typecheckOutput; // level, output
	private static List<Long> t_typecheckInput; // level, time
	private static List<Long> t_typecheckOutput; // level, time

	private static long queryId = 0; 
	private static long rulesetId = 0;
	private static String lastViewName;
	
	public static String getLastViewName() {
		return lastViewName;
	}

	public static void setLastViewName(String lastViewName) {
		Performance.lastViewName = lastViewName;
	}

	private static List<Long> t_query;
	private static List<Integer> t_query_result;
	private static List<Boolean> b_isUpdate;
	private static long s_updateRatio = 0;
	private static long t_loading = 0;
	
	public static long getT_loading() {
		return t_loading;
	}

	public static void setT_loading(long t_loading) {
		Performance.t_loading = t_loading;
	}

	private static Map<String, Object> obj = new LinkedHashMap<String, Object>();
	
	private static final String CSV_SEP = "|";
	
	private static int tid;
	
	public static void addBuildViewTime(long t) {
		t_createView.add(t);
	}

	public static void addBuildIndexTime(long t) {
		t_createIndex = t;
	}

	public static void addUpdateTime(long time) {
		t_query.add(time);
		b_isUpdate.add(true);
	}
	
	public static void addQueryTime(long time) {
		t_query.add(time);
		b_isUpdate.add(false);
	}
	
	public static void addQueryResult(int c) {
		t_query_result.add(c);
	}

	public static void setPlatform(String p) {
		platform = p;
	}

	public static void setViewType(String v) {
		viewType = v;
	}

	public static void setGraphSelectivity(long s) {
		s_selectivity = s;
	}
	
	public static void setGraphSize(long s) {
		s_graph = s;
	}

	public static void setSchemaSize(long s) {
		s_schema = s;
	}
	public static void setEgdSize(long s) {
		s_egd = s;
	}
	
	public static void setPruneTypecheck(boolean b) {
		b_prune_typecheck = b;
	}

	public static void setPruneSubquery(boolean b) {
		b_prune_subquery = b;
	}

	public static void setUseSubstituteIndex(boolean b) {
		b_use_substitute_index = b;
	}

	public static void setUseSequential(boolean b) {
		b_sequential = b;
	}
	
	public static void setUpdateRatio(long u) {
		s_updateRatio = u;
	}
	
	public static void setQueryId(long q) {
		queryId = q;
	}
	
	public static void addTypeCheck(boolean b_in, boolean b_out, long t_in, long t_out) {
		b_typecheckInput.add(b_in);
		b_typecheckOutput.add(b_out);
		t_typecheckInput.add(t_in);
		t_typecheckOutput.add(t_out);
	}
		
	public static void setGraph(String _graph) {
		graph = _graph;
	}
	
	public static void setup(String _graph, String _desc) {	
		tid = Util.startTimer();
		
		datetime = Util.getDateTime();
		b_typecheckInput = new ArrayList<Boolean>();
		b_typecheckOutput = new ArrayList<Boolean>();
		t_typecheckInput = new ArrayList<Long>();
		t_typecheckOutput = new ArrayList<Long>();
		
		t_createView = new ArrayList<Long>();
		t_query = new ArrayList<Long>();
		t_query_result = new ArrayList<Integer>();
		b_isUpdate = new ArrayList<Boolean>();
		
		t_createIndex = 0;
		b_sequential = true;
		s_updateRatio = 0;
		t_loading = 0;

		rulesetId = 0;
		queryId = 0; 
		
		graph = _graph;
		desc = _desc;
	}

	@SuppressWarnings("unchecked")
	public static void setJSON() {
		obj = new LinkedHashMap<String, Object>();
		
		obj.put("datetime", datetime);
		obj.put("elapsed_time", Util.getElapsedTime(tid));
		obj.put("desc", desc);
		obj.put("graph", graph);
		obj.put("platform", platform);
		obj.put("viewType", viewType);

		obj.put("b_prune_typecheck", b_prune_typecheck);
		obj.put("b_prune_subquery", b_prune_subquery);
		obj.put("b_use_substitute_index", b_use_substitute_index);

		obj.put("s_graph", s_graph);
		obj.put("s_selectivity", s_selectivity);
		obj.put("s_schema", s_schema);
		obj.put("s_egd", s_egd);
		obj.put("b_sequential", b_sequential);

		obj.put("b_typecheckInput", b_typecheckInput);
		obj.put("b_typecheckOutput", b_typecheckOutput);
		
		obj.put("t_typecheckInput", t_typecheckInput);
		obj.put("t_typecheckOutput", t_typecheckOutput);
		obj.put("t_createView", t_createView);
		obj.put("t_createIndex", t_createIndex);

		obj.put("rulesetId", rulesetId);
		obj.put("queryId", queryId);
		obj.put("t_query", t_query);
		obj.put("b_isUpdate", b_isUpdate);
		obj.put("s_updateRatio", s_updateRatio);
		obj.put("t_loading", t_loading);
	}

	private static String csvPath = "output.csv";
	public static void printCSVHead() {
//		System.out.println("printHead==");
		StringBuilder str = new StringBuilder();
		if (obj == null) {
			return;
		}
		
		int i = 0;
		for (Object key : obj.keySet()) {
	        String keyStr = (String)key;
	        Object keyvalue = obj.get(keyStr);

	        if (i > 0) {
	        	str.append(CSV_SEP);
//	        	System.out.print(CSV_SEP);
	        }
//	        System.out.print(keyStr);
	        str.append(keyStr);
	        i++;
	    }
//		System.out.println();
		str.append("\n");
		
//		System.out.println("csvPath: " + csvPath);
		Util.writeToFile(csvPath, str.toString(), true);
	}
	
	public static void printCSVBody() {
		String str = getCSVBody();
		Util.writeToFile(csvPath, str, true);
	}
	
	public static String getCSVBody() {
//		System.out.println("printBody==");
		StringBuilder str = new StringBuilder();

		if (obj == null) {
			return "";
		}
		
		int i = 0;
		for (Object key : obj.keySet()) {
	        String keyStr = (String)key;
	        Object keyvalue = obj.get(keyStr);
	        
//	        System.out.println("[Performance] keyStr: " + keyStr + " keyvalue: " + keyvalue);

	        if (i > 0) {
	        	str.append(CSV_SEP);
//	        	System.out.print(CSV_SEP);
	        }
	        if (keyvalue.getClass() == String.class) {
	        	String value = Util.addQuotes(keyvalue.toString());
//	        	System.out.print(value);
	        	str.append(value);
	        } else {
//	        	System.out.print(keyvalue);
	        	str.append(keyvalue);
	        }
	        i++;
	    }
		
//		System.out.println();
    	str.append("\n");

    	return str.toString();
	}

	public static Map<String, Object> getJSON() {
		return obj;
	}
	
	public static void setCSVPath(String csv) {
		csvPath = csv;
	}

	public static long getRulesetId() {
		return rulesetId;
	}

	public static void setRulesetId(long rulesetId) {
		Performance.rulesetId = rulesetId;
	}
	
	public static void logPerformance() {
		long viewTime = 0;

		for (long t : t_createView) {
			viewTime += t;
		}
		viewTime += t_createIndex;
		
		if (t_createIndex > 0) {
			viewType = "ssr";
		}
		
		String logStr;
		
		String lastViewNameForLog = graph + "_" + lastViewName;
		String newDataTimeFormat = datetime.replace(" ", "_");
		System.out.println("datetime|graph|platform|viewType|viewOrQuery|name|time|count");
		logStr = newDataTimeFormat + "|" + graph + "|" + platform + "|" + viewType + "|view|" + lastViewNameForLog + "|" + viewTime + "|0\n";
		System.out.print(logStr);
		Util.writeToFile("logs/perflog.txt", logStr, true);
		
		int j = 0;
		for (int i = 0; i < t_query.size(); i++) {
			if (b_isUpdate.get(i) == false) {
				String queryNameForLog = lastViewNameForLog + "_q" + (j + 1); 
				long queryTime = queryTime = t_query.get(i);
				int query_result_count = 0;
				
				if (t_createIndex > 0) {
					logStr = newDataTimeFormat + "|" + graph + "|" + platform + "|ssr|query|" + queryNameForLog + "|" + queryTime + "|" + t_query_result.get(j) + "\n";
				} else {
					logStr = newDataTimeFormat + "|" + graph + "|" + platform + "|" + viewType + "|query|" + queryNameForLog + "|" + queryTime + "|" + t_query_result.get(j) + "\n";
				}
				
				System.out.print(logStr);
				Util.writeToFile("logs/perflog.txt", logStr, true);
				j++;
			}
		}
	}
	
	
	public static void main(String[] args) {
		Performance.setup("wordnet", "worknet used");//, "lb", "hv", 10, 5, 12);
		Performance.setPlatform("lb");
		Performance.setViewType("hv");
		Performance.setGraphSize(120323);
		Performance.setGraphSelectivity(100); // 1%=100
		Performance.setSchemaSize(132);
		Performance.setEgdSize(13);		
		Performance.setUseSequential(true);
		Performance.addBuildViewTime(4214123);
		Performance.addBuildIndexTime(42);
		Performance.addTypeCheck(true, true, 100, 1231);
		Performance.setQueryId(1);
		Performance.addQueryTime(124124);
		Performance.addQueryTime(124235235);
		Performance.addQueryTime(3243);
		Performance.addQueryTime(32323);
		Performance.addUpdateTime(32);
		Performance.setUpdateRatio(50);
		Performance.setPruneTypecheck(true);
		Performance.setPruneSubquery(false);
		Performance.setUseSubstituteIndex(false);
		Performance.setT_loading(0);
		
		Performance.setJSON();
		Performance.printCSVHead();
		Performance.printCSVBody();

		System.out.println(Performance.getJSON());
	}
}