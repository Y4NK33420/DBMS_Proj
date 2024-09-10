package edu.upenn.cis.db.datalog.simpleengine;

public interface SimpleTerm extends Comparable<SimpleTerm> {
	public String toString();
	
	public String getString();
	
	public int getInt();
	
	public long getLong();
}
