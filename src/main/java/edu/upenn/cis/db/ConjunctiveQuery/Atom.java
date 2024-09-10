package edu.upenn.cis.db.ConjunctiveQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import edu.upenn.cis.db.datalog.rewriter.Helper;
import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Tuple;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.helper.Util;

/**
 * Constructor of Atom
 * @author sbnet21
 *
 * Reference: http://i.stanford.edu/hazy/tuffy/doc/
 */
public class Atom implements Cloneable {
	private Predicate predicate;
	private ArrayList<Term> terms;
	private String baseName; // before adornment
	private ArrayList<Boolean> adornment;
	private boolean isNegated;
	private HashMap<Integer, String> interpreted; // N(a,"X") is translated to N(a,v0), v0="X", then have (1,"v0")
	
	public String getRelName() {
		return predicate.getRelName();
	}
	
	public ArrayList<Boolean> getAdornment() {
		if (adornment == null) {
			adornment = new ArrayList<Boolean>();
		}
		return adornment;
	}
	
	public Atom(String relName, String ... vars) {
		predicate = new Predicate(relName);
		terms = new ArrayList<Term>();
		
        for (String v : vars ){
        	terms.add(new Term(v, true));
        }		
	}
	
	public Atom(boolean nonnegated, String relName, String ... vars) {
		predicate = new Predicate(relName);
		this.isNegated = !nonnegated;
		terms = new ArrayList<Term>();
		
        for (String v : vars ){
        	terms.add(new Term(v, true));
        }		
	}
	
	public Atom(String relName, HashSet<String> vars) {
		predicate = new Predicate(relName);
		terms = new ArrayList<Term>();
		
        for (String v : vars ){
        	terms.add(new Term(v, true));
        }		
	}

	public Atom(String relName, ArrayList<String> vars) {
		predicate = new Predicate(relName);
		terms = new ArrayList<Term>();
		
        for (String v : vars ){
        	terms.add(new Term(v, true));
        }		
	}

	public Atom(Predicate pred, ArrayList<Term> t) {
		predicate = pred;
		terms = t;
	}
	
	public Atom(Predicate pred) {
		if (pred == null) {
			predicate = new Predicate("_");			
		} else { 
			predicate = pred;
		}
		setNegated(false);
		terms = new ArrayList<Term>();
	}

	public Atom(Predicate pred, boolean isNonNegated) {
		predicate = pred;
		setNegated(!isNonNegated);
		terms = new ArrayList<Term>();
	}
	
//	public Atom getClone() {
//		Atom a = new Atom(new Predicate(new String(predicate.getRelName()))); //(Atom)super.clone();
//		
//		if (predicate.equals(Config.predE) == true) {
//			a.setPredicate(Config.predE);
//		} else if (predicate.equals(Config.predN) == true) {
//			a.setPredicate(Config.predN);
//		} else {  
//		// FIXME: not yet
//			a.getPredicate().setRelName(predicate.getRelName());
//			a.getPredicate().setInterpreted(predicate.isInterpreted());
//		}
//		
//		a.setTerms(new ArrayList<Term>());
//		for (int i = 0; i < terms.size(); i++) {
//			Term t = null;
//			try {
//				t = (Term)terms.get(i).clone();
//			} catch (CloneNotSupportedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			a.appendTerm(t);
//		}		
//		return a;
//	}
	
	@Override
    public Object clone() throws CloneNotSupportedException {
		Atom a = (Atom)super.clone(); 
		Predicate p = (Predicate)predicate.clone();

//		if (predicate.equals(Config.predE) == true) {
//			a.setPredicate(Config.predE);
//		} else if (predicate.equals(Config.predN) == true) {
//			a.setPredicate(Config.predN);
//		} else {  
		if (p.equals(predicate) == true) {
			System.out.println("[OMG4] p.equals(predicate) is True");
		}
		
		a.setPredicate(p);
//		if (a.getPredicate() == p) {
//			System.out.println("[OMG7] a.getPredicate() == p");
//		}
//		if (a.getPredicate().equals(predicate) == true) {
//			System.out.println("[OMG8] a.getPredicate().equals(predicate) is True");
//		}
//		}
//		System.out.println("a: " + a);
		
//		if (a.getPredicate() == predicate) {
//			System.out.println("[OMG3] a.getPredicate() == predicate");
//		}
//		
		a.setTerms(new ArrayList<Term>());
		for (int i = 0; i < terms.size(); i++) {
			Term t = (Term)terms.get(i).clone();
			a.appendTerm(t);
		}
		
		return a;
    }

