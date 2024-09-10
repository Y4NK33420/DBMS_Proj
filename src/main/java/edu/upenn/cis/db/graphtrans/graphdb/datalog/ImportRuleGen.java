package edu.upenn.cis.db.graphtrans.graphdb.datalog;

import edu.upenn.cis.db.graphtrans.Config;

public class ImportRuleGen {
	public static String getRule(String relName, String filePath) {
		// TODO:
		String rule = "// Import form CSV\n";
		String targetRel = relName + Config.relname_base_postfix;
		String inRel = "_in";
		
		String subrule = "";
		subrule += "lang:physical:filePath[`_in] = \"" + filePath + "\".\n";
		subrule += "lang:physical:fileMode[`_in] = \"import\".\n";

		if (relName.contentEquals(Config.relname_node)) {
			rule += inRel + "(offset; id, label) -> int(offset), int(id), string(label).\n";
			rule += subrule;
			rule += "+" + targetRel + "(id, label) <- " + inRel + "(_; id, label).\n";
		} else if (relName.contentEquals(Config.relname_edge)) {
			rule += inRel + "(offset; id, from, to, label) -> int(offset), int(id), int(from), int(to), string(label).\n";
			rule += subrule;
			rule += "+" + targetRel + "(id, from, to, label) <- " + inRel + "(_; id, from, to, label).\n";
		} else {
			rule += inRel + "(offset; id, property, value) -> int(offset), int(id), string(property), string(value).\n";
			rule += subrule;
			rule += "+" + targetRel + "(id, property, value) <- " + inRel + "(_; id, property, value).\n";
		}
		
//		System.out.println("[ImportRuleGen] rule: " + rule);
		return rule;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		 System.out.println(ImportRuleGen.getRule("N", "/a/b/c.csv"));
		 System.out.println(ImportRuleGen.getRule("E", "/a/b/c.csv"));
		 System.out.println(ImportRuleGen.getRule("NP", "/a/b/c.csv"));
		 System.out.println(ImportRuleGen.getRule("EP", "/a/b/c.csv"));
	}
}
