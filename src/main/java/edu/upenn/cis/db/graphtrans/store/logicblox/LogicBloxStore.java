package edu.upenn.cis.db.graphtrans.store.logicblox;

import java.util.ArrayList;
import java.util.List;

//import org.apache.commons.lang.NotImplementedException;

import com.logicblox.connect.BloxCommand.Relation;
import com.logicblox.connect.ConnectBlox.Response;

import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Type;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.datalog.simpleengine.IntegerSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.graphdb.datalog.BaseRuleGen;
import edu.upenn.cis.db.graphtrans.graphdb.datalog.ImportRuleGen;
import edu.upenn.cis.db.graphtrans.graphdb.datalog.ViewRule;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.logicblox.LogicBlox;

public class LogicBloxStore implements Store {
	private String dbname;
	
	@Override
	public String getDBname() {
		// TODO Auto-generated method stub
		return dbname;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		LogicBlox.disconnect();
		dbname = null;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
	}

	@Override
	public void createSchema(String dbname, Predicate p) {
		// TODO Auto-generated method stub
		StringBuilder str = new StringBuilder(p.getRelName());
		str.append("(");
		for (int i = 0; i < p.getArgNameList().size(); i++) {
			if (i > 0) {
				str.append(",");
			}
			str.append(p.getArgNameList().get(i));
		}
		str.append(") -> ");
		for (int i = 0; i < p.getArgNameList().size(); i++) {
			if (i > 0) {
				str.append(",");
			}
			if (p.getTypes().get(i).equals(Type.Integer) == true) {
				str.append("int(");
			} else {
				str.append("string(");
			}
			str.append(p.getArgNameList().get(i));
			str.append(")");
		}
		str.append(".");
		
		Response res = LogicBlox.runAddBlock(dbname, null, str.toString());
//		System.out.println("str: " + str.toString());
	}
//
//	@Override
//	public void addTuple(Atom a) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public void createView(String name, List<DatalogClause> cs, boolean isMaterialized) {
		// TODO Auto-generated method stub
		String logic = "";
		for (int i = 0; i < cs.size(); i++) {
			DatalogClause c = cs.get(i);
			logic += c + ".\n";
		}
		Response res = LogicBlox.runAddBlock(Config.getWorkspace(), "", logic);
//		System.out.println("333logic: " + logic + "\n res: " + res);

	}

	@Override
	public StoreResultSet getQueryResult(List<DatalogClause> cs) {
		// TODO Auto-generated method stub
		String logic = "";
		String temp_query_blk = "_temp_query";
		
		DatalogClause query = null;
		for (int i = 0; i < cs.size(); i++) {
			DatalogClause c = cs.get(i);
			if (c.getHead().getRelName().contentEquals("_") == true) {
				query = c;
			} else {
				logic += c + ".\n";
			}
		}
		if (logic.contentEquals("") == false) {
			Response res = LogicBlox.runAddBlock(Config.getWorkspace(), temp_query_blk, logic);
//			System.out.println("1logic: " + logic + "\n res: " + res);
		}
		
		// query
		Response res = LogicBlox.runExecBlock(Config.getWorkspace(), query.toString() + ".\n", true);
		Relation rel = res.getTransaction().getCommand(0).getExec().getReturnLocal(0);
		
		StoreResultSet rs = new StoreResultSet();
		rs.setFromLogicBloxRelation(rel);
		
		LogicBlox.runRemoveBlock(Config.getWorkspace(), temp_query_blk);
		return rs;
	}

	@Override
	public StoreResultSet getQueryResult(DatalogClause c) {
//		throw new NotImplementedException();
		return null;
	}