	public void setPredicate(Predicate pred) {
		this.predicate = pred;
	}

	public void appendTerm(Term t) {
		terms.add(t);
	}

	public ArrayList<Term> getTerms() {
		return terms;
	}
	
	public void setTerms(ArrayList<Term> t) {
		terms = t;
	}

	public boolean isInterpreted() {
		return predicate.isInterpreted();
	}

	public Predicate getPredicate() {
		return predicate;
	}
	
	public String getAtomStrWithGivenRelName(String relName) {
		return getAtomStrWithGivenRelName(relName, false);
	}

	public String getAtomStrWithGivenRelName(String relName, boolean convertVartoLiteral) {
		if (predicate.isInterpreted() == false) {
			return relName + "(" + getAtomBodyStr(convertVartoLiteral) + ")";	
		} else {
			return toString();
		}
	}
	
	public String getAtomBodyStr() {
		return getAtomBodyStr(false);
	}
	
	public String getAtomBodyStr(boolean convertVartoLiteral) {
		String str = "";
		if (predicate.isInterpreted() == false) {
			for (int i = 0; i < terms.size(); i++) {
				if (str.contentEquals("") == false) {
					str += ",";
				}
				if (convertVartoLiteral == false) {
					str += terms.get(i);
				} else {
					if (terms.get(i).isVariable() == true) {
						str += Util.addQuotes(terms.get(i).toString());
					} else {
						str += terms.get(i);
					}
				}
			}
		} else { // binaryOp
			str = terms.get(0) + predicate.getRelName() + terms.get(1);
		}
		return str;
	}
	
	public Atom getAtomForCanonicalDatabase(int index) {
		Atom a = new Atom(predicate.getRelName());
		a.getTerms().add(new Term("_n"+index, true));
		
		if (predicate.isInterpreted() == false) {
			for (int i = 0; i < terms.size(); i++) {
				Term t = terms.get(i);
//				System.out.println("[getAtomForCanonicalDatabase terms.get(i): " + terms.get(i));
				if (t.isVariable() == true) {
					a.getTerms().add(new Term(terms.get(i).getVar(), true));
				} else {
					a.getTerms().add(new Term(terms.get(i).toString(), false));
				}
//				a.getTerms().add(new Term(Util.addQuotes(terms.get(i).getVar()), false));
//				if (terms.get(i).isVariable() == true) {
//					if (isQuery == true) {
//						t.add(new StringSimpleTerm(terms.get(i).getVar()));
//					} else {
//						t.add(new StringSimpleTerm(terms.get(i).toString()));
//					}
//				} else {
//					t.add(new StringSimpleTerm(terms.get(i).toString()));
//				}
			}
		} else { // binaryOp
			throw new IllegalArgumentException("Only for non interpreted atom");
		}
		return a;
	}
	
	public ArrayList<SimpleTerm> getTupleForCanonicalDatabase(int index) {
		ArrayList<SimpleTerm> t = new ArrayList<SimpleTerm>();
		t.add(new LongSimpleTerm(index));			
		
		if (predicate.isInterpreted() == false) {
			for (int i = 0; i < terms.size(); i++) {
				if (terms.get(i).isVariable() == true) {
					t.add(new StringSimpleTerm(terms.get(i).toString()));	
				} else {
					t.add(new StringSimpleTerm(Util.removeQuotes(terms.get(i).toString())));
				}
				
//				t.add(new StringSimpleTerm(Util.addQuotes(terms.get(i).toString())));
//				if (terms.get(i).isVariable() == true) {
//					if (isQuery == true) {
//						t.add(new StringSimpleTerm(terms.get(i).getVar()));
//					} else {
//						t.add(new StringSimpleTerm(terms.get(i).toString()));
//					}
//				} else {
//					t.add(new StringSimpleTerm(terms.get(i).toString()));
//				}
			}
		} else { // binaryOp
			throw new IllegalArgumentException("Only for non interpreted atom");
		}
		return t;
	}
	

