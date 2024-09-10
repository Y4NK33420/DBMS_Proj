package edu.upenn.cis.db.graphtrans.graphdb.neo4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.commons.lang.NotImplementedException;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.parser.QueryToCypherParser;
import edu.upenn.cis.db.graphtrans.parser.ViewParser;
import edu.upenn.cis.db.helper.Util;

public class TranslatorToCypher {
	public enum Neo4jViewMode {
		COPY_AND_UPDATE,
		UPDATE_IN_PLACE,
		OVERLAY
	};
	
	private static HashMap<String, Neo4jViewMode> viewNameToModeMap = new HashMap<String, Neo4jViewMode>();
	private static HashMap<String, Boolean> viewNameToIsDefaultRuleMap = new HashMap<String, Boolean>();

	private static ArrayList<String> cypherRules; 
	private static Neo4jViewMode neo4jViewMode;
	private static StringBuilder rule;
	private static TransRule transRule;

	private static HashSet<String> varsInWithOrReturn = new HashSet<String>();

	public static ArrayList<String> getCypherForCreateView(TransRuleList trList) {
		System.out.println("[getCypherForCreateView]");
		cypherRules = new ArrayList<String>();

		String viewType = trList.getViewType();

		if (viewType.equals("materialized") == true) { // CU
			neo4jViewMode = Neo4jViewMode.COPY_AND_UPDATE;
		} else if (viewType.equals("hybrid") == true) { // UP
			neo4jViewMode = Neo4jViewMode.UPDATE_IN_PLACE;
		} else { // virtual OL
			neo4jViewMode = Neo4jViewMode.OVERLAY;
		}

		viewNameToModeMap.put(trList.getViewName(), neo4jViewMode);
		viewNameToIsDefaultRuleMap.put(trList.getViewName(), trList.isDefaultMap());

		if (neo4jViewMode.equals(Neo4jViewMode.COPY_AND_UPDATE) && trList.isDefaultMap() == true) {
//			throw new NotImplementedException("create view in COPY_AND_UPDATE with DEFAULT_RULE");
			cypherRules.clear();
			return cypherRules;
		} 
		if (neo4jViewMode.equals(Neo4jViewMode.UPDATE_IN_PLACE) && trList.isDefaultMap() == false) {
			cypherRules.clear();
			return cypherRules;
//			throw new NotImplementedException("create view in UPDATE_IN_PLACE with no DEFAULT_RULE");
		}

		System.out.println("[getCypherForCreateView] neo4jViewMode: " + neo4jViewMode);

		for (int i = 0; i < trList.getTransRuleList().size(); i++) {
			rule = new StringBuilder();
			transRule = trList.getTransRuleList().get(i);

			System.out.println("i: " + i + " transRule: " + transRule + " trList.isDefaultMap(): " + trList.isDefaultMap());

			if (trList.isDefaultMap() == false) {
				handleMatchClause();
				if (neo4jViewMode.equals(Neo4jViewMode.COPY_AND_UPDATE) == true) {
					rule.append("CALL apoc.refactor.cloneSubgraph(\n")
						.append("\t[").append(Util.getItemsWithComma(transRule.getMatchNodeVars())).append("],\n")
						.append("\t[").append(Util.getItemsWithComma(transRule.getMatchEdgeVars())).append("],\n")
						.append("\t{})\n")
						.append("YIELD input, output, error\n")
						.append("CALL apoc.create.setProperty(output, \"view\", \"").append(trList.getViewName() + "\")\n")
						.append("YIELD node\n")
						.append("RETURN count(*)");
				} else if (neo4jViewMode.equals(Neo4jViewMode.OVERLAY) == true) {
					rule.append("CALL apoc.create.setProperty([")
						.append(Util.getItemsWithComma(transRule.getMatchNodeVars()))
						.append("], \"view\", \"").append(trList.getViewName() + "\")\n")
						.append("YIELD node\n")
						.append("RETURN count(*)");
				}
			} else {
				rule.append("CALL apoc.periodic.iterate(\'");
				handleMatchClause();
				rule.append(" RETURN *', '");
				handleMapClause();
				rule.append("WITH *\n");
				handleDeleteClause();
				//			rule.append("RETURN *\n','\n");

				handleConstructClause();

				rule.append("RETURN count(*)\n");
				rule.append("', {batchSize:10000, parallel:false})\n");
			}

			cypherRules.add(rule.toString());
		}

		return cypherRules; 
	}

