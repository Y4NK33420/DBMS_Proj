package edu.upenn.cis.db.graphtrans.parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryBaseVisitor;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryLexer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser;
import edu.upenn.cis.db.graphtrans.datastructure.Egd;

public class EgdParser extends GraphTransQueryBaseVisitor<Void> {
	final static Logger logger = LogManager.getLogger(EgdParser.class);

	private static Egd egd;
	private static EgdParser instance = new EgdParser();
	
	private EgdParser() {
	}

	public static Egd Parse(String query) {
		CharStream input = CharStreams.fromString(query);
		GraphTransQueryLexer lexer = new GraphTransQueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GraphTransQueryParser parser = new GraphTransQueryParser(tokens);
		ParseTree tree = parser.egd_formula(); // begin parsing at rule 'r'
		
		logger.trace("Query: " + query);
		
		egd = new Egd(query);
		instance.visit(tree);
		
		return egd;
	}
	
	@Override 
	public Void visitEgd_formula(GraphTransQueryParser.Egd_formulaContext ctx) {
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitNode_atom_body(GraphTransQueryParser.Node_atom_bodyContext ctx) {
		String var = ctx.var().getText();
		
		Atom atom = new Atom(Config.predN);
		atom.appendTerm(new Term(var, true));
		if (ctx.var_or_literal().var() == null) {
//			String label = Util.removeQuotes(ctx.var_or_literal().literal().getText());
			String label = ctx.var_or_literal().literal().getText();
			atom.appendTerm(new Term(label, false));	
		} else { // variable
			atom.appendTerm(new Term(ctx.var_or_literal().var().getText(), true));
		}
		
		egd.addAtomToLhs(atom);
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitNodeProp_atom_body(GraphTransQueryParser.NodeProp_atom_bodyContext ctx) { 
		String var = ctx.var().getText();
		String prop = ctx.literal().get(0).getText();
		String value = ctx.literal().get(1).getText();
		
		Atom atom = new Atom(Config.predNP);
		atom.appendTerm(new Term(var, true));
		atom.appendTerm(new Term(prop, false));
		atom.appendTerm(new Term(value, false));
		egd.addAtomToLhs(atom);

		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitEdge_atom_body(GraphTransQueryParser.Edge_atom_bodyContext ctx) { 
		String var = ctx.var().get(0).getText();
		String from = ctx.var().get(1).getText();
		String to = ctx.var().get(2).getText();
		
		Atom atom = new Atom(Config.predE);
		atom.appendTerm(new Term(var, true));
		atom.appendTerm(new Term(from, true));
		atom.appendTerm(new Term(to, true));
		if (ctx.var_or_literal().var() == null) {
			String label = ctx.var_or_literal().literal().getText();
			atom.appendTerm(new Term(label, false));	
		} else { // variable
			atom.appendTerm(new Term(ctx.var_or_literal().var().getText(), true));	
		}
		egd.addAtomToLhs(atom);
		
		return visitChildren(ctx); 
	}

	@Override 
	public Void visitEdgeProp_atom_body(GraphTransQueryParser.EdgeProp_atom_bodyContext ctx) { 
		String var = ctx.var().getText();
		String prop = ctx.literal().get(0).getText();
		String value = ctx.literal().get(1).getText();
		
		Atom atom = new Atom(Config.predEP);
		atom.appendTerm(new Term(var, true));
		atom.appendTerm(new Term(prop, false));
		atom.appendTerm(new Term(value, false));
		egd.addAtomToLhs(atom);

		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitEgd_equality(GraphTransQueryParser.Egd_equalityContext ctx) {
		String lop = ctx.operand1().getText();
		String rop = ctx.operand2().getText();
		boolean rVariable = true;
		if (ctx.operand2().var() == null) {
			rVariable = false;
		}
		
		Atom atom = new Atom(Config.predOpEq);
		atom.appendTerm(new Term(lop, true));
		atom.appendTerm(new Term(rop, rVariable));
		egd.addAtomToRhs(atom);

		return visitChildren(ctx); 
	}


}
