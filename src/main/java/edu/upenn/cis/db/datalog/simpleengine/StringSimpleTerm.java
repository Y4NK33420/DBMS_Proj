package edu.upenn.cis.db.datalog.simpleengine;

public class StringSimpleTerm implements SimpleTerm {
	String value;
	
	public StringSimpleTerm(String value) {
		this.value = value;
	}	
	
	@Override
	public String toString() {
		return "StringTerm: " + value;
	}
	
	public int hashCode() {
        return value.hashCode();
    }

	@Override
	public boolean equals(Object o) {
		return compareTo((SimpleTerm)o) == 0;
		
	}
	
	@Override
	public int compareTo(SimpleTerm o) {
		// TODO Auto-generated method stub
		return value.compareTo(((StringSimpleTerm)o).getString());
	}
	
	@Override
	public String getString() {
		// TODO Auto-generated method stub
		return value;
	}
	@Override
	public int getInt() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getLong() {
		// TODO Auto-generated method stub
		return 0;
	}

}
