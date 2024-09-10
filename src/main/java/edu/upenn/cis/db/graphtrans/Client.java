package edu.upenn.cis.db.graphtrans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.graphtrans.catalog.Schema;
import edu.upenn.cis.db.helper.Performance;
import edu.upenn.cis.db.helper.Util;

import org.apache.commons.cli.*;

/**
 * The main entry point for the client program
 * 
 * @author sbnet21
 *
 */
public class Client {
	final static Logger logger = LogManager.getLogger(Client.class);

	/**
	 * The entry main method for the client program
	 * 
	 * @param args the script filepath (optional)
	 */
	public static void main(String[] args) throws Exception {
		Options options = new Options();

		Option input = new Option("i", "input", true, "input script file path");
		input.setRequired(false);
		options.addOption(input);
		
		Option platform = new Option("p", "platform", true, "platform (lb, pg, n4, ds)");
		platform.setRequired(false);
		options.addOption(platform);

		Option viewtype = new Option("v", "viewtype", true, "viewtype (mv, hv, vv)");
		viewtype.setRequired(false);
		options.addOption(platform);

		Option ssr = new Option("s", "ssr", false, "ssr");
		platform.setRequired(false);
		options.addOption(platform);

		Option output = new Option("o", "output", true, "output file");
		output.setRequired(false);
		options.addOption(output);

		Option config = new Option("c", "config", true, "config file");
		config.setRequired(false);
		options.addOption(config);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;

		String filepath = null;
		String configFilePath = "conf/graphview.conf";

		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("i")) {
				filepath = cmd.getOptionValue("i");
			}
			if (cmd.hasOption("c")) {
				configFilePath = cmd.getOptionValue("c");
			}
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("graphview", options);

			System.exit(1);
		}
		Config.load(configFilePath);

		Util.Console.logln("Welcome to the Graph Database supporting Transformation Views.");

		Config.initialize();
		Config.setUseQuerySubQueryInPostgres(false);
		Config.setPostgresEnabled(false);
		Config.setTypeCheckEnabled(false);
		Config.setTypeCheckPruningEnabled(false);
		Config.setSubQueryPruningEnabled(false);
		Config.setAnswerEnabled(true);
		Config.setUseMatchSSIndex(false);
		Config.setUseSimpleDatalogEngine(true);

		GraphTransServer.initialize(); // after setting Config

		Config.setWorkspace("SYN");
		Performance.setup(Config.getWorkspace(), "very first test"); // should be before readCommand

		Console console = new Console();

		CommandExecutor.setConsole(console);
		CommandExecutor.readCommand(filepath);

		Util.Console.logln("Close connection.");

		// For recording performance
		String viewType = "vv";
		if (GraphTransServer.getNumTransRuleList() > 0) {
			if (GraphTransServer.getTransRuleList(0).getViewType().equals("materialized") == true) {
				viewType = "mv";
			} else if (GraphTransServer.getTransRuleList(0).getViewType().equals("hybrid") == true) {
				viewType = "hv";
			} else if (GraphTransServer.getTransRuleList(0).getViewType().equals("asr") == true) {
				viewType = "asr";
			}
		}
		Performance.setPlatform(Config.getPlatform());
		Performance.setViewType(viewType);

		Performance.setPruneSubquery(Config.isSubQueryPruningEnabled());
		Performance.setPruneTypecheck(Config.isTypeCheckPruningEnabled());
		Performance.setUseSubstituteIndex(false);
		// Performance.setLevel(GraphTransServer.getNumTransRuleList());
		Performance.setUseSequential(true);
		Performance.setEgdSize(GraphTransServer.getEgdList().size());
		Performance.setGraphSize(1000);
		Performance.setSchemaSize(Schema.getSchemaNodes().size() + Schema.getSchemaEdges().size());
		Performance.setJSON();

		System.out.println("[perf] " + Performance.getCSVBody()); //getJSON());
		
		Performance.logPerformance();
	}
}
