package edu.upenn.cis.db.graphtrans.parser;

import java.util.ArrayList;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryBaseVisitor;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryLexer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser;

public class InsertParser extends GraphTransQueryBaseVisitor<Void>{
	final static Logger logger = LogManager.getLogger(InsertParser.class);

	private String relName; 
	private ArrayList<String> args;
	
	public InsertParser() {
		args = new ArrayList<String>();
	}
	
	public String getRelName() {
		return relName;
	}

	public ArrayList<String> getArgs() {
		return args;
	}
	
	public void Parse(String query) {
		CharStream input = CharStreams.fromString(query);
		GraphTransQueryLexer lexer = new GraphTransQueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GraphTransQueryParser parser = new GraphTransQueryParser(tokens);
		ParseTree tree = parser.insert();
		
		visit(tree);
	}
	
	@Override 
	public Void visitInsert(GraphTransQueryParser.InsertContext ctx) {
		relName = ctx.rel_name().getText();

		return visitChildren(ctx); 
	}

	@Override 
	public Void visitInsert_body(GraphTransQueryParser.Insert_bodyContext ctx) {
		
		for (int i = 0; i < ctx.int_or_literal().size(); i++) {
			String str = ctx.int_or_literal().get(i).getText();
//			if (ctx.int_or_literal().get(i).INTEGER() != null) { // int
				args.add(str);
//			} else { // literal
//				args.add(Util.addSlashes(str));
//			}
		}
		
		return visitChildren(ctx); 
	}	
}
