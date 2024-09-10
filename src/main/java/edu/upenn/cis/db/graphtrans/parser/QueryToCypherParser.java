package edu.upenn.cis.db.graphtrans.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryBaseVisitor;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryLexer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser.HopContext;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser.Term_bodyContext;
import edu.upenn.cis.db.graphtrans.graphdb.neo4j.TranslatorToCypher.Neo4jViewMode;
import edu.upenn.cis.db.helper.Util;

public class QueryToCypherParser extends GraphTransQueryBaseVisitor<Void> {
	final static Logger logger = LogManager.getLogger(QueryToCypherParser.class);

	private String from;

	private ArrayList<String> termsInMatch;
	private ArrayList<String> termsInWhere;
	private HashSet<String> varsInMatch;
	private HashSet<String> nodeVarsInMatch;
	private HashSet<String> edgeVarsInMatch;

	private HashMap<String, String> nodeVarToLabelMap;
	private HashSet<String> returnNodeSet;
	private HashSet<String> returnEdgeSet;

	private boolean useViewName;
	private boolean useCreatedDestroyed;

	public QueryToCypherParser() {
		termsInMatch = new ArrayList<String>();
		varsInMatch = new HashSet<String>();
		nodeVarsInMatch = new HashSet<String>();
		edgeVarsInMatch = new HashSet<String>();
		termsInWhere = new ArrayList<String>();

		nodeVarToLabelMap = new HashMap<String, String>();
		new HashMap<String, String>();
		new HashMap<String, Atom>();
		new HashMap<String, Atom>();
		returnNodeSet = new HashSet<String>();
		returnEdgeSet = new HashSet<String>();
		new HashSet<Atom>();
		new HashMap<String, HashMap<String, ArrayList<Atom>>>();

		useViewName = false;
		useCreatedDestroyed = false;
	}

	public String getCypher(String query, HashMap<String, 
			Neo4jViewMode> viewNameToModeMap, HashMap<String, Boolean> viewNameToIsDefaultRuleMap) {

		CharStream input = CharStreams.fromString(query);
		GraphTransQueryLexer lexer = new GraphTransQueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GraphTransQueryParser parser = new GraphTransQueryParser(tokens);
		ParseTree tree = parser.user_query();
		visit(tree);

		System.out.println("[QueryParser] Parse query: " + query + " from: " + from);

		Neo4jViewMode mode = Neo4jViewMode.UPDATE_IN_PLACE;
		Boolean isDefaultMap = false;
		
		if (from.equals("g") == false) {
			mode = viewNameToModeMap.get(from);
			isDefaultMap = viewNameToIsDefaultRuleMap.get(from);
		}
				
		if (isDefaultMap == false) {
			if (from.equals("g") == false) {
				useViewName = true;
			} else {
				useViewName = false;
			}
		} else if (mode.equals(Neo4jViewMode.OVERLAY) == true) {
			useCreatedDestroyed = true;
		}

		System.out.println("[getCypher] useViewName: " + useViewName + " useCreatedDestroyed: " + useCreatedDestroyed);
		//		System.out.println("termsInMatch: " + termsInMatch);
		//		System.out.println("varsInMatch: " + varsInMatch);
		//		System.out.println("nodeVarsInMatch: " + nodeVarsInMatch);
		//		System.out.println("edgeVarsInMatch: " + edgeVarsInMatch);
		//		System.out.println("returnNodeSet: " + returnNodeSet);
		//		System.out.println("returnEdgeSet: " + returnEdgeSet);
		//		System.out.println("termsInWhere: " + termsInWhere);

		StringBuilder cypher = new StringBuilder();
		cypher.append("MATCH ");
		for (int i = 0; i < termsInMatch.size(); i++) {
			if (i > 0) {
				cypher.append(", ");
			}
			cypher.append(termsInMatch.get(i));
			
//			if (useViewName == false) {
//				cypher.append(termsInMatch.get(i).replace("${vn}", ""));
//			} else {
//				cypher.append(termsInMatch.get(i).replace("${vn}", " {view: \"" + from + "\"}"));
//			}
		}

		if (useViewName == true) {
			for (String s : nodeVarsInMatch) {
				termsInWhere.add(s + ".view = \"" + from + "\"");	
			}
		}
		if (useCreatedDestroyed == true) {
			for (String s : varsInMatch) {
				termsInWhere.add(s + ".c >= 0");
				termsInWhere.add(s + ".d > 1");
			}
		}
		for (int i = 0; i < termsInWhere.size(); i++) {
			if (i == 0) {
				cypher.append("\nWHERE ");
			} else {
				cypher.append(" AND ");
			}
			cypher.append(termsInWhere.get(i));
		}

		cypher.append("\nRETURN ");
		int processedVarsInReturn = 0;
		for (String s : returnNodeSet) {
			if (processedVarsInReturn > 0) {
				cypher.append(", ");
			}
			cypher.append(s).append(".uid");
			processedVarsInReturn++;
		}
		for (String s : returnEdgeSet) {
			if (processedVarsInReturn > 0) {
				cypher.append(", ");
			}
			cypher.append(s).append(".uid");
			processedVarsInReturn++;
		}
		cypher.append("\n");
		
		System.out.println("[QueryToCypher] cypher: " + cypher);

		return cypher.toString();
	}

