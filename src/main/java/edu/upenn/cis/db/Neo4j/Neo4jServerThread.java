/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
// From: https://github.com/neo4j/neo4j-documentation/blob/4.0/embedded-examples/src/main/java/org/neo4j/examples/EmbeddedNeo4j.java

package edu.upenn.cis.db.Neo4j;

import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.cli.AdminTool;
import org.neo4j.cli.ExecutionContext;
//import org.neo4j.configuration.ExternalSettings;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.helpers.SocketAddress;
//import org.neo4j.dbms.DatabaseManagementSystemSettings;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.importer.ImportCommandProvider;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import apoc.ApocSettings;

import org.neo4j.importer.ImportCommand;

import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Tuple;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.graphtrans.store.neo4j.Neo4jStore;
import edu.upenn.cis.db.helper.Util;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.exceptions.KernelException;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class Neo4jServerThread extends Thread {
	private GraphDatabaseService graphDb;
	private DatabaseManagementService managementService;
	private String database = null;
	private boolean isRunning = false;
	private static HashMap<Long, Long> nidToId = new HashMap<Long, Long>();
	
	private static ArrayList<String> edgeTriggers = new ArrayList<String>();

	public Neo4jServerThread(String database) {
		this.database = database;
	}

	public String getDatabaseFileDir() {
		return graphDb.toString();
	}

	public void deleteDatabase() {
		managementService.dropDatabase(database);
	}

	public void addNode(long nid, String label) {
		//		firstNode = tx.createNode();
		//		firstNode.setProperty( "message", "Hello, " );
		//		secondNode = tx.createNode();
		//		secondNode.setProperty( "message", "World!" );
		//
		//		relationship = firstNode.createRelationshipTo( secondNode, RelTypes.KNOWS );
		//		relationship.setProperty( "message", "brave Neo4j " );

		try (Transaction tx = graphDb.beginTx()) {
//			System.out.println("[Neo4jServerThread] addNode nid: " + nid + " label: " + label);
			Node node = tx.createNode(Label.label(label));
			node.setProperty("uid", nid);
			node.setProperty("level", 0);
			node.setProperty("c", 0);
			node.setProperty("d", 99);
			tx.commit();
		}
	}

	public void addEdge(long eid, long fromId, long toId, String label) {
		//		try (Transaction tx = graphDb.beginTx()) {
		//		    Node fromNode = tx.findNode(null, "nid", fromId);
		//		    Node toNode = tx.findNode(null, "nid", toId);
		//		    Relationship edge = fromNode.createRelationshipTo(toNode, RelationshipType.withName(label));
		//		    edge.setProperty("eid", eid);
		//		    tx.commit();
		//		}		

		StringBuilder stmt = new StringBuilder("MATCH (a), (b)\n");
		stmt.append("WHERE a.uid = ")
		.append(fromId)
		.append(" AND b.uid = ")
		.append(toId)
		.append("\n")
		.append("CREATE (a)-[r:")
		.append(label)
		.append(" {uid: ")
		.append(eid)
		.append(",level:0, c:0, d:99")
		.append("}]->(b)\n")
		.append("RETURN r");

		//		System.out.println("addEdge: " + stmt.toString());

		int tid = Util.startTimer();
		
		try (Transaction tx = graphDb.beginTx()) {
			tx.execute(stmt.toString());
			tx.commit();
		}	
//		System.out.println("[##N4ServeThread] et: " + Util.getElapsedTime(tid));
		
		for (int i = 0; i < edgeTriggers.size(); i++) {
			String procName = edgeTriggers.get(i);
			String stmt2 = "CALL custom." + procName + "(" + eid + ");";
//			System.out.println("[Neo4jServerThread] " + stmt2);

			try (Transaction tx = graphDb.beginTx()) {
				tx.execute(stmt2);
				tx.commit();
			}
		}
//		System.out.println("[##N4ServeThread] et2 after trigger: " + Util.getElapsedTime(tid));
	}

	public void createDb() {
//		File databaseDirectory = new File("");
		String baseDir = Config.get("neo4j.dbdir");
		if (baseDir == null) {
			Util.Console.errln("neo4j.dbdir is not set in [" + Config.getConfigFile() + "]");
			return;
		}
	    Path databaseDirectory = Path.of(baseDir);
//	    Path data_directory = FileSystems.getDefault().getPath(baseDir + "/data").normalize();
//		Path logs_directory = FileSystems.getDefault().getPath(baseDir + "/logs").normalize();

//        try {	
//        	System.out.println("[Neo4jServerThread] databaseDirectory: " + databaseDirectory);
//			FileUtils.deleteDirectory( databaseDirectory );
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			System.out.println("IOException failed to deleteDirectory: " + databaseDirectory);
//			e.printStackTrace();
//		}

		List<String> unrestricted = new ArrayList<String>();
		unrestricted.add("apoc.*");

		managementService = new DatabaseManagementServiceBuilder(databaseDirectory)
				.setConfig(GraphDatabaseSettings.memory_transaction_database_max_size, 4*1024*1024*1024L)
				.setConfig(GraphDatabaseSettings.memory_transaction_global_max_size, 4*1024*1024*1024L)
				.setConfig(GraphDatabaseSettings.memory_transaction_max_size, 4*1024*1024*1024L)						
				.setConfig(GraphDatabaseSettings.tx_state_max_off_heap_memory, 8*1024*1024*1024L)
				.setConfig(GraphDatabaseSettings.tx_state_off_heap_max_cacheable_block_size, 512*1024*1024L)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "4096M")
				.setConfig(GraphDatabaseSettings.allow_upgrade, true)
				.setConfig(BoltConnector.enabled, true)
				.setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
//				.setConfig(HttpConnector.enabled, true)
//				.setConfig(HttpConnector.listen_address, new SocketAddress("localhost", 7689))
				.setConfig(GraphDatabaseSettings.procedure_unrestricted, unrestricted)
				.setConfig(ApocSettings.apoc_import_file_enabled, true)
				.setConfig(GraphDatabaseSettings.allow_upgrade, true)
				.build();

		System.out.println("[createDb] baseDir: " + baseDir + " databaseDirectory: " + databaseDirectory);
		System.out.println("[createDb] database: " + database);
		graphDb = managementService.database(database);
		System.out.println("[createDb] graphDb: " + graphDb);

		registerShutdownHook(managementService);
		
		registerProcedure(graphDb, apoc.help.Help.class);
		registerProcedure(graphDb, apoc.periodic.Periodic.class);
		registerProcedure(graphDb, apoc.create.Create.class);
		registerProcedure(graphDb, apoc.refactor.GraphRefactoring.class);
		registerProcedure(graphDb, apoc.map.Maps.class);
		registerProcedure(graphDb, apoc.schema.Schemas.class);
		registerProcedure(graphDb, apoc.custom.CypherProcedures.class);
		registerProcedure(graphDb, apoc.load.LoadCsv.class);
		registerProcedure(graphDb, apoc.path.PathExplorer.class);

		setRunning(true);
		System.out.println("[Neo4jServerThread] Neo4j with database [" + database + "] has started.");
	}

	public void shutDown() {
		System.out.println("[Neo4jServerThread] Neo4j is shutting down database.");
		
//		
//		try {
//			System.out.println("[Neo4jServerThread] Shutdown in 60sec");
//			Thread.sleep(300000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
		managementService.shutdown();
		setRunning(false);
	}	

	private static void registerShutdownHook( final DatabaseManagementService managementService )
	{
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				managementService.shutdown();
			}
		} );
	}

	public void run() {
		createDb();
	}   

	public StoreResultSet execute(String stmt, boolean isQuery) {
		StoreResultSet rs = null;

		int tid2 = Util.startTimer();

		try (Transaction tx = graphDb.beginTx()) {
			Result result = tx.execute(stmt);
			
			if (result == null) {
				throw new IllegalArgumentException("result is null stmt: " + stmt);
			}
			
			if (isQuery == true) {
				rs = new StoreResultSet();
//				System.out.println("code 34124 this is query stmt: " + stmt);

				while (result.hasNext()) {
					Tuple<SimpleTerm> t = new Tuple<SimpleTerm>();
//					System.out.println("code 213213 result has some next");
					
					Map<String,Object> row = result.next();
					for ( Entry<String, Object> column : row.entrySet() ) {
//						System.out.println("code 13213 column: " + column);
						if (column.getValue() == null) {
							t.getTuple().add(new StringSimpleTerm("null"));
						} else {
							String val = null;
							if (column.getValue() instanceof Node) {
								val = column.getValue().toString();
								t.getTuple().add(new StringSimpleTerm(val));
							} else if (column.getValue() instanceof Relationship) {
								val = column.getValue().toString();
								t.getTuple().add(new StringSimpleTerm(val));
							} else {
								val = column.getValue().toString();
								t.getTuple().add(new LongSimpleTerm(Long.parseLong(val)));
							}
						}
					}
					rs.getResultSet().add(t);
				}
			}
			tx.commit();
		} catch(Exception e) {
			System.out.println("e: " + e.toString());
			//			throw new IllegalArgumentException("execute failed stmt: " + stmt + " e: " + e.getMessage() + " e2: " + e.getLocalizedMessage());
			System.out.println("execute failed stmt: " + stmt + " e: " + e.getMessage() + " e2: " + e.getLocalizedMessage());
			System.exit(0);
		}
		return rs;
	}

	public void cloneAllNodes() {
		System.out.println("[cloneAllNodes]");

		StringBuilder stmt = new StringBuilder();
		stmt.append("MATCH (n) RETURN n");

		int tid = Util.startTimer();

		ArrayList<Pair<Label, Long>> nodes = new ArrayList<Pair<Label, Long>>();
		nidToId.clear();

		int batchSize = 100000;

		int cc = 0;
		ArrayList<Long> nids = new ArrayList<Long>();

		try (Transaction tx = graphDb.beginTx()) {
			Result result = tx.execute(stmt.toString());

			while (result.hasNext()) {
				Map<String,Object> row = result.next();
				for ( Entry<String,Object> column : row.entrySet()) {
					Node node = (Node)column.getValue();
					long nid = (Long)node.getProperty("uid");
					nids.add(nid);

					//				Label label = null;
					//				for (Label l : node.getLabels()) {
					//					label = l;
					//					break;
					//				}
				}
			}
			//			tx.commit();
		}

		int i = 0;
		while(true) {
			try (Transaction tx = graphDb.beginTx()) {
				while(true) {
					long nid = nids.get(i);
					Node newNode = tx.createNode();
					newNode.addLabel(Label.label("A"));
					newNode.setProperty("uid", nid);
					newNode.setProperty("level", 1);

					i++;

					if (i % 500000 == 0) {
						System.out.println("\nii: " + i);
					} else if (i % 100000 == 0) {
						System.out.print(i + "\t");
					}
					if (i % batchSize == 0 || i == nids.size()) {
						break;
					}

				}
				tx.commit();
				if (i == nids.size()) {
					break;
				}
			}
		}
		System.out.println("[cloneAllNodes] clone nodes elapsed time: " + Util.getElapsedTime(tid));
	}

	public void genBulkNodes() {
		System.out.println("[genBulkNodes]");

		int tid = Util.startTimer();
		int countNodes = 10000000;
		int batchSize = 50000;

		ArrayList<Integer> nids = new ArrayList<Integer>();
		for (int i = 0; i < countNodes; i++) {
			nids.add(i);
		}

		int i = 0;
		while(true) {
			try (Transaction tx = graphDb.beginTx()) {
				while(true) {
					Node newNode = tx.createNode();
					newNode.addLabel(Label.label("A"));
					newNode.setProperty("uid", i);
					newNode.setProperty("level", 1);

					i++;

					if (i % batchSize == 0 || i == countNodes) {
						break;
					}
				}
				tx.commit();
				if (i == countNodes) {
					break;
				}
			}
		}
		System.out.println("[genBulkNodes] time: " + Util.getElapsedTime(tid));
	}

	public void cloneAllEdges() {
		System.out.println("[cloneAllEdges]");

		StringBuilder stmt = new StringBuilder();
		stmt.append("MATCH (n1)-[r]->(n2) RETURN n1, r, n2");

		int tid = Util.startTimer();
		// n1.nid, n2.nid, e.eid, e.type
		ArrayList<Tuple4<Long, Long, Long, RelationshipType>> edges = new ArrayList<Tuple4<Long, Long, Long, RelationshipType>>();

		int tid2 = Util.startTimer();

		try (Transaction tx = graphDb.beginTx()) {
			Result result = tx.execute(stmt.toString());

			int i = 0;
			while (result.hasNext()) {
				try (Transaction tx1 = graphDb.beginTx()) {
					while (result.hasNext()) {
						Map<String,Object> row = result.next();
						Node from = (Node)row.get("n1");
						Node to = (Node)row.get("n2");
						Relationship edge = (Relationship)row.get("r");
						RelationshipType type = edge.getType();

						Node newFrom = tx.getNodeById(nidToId.get(from.getProperty("uid")));
						Node newTo = tx.getNodeById(nidToId.get(to.getProperty("uid")));
						Relationship rel = newFrom.createRelationshipTo(newTo, edge.getType());
						rel.setProperty("uid", edge.getProperty("uid"));
						rel.setProperty("level", 1);

						i++;

						if (i % 1000 == 0 || result.hasNext() == false) {
							if (i % 500000 == 0) {
								System.out.println("[cloneAllEdges] i: " + i + " elapsed time: " + Util.getElapsedTime(tid2));
							}
							break;
						}
					}
					tx1.commit();
				}
			}
		}
		System.out.println("[cloneAllEdges] clone edges elapsed time: " + Util.getElapsedTime(tid));
	}


	public void getCountNodes() {
		System.out.println("[getCountNodes] graphDb: " + graphDb);
		StringBuilder stmt = new StringBuilder();
		stmt.append("MATCH (n) RETURN COUNT(*)");

		try (Transaction tx = graphDb.beginTx()) {
			Result result = tx.execute(stmt.toString());
			String rows = "";

			while ( result.hasNext() )
			{
				Map<String,Object> row = result.next();
				for ( Entry<String,Object> column : row.entrySet() )
				{
					rows += column.getKey() + ": " + column.getValue() + "; ";
				}
				rows += "\n";
			}
			System.out.println("rows: " + rows);
		}
	}

	public void getCountEdges() {
		System.out.println("[getCountEdges] graphDb: " + graphDb);
		StringBuilder stmt = new StringBuilder();
		stmt.append("MATCH (n1)-[e]->(n2) RETURN COUNT(*)");

		try (Transaction tx = graphDb.beginTx()) {
			Result result = tx.execute(stmt.toString());
			String rows = "";

			while ( result.hasNext() )
			{
				Map<String,Object> row = result.next();
				for ( Entry<String,Object> column : row.entrySet() )
				{
					rows += column.getKey() + ": " + column.getValue() + "; ";
				}
				rows += "\n";
			}
			System.out.println("rows: " + rows);
		}
	}

	public void getApocHelp() {
		System.out.println("[getApocHelp] graphDb: " + graphDb);
		StringBuilder stmt = new StringBuilder();
		stmt.append("CALL apoc.help(\"apoc.refactor\");");

		try (Transaction tx = graphDb.beginTx()) {
			Result result = tx.execute(stmt.toString());
			String rows = "";

			while ( result.hasNext() )
			{
				Map<String,Object> row = result.next();
				for ( Entry<String,Object> column : row.entrySet() )
				{
					rows += column.getKey() + ": " + column.getValue() + "; ";
				}
				rows += "\n";
			}
			System.out.println("rows: " + rows);
		}
	}

	public void importNodesFromCSV(String filepath) {
		StringBuilder stmt = new StringBuilder();
		stmt.append("CALL apoc.periodic.iterate( ")
		.append("'CALL apoc.load.csv(\"" + filepath + "\") yield list as list return list' ")
		.append(",'CALL apoc.create.node([list[1]], {uid: list[0]}) YIELD node RETURN count(*)' ")
		.append(",{batchSize:10000, iterateList:true, parallel:true})");	

		try (Transaction tx = graphDb.beginTx()) {
			Result result = tx.execute(stmt.toString());
			String rows = null;
			while ( result.hasNext() )
			{
				Map<String,Object> row = result.next();
				for ( Entry<String,Object> column : row.entrySet() )
				{
					rows += column.getKey() + ": " + column.getValue() + "; ";
				}
				rows += "\n";
			}
			System.out.println("rows: " + rows);
		}
	}

	public void importEdgesFromCSV(String filepath) {
		StringBuilder stmt = new StringBuilder();
		stmt.append("CALL apoc.periodic.iterate( ")
		.append("'CALL apoc.load.csv(\"" + filepath + "\") yield list as list return list' ")
		.append(",'MATCH (p1 {uid:list[1]}), (p2 {uid:list[2]}) WITH p1, p2, list ")
		.append("CALL apoc.create.relationship(p1,list[3],{uid:list[0]}, p2) YIELD rel RETURN count(*) '")
		.append(",{batchSize:10000, iterateList:true, parallel:true})");	
 
		try (Transaction tx = graphDb.beginTx()) {
			Result result = tx.execute(stmt.toString());
			String rows = null;
			while ( result.hasNext() ) {
				Map<String,Object> row = result.next();
				for ( Entry<String,Object> column : row.entrySet() )
				{
					rows += column.getKey() + ": " + column.getValue() + "; ";
				}
				rows += "\n";
			}
			System.out.println("rows: " + rows);
		}
	}

	public static void registerProcedure(GraphDatabaseService db, Class<?>...procedures) {
		GlobalProcedures globalProcedures = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(GlobalProcedures.class);
		for (Class<?> procedure : procedures) {
			try {
				globalProcedures.registerProcedure(procedure, true);
				globalProcedures.registerFunction(procedure, true);
				globalProcedures.registerAggregationFunction(procedure, true);
			} catch (KernelException e) {
				throw new RuntimeException("while registering " + procedure, e);
			}
		}
	}

	public synchronized boolean isRunning() {
		return isRunning;
	}

	public synchronized void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}
	
	public void printResult(Result result) {
		// TODO Auto-generated method stub
		try (Transaction tx = graphDb.beginTx()) {
			String rows = "";

			System.out.println("=== (Neo4j-printResult) Result (Start) ===");
//			System.out.println.resultAsString()
			while ( result.hasNext() )
			{
				Map<String,Object> row = result.next();
				for ( Entry<String,Object> column : row.entrySet() )
				{
					rows += column.getKey() + ": " + column.getValue() + "; ";
				}
				rows += "\n";
			}
			System.out.println(rows);

			tx.commit();
			System.out.println("=== (Neo4j) Result (End) ===");			
		}
	}

	public static void loadDatabase(String loadPath) {
		String database = "neo4j";

		try {
		    Path databaseDirectory1 = Path.of(Config.get("neo4j.dbdir") + "/data/databases/" + database);
		    Path databaseDirectory2 = Path.of(Config.get("neo4j.dbdir") + "/data/transactions/" + database);
		    
			System.out.println("Delete directory: " + Config.get("neo4j.dbdir") + "/data/databases/" + database);
			 
			//new File(Config.get("neo4j.dbdir") + "/data/databases/" + database)
			//new File(Config.get("neo4j.dbdir") + "/data/transactions/" + database)
			FileUtils.deleteDirectory(databaseDirectory1);
			FileUtils.deleteDirectory(databaseDirectory2);		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("[loadDatabase] loadPath: " + loadPath + " Config.get(\"neo4j.dbdir\"): " + Config.get("neo4j.dbdir"));

		Path currentRelativePath = Paths.get("");
		String cwd = currentRelativePath.toAbsolutePath().toString();

		Path neo4j_home = FileSystems.getDefault().getPath(cwd, Config.get("neo4j.dbdir")).normalize();
		Path neo4j_conf_home = FileSystems.getDefault().getPath(cwd, "./conf/neo4j.conf").normalize();

		System.out.println("neo4j_home: " + neo4j_home + " neo4j_conf_home: " + neo4j_conf_home);

		ExecutionContext ctx = new ExecutionContext(neo4j_home, neo4j_conf_home);
		ArrayList<String> params = new ArrayList<String>();
		params.add("load");
		params.add("--from=" + loadPath);
		params.add("--database=" + database);

		AdminTool.execute(ctx, params.toArray(new String[]{}));

		System.out.println("[loadDatabase] (end) loadPath: " + loadPath + " to: " + database);

		//    	try {
		//			Thread.sleep(10*60*1000);
		//		} catch (InterruptedException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
	}

	public static void prepareDatabase(String dirPath) {
		String database = "neo4j";
		System.out.println("[Neo4jStore] prepareDatabase from dirPath[" + dirPath + "] with neo4jBaseDir[" + Config.get("neo4j.dbdir") + "]");
		//		Config.setWorkspace(database);

		try {
		    Path databaseDirectory1 = Path.of(Config.get("neo4j.dbdir") + "/data/databases/" + database);
		    Path databaseDirectory2 = Path.of(Config.get("neo4j.dbdir") + "/data/transactions/" + database);
		    
			System.out.println("Delete directory: " + Config.get("neo4j.dbdir") + "/data/databases/" + database);
			 
			//new File(Config.get("neo4j.dbdir") + "/data/databases/" + database)
			//new File(Config.get("neo4j.dbdir") + "/data/transactions/" + database)
			FileUtils.deleteDirectory(databaseDirectory1);
			FileUtils.deleteDirectory(databaseDirectory2);		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Path neo4j_home = FileSystems.getDefault().getPath(Config.get("neo4j.dbdir")).normalize();
		Path neo4j_conf_home = FileSystems.getDefault().getPath("./conf").normalize();

		System.out.println("neo4j_home: " + neo4j_home + " neo4j_conf_home: " + neo4j_conf_home);

		ExecutionContext ctx = new ExecutionContext(neo4j_home, neo4j_conf_home);


		// This filter will only include files ending with .py
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return name.endsWith(".csv");
			}
		};

		String[] pathnames_node, pathnames_edge;
		File f_node = new File(dirPath + "/node");
		File f_edge = new File(dirPath + "/edge");

		if (f_node.exists() == false) {
			throw new IllegalArgumentException("dirPath[" + dirPath + "] has no node folder.");
		} 
		if (f_edge.exists() == false) {
			throw new IllegalArgumentException("dirPath[" + dirPath + "] has no edge folder.");
		}
		pathnames_node = f_node.list(filter);
		pathnames_edge = f_edge.list(filter);

		// For each pathname in the pathnames array
		ArrayList<String> params = new ArrayList<String>();
		params.add("import");
		params.add("--database=" + database);
		params.add("--id-type=INTEGER");
		for (String pathname : pathnames_node) {
			params.add("--nodes=" + dirPath + "/node/" + pathname);
		}
		for (String pathname : pathnames_edge) {
			params.add("--relationships=" + dirPath + "/edge/" + pathname);
		}
		params.add("--skip-bad-relationships");


		System.out.println("params: " + params);

		AdminTool.execute(ctx, params.toArray(new String[]{}));
		//		import"				
		//				, "--database=" + database
		//				, "--id-type=INTEGER"
		//				, "--nodes=" + dirPath + "/*.n.csv"
		//				, "--relationships=data/header_e.csv,data/gen_e.csv"
		//				);		
	}	

	public static void prepareDatabase(String dbName, String srcPath, String dstPath) {
		String database = "neo4j";
		System.out.println("[Neo4jStore] prepareDatabase " + "dbName[" + dbName + "] srcPath[" + srcPath + "] dstPath [" + dstPath + "] with neo4jBaseDir[" + Config.get("neo4j.dbdir") + "]");
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();

		try {
		    Path databaseDirectory1 = Path.of(Config.get("neo4j.dbdir") + "/data/databases/" + database);
		    Path databaseDirectory2 = Path.of(Config.get("neo4j.dbdir") + "/data/transactions/" + database);
		    
			System.out.println("Delete directory: " + Config.get("neo4j.dbdir") + "/data/databases/" + database);
			 
			//new File(Config.get("neo4j.dbdir") + "/data/databases/" + database)
			//new File(Config.get("neo4j.dbdir") + "/data/transactions/" + database)
			FileUtils.deleteDirectory(databaseDirectory1);
			FileUtils.deleteDirectory(databaseDirectory2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Path neo4j_home = FileSystems.getDefault().getPath(s + "/" + Config.get("neo4j.dbdir")).normalize();
		Path neo4j_conf_home = FileSystems.getDefault().getPath(".").normalize();

		System.out.println("neo4j_home: " + neo4j_home + " neo4j_conf_home: " + neo4j_conf_home);

		ExecutionContext ctx = new ExecutionContext(neo4j_home, neo4j_conf_home);


		// This filter will only include files ending with .py
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return name.endsWith(".csv");
			}
		};

		String[] pathnames_node, pathnames_edge;
		File f_node = new File(srcPath + "/node");
		File f_edge = new File(srcPath + "/edge");

		if (f_node.exists() == false) {
			throw new IllegalArgumentException("dirPath[" + srcPath + "] has no node folder.");
		} 
		if (f_edge.exists() == false) {
			throw new IllegalArgumentException("dirPath[" + srcPath + "] has no edge folder.");
		}
		pathnames_node = f_node.list(filter);
		pathnames_edge = f_edge.list(filter);

		///////////////
		// Import
		///////////////
		// For each pathname in the pathnames array
		ArrayList<String> params = new ArrayList<String>();
		params.add("import");
		params.add("--database=" + database);
		params.add("--id-type=INTEGER");
		for (String pathname : pathnames_node) {
			params.add("--nodes=" + srcPath + "/node/" + pathname);
		}
		for (String pathname : pathnames_edge) {
			params.add("--relationships=" + srcPath + "/edge/" + pathname);
		}
		params.add("--skip-bad-relationships");


		System.out.println("params2: " + params);


		System.out.println("CWD: " + s);
		//		System.exit(0);
		AdminTool.execute(ctx, params.toArray(new String[]{}));

		///////////////
		// Dump
		///////////////
		params.clear();
		params.add("dump");
		params.add("--to=" + dstPath);
		params.add("--verbose");
		params.add("--database=" + database);
		System.out.println("params: " + params);
		AdminTool.execute(ctx, params.toArray(new String[]{}));


		//		import"				
		//				, "--database=" + database
		//				, "--id-type=INTEGER"
		//				, "--nodes=" + dirPath + "/*.n.csv"
		//				, "--relationships=data/header_e.csv,data/gen_e.csv"
		//				);
	}		
	// end::shutdownHook[]	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String database = "neo4j";
		Config.load("conf/graphview.conf");

		String basebaseDir = "experiment/dataset/targets";
		File basebaseDirFile = new File(basebaseDir);

		if(!basebaseDirFile.exists()) {
			System.out.println("basebaseDirFile: " + basebaseDirFile + " doesn't exist.");
			System.exit(0);
		}

		String dbName = "SYN-10000-1000";		
		String srcPath = basebaseDir + "/neo4j/" + dbName;
