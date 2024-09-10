package edu.upenn.cis.db.graphtrans.catalog;

public class SchemaNode {
	private String label;
	
	public SchemaNode(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String toString() {
		return "SchemaNode " + label + "";
	}

}
