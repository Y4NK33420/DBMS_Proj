package edu.upenn.cis.db.graphtrans.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.Config.IndexType;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryBaseVisitor;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryLexer;
import edu.upenn.cis.db.graphtrans.GraphQueryParser.GraphTransQueryParser;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.neo4j.Neo4jStore;
import edu.upenn.cis.db.helper.Util;

public class CommandParser extends GraphTransQueryBaseVisitor<Void> {
	final static Logger logger = LogManager.getLogger(CommandParser.class);
	
	private String query;

	public CommandParser() {
	}

	public Void Parse(String query) {
		this.query = query;

		logger.trace("Parse query: " + query);	
		if (query.equals("")) {
			return null;
		}

		CharStream input = CharStreams.fromString(query);
		GraphTransQueryLexer lexer = new GraphTransQueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GraphTransQueryParser parser = new GraphTransQueryParser(tokens);
		ParseTree tree = parser.cmd();

		return visit(tree);
	}

	@Override 
	public Void visitQuit(GraphTransQueryParser.QuitContext ctx) {
		CommandExecutor.quit();
		
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitSchema(GraphTransQueryParser.SchemaContext ctx) {
		CommandExecutor.printSchema();
		
		return visitChildren(ctx); 
	}

	@Override 
	public Void visitConnect(GraphTransQueryParser.ConnectContext ctx) {
		String platform = ctx.getChild(1).getText();
		CommandExecutor.connect(platform);
		
//		String ip = ctx.getChild(1).getText();
//		int port = Integer.parseInt(ctx.getChild(2).getText()); // default 5518
//		CommandExecutor.connect(ip, port);
		
		return visitChildren(ctx); 
	}

	@Override 
	public Void visitDisconnect(GraphTransQueryParser.DisconnectContext ctx) {
		CommandExecutor.disconnect();
		
		return visitChildren(ctx); 
	}
	
	@Override
	public Void visitCreate_graph(GraphTransQueryParser.Create_graphContext ctx) {
		String graphName = ctx.getChild(2).getText();
		CommandExecutor.createGraph(graphName);
		
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitUse_graph(GraphTransQueryParser.Use_graphContext ctx) {
		// FIXME: should check if graph exists
		String graphName = ctx.getChild(1).getText();
		CommandExecutor.useGraph(graphName);
		
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitNode_schema(GraphTransQueryParser.Node_schemaContext ctx) {
		String label = ctx.getChild(1).getText();
		CommandExecutor.addSchemaNode(false, label);
		
		return visitChildren(ctx); 
	}

	@Override 
	public Void visitEdge_schema(GraphTransQueryParser.Edge_schemaContext ctx) { 
		String label = ctx.getChild(1).getText();
		String from = ctx.getChild(3).getText();
		String to = ctx.getChild(5).getText();
		CommandExecutor.addSchemaEdge(false, label, from, to);

		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitEgds(GraphTransQueryParser.EgdsContext ctx) {
		CommandExecutor.printEgds();
		
		return visitChildren(ctx); 
	}

	@Override
	public Void visitEgd_formula(GraphTransQueryParser.Egd_formulaContext ctx) {
		int start = ctx.getStart().getStartIndex(); 
		int stop = ctx.getStop().getStopIndex() + 1;
		String egd = query.substring(start, stop);
		CommandExecutor.addEgd(egd);

		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitCreate_view(GraphTransQueryParser.Create_viewContext ctx) {
		int start = ctx.getStart().getStartIndex(); 
		int stop = ctx.getStop().getStopIndex() + 1;
		
		String createViewQuery = query.substring(start, stop);		
				
		ViewParser parser = new ViewParser();
		TransRuleList transRuleList = parser.Parse(createViewQuery);
		CommandExecutor.createView(false, createViewQuery, transRuleList);

		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitInsert(GraphTransQueryParser.InsertContext ctx) {
		int start = ctx.getStart().getStartIndex(); 
		int stop = ctx.getStop().getStopIndex() + 1;
		
		InsertParser parser = new InsertParser();
		parser.Parse(query.substring(start, stop));
		
		int et = Util.startTimer();		
		CommandExecutor.insert(parser.getRelName(), parser.getArgs());
		long etime = Util.getElapsedTime(et);
		Util.Console.logln("[Elapsed Time] Insert: " + etime);
		
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitImport_data(GraphTransQueryParser.Import_dataContext ctx) { 
		int start = ctx.getStart().getStartIndex(); 
		int stop = ctx.getStop().getStopIndex() + 1;

		ImportParser parser = new ImportParser();
		parser.Parse(query.substring(start, stop));
		
		CommandExecutor.importFromCSV(parser.getRelName(), parser.getFilePath());
		
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitViews(GraphTransQueryParser.ViewsContext ctx) {
		CommandExecutor.printViews();
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitProgram(GraphTransQueryParser.ProgramContext ctx) {
		CommandExecutor.printProgram();
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitUser_query(GraphTransQueryParser.User_queryContext ctx) {
		CommandExecutor.query(query);
		return visitChildren(ctx); 
	}


	@Override 
	public Void visitLoad_script(GraphTransQueryParser.Load_scriptContext ctx) {
		int start = ctx.filepath().getStart().getStartIndex(); 
		int stop = ctx.filepath().getStop().getStopIndex() + 1;

		String filePath = Util.removeQuotes(query.substring(start, stop));
		CommandExecutor.loadScript(filePath);
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitDrop_graph(GraphTransQueryParser.Drop_graphContext ctx) {
		String graphName = ctx.ID().toString();		
		CommandExecutor.dropGraph(graphName);
		
		return visitChildren(ctx); 
	}

//	@Override 
//	public Void visitCreate_asr(GraphTransQueryParser.Create_asrContext ctx) {
//		String viewName = ctx.view_name().getText();
//		CommandExecutor.createIndex(IndexType.ASR, viewName);
//		return visitChildren(ctx); 
//	}
	
	@Override 
	public Void visitCreate_ssr(GraphTransQueryParser.Create_ssrContext ctx) {
		String viewName = ctx.view_name().getText();	
		CommandExecutor.createIndex(IndexType.SSR, viewName);
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitOption(GraphTransQueryParser.OptionContext ctx) {
		GraphTransQueryParser.Option_nameContext nameCtx = ctx.option_name();
		GraphTransQueryParser.Opt_on_offContext onoffCtx = ctx.opt_on_off();
		boolean isOn = false;
		
		if (onoffCtx.opt_off() == null) {
			isOn = true;
		}
		
		String name = nameCtx.getText();
		if (nameCtx.opt_typecheck() != null) {
			Config.setTypeCheckEnabled(isOn);
		} else if (nameCtx.opt_prunetypecheck() != null) { 
			Config.setTypeCheckPruningEnabled(isOn);
		} else if (nameCtx.opt_prunequery() != null) {
			Config.setSubQueryPruningEnabled(isOn);
		} else if (nameCtx.opt_ivm() != null) {
			Config.setUseIVM(false);
		}
		
		Util.Console.logln("Option [" + name + "] is set to [" + (isOn ? "on" : "off") + "]");
		
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitList(GraphTransQueryParser.ListContext ctx) {
		CommandExecutor.listGraphs();
		return visitChildren(ctx); 
	}
	
	@Override 
	public Void visitPrepare_database(@NotNull GraphTransQueryParser.Prepare_databaseContext ctx) {
		String dirPath = ctx.filepath().getText();
		String platform = ctx.platform_name().getText().toLowerCase();
		
		CommandExecutor.prepareDatabase(Util.removeQuotes(dirPath), platform);
		return visitChildren(ctx); 
	}
	
//	@Override 
//	public Void visitPlatform(GraphTransQueryParser.PlatformContext ctx) {
//		String platform = ctx.platform_name().getText();
//		CommandExecutor.setPlatform(platform);
//		
//		return visitChildren(ctx); 
//	}
//	
//	@Override 
//	public Void visitServer_start(GraphTransQueryParser.Server_startContext ctx) {
//		CommandExecutor.startServer();
//		
//		return visitChildren(ctx); 
//	}
//	
//	@Override 
//	public Void visitServer_stop(GraphTransQueryParser.Server_stopContext ctx) {
//		CommandExecutor.stopServer();
//
//		return visitChildren(ctx); 
//	}
//	@Override 
//	public Void visitPrepare_database(GraphTransQueryParser.Prepare_databaseContext ctx) {
//		String database = ctx.ID().getText();
//		CommandExecutor.prepareDatabase(database);
//		
//		return visitChildren(ctx); 
//	}
}