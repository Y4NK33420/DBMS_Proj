package edu.upenn.cis.db.ConjunctiveQuery;

import java.util.ArrayList;

/**
 * Constructor of Predicate
 * @author sbnet21
 *
 */
public class Predicate implements Cloneable {
	private boolean isInterpreted;
	private String relName;
	
	private ArrayList<String> argNameList;
	private ArrayList<Type> types;
	
    public Object clone() throws CloneNotSupportedException {
    	Predicate p = (Predicate)super.clone();
 
//    	p.setRelName(new String(relName));
//    	p.setInterpreted(isInterpreted);
    	
    	return p;
    }
    
	public void setArgNames(String ...args) {
		argNameList = new ArrayList<String>();
		for (String arg : args) {
			argNameList.add(arg);
		}
	}

	public void setTypes(Type ...ts) {
		types = new ArrayList<Type>();
		for (Type t : ts) {
			types.add(t);
		}
	}
	
	public ArrayList<String> getArgNameList() {
		return argNameList;
	}

	public String getRelName() {
		return relName;
	}
	
	public void setRelName(String r) {
		relName = r;
	}

	public ArrayList<Type> getTypes() {
		return types;
	}
	
//	public Predicate() {
//	}
	
	public Predicate(String relName) {
		initialize(relName, false);
	}

	public Predicate(String relName, boolean isInterpreted) {
		initialize(relName, isInterpreted);
	}

	private void initialize(String relName, boolean isInterpreted) {
		this.isInterpreted = isInterpreted;
		this.relName = relName;
		argNameList = new ArrayList<String>();
		types = new ArrayList<Type>();
	}

	public void addArg(String argName) {
		addArg(argName, null);
	}

	public void addArg(String argName, Type t) {
		argNameList.add(argName);
		types.add(t);
	}
	
	public void setArg(String argName, Type t) {
		for (int i = 0; i < argNameList.size(); i++) {
			if (argNameList.get(i).contentEquals(argName)) {
				types.set(i, t);
			}
		}
	}

	public boolean isInterpreted() {
		return isInterpreted;
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(relName);
		str.append("(");
		for (int i = 0; i < argNameList.size(); i++) {
			str.append(argNameList.get(i));
			str.append(":");
			str.append(types.get(i));
			if (i + 1 < argNameList.size()) {
				str.append(",");
			}
		}
		str.append(")");

		return str.toString();
	}

	public void setInterpreted(boolean interpreted) {
		// TODO Auto-generated method stub
		this.isInterpreted = interpreted;
	}
	
	public boolean hasSameRelName(Predicate p) {
		if (p == null) {
			throw new IllegalArgumentException("p is null");
		}
		return relName.equals(p.getRelName());
	}
}
