package edu.upenn.cis.db.datalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import com.logicblox.connect.BloxCommand.Column;
import com.logicblox.connect.BloxCommand.Relation;
import com.logicblox.connect.ConnectBlox.Response;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.ConjunctiveQuery.Type;
import edu.upenn.cis.db.datalog.simpleengine.LongSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.StringSimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Tuple;
import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.helper.Util;
import edu.upenn.cis.db.logicblox.LogicBlox;

public class QueryRewriterSubstitution {
	private static ArrayList<Boolean> rewrittenAtoms;
//	private static ArrayList<DatalogClause> datalogRules;
	
	private static String workspace = "_QUERY_REWRITER";
	
	private static DatalogProgram p = new DatalogProgram();
	private static DatalogParser parser = new DatalogParser(p);
	
	private static Store store = null;
	
	private static void createCanonicalDatabase(DatalogClause q) {
		ArrayList<Atom> body = q.getBody();
		
		Predicate p1 = new Predicate(Config.relname_node + "_" + CommandExecutor.getFrom());
		p1.setArgNames("n", "id", "label");
		p1.setTypes(Type.Integer, Type.Integer, Type.String);
		store.createSchema(workspace, p1);

		p1 = new Predicate(Config.relname_edge + "_" + CommandExecutor.getFrom());
		p1.setArgNames("n", "id", "from", "to", "label");
		p1.setTypes(Type.Integer, Type.Integer, Type.Integer, Type.Integer, Type.String);
		store.createSchema(workspace, p1);
		
		for (int i = 0; i < body.size(); i++) {
			Atom a = body.get(i);
			if (a.isInterpreted() == false && a.getRelName().startsWith(Config.relname_nodeprop + "_") == false
					&& a.getRelName().startsWith(Config.relname_edgeprop + "_") == false) {
				store.addTuple(a.getRelName(), a.getTupleForCanonicalDatabase(i));
			}
			rewrittenAtoms.add(false);
		}
	}
	
	private static StoreResultSet queryCanonicalDatabase(DatalogClause q) {
//		System.out.println("[queryCanonicalDatabase] q: " + q);
		DatalogClause c = new DatalogClause();
		
		ArrayList<Atom> body = q.getBody();
		HashSet<String> headVars = new LinkedHashSet<String>();
		HashSet<String> bodyVars = new LinkedHashSet<String>();
		
		for (int i = 0; i < body.size(); i++) {
			bodyVars.addAll(body.get(i).getVars());
		}	
		
		for (int i = 0; i < body.size(); i++) {
			Atom a = body.get(i);
			if (a.isInterpreted() == false) {
				c.getBody().add(a.getAtomForCanonicalDatabase(i));
				headVars.add("_n" + i);
				bodyVars.add("_n" + i);
			}
		}
		
//		System.out.println("[QueryRewriter] headVars: " + headVars);
//		System.out.println("[QueryRewriter] bodyVars: " + bodyVars);
		
		String headAtoms = "";
		Atom h = new Atom("_");
		
		for (String v : headVars) {
			h.appendTerm(new Term(v, true));
		}
		for (Term t : q.getHead().getTerms()) {
			String v = t.getVar();
			
			if (headAtoms.contentEquals("") == false) {
				headAtoms += ",";
			}
			if (bodyVars.contains(v) == true) {
				h.appendTerm(new Term(v, true));
			} else {
				h.appendTerm(new Term("_", true));
			}
		}
//		System.out.println("[queryCanonicalDatabase] h: " + h + " q.head: " + q.getHead());
		c.setHead(h);
		
//		System.out.println("cccq: " + q);
//		System.out.println("ccc: " + c);
		
//		store.debug();
		
		return store.getQueryResult(c);
	}
	