	private static void handleDeleteClause() {		
		HashSet<String> varsToDelete = new HashSet<String>();
		varsToDelete.addAll(transRule.getNodeVarsToDelete());
		varsToDelete.addAll(transRule.getEdgeVarsToDelete());

		if (varsToDelete.isEmpty() == false) {
			rule.append("// DELETE clause\n");
			if (neo4jViewMode.equals(Neo4jViewMode.UPDATE_IN_PLACE) == true) {
				rule.append("DETACH DELETE ")
				.append(Util.getItemsWithComma(varsToDelete));
			} else {
				rule.append("SET ");
				int index = 0;
				for (String v : varsToDelete) {
					if (index > 0) {
						rule.append(", ");
					}
					rule.append(v).append(".d = 1");
				}
			}
			rule.append("\n");
		}
	}

	private static void handleConstructClause() {
		if (transRule.getPatternConstruct().isEmpty() == true) {
			return;
		}

		// create new nodes
		for (Atom a : transRule.getPatternConstruct()) {
			if (a.getRelName().equals(Config.relname_node) == true) {
				String var = a.getTerms().get(0).getVar();
				String label = Util.removeQuotes(a.getTerms().get(1).getVar());

				if (transRule.getMatchNodeVars().contains(var) == false
						&& transRule.getMapFromToMap().keySet().contains(var) == false) {
					rule.append("// CONSTRUCT a new node\n")
					.append("CREATE (").append(var).append(":").append(label);
					if (neo4jViewMode.equals(Neo4jViewMode.OVERLAY) == true) {
						rule.append(" {c:1, d:99}");
					}
					rule.append(")\n");
				}
			}
		}

		// create new edges 
		for (Atom a : transRule.getPatternConstruct()) {
			if (a.getRelName().equals(Config.relname_edge) == true) {
				String var = a.getTerms().get(0).getVar();
				String from = a.getTerms().get(1).getVar();
				String to = a.getTerms().get(2).getVar();
				String label = Util.removeQuotes(a.getTerms().get(3).getVar());

				if (transRule.getMatchEdgeVars().contains(var) == false) {
					rule.append("// CONSTRUCT a new edge\n")
					.append("CREATE (").append(from).append(")-[").append(var).append(":").append(label);
					if (neo4jViewMode.equals(Neo4jViewMode.OVERLAY) == true) {
						rule.append(" {c:1, d:99}");
					}
					rule.append("]->(").append(to).append(")\n");
				}
			}
		}
		varsInWithOrReturn.add("*");
	}

	private static void handleMapClause() {
		HashMap<String, ArrayList<String>> maps = transRule.getMapFromToMap();

		if (transRule.getMapFromToMap().keySet().isEmpty() == true) {
			return;
		}

		// Handle MAP FROM ... TO ...
		varsInWithOrReturn.addAll(transRule.getMatchNodeVars());
		varsInWithOrReturn.addAll(transRule.getMatchEdgeVars());

		rule.append("WITH ");
		int index = 0;
		for (String a : maps.keySet()) {
			if (index > 0) {
				rule.append(", ");
			}
			ArrayList<String> sources = maps.get(a);
			varsInWithOrReturn.removeAll(sources);
			rule.append(sources + " as _" + a);
			index++;
		}
		if (varsInWithOrReturn.isEmpty() == false) {
			if (index > 0) {
				rule.append(", ");
			}
			rule.append(Util.getItemsWithComma(varsInWithOrReturn)).append("\n");
		}

		if (neo4jViewMode.equals(Neo4jViewMode.OVERLAY) == true) {
			handleMapClauseInOverlay();
		} else if (neo4jViewMode.equals(Neo4jViewMode.UPDATE_IN_PLACE) == true) { 
			handleMapClauseInUpdateInPlace();
		}
	}

