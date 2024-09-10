package edu.upenn.cis.db.graphtrans.graphdb.neo4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.neo4j.cypher.result.QueryResult.Record;
import org.neo4j.graphdb.Result;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.Neo4j.Neo4jServerThread;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.parser.QueryToCypherParser;
import edu.upenn.cis.db.graphtrans.parser.ViewParser;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.graphtrans.store.neo4j.Neo4jStore;
import edu.upenn.cis.db.helper.Util;

public class UpdatedViewNeo4jGraph implements Neo4jGraph {
	/**
	 * Create a view by updating the graph instance. 
	 * This is the output graph of transformations but not a view.
	 * 
	 * @param transRuleList
	 */
	private static ArrayList<String> rules;
	private static StringBuilder rule;
	private Neo4jServerThread neo4jServer = null;
	private static boolean isCopyAndUpdate = false; // if false, it is updateInPlace
	
	private boolean hasMerge = false;

	private void addClauseForDelete(HashSet<String> vars) {
		if (vars.isEmpty() == false) {
			rule.append("\tDELETE ")
				.append(Util.getItemsWithComma(vars))
				.append("\n");
		}
	}
	
	private void addClauseForConstruct(TransRule tr) {
		ArrayList<Atom> atoms = tr.getPatternAdd();

		for (Atom a : atoms) {
			if (a.getRelName().equals(Config.relname_node) == true) {
				String var = a.getTerms().get(0).getVar();
				String label = Util.removeQuotes(a.getTerms().get(1).toString());
				
				if (tr.getMapFromToMap().containsKey(var) == true) {
					// https://aura.support.neo4j.com/hc/en-us/articles/1500011138861-Using-apoc-periodic-iterate-and-understanding-how-to-iterate-efficiently-within-your-graph
					ArrayList<String> varsInFromMap = tr.getMapFromToMap().get(var);
					
					rule.append("\tCALL apoc.refactor.mergeNodes(")
						.append(Util.getItemsWithComma(varsInFromMap))
						.append(", {properties: \"combine\"}) yield node") // as ")
					.append(" // merge nodes\n");
					rule.append("\tWITH ")
					.append("node as ").append(var).append("\n")
					.append("\tCALL apoc.create.setLabels(")
					.append(var)
					.append(", [\"").append(label).append("\"]) ")
					.append("YIELD node RETURN *\n");
				} else {
					rule.append("\tMERGE (")
					.append(var).append(":").append(label)
					.append(" {level:1");
					
					ArrayList<String> varsInMapTo = tr.getSkolemFunctionMap().get(var);
					if (varsInMapTo != null) {
						rule.append(", skolemname: ")
						.append(varsInMapTo.get(0))
						.append(", sources: [");
						for (int i = 1; i < varsInMapTo.size(); i++) {
							if (i > 1) {
								rule.append(", ");
							}
							rule.append("ID(").append(varsInMapTo.get(i)).append(")");
						}
						rule.append("]");
					}
					rule.append("})\n");				
				}
			}
		}
		
		for (Atom a : atoms) {
			if (a.getRelName().equals(Config.relname_edge) == true) {
				String from = a.getTerms().get(1).getVar();
				String to = a.getTerms().get(2).getVar();
				String var = a.getTerms().get(0).getVar();
				String label = a.getTerms().get(3).toString();//.getSimpleTerm().getString();

				rule.append("\tMERGE (")
				.append(from).append(")-[:").append(Util.removeQuotes(label)).append(" {level:1");

				ArrayList<String> varsInMapTo = tr.getSkolemFunctionMap().get(var);
				if (varsInMapTo != null) {
					rule.append(", skolemname: ")
					.append(varsInMapTo.get(0))
					.append(", sources: [");
					for (int i = 1; i < varsInMapTo.size(); i++) {
						if (i > 1) {
							rule.append(", ");
						}
						rule.append("ID(").append(varsInMapTo.get(i)).append(")");
					}
					rule.append("]");
				}
				rule.append("}]->(")
					.append(to).append(")\n");
			}
		}
	}

