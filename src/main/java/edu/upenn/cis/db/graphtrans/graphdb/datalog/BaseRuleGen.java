package edu.upenn.cis.db.graphtrans.graphdb.datalog;

import java.util.ArrayList;

import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Type;
import edu.upenn.cis.db.graphtrans.Config;

/**
 * BaseRuleGen. Create relations for the base graph and catalog.
 * @author sbnet21
 *
 */
public class BaseRuleGen {
	/**
	 * The type of ids is integer.
	 * @return
	 */
	private static ArrayList<Predicate> preds = new ArrayList<Predicate>();
	private static ArrayList<Predicate> predsLBOnly = new ArrayList<Predicate>();
	private static ArrayList<String> rulesLBOnly = new ArrayList<String>();
	
	public static ArrayList<Predicate> getPreds() {
		return preds;
	}
	
	public static ArrayList<Predicate> getPredsLBOnly() {
		return predsLBOnly;
	}
	
	public static ArrayList<String> getRulesLBOnly() {
		return rulesLBOnly;
	}
	
	public static void addSchemaRule() {
		// Define schema
		Predicate p1 = new Predicate(Config.relname_node_schema);
		p1.addArg("label", Type.String);
		
		Predicate p2 = new Predicate(Config.relname_edge_schema);
		p2.setArgNames("from", "to", "label");
		p2.setTypes(Type.String, Type.String, Type.String);
		
		Predicate p3 = new Predicate(Config.relname_egd);
		p3.addArg("constraint", Type.String);
		
		Predicate p4 = new Predicate(Config.relname_catalog_view);
		p4.setArgNames("name", "base", "type", "rule", "level");
		p4.setTypes(Type.String, Type.String, Type.String, Type.String, Type.Integer);
		
		Predicate p5 = new Predicate(Config.relname_catalog_index);
		p5.setArgNames("viewname", "type", "label");
		p5.setTypes(Type.String, Type.String, Type.String);

		Predicate p6 = new Predicate(Config.relname_catalog_sindex);
		p6.setArgNames("viewname", "query");
		p6.setTypes(Type.String, Type.String);

		preds.add(p1);
		preds.add(p2);
		preds.add(p3);
		preds.add(p4);
		preds.add(p5);
		preds.add(p6);
	}
	
	public static ArrayList<Predicate> getBaseGraphRuleBaseEDB(boolean isStringId) {		
		// Define base graph
		if (isStringId == true) {
			throw new UnsupportedOperationException("Only Integer IDs are supported.");
		}
		ArrayList<Predicate> preds = new ArrayList<Predicate>();
		
		Predicate p1 = new Predicate(Config.relname_node + Config.relname_base_postfix);
		p1.setArgNames("id", "label");
		p1.setTypes(Type.Integer, Type.String);
		
		Predicate p2 = new Predicate(Config.relname_edge + Config.relname_base_postfix);
		p2.setArgNames("id", "from", "to", "label");
		p2.setTypes(Type.Integer, Type.Integer, Type.Integer, Type.String);

		Predicate p3 = new Predicate(Config.relname_nodeprop + Config.relname_base_postfix);
		p3.setArgNames("id", "property", "value");
		p3.setTypes(Type.Integer, Type.String, Type.String);

		Predicate p4 = new Predicate(Config.relname_edgeprop + Config.relname_base_postfix);
		p4.setArgNames("id", "property", "value");
		p4.setTypes(Type.Integer, Type.String, Type.String);

		preds.add(p1);
		preds.add(p2);
		preds.add(p3);
		preds.add(p4);
		
		return preds;
	}
	
	public static void addBaseGraphRule(boolean isStringId) {	
		preds.addAll(getBaseGraphRuleBaseEDB(isStringId));
	}
	
	public static void addRule() {
		/**
		 * Create catalog relations for
		 *  - graph schema
		 *  - egds
		 *  - view
		 * Create relations for the base graph
		 * 	- N_g, E_g, NP_g, EP_g
		 */
		addSchemaRule();
		addBaseGraphRule(false);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		System.out.println(BaseRuleGen.getBaseGraphRuleBaseEDB(false));
		BaseRuleGen.addRule();
		
		System.out.println("preds: " + BaseRuleGen.getPreds());
		System.out.println("predsLBOnly: " + BaseRuleGen.getPredsLBOnly());
		System.out.println("rulesLBOnly: " + BaseRuleGen.getRulesLBOnly());
	}	
}