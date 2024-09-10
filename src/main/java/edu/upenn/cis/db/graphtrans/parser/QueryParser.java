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
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryBaseVisitor;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryLexer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser.HopContext;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser.Term_bodyContext;
import edu.upenn.cis.db.helper.Util;

public class QueryParser extends GraphTransQueryBaseVisitor<Void> {
	final static Logger logger = LogManager.getLogger(QueryParser.class);

//	private DatalogProgram program;
	private String query;
	private String from;
	
	private DatalogClause clause;
	
//	private DatalogProgram program;
	
	private HashMap<String, String> nodeVarToLabelMap;
	private HashMap<String, String> edgeVarToLabelMap;
	
	private HashMap<String, Atom> nodeVarToAtomMap;
	private HashMap<String, Atom> edgeVarToAtomMap;
	
	private HashSet<String> returnNodeSet;
	private HashSet<String> returnEdgeSet;
	
	private HashSet<Atom> returnInterpretedSet;
	
	private HashMap<String, HashMap<String, ArrayList<Atom>>> propertyAtoms; // var |-> (prop |-> predicates)

	public QueryParser() {
		nodeVarToLabelMap = new HashMap<String, String>();
		edgeVarToLabelMap = new HashMap<String, String>();
		nodeVarToAtomMap = new HashMap<String, Atom>();
		edgeVarToAtomMap = new HashMap<String, Atom>();
		returnNodeSet = new HashSet<String>();
		returnEdgeSet = new HashSet<String>();
		returnInterpretedSet = new HashSet<Atom>();
		propertyAtoms = new HashMap<String, HashMap<String, ArrayList<Atom>>>();
//		program = new DatalogProgram();
	}
	
	public String getFrom() {
		return from;
	}

	public DatalogClause Parse(String query) {
		this.query = query;
		
		clause = new DatalogClause();

//		System.out.println("[QueryParser] Parse query: " + query);

		CharStream input = CharStreams.fromString(query);
		GraphTransQueryLexer lexer = new GraphTransQueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GraphTransQueryParser parser = new GraphTransQueryParser(tokens);
		ParseTree tree = parser.user_query();
		visit(tree);
		
//		System.out.println("[QueryParser] Parse clause: " + clause);

//		System.out.println("[QueryParser] clause: " + clause);
		for (Atom a : clause.getBody()) {
			if (a.isInterpreted() == false) {
				String relName = a.getPredicate().getRelName();
				if (from != null) {
					relName += "_" + from; // + "v";
				}
				a.setPredicate(new Predicate(relName));
			}
		}
		
		
//		System.out.println("program===>");
//		System.out.println(program);
		
		return clause;
	}

	@Override 
	public Void visitMatch_clause(GraphTransQueryParser.Match_clauseContext ctx) 
	{
		for (int i = 0; i < ctx.hop_or_terms().hop_or_term().size(); i++) {
			if (ctx.hop_or_terms().hop_or_term(i).term() == null) {				
				HopContext hopCtx = ctx.hop_or_terms().hop_or_term(i).hop();
				// from
				String from = hopCtx.term(0).term_body().var().getText();
				if (hopCtx.term(0).term_body().label() != null) {
					String label = hopCtx.term(0).term_body().label().getText();
					nodeVarToLabelMap.put(from, Util.addQuotes(label));
				}
	
				// to
				String to = hopCtx.term(1).term_body().var().getText();
				if (hopCtx.term(1).term_body().label() != null) {
					String label = hopCtx.term(1).term_body().label().getText();	
					nodeVarToLabelMap.put(to, Util.addQuotes(label));
				}
	
				// via
				String var = hopCtx.edge_term().edge_term_body().var().getText();
				String label = hopCtx.edge_term().edge_term_body().labelRegEx().getText();
				
//				System.out.println("CODE 412412 label: " + label);
				
				Atom a = new Atom(Config.predE);
				a.appendTerm(new Term(var, true));
				a.appendTerm(new Term(from, true));
				a.appendTerm(new Term(to, true));
				a.appendTerm(new Term(Util.addQuotes(label), false));
			
				clause.addAtomToBody(a);
				
//					a.appendTerm(new Term(var + "_label", true));
//					
//					Atom b = new Atom(Config.predOpEq);
//					b.appendTerm(new Term(var + "_label", true));
//					b.appendTerm(new Term(Util.addQuotes(label), false));
//					
//					edgeVarToAtomMap.put(var, a);
//					
//					clause.addAtomToBody(a);
//					clause.addAtomToBody(b);				
			} else { // term
				Term_bodyContext termCtx = ctx.hop_or_terms().hop_or_term(i).term().term_body();
				
				String var = termCtx.var().getText();
				if (termCtx.label() != null) {
					String label = termCtx.label().getText();
					nodeVarToLabelMap.put(var, Util.addQuotes(label));
				} else {
					throw new IllegalArgumentException("Single node[" + var + "] should have a label");	
				}
			}
		}

		// node
		for (HashMap.Entry<String,String> e : nodeVarToLabelMap.entrySet()) {
			Atom a = new Atom(Config.predN);
			String var = e.getKey();
			String label = e.getValue();
			
			a.appendTerm(new Term(var, true));
			a.appendTerm(new Term(label, false));
//			Atom b = new Atom(Config.predOpEq);
//			b.appendTerm(new Term(var + "_label", true));
//			b.appendTerm(new Term(label, false));
			
			nodeVarToAtomMap.put(var, a);
			
			clause.addAtomToBody(a);
//			clause.addAtomToBody(b);				
		}
		
		return visitChildren(ctx); 
	}

