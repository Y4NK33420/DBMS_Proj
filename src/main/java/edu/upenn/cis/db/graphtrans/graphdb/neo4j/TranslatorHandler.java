package edu.upenn.cis.db.graphtrans.graphdb.neo4j;

import java.util.ArrayList;

public interface TranslatorHandler {
	public static final ArrayList<String> rules = new ArrayList<String>();
	
	public void test();
	
	public void handleMatchClause();
	public void handleConstructClause();
	public void handleMapClause();	
	public void handleDeleteClause();
}
