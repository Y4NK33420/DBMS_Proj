package edu.upenn.cis.db.graphtrans.store.neo4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang.NotImplementedException;
import org.neo4j.graphdb.Result;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.Neo4j.Neo4jServerThread;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.Neo4jGraph;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.OverlayViewNeo4jGraph;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.TranslatorToCypher;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.TranslatorToCypher.Neo4jViewMode;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.UpdatedViewNeo4jGraph;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.helper.Util;

public class Neo4jStore implements Store {
	private Neo4jServerThread neo4jServer;
	private Neo4jGraph neo4jgraph;

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		Neo4jServerThread.getEdgeTriggers().clear();
		
		if (neo4jServer.isRunning() == true) {
			neo4jServer.shutDown();
		}		
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
	}

//	@Override
//	public void addTuple(Atom a) {
//		// TODO Auto-generated method stub
//		if (a.getPredicate().getRelName().contentEquals("N") == true) {
//			neo4jServer.addNode(1, "S");
//		} else if (a.getPredicate().getRelName().contentEquals("E") == true) {
//			neo4jServer.addEdge(10, 1, 2, "T");
//		} else {
//			Util.Console.errln("Only tuples for N and E can be inserted. [" + a.getPredicate().getRelName() + "]");
//		}
//	}

	@Override
	public void createView(String name, List<DatalogClause> cs, boolean isMaterialized) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public StoreResultSet getQueryResult(List<DatalogClause> cs) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public StoreResultSet getQueryResult(DatalogClause c) {
//		throw new NotImplementedException();
		return null;
	}	

//	@Override
//	public StoreResultSet getQueryResult(DatalogClause q) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public void printRelation(String relname) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTableIndex(Predicate p, ArrayList<String> cols) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTableIndex(String name, ArrayList<Integer> arrayList) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean createDatabase(String name) {
		// TODO Auto-generated method stub
		return true;	
	}

	@Override
	public boolean deleteDatabase(String name) {
		// TODO Auto-generated method stub
		
		
//		try {
//			Thread.sleep(50000000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		
		if (neo4jServer != null && neo4jServer.isRunning() == true) {
			neo4jServer.shutDown();
		}
		
		String dbdir = Config.get("neo4j.dbdir");
		if (dbdir == null) {
			Util.Console.errln("neo4j.dbdir is not set in [" + Config.getConfigFile() + "]");
			return false;
		}

		try {
			FileUtils.deleteDirectory(new File(dbdir));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}

	@Override
	public boolean useDatabase(String name) {
		// TODO Auto-generated method stub
		String database = "neo4j"; // Neo4j community supports only 'neo4j' database(?)
		System.out.println("[startServer] database: " + database);
		neo4jServer = new Neo4jServerThread(database);
		neo4jServer.start();
		
		int i = 0;
		while(true) {
			if (neo4jServer.isRunning() == true) {
				break;
			}
			System.out.println("[Neo4jStore] Waiting for Neo4jServer to start... i[" + i++ +"]");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("[Neo4jStore] Neo4jServer has started.");
		
		return true;		
	}
	
	public void getCountNodes() {
		neo4jServer.getCountNodes();	
	}


	@Override
	public long importFromCSV(String relName, String filePath) {
		// TODO Auto-generated method stub
//		if (relName.equals("N") == true) {
//			neo4jServer.importNodesFromCSV(filePath);
//		} else {
//			neo4jServer.importEdgesFromCSV(filePath);
//		}
		System.out.println("[Neo4jStore] importFromCSV doesn't do anything, but it's okay for now.");
		return 0;
	}
	
	public boolean isServerRunning() {
		if (neo4jServer == null) {
			return false;
		}
		return neo4jServer.isRunning();
	}
	
	public void execute(String query) {
		neo4jServer.execute(query, false);
	}

	public StoreResultSet query(String query) {
		return neo4jServer.execute(query, true);
	}

	public void printResult(Result result) {
		neo4jServer.printResult(result);
	}

	@Override
	public String getDBname() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean connect() {
		// TODO Auto-generated method stub
		if (Config.get("neo4j.embedded").contentEquals("true") == false) {
			Util.Console.errln("Only embedded Neo4j is supported.");
			return false;
		}
		return true;
	}

	@Override
	public ArrayList<String> listDatabases() {
		// TODO Auto-generated method stub
		ArrayList<String> list = new ArrayList<String>();
		list.add("neo4j");
		return list;
	}

	@Override
	public void createSchema(String dbname, Predicate p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createView(DatalogProgram p, TransRuleList transRuleList) {
		// TODO Auto-generated method stub		
		ArrayList<String> cypherRules;
		
		cypherRules = TranslatorToCypher.getCypherForCreateView(transRuleList);
		for (String stmt : cypherRules) {
			System.out.println("stmt: " + stmt);
			neo4jServer.execute(stmt, false);
		}	
//		debug();
	}
	
	public StoreResultSet getQueryResult(String query) {
		System.out.println("[Neo4jStore] getQueryResult query: " + query);

		String cypher = TranslatorToCypher.getCypherForQuery(query);
		StoreResultSet rs = query(cypher);
		
		return rs;
	}


	@Override
	public void addTuple(String rel, ArrayList<SimpleTerm> arrayList) {
		// TODO Auto-generated method stub
		if (rel.contentEquals(Config.relname_node + Config.relname_base_postfix) == true) {
			neo4jServer.addNode(arrayList.get(0).getLong(), arrayList.get(1).getString());
		} else if (rel.contentEquals(Config.relname_edge + Config.relname_base_postfix) == true) {
			neo4jServer.addEdge(arrayList.get(0).getLong(), arrayList.get(1).getLong(), 
								arrayList.get(2).getLong(), arrayList.get(3).getString());
		} else {
			Util.Console.errln("Only tuples for N and E can be inserted. [" + rel + "]");
		}		
	}

//	@Override
//	public void createViewIndex(List<String> rules) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public void debug() {
		neo4jServer.execute("match (a) return a;", true);
		neo4jServer.execute("match (a)-[e]->(b) return a,e,b;", true);
	}
	
	
	@Override
	public ArrayList<String> listRelations(String dbname) {
		return null;
	}	
	
	@Override
	public String getListRelationStr(String dbname) {
		return null;
	}

	@Override
	public void createConstructors() {
		// TODO Auto-generated method stub
		
	}

}
