package edu.upenn.cis.db.graphtrans.catalog;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Schema {
	final static Logger logger = LogManager.getLogger(Schema.class);

	private static ArrayList<SchemaNode> schemaNodes = new ArrayList<>();
	private static ArrayList<SchemaEdge> schemaEdges = new ArrayList<>();
	
	private static HashMap<String, Integer> mapNode = new HashMap<>();
	private static HashMap<Triple<String, String, String>, Integer> mapEdge = new HashMap<>();
	
	private static int maxIndex = 0;
	
	public static void clear() {
		schemaNodes.clear();
		schemaEdges.clear();
		mapNode.clear();
		mapEdge.clear();
		maxIndex = 0;
	}
	
	public static ArrayList<SchemaNode> getSchemaNodes() {
		return schemaNodes;
	}
	
	public static ArrayList<SchemaEdge> getSchemaEdges() {
		return schemaEdges;
	}
	
	
	public static void addSchemaNode(String label) {
		mapNode.put(label, maxIndex++);
		schemaNodes.add(new SchemaNode(label));
	}
	
	public static void addSchemaEdge(String label, String from, String to) {
		mapEdge.put(Triple.of(label, from, to), maxIndex++);
		schemaEdges.add(new SchemaEdge(label, from, to));
	}
	
	public static int getIndexOfNodeLabel(String label) {
		return mapNode.get(label);
	}
	
	public static int getIndexOfEdgeLabel(String label, String from, String to) {
		return mapEdge.get(Triple.of(label, from, to));
	}
	
	public static int getMaxIndex() {
		return maxIndex;
	}
	
	public static void show() {
		logger.info("mapNode: " + mapNode);
		logger.info("mapEdge: " + mapEdge);
		logger.info("maxIndex: " + maxIndex);
	}
	
	public static String getString() {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < schemaNodes.size(); i++) {
			str.append(schemaNodes.get(i).toString());
			str.append("\n");
		}
		for (int i = 0; i < schemaEdges.size(); i++) {
			str.append(schemaEdges.get(i).toString());
			str.append("\n");
		}
		
		return str.toString();
	}
	
   	public static void main(String[] args) {
   		Schema.clear();
   		Schema.addSchemaNode("A");
   		Schema.addSchemaNode("B");
   		Schema.addSchemaNode("C");
   		Schema.addSchemaEdge("X","A","B");
   		Schema.addSchemaEdge("X","B","C");
   		Schema.addSchemaNode("D");
   		
   		Schema.show();
		logger.info("Done.");
	}
}
