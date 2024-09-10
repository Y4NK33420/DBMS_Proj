package edu.upenn.cis.db.graphtrans.graphdb.neo4j;

import java.util.ArrayList;
import java.util.HashSet;

import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.parser.ViewParser;


public class Translator {
	private static final Translator instance = new Translator();
	private static Neo4jViewMode viewMode = Neo4jViewMode.UPDATE_IN_PLACE;
	private static TranslatorHandler translatorHandler;
	
	private Translator() {	
	}
	
	public static Translator getInstance() {
		return instance;
	}
	
	public enum Neo4jViewMode {
		UPDATE_IN_PLACE,
		OVERLAY
	};
	
	public void setNeo4jViewMode(Neo4jViewMode v) {
		viewMode = v;
		if (viewMode.equals(Neo4jViewMode.UPDATE_IN_PLACE) == true) {
			translatorHandler = UpdateInPlaceTranslatorHandler.getInstance();
		} else if (viewMode.equals(Neo4jViewMode.OVERLAY) == true) {
//			translatorHandler = OverlayTranslatorHandler.getInstance();
		}
	}
	
	public ArrayList<String> getCypherOfCreateView(TransRuleList transRuleList) {
		// TODO Auto-generated method stub
		translatorHandler.test();
		
		System.out.println("[createViewByUpdate]");
		ArrayList<String> rules = new ArrayList<String>();
		StringBuilder rule = new StringBuilder();

		for (TransRule tr : transRuleList.getTransRuleList()) {
			rule = new StringBuilder("// Transformation\n");
			
//			if (tr.getMapMap().size() > 0) {
//				hasMerge = true;
//			}
//			
//			rule.append("CALL apoc.periodic.iterate('\n");
//			HashSet<String> vars = addMatchClause(tr.getPatternMatch(), tr.getWhereConditionForNeo4j());
//			
//			addWithClause(tr, vars);
//			addMergeNodeClause(tr.getMapMap());
//			rule.append("','");
//			addClauseForConstruct(tr);
//			addClauseForDelete(tr.getNodeVarsToDelete());
//			addClauseForDelete(tr.getEdgeVarsToDelete());
//
//			rule.append("\tRETURN count(*)\n");
//			rule.append("'\n");
//			rule.append(", {batchSize:10000, parallel:false})\n");
//
//			rules.add(rule.toString());
		}				

		return rules;
	}

	public ArrayList<String> getCypherOfQuery(TransRuleList transRuleList) {
		// TODO Auto-generated method stub
		return null;
	}	
		
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Translator.getInstance().setNeo4jViewMode(Neo4jViewMode.UPDATE_IN_PLACE);

//		Translator.getInstance().setNeo4jViewMode(Neo4jViewMode.OVERLAY);
//		Translator.getInstance().getCypherOfCreateView(null);
		
		// test
		Config.initialize();
//		System.out.println("[UpdatedViewNeo4jGraph]");
//		Config.setUseCopyForUpdatedViewNeo4jGraph(true);
		
//		Neo4jGraph neo4jGraph = new UpdatedViewNeo4jGraph(null);
		
		String view = "CREATE hybrid \n" 
				+ "	VIEW vv1 ON g WITH DEFAULT MAP (\n"
				+ "         MATCH (e2:E)-[d:DERBY]->(e1:E)\n"
				+ "         WHERE e1 < 10\n"
				+ "CONSTRUCT (s:S)\n"
				+ "MAP FROM e1,e2 TO s\n"
				+ "SET s=SK(\"ff\", e1,e2)\n"
				+ "DELETE d, e1, e2\n"
				+ ")\n";		
		ViewParser parser = new ViewParser();
		TransRuleList transRuleList = parser.Parse(view);
		
//		Config.setUseUpdatedViewNeo4jGraph(true);
		Translator.getInstance().getCypherOfCreateView(null);
//		Config.setUseCopyForUpdatedViewNeo4jGraph(false);
//		neo4jGraph.createView(null, transRuleList);

		
//		String query = "MATCH (e2:E)-[d:DERBY]->(e1:E)\n"
//				+ "         WHERE e1 < 10\n"
//				+ " RETURN (e2)"
//				+ "";
//		String cypher = neo4jGraph.getCypher(query);
//		System.out.println("cypher: " + cypher);
		
	}

}



//package edu.upenn.cis.db.graphtrans.graphdb.neo4j;
//
//import java.util.ArrayList;
//
//import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
//
//public interface CypherRewriter {
//	public ArrayList<String> getCypherOfCreateView(TransRuleList transRuleList);
//	public ArrayList<String> getCypherOfQuery(TransRuleList transRuleList);
//	
//}
