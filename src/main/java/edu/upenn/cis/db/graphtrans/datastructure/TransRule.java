package edu.upenn.cis.db.graphtrans.datastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.commons.lang3.tuple.Triple;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.helper.Util;

/**
 * Constructor of TransRule. Represent a transformation rule.
 * 
 * @author sbnet21
 *
 */
public class TransRule {
	private long level;
	private String query;
	
	private String headMatch = null;	
	private ArrayList<Atom> patternMatch; // with id_l, ld_r
	private ArrayList<Atom> patternConstruct; 
	private ArrayList<Atom> patternBefore; // to be used for typechecking
	private ArrayList<Atom> patternAffected;
	private ArrayList<Atom> patternAfter;
	private ArrayList<Atom> patternAfterForIndexing; // After including unchanged Before
	private HashSet<String> affectedVariables; // colored

	private ArrayList<Atom> patternAugmentedBefore;
	private ArrayList<Atom> patternAugmentedAfter;
	
	private ArrayList<Atom> patternAdd;

	private ArrayList<Atom> patternRemove;
	private HashMap<Atom, HashSet<String>> mapMap;

	private HashMap<String, String> mapSrcToMetaMap;
	private HashSet<String> metaSet;
	private HashSet<String> metaNodeLabel;
	private HashSet<String> metaEdgeLabel;	
	private HashSet<String> starVarSet; // it contains 'a' if {a:A}-(x:X)->b:B  
	private HashMap<String, String> nodeVarToLabelMap;
	private HashMap<String, String> metaLabelToVarMap;
	
	private HashMap<String, Triple<String, Integer, Integer>> metaNodeMap;
	private HashSet<String> varsInWhereClause;
	
	private ArrayList<String> whereConditionForNeo4j;
	
	private HashSet<String> newNodeVars;
	private HashSet<String> newEdgeVars;
	
	private HashSet<String> matchNodeVars;
	private HashSet<String> matchEdgeVars;
	
	private HashMap<String, ArrayList<String>> SkolemFunctionMap;
	private HashMap<String, ArrayList<String>> mapFromToMap;
	
	private HashSet<String> edgeVarsToDelete;
	private HashSet<String> nodeVarsToDelete;
	
//	private Atom matchHead;

	public TransRule(String q) {
		query = q;


		SkolemFunctionMap = new HashMap<String, ArrayList<String>>();
		setMapFromToMap(new HashMap<String, ArrayList<String>>());
		 
		edgeVarsToDelete = new LinkedHashSet<String>();
		nodeVarsToDelete = new LinkedHashSet<String>();
		
		newNodeVars = new LinkedHashSet<String>();
		newEdgeVars = new LinkedHashSet<String>();
		
		matchNodeVars = new LinkedHashSet<String>();
		matchEdgeVars = new LinkedHashSet<String>();
		
		patternMatch = new ArrayList<Atom>();
		patternConstruct = new ArrayList<Atom>();
		patternBefore = new ArrayList<Atom>();
		patternAffected = new ArrayList<Atom>();
		patternAfter = new ArrayList<Atom>();
		patternAfterForIndexing = new ArrayList<Atom>();
		
		affectedVariables = new LinkedHashSet<String>();
		
		patternAugmentedBefore = new ArrayList<Atom>();
		patternAugmentedAfter = new ArrayList<Atom>();
		
		patternAdd = new ArrayList<Atom>();
		patternRemove = new ArrayList<Atom>();
		mapMap = new HashMap<Atom, HashSet<String>>();
		mapSrcToMetaMap = new HashMap<String, String>();
		metaSet = new HashSet<String>();
		metaNodeLabel = new HashSet<String>();
		metaEdgeLabel = new HashSet<String>();
		starVarSet = new HashSet<String>();
		nodeVarToLabelMap = new HashMap<String, String>();
		metaLabelToVarMap = new HashMap<String, String>();

		metaNodeMap = new HashMap<String, Triple<String, Integer, Integer>>();
		varsInWhereClause = new HashSet<String>();
		
		
		
		whereConditionForNeo4j = new ArrayList<String>();
//		System.out.println("[TransRule] **********************");
	}

	public HashSet<String> getAffectedVariables() {
		return affectedVariables;
	}

	public ArrayList<String> getWhereConditionForNeo4j() {
		return whereConditionForNeo4j;
	}
	
	public HashSet<String> getVarsInWhereClause() {
		return varsInWhereClause;
	}
	