	@Override 
	public Void visitMatch_clause(GraphTransQueryParser.Match_clauseContext ctx) {
		for (int i = 0; i < ctx.hop_or_terms().hop_or_term().size(); i++) {
			boolean isNegated = false;
			StringBuilder term = new StringBuilder();
			if (ctx.hop_or_terms().hop_or_term(i).term() == null) {	// (a:A)-[x:X]->(b:C)
				HopContext hopCtx = ctx.hop_or_terms().hop_or_term(i).hop();

				if (hopCtx.negation() != null) {
					isNegated = true;
					term.append("NOT ");
				}

				// via
				String var = hopCtx.edge_term().edge_term_body().var().getText();
				StringBuilder termForEdge = new StringBuilder();
				varsInMatch.add(var);
				edgeVarsInMatch.add(var);
				termForEdge.append("-[");
				if (var.equals("_") == false) {
					termForEdge.append(var);
				}
				String edgeLabel = ""; 
				if (hopCtx.edge_term().edge_term_body().labelRegEx().label() != null) {
					edgeLabel = hopCtx.edge_term().edge_term_body().labelRegEx().label().getText();
					termForEdge.append(":").append(edgeLabel);
				}
				termForEdge.append("]->");

				ArrayList<String> endPoints = new ArrayList<String>();
				// from, to
				for (int j = 0; j < hopCtx.term().size(); j++) {
					if (j == 1) {
						term.append(termForEdge);
					}
					String endpoint = hopCtx.term(j).term_body().var().getText();
					term.append("(");
					if (endpoint.equals("_") == false) {
						term.append(endpoint);
					}
					if (hopCtx.term(j).term_body().label() != null) {
						String label = hopCtx.term(j).term_body().label().getText();
						nodeVarToLabelMap.put(endpoint, Util.addQuotes(label));
						term.append(":" + label);
					}
//					term.append("${vn}");
					term.append(")");
					varsInMatch.add(endpoint);
					nodeVarsInMatch.add(endpoint);
					endPoints.add(endpoint);
				}

				Atom a = new Atom(Config.predE);
				a.appendTerm(new Term(var, true));
				a.appendTerm(new Term(endPoints.get(0), true));
				a.appendTerm(new Term(endPoints.get(1), true));
				a.appendTerm(new Term(Util.addQuotes(edgeLabel), false));
			} else { // (s:S)
				Term_bodyContext termCtx = ctx.hop_or_terms().hop_or_term(i).term().term_body();

				String var = termCtx.var().getText();
				term.append("(").append(var);
				if (termCtx.label() != null) {
					String label = termCtx.label().getText();
					term.append(":").append(label);
//					term.append("${vn}");
					nodeVarToLabelMap.put(var, Util.addQuotes(label));
				} else {
					throw new IllegalArgumentException("Single node[" + var + "] should have a label");	
				}
				term.append(")");
				varsInMatch.add(var);
				nodeVarsInMatch.add(var);
			}
			if (isNegated == false) {
				termsInMatch.add(term.toString());
			} else {
				termsInWhere.add(term.toString());
			}
		}
		return visitChildren(ctx); 
	}

	@Override public Void visitFrom_clause(GraphTransQueryParser.From_clauseContext ctx) {
		from = ctx.ID().getText();
		return visitChildren(ctx); 
	}

	@Override public Void visitWhere_condition(GraphTransQueryParser.Where_conditionContext ctx) {
		String lvar = ctx.lop().var().getText();
		String lprop = (ctx.lop().prop() != null) ? ctx.lop().prop().getText() : "";

		String op = ctx.operator().getText();

		String rvar = (ctx.rop().var() != null) ? ctx.rop().var().getText() : "";
		String rprop = (ctx.rop().prop() != null) ? ctx.lop().prop().getText() : "";
		String rval = (ctx.rop().propValue() != null) ? ctx.rop().propValue().getText() : "";  

		StringBuilder term = new StringBuilder();
		if (lprop.equals("") == true) {
			term.append(lvar).append(".uid");
		} else {
			term.append(lvar).append(".").append(lprop);
		}
		if (op.equals("!=") == true) {
			op = "<>";
		}
		term.append(op);

		if (rval.equals("") == false) { // value
			term.append(rval);
		} else { // property
			term.append(rvar);
			if (rprop.equals("") == true) {
				term.append(".uid");
			} else {
				term.append(".").append(rprop);
			}
		}
		termsInWhere.add(term.toString());

		return visitChildren(ctx); 
	}

	@Override public Void visitReturn_clause(GraphTransQueryParser.Return_clauseContext ctx) {
		for (int i = 0; i < ctx.hop_or_terms().hop_or_term().size(); i++) {
			if (ctx.hop_or_terms().hop_or_term(i).term() == null) {				
				HopContext hopCtx = ctx.hop_or_terms().hop_or_term(i).hop();
				String from = hopCtx.term(0).term_body().var().getText();
				String to = hopCtx.term(2).term_body().var().getText();
				String var = hopCtx.term(1).term_body().var().getText();

				returnNodeSet.add(from);
				returnNodeSet.add(to);
				returnEdgeSet.add(var);
			} else { // term
				Term_bodyContext termCtx = ctx.hop_or_terms().hop_or_term(i).term().term_body();

				String var = termCtx.var().getText();
				returnNodeSet.add(var);
			}
		}

		Atom h = new Atom(new Predicate(Config.relname_query));
		for (String a : returnNodeSet) {
			h.appendTerm(new Term(a, true));
		}
		for (String a : returnEdgeSet) {
			h.appendTerm(new Term(a, true));
		}		

		return visitChildren(ctx); 
	}
}
