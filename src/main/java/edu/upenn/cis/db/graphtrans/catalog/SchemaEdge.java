package edu.upenn.cis.db.graphtrans.catalog;

public class SchemaEdge {
	private String label;
	private String from;
	private String to;

	public SchemaEdge(String label, String from, String to) {
		this.label = label;
		this.from = from;
		this.to = to;
	}

	public String getLabel() {
		return label;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	public String toString() {
		return "SchemaEdge " + label + " (" + from + " -> " + to + ")";
	}
}
