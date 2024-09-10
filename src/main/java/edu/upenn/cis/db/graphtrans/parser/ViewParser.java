package edu.upenn.cis.db.graphtrans.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryBaseVisitor;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryLexer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser;
import edu.upenn.cis.db.graphtrans.catalog.Catalog;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;

public class ViewParser extends GraphTransQueryBaseVisitor<Void> {
	final static Logger logger = LogManager.getLogger(ViewParser.class);

	private String query;
	private String viewName;
	private String baseName;
	private String viewType;
	TransRuleList transRuleList;
	
	public TransRuleList Parse(String query) {
		this.query = query;

		logger.trace("Parse query: " + query);	
		if (query.equals("")) {
			return null;
		}

		CharStream input = CharStreams.fromString(query);
		GraphTransQueryLexer lexer = new GraphTransQueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GraphTransQueryParser parser = new GraphTransQueryParser(tokens);
		ParseTree tree = parser.create_view();

		visit(tree);
		
		return transRuleList;
	}
	
	@Override 
	public Void visitCreate_view(GraphTransQueryParser.Create_viewContext ctx) {
		/*
		 * 1. Create TransRuleList
		 * 2. TypeCheck with schema and egds
		 * 3. If passed, create view
		 */
		int start = ctx.getStart().getStartIndex(); 
		int stop = ctx.getStop().getStopIndex() + 1;
		
		String createViewQuery = query.substring(start, stop);
		
//		System.out.println("createViewQuery: " + createViewQuery);
		
		viewName = ctx.view_name().getText();
		baseName = "g";
		viewType = "virtual";
		
		if (ctx.view_type() != null) {
			viewType = ctx.view_type().getText();
		}
		if (ctx.view_base() != null) {
			baseName = ctx.view_base().view_name().getText();
		}

		start = ctx.view_definition().getStart().getStartIndex();
		stop = ctx.view_definition().getStop().getStopIndex() + 1;
		
		long level = 1;
		if (baseName.contentEquals("g") == false) {
//			System.out.println("baseName: " + baseName);
//			System.out.println(Catalog.loadViewCatalog());
			
			level = GraphTransServer.getNumOfTransRuleListList();
			//get Catalog.loadViewCatalog().get(baseName).getLevel() + 1;
		}
		transRuleList = new TransRuleList(viewName, baseName, viewType, level, createViewQuery);
		transRuleList.setDefaultMap(ctx.with_default() != null);
	
		
		for (int i = 0; i < ctx.view_definition().trans_rule().size(); i++) {
			start = ctx.view_definition().trans_rule(i).getStart().getStartIndex();
			stop = ctx.view_definition().trans_rule(i).getStop().getStopIndex() + 1;
			String subquery = query.substring(start, stop);
			logger.trace("transRule i: " + i + " => " + subquery);
			
			TransRuleParser parser = new TransRuleParser();
			TransRule transRule = parser.Parse(subquery);
			transRule.computePatterns();
			//transRule.show();
			transRuleList.addTransRule(transRule);
		}
		
		return visitChildren(ctx); 
	}
	
	public String getViewName() {
		return viewName;
	}
	
	public String getBaseName() {
		return baseName;
	}
}
