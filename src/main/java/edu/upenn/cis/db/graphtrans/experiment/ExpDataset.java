package edu.upenn.cis.db.graphtrans.experiment;

import java.util.ArrayList;

public class ExpDataset {
	private String name;
	private boolean execute;
	private boolean synthetic;
	
	private ArrayList<String> egds;
	private ArrayList<String> schemas;
	
	private ArrayList<ArrayList<ArrayList<String>>> transrules; // setId, ruleId, subRuleId
	private ArrayList<ArrayList<String>> queries; // setId, queryId

	private long base_selectivity;
	private ArrayList<Long> sizes;
	private long base_size;
	private ArrayList<Long> selectivities;
	
	private String csv_default;
	private String csv_neo4j;
		
	public ExpDataset() {
		egds = new ArrayList<String>();
		schemas = new ArrayList<String>();
		transrules = new ArrayList<ArrayList<ArrayList<String>>>();
		queries = new ArrayList<ArrayList<String>>();
	}

	public String getCsv_default() {
		return csv_default;
	}

	public String getCsv_neo4j() {
		return csv_neo4j;
	}
	
	public long getBase_size() {
		return base_size;
	}

	public ArrayList<Long> getSizes() {
		return sizes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isExecute() {
		return execute;
	}

	public void setExecute(boolean execute) {
		this.execute = execute;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder("ExpDataset: {\n");
		str.append("  name: ").append(name).append(",\n");
		str.append("  execute: ").append(execute).append("\n");
		str.append("  schemas: ").append(schemas).append("\n");
		str.append("  egds: ").append(egds).append("\n");
		str.append("  transrules: ").append(transrules).append("\n");
		str.append("  queries: ").append(queries).append("\n");
		str.append("  base_selectivity: ").append(base_selectivity).append("\n");
		str.append("  sizes: ").append(sizes).append("\n");
		str.append("  base_size: ").append(base_size).append("\n");
		str.append("  selectivities: ").append(selectivities).append("\n");
		str.append("  csv_default: ").append(csv_default).append("\n");
		str.append("  csv_neo4j: ").append(csv_neo4j).append("\n");
		str.append("}");
		
		return str.toString();
	}

	public ArrayList<String> getEgds() {
		return egds;
	}

	public void setEgds(ArrayList<String> egds) {
		this.egds = egds;
	}

	public ArrayList<String> getSchemas() {
		return schemas;
	}

	public void setSchemas(ArrayList<String> schemas) {
		this.schemas = schemas;
	}

	public ArrayList<ArrayList<ArrayList<String>>> getTransrules() {
		return transrules;
	}

	public boolean isSynthetic() {
		return synthetic;
	}

	public void setSynthetic(boolean synthetic) {
		this.synthetic = synthetic;
	}	
	
	public ArrayList<ArrayList<String>> getQueries() {
		return queries;
	}

	public long getBase_selectivity() {
		return base_selectivity;
	}

	public void setBase_selectivity(long base_selectivity) {
		this.base_selectivity = base_selectivity;
	}

	public ArrayList<Long> getSelectivities() {
		return selectivities;
	}

	public void setSelectivities(ArrayList<Long> selectivities) {
		this.selectivities = selectivities;
	}

	public void setBase_size(long base_size) {
		this.base_size = base_size;
	}

	public void setSizes(ArrayList<Long> sizes) {
		this.sizes = sizes;
	}

	public void setCsv_default(String csv_default) {
		this.csv_default = csv_default;
	}

	public void setCsv_neo4j(String csv_neo4j) {
		this.csv_neo4j = csv_neo4j;
	}
}