	private static void handleMapClauseInUpdateInPlace() {
		for (String a : transRule.getMapFromToMap().keySet()) {
			String label = null;
			for (Atom b : transRule.getPatternConstruct()) {
				if (b.getRelName().equals(Config.relname_node) == true) {
					if (b.getTerms().get(0).getVar().equals(a) == true) {
						label = Util.removeQuotes(b.getTerms().get(1).toString());
					}
				}
			}
			if (label == null) {
				throw new IllegalArgumentException("a: " + a + " has no label");
			}
			rule.append("// MAP with variable [" + a + "]\n")
			.append("CALL apoc.refactor.mergeNodes(_")
			.append(a)
			.append(", {properties: \"combine\"}) yield node\n"); // as ")

			rule.append("WITH node as ").append(a).append(", ")
			.append(Util.getItemsWithComma(varsInWithOrReturn)).append("\n")
			.append("CALL apoc.create.setLabels(").append(a).append(", [\"").append(label).append("\"]) ")
			.append("YIELD node\n");
		}	
	}

	private static void handleMapClauseInOverlay() {
		for (String a : transRule.getMapFromToMap().keySet()) {
			ArrayList<String> sources = transRule.getMapFromToMap().get(a);
			String repSource = null;
			for (String s : sources) {
				repSource = s;
				break;
			}

			String target = a;
			String source = "_" + a;
			String label = null;

			for (Atom b : transRule.getPatternConstruct()) {
				if (b.getRelName().equals(Config.relname_node) == true) {
					if (b.getTerms().get(0).getVar().equals(a) == true) {
						label = Util.removeQuotes(b.getTerms().get(1).toString());
					}
				}
			}

			if (label == null) {
				throw new IllegalArgumentException("a: " + a + " has no label");
			}

			rule.append("// MAP clause for variable [").append(target).append("] \n")
			.append("CREATE (").append(target).append(":").append(label)
			.append(" {c:1, d:99})\n"); // uid:" + repSource + ".uid+10000000, 
		}

		rule.append("WITH ");
		int index = 0; 
		for (String target : transRule.getMapFromToMap().keySet()) {
			String source = "_" + target;

			if (index > 0) {
				rule.append(",");
			}
			rule.append(source)
			.append(", ")
			.append(target);
			index++;
		}
		rule.append("\n");
		rule.append("CALL {\n");
		index = 0;
		for (String target : transRule.getMapFromToMap().keySet()) {
			String source = "_" + target;
			if (index > 0) {
				rule.append("\t\tUNION\n");
			}
			// incoming to source nodes
			rule.append("\tWITH ").append(source).append(", ").append(target).append("\n")
			.append("\tUNWIND ")
			.append(source).append(" AS ess ")
			.append("SET ess.d = 1\n")
			.append("\tWITH ").append(source).append(", ess, ").append(target).append("\n")
			.append("\tMATCH (a)-[r]->(ess) ")
			.append("WHERE NOT(a IN ").append(source).append(") ")
			.append("SET r.d = 1\n")
			.append("\tWITH ").append(source).append(", ess, ").append(target).append(", a, r\n")
			.append("\tCALL apoc.create.relationship(a, type(r), {uid: r.uid, c:1,d:99}, ").append(target).append(") YIELD rel\n")
			.append("\tWITH ").append(source).append(", ").append(target).append("\n")
			.append("\tRETURN 1\n");
			// outgoing from source nodes
			rule.append("\t\tUNION\n")	
			.append("\tWITH ").append(source).append(", ").append(target).append("\n")
			.append("\tUNWIND ").append(source).append(" AS ess ")
			.append("SET ess.d = 1\n")
			.append("\tWITH ").append(source).append(", ess, ").append(target).append("\n")
			.append("\tMATCH (ess)-[r]->(a) ")
			.append("WHERE NOT(a IN ").append(source).append(") ")
			.append("SET r.d = 1\n")
			.append("\tWITH ").append(source).append(", ess, ").append(target).append(", a, r\n")
			.append("\tCALL apoc.create.relationship(").append(target).append(", type(r), {uid: r.uid, c:1,d:99}, ").append("a").append(") YIELD rel\n")
			.append("\tWITH ").append(source).append(", ").append(target).append("\n")
			.append("\tRETURN 1\n");
			// internal edges between source nodes
			rule.append("\t\tUNION\n")
			.append("\tWITH ").append(source).append(", ").append(target).append("\n")
			.append("\tUNWIND ").append(source).append(" AS ess ")
			.append("SET ess.d = 1\n")
			.append("\tWITH ").append(source).append(", ess, ").append(target).append("\n")
			.append("\tMATCH (a)-[r]->(ess) ").append("WHERE a IN ").append(source).append(" ")
			.append("SET r.d = 1\n")
			.append("\tWITH ").append(source).append(", ess, ").append(target).append(", a, r\n")
			.append("\tCALL apoc.create.relationship(").append(target).append(", type(r), {uid: r.uid, c:1,d:99}, ").append(target).append(") YIELD rel\n")
			.append("\tWITH ").append(source).append(", ").append(target).append("\n")
			.append("\tRETURN 1\n");
			index++;
		}		
		rule.append("}\n");
	}