	public HashMap<String, Triple<String, Integer, Integer>> getMetaNodeMap() {
//		System.out.println("[getMetaNodeMap] ********************** get");
		return metaNodeMap;
	}

	public String getQuery() {
		return query;
	}
	
	public HashSet<String> getMetaSet() {
		return metaSet;
	}

	public HashSet<String> getMetaEdgeLabel() {
		return metaEdgeLabel;
	}
	
	public HashSet<String> getMetaNodeLabel() {
		return metaNodeLabel;
	}
	
	public HashMap<Atom, HashSet<String>> getMapMap() {
		return mapMap;
	}
	
	public HashMap<String, String> getMetaLabelToVarMap() {
		return metaLabelToVarMap;
	}

	public void addMapMap(Atom atom, HashSet<String> set) {
		mapMap.put(atom, set);
		String meta = atom.getTerms().get(0).toString(); // pick as a representative node
		String label = atom.getTerms().get(1).toString();
		
		metaSet.add(meta);
		metaNodeLabel.add(Util.removeQuotes(label));
		metaLabelToVarMap.put(label, meta);
		
		for (String var : set) {
			mapSrcToMetaMap.put(var, meta);
		}
	}

//	private boolean isMetaNode(String var) {
//		return mapSrcToMetaMap.containsKey(var);
//	}
//
//	private boolean isMapSrcNode(String var) {
//		return metaSet.contains(var);
//	}

	public void addAtomToPatternAdd(Atom a) {
		patternAdd.add(a);
	}

	public void addAtomToPatternRemove(Atom a) {
		patternRemove.add(a);
	}

