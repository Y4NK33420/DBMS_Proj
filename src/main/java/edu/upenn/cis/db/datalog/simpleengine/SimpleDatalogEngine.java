package edu.upenn.cis.db.datalog.simpleengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.map.ConcurrentMutableMap;

import com.logicblox.connect.BloxCommand.Column;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogParser;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.helper.Util;

public class SimpleDatalogEngine<T extends Comparable<T>> {
	final static Logger logger = LogManager.getLogger(SimpleDatalogEngine.class);
	
	private static HashMap<ArrayList<Long>, Long> newIdMap;
	private static long newIdIndex = 100000000;
	private static int resultQueryIndex = 0;

	private static HashSet<String> tempRels;
	private static boolean isQuerying;
	
	private HashMap<String, Relation<T>> db;
//	private HashMap<Integer, HashSet<String>> tempRelsMap = new HashMap<Integer, HashSet<String>>();
	private int queryIndex = 1000;	

//	public static long geteNewId(ArrayList<Long> arr) {
//		if (newIdMap.containsKey(arr) == false) {
//			newIdMap.put(arr, newIdIndex++);
//		}
//		return newIdMap.get(arr); 
//	}
	
	public static long getNewId() {
		return newIdIndex++;
	}
	
	public void renameRelation(String rel1, String rel2) {
		db.put(rel2, db.get(rel1));
		db.remove(rel1);
	}
	
	
	public Relation<T> getRelation(String name) {
		return db.get(name);
	}
	
	public void removeRelation(String name) {
		if (db.containsKey(name) == true) {
			db.remove(name);
		}
	}
	
	public String getRelationList() {
		String str = "";
		for (String rel : db.keySet()) {
			str += rel + ":" + db.get(rel).getTuples().size() + ", ";
		}
		return str; 
	}
	
	public SimpleDatalogEngine() {
		db = new HashMap<String, Relation<T>>();
		newIdMap = new HashMap<ArrayList<Long>, Long>();
	}

	public void addRel(String name, Relation<T> rel) {
		if (db.containsKey(name) == true) {
//			throw new IllegalArgumentException("(name: " + name + ") Relation [" + rel +"] exists. db: " + getRelationList());
//			System.out.println("[ERR] (name: " + name + ") Relation [" + rel +"] exists. db: " + getRelationList());
		}
		db.put(name, rel);
	}
	
	public void insertTuple(String name, Tuple<T> t) {
//		System.out.println("[Engine] insertTuple name[" + name + "] tuple[" + t + "]");
		Relation<T> rel = null;
		if (db.containsKey(name) == false) {
			throw new IllegalArgumentException("db has no rel: " + name);
		}
		rel = db.get(name);
//		System.out.println("insertTuple name: " + name + " t: " + t);
		rel.addTuple(t); 
	}
	
	public String toString() {
		return "Engine: relations: " + db;
	}
	
	public void executeRules(List<DatalogClause> cs) {
		for (DatalogClause c : cs) {
			executeRule(c);
		}		
	}

	public void executeRule(DatalogClause c) {
//		System.out.println("[executeRule] c: " + c);	
	
		
		ArrayList<Atom> body = new ArrayList<Atom>();
		
		if (c.getHeads().size() == 0) {
			c.getHeads().add(c.getHead());
		}
		for (int i = 0; i < c.getBody().size(); i++) {
			Atom a = c.getBody().get(i);
//			if (a.getVars().contains("_") == true) {
				ArrayList<Atom> bs = a.getAtomBodyStrWithInterpretedAtoms("");
				body.addAll(bs);
//			} else {
//				body.add(a);
//			}
		}
		HashMap<String, HashSet<Atom>> interpretedAtomsMap = getInterpretedAtomsMap(body);
//		System.out.println("interpretedAtomsMap: " + interpretedAtomsMap);
		ArrayList<Atom> orderedAtoms = getOrderedAtoms(body);
		Relation<T> workingRel = executeOperator(orderedAtoms, interpretedAtomsMap);
		processHead(c.getHeads(), workingRel, interpretedAtomsMap);
		
//		System.out.println("wr: " + workingRel);
	}

