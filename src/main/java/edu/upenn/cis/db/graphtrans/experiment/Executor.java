package edu.upenn.cis.db.graphtrans.experiment;

import java.io.File;
import java.util.ArrayList;

import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.Console;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.helper.Performance;
import edu.upenn.cis.db.helper.Util;

public class Executor {
	private static ExpDataset dataset;
	private static ArrayList<String> platforms = new ArrayList<String>();
	private static ArrayList<String> datasets = new ArrayList<String>();
	private static ArrayList<String> viewtypes = new ArrayList<String>();
	private static ArrayList<Boolean> b_typecheck_prune = new ArrayList<Boolean>();
	private static ArrayList<Boolean> b_useIndex = new ArrayList<Boolean>();
	private static ArrayList<Boolean> b_subquery_prune = new ArrayList<Boolean>();
	private static ArrayList<Boolean> b_sequentials = new ArrayList<Boolean>();
	private static ArrayList<Long> updateRatios = new ArrayList<Long>();
	private static ArrayList<Long> update_querySet = new ArrayList<Long>();

	private static boolean b_incrementalViewMaintenance;
	private static boolean b_sequential;
	private static boolean importData;
	
	private static boolean usePruneTypecheck;
	private static boolean usePruneSubQuery;
	private static boolean useSubstitutionIndex;
	private static boolean b_querying;
	
	private static boolean headPrinted;
	
	private static String csvPath;
	private static ArrayList<String> rules = new ArrayList<String>();;
	private static String platform;
	
	private static long graphSize;
	private static long graphSelectivity;
	private static long schemaSize;
	private static long egdSize;
	private static long ivm_iteration;
	
	private static String viewType;
	private static long level;
	private static long rulesetId;
	private static long queryId;
	private static long updateRatio;
//	private static long updateQuerySet;
	
	private static long expIteration;
	private static long count = 0;
	
	private static boolean isVaryingScale;
	private static boolean isTypeChecking;
	private static boolean isExpParallel;
	private static boolean isExpParallelVarying;
	
	private static boolean isOnlyForCount;
	private static long expCount = 0;
	private static long subexpCount = 0;
	
	private static int indexOfPrepareCmd = -1;
	private static int indexOfCreateDBCmd = -1;
	
//	private static String csv_default;
//	private static String csv_neo4j_node;
//	private static String csv_neo4j_edge;
	

	public static ArrayList<Long> getUpdate_querySet() {
		return Executor.update_querySet;
	}
	
	public static void setTypeChecking(boolean isTypeChecking) {
		Executor.isTypeChecking = isTypeChecking;
	}
	
    public static void setB_querying(boolean b_querying) {
		Executor.b_querying = b_querying;
	}

	private static void setCSVPath(String csv) {
    	csvPath = csv;
    }
	
	public static void executeTypeChecking() {
		// TODO Auto-generated method stub
		System.out.println("[expTypeCheck]");
		
		rules = new ArrayList<String>();
		
		int maxRuleNumFactor = 5;
//		int maxPruningFactor = 2;
		int defaultPruningFactor = 1;
		int defaultRuleNumFactor = 5;
		int iteration = (int) expIteration;
		
		Boolean[] pruningOptions = {false, true}; //, true};
		Boolean[] isVaryingRuleExp = {true, false}; //, false};
		
		int firstRuleNumFactor;
		int lastRuleNumFactor;
//		int firstPruningFactorRate;
//		int lastPruningFactorRate;
		int firstPruningFactor;
		int lastPruningFactor;
		
		
		for (int it = 0; it < iteration; it++) {
			System.out.println("[expTypeCheck] iteration: " + (it+1) + " out of " + iteration);
			for (int s = 0; s < isVaryingRuleExp.length; s++) { // experiment A or B
				for (int j = 0; j < pruningOptions.length; j++) { // pruning or not
					if (isVaryingRuleExp[s] == true) {
						System.out.println("[expTypeCheck#" + s + "] Varying # of rules");
						firstRuleNumFactor = 1;
						lastRuleNumFactor = maxRuleNumFactor;
						firstPruningFactor = defaultPruningFactor;
						lastPruningFactor = defaultPruningFactor;
					} else {
						System.out.println("[expTypeCheck#" + s + "] Varying pruning rate");
						firstRuleNumFactor = defaultRuleNumFactor;
						lastRuleNumFactor = defaultRuleNumFactor;
						firstPruningFactor = defaultRuleNumFactor;
						lastPruningFactor = 0;
					}
					for (int i = firstRuleNumFactor; i <= lastRuleNumFactor; i++) {
	//					firstPruningFactor = i * firstPruningFactorRate / 100;
	//					lastPruningFactor = i * lastPruningFactorRate / 100;	
	//					if (isVaryingRuleExp[s] == true) {
	//						firstPruningFactor = i / 2;
	//						lastPruningFactor = i / 2;
	//					}
						for (int k = i-firstPruningFactor; k <= i-lastPruningFactor; k++) {
							if (k < 0) {
								throw new IllegalArgumentException("[Executor] k: " + k + " should be >= 0");
							}
							boolean pruning = pruningOptions[j];
							rules.clear();
	
							queryId = (isVaryingRuleExp[s] == true) ? 1 : 2;
							usePruneTypecheck = pruning;
				    		setRulesetId((int)Math.pow(2,i)*2); // # of transrules 
				    		updateRatio = (int)Math.pow(2,k)*2; // pruning ratio
				    		egdSize = 0;
				    		schemaSize = 0;
	
							System.out.println("[expTypeCheck#" + s + "] ruleFactor: " + i + " pruneFactor: " + k 
									+ " Pruning? " + pruningOptions[j] + " rulesetId(#ofRules): " + rulesetId + " " + " updateRatio(pruned): " + updateRatio);
	
							ExpTypeChecking.addRulesForTypeCheck(i, k, pruningOptions[j]);
				    		rules = ExpTypeChecking.getRules();
	//			    		System.out.println("rules: " + rules);
	
	//						
				    		runRules("SYN", rules.size());
						}
					}
				}
			}
		}
		
	}

