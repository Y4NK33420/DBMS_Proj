package edu.upenn.cis.db.graphtrans.catalog;

/**
 * View Catalog class.
 * @author sbnet21
 *
 */
public class ViewCatalog {
	private String viewName;
	private String baseName;
	private String type;
	private String query;
	private long level;
	
	public ViewCatalog(String v, String b, String t, String q, long l) {
		viewName = v;
		baseName = b;
		type = t;
		query = q;
		level = l;
	}
	
	public String getViewName() {
		return viewName;
	}
	
	public String getBaseName() {
		return baseName;
	}
	
	public String getType() {
		return type;
	}
	
	public String getQuery() {
		return query;
	}
	
	public long getLevel() {
		return level;
	}
	
	public String toString() {
		return query;
	}
}
