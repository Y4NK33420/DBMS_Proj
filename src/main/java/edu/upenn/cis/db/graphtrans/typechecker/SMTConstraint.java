package edu.upenn.cis.db.graphtrans.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SMTConstraint {
	// below 3 variables are required to declare corresponding variables 
	static int maxNodeId;
	static int maxEdgeId;
	static int maxLabelId;

	private static int currentLabelId;
	static HashMap<String, Integer> labelToId;

	static ArrayList<String> interfereRules;
	static ArrayList<HashSet<Integer>> egdsSet;

	public static void show() {
		System.out.println("labelToId: " + labelToId);
		System.out.println("interfereRules: " + interfereRules);
		System.out.println("egdsSet: " + egdsSet);
	}

	public static void addRuleEgdsPair(String interfereRule, HashSet<Integer> egds) {
		interfereRules.add(interfereRule);
		egdsSet.add(egds);
	}

	public static void compareAndSetMaxNodeId(int maxId) {
		if (maxId > maxNodeId) {
			maxNodeId = maxId;
		}
	}

	public static void compareAndSetMaxEdgeId(int maxId) {
		if (maxId > maxEdgeId) {
			maxEdgeId = maxId;
		}
	}

	public static void compareAndSetMaxLabelId(int maxId) {
		if (maxId > maxLabelId) {
			maxLabelId = maxId;
		}
	}

	public static void setLabelId(String label) {
		if (labelToId.containsKey(label) == false) {
			labelToId.put(label, currentLabelId++);
		}
	}

	public static int getLabelId(String label) { 
		setLabelId(label);
		return labelToId.get(label);
	}

	public static void initialize() {
		maxNodeId = 0;
		maxEdgeId = 0;
		maxLabelId = 0;    		
		currentLabelId = 0;
		if (labelToId == null) {
			labelToId = new HashMap<String, Integer>();
			interfereRules = new ArrayList<String>();
			egdsSet = new ArrayList<HashSet<Integer>>();
		} else {
			labelToId.clear();
			interfereRules.clear();
			egdsSet.clear();
		}
	}

	public static String getSMTConstraint() {
		String c = "";

		for (int i = 0; i < interfereRules.size(); i++) {
			c += interfereRules.get(i) + "\n";

			for (int j : egdsSet.get(i)) {
				c += "egs j: " + j + "\n";
			}
		}
		return c;
	}
}
