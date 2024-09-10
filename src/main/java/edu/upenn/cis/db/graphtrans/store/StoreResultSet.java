package edu.upenn.cis.db.graphtrans.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import com.logicblox.connect.BloxCommand.Column;
import com.logicblox.connect.BloxCommand.Relation;

import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Tuple;

public class StoreResultSet {
	private ArrayList<String> columns;
	private ArrayList<Tuple<SimpleTerm>> resultSet;
	
	public StoreResultSet() {
		columns = new ArrayList<String>();
		resultSet = new ArrayList<Tuple<SimpleTerm>>();
	}
	
	public StoreResultSet(ArrayList<String> cols, HashSet<Tuple<SimpleTerm>> tuples) {
		columns = cols;
		resultSet = new ArrayList<Tuple<SimpleTerm>>();
		resultSet.addAll(tuples);
	}

	public ArrayList<Tuple<SimpleTerm>> getResultSet() {
		return resultSet;
	}
	
	public void setFromPostgresResultSet() {
	}

	public void setFromLogicBloxRelation(Relation rel) {
		List<Column> columns = rel.getColumnList();
		int cols = columns.size();
		int rows = (cols == 0) ? 0 : rel.getColumn(0).getInt64Column().getValuesCount();
		
		for (int i = 0; i < rows; i++) {
			Tuple<SimpleTerm> t = new Tuple<SimpleTerm>();
			
			for (int j = 0; j < cols; j++) {
				if (columns.get(j).hasStringColumn() == true) {
					t.getTuple().add(new StringSimpleTerm(rel.getColumn(j).getStringColumn().getValues(i)));
				} else if (columns.get(j).hasInt64Column() == true) {
					t.getTuple().add(new LongSimpleTerm(rel.getColumn(j).getInt64Column().getValues(i)));
				} else {
					throw new UnsupportedOperationException("Not supported types (only String, Long are supported).");
				}
			}
			resultSet.add(t);
		}
	}
	
	public void setFromNeo4jResult(Result result) {
		while (result.hasNext()) {
			Tuple<SimpleTerm> t = new Tuple<SimpleTerm>();

			Map<String,Object> row = result.next();
			for ( Entry<String,Object> column : row.entrySet() ) {
				String val = column.getValue().toString();
				t.getTuple().add(new LongSimpleTerm(Long.parseLong(val)));

				resultSet.add(t);
			}
		}
		System.out.println("[setFromNeo4jResult] resultSet: " + resultSet);
	}

	public ArrayList<String> getColumns() {
		return columns;
	}
	
	public String toString() {
		String str = "StoreResultSet\n\tcolumns: " + columns + "\n";
		str += "\tresultSet: \n";
		for (int i = 0; i < resultSet.size(); i++) {
			 str += resultSet.get(i) + "\n";
		}
//		str += "\tresultSet: " + resultSet; 
		return str;
	}
}
