package edu.upenn.cis.db.graphtrans.experiment;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import edu.upenn.cis.db.helper.Util;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

/**
 * For query rewriting for Indexing
 * 10/14/2020
 * @author sbnet21
 *
 */
public class GraphGenerator {
	static int nid = 0;
	static int eid = 0;
	
	private static boolean isForNeo4j = false;
	
	private static ArrayList<Pair<Integer, Boolean>> workload = new ArrayList<Pair<Integer, Boolean>>();
	private static int indexWorkload = 0;
	private static ArrayList<String> queries = new ArrayList<String>();
	
	private static StringBuilder str = new StringBuilder();
	private static HashSet<Integer> selectedIter = new HashSet<Integer>();
	private static long indexRatio = 5000; // 100 is 1%
	private static long maxIter = 0;
	private static Random pickEidRand;
	private static Random pickQueryRand;
	
	private static long querySet = -1;
	
	private static ArrayList<Integer> selectedIterArr = new ArrayList<Integer>();
	
	private static int getNextNid() {
		return nid++;
	}

	private static int getNextEid() {
		return eid++;
	}

	private static String getLabel(String base, String postfix) {
		StringBuilder str = new StringBuilder();
		str.append("\"").append(base).append(postfix).append("\"");
		return str.toString();
	}

	public static void addNodeStr(int nid, String label, String postfix) {
		str.append(nid).append(",").append(getLabel(label, postfix));
		if (isForNeo4j == true) {
			str.append(",0,0,99"); // level=0,c=0,d=99
		}
		str.append("\n");
	}

