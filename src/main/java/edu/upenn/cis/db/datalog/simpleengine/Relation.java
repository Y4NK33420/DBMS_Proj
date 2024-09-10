package edu.upenn.cis.db.datalog.simpleengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.helper.Util;

public class Relation<T extends Comparable<T>> implements Iterable<Tuple<T>> {
	ArrayList<String> columns;
	HashSet<Tuple<T>> tuples;
		
	public Relation() {
//		this.columns = new ArrayList<String>();
		tuples = new HashSet<Tuple<T>>();
		columns = new ArrayList<String>();		
	}

//	public Relation(ArrayList<Tuple> tuples) {
//		this.tuples = tuples;
//	}
//
	public Relation(ArrayList<String> columns) {
		this.columns = columns;
		tuples = new HashSet<Tuple<T>>();
	}

	public Relation(HashSet<Tuple<T>> tuples, ArrayList<String> columns) {
		this.tuples = tuples;
		this.columns = columns;
	}

	public Relation(Relation<T> rel, ArrayList<String> columns) {
		if (rel == null) {
			throw new IllegalArgumentException("rel is null columns: " + columns);
		}
		this.tuples = rel.getTuples();
		this.columns = columns;
	}

	public void addTuple(Tuple<T> tuple) {
		this.tuples.add(tuple);
	}

	public ArrayList<String> getColumns() {
		return columns;
	}
	
	public HashSet<Tuple<T>> getTuples() {
		return tuples;
	}
	
	@Override
	public Iterator<Tuple<T>> iterator() {
		// TODO Auto-generated method stub
        return new RelationIterator<>(this); 
	}
	
	private int count = 0;
	private long elapsed = 0;
	
	public boolean isSelected(Tuple<T> leftTuple, Tuple<T> rightTuple, 
			ArrayList<Triple<Integer, Pair<Integer, Integer>, T>> pred) {
		// op, (index1, index2=-1), or value)
		int leftTupleSize = leftTuple.getTuple().size();
		int rightTupleSize = (rightTuple == null) ? 0 : rightTuple.getTuple().size();
		
//		int tid = Util.startTimer();
		
//		System.out.println("[isSelected] pred: " + pred);
		
		boolean isSelected = true;
		for (int i = 0; i < pred.size(); i++) {
			int op = pred.get(i).getLeft();
			int lhv = pred.get(i).getMiddle().getLeft();
			int rhv = pred.get(i).getMiddle().getRight();
			T value = pred.get(i).getRight();
			T lvalue = null;
			T rvalue = null;
			if (lhv < leftTupleSize) {
				lvalue = leftTuple.getTuple().get(lhv); 
			} else {
				lvalue = rightTuple.getTuple().get(lhv - leftTupleSize);
			}
			if (rhv >= 0) {
				if (rhv < leftTupleSize) {
					rvalue = leftTuple.getTuple().get(rhv);
				} else {
					rvalue = rightTuple.getTuple().get(rhv - leftTupleSize);
				}
			} else {
				rvalue = value;
			}
//			System.out.println("newTuple: " + newTuple);
//			System.out.println("lvalue: " + lvalue + "[lhv: " + lhv + "] rvalue: " + rvalue + "[rhv: " + rhv + "]");
			
//			if (lvalue.getClass().equals(rvalue.getClass()) == false) {
//				return false;
//			}
			if (op == 1) { // eq
				if (lvalue.compareTo(rvalue) != 0) {
					isSelected = false;
					break;
				}
			} else if (op == 2) { // <
				if (lvalue.compareTo(rvalue) >= 0) {
					isSelected = false;
					break;
				}
			} else if (op == 3) { // >
				if (lvalue.compareTo(rvalue) <= 0) {
					isSelected = false;
					break;
				}
			} else if (op == 4) { // !=
				if (lvalue.compareTo(rvalue) == 0) {
					isSelected = false;
					break;
				}
			} else if (op == 5) { // <=
				if (lvalue.compareTo(rvalue) > 0) {
					isSelected = false;
					break;
				}
			} else if (op == 6) { // >=
				if (lvalue.compareTo(rvalue) < 0) {
					isSelected = false;
					break;
				}
			} else {
				throw new UnsupportedOperationException("op: " + op + " lvalue: " + lvalue + " rvalue: " + rvalue);
			}
		}
		
//		elapsed = elapsed + Util.getElapsedTimeMicro(tid);
//		count++;
		return isSelected;
	}
	