//		System.out.println("dbName: " + dbName + " srcPath: " + srcPath);

//		Neo4jServerThread.loadDatabase(srcPath);

		Neo4jServerThread neo4jServer = new Neo4jServerThread(database);
		neo4jServer.start();

		boolean hasRunCommand = false;
		while(true) {
			if (hasRunCommand == false && neo4jServer.isRunning() == true) {
//				neo4jServer.getCountNodes();
				int tid = Util.startTimer();
//				neo4jServer.getApocHelp();

				System.out.println("datetime: " + Util.getDateTime());

				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

//				neo4jServer.shutDown();
//				hasRunCommand = true;
//				break;
			}
		}		
		
//		System.exit(0);

		
		
		
		
		
		
		
		
//		File directoryPath = new File(basebaseDir);
//		String contents[] = directoryPath.list();
//		System.out.println("List of files and directories in the specified directory:");
//		String[] excluedDirs = {"backup", "neo4j", "logicblox", "postgres"};
//		List<String> excludedDirsArr = Arrays.asList(excluedDirs);
//		for(int i = 0; i < contents.length; i++) {
//			if ((new File(basebaseDir + "/" + contents[i])).isDirectory() == true) {
//				if (excludedDirsArr.contains(contents[i]) == false) {
//					System.out.println(contents[i]);
//				}
//			}
//		}
//		System.exit(0);