	//int eid, int from, int to, String label, 
	public static void addEdgeStr(int eid, int from, int to, String label, String postfix) {
		Pair<Integer, Boolean> p = null;
//		System.out.println("indexWorkload: " + indexWorkload + " workload.size(): " + workload.size());
//		Triple<String, String, String> labels = null;
		Tuple5<String, String, String, String, String> labels = null;
		while(true) {
			if (indexWorkload >= workload.size()) {
				break;
			}
			p = workload.get(indexWorkload);
			if (p.getRight() == true) { // query
				indexWorkload++;
				// #match b:B-e:X->s:S from v1 where b.id > 10000, b.id < 10050 return b;
				labels = getEdgeLabels(p.getLeft());
				String query = null;
				
				int int_random = pickEidRand.nextInt(selectedIterArr.size());
				int eid2 = selectedIterArr.get(int_random) * 8 + 3;

//				query = "match c:B-e:X->d:S, d-e2:X->f:T, f-e3:X->g:F ";
//				query = query + " from v0 where e.id = " + eid2 + " return c-e->d, d-e2->f, f-e3->g";
 
				if (querySet == 1) { // 4-hops
					query = "match d:S";
					query = query + " from v0 where d > 10000000050, d < 10000000060 return d"; // = " + eid2 + " return c-e->d, d-e2->f1, f1-e3->f2, f2-e4->g";
				} else if (querySet == 2) {
					query = "match d:S-e:X->d:S";
					query = query + " from v0 where e > " + eid2 + ", e < " + (eid2 + 10) + " return e";
				} else if (querySet == 3) { // 1-hops
//					query = "match b:B-e:X->d:S, d-e2:X->d"; // , b-e1:X->t:T, t-e2:Y->t
//					query = query + " from v0 where "
//							+ "e2 > " + eid2 + ", e2 < " + (eid2+30) + " return e";		
					query = "match b:B-e:X->d:S, b:B-e1:X->t:T, d-e2:X->d, t-e3:Y->t"; // , b-e1:X->t:T, t-e2:Y->t
					query = query + " from v0 return e"; //where "
//							+ "e2 > " + eid2 + ", e2 < " + (eid2+30) + " return e";		
//					query = "match b:B-e:X->d:S, d-e2:X->d ";
//					query = query + " from v0 where "
//							+ "e2 > " + eid2 + ", e2 < " + (eid2+30) + " return e";
				}			
//				
//				query = "match d:S";
//				query = query + " from v0 where d > 1000000 return d"; // = " + eid2 + " return c-e->d, d-e2->f1, f1-e3->f2, f2-e4->g";

				/*
				if (labels.getT4().contentEquals("") == true) {
					query = "match c:" + labels.getT1() + "-e:" + labels.getT2() + "->d:" + labels.getT3();
					query = query + " from v0 where e.id = " + p.getLeft() + " return c-e->d";
				} else {
					query = "match c:" + labels.getT1() + "-e:" + labels.getT2() + "->d:" + labels.getT3() + ", ";
					query = query + "d-e2:" + labels.getT4() + "->f:" + labels.getT5() + " ";
					query = query + " from v0 where e.id = " + p.getLeft() + " return c-e->d, d-e2->f";
				}
				*/
				queries.add(query);
//				System.out.println("[Q] eid: " + eid + " p.getLeft: " + p.getLeft() + " p: " + p + " indexWorkload: " + indexWorkload);

			} else { // update
				break;
			}
		} 

//		if (p != null) {
//			System.out.println("[U] eid: " + eid + " p.getLeft: " + p.getLeft() + " p: " + p + " indexWorkload: " + indexWorkload);
//		}
		
		if (p != null && eid == p.getLeft()) {
			// insert E (101,1,2,"X");
			queries.add("insert E (" + eid + "," + from + "," + to + "," + getLabel(label, postfix) + ")");
//			System.out.println("[UUUU] eid: " + eid + " p.getLeft: " + p.getLeft() + " p: " + p + " indexWorkload: " + indexWorkload);

			indexWorkload++;
		} else {
			str.append(eid).append(",").append(from)
				.append(",").append(to).append(",").append(getLabel(label, postfix));
			if (isForNeo4j == true) {
				str.append(",0,0,99"); // level=0,c=0,d=99
			}
			str.append("\n");
		}

		while(true) {
			if (indexWorkload >= workload.size()) {
				break;
			}
			p = workload.get(indexWorkload);
			if (p.getRight() == true) { // query
				indexWorkload++;
				// #match b:B-e:X->s:S from v1 where b.id > 10000, b.id < 10050 return b;
				labels = getEdgeLabels(p.getLeft());

				int int_random = pickEidRand.nextInt(selectedIterArr.size());
				int eid2 = selectedIterArr.get(int_random) * 8 + 3;
				
				String query = null;
				
//				if (querySet == 1) { // 4-hops
//					query = "match c:B-e:X->d:S, d-e2:X->f1:T, f1-e3:Y->f2:T, f2-e4:X->g:F ";
//					query = query + " from v0 where e = " + eid2 + " return c-e->d, d-e2->f1, f1-e3->f2, f2-e4->g";
//				} else if (querySet == 2) {
//					int rand = pickQueryRand.nextInt(3);
//					if (rand % 3 == 0) { // 3-hops
//						query = "match c:B-e:X->d:S, d-e2:X->f:T, f-e3:X->g:F ";
//						query = query + " from v0 where e = " + eid2 + " return c-e->d, d-e2->f, f-e3->g";
//					} else if (rand % 3 == 1) { // 2-hops
//						query = "match c:B-e:X->d:S, d-e2:X->f:T ";
//						query = query + " from v0 where e = " + eid2 + " return c-e->d, d-e2->f";
//					} else if (rand % 3 == 2) { // 1-hop
//						query = "match c:B-e:X->d:S ";
//						query = query + " from v0 where e = " + eid2 + " return c-e->d";
//					}
//				} else if (querySet == 3) { // 1-hops
//					query = "match c:B-e:X->d:S ";
//					query = query + " from v0 where e = " + eid2 + " return c-e->d";
//				}
				if (querySet == 1) { // 4-hops
					query = "match d:S";
					query = query + " from v0 where d > 10000000050, d < 10000000060 return d"; // = " + eid2 + " return c-e->d, d-e2->f1, f1-e3->f2, f2-e4->g";
				} else if (querySet == 2) {
					query = "match d:S-e:X->d:S";
					query = query + " from v0 where e > " + eid2 + ", e < " + (eid2 + 10) + " return e";
				} else if (querySet == 3) { // 1-hops
					query = "match b:B-e:X->t:T, t-e2:Y->t"; // , b-e1:X->t:T, t-e2:Y->t
					query = query + " from v0 return e"; // where "
//							+ "e2 > " + eid2 + ", e2 < " + (eid2+3000) + " return e";					
//					query = "match b:B-e:X->d:S, d-e2:X->d ";
//					query = query + " from v0 where "
//							+ "e2 > " + eid2 + ", e2 < " + (eid2+30) + " return e";
				}			
				queries.add(query);
//				System.out.println("[Q] eid: " + eid + " p.getLeft: " + p.getLeft() + " p: " + p + " indexWorkload: " + indexWorkload);
			} else { // update
				break;
			}
		} 
		
//		System.out.println("queries:" + queries);
	}
	
