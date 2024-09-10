package edu.upenn.cis.db.datalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Term;

public class DatalogClause {
	private Atom head;
	private ArrayList<Atom> heads;
	private ArrayList<Atom> body;
	private String desc = "";
	
	public DatalogClause() {
		initialize(false);
	}
	
	public void addDesc(String desc) {
		if (this.desc.equals("") == false) {
			this.desc += ", ";
		}
		this.desc += desc; 
	}

	public String getDesc() {
		return desc;
	}
	
	public DatalogClause(ArrayList<Atom> h, ArrayList<Atom> b) {
		initialize(false);

		heads = h;
		body = b;
	}
	
	public DatalogClause(ArrayList<Atom> h, HashSet<Atom> b) {
		initialize(false);

		heads = h;
		for (Atom a : b) {
			body.add(a);
		}
	}
	
	public DatalogClause(Atom h, Set<Atom> b) {
		initialize(false);
		
		head = h;
		for (Atom a : b) {
			body.add(a);
		}
		
		// assertion
		HashSet<String> vars = new HashSet<String>();
		for (Term t : head.getTerms()) {
			if (t.isVariable() == true) {
				vars.add(t.toString());
			}
		}
		for (Atom a : b) {
			for (Term t : a.getTerms()) {
				if (t.isVariable() == true) {
					vars.remove(t.toString());
				}
			}
		}
		
		if (vars.size() > 0) {
			throw new IllegalArgumentException("[ERROR] vars[" + vars + "] in the head do not exist in body. head: " + head + " body: " + b);
		}
	}
	
	public DatalogClause(boolean q) {
		initialize(q);
	}	
	
	private void initialize(boolean q) {
		head = null;
		heads = new ArrayList<Atom>();
		body = new ArrayList<Atom>();
	}
	
	public Atom getHead() {
		return head;
	}

	public void setHead(Atom head) {
		this.head = head;
	}
	
	public void addAtomToHeads(Atom b) {
		head = b; // FIXME:
		heads.add(b);
	}
	
	public void addAtomToBody(Atom b) {
		body.add(b);
	}

	public ArrayList<Atom> getBody() {
		return body;
	}

	public void setBody(ArrayList<Atom> body) {
		this.body = body;
	}
	public ArrayList<Atom> getHeads() {
		return heads;
	}
	
	public String getAtomsToString(ArrayList<Atom> atoms) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < atoms.size(); i++) {
			if (str.length() > 0 && atoms.get(i) != null) {
				str.append(", ");
			}
			if (atoms.get(i) != null) {
				str.append(atoms.get(i));
			}
		}
		return str.toString();
	}
	
	public void normalizeVarNames() {
		Set<String> uniQuanVars = new LinkedHashSet<String>(); // universally quantified variables
		Set<String> existQuanVars = new LinkedHashSet<String>(); // existentially quantified variables
		
		for (int i = 0; i < head.getTerms().size(); i++) {
			Term t = head.getTerms().get(i);
			if (t.isVariable() == true) {
				uniQuanVars.add(t.getVar());
			}
		}
		for (int i = 0; i < body.size(); i++) {
			Atom a = body.get(i);
			for (int j = 0; j < a.getTerms().size(); j++) {
				Term t = a.getTerms().get(j);
				if (t.isVariable() == true && uniQuanVars.contains(t.getVar()) == false) {
					existQuanVars.add(t.getVar());
				}
			}
		}
//		System.out.println("uniQuanVars: " + uniQuanVars);
//		System.out.println("existQuanVars: " + existQuanVars);
		
		List<String> uniQuanVarsList = new ArrayList<String>(uniQuanVars);
		List<String> existQuanVarsList = new ArrayList<String>(existQuanVars);
		
		for (int i = 0; i < head.getTerms().size(); i++) {
			try {
				if (head.getTerms().get(i).isVariable() == true) {
					Term t = (Term)head.getTerms().get(i).clone();
					t.setVar("u"+uniQuanVarsList.indexOf(t.getVar()));
					head.getTerms().set(i, t);
				}
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (int i = 0; i < body.size(); i++) {
			Atom a = body.get(i);
			for (int j = 0; j < a.getTerms().size(); j++) {
				if (a.getTerms().get(j).isVariable() == true) {
					try {
						Term t = (Term)a.getTerms().get(j).clone();
						if (uniQuanVars.contains(t.getVar()) == true) {
							t.setVar("u"+uniQuanVarsList.indexOf(t.getVar()));
							a.getTerms().set(j, t);
						} else if (existQuanVars.contains(t.getVar()) == true) {
							t.setVar("_e"+existQuanVarsList.indexOf(t.getVar()));
							a.getTerms().set(j, t);
						} else {
							throw new IllegalArgumentException("variable [" + t.getVar() + "] is invalid.");
						}
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}	
//		System.out.println("head: " + head);
//		System.out.println("body: " + body);
	}
	
	public String toString() {		
		if (heads.size() > 0) {
			return getAtomsToString(heads) + " <- " + getAtomsToString(body);
		} else {
			return head + " <- " + getAtomsToString(body);
		} 
	}
}
