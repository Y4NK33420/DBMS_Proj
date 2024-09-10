package edu.upenn.cis.db.graphtrans.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryBaseVisitor;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryLexer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser;

public class LabelRegExParser extends GraphTransQueryBaseVisitor<Void> {
	final static Logger logger = LogManager.getLogger(LabelRegExParser.class);

	private static LabelRegExParser instance = new LabelRegExParser();
	
	private static boolean isKleeneStar = false;
	private static String labels;
	
	private LabelRegExParser() {
	}

	public static Pair<Boolean, String> Parse(String query) {
		CharStream input = CharStreams.fromString(query);
		GraphTransQueryLexer lexer = new GraphTransQueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GraphTransQueryParser parser = new GraphTransQueryParser(tokens);
		ParseTree tree = parser.labelRegEx(); // begin parsing at rule 'r'
		
		logger.trace("Query: " + query);
		
//		egd = new Egd(query);
		instance.visit(tree);
		
		return Pair.of(isKleeneStar, labels);
	}
	
	@Override 
	public Void visitLabelRegEx(@NotNull GraphTransQueryParser.LabelRegExContext ctx) {
		if (ctx.label() != null) {
			isKleeneStar = false;
			labels = ctx.label().getText();
		} else {
			isKleeneStar = true;
			labels = ctx.labels().getText();	
		}
		
		return visitChildren(ctx); 
	}
	
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		System.out.println("OUTPUT: " + LabelRegExParser.Parse("(AB)*"));
//		System.out.println("OUTPUT: " + LabelRegExParser.Parse("B"));
//	}
}