    public static void run() {
    	if (isTypeChecking == true) {
    		executeTypeChecking();

    		return;
    	}
    	
    	for (int i = 0; i < 2; i++) {
    		if (i == 0) {
    			expCount = 0;
    		}
    		subexpCount = 0;
    		isOnlyForCount = (i == 0);
    		runPlatform();
    		
    		if (isOnlyForCount == true) { // Just to count # of experiments
    			System.out.println("[Executor] expCount: " + expCount);
    		}
    		

    		
//    		for (int j = 0; j < rules.size(); j++) {
//    			System.out.println("j: " + j + " => " + rules.get(j));
//    		}
//    		break;
    	}
    }
    
    private static void runPlatform() {
    	int k = 0;
    	for (String p : platforms) {
			int d = 0;
	    	rules = new ArrayList<String>();
    		platform = p;

//    		System.out.println("## platform: " + p + " platforms: " + platforms + " k: " + k);
    		k++;
    		runOptions(d);
    	}
    }
     
    private static void runOptions(int depth) {
		headPrinted = false;

    	for (int i = 0; i < b_typecheck_prune.size(); i++) {
    		for (int j = 0; j < b_subquery_prune.size(); j++) {
    			int d = depth;
    			usePruneTypecheck = b_typecheck_prune.get(i);
    			usePruneSubQuery = b_subquery_prune.get(j);

    	    	rules.add(d++, "# options");
    		    rules.add(d++, "option typecheck on");
				rules.add(d++, "option prunetypecheck " + (usePruneTypecheck ? "on" : "off"));
				rules.add(d++, "option prunequery " + (usePruneSubQuery ? "on" : "off"));
				rules.add(d++, "option ivm " + (b_incrementalViewMaintenance ? "on" : "off"));

				runGraphs(d);
    		}
    	}
    }
    
    public static ArrayList<String> getPlatforms() {
		return platforms;
	}

	public static ArrayList<String> getDatasets() {
		return datasets;
	}

	public static ArrayList<String> getViewtypes() {
		return viewtypes;
	}

	public static ArrayList<Boolean> getB_typecheck_prune() {
		return b_typecheck_prune;
	}

	public static ArrayList<Boolean> getB_useIndex() {
		return b_useIndex;
	}

	public static ArrayList<Boolean> getB_subquery_prune() {
		return b_subquery_prune;
	}

	public static long getGraphSelectivity() {
		return graphSelectivity;
	}

	public static long getUpdateRatio() {
		return updateRatio;
	}

	public static void setB_incrementalViewMaintenance(boolean b_incrementalViewMaintenance) {
		Executor.b_incrementalViewMaintenance = b_incrementalViewMaintenance;
	}

	public static void setImportData(boolean importData) {
		Executor.importData = importData;
	}

	public static void setUsePruneTypecheck(boolean usePruneTypecheck) {
		Executor.usePruneTypecheck = usePruneTypecheck;
	}

