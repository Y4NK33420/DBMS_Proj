//package edu.upenn.cis.db.graphtrans.experiment;
//
//import java.io.IOException;
//import java.util.ArrayList;
//
//import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.CommandLineParser;
//import org.apache.commons.cli.DefaultParser;
//import org.apache.commons.cli.HelpFormatter;
//import org.apache.commons.cli.Option;
//import org.apache.commons.cli.Options;
//import org.apache.commons.cli.ParseException;
//
//import edu.upenn.cis.db.graphtrans.Config;
//import edu.upenn.cis.db.helper.Performance;
//import edu.upenn.cis.db.helper.Util;
//
//public class ExpStarter extends Thread  {
//	private final static String experimentBasePath = "experiment";
//	private final static String resultCSVSubPath = "result_csv";
//	private final static String resultCSVFullPath = experimentBasePath + "/" + resultCSVSubPath;
//
////	private static String option_viewtype = null;
////	private static String option_platform = null;
////	private static boolean option_ssr = false;
////	private static boolean option_test = false;
////	private static String option_workload = null;
////	private static String option_graph = null;
////	private static String configFilePath = "graphview.conf";
//
//	
//	   public static void expIVM() {
////	    	// parameter: update ratio (0 to 1)
////	    	// w/ and w/out index
////	    	// Set aside k=300 edges from input datasets
////	    	// An update is to insert an edge from the set above
////	    	// THe result of a query involves one of the node of the edge inserted.
////	    	
//	    	System.out.println("[expIncrementalViewMaintenance]");
//  
//	    	
//	    	Executor.initialize();    	
//	    	Executor.getDatasets().add("SYN");
////	    	Executor.getViewtypes().add("MV");
////	    	Executor.getViewtypes().add("HV");
////	    	Executor.getViewtypes().add("ASR");
////	    	Executor.getViewtypes().add("SSR");
//			Executor.getViewtypes().addAll((ArrayList<String>)ExpConfig.get("exp_viewtypes"));
//
////	    	Executor.getPlatforms().add("LB");
//	    	Executor.getPlatforms().addAll((ArrayList<String>)ExpConfig.get("exp_platforms"));
//			Executor.getB_typecheck_prune().add((Boolean)ExpConfig.get("prune_typecheck"));
//			Executor.getB_useIndex().addAll((ArrayList<Boolean>)ExpConfig.get("exp_indexes"));
//			Executor.getB_subquery_prune().add((Boolean)ExpConfig.get("prune_subquery"));
//			Executor.setB_incrementalViewMaintenance(true); // key point 	
//			Executor.setImportData(true);
//			Executor.getB_sequentials().add(false);
//			
//			Executor.getUpdate_querySet().addAll((ArrayList<Long>)ExpConfig.get("update_querySet"));
//			
//			Executor.getUpdateRatios().addAll((ArrayList<Long>)ExpConfig.get("update_ratio"));
//			Executor.setB_querying(true);
//			Executor.setIvm_iteration((Long)ExpConfig.get("ivm_iteration"));
//			Executor.setExpIteration((Long)ExpConfig.get("expIteration"));
//			
//			String dt = Util.getDateTime().replace("-", "");
//			dt = dt.replace(":",  "");
//			dt = dt.replace(".",  "");
//			dt = dt.replace(" ",  "_");
//			
//			String csvPath = "result_ivm";
//			Executor.setCsvPath(resultCSVFullPath + "/" + csvPath + ".csv");
//			System.out.println("csvPath: " + csvPath);
//			
//			Executor.run();
//	    }
//	
//	public static void expParallel() {
//    	System.out.println("[expParallel]");
//		
//    	Executor.initialize();   
//    	Executor.setExpParallel(true);
//		ArrayList<String> arr_datasets = (ArrayList<String>)ExpConfig.get("datasets");
//		ArrayList<Boolean> datasets_execute = (ArrayList<Boolean>)ExpConfig.get("datasets_execute");
//		
//		System.out.println("arr_datasets: " + arr_datasets);
//		System.out.println("datasets_execute: " + datasets_execute);
//		
//		for (int i = 0; i < datasets_execute.size(); i++) {
//			if (datasets_execute.get(i) == true) {
//				Executor.getDatasets().add(arr_datasets.get(i));
//			}
//		}
//		Executor.setImportData(true);
//		Executor.getViewtypes().add("MV");
//		Executor.getPlatforms().addAll((ArrayList<String>)ExpConfig.get("exp_platforms"));
//		Executor.getB_typecheck_prune().add((Boolean)ExpConfig.get("prune_typecheck"));
//		Executor.getB_useIndex().add(false);
//		Executor.getB_subquery_prune().add((Boolean)ExpConfig.get("prune_subquery"));
//	    Executor.getB_sequentials().addAll((ArrayList<Boolean>)ExpConfig.get("exp_sequential"));
//	    Executor.setB_querying(false);
//		
//		String dt = Util.getDateTime().replace("-", "");
//		dt = dt.replace(":",  "");
//		dt = dt.replace(".",  "");
//		dt = dt.replace(" ",  "_");
//		
//		String csvPath = "result_parallel";
//		Executor.setCsvPath(resultCSVFullPath + "/" + csvPath + ".csv");
//		System.out.println("[expParallel] csvPath: " + csvPath);
//		
//	    Executor.run();	        	
//    }
//    
//	public static void expParallelVarying() {
//    	System.out.println("[expParallelVarying]");
//		
//    	Executor.initialize();   
//    	Executor.setExpParallelVarying(true);
////		ArrayList<String> arr_datasets = (ArrayList<String>)ExpConfig.get("datasets");
////		ArrayList<Boolean> datasets_execute = (ArrayList<Boolean>)ExpConfig.get("datasets_execute");
////		
////		System.out.println("arr_datasets: " + arr_datasets);
////		System.out.println("datasets_execute: " + datasets_execute);
////		
////		for (int i = 0; i < datasets_execute.size(); i++) {
////			if (datasets_execute.get(i) == true) {
////				Executor.getDatasets().add(arr_datasets.get(i));
////			}
////		}
//		Executor.setImportData(true);
//		Executor.getViewtypes().add("MV");
//		Executor.getPlatforms().add("LB"); //All((ArrayList<String>)ExpConfig.get("exp_platforms"));
//		Executor.getDatasets().add("SYN");
//		Executor.getB_typecheck_prune().add((Boolean)ExpConfig.get("prune_typecheck"));
//		Executor.getB_useIndex().add(false);
//		Executor.getB_subquery_prune().add((Boolean)ExpConfig.get("prune_subquery"));
//	    Executor.getB_sequentials().addAll((ArrayList<Boolean>)ExpConfig.get("exp_sequential"));
//	    Executor.setB_querying(false);
//		
//		String dt = Util.getDateTime().replace("-", "");
//		dt = dt.replace(":",  "");
//		dt = dt.replace(".",  "");
//		dt = dt.replace(" ",  "_");
//		
//		String csvPath = "result_parallel_varying";
//		Executor.setCsvPath(resultCSVFullPath + "/" + csvPath + ".csv");
//		System.out.println("[expParallelVarying] csvPath: " + csvPath);
//		
//	    Executor.run();	        	
//    }	
//	
//    public static void expVariousDataset(boolean useQuery) {
//    	System.out.println("[expVariousDataset] useQuery: " + useQuery);
//
//    	Executor.initialize();
//    	Executor.setImportData(true);
//		ArrayList<String> arr_datasets = (ArrayList<String>)ExpConfig.get("datasets");
//		ArrayList<Boolean> arr_datasets_execute = (ArrayList<Boolean>)ExpConfig.get("datasets_execute");
////		System.out.println("arr_datasets: " + arr_datasets);
////		System.out.println("arr_datasets_execute: " + arr_datasets_execute);
//		
//		for (int i = 0; i < arr_datasets.size(); i++) {
//			if (arr_datasets.get(i).contentEquals("ALL") == false) {
//				ExpDataset eds = ExpConfig.getDataSets().get(arr_datasets.get(i));
//				if (eds.isSynthetic() == false && arr_datasets_execute.get(i) == true) {
//					Executor.getDatasets().add(arr_datasets.get(i));
//				}
//			}
//		}
//		Executor.getViewtypes().addAll((ArrayList<String>)ExpConfig.get("exp_viewtypes"));
//		Executor.getPlatforms().addAll((ArrayList<String>)ExpConfig.get("exp_platforms"));
//		Executor.getB_typecheck_prune().add((Boolean)ExpConfig.get("prune_typecheck"));
//		Executor.getB_subquery_prune().add((Boolean)ExpConfig.get("prune_subquery"));
//		Executor.getB_useIndex().addAll((ArrayList<Boolean>)ExpConfig.get("exp_indexes"));
//	    Executor.setB_querying(useQuery);
//	    Executor.getB_sequentials().add(false);
//	    
//		String dt = Util.getDateTime().replace("-", "");
//		dt = dt.replace(":",  "");
//		dt = dt.replace(".",  "");
//		dt = dt.replace(" ",  "_");
//		
//		String csvPath = "result_various";
//		if (useQuery == true) {
//	    	Executor.setExpIteration(1L);
//			csvPath += "_query";
//		}
//		csvPath = resultCSVFullPath + "/" + csvPath + ".csv";
//		Executor.setCsvPath(csvPath);
//		System.out.println("[expVariousDataset] csvPath: " + csvPath);
//			
//	    Executor.run();	     	
//    }    
//    
//    public static void expVarying(boolean isScale, boolean useQuery) {
//    	System.out.println("[expVarying] isScale: " + isScale + " useQuery: " + useQuery);
//    	
//		Executor.initialize();
//		Executor.getDatasets().add("SYN");
//		Executor.setImportData(true);
//		Executor.getViewtypes().addAll((ArrayList<String>)ExpConfig.get("exp_viewtypes"));
//		Executor.getPlatforms().addAll((ArrayList<String>)ExpConfig.get("exp_platforms"));
//		Executor.getB_typecheck_prune().add((Boolean)ExpConfig.get("prune_typecheck"));
//		Executor.getB_subquery_prune().add((Boolean)ExpConfig.get("prune_subquery"));
//		Executor.setVaryingScale(isScale);
//		Executor.getB_sequentials().add(false);
//
//		Executor.getB_useIndex().addAll((ArrayList<Boolean>)ExpConfig.get("exp_indexes"));
//		if (useQuery == true) { // measure QUERY
//	    	Executor.setExpIteration(1L);
//		}
//	    Executor.setB_querying(useQuery);
//		
//		String dt = Util.getDateTime().replace("-", "");
//		dt = dt.replace(":",  "");
//		dt = dt.replace(".",  "");
//		dt = dt.replace(" ",  "_");
//		
//		String csvPath = "result";
//		if (isScale == true) {
//			csvPath += "_scalability";
//		} else {
//			csvPath += "_selectivity";
//		}
//		if (useQuery == true) {
//			csvPath += "_query";
//		}
//		csvPath = resultCSVFullPath + "/" + csvPath + ".csv";
//		Executor.setCsvPath(csvPath);
//		Util.Console.logln("[expVarying] csvPath: " + csvPath);
//
//	    Executor.run();
//    }    
//	
//    private static void expTypeCheck() {
//		System.out.println("[expTypeCheck]");
//
//		Executor.initialize();
//		Executor.setTypeChecking(true);
//
//    	Executor.getPlatforms().add("LB");
//		Executor.getViewtypes().add("MV");
//		
//		Executor.getB_typecheck_prune().add(false);
//		Executor.getB_typecheck_prune().add(true);
//		
//		Executor.getB_useIndex().add(false);
//		Executor.getB_subquery_prune().add((Boolean)ExpConfig.get("prune_subquery"));
//		Executor.getB_sequentials().add(false);
//		
//		String dt = Util.getDateTime().replace("-", "");
//		dt = dt.replace(":",  "");
//		dt = dt.replace(".",  "");
//		dt = dt.replace(" ",  "_");
//
//		String csvPath = resultCSVFullPath + "/" + "result_typecheck.csv";
//		Executor.setCsvPath(csvPath); 
//		Util.Console.logln("[expTypeCheck] csvPath: " + csvPath);
//		
//		Executor.run();		
//    }
//    
//	private static void expTypeCheck_deprecated() {
//		System.out.println("[expTypeCheck]");
//
//		Executor.initialize();
//		Executor.setTypeChecking(true);
//		Executor.getDatasets().add("ALL");
//
////    	Executor.getDatasets().addAll((ArrayList<String>)ExpConfig.get("datasets"));
//		ArrayList<String> arr_datasets = (ArrayList<String>)ExpConfig.get("datasets");	
//		ArrayList<Boolean> datasets_execute = (ArrayList<Boolean>)ExpConfig.get("datasets_execute");
//		
////		System.out.println("arr_datasets: " + arr_datasets);
////		System.out.println("datasets_execute: " + datasets_execute);
//		
//		for (int i = 0; i < datasets_execute.size(); i++) {
//			if (datasets_execute.get(i) == true) {
//				Executor.getDatasets().add(arr_datasets.get(i));
//			}
//		}
//		
//		ExpDataset eds_all = new ExpDataset();
//		eds_all.getTransrules().add(new ArrayList<ArrayList<String>>());
//		for (String dataset : Executor.getDatasets()) {
//			if (dataset.contentEquals("ALL") == false) {
//				ExpDataset eds = ExpConfig.getDataSets().get(dataset);
//				eds_all.getSchemas().addAll(eds.getSchemas());
//				eds_all.getEgds().addAll(eds.getEgds());
//				if (eds.isSynthetic() == true) {
//					for (int i = 0; i < eds.getTransrules().size(); i++) {
//						ArrayList<ArrayList<String>> rules = eds.getTransrules().get(i);
//						System.out.println("rules i: " + i + " => " + rules);
//						for (int j = 0; j < rules.size(); j++) {
//							if (eds_all.getTransrules().size() <= i) {
//								eds_all.getTransrules().get(0).add(rules.get(j));
//							}
//						}
//					}
//				}
//			}
//		}
//		System.out.println("eds_all: " + eds_all);
//		
//		ExpConfig.getDataSets().put("ALL", eds_all);
//
//    	Executor.getPlatforms().add("LB");
//		Executor.getViewtypes().add("MV");
//		
//		Executor.getB_typecheck_prune().add(false);
//		Executor.getB_typecheck_prune().add(true);
//		
//		Executor.getB_useIndex().add(false);
//		Executor.getB_subquery_prune().add((Boolean)ExpConfig.get("prune_subquery"));
//		Executor.getB_sequentials().add(false);
//		
//		String dt = Util.getDateTime().replace("-", "");
//		dt = dt.replace(":",  "");
//		dt = dt.replace(".",  "");
//		dt = dt.replace(" ",  "_");
//
//		String csvPath = resultCSVFullPath + "/" + "result_typecheck.csv";
//		Executor.setCsvPath(csvPath); 
//		Util.Console.logln("[expTypeCheck] csvPath: " + csvPath);
//
////		System.out.println("ExpConfig.getDataSets(): " + ExpConfig.getDataSets());
//		
//		Executor.run();
//	}
//    	
//	@SuppressWarnings("unchecked")
//	public static void execute() {
//		System.out.println(ExpConfig.getString());
//		
//		try {
//			Config.load((String)ExpConfig.get("configFilePath"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		if ((Boolean)ExpConfig.get("doTypeCheck") == true) {
//			expTypeCheck(); //ExpTypeChecking.run();
//		}
//		if ((Boolean)ExpConfig.get("doVaryingScale") == true) {
//			for (boolean useQuery : (ArrayList<Boolean>)ExpConfig.get("exp_useQueries")) {
//				expVarying(true, useQuery);
//			}
//		} 
//		if ((Boolean)ExpConfig.get("doVaryingSelectivity") == true) {
//			for (boolean useQuery : (ArrayList<Boolean>)ExpConfig.get("exp_useQueries")) {
//				expVarying(false, useQuery);
//			}
//		}
//		if ((Boolean)ExpConfig.get("doVariousDataset") == true) {
//			for (boolean useQuery : (ArrayList<Boolean>)ExpConfig.get("exp_useQueries")) {
//				expVariousDataset(useQuery);
//			}
//		}
//		if ((Boolean)ExpConfig.get("doParallel")  == true) {
//			expParallel();
//		}
//		if ((Boolean)ExpConfig.get("doParallelVarying")  == true) {
//			expParallelVarying();
//		}
//		
//		
//		if ((Boolean)ExpConfig.get("doIVM") == true) {
//			expIVM();
//		}
//	}
//	
//    public static void expVariousDataset(String dataset) {
//
//    }
//	
//	public void run() {
//		while (true) {
//			long heapSize = Runtime.getRuntime().totalMemory(); 
//			// Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
//			long heapMaxSize = Runtime.getRuntime().maxMemory();
//			 // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
//			long heapFreeSize = Runtime.getRuntime().freeMemory();
//			
//			System.out.println("heapSize: " + heapSize / 1024 / 1024 + "MB");
//			System.out.println("heapMaxSize: " + heapMaxSize / 1024 / 1024 + "MB");
//			System.out.println("heapFreeSize: " + heapFreeSize / 1024 / 1024 + "MB");
//			
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
//	
//	public static void main(String[] args) {
//		/*
//			- Size: for synthetic
//			- Ratio: for synthetic
//			- Schema
//			- Egds
//			- Rules:
//			- level: 1,2,3
//			- Query
//		 */
//		
//
////        (new ExpStarter()).start();
//
////		Options options = new Options();
////
////		Option platform = new Option("p", "platform", true, "platform (lb, pg, n4, ds)");
////		platform.setRequired(false);
////		options.addOption(platform);
////
////		Option viewtype = new Option("v", "viewtype", true, "viewtype (mv, hv, vv)");
////		viewtype.setRequired(false);
////		options.addOption(viewtype);
////
////		Option ssr = new Option("s", "ssr", false, "ssr");
////		ssr.setRequired(false);
////		options.addOption(ssr);
////
////		Option test = new Option("t", "test", false, "with test dataset");
////		test.setRequired(false);
////		options.addOption(test);
////		
////		Option dataset = new Option("g", "graph", true, "dataset graph");
////		dataset.setRequired(false);
////		options.addOption(dataset);
//
////		Option output = new Option("o", "output", true, "output file");
////		output.setRequired(false);
////		options.addOption(output);
//
////		Option workload = new Option("w", "workload", true, "workload JSON file");
////		workload.setRequired(true);
////		options.addOption(workload);
//
//		CommandLineParser parser = new DefaultParser();
//		HelpFormatter formatter = new HelpFormatter();
//		CommandLine cmd = null;
//
////		try {
////			cmd = parser.parse(options, args);
////			if (cmd.hasOption("p")) {
////				option_platform = cmd.getOptionValue("p");
////			}
////			if (cmd.hasOption("v")) {
////				option_viewtype = cmd.getOptionValue("v");
////			}
////			if (cmd.hasOption("w")) {
////				option_workload = cmd.getOptionValue("w");
////			}
////			if (cmd.hasOption("g")) {
////				option_graph = cmd.getOptionValue("g");
////			}
////			if (cmd.hasOption("s")) {
////				option_ssr = true;
////			}
////			if (cmd.hasOption("t")) {
////				option_test = true;
////			}
////		} catch (ParseException e) {
////			System.out.println(e.getMessage());
////			formatter.printHelp("graphview", options);
////
////			System.exit(1);
////		}
//		try {
//			Config.load(configFilePath);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
////		System.out.println("option_viewtype[" + option_viewtype + "] option_platform[" + option_platform + "] "
////				+ "option_ssr[" + option_ssr + "] option_workload[" + option_workload + "] "
////				+ "option_graph[" + option_graph + " option_test[" + option_test + "]");
//		
//		ExpConfigLoader.load(option_workload);
//
//		
//		int tid = Util.startTimer();
//		execute();
//		long et = Util.getElapsedTime(tid);
//	    System.out.println("#################");
//		System.out.println("#### All is done. Elapsed Time: " + et + " ms / " + (et/1000.0) + " sec");
//		System.out.println("#################");
//		
//	}   
//
//}