	@Override public Void visitFrom_clause(GraphTransQueryParser.From_clauseContext ctx) {
		from = ctx.ID().getText();
//		System.out.println("[adsfsd3432] from: " + from);
		return visitChildren(ctx); 
	}
	
	@Override public Void visitWhere_clause(GraphTransQueryParser.Where_clauseContext ctx) {
		visitChildren(ctx);
		ParserHelper.processWhereClause(nodeVarToAtomMap, clause.getBody(), propertyAtoms, null);

		return null;
	}

	@Override public Void visitWhere_condition(GraphTransQueryParser.Where_conditionContext ctx) {
		String var = ctx.lop().var().getText();
		String prop = "";
		if (ctx.lop().prop() != null) {
			prop = ctx.lop().prop().getText();
		}

		String op = ctx.operator().getText();

		String rvar;
		if (ctx.rop().var() != null) {
			rvar = ctx.rop().var().getText();
//			System.out.println("CODE 4114 - rvar: " + rvar);			
		} else if (ctx.rop().propValue() != null) {
//			String rvar = ctx.rop().var().getText();
			String val = ctx.rop().propValue().int_or_literal().getText();		
			ParserHelper.processWhereCondition(var, prop, op, val, false, clause.getBody(), propertyAtoms, null, null);
			
//			throw new IllegalArgumentException("rop propValue is not supported yet ctx.rop: " + ctx.rop().getText());
		} else if (ctx.rop().prop() == null) {
			throw new IllegalArgumentException("rop prop is not supported yet ctx.rop: " + ctx.rop().getText());
	//		ParserHelper.processWhereCondition(var, prop, op, val, clause.getBody(), propertyAtoms, null, null);
			//ParserHelper.processWhereCondition(var, prop, op, val, clause.getBody(), propertyAtoms, null, null);
		}
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
		
//		System.out.println("@@@returnNodeSet: " + returnNodeSet);
//		System.out.println("@@@returnEdgeSet: " + returnEdgeSet);
//		
//		System.out.println("@@@nodeVarToAtomMap: " + nodeVarToAtomMap);
		
//		System.out.println("returnNodeSet: " + returnNodeSet);
//		System.out.println("returnEdgeSet: " + returnEdgeSet);

		Atom h = new Atom(new Predicate(Config.relname_query));
		for (String a : returnNodeSet) {
			h.appendTerm(new Term(a, true));
//			h.appendTerm(new Term(a + "_l", true));
//			h.appendTerm(new Term(a + "_r", true));
//			h.appendTerm(new Term(nodeVarToLabelMap.get(a), false));
		}
		for (String a : returnEdgeSet) {
			h.appendTerm(new Term(a, true));
//			h.appendTerm(new Term(a + "_l", true));
//			h.appendTerm(new Term(a + "_r", true));
//			h.appendTerm(new Term(edgeVarToLabelMap.get(a), false));
		}		
		clause.addAtomToHeads(h);

//		try {
//			for (String a : returnNodeSet) {
//				Atom b = nodeVarToAtomMap.get(a);
//				Atom h = (Atom)b.clone();
//				String relName = "ANS_N";
//				h.setPredicate(new Predicate(relName));
//				clause.addAtomToHeads(h);
//				
////				program.addRule(clause);
////				System.out.println("h1: " + h);
//			}
//			for (String a : returnEdgeSet) {
//				Atom b = edgeVarToAtomMap.get(a);
//				Atom h = (Atom)b.clone();
//				String relName = "ANS_E";
//				h.setPredicate(new Predicate(relName));
//				clause.addAtomToHeads(h);
//				
////				program.addRule(clause);
////				System.out.println("h2: " + h);
//			}
//		} catch (Exception e) {
//			throw new IllegalArgumentException();
//		}
		
		return visitChildren(ctx); 
	}

}