	public static void setUsePruneSubQuery(boolean usePruneSubQuery) {
		Executor.usePruneSubQuery = usePruneSubQuery;
	}

	public static void setUseSubstitutionIndex(boolean useSubstitutionIndex) {
		Executor.useSubstitutionIndex = useSubstitutionIndex;
	}

	public static void setCsvPath(String csvPath) {
		Executor.csvPath = csvPath;
	}

	public static void setGraphSize(long graphSize) {
		Executor.graphSize = graphSize;
	}

	public static void setRulesetId(long rulesetId) {
		Executor.rulesetId = rulesetId;
	}

	public static void setVaryingScale(boolean isVaryingScale) {
		Executor.isVaryingScale = isVaryingScale;
	}

	private static void runGraphs(int depth) {
 	    for (String graph : datasets) {
// 	    	System.out.println("[runGraph] graph: " + graph);
 	    	int d = depth;
 	    	
    		dataset = ExpConfig.getDataSets().get(graph);
 	    	rules.add(d++, "# init");

 	    	indexOfPrepareCmd = d;
 	    	System.out.println("indexOfPrepareCmd: " + indexOfPrepareCmd);
 	    	rules.add(d++, "prepare from");
 	    	
// 	    	if (platform.contentEquals("N4") == true) {
//	    		String csv_neo4j = dataset.getCsv_neo4j();
//
// 	 	    	rules.add(d++, "prepare from \"" + csv_neo4j + "\"");
// 	    	}
 	    	rules.add(d++, "connect " + platform.toLowerCase());
 	    	indexOfCreateDBCmd = d;
 	    	System.out.println("indexOfCreateDBCmd: " + indexOfCreateDBCmd);
 	    	rules.add(d++, "create graph exp"); //" + graph);
 	    	rules.add(d++, "use exp"); //" + graph);
    		addSchemaEgd(graph, d);    		
	    }
    }
    
