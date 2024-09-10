package edu.upenn.cis.db.graphtrans.datastructure;

import java.util.ArrayList;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;

/**
 * Constructor of Egd. Represent an EGD constraint.
 * 
 * @author sbnet21
 *
 */

public class Egd {
	private ArrayList<Atom> lhs;
	private ArrayList<Atom> rhs;

	public Egd(String str) {
		lhs = new ArrayList<Atom>();
		rhs = new ArrayList<Atom>();
	}

	public void addAtomToLhs(Atom a) {
		lhs.add(a);
	}

	public void addAtomToRhs(Atom a) {
		rhs.add(a);
	}

	public String toString() {
		return lhs + "->" + rhs;
	}

	public ArrayList<Atom> getLhs() {
		return lhs;
	}

	public ArrayList<Atom> getRhs() {
		return rhs;
	}
}
