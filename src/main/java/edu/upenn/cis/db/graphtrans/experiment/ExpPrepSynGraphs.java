package edu.upenn.cis.db.graphtrans.experiment;

import java.io.File;

public class ExpPrepSynGraphs {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("[ExpPrepSynGraphs] Start...");
		
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

		int baseSize = 1000000;
		int baseSelect = 1000;
		int[] querySizes = {10000, 100000, 1000000, 10000000};
		int[] querySelect = {100, 200, 400, 800, 1600, 3200};
		
		String basebaseDir = "experiment/dataset/targets";
		File basebaseDirFile = new File(basebaseDir);
		
		if(!basebaseDirFile.exists()) {
		    System.out.println("basebaseDirFile: " + basebaseDirFile + " doesn't exist.");
		    System.exit(0);
		}
		
		for (int i = 0; i < querySizes.length; i++) {
			size = querySizes[i];
			rate = baseSelect;
			
			String baseDir = basebaseDirFile + "/" + "SYN-" + size + "-" + rate;
			new File(baseDir).mkdirs();
			new File(baseDir + "/neo4j").mkdirs();
			new File(baseDir + "/neo4j/node").mkdirs();
			new File(baseDir + "/neo4j/edge").mkdirs();
			System.out.println("baseDir: " + baseDir);
			GraphGenerator.createInputGraph(seed, size, rate, querySize, queryRatio, querySet, false, baseDir, false);
			GraphGenerator.createInputGraph(seed, size, rate, querySize, queryRatio, querySet, true, baseDir, false);
		}

		for (int i = 0; i < querySelect.length; i++) {
			size = baseSize;
			rate = querySelect[i];

			String baseDir = basebaseDirFile + "/" + "SYN-" + size + "-" + rate;
			new File(baseDir).mkdirs();
			new File(baseDir + "/neo4j").mkdirs();
			new File(baseDir + "/neo4j/node").mkdirs();
			new File(baseDir + "/neo4j/edge").mkdirs();
			System.out.println("baseDir: " + baseDir);			
			GraphGenerator.createInputGraph(seed, size, rate, querySize, queryRatio, querySet, false, baseDir, false);
			GraphGenerator.createInputGraph(seed, size, rate, querySize, queryRatio, querySet, true, baseDir, false);
		}

//		GraphGenerator.createInputGraph(seed, size, rate, querySize, queryRatio, querySet, true);
//		GraphGenerator.createInputGraph(seed, size, rate, querySize, queryRatio, querySet, false);
		
		System.out.println("[ExpPrepSynGraphs] End...");
	}

}
