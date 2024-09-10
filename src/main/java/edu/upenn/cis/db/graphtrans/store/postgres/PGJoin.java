package edu.upenn.cis.db.graphtrans.store.postgres;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;

public class PGJoin {

	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HashMap<String, ArrayList<Pair<Integer, Integer>>> varBindings
			 = new HashMap<String, ArrayList<Pair<Integer, Integer>>>();
		
		ArrayList<Pair<Integer, Integer>> b = new ArrayList<Pair<Integer, Integer>>();
		b.add(Pair.of(0, 0));
		b.add(Pair.of(1, 0));
		b.add(Pair.of(2, 1));
		varBindings.put("v1", b);
		
		b = new ArrayList<Pair<Integer, Integer>>();
		b.add(Pair.of(0, 1));
		b.add(Pair.of(1, 1));
		b.add(Pair.of(3, 1));
		varBindings.put("v2", b);
		
		b = new ArrayList<Pair<Integer, Integer>>();
		b.add(Pair.of(3, 1));
		b.add(Pair.of(4, 1));		
		varBindings.put("v3", b);

		b = new ArrayList<Pair<Integer, Integer>>();
		b.add(Pair.of(10, 1));
		b.add(Pair.of(11, 1));		
		varBindings.put("v4", b);
		
		b = new ArrayList<Pair<Integer, Integer>>();
		b.add(Pair.of(5, 2));
		varBindings.put("v5", b);

		System.out.println("varBindings: " + varBindings);
	
		ArrayList<ArrayList<Integer>> relVarSets = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> relCrossJoins = new ArrayList<Integer>();
		
        for (String var : varBindings.keySet()) {   
        	ArrayList<Pair<Integer, Integer>> bindings = varBindings.get(var); 
        	if (bindings.size() == 1) {
        		relCrossJoins.add(bindings.get(0).getLeft());
        		continue;
        	}
            for (int i = 0; i < bindings.size(); i++) {
        		Pair<Integer, Integer> p1 = bindings.get(i);
            	for (int j = i + 1; j < bindings.size(); j++) {
            		Pair<Integer, Integer> p2 = bindings.get(j);
            		ArrayList<Integer> arr = new ArrayList<Integer>();
            		if (p1.getLeft() < p2.getLeft()) {
            			arr.add(p1.getLeft());
            			arr.add(p2.getLeft());
            			arr.add(p1.getRight());
            			arr.add(p2.getRight());
            		} else {
            			arr.add(p2.getLeft());
            			arr.add(p1.getLeft());
            			arr.add(p2.getRight());
            			arr.add(p1.getRight());
            		}
        			relVarSets.add(arr);
            	}
            }
        } 
        System.out.println("relVarSets: " + relVarSets);
        
        HashSet<Integer> checkedRelIDs = new HashSet<Integer>();
        String str = "";

        boolean hasInnerJoin = relVarSets.size() > 0;
        if (relVarSets.size() > 0) {
            // start with the first join
    		int r0 = relVarSets.get(0).get(0);
    		int r1 = relVarSets.get(0).get(1);
    		str += "XX AS R" + r0 + " INNER JOIN " + "XX AS R" + r1;
    		checkedRelIDs.add(r0);
    		checkedRelIDs.add(r1);
    		
        	boolean usedON = false;
	        while(relVarSets.size() > 0) {
	        	boolean checkedAll = true;
	        	for (int i = 0; i < relVarSets.size(); i++) {
	        		// if both rel are already in join, put this them in ON
	        		r0 = relVarSets.get(i).get(0);
	        		r1 = relVarSets.get(i).get(1);
	        		int a0 = relVarSets.get(i).get(2);
	        		int a1 = relVarSets.get(i).get(3);
	        		
	        		if (checkedRelIDs.contains(r0) == true && checkedRelIDs.contains(r1) == true) {
	        			str += "\n\t";
	        			if (usedON == false) {
	        				str += "ON ";
	        				usedON = true;
	        			} else {
	        				str += "AND ";
	        			}
	        			str += "R" + r0 + "._" + a0 + " = R" + r1 + "._" + a1 + " ";
	        			relVarSets.remove(i);
	        			checkedAll = false;
	        			break;
	        		}
	        	}
	        	if (checkedAll == false) {
	        		continue;
	        	}
        		usedON = false;

	        	if (relVarSets.size() > 0) {
	        		r0 = relVarSets.get(0).get(0);
	        		r1 = relVarSets.get(0).get(1);
	        		
	        		if (checkedRelIDs.contains(r0) == false && checkedRelIDs.contains(r1) == false) {
	        			str += "\nCROSS JOIN XX AS R" + r0;
	        			checkedRelIDs.add(r0);
	        		} else if (checkedRelIDs.contains(r0) == false) {
	            		str += "\nINNER JOIN XX AS R" + r0;
	            		checkedRelIDs.add(r0);
	        		} else if (checkedRelIDs.contains(r1) == false) {
	        			str += "\nINNER JOIN XX AS R" + r1;
	        			checkedRelIDs.add(r1);
	        		} 
	        	}
	        }
        }
        for (int i = 0; i < relCrossJoins.size(); i++) {
            int r0 = relCrossJoins.get(i);
        	if (i == 0 && hasInnerJoin == false) {
        		str += "\nXX AS R" + r0 + " ";
        	} else {
        		str += "\nCROSS JOIN XX AS R" + r0 + " ";
        	}
        }
        
        System.out.println("str: " + str);
	}

}
