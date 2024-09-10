package edu.upenn.cis.db.datalog.simpleengine;

public class IntegerSimpleTerm implements SimpleTerm {
	int value;
	
	public IntegerSimpleTerm(int value) {
		this.value = value;
	}
	
	@Override
	public int getInt() {
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
        return value;
    }

	@Override
	public int compareTo(SimpleTerm o) {
		// TODO Auto-generated method stub
		if (o instanceof IntegerSimpleTerm == false) {
			System.out.println("[IntegerSimpleTerm] value: " + value + " compareTo  o: " + o);
		}
		return Integer.compare(value, ((IntegerSimpleTerm)o).getInt());
	}

	@Override
	public String getString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLong() {
		// TODO Auto-generated method stub
		return 0;
	}
}