	public void addAtomToPatternBefore(Atom a) {
		patternBefore.add(a);
		try {
			Atom b = (Atom)a.clone();
			// FIXME: make it more general to handle predefined predicates 
			patternAfterForIndexing.add(b);			
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addAtomToPatternMatch(Atom a) {
		patternMatch.add(a);
	}

	public void addAtomToPatternConstruct(Atom a) {
		patternConstruct.add(a);
	}
	
	public void addVarToStarVarSet(String a) {
		starVarSet.add(a);
	}
	
	public HashSet<String> getStarVarSet() {
		return starVarSet;
	}
	
	/**
	 * Compute patterAffected, patternAfter based on patternBefore, map, add, remove.
	 */
	public void computePatterns() {
		// FIXME: using map, add, remove
		HashSet<String> affectedNodeSet = new HashSet<>();
		HashSet<String> afterNodeSet = new HashSet<>();

		/**
		 * Compute affectedPattern
		 * 1. affectedNodeSet
		 * 		- source nodes of mapping
		 * 		- added or removed nodes
		 * 		- endpoints (not metanode) of added or removed edges
		 * 2. affecteEdgeSet
		 * 		- add edge in beforePattern or added/removed edges 
		 * 		  if both endpoints are in affectedNodeSet (i.e., not metanode)
		 * 
		 * Compute afterPattern
		 * 1. afterNodeSet
		 * 		- same as above, but target nodes of mapping
		 * 2. edges
		 * 		- add edge in beforePattern after replacing map.src with map.dst
		 * 		- add added/removed edges
		 * 
		 * Compute patternAfterForIndexing
		 * 1. Starting from before
		 * 2. Apply mapping (replace nodes)
		 * 3. Add node/edge
		 * 4. Remove edge/edge
		 * 5. Remove duplicated node/edge (?)
		 */
		pupulateAugmentedBefore();
		
		for (HashMap.Entry<Atom, HashSet<String>> entry : mapMap.entrySet()) {
			// maps entry.getValue() to entry.getKey()
			patternAfterForIndexing.add(entry.getKey());

			for (String var : entry.getValue()) {
				affectedNodeSet.add(var);
				affectedVariables.add(var);
				
				// Substitute fromVar to toVar
				String toVar = entry.getKey().getTerms().get(0).getVar();
				for (int i = 0; i < patternAfterForIndexing.size(); i++) {
					Atom a = patternAfterForIndexing.get(i);
					if (a.getPredicate().equals(Config.predN) == true) {
						if (a.getTerms().get(0).toString().equals(var) == true) {
							patternAfterForIndexing.remove(i);
							break;
						}
					} else if (a.getPredicate().equals(Config.predE) == true) {
						if (a.getTerms().get(1).toString().equals(var) == true) {
							a.getTerms().get(1).setVar(toVar);
						}
						if (a.getTerms().get(2).toString().equals(var) == true) {
							a.getTerms().get(2).setVar(toVar);
						}
					}
				}
			}
			afterNodeSet.add(entry.getKey().getTerms().get(0).toString());
			patternAfter.add(entry.getKey()); // metanode 
		}

		// FIXME: add code for ADD/REMOVE node/edge
		for (int i = 0; i < patternAdd.size(); i++) {
			affectedNodeSet.add(patternAdd.get(i).getTerms().get(0).toString());
			if (patternAdd.get(i).getRelName().contentEquals(Config.relname_edge) == true) {
				affectedVariables.add(patternAdd.get(i).getTerms().get(1).toString());
				affectedVariables.add(patternAdd.get(i).getTerms().get(2).toString());
			}
			affectedVariables.add(patternAdd.get(i).getTerms().get(0).toString());
			afterNodeSet.add(patternAdd.get(i).getTerms().get(0).toString());

			Atom a = (Atom)patternAdd.get(i);
			patternAfterForIndexing.add(a); // node or edge
			
//			System.out.println("[TransRule] computePatterns() patternAdd a: " + a);
		}

		for (int i = 0; i < patternRemove.size(); i++) {
			affectedNodeSet.add(patternRemove.get(i).getTerms().get(0).toString());
			affectedVariables.add(patternRemove.get(i).getTerms().get(0).toString());
			afterNodeSet.add(patternRemove.get(i).getTerms().get(0).toString());
			
			// remove node and edge related to the removed node
			String var = patternRemove.get(i).getTerms().get(0).getVar();
			for (int j = 0; j < patternAfterForIndexing.size(); j++) {
				Atom a = patternAfterForIndexing.get(j);
				if (a.getPredicate().equals(Config.predN) == true) {
					if (a.getTerms().get(0).toString().equals(var) == true) {
						patternAfterForIndexing.remove(j);
						break;
					}
				} else if (a.getPredicate().equals(Config.predE) == true) {
					if (a.getTerms().get(1).toString().equals(var) == true) {
						patternAfterForIndexing.remove(j);
					} else if (a.getTerms().get(2).toString().equals(var) == true) {
						patternAfterForIndexing.remove(j);
					}
				}
			}
		}
		
//		for (int i = 0; i < patternBefore.size(); i++) {
//			if (patternBefore.get(i).getPredicate().hasSameRelName(Config.predN)) { // node
//				if (affectedNodeSet.contains(patternBefore.get(i).getTerms().get(0).toString())) {
//					patternAffected.add(patternBefore.get(i));
//				}
//				if (afterNodeSet.contains(patternBefore.get(i).getTerms().get(0).toString())) {
//					patternAfter.add(patternBefore.get(i));
//				}
//			} else { // edge
//				if (affectedNodeSet.contains(patternBefore.get(i).getTerms().get(1).toString())
//						|| affectedNodeSet.contains(patternBefore.get(i).getTerms().get(2).toString())) {
//					affectedVariables.add(patternBefore.get(i).getTerms().get(0).toString());
//				}
//				
//				if (affectedNodeSet.contains(patternBefore.get(i).getTerms().get(1).toString())
//						&& affectedNodeSet.contains(patternBefore.get(i).getTerms().get(2).toString())) {
//					patternAffected.add(patternBefore.get(i));
//				}
//				if (afterNodeSet.contains(patternBefore.get(i).getTerms().get(1).toString())
//						|| afterNodeSet.contains(patternBefore.get(i).getTerms().get(2).toString())) {
//					patternAfter.add(patternBefore.get(i));
//				}
//			}
//		}
		
//		System.out.println("patternAdd: " + patternAdd);
//		System.out.println("patternBefore: " + patternBefore);
//		System.out.println("patternAffected: " + patternAffected);
//		System.out.println("patternAfter: " + patternAfter);
//		System.out.println("affectedNodeSet: " + affectedNodeSet);
//		System.out.println("afterNodeSet: " + afterNodeSet);

		for (int i = 0; i < patternAdd.size(); i++) {
			patternAfter.add(patternAdd.get(i));
//			Atom a = patternAdd.get(i);
//			if (a.getPredicate().getRelName().contentEquals(Config.relname_node) == true) {
//				patternAfter.add(a);
//			}
//			
//			if (affectedNodeSet.contains(patternAdd.get(i).getTerms().get(1).toString())
//					|| affectedNodeSet.contains(patternBefore.get(i).getTerms().get(2).toString())) {
//				patternAffected.add(patternAdd.get(i));
//			}
//			if (afterNodeSet.contains(patternBefore.get(i).getTerms().get(1).toString())
//					|| afterNodeSet.contains(patternBefore.get(i).getTerms().get(2).toString())) {
//				patternAfter.add(patternAdd.get(i));
//			}
		}

//		System.out.println("2patternAdd: " + patternAdd);
//		System.out.println("2patternBefore: " + patternBefore);
//		System.out.println("2patternAfter: " + patternAfter);
//		System.out.println("2affectedNodeSet: " + affectedNodeSet);
//		System.out.println("2afterNodeSet: " + afterNodeSet);
		
		for (int i = 0; i < patternRemove.size(); i++) {
			patternAffected.add(patternRemove.get(i));
	
//			if (affectedNodeSet.contains(patternBefore.get(i).getTerms().get(1).toString())
//					|| affectedNodeSet.contains(patternBefore.get(i).getTerms().get(2).toString())) {
//				patternAfter.add(patternRemove.get(i));
//			}
//			if (afterNodeSet.contains(patternBefore.get(i).getTerms().get(1).toString())
//					|| afterNodeSet.contains(patternBefore.get(i).getTerms().get(2).toString())) {
//				patternAfter.add(patternRemove.get(i));
//			}		
		}
//		System.out.println("3patternAdd: " + patternAdd);
//		System.out.println("3patternBefore: " + patternBefore);
//		System.out.println("3patternAfter: " + patternAfter);
//		System.out.println("3patternAffected: " + patternAffected);
//		System.out.println("3affectedNodeSet: " + affectedNodeSet);
//		System.out.println("3afterNodeSet: " + afterNodeSet);
	}
	
	private void pupulateAugmentedBefore() {
//		// FIXME: remove patternAugmentedAfter.add(b);
//		for (Atom a : patternBefore) {
//			String var = a.getTerms().get(0).getVar().toString();
//			patternAugmentedBefore.add(a);
//			patternAugmentedAfter.add(a);
//			if (a.getPredicate().hasSameRelName(Config.predN)) { // node
//				Atom b;
//				b = new Atom(Config.predN);
//				b.appendTerm(new Term(var + "_i0", true));
//				b.appendTerm(new Term(var + "_i0_l", true));
//				patternAugmentedBefore.add(b);
//				patternAugmentedAfter.add(b);
//				
//				b = new Atom(Config.predN);
//				b.appendTerm(new Term(var + "_i1", true));
//				b.appendTerm(new Term(var + "_i1_l", true));
//				patternAugmentedBefore.add(b);
//				patternAugmentedAfter.add(b);
//				
//				b = new Atom(Config.predN);
//				b.appendTerm(new Term(var + "_o0", true));
//				b.appendTerm(new Term(var + "_o0_l", true));
//				patternAugmentedBefore.add(b);
//				patternAugmentedAfter.add(b);
//				
//				b = new Atom(Config.predN);
//				b.appendTerm(new Term(var + "_o0", true));
//				b.appendTerm(new Term(var + "_o0_l", true));
//				patternAugmentedBefore.add(b);
//				patternAugmentedAfter.add(b);
//				
//				b = new Atom(Config.predE);
//				b.appendTerm(new Term(var + "_ei0", true));
//				b.appendTerm(new Term(var + "_i0", true));
//				b.appendTerm(new Term(var, true));
//				b.appendTerm(new Term(var + "_ei0_l", true));
//				patternAugmentedBefore.add(b);
//				patternAugmentedAfter.add(b);
//				
//				b = new Atom(Config.predE);
//				b.appendTerm(new Term(var + "_ei1", true));
//				b.appendTerm(new Term(var + "_i1", true));
//				b.appendTerm(new Term(var, true));
//				b.appendTerm(new Term(var + "_ei1_l", true));
//				patternAugmentedBefore.add(b);
//				patternAugmentedAfter.add(b);
//				
//				b = new Atom(Config.predE);
//				b.appendTerm(new Term(var + "_eo0", true));
//				b.appendTerm(new Term(var, true));
//				b.appendTerm(new Term(var + "_o0", true));
//				b.appendTerm(new Term(var + "_eo0_l", true));
//				patternAugmentedBefore.add(b);
//				patternAugmentedAfter.add(b);
//				
//				b = new Atom(Config.predE);
//				b.appendTerm(new Term(var + "_eo1", true));
//				b.appendTerm(new Term(var, true));
//				b.appendTerm(new Term(var + "_o1", true));
//				b.appendTerm(new Term(var + "_eo1_l", true));
//				patternAugmentedBefore.add(b);
//				patternAugmentedAfter.add(b);
//			}			
//		}
	}

	public ArrayList<Atom> getPatternMatch() {
		return patternMatch;
	}
	
	public ArrayList<Atom> getPatternConstruct() {
		return patternConstruct;
	}
	
	public ArrayList<Atom> getPatternBefore() {		
		return patternBefore;
	}


	public ArrayList<Atom> getPatternAugmentedBefore() {		
		return patternAugmentedBefore;
	}

	public ArrayList<Atom> getPatternAffected() {		
		return patternAffected;
	}

	public ArrayList<Atom> getPatternAfter() {		
		return patternAfter;
	}

	public ArrayList<Atom> getPatternAugmentedAfter() {		
		return patternAugmentedAfter;
	}
	
	public ArrayList<Atom> getPatternAfterForIndexing() {		
		return patternAfterForIndexing;
	}
	

	public void show() {
		System.out.println("TransRule\n==========");
		System.out.println("match: " + patternMatch);
		System.out.println("construct: " + patternConstruct);
		System.out.println("before: " + patternBefore);
		System.out.println("affected: " + patternAffected);
		System.out.println("after: " + patternAfter);
		System.out.println("augmentedBefore: " + patternAugmentedBefore);
		System.out.println("augmentedAfter: " + patternAugmentedAfter);
		System.out.println("patternAdd: " + patternAdd);
		System.out.println("patternRemove: " + patternRemove);
//		System.out.println("starVarSet: " + starVarSet);
//		System.out.println("mapMap: " + mapMap);
		System.out.println("SkolemFunctionMap: " + SkolemFunctionMap);
		System.out.println("mapFromToMap: " + mapFromToMap);
	}

	public long getLevel() {
		return level;
	}

	public void setLevel(long level) {
		this.level = level;
	}
	
	public ArrayList<Atom> getPatternAdd() {
		return patternAdd;
	}

	public ArrayList<Atom> getPatternRemove() {
		return patternRemove;
	}

	public HashMap<String, String> getNodeVarToLabelMap() {
		return nodeVarToLabelMap;
	}

	public void setNodeVarToLabelMap(HashMap<String, String> nodeVarToLabelMap) {
		this.nodeVarToLabelMap = nodeVarToLabelMap;
	}

	public String getHeadMatch() {
		return headMatch;
	}

	public HashSet<String> getNewNodeVars() {
		return newNodeVars;
	}

	public void setNewNodeVars(HashSet<String> newNodeVars) {
		this.newNodeVars = newNodeVars;
	}

	public HashSet<String> getNewEdgeVars() {
		return newEdgeVars;
	}

	public void setNewEdgeVars(HashSet<String> newEdgeVars) {
		this.newEdgeVars = newEdgeVars;
	}

	public HashSet<String> getMatchNodeVars() {
		return matchNodeVars;
	}

	public void setMatchNodeVars(HashSet<String> matchNodeVars) {
		this.matchNodeVars = matchNodeVars;
	}

	public HashSet<String> getMatchEdgeVars() {
		return matchEdgeVars;
	}

	public void setMatchEdgeVars(HashSet<String> matchEdgeVars) {
		this.matchEdgeVars = matchEdgeVars;
	}

	public HashMap<String, ArrayList<String>> getSkolemFunctionMap() {
		return SkolemFunctionMap;
	}

	public void setSkolemFunctionMap(HashMap<String, ArrayList<String>> skolemFunctionMap) {
		SkolemFunctionMap = skolemFunctionMap;
	}

	public HashMap<String, ArrayList<String>> getMapFromToMap() {
		return mapFromToMap;
	}

	public void setMapFromToMap(HashMap<String, ArrayList<String>> mapFromToMap) {
		this.mapFromToMap = mapFromToMap;
	}

	public HashSet<String> getEdgeVarsToDelete() {
		return edgeVarsToDelete;
	}

	public void setEdgeVarsToDelete(HashSet<String> edgeVarsToDelete) {
		this.edgeVarsToDelete = edgeVarsToDelete;
	}

	public HashSet<String> getNodeVarsToDelete() {
		return nodeVarsToDelete;
	}

	public void setNodeVarsToDelete(HashSet<String> nodeVarsToDelete) {
		this.nodeVarsToDelete = nodeVarsToDelete;
	}

//	public void setHeadMatch(String headMatch) {
//		this.headMatch = headMatch;
//	}
//	
//	public Atom getMatchHead() {
//		return matchHead;
//	}
//	
//	public void setMatchHead(Atom h) {
//		matchHead = h;
//	}
}
