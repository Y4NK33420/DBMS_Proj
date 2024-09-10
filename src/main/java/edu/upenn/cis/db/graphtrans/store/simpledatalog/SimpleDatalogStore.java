package edu.upenn.cis.db.graphtrans.store.simpledatalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

//import org.apache.commons.lang.NotImplementedException;

import com.logicblox.connect.ConnectBlox.Response;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Type;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogExecutor;
import edu.upenn.cis.db.datalog.DatalogParser;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.datalog.simpleengine.IntegerSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Relation;
import edu.upenn.cis.db.datalog.simpleengine.SimpleDatalogEngine;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Tuple;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.graphdb.datalog.BaseRuleGen;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.helper.Util;
import edu.upenn.cis.db.logicblox.LogicBlox;

public class SimpleDatalogStore implements Store {
	private HashMap<String, SimpleDatalogEngine> databases; 
	private String currentDatabase = null;
	private SimpleDatalogEngine db = null;
//	private UUID uuid = null;
	
	@Override
	public String getDBname() {
		// TODO Auto-generated method stub
//		throw new NotImplementedException();
		return null;
	}

	@Override
	public boolean connect() {
		// TODO Auto-generated method stub
		databases = new HashMap<String, SimpleDatalogEngine>();
		return true;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		databases.clear();
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
//		throw new NotImplementedException();		
	}

	@Override
	public void createSchema(String dbname, Predicate p) {
		// TODO Auto-generated method stub
		ArrayList<String> attributes = new ArrayList<String>();
		for (int i = 0; i < p.getArgNameList().size(); i++) {
			attributes.add("_" + Integer.toString(i+1));
		}
		Relation<SimpleTerm> rel = new Relation<SimpleTerm>(new ArrayList<String>(attributes));
//		System.out.println("[createSchema] dbname: " + dbname + " rel: " + p.getRelName());
		databases.get(dbname).addRel(p.getRelName(), rel);		
	}

	@Override
	public void createView(String name, List<DatalogClause> cs, boolean isMaterialized) {
		db.executeRules(cs);
	}

	@Override
	public void createView(DatalogProgram p, TransRuleList transRuleList) {
		// TODO Auto-generated method stub
		int createdViewStartId = p.getCreatedViewId();
		for (int i = createdViewStartId; i < p.getHeadRules().size(); i++) {
			List<DatalogClause> rules = p.getRules(p.getHeadRules().get(i));
			String name = rules.get(0).getHead().getPredicate().getRelName();
			boolean isMaterialized = p.getEDBs().contains(name);
			
//			System.out.println("rules: " + rules);
			db.executeRules(rules);
//			for (DatalogClause c : rules) {
//				db.executeRule(c);
//			}
			p.incCreatedViewId();
		}
	}

//	@Override
//	public void createViewIndex(List<String> rules) {
//		// TODO Auto-generated method stub
//		throw new NotImplementedException();		
//	}

	@Override
	public StoreResultSet getQueryResult(List<DatalogClause> cs) {
		// TODO Auto-generated method stub
		StoreResultSet rs = new StoreResultSet();
		Relation rel = db.executeQuery(cs);
//		System.out.println("[runQuery] rel: " + rel + " cs: " + cs);
		rs.getResultSet().addAll(rel.getTuples());
		rs.getColumns().addAll(rel.getColumns());
		
		return rs;
	}
	
	@Override
	public StoreResultSet getQueryResult(DatalogClause c) {
		StoreResultSet rs = new StoreResultSet();
		Relation rel = databases.get(currentDatabase).executeQuery(c);
		rs.getResultSet().addAll(rel.getTuples());
		rs.getColumns().addAll(rel.getColumns());
		
		return rs;
	}

	@Override
	public void printRelation(String relname) {
		// TODO Auto-generated method stub
//		throw new NotImplementedException();
	}

	@Override
	public void addTableIndex(Predicate p, ArrayList<String> cols) {
		// TODO Auto-generated method stub
//		throw new NotImplementedException();		
	}

	@Override
	public void addTableIndex(String name, ArrayList<Integer> arrayList) {
		// TODO Auto-generated method stub
//		throw new NotImplementedException();
	}

	@Override
	public boolean createDatabase(String name) {
		SimpleDatalogEngine db = new SimpleDatalogEngine();
		databases.put(name, db);

		
		return true;
	}

	@Override
	public boolean deleteDatabase(String name) {
		if (databases.containsKey(name) == true) {
			databases.remove(name);
		}
		return true;
	}

	@Override
	public boolean useDatabase(String name) {
		if (databases.containsKey(name) == true) {
			currentDatabase = name;
			db = databases.get(currentDatabase);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ArrayList<String> listDatabases() {
		return new ArrayList<String>(databases.keySet());
	}
	
	@Override
	public ArrayList<String> listRelations(String dbname) {
		return null; //databases.get(dbname).getRelationList();
	}
	
	@Override
	public String getListRelationStr(String dbname) {
		return databases.get(dbname).getRelationList();
	}


	@Override
	public void addTuple(String rel, ArrayList<SimpleTerm> arrayList) {
		// TODO Auto-generated method stub
		if (db.getRelation(rel) == null) {
			throw new IllegalArgumentException("rel: " + rel + " terms: " + arrayList + " storeRel: " + getListRelationStr(currentDatabase));
		}
//		Util.console_logln("db: " + currentDatabase + " rel: " + rel + " terms: " + arrayList, 4);
		db.getRelation(rel).addTuple(new Tuple<SimpleTerm>(arrayList));
	}

	@Override
	public long importFromCSV(String relName, String filePath) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void debug() {
//		System.out.println(databases);
//		System.out.println("TEST1: " + databases.get("TEST1").getRelationList());
//		System.out.println("TEST1: " + databases.get("TEST1"));
		
		DatalogProgram program = new DatalogProgram();
		DatalogParser parser = new DatalogParser(program);
		
		System.out.println("[debug] currentDatabase: " + currentDatabase + " db: "+ databases.get(currentDatabase));
		
//		System.exit(0);
//		DatalogClause c = parser.ParseQuery("_(id,label) <- E_g(id,_from,_to,label).");
//		System.out.println("output: " + databases.get(currentDatabase).execute(c));
//		
//		c = parser.ParseQuery("_(id,_from,from,_to) <- E_g(id,_from,_to,label), MAP_v0(_from,_,from,_).");//, MAP_v0(_to,_,to,_).");
//		System.out.println("output: " + databases.get(currentDatabase).execute(c));

//		
//		E_ADD_v0(id,from,to,label) <- E_g(id,_from,to,label), MAP_v0(_from,_,from,_), !MAP_v0(to,_,_,_)
//		E_ADD_v0(id,from,to,label) <- E_g(id,from,_to,label), !MAP_v0(from,_,_,_), MAP_v0(_to,_,to,_)
//		E_ADD_v0(id,from,to,label) <- E_g(id,_from,_to,label), MAP_v0(_from,_,from,_), MAP_v0(_to,_,to,_)
	}

	@Override
	public void createConstructors() {
		// TODO Auto-generated method stub
		
	}

//	public void removeRelation(String dbname, String relname) {
//		// TODO Auto-generated method stub
//		db.removeRelation(relname);
//	}
}