	public static void setQueryWorkLoad(long seed, long size, long querySize, long queryRatio) {
		int tid = Util.startTimer();
//		System.out.println("[getQueryWorkLoad] seed[" + seed + "] size[" + size + "] querySize[" + querySize + "] queryRatio[" + queryRatio + "]");
		
		queries.clear();
		indexWorkload = 0;
		workload.clear();
		
		Random rand = new Random(seed+5891); //instance of random class
		long updateSize = querySize * (10000 - queryRatio) / 10000;
		HashSet<Integer> updateIds = new HashSet<Integer>();
		
//		System.out.println("[getQueryWorkLoad] updateSize[" + updateSize + "]");
		
		
		int int_random;
		for (int i = 0; i < updateSize; i++) {
			int_random = rand.nextInt((int)size);
			updateIds.add(int_random);
		}
		
		while(updateIds.size() < updateSize) {
			int_random = rand.nextInt((int)size);
			updateIds.add(int_random);
		}
		
//		System.out.println("[getQueryWorkLoad] updateIds[" + updateIds.size() + "]");

		ArrayList<Integer> updates = new ArrayList<Integer>();
		
		updates.addAll(updateIds);
		Collections.sort(updates);		
		
		int indexUpdates = 0;
		int indexQueries = 0;
		selectedIterArr.addAll(selectedIter);
//		System.out.println("selectedIterArr: " + selectedIterArr.size());
		
		while(indexQueries + indexUpdates < querySize) {
			int_random = rand.nextInt(10000);
			if (int_random < queryRatio) { // query 
				if (indexQueries < querySize - updateSize) {
//					System.out.println("##query");
					int_random = rand.nextInt(10000);
					if (int_random < indexRatio) { // choose from index
						int_random = rand.nextInt(selectedIterArr.size());
//						System.out.println("FROM ARR: " + selectedIterArr.get(int_random));
						workload.add(Pair.of((selectedIterArr.get(int_random) * 8) + rand.nextInt(4) + 3, true));
//						System.out.println("##val: " + ((selectedIterArr.get(int_random) * 8) + rand.nextInt(4) + 3));
					} else {
						int_random = rand.nextInt((int)size);
						workload.add(Pair.of(int_random, true));
					}
					indexQueries++;
				}
			} else { // update
				if (indexUpdates < updateSize) {
//					System.out.println("##update");
					workload.add(Pair.of(updates.get(indexUpdates++), false));
				}
			}
		}
		
		Util.Console.logln("[GraphGenerator/WorkloadGen] Elapsed time: " + Util.getElapsedTime(tid) + " querySize[" + querySize + "] queryRatio[" + queryRatio + "] workload[" + workload.size() + "]");

//		for (int i = 0; i < workload.size(); i++) {
//			System.out.println("[WORKLOAD] i: " + i + " => " + workload.get(i));
//		}
	}
	
	public static void createInputGraph(long seed, long size, long rate, long querySet, boolean neo4j) {
		createInputGraph(seed, size, rate, 0, 0, querySet, neo4j);
	}
	
