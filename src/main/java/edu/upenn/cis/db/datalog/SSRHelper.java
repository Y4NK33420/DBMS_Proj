package edu.upenn.cis.db.datalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.catalog.Schema;
import edu.upenn.cis.db.graphtrans.catalog.SchemaEdge;
import edu.upenn.cis.db.graphtrans.catalog.SchemaNode;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.graphtrans.store.simpledatalog.SimpleDatalogStore;
import edu.upenn.cis.db.helper.Util;
import edu.upenn.cis.db.logicblox.LogicBlox;

/**
 * @author sbnet21
 * SSR (Substitution Subgraph Relations)
 */
public class SSRHelper {
	private static HashMap<String, Integer> nodeLabelToIdMap = new HashMap<String, Integer>();
	private static int nodeLabelId = 0;
	private static int edgeLabelId = 0;

	private static String workspace = "ssr";
	private static Store store;
	
	public static void init() {
		nodeLabelToIdMap.clear();
		nodeLabelId = 0;
		edgeLabelId = 0;
	}
	
	public static void populateSchemaGraph() {
		store = GraphTransServer.getBaseStore();
//		if (store.listDatabases().contains(workspace) == true) {
//			store.deleteDatabase(workspace);
//			store.createDatabase(workspace);
//			store.connect();
//		}
//		
//		((SimpleDatalogStore)store).removeRelation(workspace, "S_"+Config.relname_node);
//		((SimpleDatalogStore)store).removeRelation(workspace, "S_"+Config.relname_edge);
		
		// 1. Create Schema Graph
		Predicate pred = new Predicate("S_" + Config.relname_node);
		pred.setArgNames("id", "label");
		pred.setTypes(Type.Integer, Type.String);
		store.createSchema(workspace, pred);

		pred = new Predicate("S_" + Config.relname_edge);
		pred.setArgNames("id", "from", "to", "label");
		pred.setTypes(Type.Integer, Type.Integer, Type.Integer, Type.String);
		store.createSchema(workspace, pred);

		// 2. Populate Schema Graph S(N, E)
//		System.out.println(" Schema.getSchemaNodes()): " +  Schema.getSchemaNodes());
//		System.out.println(" nodeLabelToIdMap: " +  nodeLabelToIdMap);

		
		for (SchemaNode s : Schema.getSchemaNodes()) {
			String label = s.getLabel();
			if (nodeLabelToIdMap.containsKey(label) == false) {
				nodeLabelToIdMap.put(label, nodeLabelId);
				nodeLabelId++;
			}
			int nodeId = nodeLabelToIdMap.get(label);
			ArrayList<SimpleTerm> t = new ArrayList<SimpleTerm>();
			t.add(new LongSimpleTerm(nodeId));
			t.add(new StringSimpleTerm(label));
			store.addTuple("S_" + Config.relname_node, t);
			
		}
//		System.out.println(" Schema.getSchemaEdges()): " +  Schema.getSchemaEdges());

		for (SchemaEdge s : Schema.getSchemaEdges()) {
			String label = s.getLabel();
			String fromLabel = s.getFrom();
			String toLabel = s.getTo();
			int fromId = nodeLabelToIdMap.get(fromLabel);
			int toId = nodeLabelToIdMap.get(toLabel);

			ArrayList<SimpleTerm> t = new ArrayList<SimpleTerm>();
			t.add(new LongSimpleTerm(edgeLabelId));
			t.add(new LongSimpleTerm(fromId));
			t.add(new LongSimpleTerm(toId));
			t.add(new StringSimpleTerm(label));
			store.addTuple("S_" + Config.relname_edge, t);

			edgeLabelId++;
		}
	}

	public static void populateSchemasByRule(TransRule tr, String viewName) {
		populateTransformedSchemaGraph(tr);
		populateSSRSchemaGraph(tr, viewName);
	}

