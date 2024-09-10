package edu.upenn.cis.db.datalog.simpleengine;

import java.util.ArrayList;
import java.util.List;

public class Tuple<T extends Comparable<T>> {
	List<T> values;

	public Tuple() {
		this.values = new ArrayList<T>();
	}
	
	public Tuple(T ... values) {
		this.values = new ArrayList<T>();
		for (T v : values) 
            this.values.add(v); 
	}

	public Tuple(List<T> values) {
		this.values = values;
	}
	
	public Tuple<T> copy() { // FIXME: hard copy
		Tuple<T> newTuple = new Tuple<T>();
		for (T t : values) {
			newTuple.getTuple().add(t);
		}
		return newTuple;
	}
	
	public List<T> getTuple() {
		return values;
	}
	
	public int hashCode() {
        return getTuple().get(0).hashCode();
    }
	
	@Override
    public boolean equals(Object obj) 
    { 
	    if (this == obj) 
            return true; 
          
        if(obj == null || obj.getClass()!= this.getClass()) 
            return false; 
          
        Tuple<T> t = (Tuple<T>)obj; 
          
        return (getTuple().equals(t.getTuple())); 
    } 
      	
	public String toString() {
		String str = "";
		for (int i = 0; i < values.size(); i++) {
			if (values.get(i) instanceof IntegerSimpleTerm) {
				str += String.format("%12s", ((IntegerSimpleTerm)values.get(i)).getInt());	
			} else if (values.get(i) instanceof LongSimpleTerm) {
				str += String.format("%12s", ((LongSimpleTerm)values.get(i)).getLong());
			} else if (values.get(i) instanceof StringSimpleTerm) {
				str += String.format("%12s", ((StringSimpleTerm)values.get(i)).getString());
			} else {
				throw new IllegalArgumentException("values.get(i): " + values.get(i) + " is null i: " + i + " values: " + values);
			}
		}
		return str;
//		return values.toString();
	}
	
	public static void main(String[] args) {
		Tuple<SimpleTerm> t1 = new Tuple<SimpleTerm>();
		t1.getTuple().add(new IntegerSimpleTerm(12));
		t1.getTuple().add(new StringSimpleTerm("GG"));

		Tuple<SimpleTerm> t2 = new Tuple<SimpleTerm>();
		t2.getTuple().add(new IntegerSimpleTerm(12));
		t2.getTuple().add(new StringSimpleTerm("GG"));
		
		System.out.println("t1.equals(t2)?: " + t1.equals(t2));
	}
}