//	@Override
//	public StoreResultSet getQueryResult(DatalogClause q) {
//		// TODO Auto-generated method stub
//		String query = q.toString() + ".";
//		StoreResultSet rs = new StoreResultSet();
//		Response res = LogicBlox.runExecBlock(Config.getWorkspace(), query, true);
//		Relation rel = res.getTransaction().getCommand(0).getExec().getReturnLocal(0);
//		rs.setFromLogicBloxRelation(rel);
//		
//		return rs;
//	}

	@Override
	public void printRelation(String relname) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTableIndex(Predicate p, ArrayList<String> cols) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTableIndex(String name, ArrayList<Integer> arrayList) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean createDatabase(String name) {
		if (LogicBlox.getListWorkSpaces().contains(name) == true) {
			return false;
		}
		// TODO Auto-generated method stub
		LogicBlox.createWorkspace(name, true);

		BaseRuleGen.addRule();
		
		ArrayList<Predicate> preds = BaseRuleGen.getPreds();
		ArrayList<Predicate> predsLBOnly = BaseRuleGen.getPredsLBOnly();

//		StringBuilder str = new StringBuilder();
//		str.append("nid(n), nid_id(n : i) -> int(i).\n")
//			.append("eid(n), eid_id(n : i) -> int(i).\n")
//			.append("lang:autoNumbered(`nid_id).\n")
//			.append("lang:autoNumbered(`eid_id).\n");
//		LogicBlox.runAddBlock(name, "_constructor", str.toString());
		
		for (Predicate p : preds) {
			createSchema(name, p);
		}
		
		for (Predicate p : predsLBOnly) {
			createSchema(name, p);
		}		
		
//		Util.writeToFile("logicblox.logic", rule, false); // LB TO FILE
//		Response res = LogicBlox.runAddBlock(name, "base", rule);
//		
//		// import EDB schemas only
//		String edbRules = BaseRuleGen.getBaseGraphRuleBaseEDB(false);				
//		DatalogParser parser = new DatalogParser(GraphTransServer.getProgram());
//		parser.ParseAndAddRules(edbRules);				
		
		return true;		
	}

	@Override
	public boolean deleteDatabase(String name) {
		// TODO Auto-generated method stub
		
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		if (LogicBlox.deleteWorkspace(name) == false) {
			return false;
		}
		if (dbname != null && dbname.contentEquals(name) == true) {
			dbname = null;
		}
		return true;
	}

	@Override
	public boolean useDatabase(String name) {
		// TODO Auto-generated method stub
//		if (LogicBlox.getListWorkSpaces().contains(name) == false) {
//			return false;
//		}
		dbname = name;
		return true;
	}

	@Override
	public void addTuple(String rel, ArrayList<SimpleTerm> a) {
		// TODO Auto-generated method stub
		StringBuilder logic = new StringBuilder("+").append(rel).append("(");
		
		for (int i = 0; i < a.size(); i++) {
			if (i > 0) {
				logic.append(",");
			}
			if (a.get(i) instanceof StringSimpleTerm) {
				logic.append("\"").append(a.get(i).getString()).append("\"");
			} else if (a.get(i) instanceof LongSimpleTerm){
				logic.append(a.get(i).getLong()); 
			} else if (a.get(i) instanceof IntegerSimpleTerm){
				logic.append(a.get(i).getInt());
			}
		}
		logic.append(").");
		
//		System.out.println("logic: " + logic.toString());
		Response res = LogicBlox.runExecBlock(Config.getWorkspace(), logic.toString(), false);
//		System.out.println("res: " + res);
	}

	@Override
	public long importFromCSV(String relName, String filePath) {
		// TODO Auto-generated method stub
		String rule = ImportRuleGen.getRule(relName, filePath);
		Response res = LogicBlox.runExecBlock(Config.getWorkspace(), rule, false);
		return 0;
	}

	@Override
	public boolean connect() {
		// TODO Auto-generated method stub
		String ip = Config.get("logicblox.ip");
		String port = Config.get("logicblox.port");
		String adminport = Config.get("logicblox.adminport");
		if (ip == null || port == null || adminport == null) {
			return false;
		}
		return LogicBlox.connect(ip, Integer.parseInt(port), Integer.parseInt(adminport)); 		
	}

	@Override
	public ArrayList<String> listDatabases() {
		// TODO Auto-generated method stub
		return LogicBlox.getListWorkSpaces();
	}

	@Override
	public void createView(DatalogProgram p, TransRuleList transRuleList) { // p is not used
//		System.out.println("createView In LB");
		
		DatalogProgram program = new DatalogProgram();
//		System.out.println("program1: " + program.getString());
		ViewRule.addViewRuleToProgram(program, transRuleList, false, true);
//		System.out.println("program2: " + program.getString());
		String viewRule = program.getString();

		String newViewRule = viewRule.replace("#", "//");
		String blk_name = transRuleList.getViewName() + "_view_creation";
		
		Response res = LogicBlox.runAddBlock(Config.getWorkspace(), blk_name, newViewRule);
		
//		System.out.println("newViewRule: " + newViewRule);
//		System.out.println("createView res:" + res);
	}

//	@Override
//	public void createViewIndex(List<String> rules) {
//		// TODO Auto-generated method stub
//		DatalogProgram p = GraphTransServer.getProgram();
//		DatalogParser parser = new DatalogParser(p);
//		
//		for (int i = 0; i < rules.size(); i++) {
//			DatalogClause q = parser.ParseQuery(rules.get(i));
////			System.out.println("[LB-createViewIndex] q: " + q);
//			p.addRule(q);
//		}
//
//		Response res0 = LogicBlox.runAddBlock(Config.getWorkspace(), null, p.getString());
////		DatalogQueryProcessor.runQuery(null, p);
//	}
	
	@Override
	public void debug() {
		
	}	
	
	@Override
	public ArrayList<String> listRelations(String dbname) {
		return null;
	}	
	
	@Override
	public String getListRelationStr(String dbname) {
		return null;
	}
	
	@Override
	public void createConstructors() {
		String logic = GraphTransServer.getProgram().getString(true);
//		System.out.println("[code-34234] logic: " + logic);
		Response res = LogicBlox.runAddBlock(Config.getWorkspace(), null, logic);

//		System.out.println("createConstructors logic: " + logic + "\nres0: " + res);
	}

}
