package edu.upenn.cis.db.graphtrans.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryBaseVisitor;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryLexer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser;
import edu.upenn.cis.db.helper.Util;

public class ImportParser extends GraphTransQueryBaseVisitor<Void> {
	final static Logger logger = LogManager.getLogger(InsertParser.class);

	private String relName; 
	private String filePath;
	
	public ImportParser() {
	}
	
	public String getRelName() {
		return relName;
	}

	public String getFilePath() {
		return filePath;
	}
	
	public void Parse(String query) {
		CharStream input = CharStreams.fromString(query);
		GraphTransQueryLexer lexer = new GraphTransQueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GraphTransQueryParser parser = new GraphTransQueryParser(tokens);
		ParseTree tree = parser.import_data();
		
		visit(tree);
	}
	
	@Override 
	public Void visitImport_data(GraphTransQueryParser.Import_dataContext ctx) {
		relName = ctx.rel_name().getText();
		filePath = Util.removeQuotes(ctx.filepath().getText());

		return visitChildren(ctx); 
	}
}
