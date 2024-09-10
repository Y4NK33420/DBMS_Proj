package edu.upenn.cis.db.graphtrans.datastructure;

import java.util.ArrayList;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.graphtrans.Config.IndexType;

public class TransRuleList {
	public ArrayList<TransRule> getTransRuleList() {
		return TransRuleList;
	}

	public void setTransRuleList(ArrayList<TransRule> transRuleList) {
		TransRuleList = transRuleList;
	}

	private ArrayList<TransRule> TransRuleList;
	private String query;
	
	private String viewType;
	private String viewName;
	private String baseName;
	
	private boolean isDefaultMap;
	
	// index
	private IndexType indexType;
	private ArrayList<DatalogClause> indexRuleList;
	private ArrayList<ArrayList<Atom>> outputPatternList; // each variables exists in the head of indexRuleList (used by SSR only) 
	
	private long level;
	
	public TransRuleList(String viewName, String baseName, String viewType, long level, String query) {
		setDefaultMap(false);

		this.viewName = viewName;
		this.baseName = baseName;
		this.viewType = viewType;
		this.indexType = IndexType.NONE;
		this.level = level;
		this.query = query;
		TransRuleList = new ArrayList<TransRule>();
		indexRuleList = new ArrayList<DatalogClause>();
		outputPatternList = new ArrayList<ArrayList<Atom>>();
	}
	
	public void addOutputPatternList(ArrayList<Atom> a) {
		outputPatternList.add(a);
	}
	
	public ArrayList<ArrayList<Atom>> getOutputPatternList() {
		return outputPatternList;
	}
	
	public void addIndexRuleList(DatalogClause c) {
		indexRuleList.add(c);
	}
	
	public ArrayList<DatalogClause> getIndexRuleList() {
		return indexRuleList;
	}
	
	public long getLevel() {
		return level;
	}

	public void addTransRule(TransRule t) {
		t.setLevel(level);
		TransRuleList.add(t);
	}
	
	public TransRule getTransRule(int index) {
		return TransRuleList.get(index);
	}

	public int getNumTransRuleList() {
		return TransRuleList.size();
	}

	public String getViewName() {
		return viewName;
	}

	public String getBaseName() {
		return baseName;
	}
	
	public IndexType getIndexType() {
		return indexType;
	}

	public String toString() {
		//return query;
		return "TransRuleList viewName: " + viewName + "\n"
				+ "query: " + query + "\n"
				+ "indexType: " + indexType + "\n"
				+ "indexRuleList: " + indexRuleList + "\n"
				+ "outputPatternList: " + outputPatternList + "\n";
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}

	public void setIndexType(IndexType indexType) {
		this.indexType = indexType;
	}

	public boolean isDefaultMap() {
		return isDefaultMap;
	}

	public void setDefaultMap(boolean isDefaultMap) {
		this.isDefaultMap = isDefaultMap;
	}

}
