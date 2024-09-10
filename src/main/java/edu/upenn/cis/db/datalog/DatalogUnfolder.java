package edu.upenn.cis.db.datalog;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;

public class DatalogUnfolder {

	
	private static ArrayList<ArrayList<Atom>> getUnfoldings(DatalogClause q, DatalogProgram p) {
		ArrayList<ArrayList<Atom>> unfoldings = new ArrayList<ArrayList<Atom>>();
		
		Queue<ArrayList<Atom>> queue = new LinkedList<ArrayList<Atom>>();
		queue.add(q.getBody());
		
		while(queue.isEmpty() == false) {
			ArrayList<Atom> dc = queue.poll();
			System.out.println("dc: " + dc);
			
			boolean didUnfold = false;
			for (int i = 0; i < dc.size(); i++) {
				Atom a = dc.get(i);
				if (p.getHeadRules().contains(a.getRelName()) == true) { // if IDB
					if (a.isNegated() == true) { // IDB atom is negative
						for (int j = 0; j < p.getRules(a.getRelName()).size(); j++) { // for each IDB rule
							for (int k = 0; k < p.getRules(a.getRelName()).get(j).getBody().size(); k++) {
								ArrayList<Atom> d = new ArrayList<Atom>(); // unfolding
								for (int t = 0; t < dc.size(); t++) {
									if (t == i) {
										Atom b = p.getRules(a.getRelName()).get(j).getBody().get(k);
										try {
											Atom c = (Atom)b.clone();
											if (c.isNegated() == true) {
												c.setNegated(false);
											} else {
												c.setNegated(true);
											}
											d.add(c);
										} catch (CloneNotSupportedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									} else {
										Atom b = dc.get(k);
										try {
											d.add((Atom)b.clone());
										} catch (CloneNotSupportedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
								didUnfold = true;
								queue.add(d);								
							}
						}
					} else { // IDB atom is positive
						for (int j = 0; j < p.getRules(a.getRelName()).size(); j++) { // for each IDB rule
							ArrayList<Atom> d = new ArrayList<Atom>(); // unfolding 
							for (int k = 0; k < dc.size(); k++) { // add replaced IDB's body
								if (k == i) {
									DatalogClause d1 = p.getRules(a.getRelName()).get(j);
									for (int r = 0; r < d1.getBody().size(); r++) {
										Atom b = d1.getBody().get(r); 
										try {
											d.add((Atom)b.clone());
										} catch (CloneNotSupportedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								} else {
									Atom b = dc.get(k);
									try {
										d.add((Atom)b.clone());
									} catch (CloneNotSupportedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
							didUnfold = true;
							queue.add(d);
						}
					}
					break;
				}
			}
			if (didUnfold == false) {			
				unfoldings.add(dc);
			}
		}
		
		return unfoldings;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		System.out.println("[DatalogUnfolder] start ...");
		DatalogProgram p = new DatalogProgram();
		DatalogParser parser = new DatalogParser(p);
		
		parser.Parse("N(a,b) <- R(a,c), S(b,c).");
		parser.Parse("N(a,b) <- R(a,c), T(b,c).");
		parser.Parse("M(a,b) <- U(a,c), V(b,c).");
		parser.Parse("M(a,b) <- U(a,c), X(b,c).");		
		
		System.out.println("program: " + p);
		
		DatalogClause q = parser.ParseQuery("Q(x,z) <- N(x,y), !M(y,z).");	
		System.out.println("query: " + q);
		
		ArrayList<ArrayList<Atom>> unfoldings = DatalogUnfolder.getUnfoldings(q, p);
		System.out.println("unfoldings: " + unfoldings);

		
		System.out.println("[DatalogUnfolder] end ...");

	}

}