    private static void setGraphAndSchemaEgd(String graph, int depth) {
    	int d = depth;

//    	System.out.println("[setGraphAndSchemaEgd] importData: " + importData + " dataset.isSynthetic(): " + dataset.isSynthetic() + " isOnlyForCount: " + isOnlyForCount);
    	String userDir = System.getProperty("user.dir");
    	System.out.println("userDir: " + userDir);
    	
    	if (importData == false) {
    		addView(graph, d);
    	} else {
	    	if (dataset.isSynthetic() == true) { // Synthetic graphs
	    		ArrayList<Long> sizes = new ArrayList<Long>(); 
	    		ArrayList<Long> selectivities = new ArrayList<Long>();

	    		if (isExpParallel == false) {
		    		if (isVaryingScale == true) {
		    			sizes.addAll(dataset.getSizes());
		    			selectivities.add(dataset.getBase_selectivity());
		    		} else { // selectivity
		    			sizes.add(dataset.getBase_size());
		    			selectivities.addAll(dataset.getSelectivities());	    			
		    		}
	    		} else {
	    			sizes.add(dataset.getBase_size());
	    			selectivities.add(dataset.getBase_selectivity());
	    		}
//	    		System.out.println("sizes: " + sizes + " selectivity: " + selectivities);
	    		
	    		int seed = 1234;
	    		if (b_incrementalViewMaintenance == true) { // IVM
	    			
	    			if (platform.contentEquals("N4") == true) {
	    				rules.set(indexOfPrepareCmd, "prepare from \"ivm\" on n4");
	    			} else {
	    				rules.set(indexOfPrepareCmd, "# not use prepare");
	    			}
	    			for (int k = 0; k < updateRatios.size(); k++) {
	    				long querySize = (Long)ExpConfig.get("ivm_iteration");
	    				updateRatio = updateRatios.get(k);
	    				long queryRatio = updateRatio * 100;    				
	    				
	    				for (int j = 0; j < update_querySet.size(); j++) {
		    				d = depth;
		    				queryId = update_querySet.get(j);
		    				if (isOnlyForCount == false) {
		    					GraphGenerator.createInputGraph(seed, dataset.getBase_size(), dataset.getBase_selectivity(),
		    							querySize, queryRatio, queryId, platform.contentEquals("N4"));
		    				}
	
	//	    		    	System.out.println("setGraph: " + graph + " updateRatio: " + updateRatio);
		    				if (platform.contentEquals("N4") == false) {
		    					userDir = userDir.replace("\\","/");
			    	    		rules.add(d++, "# import data from CSV w/ UpdateRatio: " + updateRatio); 
//			    	    		rules.add(d++, "import N from \"/home/ubuntu/src/graph-trans/data/" + graph + "_n.csv\"");
//			    	    		rules.add(d++, "import E from \"/home/ubuntu/src/graph-trans/data/" + graph + "_e.csv\"");

			    	    		rules.add(d++, "import N from \"" + userDir + "/data/node.csv\"");
			    	    		rules.add(d++, "import E from \"" + userDir + "/data/edge.csv\"");			    	    		
			    	    		
//			    	    		rules.add(d++, "import N from \"" + userDir + "/data/" + graph + "_n.csv\"");
//			    	    		rules.add(d++, "import E from \"" + userDir + "/data/" + graph + "_e.csv\"");			    	    		
//		    				} else { // neo4j
//		    					userDir = userDir.replace("\\","/");
//			    	    		rules.add(d++, "# import data from CSV w/ UpdateRatio: " + updateRatio); 
//			    	    		rules.add(d++, "import N from \"csv/node/node.csv\"");
//			    	    		rules.add(d++, "import E from \"csv/edge/edge.csv\"");			    	    		
		    				}
	    	    		
		    	    		addView(graph, d);
	    				}
	    			}
    	    	} else { // NON-IVM & SYN
    	    		if (isExpParallelVarying == true) {
	    	    		graphSize = sizes.get(0);
	    	    		graphSelectivity = selectivities.get(0);
	    	    		rules.set(indexOfPrepareCmd, "# prepare from");

	    	    		d = depth;
    			    	queryId = 0; //update_querySet.get(k);

//		    				if (isOnlyForCount == false && (Boolean)ExpConfig.get("useRealExecution") == true) {
//			    				GraphGenerator.createInputGraph(seed, graphSize, graphSelectivity, queryId, platform.contentEquals("N4"));
//		    	    		}
    					GraphGenerator.createInputGraph(seed, graphSize, graphSelectivity, 0, 0, 0, false, "data", true);
    					rules.add(d++, "# import data from CSV"); 
	    	    		rules.add(d++, "import N from \"" + userDir + "/data/node.csv\"");
	    	    		rules.add(d++, "import E from \"" + userDir + "/data/edge.csv\"");
    						    	    		    					
    					addView(graph, d);
//    	    			System.out.println("[ExpParallelVarying] DOING");
    	    		} else { // not parallel varying
			    		for (int i = 0; i < sizes.size(); i++) {
			    			for (int j = 0; j < selectivities.size(); j++) {
			    	    		graphSize = sizes.get(i);
			    	    		graphSelectivity = selectivities.get(j);
			    	
		    			    	d = depth;
		    			    	queryId = 0; //update_querySet.get(k);
	 
	//		    				if (isOnlyForCount == false && (Boolean)ExpConfig.get("useRealExecution") == true) {
	//			    				GraphGenerator.createInputGraph(seed, graphSize, graphSelectivity, queryId, platform.contentEquals("N4"));
	//		    	    		}
			    				
			    				String preparedDB = "SYN-" + graphSize + "-" + graphSelectivity;
			    	    		System.out.println("preparedDB: " + preparedDB);
			    	    		rules.set(indexOfPrepareCmd, "prepare from \"" + preparedDB + "\" on " + platform.toLowerCase());
			    	    		rules.set(indexOfCreateDBCmd, "# will not created DB");
	//		    	    		
	//		    	    		rules.add(d++, "# import data from CSV"); 
	//		    	    		rules.add(d++, "import N from \"" + userDir + "/data/" + graph + "_n.csv\"");
	//		    	    		rules.add(d++, "import E from \"" + userDir + "/data/" + graph + "_e.csv\"");
		    	    		
			    	    		addView(graph, d);
		    	    		}
	    				}
    	    		}
	    		}
	    	} else { // not a synthetic graph
	    		// import dataset
				String preparedDB = graph.toLowerCase();
	    		System.out.println("preparedDB: " + preparedDB);
	    		rules.set(indexOfPrepareCmd, "prepare from \"" + preparedDB + "\" on " + platform.toLowerCase());
	    		rules.set(indexOfCreateDBCmd, "# will not created DB");
	    		
//	    		String csv_default = dataset.getCsv_default();
//	    		rules.add(d++, "# import data from CSV for dataset [" + graph +"]"); 
//	    		rules.add(d++, "import N from \"" + userDir + "/" + csv_default + "/node.csv\"");
//	    		rules.add(d++, "import E from \"" + userDir + "/" + csv_default + "/edge.csv\"");
//	    		rules.add(d++, "import NP from \"" + userDir + "/" + csv_default + "/nodeProp.csv\"");
//	    		rules.add(d++, "import EP from \"" + userDir + "/" + csv_default + "/edgeProp.csv\"");
	    		
	        	addView(graph, d);
	    	}
    	}
    }
    
