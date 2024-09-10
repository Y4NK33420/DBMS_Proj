package edu.upenn.cis.db.graphtrans.experiment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ExpConfigLoader {
	public static class Node {
		long id;
		String label;
		
		Node(long id, String label) {
			this.id = id;
			this.label = label;
		}
		
		public String toString() {
			return "Node(" + id + "," + label + ")";
		}
	};
	
	public static class Edge {
		long id;
		long src;
		long dst;
		String label;

		Edge(long id, long src, long dst, String label) {
			this.id = id;
			this.src = src;
			this.dst = dst;
			this.label = label;
		}

		public String toString() {
			return "Edge(" + id + "," + src + "," + dst + "," + label + ")";
		}
	};
	private static ArrayList<String> getStringArray(JSONArray ja) {
		ArrayList<String> values = new ArrayList<String>();
		for (Iterator itr = ja.iterator(); itr.hasNext();) {
			values.add((String)itr.next());
		}
		return values;
	}

	private static ArrayList<Boolean> getBooleanArray(JSONArray ja) {
		ArrayList<Boolean> values = new ArrayList<Boolean>();
		for (Iterator itr = ja.iterator(); itr.hasNext();) {
			values.add((Boolean)itr.next());
		}
		return values;
	}

	private static ArrayList<Long> getLongArray(JSONArray ja) {
		ArrayList<Long> values = new ArrayList<Long>();
		for (Iterator itr = ja.iterator(); itr.hasNext();) {
			values.add((Long)itr.next());
		}
		return values;
	}   
	
	public static void loadDataset(String graph) throws FileNotFoundException {
//		System.out.println("[loadDataset] dataset: " + graph);
		
		HashMap<String, ArrayList<String>> graphToSchemaNodesMap = (HashMap<String, ArrayList<String>>) ExpConfig.get("graphToSchemaNodesMap");
		HashMap<String, ArrayList<ArrayList<String>>> graphToSchemaEdgesMap = (HashMap<String, ArrayList<ArrayList<String>>>) ExpConfig.get("graphToSchemaEdgesMap");
		HashMap<String, ArrayList<String>> graphToConstraintsMap = (HashMap<String, ArrayList<String>>) ExpConfig.get("graphToConstraintsMap");
		HashMap<String, ArrayList<String>> graphToNamesMap = (HashMap<String, ArrayList<String>>) ExpConfig.get("graphToNamesMap");
		HashMap<String, ArrayList<ArrayList<String>>> graphToViewsMap = (HashMap<String, ArrayList<ArrayList<String>>>) ExpConfig.get("graphToViewsMap");
		HashMap<String, ArrayList<ArrayList<String>>> graphToQueriesMap = (HashMap<String, ArrayList<ArrayList<String>>>) ExpConfig.get("graphToQueriesMap");
		HashMap<String, ArrayList<Node>> graphToTestsetNodesMap = (HashMap<String, ArrayList<Node>>) ExpConfig.get("graphToTestsetNodesMap");
		HashMap<String, ArrayList<Edge>> graphToTestsetEdgesMap = (HashMap<String, ArrayList<Edge>>) ExpConfig.get("graphToTestsetEdgesMap");
		
		JSONObject jo = getJSONObject("experiment/workload/" + graph + ".json");

		// constraints
		graphToConstraintsMap.put(graph, getStringArray((JSONArray)jo.get("constraints")));
		
		// schemas
		graphToSchemaNodesMap.put(graph, getStringArray((JSONArray)((JSONObject)jo.get("schema")).get("nodes")));
		JSONArray schemaEdgesObj = (JSONArray)((JSONObject)jo.get("schema")).get("edges");
		ArrayList<ArrayList<String>> schemaEdges = new ArrayList<ArrayList<String>>();	
		for (Iterator itr = schemaEdgesObj.iterator(); itr.hasNext();) {
			schemaEdges.add(getStringArray((JSONArray)itr.next()));
		}
		graphToSchemaEdgesMap.put(graph, schemaEdges);

		// views & queries
		JSONArray workloadsObj = (JSONArray)jo.get("workload");
		

		ArrayList<String> names = new ArrayList<String>();
		ArrayList<ArrayList<String>> views = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> queries = new ArrayList<ArrayList<String>>();
		
		for (Iterator itr = workloadsObj.iterator(); itr.hasNext();) {
			JSONObject workload = (JSONObject)itr.next();
			String workloadName = (String) workload.get("name");
			JSONArray viewObj = (JSONArray) workload.get("views");
			JSONArray queryObj = (JSONArray) workload.get("queries");
			views.add(getStringArray(viewObj));
			queries.add(getStringArray(queryObj));
			names.add(workloadName);
		}
		graphToNamesMap.put(graph, names);
		graphToViewsMap.put(graph, views);
		graphToQueriesMap.put(graph, queries);		
		
		// testset
		JSONObject testsetObj = (JSONObject)jo.get("testset");
		JSONArray testsetNodesObj = (JSONArray)testsetObj.get("nodes");
		JSONArray testsetEdgesObj = (JSONArray)testsetObj.get("edges");

		ArrayList<Node> testsetNodes = new ArrayList<Node>();
		ArrayList<Edge> testsetEdges = new ArrayList<Edge>();
			
		for (Iterator itr = testsetNodesObj.iterator(); itr.hasNext();) {
			JSONArray nodeObj = (JSONArray)itr.next();
			Long id = (Long)nodeObj.get(0);
			String label = (String)nodeObj.get(1);
			testsetNodes.add(new Node(id, label));
		}
		for (Iterator itr = testsetEdgesObj.iterator(); itr.hasNext();) {
			JSONArray edgeObj = (JSONArray)itr.next();
			Long id = (Long)edgeObj.get(0);
			Long src = (Long)edgeObj.get(1);
			Long dst = (Long)edgeObj.get(2);
			String label = (String)edgeObj.get(3);
			testsetEdges.add(new Edge(id, src, dst, label));
		}	
		graphToTestsetNodesMap.put(graph, testsetNodes);
		graphToTestsetEdgesMap.put(graph, testsetEdges);
	}
	
	private static JSONObject getJSONObject(String fileName) {
//		System.out.println("[getJSONObject] fileName: " + fileName);
		Object obj = null;
		
		BufferedReader reader = null;
		StringBuilder jsonStrBuilder = new StringBuilder();
		String line = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			while ((line = reader.readLine()) != null) {
				if (line.trim().startsWith("//") == false) { 
					jsonStrBuilder.append(line.trim() + " ");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String jsonStr = jsonStrBuilder.toString();

//		System.out.println("jsonStr: " + jsonStr);
		
		try {
			obj = new JSONParser().parse(jsonStr);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return (JSONObject)obj; 	
	}

	public static void load(String fileName) throws FileNotFoundException {
		JSONObject jo = getJSONObject(fileName);

		ExpConfig.setConf("configFilePath", (String)jo.get("configFilePath"));
		ExpConfig.setConf("testmode", (Boolean)jo.get("testmode"));
		ExpConfig.setConf("printRules", (Boolean)jo.get("printRules"));
		ExpConfig.setConf("printTiming", (Boolean)jo.get("printTiming"));
		ExpConfig.setConf("printConsole", (Boolean)jo.get("printConsole"));

		ExpConfig.setConf("expIteration", (Long)jo.get("expIteration"));

		ExpConfig.setConf("platforms", getStringArray((JSONArray)jo.get("platforms")));
		ExpConfig.setConf("viewtypes", getStringArray((JSONArray)jo.get("viewtypes")));
		
		HashMap<String, ArrayList<String>> graphToSchemaNodesMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<ArrayList<String>>> graphToSchemaEdgesMap = new HashMap<String, ArrayList<ArrayList<String>>>();
		HashMap<String, ArrayList<String>> graphToConstraintsMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> graphToNamesMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<ArrayList<String>>> graphToViewsMap = new HashMap<String, ArrayList<ArrayList<String>>>();
		HashMap<String, ArrayList<ArrayList<String>>> graphToQueriesMap = new HashMap<String, ArrayList<ArrayList<String>>>();
		HashMap<String, ArrayList<Node>> graphToTestsetNodesMap = new HashMap<String, ArrayList<Node>>();
		HashMap<String, ArrayList<Edge>> graphToTestsetEdgesMap = new HashMap<String, ArrayList<Edge>>();

		ExpConfig.setConf("graphToSchemaNodesMap", graphToSchemaNodesMap);
		ExpConfig.setConf("graphToSchemaEdgesMap", graphToSchemaEdgesMap);
		ExpConfig.setConf("graphToConstraintsMap", graphToConstraintsMap);
		ExpConfig.setConf("graphToNamesMap", graphToNamesMap);
		ExpConfig.setConf("graphToViewsMap", graphToViewsMap);
		ExpConfig.setConf("graphToQueriesMap", graphToQueriesMap);
		ExpConfig.setConf("graphToTestsetNodesMap", graphToTestsetNodesMap);
		ExpConfig.setConf("graphToTestsetEdgesMap", graphToTestsetEdgesMap);
		
		graphToConstraintsMap.put("default", getStringArray((JSONArray)jo.get("constraints")));
		
		ArrayList<String> datasets = getStringArray((JSONArray)jo.get("datasets"));
		ArrayList<Boolean> datasets_execute = getBooleanArray((JSONArray)jo.get("datasets_execute"));
		
		ExpConfig.setConf("datasets", datasets);
		ExpConfig.setConf("datasets_execute", datasets_execute);
		
		for (int i = 0; i < datasets_execute.size(); i++) {
		if (datasets_execute.get(i) == true) {
			loadDataset(datasets.get(i));
		}
	}

//		System.out.println("ExpConfig: " + ExpConfig.get("graphToConstraintsMap"));
		
	}
}