	private static void populateTransformedSchemaGraph(TransRule tr) {
		ArrayList<DatalogClause> cs = new ArrayList<DatalogClause>();
		HashMap<String, String> varToLabelMap = tr.getNodeVarToLabelMap();
		
		// 1. Create SchemaView Graph SV
		DatalogClause c = new DatalogClause();
		c.setHead(new Atom("SV_N", "n", "l"));
		c.addAtomToBody(new Atom("S_N", "n", "l"));
		cs.add(c);
		
		c = new DatalogClause();
		c.setHead(new Atom("SV_E", "e", "f", "t", "l"));
		c.addAtomToBody(new Atom("S_E", "e", "f", "t", "l"));
		cs.add(c);

		store.createView(workspace, cs, true);

		// 2. Handle MAP clauses (src->dst mapping)
		Predicate p1 = new Predicate("SMETA_" + Config.relname_node);
		p1.setArgNames("src", "dst");
		p1.setTypes(Type.Integer, Type.Integer);
		store.createSchema(workspace, p1);
		
		for(Map.Entry e : tr.getMapMap().entrySet()) {
			Atom a = (Atom)e.getKey();
            HashSet<String> srcSet = (HashSet<String>)e.getValue();
            String label = Util.removeQuotes(a.getTerms().get(1).toString());

            if (nodeLabelToIdMap.containsKey(label) == false) {
				nodeLabelToIdMap.put(label, nodeLabelId);
				nodeLabelId++;
			}
            var varId = nodeLabelToIdMap.get(label);

			ArrayList<SimpleTerm> t = new ArrayList<SimpleTerm>();
			t.add(new LongSimpleTerm(varId));
			t.add(new StringSimpleTerm(label));
			store.addTuple("SV_" + Config.relname_node, t);
			
            for (String src : srcSet) {
            	String srcLabel = Util.removeQuotes(varToLabelMap.get(src));
            	if (nodeLabelToIdMap.containsKey(srcLabel) == false) {
            		throw new IllegalArgumentException("srcLabel: " + srcLabel + " is not in nodeLabelToIdMap: " + nodeLabelToIdMap);
            	}
            	int srcId = nodeLabelToIdMap.get(srcLabel);

            	t = new ArrayList<SimpleTerm>();
    			t.add(new LongSimpleTerm(srcId));
    			t.add(new LongSimpleTerm(varId));
    			store.addTuple("SMETA_" + Config.relname_node, t);
            }
        }

		// 3. Populate SchemaView Graph (Edge Reconnection rule)
		cs = new ArrayList<DatalogClause>();
		
		c = new DatalogClause();
		c.setHead(new Atom("SV_E", "e", "dst", "t", "l"));
		c.addAtomToBody(new Atom("SV_E", "e", "src", "t", "l"));
		c.addAtomToBody(new Atom("SMETA_N", "src", "dst"));
		c.addAtomToBody(new Atom(false, "SMETA_N", "t", "_"));
		cs.add(c); // incoming

		c = new DatalogClause();
		c.setHead(new Atom("SV_E", "e", "f", "dst", "l"));
		c.addAtomToBody(new Atom("SV_E", "e", "f", "src", "l"));
		c.addAtomToBody(new Atom("SMETA_N", "src", "dst"));
		c.addAtomToBody(new Atom(false, "SMETA_N", "f", "_"));
		cs.add(c); // outgoing

		c = new DatalogClause();
		c.setHead(new Atom("SV_E", "e", "dst", "dst2", "l"));
		c.addAtomToBody(new Atom("SV_E", "e", "src", "src2", "l"));
		c.addAtomToBody(new Atom("SMETA_N", "src", "dst"));
		c.addAtomToBody(new Atom("SMETA_N", "src2", "dst2"));
		cs.add(c); // self-loop
		
//		System.out.println("[populateTransformedSchemaGraph] *******cs[cnt: " + cs.size() + " => " + cs);
		store.createView(workspace, cs, true);
	}
	
	private static void populateSSRSchemaGraph(TransRule tr, String viewName) {
		ArrayList<Atom> atoms = tr.getPatternAfterForIndexing();
		
		
		ArrayList<DatalogClause> cs = new ArrayList<DatalogClause>();
		DatalogClause c = new DatalogClause();
		Atom h = new Atom("SSR_N");
		h.appendTerm(new Term("n", true));
		h.appendTerm(new Term("l", true));
		Atom b = new Atom("N_" + viewName);
		b.appendTerm(new Term("n", true));
		b.appendTerm(new Term("l", true));
		c.addAtomToHeads(h);
		c.addAtomToBody(b);
		cs.add(c);
		
		c = new DatalogClause();
		h = new Atom("SSR_E");
		h.appendTerm(new Term("e", true));
		h.appendTerm(new Term("f", true));
		h.appendTerm(new Term("t", true));
		h.appendTerm(new Term("l", true));
		b = new Atom("E_" + viewName);
		b.appendTerm(new Term("e", true));
		b.appendTerm(new Term("f", true));
		b.appendTerm(new Term("t", true));
		b.appendTerm(new Term("l", true));
		c.addAtomToHeads(h);
		c.addAtomToBody(b);
		cs.add(c);
		
		store.createView(workspace, cs, true);
	}
	
	public static ArrayList<String> getHeadVarsProjected(HashSet<String> headVars, HashSet<String> bodyVars) {
		ArrayList<String> vars = new ArrayList<String>();
		
		for (String var : headVars) {
			if (bodyVars.contains(var) == true) {
				vars.add(var);
			} else {
				vars.add("_");
			}
		}
		return vars;
	}
	
	
	public static boolean testCoverednessOnSchemas(ArrayList<Atom> p) {
		ArrayList<String> relPrefixes = new ArrayList<String>();
		ArrayList<Integer> rowsList = new ArrayList<Integer>();
		relPrefixes.add("SSR");
		relPrefixes.add("SV");
		
//		store.debug();
//		System.out.println("[testCoverednessOnSchemas] p: " + p);
		
		for (String relPrefix : relPrefixes) {
			DatalogClause c = new DatalogClause();
			HashSet<String> headVars = new LinkedHashSet<String>();
			for (Atom a : p) {
				Atom b = null;
				try {
					b = (Atom)a.clone();
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (b.getRelName().startsWith(Config.relname_node) == true) {
					b.getPredicate().setRelName(relPrefix + "_N");
				} else if (b.getRelName().startsWith(Config.relname_edge) == true) {
					b.getPredicate().setRelName(relPrefix + "_E");
				}
				c.addAtomToBody(b);
				headVars.addAll(a.getVars());
			}
			
			Atom h = new Atom("_");
			for (String v : headVars) {
				h.appendTerm(new Term(v, true));
			}
			c.addAtomToHeads(h);
			
//			System.out.println("c: " + c);
//			System.out.println("c: " + c);
//			store.debug();
			
			StoreResultSet rs = store.getQueryResult(c);
//			System.out.println("rs: " + rs.getResultSet());
			rowsList.add(rs.getResultSet().size());
		}
//		System.out.println("[testCoverednessOnSchemas] rowsList: " + rowsList);
		
//		store.debug();
		return rowsList.get(0) == rowsList.get(1);
	}
}
