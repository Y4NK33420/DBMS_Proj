package edu.upenn.cis.db.datalog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.logicblox.connect.BloxCommand.Relation;
import com.logicblox.connect.ConnectBlox.Response;

import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.helper.Util;
import edu.upenn.cis.db.logicblox.LogicBlox;

/**
 * Query Executor.
 * @author sbnet21
 *
 */
public class DatalogExecutor {
	final static Logger logger = LogManager.getLogger(DatalogExecutor.class);


	public static Relation run(DatalogClause q) {
		String query = q.toString() + ".";
		String blockName = "query_temp";

		Response ans = LogicBlox.runExecBlock(Config.getWorkspace(), query, true);
		Relation rel = ans.getTransaction().getCommand(0).getExec().getReturnLocal(0);

		return rel;
	}

	
//	public static Relation run(DatalogProgram p, DatalogClause q) {
//		String query = "_(" + q.getHead().getAtomBodyStr() + ") <- " + q.getHead() + ".";
//		
//		String blockName = "query_temp";
//		Util.Console.logln("length: " + p.getString().length());	
//		
//		if (Config.isSubQueryPruningEnabled() == false) {
//			LogicBlox.runAddBlock(Config.getWorkspace(), blockName, p.getString());
//		}
//
//		Response ans = LogicBlox.runExecBlock(Config.getWorkspace(), query, true);
//		
//		Relation rel = ans.getTransaction().getCommand(0).getExec().getReturnLocal(0);
//				
//		// FIXME: remove blockName
////		LogicBlox.runRemoveBlock(Config.getWorkspace(), blockName);
//		
//		return rel;
//	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		logger.info("Done.");
	}
}
