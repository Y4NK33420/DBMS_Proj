package edu.upenn.cis.db.datalog.simpleengine;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class RelationIterator<T extends Comparable<T>> implements Iterator<Tuple<T>> {
	Relation<T> rel;
	ArrayList<Triple<Integer, Pair<Integer, Integer>, T>> preds;

	int index;
    Iterator<Tuple<T>> iter; 

	Tuple<T> nextTuple;
	
	public RelationIterator(Relation rel) {
		// TODO Auto-generated method stub
		this.rel = rel;
		index = 0;
	}

	public RelationIterator(Relation rel, ArrayList<Triple<Integer, Pair<Integer, Integer>, T>> preds) {
		// TODO Auto-generated method stub
		this.rel = rel;
		this.preds = preds;
		index = 0;
	}
	
	public boolean isSelected(Tuple<T> t, 
			ArrayList<Triple<Integer, Pair<Integer, Integer>, T>> pred) {
		// a > 1, a = 3, a > b, a= b
		// op, (index1, index2=-1), or value)
		for (int i = 0; i < pred.size(); i++) {
			int op = pred.get(i).getLeft();
			int lhv = pred.get(i).getMiddle().getLeft();
			int rhv = pred.get(i).getMiddle().getRight();
			T value = pred.get(i).getRight();
			T lvalue = t.getTuple().get(lhv);
			T rvalue; 
			if (rhv >= 0) {
				rvalue = t.getTuple().get(rhv);
			} else {
				rvalue = value;
			}

			if (op == 1) { // eq
				if (lvalue.compareTo(rvalue) != 0) return false;
			} else if (op == 2) { // <
				if (lvalue.compareTo(rvalue) >= 0) return false;
			} else if (op == 3) { // >
				if (lvalue.compareTo(rvalue) <= 0) return false;
			}
		}
		return true;
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		if (iter == null) {
			 iter = rel.getTuples().iterator();
		}
		nextTuple = null;
		while(iter.hasNext() == true) {
			Tuple<T> t = (Tuple<T>)iter.next();
			if (isSelected(t, preds) == true) {
				nextTuple = t; //rel.getTuples().get(index);
				break;
			}
//			iter = iter.next().next();
//			index++;
		}
//		while(index < rel.getTuples().size()) {
//			Tuple<T> t = rel.getTuples().get(index);
//			if (isSelected(t, preds) == true) {
//				nextTuple = rel.getTuples().get(index);
//				break;
//			}
//			index++;
//		}
		return nextTuple != null;
	}

	@Override
	public Tuple<T> next() {
		// TODO Auto-generated method stub
		return nextTuple; //rel.getTuples().get(index++);
	}

}