	public ArrayList<Triple<Integer, Pair<Integer, Integer>, T>> getPredicates(Set<Atom> interpretedAtoms, 
		ArrayList<String> allCols) {
		
//		int tid = Util.startTimer();
        ArrayList<Triple<Integer, Pair<Integer, Integer>, T>> pred = new ArrayList<>();
        
		HashMap<String, Integer> varToPos = new HashMap<String, Integer>();
		for (int i = allCols.size() - 1; i >= 0; i--) { // backward
			varToPos.put(allCols.get(i), i);
		}

		for (Atom a : interpretedAtoms) {
			int op = 0;
			int lhv = -1;
			boolean toAdd = true;
			if (varToPos.containsKey(a.getTerms().get(0).toString()) == true) {
				lhv = varToPos.get(a.getTerms().get(0).toString());
			} else {
				toAdd = false;
			}
			int rhv = -1;
			if (a.getPredicate().equals(Config.predOpEq) == true || a.getPredicate().getRelName().equals("=")) {
				op = 1;
			} else if (a.getPredicate().equals(Config.predOpLt) == true || a.getPredicate().getRelName().equals("<")) {
				op = 2;
			} else if (a.getPredicate().equals(Config.predOpGt) == true || a.getPredicate().getRelName().equals(">")) {
				op = 3;
			} else if (a.getPredicate().equals(Config.predOpNeq) == true || a.getPredicate().getRelName().equals("!=")) {
				op = 4;
			} else if (a.getPredicate().equals(Config.predOpLe) == true || a.getPredicate().getRelName().equals("<=")) {
				op = 5;
			} else if (a.getPredicate().equals(Config.predOpGe) == true || a.getPredicate().getRelName().equals(">=")) {
				op = 6;
			} else {
				throw new UnsupportedOperationException("op: " + op + " a.pred: " + a.getPredicate());
			}
			
			T valueTerm = null;
//			System.out.println("aaa: " + a + " term1: " + a.getTerms().get(1) + " isVar: " + a.getTerms().get(1).isVariable());
			if (a.getTerms().get(1).isVariable() == true) {
				if (varToPos.containsKey(a.getTerms().get(1).toString()) == true) {
					rhv = varToPos.get(a.getTerms().get(1).toString());
				} else {
					toAdd = false;
				}
			} else {
				valueTerm = (T)a.getTerms().get(1).getSimpleTerm(); // FIXME
			}
			
			if (toAdd == true) {
				pred.add(Triple.of(op, Pair.of(lhv, rhv), valueTerm));
			}
		}
		
//		elapsed = elapsed + Util.getElapsedTimeMicro(tid);
//		count++;
		
//		System.out.println("[Relation] pred: " + pred);
		return pred;
		
	}
	
	public Relation<T> filter(Set<Atom> interpretedAtoms) {
//		int tid = Util.startTimer();
		ArrayList<Triple<Integer, Pair<Integer, Integer>, T>> pred = 
				getPredicates(interpretedAtoms, columns);
        
		HashSet<Tuple<T>> newTuples = new HashSet<Tuple<T>>();
		for (Tuple<T> t : tuples) {
			if (isSelected(t, null, pred) == true) {
				newTuples.add(t);
			}
		}
		Relation<T> newRel = new Relation(newTuples, columns);
		
//		System.out.println(">>time-filter: " + Util.getElapsedTime(tid));
		
//		System.out.println("filter elapsed: " + elapsed + " count: " + count);
//		elapsed = 0;
//		count = 0;
		return newRel;
	}
	
	public Relation<T> notin(Relation<T> relR, Atom a) {
		Relation<T> result = new Relation<T>();
		HashMap<String, Integer> colsMap = new HashMap<String, Integer>();
		for (int i = 0; i < getColumns().size(); i++) {
			result.getColumns().add(getColumns().get(i));
			colsMap.put(getColumns().get(i), i);
		}
//		HashSet<String> negatedCols = new LinkedHashSet<String>();
//		negatedCols.addAll(negatedAtom.getVars());

		ArrayList<Integer> map = new ArrayList<Integer>();
		for (int i = 0; i < a.getTerms().size(); i++) {
			Term t = a.getTerms().get(i);
			if (t.isVariable() == false) {
				throw new IllegalArgumentException("a: " + a + " has constant...");
			}
			map.add(colsMap.get(t.getVar()));
		}
		
//		HashSet<Tuple<T>> toBeDeleted = new LinkedHashSet<Tuple<T>>();
		for (Tuple<T> t1 : tuples) {
			boolean isIn = false;
			for (Tuple<T> t2 : relR.getTuples()) {
				if (isSame(t1, t2, map) == true) {
					isIn = true;
					break;
				}
			}
			
			if (isIn == false) {
				result.addTuple(t1);
			}
		}
//		tuples.removeAll(toBeDeleted);
		
//		System.out.println("notin map: " + map);
		return result;
	}
	
