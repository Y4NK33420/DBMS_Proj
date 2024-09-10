package edu.upenn.cis.db.graphtrans.store;

import java.util.ArrayList;
import java.util.List;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;

public interface Store {
	String getDBname();
	
	boolean connect(); 
	
	void disconnect();
			
	void initialize();
	
	/**
	 * Create a schema of EDB
	 */
	void createSchema(String dbname, Predicate p);
	
	
	/**
	 * Add a tuple to EDB
	 */
//	void addTuple(Atom a);
	
	/**
	 * Create a view of IDB (materialized or virtual). 
	 * Support disjunctive query.
	 */
	void createView(String name, List<DatalogClause> cs, boolean isMaterialized);
	
	void createView(DatalogProgram p, TransRuleList transRuleList);
	
//	void createViewIndex(List<String> rules);
	StoreResultSet getQueryResult(List<DatalogClause> cs);

	StoreResultSet getQueryResult(DatalogClause c);

	/**
	 * Get answer of a query
	 */
//	StoreResultSet getQueryResult(DatalogClause q); // get answer of query

	/**
	 * Print a relation
	 * @param relname
	 */
	void printRelation(String relname);

	void addTableIndex(Predicate p, ArrayList<String> cols);
	
	void addTableIndex(String name, ArrayList<Integer> arrayList);

	boolean createDatabase(String name);

	boolean deleteDatabase(String name);

	boolean useDatabase(String name);
	
	ArrayList<String> listDatabases();
	
	ArrayList<String> listRelations(String dbname);
	
	String getListRelationStr(String dbname);

	void addTuple(String rel, ArrayList<SimpleTerm> arrayList);

	long importFromCSV(String relName, String filePath);
	
	void debug();

	void createConstructors();
}