	public ArrayList<Atom> getAtomBodyStrWithInterpretedAtoms(String baseName) {
		ArrayList<Atom> atoms = new ArrayList<Atom>();
		
		String interpretedAtoms = "";
		String str = "";
		Atom a = new Atom(predicate.getRelName());
		atoms.add(a);
		if (predicate.isInterpreted() == false) {
			if (baseName.contentEquals("") == true) {
				a.getPredicate().setRelName(predicate.getRelName());
			} else {
				a.getPredicate().setRelName(predicate.getRelName() + "_" + baseName);
			}

			a.setNegated(isNegated);
			for (int i = 0; i < terms.size(); i++) {
				if (str.contentEquals("") == false) {
					str += ",";
				}
				if (terms.get(i).isConstant() == true) {
					String newVar = Helper.getNewVar();
					a.getTerms().add(new Term(newVar, true));
					Atom b = new Atom(Config.predOpEq);
					b.getTerms().add(new Term(newVar, true));
					b.getTerms().add(new Term(terms.get(i).toString(), false));
					atoms.add(b);
					if (interpreted == null) {
						interpreted = new HashMap<Integer, String>();
					}
					interpreted.put(i, newVar);
//					interpretedAtoms += ", " + newVar + " = " + terms.get(i).toString();
//					str += newVar;
				} else {
					a.getTerms().add(terms.get(i));
					str += terms.get(i);
				}
			}
		} else { // binaryOp
			a.setPredicate(predicate);
			a.getTerms().addAll(terms);
//			str = terms.get(0) + predicate.getRelName() + terms.get(1);
		}
		
//		atoms.add(str);
//		atoms.add(interpretedAtoms);
//		System.out.println("[getAtomBodyStrWithInterpretedAtoms] atoms: " + atoms);
		return atoms;
	}
	public HashMap<Integer, String> getInterpreted() {
		return interpreted;
	}

	public void setInterpreted(HashMap<Integer, String> interpreted) {
		this.interpreted = interpreted;
	}

	public String toString() {
		String str = getAtomBodyStr();
		if (predicate.isInterpreted() == false) {
			str = predicate.getRelName() + "(" + str + ")";
		}
		if (isNegated == true) {
			str = "!" + str; 
		}
		return str;
	}

	public boolean isNegated() {
		return isNegated;
	}

	public void setNegated(boolean isNegated) {
		this.isNegated = isNegated;
	}
	
	public HashSet<String> getVars() {
		HashSet<String> vars = new LinkedHashSet<String>();
		for (Term t : terms) {
			if (t.isVariable() == true) {
				vars.add(t.toString());
			}
		}
		return vars;
	}

	public HashSet<String> getNonConstantVars() {
		HashSet<String> vars = new HashSet<String>();
		for (Term t : terms) {
			if (t.isVariable() == true && t.getVar().subSequence(0, 1).equals("_") == false) {
				vars.add(t.toString());
			}
		}
		return vars;
	}
	
	public ArrayList<String> getNonConstantVarsList() {
		ArrayList<String> vars = new ArrayList<String>();
		for (Term t : terms) {
			if (t.isVariable() == true && t.getVar().subSequence(0, 1).equals("_") == false) {
				vars.add(t.toString());
			}
		}
		return vars;
	}

	public String getBaseName() {
		return baseName;
	}

	public void setBaseName(String baseName) {
		this.baseName = baseName;
	}
}