	private boolean isSame(Tuple<T> t1, Tuple<T> t2, ArrayList<Integer> map) {
		// TODO Auto-generated method stub
		boolean isSame = true;
		for (int i = 0; i < map.size(); i++) {
			if (map.get(i) == null) continue;

			T v1 = t1.getTuple().get(map.get(i));
			T v2 = t2.getTuple().get(i);
			
			if (v1.equals(v2) == false) {
				isSame = false;
				break;
			}
		}
		return isSame;
	}

	public Relation<T> join(Relation<T> rel, Set<Atom> interpretedAtoms) {
		Relation<T> result = new Relation<T>();
//		System.out.println("[join] rel.getTuples().size() : " + rel.getTuples().size() + " interpretedAtoms: "+ interpretedAtoms);
//		System.out.println("[join] rel: " + rel.getTuples());


		int tid = Util.startTimer();
		ArrayList<String> allCols = new ArrayList<String>(getColumns());
		allCols.addAll(rel.getColumns());

		ArrayList<String> relColumns = new ArrayList<String>(rel.getColumns());
		relColumns.removeAll(getColumns());

		ArrayList<String> newColumns = new ArrayList<String>(getColumns());
		newColumns.addAll(relColumns);

		//HashSet<Pair<Integer, Integer>> equals = new HashSet<Pair<Integer, Integer>>();
		HashSet<Integer> toBeDeleted = new HashSet<Integer>();

//		System.out.println("[Relation] allCols: " + allCols);
//		System.out.println("[Relation] columns: " + columns);
//		System.out.println("[Relation] rel.columns: " + rel.getColumns());
//		System.out.println("[Relation] newColumns: " + newColumns);
//		System.out.println("[Relation] interpretedAtoms: " + interpretedAtoms);

		ArrayList<Triple<Integer, Pair<Integer, Integer>, T>> pred = getPredicates(interpretedAtoms, allCols);

//		System.out.println("[Relation] pred_original2: " + pred);
//		System.out.println("[Relation] columns2: " + columns);
//		System.out.println("[Relation] rel.columns2: " + rel.getColumns());
//		System.out.println("[Relation] newColumns2: " + newColumns);
//		System.out.println("[Relation] interpretedAtoms2: " + interpretedAtoms);

		
		for (int i = 0; i < allCols.size(); i++) {
			if (allCols.get(i).equals("_") == true) {
				toBeDeleted.add(i);
			}			
			for (int j = i + 1; j < allCols.size(); j++) {
				if (allCols.get(j).equals("_") == true) {
					toBeDeleted.add(j);
					continue;
				}
				if (allCols.get(i).contentEquals(allCols.get(j)) == true) {
					//equals.add(Pair.of(i, j));
					pred.add(Triple.of(1, Pair.of(i, j), null)); // FIXME was 0
					toBeDeleted.add(j);
				}
			}
		}

//		System.out.println("[Relation] pred: " + pred);

		ArrayList<Integer> toBeDeletedSorted = new ArrayList<Integer>();
		toBeDeletedSorted.addAll(toBeDeleted);
		Collections.sort(toBeDeletedSorted);
	
//		System.out.println("toBeDeletedSorted: " + toBeDeletedSorted);
//		System.out.println("newColumns: " + newColumns);
		
		for (int i = 0; i < allCols.size(); i++) {
			if (toBeDeletedSorted.contains(i) == false) {
				result.getColumns().add(allCols.get(i));
			}
		}
		
//		System.out.println("join time2: " + Util.getElapsedTimeMicro(tid));
	
		int cnt = 0;
		int sizeOfFirstTuple = getColumns().size();
		int sizeOfSecondTuple = rel.getColumns().size();
		for (Tuple<T> t1 : tuples) {
			for (Tuple<T> t2 : rel.getTuples()) {
				cnt++;
//				Tuple<T> newTuple = t1.copy(); 	
//				newTuple.getTuple().addAll(t2.getTuple());
				
//				System.out.println("t1: " + t1 + " t2: " + t2 + " isSelected(t1, t2, pred) == true: " + isSelected(t1, t2, pred));
				if (isSelected(t1, t2, pred) == true) {
					Tuple<T> newTuple2 = new Tuple<T>();//t1.copy();
					for (int i = 0; i < sizeOfFirstTuple; i++) {
						if (toBeDeletedSorted.contains(i) == false) {
							newTuple2.getTuple().add(t1.getTuple().get(i));
						}
					}
					for (int i = 0; i < sizeOfSecondTuple; i++) {
						if (toBeDeletedSorted.contains(i+sizeOfFirstTuple) == false) {
							newTuple2.getTuple().add(t2.getTuple().get(i));
						}
					}
					
//					for (int i = toBeDeletedSorted.size() - 1; i >=0; i--) {
//						newTuple.getTuple().remove(toBeDeletedSorted.get(i).intValue());
//					}
					result.addTuple(newTuple2);
					
//					System.out.println("	[added] newTuple2: " + newTuple2);
				} else {
//					System.out.println("[not added] newTuple: " + newTuple);
				}
			}
		}
//		System.out.println("join time3: " + Util.getElapsedTimeMicro(tid));
		
//		System.out.println("join elapsed: " + elapsed + " count: " + count);
		elapsed = 0;
		count = 0;
//		System.out.println(">>time-join: " + Util.getElapsedTime(tid)
//				+ " result.size: " + result.getTuples().size() + " cnt: " + cnt
//				+ " tuples: " + tuples.size() + " rel: " + rel.getTuples().size());
		
//		System.out.println("joined: " + result);
		return result;
	}
	