	private void addWithClause(TransRule transrule, HashSet<String> vars) {
		HashMap<Atom, HashSet<String>> maps = transrule.getMapMap();

		if (hasMerge == true) {
			rule.append("WITH ");
		} else {
			rule.append("RETURN ");
		}
		if (maps.size() > 0) {
			int i = 0;
			for (Atom a : maps.keySet()) {
				HashSet<String> sources = maps.get(a);
				vars.removeAll(sources);

				if (i > 0) {
					rule.append(", ");
				}
				rule.append(sources)
				.append(" as ")
				.append("_" + a.getTerms().get(0).toString());
				i++;
			}
			rule.append(", ");
		}
		int i = 0;
		for (String v : vars) {
			if (i > 0) {
				rule.append(", ");
			}
			rule.append(v);
			i++;
		}
		rule.append("\n");
	}

	private void addMergeNodeClause(HashMap<Atom, HashSet<String>> hashMap) { 
		//		System.out.println("[addMergeNodeClause]");
		//		System.out.println("hashMap: " + hashMap);

		for (Atom a : hashMap.keySet()) {
			hasMerge = true;
			
//			System.out.println("a234234: " + a);
			HashSet<String> sources = hashMap.get(a);

//			+ "MATCH (n1:MA)-[e1:L]->(n2:AA)\n"
//			+ "WHERE n1.uid < 300000\n"
//			+ "WITH [n1, n2] as _s, e1\n"
//			+ "CALL apoc.refactor.mergeNodes(_s, {properties: \"combine\"}) yield node // merge nodes\n"
//			+ "WITH node as _s\n"
//			+ "CALL apoc.create.setLabels(_s, [\"S\"]) YIELD node RETURN count(*)\n";

			rule.append("CALL apoc.refactor.mergeNodes(")
			.append("_" + a.getTerms().get(0).toString())
			.append(", {properties: \"combine\"}) yield node") // as ")
			//.append(a.getTerms().get(0).toString())
			.append(" // merge nodes\n");
			
			String label = Util.removeQuotes(a.getTerms().get(1).toString());
			rule.append("WITH ")
			.append("node as _" + a.getTerms().get(0).toString() + "\n")
			.append("CALL apoc.create.setLabels(")
			.append("_" + a.getTerms().get(0).toString())
			.append(", [\"").append(label).append("\"]) ")
			.append("YIELD node RETURN *\n");
		}
	}