//		Neo4jServerThread.prepareDatabase(dbName, srcPath, dstPath);


		//		
		//		Neo4jServerThread neo4jServer = new Neo4jServerThread(database);
		////		neo4jServer.loadDatabase("/home/sbnet21/tools/neo4j-community-4.1.11/backup-neo4j-01");
		//		neo4jServer.prepareDatabase("data");
		//		
		////		neo4jServer.prepareDatabase("test/neo4j/data/neo4j");
		//		neo4jServer.start();
		//
		//		boolean hasRunCommand = false;
		//		while(true) {
		//			if (hasRunCommand == false && neo4jServer.isRunning() == true) {
		//				neo4jServer.getCountNodes();
		//				int tid = Util.startTimer();
		////				neo4jServer.cloneAllNodes();
		////				System.out.println("Elapsed time for cloneAllNodes: " + Util.getElapsedTime(tid));
		////				neo4jServer.cloneAllEdges();
		////				System.out.println("Elapsed time for cloneAllEdges: " + Util.getElapsedTime(tid));
		//				neo4jServer.getApocHelp();
		//				
		//				try {
		//					Thread.sleep(50000);
		//				} catch (InterruptedException e) {
		//					// TODO Auto-generated catch block
		//					e.printStackTrace();
		//				}
		//				
		//				//neo4jServer.genBulkNodes();
		//				//neo4jServer.genBulkNodes();
		//				
		//				//neo4jServer.getCountNodes();
		//				//neo4jServer.getCountEdges();
		////				try {
		////					Thread.sleep(50000);
		////				} catch (InterruptedException e) {
		////					// TODO Auto-generated catch block
		////					e.printStackTrace();
		////				}
		//
		//				neo4jServer.shutDown();
		//				hasRunCommand = true;
		//				break;
		//			}
		//		}
//		System.out.println("DONE.");
	}

	public static ArrayList<String> getEdgeTriggers() {
		return edgeTriggers;
	}



}