	private static void handleMatchClause() {
		ArrayList<Atom> atoms = transRule.getPatternMatch();
		ArrayList<String> whereConditionsForNeo4j = transRule.getWhereConditionForNeo4j();
		
		ArrayList<ArrayList<String>> edges = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> negatedEdges = new ArrayList<ArrayList<String>>();
		HashMap<String, String> nodes = new HashMap<String, String>(); // var, label
		HashSet<String> varsInPostiveAtoms = new LinkedHashSet<String>();
		HashSet<String> nodesShownInEdge = new HashSet<String>();

		rule.append("// MATCH clause\n");
		System.out.println("atoms: " + atoms);
		for (int i = 0; i < atoms.size(); i++) {
			Atom a = atoms.get(i);
			if (a.getRelName().equals(Config.relname_edge) == true) { // edge
				ArrayList<String> edge = new ArrayList<String>();
				edge.add(a.getTerms().get(0).toString()); // edgeVar
				edge.add(a.getTerms().get(1).toString()); // edgeFrom
				edge.add(a.getTerms().get(2).toString()); // edgeTo
				edge.add(Util.removeQuotes(a.getTerms().get(3).toString())); // edgeLabel

				if (a.isNegated() == true) {
					negatedEdges.add(edge);
				} else {
					varsInPostiveAtoms.addAll(a.getVars());
					edges.add(edge);
				}
			} else if (a.getRelName().equals(Config.relname_node) == true) { // node
				String nodeVar = a.getTerms().get(0).toString();
				String nodeLabel = Util.removeQuotes(a.getTerms().get(1).toString());
				nodes.put(nodeVar, nodeLabel);
				varsInPostiveAtoms.add(nodeVar);
			}
		}

		for (int i = 0; i < negatedEdges.size(); i++) {
			ArrayList<String> edge = negatedEdges.get(i);

			StringBuilder negatedEdge = new StringBuilder("NOT (");
			if (edge.get(1).equals("_") == false && varsInPostiveAtoms.contains(edge.get(1)) == true) {
				negatedEdge.append(edge.get(1));
			}
			if (nodes.get(edge.get(1)) != null) {
				negatedEdge.append(":");
				negatedEdge.append(nodes.get(edge.get(1)));
			}
			negatedEdge.append(")-[");
			if (edge.get(0).equals("_") == false && varsInPostiveAtoms.contains(edge.get(0)) == true) {
				negatedEdge.append(edge.get(0));
			}
			negatedEdge.append(":").append(edge.get(3)).append("]->(");
			if (edge.get(2).equals("_") == false && varsInPostiveAtoms.contains(edge.get(2)) == true) {
				negatedEdge.append(edge.get(2));
			}
			if (nodes.get(edge.get(2)) != null) {
				negatedEdge.append(":");
				negatedEdge.append(nodes.get(edge.get(2)));
			}
			negatedEdge.append(")");

			whereConditionsForNeo4j.add(negatedEdge.toString());
		}

		rule.append("MATCH ");
		int index = 0;
		for (ArrayList<String> edge : edges) {
			if (index > 0) {
				rule.append(", ");
			}
			rule.append("(").append(edge.get(1)).append(":").append(nodes.get(edge.get(1))).append(")-[")
			.append(edge.get(0)).append(":").append(edge.get(3)).append("]->(")
			.append(edge.get(2)).append(":").append(nodes.get(edge.get(2))).append(")");

			nodesShownInEdge.add(edge.get(1));
			nodesShownInEdge.add(edge.get(2));

			index++;
		}

		for (String v : nodes.keySet()) {
			if (nodesShownInEdge.contains(v) == false) {
				if (index > 0) {
					rule.append(", ");
				}
				rule.append("(").append(v).append(":").append(nodes.get(v)).append(")");
			}
			index++;
		}
		rule.append("\n");

		System.out.println("[handleMatchClause] whereConditionsForNeo4j: " + whereConditionsForNeo4j);		
		int countForLevel = 0;
		if (whereConditionsForNeo4j.size() > 0) {
			for (String cond : whereConditionsForNeo4j) { 
				if (countForLevel == 0) {
					rule.append("WHERE ");
				} else {
					rule.append(" AND ");
				}
				rule.append(cond);
				countForLevel++;
			}
		}

		if (countForLevel > 0) {
			rule.append("\n");
		}
	}

