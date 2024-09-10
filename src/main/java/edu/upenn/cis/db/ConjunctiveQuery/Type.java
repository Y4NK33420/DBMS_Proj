package edu.upenn.cis.db.ConjunctiveQuery;

/**
 * Constructor of Type.
 * 
 * @param name the name of this new type
 */
public class Type {
	public static Type Integer = new Type("INT");
	public static Type String = new Type("STRING");

	public String name;

	public Type(String name){
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
}