	public Relation<T> executeQuery(List<DatalogClause> cs) {
		HashSet<String> relsToDel = new LinkedHashSet<String>();
		
		for (DatalogClause c : cs) {
			for (Atom h : c.getHeads()) {
				relsToDel.add(h.getRelName());
			}
		}
		executeRules(cs);
		Relation<T> rel = db.get("_");
		for (String rels : relsToDel) {
			db.remove(rels);
		}
		renameRelation("_", "_" + (resultQueryIndex++));
		
		return rel;
	}

	public Relation<T> executeQuery(DatalogClause c) {
		executeRule(c);
		Relation<T> rel = db.get("_");
		db.remove("_");
		
		return rel;
	}

//	public Relation<T> execute(DatalogClause c) {
//		int tid = Util.startTimer();
//		Relation<T> rel = new Relation<T>(null);
//		ArrayList<Atom> body = new ArrayList<Atom>();
//		
//		for (int i = 0; i < c.getBody().size(); i++) {
//			Atom a = c.getBody().get(i);
//			if (a.getVars().contains("_") == true) {
//				ArrayList<Atom> bs = a.getAtomBodyStrWithInterpretedAtoms("");
//				body.addAll(bs);
//			} else {
//				body.add(a);
//			}
//		}
//		HashMap<String, HashSet<Atom>> interpretedAtomsMap = getInterpretedAtomsMap(body);
//		ArrayList<Atom> orderedAtoms = getOrderedAtoms(body);
//		Relation<T> workingRel = executeOperator(orderedAtoms, interpretedAtomsMap);
//		
////		System.out.println("1.heads: " + c.getHeads());
////		System.out.println("2.orderedAtoms: " + orderedAtoms);
////		System.out.println("3.workingRel size: " + workingRel.getTuples().size());
//		processHead(c.getHeads(), workingRel);
//		
//		String relName = c.getHeads().get(0).getRelName();
//		return db.get(relName);
//	}
	
	/*
	 UDF
	 	name: IDGEN_3(1,n1,n2,s)
	 	arguments: 3 inputs and 1 output
	 	when evaluated? all inputs except the output are bound (in workingRel)
	 	add s to workingRel
	 	
	 Negation
	 	name: R(a,b,c), ~S(a,b)
	 	when evaluated? all attributes are bound
	 	tuple is inserted if (a,b,c) is not in R
	 */
	
