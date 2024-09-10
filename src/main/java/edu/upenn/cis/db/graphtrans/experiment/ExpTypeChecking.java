package edu.upenn.cis.db.graphtrans.experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.Console;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.helper.Performance;
import edu.upenn.cis.db.helper.Util;

public class ExpTypeChecking {
	private static ArrayList<String> rules;// = new ArrayList<String>();

	private static String[] baseSchema = {
		"create node A$1$",
		"create node B$1$",
		"create node C$2$",
		"create node D$2$",		
		"create node E$4$",
		"create node C$3$",
		"create node B$4$",
		"create edge X (A$1$ -> B$1$)",
		"create edge Y (B$1$ -> C$2$)",
		"create edge Z (C$2$ -> D$2$)",
		"create edge X (C$3$ -> E$4$)",
		"create edge Y (E$4$ -> B$4$)",
	};

	public static String[]  baseConstraints = {
		"add constraint N(b,\"C$2$\"),N(c1,l1),N(c2,l2),E(e1,b,c1,l3),E(e2,b,c2,l4) -> c1=c2, l1=l2, e1=e2, l3=l4",
		"add constraint N(b,\"C$2$\"),N(c1,l1),N(c2,l2),E(e1,c1,b,l3),E(e2,c2,b,l4) -> c1=c2, l1=l2, e1=e2, l3=l4",
	};

	public static String[]  baseConstraints2 = {
			"add constraint N(b,l),N(c1,l1),N(c2,l2),E(e1,b,c1,l3),E(e2,b,c2,l4) -> c1=c2, l1=l2, e1=e2, l3=l4",
			"add constraint N(b,l),N(c1,l1),N(c2,l2),E(e1,c1,b,l3),E(e2,c2,b,l4) -> c1=c2, l1=l2, e1=e2, l3=l4",
		};
	private static String[] baseTransRules = {
		"match a:A$1$-x:X->b:B$1$, b-y:Y->c:C$2$, c-z:Z->d:D$2$ map (c,d) to s:S$2$",
		"match e:C$3$-x:X->f:E$4$, f-y:Y->g:B$4$ map (e,f) to t:T$3$",
	};
	
	public static void addConstraints(int ruleNumFactor, int pruningFactor) {
		// add edgs
	}
	
	public static String replaceRule(boolean overlapping, String rule, int index) {
		String replacedRule;
		if (overlapping == false) {
			replacedRule = rule.replace("$1$", "_"+ Integer.toString(index))
					.replace("$2$", "_"+ Integer.toString(index))
					.replace("$3$", "_"+ Integer.toString(index))
					.replace("$4$", "_"+ Integer.toString(index)+"_");
		} else {
			replacedRule = rule.replace("$1$", Integer.toString(index))
					.replace("$2$", "")
					.replace("$3$", "")
					.replace("$4$", Integer.toString(index));
		}
		return replacedRule;
	}
	
	public static void addView(int ruleNumFactor, int pruningFactor) {
		HashSet<String> schemas = new LinkedHashSet<String>();
		HashSet<String> constraints = new LinkedHashSet<String>();
		ArrayList<String> views = new ArrayList<String>();

		for (int i = 1; i <= Math.pow(2, ruleNumFactor); i++) {
			for (int j = 0; j < baseSchema.length; j++) {
				schemas.add(replaceRule((i > Math.pow(2, pruningFactor)), baseSchema[j], i));
			}
			if (i == 1) {
				constraints.addAll(Arrays.asList(baseConstraints2));
			}
//			for (int j = 0; j < baseConstraints.length; j++) {
//				constraints.add(replaceRule((i > pruningFactor), baseConstraints[j], i));
//			}
			for (int j = 0; j < baseTransRules.length; j++) {
				views.add(replaceRule((i > Math.pow(2, pruningFactor)), baseTransRules[j], i));
			}
		}
		for (String s : schemas) {
			rules.add(s);
		}
		for (String c : constraints) {
			rules.add(c);
		}
		String view = "create virtual view v0 as ";
		for (int k = 0; k < views.size(); k++) {
//			System.out.println("views k: " + k + " => " + views.get(k));
			view = view + "{" + views.get(k) + "}";
			if (k + 1 < views.size()) {
				view += ", ";
			} else {
				view += "";
			}
		}
		rules.add(view);
		
		// add virtual view
	}
	
	public static ArrayList<String> getRules() {
		return rules;
	}
	
	public static void addRulesForTypeCheck(int ruleNumFactor, int pruningFactor, boolean pruning) {
		rules = new ArrayList<String>();
		System.out.println("[addRulesForTypeCheck] ruleNumFactor: " + ruleNumFactor + " pruningFactor: " + pruningFactor);
		rules.add("# options");
		rules.add("option typecheck on");
		rules.add("option prunetypecheck " + (pruning == true ? "on" : "off") + "");
		rules.add("option prunequery off");
		rules.add("# init");
		rules.add("connect sd");
		rules.add("create graph exp");
		rules.add("use exp");
		rules.add("# schema, constraints, and views");
		addView(ruleNumFactor, pruningFactor);
		rules.add("# close");
		rules.add("drop exp");
		rules.add("disconnect");
		
	}
}
