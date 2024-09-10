package edu.upenn.cis.db.datalog.simpleengine;

public class LongSimpleTerm implements SimpleTerm {
	long value;
	
	public LongSimpleTerm(long value) {
		this.value = value;
	}
	
	@Override
	public long getLong() {
		return value;
	}
	
	@Override
	public String toString() {
		return "IntegerTerm: " + value;
	}
	
	@Override
	public boolean equals(Object o) {
		return compareTo((SimpleTerm)o) == 0;
	}
	
	public int hashCode() {
        return (int)value;
    }

	@Override
	public int compareTo(SimpleTerm o) {
		// TODO Auto-generated method stub
		if (o instanceof LongSimpleTerm == false) {
			System.out.println("[LongSimpleTerm] value: " + value + " compareTo  o: " + o);
		}
		return Long.compare(value, ((LongSimpleTerm)o).getLong());
	}

	@Override
	public String getString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInt() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