	public static Tuple5<String, String, String, String, String> getEdgeLabels(int eid) {		
		int c = eid % 8;
		int iter = eid / 8;
		Tuple5<String, String, String, String, String> labels = null;

		String postfix = "_";
		
//		System.out.println(selectedIter);
//		System.out.println("eid: " + eid + " c: " + c + " iter: " + iter + " selectedIter.contains(iter): " + selectedIter.contains(iter));
		
		if (selectedIter.contains(iter) == true) {
			postfix = "";
		}
		
		switch(c) {
		case 0:
		case 1:
		case 2:
			// A-[X]-B-[X]-S
			// _A-[X]-_B-[X]-_C 
			if (postfix.contentEquals("_") == true) {
				labels = Tuples.of("A"+postfix, "X"+postfix, "B"+postfix, "X"+postfix, "C"+postfix);
			} else {
				labels = Tuples.of("A"+postfix, "X"+postfix, "B"+postfix, "X"+postfix, "S"+postfix);
			}
			break;
		case 3:
			if (postfix.contentEquals("_")) {
				labels = Tuples.of("B"+postfix, "X"+postfix, "C"+postfix, "X"+postfix, "D"+postfix);
			} else {
				labels = Tuples.of("B"+postfix, "X"+postfix, "S"+postfix, "X"+postfix, "T"+postfix);
			}
			break;
		case 4:
			if (postfix.contentEquals("_")) {
				labels = Tuples.of("C"+postfix, "X"+postfix, "D"+postfix, "X"+postfix, "C"+postfix);
			} else {
				labels = Tuples.of("S"+postfix, "X"+postfix, "S"+postfix, "X"+postfix, "T"+postfix);
			}
			break;
		case 5:
			if (postfix.contentEquals("_")) {
				labels = Tuples.of("D"+postfix, "X"+postfix, "C"+postfix, "X"+postfix, "D"+postfix);
			} else {
				labels = Tuples.of("S"+postfix, "X"+postfix, "T"+postfix, "X"+postfix, "T"+postfix);
			}
			break;
		case 6:
			if (postfix.contentEquals("_")) {
				labels = Tuples.of("C"+postfix, "Y"+postfix, "D"+postfix, "X"+postfix, "F"+postfix);
			} else {
				labels = Tuples.of("T"+postfix, "Y"+postfix, "T"+postfix, "X"+postfix, "F"+postfix);
			}
			break;
		case 7:
			if (postfix.contentEquals("_")) {
				labels = Tuples.of("D"+postfix, "X"+postfix, "F"+postfix, "", "");			
			} else {
				labels = Tuples.of("S"+postfix, "X"+postfix, "F"+postfix, "", "");			
			}
			break;			
		}		
		return labels;
	}
	
//	public static Triple<String, String, String> getEdgeLabelsTriple(int eid) {
//		int c = eid % 8;
//		int iter = eid / 8;
//		Triple<String, String, String> labels = null;
//		String postfix = "_";
//		
////		System.out.println(selectedIter);
////		System.out.println("eid: " + eid + " c: " + c + " iter: " + iter + " selectedIter.contains(iter): " + selectedIter.contains(iter));
//		
//		if (selectedIter.contains(iter) == true) {
//			postfix = "";
//		}
//		
//		switch(c) {
//		case 0:
//			// A-[X]-B-[X]-S
////			if (postfix.contentEquals("_") == true) {
////				labels = Tuples.of("A"+postfix, "X"+postfix, "B"+postfix, "X"+postfix, "C"+postfix);
////			} else {
////				
////			}
//			labels = Triple.of("A"+postfix, "B"+postfix, "X"+postfix);
//			break;
//		case 1:
//			labels = Triple.of("A"+postfix, "B"+postfix, "X"+postfix);
//			break;
//		case 2:
//			labels = Triple.of("A"+postfix, "B"+postfix, "X"+postfix);
//			break;
//		case 3:
//			if (postfix.contentEquals("_")) {
//				labels = Triple.of("B"+postfix, "C"+postfix, "X"+postfix);
//			} else {
//				labels = Triple.of("B"+postfix, "S"+postfix, "X"+postfix);
//			}
//			break;
//		case 4:
//			if (postfix.contentEquals("_")) {
//				labels = Triple.of("C"+postfix, "D"+postfix, "X"+postfix);
//			} else {
//				labels = Triple.of("S"+postfix, "S"+postfix, "X"+postfix);
//			}
//			break;
//		case 5:
//			if (postfix.contentEquals("_")) {
//				labels = Triple.of("D"+postfix, "C"+postfix, "X"+postfix);	
//			} else {
//				labels = Triple.of("S"+postfix, "T"+postfix, "X"+postfix);
//			}
//			break;
//		case 6:
//		case 7:
//			if (postfix.contentEquals("_")) {
//				labels = Triple.of("C"+postfix, "D"+postfix, "Y"+postfix);
//			} else {
//				labels = Triple.of("T"+postfix, "T"+postfix, "Y"+postfix);
//			}
//			break;
//		}
////		addEdgeStr(e01, a1, b1, "X", postfix);
////		addEdgeStr(e02, a2, b1, "X", postfix);
////		addEdgeStr(e03, a3, b1, "X", postfix);
////		addEdgeStr(e04, b1, c1, "X", postfix);
////		addEdgeStr(e05, c1, d1, "X", postfix);
////		addEdgeStr(e06, d1, c2, "X", postfix);
////		addEdgeStr(e07, c2, d2, "Y", postfix);
////		addEdgeStr(e08, d2, f1, "X", postfix);
//		return labels;
//		
//	}
		