	private HashSet<String> addMatchClause(ArrayList<Atom> atoms, ArrayList<String> whereConditionsForNeo4j) {
		ArrayList<ArrayList<String>> edges = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> negatedEdges = new ArrayList<ArrayList<String>>();
		HashMap<String, String> nodes = new HashMap<String, String>(); // var, label
		HashSet<String> varsInPostiveAtoms = new LinkedHashSet<String>();
		
		for (int i = 0; i < atoms.size(); i++) {
			Atom a = atoms.get(i);
			//			System.out.println("a: " + a);
			if (a.getPredicate().getRelName().contentEquals("E") == true) { // edge
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
			} else if (a.getRelName().contentEquals(Config.relname_node) == true) { // node
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
			negatedEdge.append(":");
			negatedEdge.append(edge.get(3));
			negatedEdge.append("]->(");
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
		
		HashSet<String> vars = new LinkedHashSet<String>();
		rule.append("MATCH ");
		int index = 0;
		for (ArrayList<String> edge : edges) {
			if (index > 0) {
				rule.append(", ");
			}
			rule.append("(")
			.append(edge.get(1))
			.append(":")
			.append(nodes.get(edge.get(1)))
			.append(")-[")
			.append(edge.get(0))
			.append(":")
			.append(edge.get(3))
			.append("]->(")
			.append(edge.get(2))
			.append(":")
			.append(nodes.get(edge.get(2)))
			.append(")");

			vars.add(edge.get(1));
			vars.add(edge.get(0));
			vars.add(edge.get(2));
			
			index++;
		}
//		rule.append("\n");
		
		for (String node : nodes.keySet()) {
			if (vars.contains(node) == true) continue; 
			if (index > 0) {
				rule.append(", ");
			}			
			String label = nodes.get(node);
			rule.append("(")
				.append(node)
				.append(":")
				.append(label)
				.append(")");
			index++;
			vars.add(node);
		}
		rule.append("\n");

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
		return vars;
	}
	
	public UpdatedViewNeo4jGraph(Neo4jServerThread neo4jServer) {
		this.neo4jServer = neo4jServer;
	}

	@Override
	public void createView(Store store, TransRuleList transRuleList) {
		if (Config.isUseCopyForUpdatedViewNeo4jGraph() == true) {
			createViewForCopyAndUpdate(store, transRuleList);
		} else {
			createViewForUpdateInPlace(store, transRuleList);
		}

	}
	
	public void createViewForCopyAndUpdate(Store store, TransRuleList transRuleList) {
		if (transRuleList.isDefaultMap() == true) {
			throw new NotImplementedException("createViewForCopyAndUpdate with default map (reason: too expensive to execute)");
		}
		
//		throw new NotImplementedException("createViewForCopyAndUpdate with no default map YET");
		System.out.println("[createViewForCopyAndUpdate]");
		rules = new ArrayList<String>();
		
		for (TransRule tr : transRuleList.getTransRuleList()) {
			rule = new StringBuilder("// Transformation - CopyAndUpdate\n");

			if (tr.getMapMap().size() > 0) {
				hasMerge = true;
			}
			
//			rule.append("CALL apoc.periodic.iterate('\n");
			HashSet<String> vars = addMatchClause(tr.getPatternMatch(), tr.getWhereConditionForNeo4j());
			
			rule.append("CALL apoc.refactor.cloneSubgraph(\n")
				.append("\t[").append(Util.getItemsWithComma(tr.getMatchNodeVars())).append("],\n")
				.append("\t[").append(Util.getItemsWithComma(tr.getMatchEdgeVars())).append("],\n")
				.append("\t{})\n")
			    .append("YIELD input, output, error\n")
			    .append("CALL apoc.create.setProperty(output, \"view\", \"").append(transRuleList.getViewName() + "\")\n")
			    .append("YIELD node\n")
			    .append("RETURN count(*);");
			
//			addWithClause(tr, vars);
//			addMergeNodeClause(tr.getMapMap());
//			rule.append("','");
//			addClauseForConstruct(tr);
//			addClauseForDelete(tr.getNodeVarsToDelete());
//			addClauseForDelete(tr.getEdgeVarsToDelete());

//			rule.append("\tRETURN count(*)\n");
//			rule.append("'\n");
//			rule.append(", {batchSize:10000, parallel:false})\n");

			rules.add(rule.toString());
		}			

	}
	
	public void createViewForUpdateInPlace(Store store, TransRuleList transRuleList) {
		if (transRuleList.isDefaultMap() == false) {
			throw new NotImplementedException("createViewForUpdateInPlace with no default map");
		}
		
		System.out.println("[createViewForUpdateInPlace]");
		rules = new ArrayList<String>();

		for (TransRule tr : transRuleList.getTransRuleList()) {
			rule = new StringBuilder("// Transformation - UpdateInPlace\n");
			
			if (tr.getMapMap().size() > 0) {
				hasMerge = true;
			}
			
			rule.append("CALL apoc.periodic.iterate('\n");
			HashSet<String> vars = addMatchClause(tr.getPatternMatch(), tr.getWhereConditionForNeo4j());
			
			addWithClause(tr, vars);
			addMergeNodeClause(tr.getMapMap());
			rule.append("','");
			addClauseForConstruct(tr);
			addClauseForDelete(tr.getNodeVarsToDelete());
			addClauseForDelete(tr.getEdgeVarsToDelete());

			rule.append("\tRETURN count(*)\n");
			rule.append("'\n");
			rule.append(", {batchSize:10000, parallel:false})\n");

			rules.add(rule.toString());
		}				

		for (int i = 0; i < rules.size(); i++) {
			Util.console_logln("CypherRules i: " + i + " out of " + rules.size(), 3);
			System.out.println(rules.get(i));

			if (store != null) {
				((Neo4jStore)store).execute(rules.get(i).toString());
			}
		}
	}
	
	@Override
	public String getCypher(String query) {
		QueryToCypherParser parser = new QueryToCypherParser();
		String cypher = null; 
//		parser.getCypherWithViewName(query);
		
		return cypher;
	}
		
	public static void main(String[] args) throws Exception {
		Config.initialize();

		String view = "CREATE materialized \n" 
				+ "	VIEW vv1 ON g (\n"
				+ "     MATCH (a:A)-[x:X]->(b:B), (b:B)-[y:Y]->(c:C), (c:C)-[z:Z]->(d:D)\n"
				+ "		CONSTRUCT (a:A)-[x:X]->(b:B), (b:B)-[y:Y]->(c:C), (c:C)-[z:Z]->(d:D)\n"
				//+ "MAP FROM e1,e2 TO s\n"
				//+ "SET s=SK(\"ff\", e1,e2)\n"
				//+ "DELETE d, e1, e2\n"
				+ ")\n";
		ViewParser parser = new ViewParser();
		TransRuleList transRuleList = parser.Parse(view);
		
		String viewType = transRuleList.getViewType();
		boolean isDefaultMap = transRuleList.isDefaultMap();
		Neo4jGraph neo4jGraph = null;
		
		System.out.println("viewType: " + viewType);
		
		if (viewType.equals("materialized") == true) { // CU
			Config.setUseUpdatedViewNeo4jGraph(true);
			Config.setUseCopyForUpdatedViewNeo4jGraph(true);
			
			neo4jGraph = new UpdatedViewNeo4jGraph(null);
		} else if (viewType.equals("hybrid") == true) { // UP
			Config.setUseUpdatedViewNeo4jGraph(true);
			Config.setUseCopyForUpdatedViewNeo4jGraph(false);
			
			neo4jGraph = new UpdatedViewNeo4jGraph(null);			
		} else { // virtual OL
			Config.setUseUpdatedViewNeo4jGraph(false);
			Config.setUseCopyForUpdatedViewNeo4jGraph(true);
			
			neo4jGraph = new OverlayViewNeo4jGraph();		
		}
//		CommandExecutor.createView(false, view, transRuleList);

//		Config.setUseUpdatedViewNeo4jGraph(true);
//		Config.setUseCopyForUpdatedViewNeo4jGraph(true);
		neo4jGraph.createView(null, transRuleList);

		for (int i = 0; i < rules.size(); i++) {
			System.out.println(rules.get(i));
		}
		
		
		//String query = "match s:S-e:X->t:T, r:R, t-e1:Y->v:V from v0 where s<= 100 return s-e->t";
		String query = "MATCH (a:A)-[x:X]->(b:B)\n"
				+ " FROM vv1 "
				+ "         WHERE a.uid < 10\n"
				+ " RETURN (a)"
				+ "";
//				
		String cypher = neo4jGraph.getCypher(query);
//		
		System.out.println("cypher: " + cypher);
	}

	public static boolean isCopyAndUpdate() {
		return isCopyAndUpdate;
	}

	public static void setCopyAndUpdate(boolean isCopyAndUpdate) {
		UpdatedViewNeo4jGraph.isCopyAndUpdate = isCopyAndUpdate;
	}
}