    private static void addSchemaEgd(String graph, int depth) {
//    	System.out.println("addSchemaEgd: " + graph + " depth: " + depth);
    	if (dataset == null) { // ALL
    		for (String g : datasets) {
    			addSchemaEgdFromDataset(g, depth); 
    		}
    	} else {
			addSchemaEgdFromDataset(graph, depth); 
    	}
    }

    private static void addSchemaEgdFromDataset(String graph, int d) {
		rules.add(d++, "# schema");
		for (String s : dataset.getSchemas()) {
			rules.add(d++, s);
		}
		rules.add(d++, "# constraints (EGDs)");
		for (String s : dataset.getEgds()) {
			rules.add(d++, "add constraint " + s);
		}
		schemaSize = dataset.getSchemas().size();
		egdSize = dataset.getEgds().size();
		
	 	setGraphAndSchemaEgd(graph, d);
    }
    
    private static void addView(String graph, int depth) {
//    	System.out.println("addView: " + graph + " depth: " + depth);
    	
    	if (isExpParallelVarying == true) {

			int ruleNum = 16;
			viewType = "MV";

		 	String r1 = "{match b:B-e5:X<pp>->c:C, c:C-e2:X<pp>->d:D map (c,d) to s:S<pp>}"; 
		 	String r2 = "{match b:B-e6:X<pp>->c:C, c:C-e2:Y<pp>->d:D map (c,d) to t:T<pp>}";
			int d = depth; 
		    rules.add(d++, "option typecheck off");

			int depth2 = d;

			for (int i = 0; i <= 4; i++) {
				d = depth2;
				int ruleEachStratum = (int) Math.pow(2, i-1);
				System.out.println("[Executor] ruleEachStratum: " + ruleEachStratum);
				for (int j = 0; j < ruleNum / ruleEachStratum; j++) { // each view
					String rule = "create materialized view v" + j + " ";
					if (j > 0) {
						rule += "on v" + (j-1) + " ";
					}
					rule += "as ";

					for (int k = 0; k < ruleEachStratum; k++) { // rules in the view
						int ruleIdx = j * ruleEachStratum + k;
						String ruleToAdd;
						if (ruleIdx % 2 == 0) {
							ruleToAdd = r1.replaceAll("<pp>", Integer.toHexString(ruleIdx).toUpperCase());
						} else {
							ruleToAdd = r2.replaceAll("<pp>", Integer.toHexString(ruleIdx).toUpperCase());
						}
						rule += ruleToAdd;
						if (k + 1 == ruleEachStratum) {
							rule += "";
						} else {
							rule += ",";
						}
//						rule += "\n";
//						System.out.println("[Executor] j: " + j + " k: " + k);
					}
					rules.add(d++, rule);
					System.out.println("[Executor] rule: " + rule);    							
				}
		    	addIndex(graph, d);
			}
			return;
    	}
    	for (int i = 0; i < dataset.getTransrules().size(); i++) {
//    		System.out.println("[addView] graph: " + graph + " i: " + i + " dataset.getTransrules().size(): " + dataset.getTransrules().size());
	    	for (String s : viewtypes) {
	    		for (boolean b : b_sequentials) {
//	    			System.out.println("[addView] b_sequential: " + b + " isExpParallel: " + isExpParallel);
	    			int d = depth; 

	    			b_sequential = b;
		    		viewType = s;
		    		level = 1;
		    		rulesetId = i + 1;
		    		
		    		String v = "virtual";
		    		if (s.contentEquals("MV") == true) {
		    			v = "materialized";
		    		} else if (s.contentEquals("HV") == true) {
		    			v = "hybrid";
		    		} else if (s.contentEquals("ASR") == true) {
		    			v = "asr";
		    		}
		    		rules.add(d++, "# views");
		    		
		    		ArrayList<ArrayList<String>> arr1 = dataset.getTransrules().get(i);

		    		boolean hasParallel = false;
    				for (int j = 0; j < arr1.size(); j++) {
	    				// check if it is parallel, so that there is corresponding sequential execution 
    					if (arr1.get(j).size() > 1) {
    						hasParallel = true;
    						break;
    					}
    				}
    				
//	    			System.out.println("[addView] graph: " + graph + " b_sequentials: " + b_sequentials + " b: " + b + " hasParallel: " + hasParallel);

    				/*
    				 */
 
    				if (isExpParallel == true && hasParallel == false) {
    					continue;
    				}
//	    			System.out.println("[addView] passed");

	    			
	    			if (b_sequential == true) {
//	    				System.out.println("	b_sequtial: " + b_sequential + " hasParallel: " + hasParallel);
	    				if (hasParallel == false) {
	    					continue;
	    				} else { // sequential=true
	    					int viewId = 0;
	    					for (int j = 0; j < arr1.size(); j++) {
	    						for (int k = 0; k < arr1.get(j).size(); k++) {
	    							String rule = "create " + v + " view v" + viewId;
	    							if (viewId > 0) {
	    								rule = rule + " on v" + (viewId-1);  
	    							} 
	    							rule = rule + " as " + arr1.get(j).get(k);
//	    							System.out.println("[" + dataset.getName() + "_" + rulesetId + "] b_sequential[" + b_sequential + "] rule: " + rule);
			    					rules.add(d++, rule);
			    					viewId++;
	    						}
	    					}
	    				}
//	    				System.out.println("[addView] runSeq");
	    			} else { // parallel (sequential=false)
//	    				if (isExpParallel == true) {
//	    					continue;
//	    				}
	    				long l_max = 1;
	    				if (isTypeChecking == true) {
	    					l_max = (Long)ExpConfig.get("queryIteration");
	    				}
						for (int l = 0; l < l_max; l++) {
		    				for (int j = 0; j < arr1.size(); j++) {
	
								String rule = "create " + v + " view v" + j;
								if (isTypeChecking == true) {
									rule = rule +  ("i" + l);
								}
								if (j > 0) {
		    						rule = rule + " on v" + (j-1);
		    						if (isTypeChecking == true) {
		    							rule = rule + "i" + l;
		    						}
		    					}
		    					rule = rule + " as ";
		    					for (int k = 0; k < arr1.get(j).size(); k++) {
		    						if (k > 0) {
		    							rule = rule + ", ";
		    						}
		    						rule = rule + arr1.get(j).get(k);
		    					}
//		    					System.out.println("[" + dataset.getName() + "_" + rulesetId + "] b_sequential[" + b_sequential + "] rule: " + rule);
		    					rules.add(d++, rule);
							}
						}
//						System.out.println("[addView] runPar");
		    		}
			    	addIndex(graph, d);
				}
    		}
    	}
     }
    