	private HashMap<String, HashSet<Atom>> getInterpretedAtomsMap(ArrayList<Atom> body) {
		HashMap<String, HashSet<Atom>> map = new HashMap<String, HashSet<Atom>>();
		
		Set<Atom> interpretedAtoms = new LinkedHashSet<Atom>();
		int newVarId = 0;		
		for (int i = 0; i < body.size(); i++) {
			Atom a = body.get(i);
//			System.out.println("[getInterpretedAtomsMap] a: " + a);
			if (a.isInterpreted() == false) {
				Atom b = null;
				for (int j = 0 ; j < a.getTerms().size(); j++) {
//					System.out.println("[getInterpretedAtomsMap] t: " + a.getTerms().get(j));
					if (a.getPredicate().getRelName().startsWith(Config.relname_gennewid + "_") == true) continue;
					if (a.getTerms().get(j).isConstant() == true) {
//						System.out.println("[getInterpretedAtomsMap] constant t: " + a.getTerms().get(j));
						
						String newVar = "_" + newVarId++;
						Term t1 = new Term(newVar, true);						
						Atom newAtom = new Atom(Config.predOpEq);
						ArrayList<Term> terms = new ArrayList<Term>();
						terms.add(t1);
						String var = a.getTerms().get(j).toString(); 
						terms.add(new Term(var, true));
						newAtom.setTerms(terms);
						if (b == null) {
							try {
								b = (Atom)a.clone();
							} catch (CloneNotSupportedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						b.getTerms().set(j, t1);
						interpretedAtoms.add(newAtom);
						
						map.put(newVar, new LinkedHashSet<Atom>());
						map.get(newVar).add(newAtom);
					}
				}
				if (b != null) {
					body.set(i, b);
				}
			} else if (a.isInterpreted() == true) {
				interpretedAtoms.add(a);
				
				for (Term t : a.getTerms() ) {
					if (t.isVariable() == true) {
						String var = t.getVar();
						if (map.containsKey(var) == false) {
							map.put(var, new LinkedHashSet<Atom>());
						}
						map.get(var).add(a);
					}
				}				
			}
		}
		
//		System.out.println("[getInterpretedAtomsMap] map: " + map);
		
		return map;
	}

	private ArrayList<Atom> getOrderedAtoms(ArrayList<Atom> body) {
		// TODO Auto-generated method stub
		HashSet<Atom> udfAtoms = new LinkedHashSet<Atom>();
		HashSet<Atom> negatedAtoms = new LinkedHashSet<Atom>();
		ArrayList<Atom> orderedAtoms = new ArrayList<Atom>();
		
		for (Atom a : body) {
			if (a.isInterpreted() == true) continue;
			if (a.getPredicate().getRelName().contains(Config.relname_gennewid + "_") == true) {
				udfAtoms.add(a);
			} else if (a.isNegated() == true) {
				negatedAtoms.add(a); 
			} else {
				orderedAtoms.add(a);
			}
		}
		orderedAtoms.addAll(udfAtoms);
		orderedAtoms.addAll(negatedAtoms);
		
//		System.out.println("[getOrderedAtoms] orderedAtoms: " + orderedAtoms);
		return orderedAtoms;
	}
	
	private Set<Atom> getRelatedInterpretedAtoms(HashMap<String, HashSet<Atom>> map, Atom a) {
		HashSet<Atom> interpretedAtoms = new LinkedHashSet<Atom>();
		
		for (Term t : a.getTerms()) {
			if (t.isVariable() == true) {
				if (map.containsKey(t.getVar()) == true) {
					interpretedAtoms.addAll(map.get(t.getVar()));
				}
			}
		}
		return interpretedAtoms;
	}

	private Relation<T> executeOperator(ArrayList<Atom> orderedAtoms,
			HashMap<String, HashSet<Atom>> interpretedAtomsMap) {

		Relation<T> workingRel = null; // currentWorking Rel
		Set<Atom> interpretedAtoms = null;
//		interpretedAtomsMap
		for (Atom a : orderedAtoms) {
//			System.out.println("[executeOperator] a: " + a);
			if (a.isInterpreted() == false) {
				String relName = a.getPredicate().getRelName();
				if (workingRel == null) {
					if (a.isNegated() == true) {
						throw new IllegalArgumentException("Body contains only single negated atom. a: " + a);
					}
					ArrayList<String> cols = new ArrayList<String>();
					for (Term t : a.getTerms()) {
						cols.add(t.getVar().toString());
					}
					if (db.containsKey(relName) == false) {
						throw new IllegalArgumentException("DB doesn't have rel: " + relName + " a: "+ a + " rels: " + getRelationList()) ;	
					}
					workingRel = new Relation<T>(db.get(relName), cols);
					interpretedAtoms = getRelatedInterpretedAtoms(interpretedAtomsMap, a);

//					System.out.println("132interpretedAtoms: " + interpretedAtoms + " wr: " + workingRel);

				} else { // join
					int tid2 = Util.startTimer();
					ArrayList<String> cols = new ArrayList<String>();
					
					for (Term t : a.getTerms()) {
						cols.add(t.getVar().toString());
					}

//					if (relName.startsWith(Config.relname_gennewid + "_MAP_" ) == true) {
//						//workingRel = workingRel.join(rRel, interpretedAtoms);
//						workingRel = workingRel.addNewId(a);
//					} else {
						if (db.get(relName) == null) {
							throw new IllegalArgumentException("relName: " + relName + " doesn't exist in db. re");
						}

						if (a.isNegated() == true) { // negation
							workingRel = workingRel.notin(db.get(relName), a);
						} else { // join
							interpretedAtoms.addAll(getRelatedInterpretedAtoms(interpretedAtomsMap, a));
	
							Relation<T> rRel = new Relation<T>(db.get(relName), cols);
							int workingRelSize = workingRel.getTuples().size();
//							System.out.println("workingSize: "+ workingRelSize + " rRel: " + rRel.getTuples().size());
							
//							System.out.println("132interpretedAtomsMap: " + interpretedAtomsMap);
//							System.out.println("132interpretedAtoms: " + interpretedAtoms);
							
							workingRel = workingRel.join(rRel, interpretedAtoms);
						}
//					}
				}
			}
		}
//		System.out.println("[executeOperator] before filter workingRel.size(): " + workingRel.getTuples().size() + " interpretedAtoms: " + interpretedAtoms);
		workingRel = workingRel.filter(interpretedAtoms);
//		System.out.println("[executeOperator] after filter workingRel.size(): " + workingRel.getTuples().size());

		return workingRel;
	}

	private void processHead(ArrayList<Atom> heads, Relation<T> workingRel, HashMap<String, HashSet<Atom>> interpretedAtomsMap) {
//		System.out.println("[processHead] heads: " +  heads + " workingRel: " + workingRel.getColumns());
		for (Atom head : heads) {
			HashMap<Integer, Integer> varToVar = new HashMap<Integer, Integer>();
			String name = head.getPredicate().getRelName();
		
			ArrayList<String> vars = workingRel.getColumns();
			
//			System.out.println("[processHead] vars: " + vars);
			int size = head.getTerms().size();
			for (int i = 0; i < size; i++) {
				Term t = head.getTerms().get(i);				
				String n = t.getVar();
				
				if (t.isConstant() == true) {
					continue;
				}
				int index = vars.indexOf(n);
				if (index >= 0) {
					varToVar.put(i, index);
				} else if (n.contentEquals("_") == false) {
					if (name.startsWith(Config.relname_gennewid + "_") == true && i == size - 1) continue;
					
//					throw new IllegalArgumentException("variable [" + n + "] does not exist in the body. vars: " + vars + " head: " + head);
				}
			}
//			System.out.println("varToVar: " + varToVar);
			
			if (name.startsWith(Config.relname_gennewid + "_CONST_") == true) {
				String gennewid_map = name.replace("_CONST_", "_MAP_");
				if (db.containsKey(gennewid_map) == false) {
					Relation<T> rel = new Relation<T>();
					db.put(gennewid_map, rel);
				}
//				System.out.println("dbbbb: "+ getRelationList());
				Relation<T> mapRel = db.get(gennewid_map);
				for (Tuple<T> tuple1 : workingRel.getTuples()) {
					boolean found = false;
					for (Tuple<T> tuple2 : mapRel.getTuples()) {
						found = true;
						int size2 = head.getTerms().size();
						for (int i = 0; i < size2-1; i++) {
							Term t = head.getTerms().get(i);
							int bodyIdx = varToVar.get(i);
							T st1 = tuple1.getTuple().get(bodyIdx);
							T st2 = tuple2.getTuple().get(i);
//							System.out.println("st1: " + st1 + " st2: " + st2);
							if (st1.equals(st2) == false) {
								found = false;
								break;
							}
						}
						if (found == true) {
							break;
						}
					}

					if (found == false) {
//						System.out.println("***NOT FOUND***");

						Tuple<T> newTuple = new Tuple<T>();
						int size2 = head.getTerms().size();
						for (int i = 0; i < size2-1; i++) {
							Term t = head.getTerms().get(i);
							int bodyIdx = varToVar.get(i);
							T st1 = tuple1.getTuple().get(bodyIdx);
							newTuple.getTuple().add(st1);
						}
						newTuple.getTuple().add((T) new LongSimpleTerm(getNewId()));
						mapRel.getTuples().add(newTuple);
//						System.out.println("gennewid_map: " + gennewid_map + " mapRel: " + mapRel);
						// TODO: insert with new gen id
//						System.out.println("dbbbb: "+ getRelationList());

//						break;
					}
				}
				continue;
			}
			if (name.startsWith(Config.relname_gennewid + "_") == true) continue;
			
//			System.out.println("name: " + name + " db.containsKey(name): " + db.containsKey(name));
			if (db.containsKey(name) == false) {
				Relation<T> rel2 = new Relation<T>();
				db.put(name, rel2);
			}
			if (db.get(name).getColumns().size() == 0) {
				for (Term t : head.getTerms()) {
					String v = t.getVar();
					db.get(name).getColumns().add(v);
				}
			}
			

			for (Tuple<T> tuple : workingRel.getTuples()) {
				if (tuple == null) {
					throw new IllegalArgumentException("head [" + head + "] has tuple==null.");
				}
//				System.out.println("tuple from workingRel: " + tuple + "head: " + head + " varToVar: " + varToVar);
				Tuple<T> newTuple = new Tuple<T>();
				for (int j = 0; j < head.getTerms().size(); j++) {
					String n = head.getTerms().get(j).toString();
					Term term = head.getTerms().get(j);
			
					T t = null;
					if (term.isVariable() == false) {
						t = (T)term.getSimpleTerm();
					} else if (term.getVar().contentEquals("_") == true) {
						t = (T)new LongSimpleTerm(-999);
						throw new IllegalArgumentException("head [" + head + "] has _");
					} else {
						if (varToVar.containsKey(j) == true) {
							t = tuple.getTuple().get(varToVar.get(j));
						} else if (interpretedAtomsMap.containsKey(n) == true) {
//							System.out.println("varToVar: " + varToVar + " j: " + j + " head: " + head);
//							System.out.println("interpretedAtomsMap: " + interpretedAtomsMap);
//							System.out.println("varToVar.get(j): " + varToVar.get(j));
//							throw new IllegalArgumentException("variable1 [" + varToVar.get(j) + "] does not exist in the body. head: " + head);
							
							for (Atom a : interpretedAtomsMap.get(n)) {
								if (a.isInterpreted() == true && a.getRelName().equals("=") == true && a.getTerms().get(1).isConstant() == true) {
									String val = a.getTerms().get(1).toString();
									if (val.contains("\"") == true) {
										t = (T)new StringSimpleTerm(Util.removeQuotes(val));
									} else {
										t = (T)new LongSimpleTerm(Long.parseLong(val));
									}
								}
							}
						} else {
							throw new IllegalArgumentException("variable [" + varToVar.get(j) + "] does not exist in the body. head: " + head);
							//vars: " + vars + " head: " + head);
						}
					}
					newTuple.getTuple().add(t);
				}
				insertTuple(name, newTuple);
			}
		}		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int tid = Util.startTimer();
		
		Config.initialize();
		SimpleDatalogEngine engine = new SimpleDatalogEngine();
		// Create Linked List 
		Relation relN = new Relation(
				new ArrayList<String>(Arrays.asList("nid","label")));
//		System.out.println("4DONE (Time: " + Util.getElapsedTime(tid) + " msec)");
		relN.addTuple(new Tuple<SimpleTerm>(new ArrayList<SimpleTerm>(Arrays.asList(new IntegerSimpleTerm(1),new IntegerSimpleTerm(1000)))));
		relN.addTuple(new Tuple<SimpleTerm>(new ArrayList<SimpleTerm>(Arrays.asList(new IntegerSimpleTerm(2),new IntegerSimpleTerm(1001)))));
		engine.addRel("N",  relN);
		
//		Relation relE = new Relation(
//				new ArrayList<String>(new ArrayList<String>(Arrays.asList("nid","from","to","label"))));		
//		relE.addTuple(new Tuple<Integer>(new ArrayList<Integer>(Arrays.asList(100,1,2,2000))));
//		relE.addTuple(new Tuple<Integer>(new ArrayList<Integer>(Arrays.asList(101,2,3,2001))));
//		relE.addTuple(new Tuple<Integer>(new ArrayList<Integer>(Arrays.asList(102,1,3,2002))));
////		System.out.println("2DONE (Time: " + Util.getElapsedTime(tid) + " msec)");
//		engine.addRel("E",  relE);
//		
//		String query = "_(n1,n2,l1,l2) <- N(n1,l1), N(n2,l2), E(e1,n1,n2,l3), n1=2.";
//		DatalogProgram program = new DatalogProgram();
//		DatalogParser parser = new DatalogParser(program);
//		DatalogClause c = parser.ParseQuery(query);
//		System.out.println("2332DONE (Time: " + Util.getElapsedTime(tid) + " msec)");
//	
//		System.out.println("c: " + c);
//		Relation<Integer> result = engine.execute(c);
////		String name = c.getHead().getPredicate().getRelName();
////		engine.addRel(name, result); // materialize
//		
//		System.out.println("==RESULT==");
//		System.out.println(result.getColumns());
//		for (Tuple t : result.getTuples()) {
//			System.out.println(t);
//		}
//		
//		System.out.println("==Input to new relation==");	
//		query = "COLOR(n1),COLOR(l1) <- N(n1,l1), n1 > 1, n1!=l1.";
//		c = parser.ParseQuery(query);
//		engine.execute(c);
//		
//		System.out.println("==IDB rule==");
//		engine.insertTuple("COLOR", new Tuple<Integer>(
//				new ArrayList<Integer>(Arrays.asList(101,2,3,2001))));
//		engine.insertTuple("COLOR", new Tuple<Integer>(
//				new ArrayList<Integer>(Arrays.asList(141,2,3,4401))));
		
		Relation<SimpleTerm> relT = new Relation<SimpleTerm>(
				new ArrayList<String>(new ArrayList<String>(Arrays.asList("x","y"))));
		Relation<SimpleTerm> relS = new Relation<SimpleTerm>(
				new ArrayList<String>(new ArrayList<String>(Arrays.asList("u","v"))));
		relT.addTuple(new Tuple<SimpleTerm>(Arrays.asList(
				(SimpleTerm)new IntegerSimpleTerm(14), 
				(SimpleTerm)new IntegerSimpleTerm(10))));
		relS.addTuple(new Tuple<SimpleTerm>(Arrays.asList(
				(SimpleTerm)new IntegerSimpleTerm(7), 
				(SimpleTerm)new IntegerSimpleTerm(10))));
//		relS.addTuple(new Tuple<SimpleTerm>(Arrays.asList(
//				(SimpleTerm)new IntegerSimpleTerm(12), 
//				(SimpleTerm)new StringSimpleTerm("A"))));
		engine.addRel("T", relT);
		engine.addRel("S", relS);
		
		System.out.println("==Input to new relation==");
		DatalogProgram program = new DatalogProgram();
		DatalogParser parser = new DatalogParser(program);

		String query = "V(x1,y1,v1) <- T(x1,y1), S(v1,v1), y1=v1."; //, n1 > 1, n1!=l1.";
		DatalogClause c = parser.ParseQuery(query);
		engine.executeRule(c);

		
//		String query = "COLOR(n1),COLOR(l1) <- T(n1,l1)."; //, n1 > 1, n1!=l1.";
//		DatalogClause c = parser.ParseQuery(query);
//		engine.executeRule(c);
		
//		query = "DUP(a) <- COLOR(a)."; //, n1 > 1, n1!=l1.";
//		c = parser.ParseQuery(query);
//		engine.executeRule(c);
//
//		query = "DUP2(a) <- T(a,b), b=\"B\"."; //, n1 > 1, n1!=l1.";
//		c = parser.ParseQuery(query);
//		engine.executeRule(c);
//
//		query = "DUP10(a,\"GOO\",s) <- !DUP2(a), T(a,b), NEWID_1(1,a,s)."; //, n1 > 1, n1!=l1.";
//		c = parser.ParseQuery(query);
//		engine.executeRule(c);
//		
//		
//		query = "DUP4(a), DUP3(a, \"ago\") <- T(a,\"B\")."; //, n1 > 1, n1!=l1.";
//		c = parser.ParseQuery(query);
//		engine.executeRule(c);
//
//		query = "DUP3(a, \"ago2\") <- T(a,\"A\")."; //, n1 > 1, n1!=l1.";
//		c = parser.ParseQuery(query);
//		engine.executeRule(c);
//
//		System.out.println("===DB===");
//		System.out.println(engine);
//
//		System.out.println("DUP2==>");
//		Relation<SimpleTerm> rel = engine.getRelation("DUP2");
//		for (Tuple<SimpleTerm> t : rel.getTuples()) {
//			System.out.println("t: " + t);
//		}

		Relation<SimpleTerm> rel;
		System.out.println("V==>");
		rel = engine.getRelation("V");
		for (Tuple<SimpleTerm> t : rel.getTuples()) {
			System.out.println("t: " + t);
		}
		
		System.out.println(engine);
		System.out.println("DONE (Time: " + Util.getElapsedTime(tid) + " msec)");
	}


}
