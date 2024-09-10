package edu.upenn.cis.db.graphtrans.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.helper.Util;

public class ParserHelper {

	public static void processWhereClause(HashMap<String, ?> nodeVarToAtomMap, 
			ArrayList<Atom> atoms, HashMap<String, HashMap<String, ArrayList<Atom>>> propertyAtoms,
			HashSet<String> getVarsInWhereClause) {

		for (String var : propertyAtoms.keySet()) {
//			System.out.println("[ParserHelper-processWhereClause] propertyAtoms: " + propertyAtoms);
			for (String key : propertyAtoms.get(var).keySet()) {
				Atom a = null;
				if (nodeVarToAtomMap.containsKey(var) == true) {
					a = new Atom(Config.predNP);
				} else {
					a = new Atom(Config.predEP);
				}
				a.appendTerm(new Term(var, true));
				a.appendTerm(new Term(Util.addQuotes(key), false));
				String value_var = var + "_" + key + "_val";
				a.appendTerm(new Term(value_var, true));
				atoms.add(a);
				if (getVarsInWhereClause != null) {
					getVarsInWhereClause.add(value_var);
				}
				for (Atom b : propertyAtoms.get(var).get(key)) {
					b.getTerms().get(0).setVar(value_var);
					atoms.add(b);
				}
			}
		}		
	}
	
	public static void processWhereCondition(String var, String prop, String op, String val, boolean ropIsVar,
			ArrayList<Atom> atoms, HashMap<String, HashMap<String, ArrayList<Atom>>> propertyAtoms,
			HashSet<String> getVarsInWhereClause, ArrayList<String> whereConditionForNeo4j) {

//		System.out.println("[ParserHelper-processWhereCondition] var: " + var + " prop: " + prop + " op: " + op + " val: " + val);
		Atom a = null;
		if (prop.contentEquals("") == true) { // not a property
			if (op.equals("=") == true) {
				 a = new Atom(Config.predOpEq);
			} else if (op.equals(">") == true) {
				 a = new Atom(Config.predOpGt);
			} else if (op.equals("<") == true) {
				 a = new Atom(Config.predOpLt);
			} else if (op.equals(">=") == true) {
				 a = new Atom(Config.predOpGe);
			} else if (op.equals("<=") == true) {
				 a = new Atom(Config.predOpLe);
			} else if (op.equals("!=") == true) {
				 a = new Atom(Config.predOpNeq);
			} else {
				throw new IllegalArgumentException("In the where clause, " + op + " is supported yet.");	
			}	
			a.appendTerm(new Term(var, true));
			a.appendTerm(new Term(val, ropIsVar));
//			System.out.println("Added: " + a);
	//		returnInterpretedSet.add(a);
			atoms.add(a);
			if (whereConditionForNeo4j != null) {
				if (op.endsWith("!=") == true) {
					op = "<>";
				}
				whereConditionForNeo4j.add(var + ".uid " + op + " " + val);
			}
		} else { // with Property
			if (op.equals("=") == true) {
				 a = new Atom(Config.predOpEq);
			} else if (op.equals(">") == true) {
				 a = new Atom(Config.predOpGt);
			} else if (op.equals("<") == true) {
				 a = new Atom(Config.predOpLt);
			} else if (op.equals(">=") == true) {
				 a = new Atom(Config.predOpGe);
			} else if (op.equals("<=") == true) {
				 a = new Atom(Config.predOpLe);
			} else if (op.equals("!=") == true) {
				 a = new Atom(Config.predOpNeq);
			} else {
				throw new IllegalArgumentException("In the where clause, " + op + " is supported yet.");	
			}	
			a.appendTerm(new Term(prop, true));
			a.appendTerm(new Term(val, false));
			if (propertyAtoms.containsKey(var) == false) {
				propertyAtoms.put(var, new HashMap<String, ArrayList<Atom>>());
			}
			if (propertyAtoms.get(var).containsKey(prop) == false) {
				propertyAtoms.get(var).put(prop, new ArrayList<Atom>());
			}
			ArrayList<Atom> atoms1 = propertyAtoms.get(var).get(prop);
			atoms1.add(a);
			if (whereConditionForNeo4j != null) {
				if (op.equals("!=") == true) {
					op = "<>";
				}
				whereConditionForNeo4j.add(var + "." + prop + " " + op + " " + val);
			}

		}
		if (getVarsInWhereClause != null) {
			getVarsInWhereClause.add(var);
		}
		
//		System.out.println("[QueryParser-visitWhere_condition] propertyAtoms: " + propertyAtoms);				
	}
}