    private static void addIndex(String graph, int depth) {
    	int d = depth; 

//    	System.out.println("viewType: " + viewType);
    	if (viewType.contentEquals("SSR") == true) {
	    	rules.add(d++, "# indexes");
	    	int viewId = dataset.getTransrules().get((int)rulesetId-1).size();
	    	rules.add(d++, "create " + viewType.toLowerCase() + " on v" + (viewId-1));
    	} 
    	
//    	for (int i = 0; i < b_useIndex.size(); i++) {
//        	int d = depth; 
//    		
//    		useSubstitutionIndex = b_useIndex.get(i);
////        	System.out.println("addIndex: " + graph + " depth: " + depth + " useSubstitutionIndex: " + useSubstitutionIndex);
//
//    		if (useSubstitutionIndex == true) {
//    	    	rules.add(d++, "# indexes");
//    	    	
//    	    	int viewId = dataset.getTransrules().get((int)rulesetId-1).size();
//    	    	rules.add(d++, "create index on v" + (viewId-1));
//    		}
//
////        	System.out.println("addIndex");
       	addQuery(graph, d);
//    	}
    }
    
    private static void addQuery(String graph, int depth) {
    	int d = depth; 

//    	System.out.println("addQuery b_querying: " + b_querying);
    	if (b_querying == true) {
    		if (useSubstitutionIndex == true && viewType.contentEquals("MV") == false && b_incrementalViewMaintenance == false) {
    			queryId = 0;
        		dropGraphAndRunRules(graph, d);
    		} else {
    			if (b_incrementalViewMaintenance == false) {
    				ArrayList<String> queries = dataset.getQueries().get((int)(rulesetId-1));
    				for (int i = 0; i < queries.size(); i++) {
    					d = depth; 
	    				queryId = i + 1;
	    				rules.add(d++, "# query");
	    				for (int j = 0; j < (Long)ExpConfig.get("queryIteration"); j++) {
	    					rules.add(d++, queries.get(i));
	    				}
	    	    		dropGraphAndRunRules(graph, d);
	    			}
    			} else { // Incremental View Maintenance 
	    			ArrayList<String> queries = GraphGenerator.getQueries();

	    			rules.add(d++, "# query and update (#: " + queries.size() + ")");
	    			for (int i = 0; i < queries.size(); i++) {
//	    				System.out.println("query i: " + i + " => " + queries.get(i));
	    				rules.add(d++, queries.get(i));
	    			}
    	    		dropGraphAndRunRules(graph, d);
    			}
    		}
    	} else { // this is for create view/index only
    		queryId = 0;
    		dropGraphAndRunRules(graph, d);
    	}
    }
    