	public static String getCypherForQuery(String query) {
		QueryToCypherParser parser = new QueryToCypherParser();
		String cypherQuery = parser.getCypher(query, viewNameToModeMap, viewNameToIsDefaultRuleMap);
		
		return cypherQuery;
	}

	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) throws Exception {
		Config.initialize();
		System.out.println("[TranslatorToCypher]");
		//		Config.setUseCopyForUpdatedViewNeo4jGraph(true);

		//		Neo4jGraph neo4jGraph = new TranslatorToCypher(null);

		String view = "CREATE virtual \n" 
				+ "	VIEW vv1 ON g  (\n"
				+ "     MATCH (a:A)-[x:X]->(b:B), (b:B)-[y:Y]->(c:C), (c:C)-[z:Z]->(d:D)\n"
				+ " WHERE a.uid < 100\n"
				+ "		CONSTRUCT (a:A)-[x:X]->(b:B), (b:B)-[y:Y]->(c:C), (c:C)-[z:Z]->(d:D)\n"
				//+ "MAP FROM e1,e2 TO s\n"
				//+ "SET s=SK(\"ff\", e1,e2)\n"
//				+ "DELETE d\n"
				+ ")\n";

		
		view = "  CREATE virtual VIEW v1 ON g WITH DEFAULT MAP (\n"
				+ "                        MATCH (w:W)-[ws:WS]->(s:S)\n"
				+ "                        CONSTRUCT (m:MS)\n"
				+ "                        MAP FROM w, s TO m\n"
				+ "                        SET m = SK(\"ff\", s)\n"
				+ "                        DELETE ws\n"
				+ "                    )\n";
		
		//		view = "CREATE hybrid VIEW v1 ON g WITH DEFAULT MAP (\n"
		//				+ "                        MATCH (p1:U)-[f1:F]->(p2:U), (p2)-[f2:F]->(p3:U), !(p1)-[f3:F]->(p3)\n"
		//				+ "                        WHERE p1 != p3 AND p1 < 100000\n"
		//				+ "                        CONSTRUCT (p1:U)-[r:R]->(p3:U)\n"
		//				+ "                        SET r = SK(\"rr\", p1, p3)\n"
		//				+ "                    )\n";
		//		
		//		view = "CREATE hybrid VIEW v1 ON g WITH DEFAULT MAP (\n"
		//				+ "                        MATCH (p1:U)\n"
		//				+ "                        WHERE p1 < 501\n"
		//				+ "                        DELETE p1\n"
		//				+ "                    )\n";

		ViewParser parser = new ViewParser();
		TransRuleList transRuleList = parser.Parse(view);
		//		CommandExecutor.createView(false, view, transRuleList);


		Neo4jViewMode mode = Neo4jViewMode.OVERLAY;
		//		mode = Neo4jViewMode.UPDATE_IN_PLACE;

		String viewType = transRuleList.getViewType();
		boolean isDefaultMap = transRuleList.isDefaultMap();
		Neo4jGraph neo4jGraph = null;

		System.out.println("viewType: " + viewType);

		if (viewType.equals("materialized") == true) { // CU
			mode = Neo4jViewMode.COPY_AND_UPDATE;
		} else if (viewType.equals("hybrid") == true) { // UP
			mode = Neo4jViewMode.UPDATE_IN_PLACE;
		} else { // virtual OL
			mode = Neo4jViewMode.OVERLAY;
		}

		ArrayList<String> cypherRules = TranslatorToCypher.getCypherForCreateView(transRuleList);
		for (int i = 0; i < cypherRules.size(); i++ ) {
			System.out.println("cypherRules[" + i + "] => \n" + cypherRules.get(i) + ";");
		}

		String query = "MATCH (a:A)-[x:X]->(b:B)\n"
				+ " FROM vv1 "
				+ "         WHERE a.uid < 10 "
				+ " RETURN (a)"
				+ "";
		//						
		String cypher = TranslatorToCypher.getCypherForQuery(query);
		//				
		System.out.println("cypher: " + cypher);
	}
}