	public static void createInputGraph(long seed, long size, long rate, long querySize, long queryRatio, long querySet, boolean neo4j) {
		createInputGraph(seed, size, rate, querySize, queryRatio, querySet, neo4j, "data", false);
	}
	
	public static void createInputGraph(long seed, long size, long rate, long querySize, long queryRatio, long querySet, boolean neo4j, String baseDir, boolean forParalellExperiment) {
		GraphGenerator.querySet = querySet;
		
		int tid = Util.startTimer();
		long iter = size / 9; // divisor = # of nodes in the minimal graph
		long sizeToGen = iter * 8; // multiplier = # of edges in the minimal graph
		maxIter = iter;
		
		pickEidRand = new Random(seed + 419084);
		pickQueryRand = new Random(seed + 89890);

		nid = 0;
		eid = 0;

		long countSelectedIters = iter * rate / 10000;
		selectedIter.clear();
		
		Random rand = new Random(seed); //instance of random class
		for (int i = 0; i < countSelectedIters; i++) {
			int int_random = rand.nextInt((int)iter);
			selectedIter.add(int_random);
		}
		while(selectedIter.size() < countSelectedIters) {
			int int_random = rand.nextInt((int)iter);
			selectedIter.add(int_random);
		}
		
//		System.out.println("selectedIter: " + selectedIter);
		
		long queryRatioAmongAll = querySize * 10000 / size; // e.g., size: 1000 querySize: 30, ratio: 300 (3%)
		
		setQueryWorkLoad(seed, sizeToGen, querySize, queryRatio);
		
//		Util.Console.logln("Elaspsed Time (graphGen seed[" + seed + "] size[" + size + "] iter[" + iter +"] ratio[" + rate + "] querySize[" + querySize + "] queryRatio[" + queryRatio + "] queryRatioAmongAll[" + queryRatioAmongAll + "]");
		
		if (size < querySize) {
			throw new IllegalArgumentException("size: " + size + " < querySize: " + querySize);
		}
		queries.clear();
//		selectedIter.clear();
		
		isForNeo4j = neo4j;
		
		long querySoFar = 0;
		
		try {
			FileWriter n, e;
			
			if (isForNeo4j == true) {
				n = new FileWriter(baseDir + "/node/node.csv");
				e = new FileWriter(baseDir + "/edge/edge.csv");
				n.write("uid:ID,:LABEL,level:INT,c:INT,d:INT\n");
				e.write("uid:INT,:START_ID,:END_ID,:TYPE,level:INT,c:INT,d:INT\n");
			} else {
				n = new FileWriter(baseDir + "/node.csv");
				e = new FileWriter(baseDir + "/edge.csv");
			}

//			Random rand2 = new Random(seed+1742); //instance of random class
			Random rand3 = new Random(seed+5231); //instance of random class			
			for (int i = 0; i < iter; i++) {
//				int int_random = rand.nextInt(10000);
				
				String postfix = "";
				if (selectedIter.contains(i) == false) {
					postfix = "_";
				}

				int randSet = rand3.nextInt(32);
				
				if (forParalellExperiment == true) {
					// 16/32 (50%) is non matching
					// 1/32 is matched by A0, etc.
					if (randSet < 16) {
						postfix = Integer.toHexString(randSet).toUpperCase();						
					} else {
						postfix = "";
					}
				}
				
				//int number_of_a = 3;

				int a1 = getNextNid();
				int a2 = getNextNid();
				int a3 = getNextNid();
				int b1 = getNextNid();
				int c1 = getNextNid();
				int d1 = getNextNid();
				int c2 = getNextNid(); //added
				int d2 = getNextNid(); //added
				int f1 = getNextNid();
				
				str = new StringBuilder();
				addNodeStr(a1, "A", postfix);
				addNodeStr(a2, "A", postfix);
				addNodeStr(a3, "A", postfix);
				addNodeStr(b1, "B", postfix);
				addNodeStr(c1, "C", postfix);
				addNodeStr(d1, "D", postfix);
				addNodeStr(c2, "C", postfix);
				addNodeStr(d2, "D", postfix);
				addNodeStr(f1, "F", postfix);
				
				n.write(str.toString());
				
				int e01 = getNextEid();
				int e02 = getNextEid();
				int e03 = getNextEid();
				int e04 = getNextEid();
				int e05 = getNextEid();
				int e06 = getNextEid();
				int e07 = getNextEid();
				int e08 = getNextEid();
				
				str = new StringBuilder();
				
				addEdgeStr(e01, a1, b1, "X", postfix);
				addEdgeStr(e02, a2, b1, "X", postfix);
				addEdgeStr(e03, a3, b1, "X", postfix);
				addEdgeStr(e04, b1, c1, "X", postfix);
				addEdgeStr(e05, c1, d1, "X", postfix);
				addEdgeStr(e06, d1, c2, "X", postfix);
				addEdgeStr(e07, c2, d2, "Y", postfix);
				addEdgeStr(e08, d2, f1, "X", postfix);
				
				e.write(str.toString());
			}
			n.close();
			e.close();
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		Util.Console.logln("[GraphGenerator/GraphGen1] Elaspsed Time: " + Util.getElapsedTime(tid) + " seed[" + seed + "] size[" + size + "] iter[" + iter +"] ratio[" + rate + "] querySize[" + querySize + "] queryRatio[" + queryRatio + "] baseDir[" + baseDir + "]");
		Util.Console.logln("[GraphGenerator/GraphGen2] sizeToGen[" + sizeToGen + "] nid[" + nid + "] eid[" + eid + "] queries[" + queries.size() + "]");
		
		
//		ArrayList<String> queries = GraphGenerator.getQueries();
//		for (int i = 0; i < queries.size(); i++) {
//			System.out.println("queries i: " + i + " => " + queries.get(i));
//		}		    					

	}

	public static ArrayList<String> getQueries() {
		return queries;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stb
		long seed = 12345;
		int size = 0;
		size = 1111110; // 1K
//					size = 111; // 1K
//					size = 1110; // 10K
//					size = 3330; // 50K
//							size = 11110; // 100K
//				size = 111110; // 1M
		//size = (int) Math.pow(10,7); 
		int rate = 1000; // up to 10000 = 100%
		boolean forNeo4j = false;
		
		long querySize = 50; // 300;
		long queryRatio = 1000; // 100 is 1%
		
		int querySet = 3;
//		GraphGenerator.createInputGraph(seed, size, rate, querySize, queryRatio, querySet, true);
//		GraphGenerator.createInputGraph(seed, size, rate, querySize, queryRatio, querySet, false);
		
		GraphGenerator.createInputGraph(seed, size, rate, 0, 0, 0, false, "data", true);
//		GraphGenerator.setQueryWorkLoad(seed, size, querySize, queryRatio);

//		System.out.println("=====queries: " + queries.size());
//		ArrayList<String> queries = GraphGenerator.getQueries();
//		for (int i = 0; i < queries.size(); i++) {
//			System.out.println("queries i: " + i + " => " + queries.get(i));
//		}
		System.out.println("END");
	}

}