    public static void dropGraphAndRunRules(String graph, int depth) {
    	int d = depth;
    	
    	rules.add(d++, "drop exp");//" + graph);
//    	if (platform.contentEquals("N4") == true) {
// 	    	rules.add(d++, "server stop");
// 	    	rules.add(d++, "delete " + graph);
//    	}
    	rules.add(d++, "disconnect");
    	
//    	System.out.println("[dropGraphAndRunRules]"); 
		
    	System.out.println("isTypeChecking: " + isTypeChecking);
		if (Executor.isTypeChecking == true) {
			rules.set(indexOfPrepareCmd, "# not use prepare");
		}

		
    	for (int i = 0; i < expIteration; i++) {
    		runRules(graph, d);
    	}    	
    }
        
    private static void runRules(String graph, int depth) {
    	if (isOnlyForCount == true) {
    		expCount++;
    		return;
    	} else {
    		subexpCount++;
    	}
    	
		Config.initialize();
		Config.setTypeCheckEnabled(true);
		
		GraphTransServer.initialize(); // after setting Config 

		Util.Console.setEnable((Boolean)ExpConfig.get("printConsole"));
		Util.setConsole(false);

		Console console = new Console();

		Performance.setup(graph, graph + " workload");		

		CommandExecutor.setConsole(console);

		int tttid = Util.startTimer();
    	for (int i = 0; i < depth; i++) {
//    		if (rules.size() < i) {
//    			System.out.println("rules.size(): " + rules.size() + " i: " + i);
//    		}
//    		if (rules.get(i) == null) {
//    			System.out.println("rules.get(i:" + i + " depth: " + depth + ") is null");
//    		}
			if (rules.get(i).trim().substring(0,1).contentEquals("#") == false) {
				if ((Boolean)ExpConfig.get("useRealExecution") == true) {
					CommandExecutor.run(rules.get(i));
				}
				if ((Boolean)ExpConfig.get("printRules") == true) {
					if ((Boolean)ExpConfig.get("printTiming") == true) {
						System.out.println(rules.get(i) + "; (" + Util.getElapsedTime(tttid) + " ms)");
					} else {
						System.out.println(rules.get(i) + ";");
					}
				}
			} else {
				if ((Boolean)ExpConfig.get("printRules") == true) {
					System.out.println(rules.get(i) + ";");					
				}
			}
    	}
		// For recording performance
		Performance.setCSVPath(csvPath);
		Performance.setPlatform(platform);
		Performance.setViewType(viewType);
		
		Performance.setPruneSubquery(usePruneSubQuery);
		Performance.setPruneTypecheck(usePruneTypecheck);
		Performance.setUseSubstituteIndex(useSubstitutionIndex);
//		Performance.setLevel(GraphTransServer.getNumTransRuleList());
//		Performance.setLevel(level);	
		
		Performance.setUseSequential(b_sequential);
		Performance.setEgdSize(egdSize);
		Performance.setSchemaSize(schemaSize);
		Performance.setGraphSize(graphSize);
		Performance.setGraphSelectivity(graphSelectivity);
		
		Performance.setRulesetId(rulesetId);
		Performance.setQueryId(queryId);
//		Performance.setSchemaSize(Schema.getSchemaNodes().size() + Schema.getSchemaEdges().size());
		Performance.setUpdateRatio(updateRatio);
		
		if (isTypeChecking == true) {
    		Performance.setPlatform("LB");
    		Performance.setViewType("MV");
		}
	
		Performance.setJSON();
		
		System.out.println("[expNo " + subexpCount + "/" + expCount + "] totalcount[" + (++count) + "] perf: " + CommandExecutor.getPerformance());
    	
		if (headPrinted == false && (new File(csvPath)).exists() == false) {
			Performance.printCSVHead();
			headPrinted = true;
		}
		Performance.printCSVBody();
		
    	System.gc();
    	System.gc();
    	
    	try {
    		if (isTypeChecking == true) {
    			Thread.sleep(300);
    		} else {
    			Thread.sleep(2000);
    		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
  
	public static void initialize() {
		datasets.clear();
		platforms.clear();
		viewtypes.clear();
		b_typecheck_prune.clear();
		b_useIndex.clear();
		b_subquery_prune.clear();
		b_sequentials.clear();

		update_querySet.clear();

		b_typecheck_prune.clear();
		b_useIndex.clear();
		b_subquery_prune.clear();
		b_incrementalViewMaintenance = false;
	    b_querying = false;
		b_sequential = false;
		importData = false;
		
		isVaryingScale = false;
		isTypeChecking = false;
		isExpParallel = false;
		
		setExpIteration((Long)ExpConfig.get("expIteration"));
	}

	public static void setExpIteration(long expIteration) {
		Executor.expIteration = expIteration;
	}

	public static ArrayList<Boolean> getB_sequentials() {
		return b_sequentials;
	}

	public static ArrayList<Long> getUpdateRatios() {
		return updateRatios;
	}

	public static void setIvm_iteration(long ivm_iteration) {
		Executor.ivm_iteration = ivm_iteration;
	}

	public static void setExpParallel(boolean isExpParallel) {
		Executor.isExpParallel = isExpParallel;
	}

	public static void setExpParallelVarying(boolean isExpParallelVarying) {
		Executor.isExpParallelVarying = isExpParallelVarying;
	}
}




/*
CREATE MATERIALIZED VIEW IDX_v0_N_T AS 
(
SELECT DISTINCT R2._0 AS _0, R2._3 AS _1, R0._0 AS _2, R0._1 AS _3, R1._0 AS _4, R1._1 AS _5 
FROM N_v0 AS R0 
	,N_v0 AS R1 
	,E_v0 AS R2 
WHERE R0._0 = R2._1 AND R1._0 = R2._2 AND R0._1 = 'T'
)
UNION
(
SELECT DISTINCT R2._0 AS _0, R2._3 AS _1, R0._0 AS _2, R0._1 AS _3, R1._0 AS _4, R1._1 AS _5 
FROM N_v0 AS R0 
	,N_v0 AS R1 
	,E_v0 AS R2 
WHERE R0._0 = R2._1 AND R1._0 = R2._2 AND R1._1 = 'T'
)
;
 */



//[ERROR] query: CREATE MATERIALIZED VIEW IDX_v0_N_T AS ((SELECT DISTINCT R2._0 AS _0, R2._3 AS _1, R0._0 AS _2, R0._1 AS _3, R1._0 AS _4, R1._1 AS _5 FROM N_v0 AS R0 CROSS JOIN N_v0 AS R1 CROSS JOIN E_v0 AS R2 WHERE R0._0 = R2._1 AND R1._0 = R2._2 AND R0._1 = 'T') UNION (SELECT DISTINCT R2._0 AS _0, R2._3 AS _1, R0._0 AS _2, R0._1 AS _3, R1._0 AS _4, R1._1 AS _5 FROM N_v0 AS R0 CROSS JOIN N_v0 AS R1 CROSS JOIN E_v0 AS R2 WHERE R0._0 = R2._1 AND R1._0 = R2._2 AND R1._1 = 'T')); msg: An I/O error occurred while sending to the backend.
//[ERROR] query: CREATE MATERIALIZED VIEW IDX_v0_N_S AS ((SELECT DISTINCT R2._0 AS _0, R2._3 AS _1, R0._0 AS _2, R0._1 AS _3, R1._0 AS _4, R1._1 AS _5 FROM N_v0 AS R0 CROSS JOIN N_v0 AS R1 CROSS JOIN E_v0 AS R2 WHERE R0._0 = R2._1 AND R1._0 = R2._2 AND R0._1 = 'S') UNION (SELECT DISTINCT R2._0 AS _0, R2._3 AS _1, R0._0 AS _2, R0._1 AS _3, R1._0 AS _4, R1._1 AS _5 FROM N_v0 AS R0 CROSS JOIN N_v0 AS R1 CROSS JOIN E_v0 AS R2 WHERE R0._0 = R2._1 AND R1._0 = R2._2 AND R1._1 = 'S')); msg: This connection has been closed.

