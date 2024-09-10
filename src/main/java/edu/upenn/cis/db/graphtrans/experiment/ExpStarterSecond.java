package edu.upenn.cis.db.graphtrans.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONObject;

import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.Console;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.experiment.ExpConfigLoader.Edge;
import edu.upenn.cis.db.graphtrans.experiment.ExpConfigLoader.Node;
import edu.upenn.cis.db.graphtrans.parser.ViewParser;
import edu.upenn.cis.db.helper.Performance;
import edu.upenn.cis.db.helper.Util;

public class ExpStarterSecond extends Thread  {
	private final static String experimentBasePath = "experiment";
	private final static String resultCSVSubPath = "result_csv";
	private final static String resultCSVFullPath = experimentBasePath + "/" + resultCSVSubPath;

	private static String option_viewtype = null;
	private static String option_platform = null;
	private static boolean option_ssr = false;
	private static boolean option_test = false;
	private static String option_workload = null;
	private static String option_graph = null;
	private static String configFilePath = "conf/graphview.conf";
	
	@SuppressWarnings("unchecked")
	public static void execute(int viewIndex, String dataset) {
		try {
			Config.load((String)ExpConfig.get("configFilePath"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("[option_graph] " + option_graph);
		
		HashMap<String, ArrayList<String>> graphToConstraintsMap
				= (HashMap<String, ArrayList<String>>)ExpConfig.get("graphToConstraintsMap");
		HashMap<String, ArrayList<ArrayList<String>>> graphToViewsMap
				= (HashMap<String, ArrayList<ArrayList<String>>>)ExpConfig.get("graphToViewsMap");
		HashMap<String, ArrayList<ArrayList<String>>> graphToQueriesMap
				= (HashMap<String, ArrayList<ArrayList<String>>>)ExpConfig.get("graphToQueriesMap");
		HashMap<String, ArrayList<String>> graphToSchemaNodesMap
				= (HashMap<String, ArrayList<String>>)ExpConfig.get("graphToSchemaNodesMap");
		HashMap<String, ArrayList<ArrayList<String>>> graphToSchemaEdgesMap
				= (HashMap<String, ArrayList<ArrayList<String>>>)ExpConfig.get("graphToSchemaEdgesMap");
		HashMap<String, ArrayList<Node>> graphToTestsetNodesMap
				= (HashMap<String, ArrayList<Node>>)ExpConfig.get("graphToTestsetNodesMap");
		HashMap<String, ArrayList<Edge>> graphToTestsetEdgesMap
				= (HashMap<String, ArrayList<Edge>>)ExpConfig.get("graphToTestsetEdgesMap");

		option_test = (Boolean)ExpConfig.get("testmode");
		
//		System.out.println("==> " + graphToWorkloadMap.get("views").get(option_graph));
//		System.out.println("==> " + graphToWorkloadMap.get("queries").get(option_graph));
//		System.out.println("==> " + graphToWorkloadMap.get("constraints").get(option_graph));
//		System.out.println("==> " + graphToSchemaNodesMap.get(option_graph));
//		System.out.println("==> " + graphToSchemaEdgesMap.get(option_graph));
//		System.out.println("==> " + graphToTestsetNodesMap.get(option_graph));
//		System.out.println("==> " + graphToTestsetEdgesMap.get(option_graph));

		ArrayList<String> rules = new ArrayList<String>();
		rules.add("# OPTIONS");
		rules.add("option typecheck off");
		rules.add("option prunetypecheck on");
		rules.add("option prunequery off");
		rules.add("");
		
		rules.add("# DB SETUP");
		if (option_test == false) {
			rules.add("prepare from \"" + dataset + "\" on " + option_platform);
		}
		rules.add("connect " + option_platform);
		if (option_test == true) {
			rules.add("drop exp");
			rules.add("create graph exp");
		}
		rules.add("use exp");

		rules.add("");
		rules.add("# SCHEMAS");
		
		for (String label : graphToSchemaNodesMap.get(option_graph)) {
			rules.add("create node " + label);	
		}
		
		for (ArrayList<String> edge : graphToSchemaEdgesMap.get(option_graph)) {
			rules.add("create edge " + edge.get(0) + " (" + edge.get(1) + " -> " + edge.get(2) + ")");
		}

		rules.add("");
		rules.add("# CONSTRAINTS (EGDs)");
		for (String c : graphToConstraintsMap.get("default")) {
			rules.add("add constraint " + c);	
		}
		for (String c : graphToConstraintsMap.get(option_graph)) {
			rules.add("add constraint " + c);	
		}		

		if (option_test == true) {
			rules.add("");
			rules.add("# TEST DATASET");

			for (Node n : graphToTestsetNodesMap.get(option_graph)) {
				rules.add("insert N (" + n.id + ",\"" + n.label + "\")");
			}
			
			for (Edge e : graphToTestsetEdgesMap.get(option_graph)) {
				rules.add("insert E (" + e.id + ", " + e.src + ", " + e.dst + ",\"" + e.label + "\")");	
			}
		}		
		
		rules.add("#");
		rules.add("# VIEWS");
//		System.out.println("graphToViewsMap: " + graphToViewsMap + " viewIndex: " + viewIndex);
		
		String lastView = null;
		for (String c : graphToViewsMap.get(option_graph).get(viewIndex)) {
			String viewtype = "virtual";
			if (option_viewtype.equals("mv") == true) {
				viewtype = "materialized";	
			} else if (option_viewtype.equals("hv") == true) {
				viewtype = "hybrid";	
			}
			c = c.replace("CREATE VIEW", "CREATE " + viewtype + " VIEW");
			rules.add(c);
			lastView = c;
//			System.out.println("lastView: " + lastView);
		}
		
		if (option_viewtype.equals("ssr") == true) {
//			System.out.println("lastView2: " + lastView);
			ViewParser parser = new ViewParser();
			TransRuleList transRuleList = parser.Parse(lastView);
			String lastViewName = transRuleList.getViewName();
			String c = "create ssr on " + lastViewName;
			rules.add(c);
		}
		
		rules.add("#");
		rules.add("# QUERIES");
		for (String c : graphToQueriesMap.get(option_graph).get(viewIndex)) {
			rules.add(c);	
		}
			
		rules.add("#");
		rules.add("# FINISH");
		rules.add("drop exp");
		rules.add("disconnect");
		
		System.out.println("[Exp] # of rules: " + rules.size());
		for (int i = 0; i < rules.size(); i++) {
			System.out.println("#" + i + " => " + rules.get(i));
//			System.out.println(rules.get(i) + ";");
		}
		
		runRules(rules);
	}
	
	 private static void runRules(ArrayList<String> rules) {
		Config.initialize();
		Config.setTypeCheckEnabled(true);
		
		GraphTransServer.initialize(); // after setting Config 

		Util.Console.setEnable((Boolean)ExpConfig.get("printConsole"));
		Util.setConsole(false);

		Console console = new Console();

		Performance.setup(option_graph, option_graph + " workload");		

		CommandExecutor.setConsole(console);

		int tttid = Util.startTimer();
    	for (int i = 0; i < rules.size(); i++) {
    		String trimedRule = rules.get(i).trim();
			if (trimedRule.length() > 0 && trimedRule.substring(0,1).contentEquals("#") == false) {
				if ((Boolean)ExpConfig.get("printRules") == true) {
					if ((Boolean)ExpConfig.get("printTiming") == true) {
						System.out.println(rules.get(i) + "; (" + Util.getElapsedTime(tttid) + " ms)");
					} else {
						System.out.println(rules.get(i) + ";");
					}
				}
				CommandExecutor.run(rules.get(i));
			} else {
				if ((Boolean)ExpConfig.get("printRules") == true) {
					System.out.println(rules.get(i) + ";");					
				}
			}
    	}
		// For recording performance

		Performance.setPlatform(Config.getPlatform());
		Performance.setViewType(option_viewtype);
		Performance.logPerformance();

    	System.gc();
    	System.gc();
      }
	
    public static void expVariousDataset(String dataset) {

    }
	
	public void run() {
		while (true) {
			long heapSize = Runtime.getRuntime().totalMemory(); 
			// Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
			long heapMaxSize = Runtime.getRuntime().maxMemory();
			 // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
			long heapFreeSize = Runtime.getRuntime().freeMemory();
			
			System.out.println("heapSize: " + heapSize / 1024 / 1024 + "MB");
			System.out.println("heapMaxSize: " + heapMaxSize / 1024 / 1024 + "MB");
			System.out.println("heapFreeSize: " + heapFreeSize / 1024 / 1024 + "MB");
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;

		try {
			Config.load(configFilePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		option_viewtype = "mv";
		option_platform = "lb";
		option_workload = "experiment/workload/workload.json";
		option_graph = "soc";
		option_test = true;
		
//		System.out.println("option_viewtype[" + option_viewtype + "] option_platform[" + option_platform + "] "
//				+ "option_ssr[" + option_ssr + "] option_workload[" + option_workload + "] "
//				+ "option_graph[" + option_graph + " option_test[" + option_test + "]");
		
		try {
			ExpConfigLoader.load(option_workload);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		int tid = Util.startTimer();
		
		option_ssr = false;
		option_test = false;

		int iteration = 2;
		// todo: ssr -> one of viewtype

		int count = 0;
		HashMap<String, ArrayList<ArrayList<String>>> graphToViewsMap = (HashMap<String, ArrayList<ArrayList<String>>>)ExpConfig.get("graphToViewsMap");
		HashMap<String, ArrayList<String>> graphToNamesMap = (HashMap<String, ArrayList<String>>)ExpConfig.get("graphToNamesMap");

		for (int i = 0; i < (Long)ExpConfig.get("expIteration"); i++) {
			for (String platform : (ArrayList<String>)ExpConfig.get("platforms")) {
				option_platform = platform;
				for (String viewtype : (ArrayList<String>)ExpConfig.get("viewtypes")) {
					option_viewtype = viewtype;
					
//					if (platform.equals("n4") == true && viewtype.equals("mv") == true) continue;
					if (platform.equals("n4") == true && viewtype.equals("ssr") == true) continue;
					
					for (int j = 0; j < ((ArrayList<Boolean>)ExpConfig.get("datasets_execute")).size(); j++) {
						Boolean dataset_execute = ((ArrayList<Boolean>)ExpConfig.get("datasets_execute")).get(j);
						if (dataset_execute == false) continue;

						String dataset = ((ArrayList<String>)ExpConfig.get("datasets")).get(j);						
						option_graph = dataset;
						if (graphToViewsMap.containsKey(dataset) == true) {
							for (int k = 0; k < graphToViewsMap.get(dataset).size(); k++) {
								String workloadName = graphToNamesMap.get(dataset).get(k);
								Performance.setLastViewName(workloadName);
								
//								System.out.println("iteration: " + (i+1) + " count: " + (++count) + " platform: " + platform + " viewtype: " + viewtype + " dataset: " + dataset + " viewIndex: " + k);
								execute(k, dataset);
							}
						}
					}
				}
			}
		}
		long et = Util.getElapsedTime(tid);
	    System.out.println("#################");
		System.out.println("#### All is done. Elapsed Time: " + et + " ms / " + (et/1000.0) + " sec");
		System.out.println("#################");
		
	}   

}