	/**
	 * @param q1
	 * @param q2
	 */
	public static DatalogClause rewrite(DatalogClause q1, ArrayList<DatalogClause> rules) {
		store = GraphTransServer.getBaseStore();
		store.createDatabase(workspace);
		store.useDatabase(workspace);
		
		DatalogClause rewriting = new DatalogClause();
		rewrittenAtoms = new ArrayList<Boolean>();
		
//		System.out.println("12340 code => q1: " + q1);
		createCanonicalDatabase(q1);
		
//		System.out.println("[QueryRewriterSubstitution] q1: " + q1);
		
		HashMap<String, String> varsToCoveredIndex = new HashMap<String, String>();
		for (int i = 0; i < rules.size(); i++) {
			int numOfAtoms = 0;
			for (int j = 0; j < rules.get(i).getBody().size(); j++) {
				if (rules.get(i).getBody().get(j).isInterpreted() == false) {
					numOfAtoms++;
				}
			}
			StoreResultSet rs = queryCanonicalDatabase(rules.get(i));
			
//			System.out.println("[QueryRewriterSubstitution] rules[" + i + "]: "+ rules.get(i));
//			System.out.println("[QueryRewriterSubstitution] rs: "+ rs);
			
			List<String> columns = rs.getColumns();
			int cols = columns.size();
			int rows = (cols == 0) ? 0 : rs.getResultSet().size();
			
			for (int j = 0; j < rows; j++) {	
				boolean touchedNewNodeOrEdge = false;

				Tuple<SimpleTerm> st = rs.getResultSet().get(j);
				Atom a = new Atom(rules.get(i).getHead().getRelName());
//				System.out.println("[167] a: " + a + " cols: " + cols);
				for (int k = 0; k < cols; k++) {
					SimpleTerm t = st.getTuple().get(k);
//					System.out.println("[170] t: " + t);
					if (columns.get(k).contentEquals("_") == true) {
						a.appendTerm(new Term("_", true));
					} else if ((t instanceof StringSimpleTerm) == true) {
						if (k >= numOfAtoms) {
							String var = t.getString();
							var = Util.removeQuotes(var);
							if (var.startsWith("\"") == true) {
								a.appendTerm(new Term(var, false));
							} else {
								a.appendTerm(new Term(var, true));
							}
//							System.out.println("[179] a: " + a + " numOfAtoms: " + numOfAtoms);
//							String var = t.getString();
//							indexHeadVars += var;
							varsToCoveredIndex.put(var, rules.get(i).getHead().getRelName());
						}
					} else if ((t instanceof LongSimpleTerm) == true) {
						if (k < numOfAtoms) {
							int touchedIndex = (int)t.getLong();
							//System.out.println("touched atom index: " + rel.getColumn(k).getInt64Column().getValues(j));
							if (rewrittenAtoms.get(touchedIndex) == false) {
								rewrittenAtoms.set(touchedIndex, true);
								touchedNewNodeOrEdge = true;
							}
						}
					} else {
						throw new UnsupportedOperationException("Not supported types (only String, Long are supported).");
					}
				}
				
				if (touchedNewNodeOrEdge == true) {
					a.getPredicate().setRelName(rules.get(i).getHead().getPredicate().getRelName());
					rewriting.addAtomToBody(a);	
//					System.out.println("[QueryRewriterSubstitution] addAtomToBody a: " + a);
				}
			}
		}
		
//		System.out.println("varsToCoveredIndex: " + varsToCoveredIndex);
		for (int i = 0; i < rewrittenAtoms.size(); i++) {
			if (rewrittenAtoms.get(i) == false) {
				Atom a = q1.getBody().get(i);
				String rel = a.getPredicate().getRelName();
				
//				System.out.println("rel: " +rel);
				String var = a.getTerms().get(0).getVar();
				Atom b = null;
				try {
					b = (Atom)a.clone();
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (rel.startsWith(Config.relname_nodeprop + "_") == true) {
					String newRel = varsToCoveredIndex.get(var) + "_NP";
					b.getPredicate().setRelName(newRel);
//					rewritingStr += a.getAtomStrWithGivenRelName(newRel);
				} else if (rel.startsWith(Config.relname_edgeprop + "_") == true) {
					String newRel = varsToCoveredIndex.get(var) + "_EP";
					b.getPredicate().setRelName(newRel);
//					rewritingStr += a.getAtomStrWithGivenRelName(newRel);
				}
//				System.out.println("[QueryRewriterSubstitution] addAtomToBody b: " + b);

				rewriting.addAtomToBody(b);			}
		}
		
		rewriting.setHead(q1.getHead());
		
		return rewriting;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Config.initialize();
		
		workspace = "TEST1";
//		
		LogicBlox.connect("127.0.0.1", 5518, 5519);
		
		DatalogProgram p = new DatalogProgram();
		DatalogParser parser = new DatalogParser(p);
//		DatalogClause q1 = parser.ParseQuery("Q(a,b,c) <- E(e1,a1,b,\"X\"),E(a2,a2,b,\"X\"),E(e3,b,c,\"Y\"), c>10.");
//		DatalogClause q2 = parser.ParseQuery("I_v0_1(x,y,z) <- E(e10,x,y,\"X\"),E(e11,y,z,\"Y\").");

		DatalogClause q1 = parser.ParseQuery("Q(a,b,c,d) <- E(e1,a,b,\"X\"),E(a2,b,c,\"X\"),E(e3,c,d,\"X\"), c>10.");
		DatalogClause q2 = parser.ParseQuery("I_v0_1(x,y,z) <- E(e10,x,y,\"X\"),E(e11,y,z,\"X\").");
		ArrayList<DatalogClause> rules = new ArrayList<DatalogClause>();
		rules.add(q2);
		QueryRewriterSubstitution.rewrite(q1, rules);
		
//		LogicBlox.disconnect();
	}

}
