//package edu.upenn.cis.db.Neo4j;
//
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import org.neo4j.graphdb.Result;
//
//import edu.upenn.cis.db.graphtrans.Config;
//
//public class Neo4jTransform {
//	private static Neo4jServerThread neo4jServer;
//	
//	private static void transform() {
//		System.out.println("[Neo4jTransform-transform] Start");
//		neo4jServer.execute("MATCH (n) DETACH DELETE n");
//
//		neo4jServer.addNode(1, "A");
//		neo4jServer.addNode(2, "B");
//		neo4jServer.addNode(3, "C");
//		neo4jServer.addNode(4, "D");
//		neo4jServer.addNode(11, "C");
//		neo4jServer.addNode(12, "E");
//		
//		neo4jServer.addEdge(101, 1, 2, "X");
//		neo4jServer.addEdge(102, 2, 3, "Y");
//		neo4jServer.addEdge(103, 3, 4, "Z");
//		neo4jServer.addEdge(104, 11, 12, "X");
//		neo4jServer.addEdge(105, 12, 2, "Y");
//		
//		neo4jServer.execute("MATCH (n1:A)-[e1:X]->(n2:B), (n2)-[e2:Y]-(n3:C), (n3)-[e3:Z]->(n4:D) RETURN n1,n2,n3,n4,e1,e2,e3");
//		// 1. reconnect edges (for each rule) - add e3'(s,s), add e2'(b,s), del e2,e3, del (?,c),(?,d),(c,?),(d,?), add (s,c),(s,d),(c,s),(d,s)
//		// 2. reconcile edges (after all rules) - after all rules are evaluated, reconcile if (s,x),(y,t) where s,t are new, etc. add (s,t), del the two.		
//		neo4jServer.execute("CREATE (s : S {uid : 1000001, c:1, d:99, level:0})");
//
//		neo4jServer.execute("MATCH (n) RETURN n, labels(n)");
//		
//		neo4jServer.execute("MATCH (n1:C)-[e1:X]->(n2:E), (n2)-[e2:Y]-(n3:B) RETURN n1,n2,n3,e1,e2");
//		
//		System.out.println("[Transformation");
//		System.out.println("1. Find matches");
//		System.out.println("2. Map"); // uid=Skolem value, 1)reconnect edges 2)reconcile edges
//		System.out.println("3. Add nodes"); // uid=Skolem value,
//		System.out.println("4. Add edges"); // uid=Skolem value, can be some endpoints are from 2,3
//		System.out.println("5. Del edges"); // can be some endpoints are from 2,3 and edges from 4 
//		System.out.println("6. Del nodes"); // can be from 2,3, remove all its edges, reconcile? 
//		
//		
////		neo4jServer.printResult(result);
////		System.out.println("result: " + result);
//		
//		System.out.println("[Neo4jTransform-transform] End");
//	}
//	
//	private static void printAlgorithm() {
//		String algo[] = {
//"[How to transform]",
//"1. find match",
//"2. map s1,s2 to t",
//"   > add t=SK add e->t, del e->s, del s",
//"3. add n=SK",
//"4. add e=SK",
//"5. del e",
//"6. del n",
//		};
//		System.out.println("[Neo4jTransform-printAlgorithm] Start");
//		
//		for (int i = 0; i < algo.length; i++) {
//			System.out.println(algo[i]);
//		}
//		
//		
//		System.out.println("[Neo4jTransform-printAlgorithm] End");
//	}
//	
//	
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		System.out.println("[Neo4jTransform] Start");
//		
//		printAlgorithm();
//		System.exit(0);
//		
//		// TODO Auto-generated method stub
//		String database = "neo4j";
//		try {
//			Config.load("graphview.conf");
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//		neo4jServer = new Neo4jServerThread(database);
//		neo4jServer.start();
//		
//		while(true) {
//			if (neo4jServer.isRunning() == false) {
//				try {
//					Thread.sleep(200);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			} else {
//				break;
//			}
//		}
//		transform();
//
////		while(true) {
//			try {
//				Thread.sleep(100); //120000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
////		}		
//		
//		neo4jServer.shutDown();
//		
//		System.out.println("[Neo4jTransform] End");
//	}
//
//}