	public String toString() {
//		return "Relation: {\n\tcolumns: " + columns + "\n\tsize: " + tuples.size() + "\n}"; // + "\n\ttuples: " + tuples + "\n}"; 
		String str = "Relation: {\n\tcolumns: " + columns + "\n\tsize: " + tuples.size() + "\n\ttuples: \n";
		for (Tuple<T> t : tuples) {
			str += "\t" + t;
			str += "\n";
		}
		return str;	
	}

//	@SuppressWarnings("unchecked")
//	public Relation<T> addNewId(Atom a) {
//		// TODO Auto-generated method stub
//		Relation<T> result = new Relation<T>();
//		
//		int lastIndex = a.getTerms().size() - 1;
//		String newVar = a.getTerms().get(lastIndex).getVar();
//		
//		HashMap<String, Integer> colsMap = new HashMap<String, Integer>();
//		for (int i = 0; i < getColumns().size(); i++) {
//			colsMap.put(getColumns().get(i), i);
//		}
//
//		
////		System.out.println("[addNewId] a: " + a + " newVar: " + newVar);
//		
//		result.getColumns().addAll(columns);
//		result.getColumns().add(newVar);
//		for (Tuple<T> t1 : tuples) {
//			Tuple<T> t2 = new Tuple<T>();
//			ArrayList<Long> keyArr = new ArrayList<Long>();
//			for (int i = 0; i < a.getTerms().size()-1; i++) {
////				System.out.println("[AddNewId] a: " + a);
//				if (a.getTerms().get(i).isConstant() == true) {
//					keyArr.add(Long.parseLong(a.getTerms().get(i).toString()));
//				} else {
//					String var = a.getTerms().get(i).getVar();
//					if (colsMap.containsKey(var) == false) {
//						throw new IllegalArgumentException("colsMap: " + colsMap + " var: "+ var + " a: " + a);
//					}
//					int index = colsMap.get(var);
//					long value = (long)((LongSimpleTerm)t1.getTuple().get(index)).getLong();
//					keyArr.add(value);
//				}
//			}
//			for (T t : t1.getTuple()) {
//				t2.getTuple().add(t);
//			}
//			long newId = SimpleDatalogEngine.geteNewId(keyArr);
//			t2.getTuple().add((T)new LongSimpleTerm(newId));
//			result.getTuples().add(t2);
//		}
//		return result;
//	}

}
