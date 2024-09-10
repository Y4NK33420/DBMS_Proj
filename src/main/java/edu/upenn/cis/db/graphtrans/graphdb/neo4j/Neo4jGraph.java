package edu.upenn.cis.db.graphtrans.graphdb.neo4j;

import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;

/**
 * Generate NEO4J Cypher queries to create the output graph 
 * of transformation rules by directly updating the raw graph.
 * This is done by collapsing node via APOC and creating/deleting 
 * nodes and edges.
 * 
 * @author sbnet21
 *
 */
public interface Neo4jGraph {
	public void createView(Store store, TransRuleList transRuleList);
	
	public String getCypher(String query);
}