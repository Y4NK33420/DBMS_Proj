package edu.upenn.cis.db.graphtrans.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;
import com.microsoft.z3.Params;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.Egd;
import edu.upenn.cis.db.graphtrans.datastructure.TransRule;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.helper.Util;

/**
 * Rule Overlap Check. 
 * @author soonbo
 *
 */
public class RuleOverlapCheck {
	final static Logger logger = LogManager.getLogger(RuleOverlapCheck.class);

	private static Context ctx = null;
	private static FuncDecl relN = null;
	private static FuncDecl relE = null;
	private static FuncDecl relNP = null;
	private static FuncDecl relEP = null;
	private static FuncDecl relNCOLOR = null;
	private static FuncDecl relECOLOR = null;
	private static Fixedpoint fp = null;
	private static Solver solver = null;
	private static String viewName = null;

	final private static String prefixOfVarForViolationCheck = "_b"; 
//	private static ArrayList<Triple<Integer,Integer,Integer>> rulePairsList; // (v1,0) vs (v2,v3)
	private static ArrayList<Pair<Integer,Integer>> newRulePairsList;

	private static HashMap<Integer, HashSet<Integer>> ruleEgdsMap; // (ruleId,subType) |-> {egds}

	private static ArrayList<LinkedHashSet<Integer>> coloredNodeIds = null;
	private static ArrayList<LinkedHashSet<Integer>> coloredEdgeIds = null;
	
	/**
	 * Add rules to declare relations for N, E, NP, and EP.
	 */
	private static void addRelationDecl() {
		Sort[] domainForN = new Sort[] {ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForE = new Sort[] {ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForNP = new Sort[] {ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForEP = new Sort[] {ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForNCOLOR = new Sort[] {ctx.getIntSort()};
		Sort[] domainForECOLOR = new Sort[] {ctx.getIntSort()};		
		Sort range = ctx.getBoolSort();

		relN = ctx.mkFuncDecl("N", domainForN, range);
		relE = ctx.mkFuncDecl("E", domainForE, range);
		relNP = ctx.mkFuncDecl("NP", domainForNP, range);
		relEP = ctx.mkFuncDecl("EP", domainForEP, range);
		relNCOLOR = ctx.mkFuncDecl("NCOLOR", domainForNCOLOR, range);
		relECOLOR = ctx.mkFuncDecl("ECOLOR", domainForECOLOR, range);		

		fp.registerRelation(relN);
		fp.registerRelation(relE);
		fp.registerRelation(relNP);
		fp.registerRelation(relEP);
		fp.registerRelation(relNCOLOR);
		fp.registerRelation(relECOLOR);
	}

	/**
	 * Add assertions to the solver that express EGDs 
	 * stating that the two nodes (or edges) having the same id also have the same label.
	 */
	private static void addDefaultEgds() {
		Expr[] varN = new Expr[] {
				ctx.mkConst("a", relN.getDomain()[0]),
				ctx.mkConst("t1", relN.getDomain()[1]),
				ctx.mkConst("t2", relN.getDomain()[1])
		};
		Expr exprN = ctx.mkImplies(ctx.mkAnd((BoolExpr)relN.apply(varN[0], varN[1]),
				(BoolExpr)relN.apply(varN[0], varN[2])), ctx.mkEq(varN[1], varN[2]));
		Quantifier qN = ctx.mkForall(varN, exprN, 1, null, null, null, null);
		fp.addRule(qN, ctx.mkSymbol("UniqueNode"));

		Expr[] varE = new Expr[] {
				ctx.mkConst("e", relE.getDomain()[0]),
				ctx.mkConst("a1", relE.getDomain()[1]),
				ctx.mkConst("b1", relE.getDomain()[2]),
				ctx.mkConst("t1", relE.getDomain()[3]),
				ctx.mkConst("a2", relE.getDomain()[1]),
				ctx.mkConst("b2", relE.getDomain()[2]),
				ctx.mkConst("t2", relE.getDomain()[3])
		};
		Expr exprE = ctx.mkImplies(ctx.mkAnd((BoolExpr)relE.apply(varE[0], varE[1], varE[2], varE[3]),
				(BoolExpr)relE.apply(varE[0], varE[4], varE[5], varE[6])), 
				ctx.mkAnd(ctx.mkEq(varE[1], varE[4]), ctx.mkEq(varE[2], varE[5]), ctx.mkEq(varE[3], varE[6])));
		Quantifier qE = ctx.mkForall(varE, exprE, 1, null, null, null, null);

		fp.addRule(qE, ctx.mkSymbol("UniqueEdge"));
	}

	private static FuncDecl getFuncDeclFromRelName(String relName) {
		FuncDecl rel = null;
		if (relName.contentEquals(Config.relname_node) == true) {
			rel = relN;
		} else if (relName.contentEquals(Config.relname_edge) == true) {
			rel = relE;
		} else if (relName.contentEquals(Config.relname_nodeprop) == true) {
			rel = relNP;
		} else if (relName.contentEquals(Config.relname_edgeprop) == true) {
			rel = relEP;
		} else if (relName.contentEquals("NCOLOR") == true) {
			rel = relNCOLOR;
		} else if (relName.contentEquals("ECOLOR") == true) {
			rel = relECOLOR;
		}
		return rel;
	}

	/** 
	 * Create constraints from EGDs.
	 * @param egd
	 */
	private static Quantifier getEgd(Egd egd) {
		HashMap<String, Expr> exprMap = new HashMap<String, Expr>();

		BoolExpr lhsExprs = null;
		for (Atom a : egd.getLhs()) {
			FuncDecl rel = getFuncDeclFromRelName(a.getPredicate().getRelName());			
			Expr[] exprs = new Expr[a.getTerms().size()];
			for (int i = 0; i < a.getTerms().size(); i++) {
				String val = a.getTerms().get(i).toString();
				if (rel.getDomain()[i].equals(ctx.getIntSort())) {
					if (a.getTerms().get(i).isVariable() == true) {
						if (exprMap.containsKey(val) == false) {
							exprMap.put(val, ctx.mkIntConst(val));
						}
						exprs[i] = exprMap.get(val);
					} else {
						exprs[i] = ctx.mkInt(SMTConstraint.getLabelId(Util.removeQuotes(val)));
						//throw new IllegalArgumentException("[ERROR] Constant int is not supported yet.");
					}
				} else if (rel.getDomain()[i].equals(ctx.getStringSort())) {
					if (a.getTerms().get(i).isVariable() == true) {
						if (exprMap.containsKey(val) == false) {
							exprMap.put(val, ctx.mkConst(val, ctx.getStringSort()));
						}
						exprs[i] = exprMap.get(val);
					} else {
						exprs[i] = ctx.mkInt(SMTConstraint.getLabelId(Util.removeQuotes(a.getTerms().get(i).toString())));
						//exprs[i] = ctx.mkString(Util.removeQuotes(a.getTerms().get(i).toString()));
					}
				} else {
					throw new IllegalArgumentException("[ERROR] Neither integer nor string is not supported.");
				}
			}
			BoolExpr bExpr = (BoolExpr)rel.apply(exprs);
			if (lhsExprs == null) {
				lhsExprs = bExpr;
			} else {
				lhsExprs = ctx.mkAnd(bExpr, lhsExprs);
			}
		}

		BoolExpr rhsExprs = null;
		for (Atom a : egd.getRhs()) {
			if (a.isInterpreted() == false) {
				throw new IllegalArgumentException("[ERROR] RHS should be a set of equality predicates.");
			} else {
				Expr[] exprs = new Expr[a.getTerms().size()];
				for (int i = 0; i < a.getTerms().size(); i++) {
					String val = a.getTerms().get(i).toString();
					if (a.getTerms().get(i).isVariable() == true) {
						exprs[i] = exprMap.get(val);
					} else {
						int labelId = SMTConstraint.getLabelId(Util.removeQuotes(val));
						exprs[i] = ctx.mkInt(labelId);
					}
				}
				BoolExpr bExpr = (BoolExpr)ctx.mkEq(exprs[0], exprs[1]);				

				if (rhsExprs == null) {
					rhsExprs = bExpr;
				} else {
					rhsExprs = ctx.mkAnd(bExpr, rhsExprs);
				}
			}
		}
		BoolExpr bExpr = ctx.mkImplies(lhsExprs, rhsExprs);

		Expr[] exprs = exprMap.values().toArray(new Expr[exprMap.values().size()]);
		Quantifier q = ctx.mkForall(exprs, bExpr, 1, null, null, null, null);

		return q;
	}

	private static void addRulesForTransRules() {	
		BoolExpr bExpr = null;
		TransRuleList transRules = GraphTransServer.getTransRuleList(viewName);
		
		logger.info("[addRulesForTransRules] newRulePairsList: " + newRulePairsList.size());
		
//		System.out.println("transRules: " + transRules);
		
//		for (int i = 0; i < transRules.getTransRuleList().size(); i++) {
//			TransRule tr = transRules.getTransRuleList().get(i);
//			System.out.println("[RuleOverlapCheck] tr.match: " + tr.getPatternMatch());
//			System.out.println("[RuleOverlapCheck] tr.affectedVars: " + tr.getAffectedVariables());
//			System.out.println("[RuleOverlapCheck] tr.before: " + tr.getPatternBefore());
//			System.out.println("[RuleOverlapCheck] tr.affected: " + tr.getPatternAffected());
//		}
//		System.exit(0);
		
		int rulePairId = 0;
		for (Pair<Integer, Integer> t : newRulePairsList) {		
			coloredNodeIds = new ArrayList<LinkedHashSet<Integer>>();
			coloredEdgeIds = new ArrayList<LinkedHashSet<Integer>>();
			
			ArrayList<ArrayList<Atom>> patterns = new ArrayList<ArrayList<Atom>>();
			ArrayList<HashSet<String>> affectedVariables = new ArrayList<HashSet<String>>();
			
			patterns.add(transRules.getTransRule(t.getLeft()).getPatternBefore());
			patterns.add(transRules.getTransRule(t.getRight()).getPatternBefore());

//			System.out.println("[RuleOverlapCheck] rulePairId" + rulePairId + " t: " + t + " patterns: " + patterns);
			
			affectedVariables.add(transRules.getTransRule(t.getLeft()).getAffectedVariables());
			affectedVariables.add(transRules.getTransRule(t.getRight()).getAffectedVariables());
			
			BoolExpr bExprSub = getRuleForTwoTransRules(patterns, null, affectedVariables);
			bExprSub = ctx.mkAnd(bExprSub, ctx.mkEq(ctx.mkBoolConst(prefixOfVarForViolationCheck + rulePairId++), ctx.mkBool(true))); // to pick a violation case
			bExpr = (bExpr == null) ? bExprSub : ctx.mkOr(bExpr, bExprSub);			
			

			FuncDecl relNColor = getFuncDeclFromRelName("NCOLOR");
			for (int c : coloredNodeIds.get(1)) {
				Expr exprColor = ctx.mkIntConst("n" + c);
				bExpr = ctx.mkAnd(bExpr, ctx.mkNot((BoolExpr)relNColor.apply(exprColor)));
			}

		}
//		System.out.println("[RuleOverlapCheck] bExpr: " + bExpr);

		fp.addRule(bExpr, ctx.mkSymbol("transrules"));

		bExpr = null;
		for (int i = 0; i < SMTConstraint.maxNodeId; i++) {
			BoolExpr bExprSub = ctx.mkGe(ctx.mkIntConst("n"+i), ctx.mkInt(0));
			bExpr = (bExpr == null) ? bExprSub : ctx.mkAnd(bExpr, bExprSub);
		}
		if (bExpr != null) {
			fp.addRule(bExpr, ctx.mkSymbol("positive node id"));
		}
		
		bExpr = null;
		for (int i = 0; i < SMTConstraint.maxEdgeId; i++) {
			BoolExpr bExprSub = ctx.mkGe(ctx.mkIntConst("e"+i), ctx.mkInt(0));
			bExpr = (bExpr == null) ? bExprSub : ctx.mkAnd(bExpr, bExprSub);
		}
		if (bExpr != null) {
			fp.addRule(bExpr, ctx.mkSymbol("positive edge id"));
		}

		bExpr = null;
		for (int i = 0; i < SMTConstraint.maxNodeId; i++) {
			for (int j = i + 1; j < SMTConstraint.maxNodeId; j++) {
				BoolExpr bExprSub = ctx.mkNot(ctx.mkEq(ctx.mkIntConst("n"+i), ctx.mkIntConst("n"+j)));
				bExpr = (bExpr == null) ? bExprSub : ctx.mkAnd(bExpr, bExprSub);				
			}
		}
		if (bExpr != null) {
			fp.addRule(bExpr, ctx.mkSymbol("positive edge id"));
		}

		bExpr = null;
		for (int i = 0; i < SMTConstraint.maxNodeId; i++) {
			if (coloredNodeIds.get(1).contains(i) == true) {
				BoolExpr bExprSub = ctx.mkAnd(
						ctx.mkLt(ctx.mkIntConst("n"+i),  ctx.mkIntConst("_N")), 
						ctx.mkGe(ctx.mkIntConst("_M"),  ctx.mkInt(i+1))
//						(BoolExpr)relNColor.apply(exprColor)
//						ctx.mkNot((BoolExpr)relNColor.apply(exprColor))						
				);
				bExpr = (bExpr == null) ? bExprSub : ctx.mkOr(bExpr, bExprSub);
			}
		}
		if (bExpr != null) {
			fp.addRule(bExpr, ctx.mkSymbol("exists overlapping node"));
		}
	}

	private static BoolExpr getRuleForTwoTransRules(ArrayList<ArrayList<Atom>> atomList, HashSet<Integer> egds, ArrayList<HashSet<String>> affectedVariables) {
		int numOfLabelVars = 0;
		int numOfNodeVars = 0;

		BoolExpr bExpr = null;

		for (int i = 0; i < 2; i++) {
			HashSet<String> affectedVars = affectedVariables.get(i);
			numOfNodeVars = 0;
			int numOfEdgeVars = 0;

			HashMap<String, Integer> nodeVarToIdMap = new HashMap<String, Integer>();
			HashMap<String, Integer> edgeVarToIdMap = new HashMap<String, Integer>();
			HashMap<String, Integer> labelVarToIdMap = new HashMap<String, Integer>();

			ArrayList<Atom> atoms = atomList.get(i);
			
//			System.out.println("[RuleOverlapCheck] pattern i: " + i + " => " + atoms);

			BoolExpr bExprSub = null;

			for (int j = 0; j < atoms.size(); j++) {
				Atom a = atoms.get(j);

				coloredNodeIds.add(new LinkedHashSet<Integer>());
				coloredEdgeIds.add(new LinkedHashSet<Integer>());
				
				FuncDecl rel = getFuncDeclFromRelName(a.getPredicate().getRelName());
				Expr[] exprs = new Expr[rel.getDomainSize()];

				for (int k = 0; k < a.getTerms().size(); k++) {
					if (a.getTerms().get(k).isVariable() == true) { // variable
						if (a.getPredicate().equals(Config.predE) == true && k == 0) { // edge var
							if (edgeVarToIdMap.containsKey(a.getTerms().get(k).toString()) == false) { // always true
								edgeVarToIdMap.put(a.getTerms().get(k).toString(), numOfEdgeVars++);
							}
							int edgeId = edgeVarToIdMap.get(a.getTerms().get(k).toString());
							exprs[k] = (i == 0) ? ctx.mkInt(edgeId) : ctx.mkIntConst("e" + edgeId);
							if (affectedVars.contains(a.getTerms().get(k).toString()) == true) {
								coloredEdgeIds.get(i).add(edgeId);
							}
						} else if (k + 1 == a.getTerms().size()) { // label var
							if (labelVarToIdMap.containsKey(a.getTerms().get(k).toString()) == false) {
								labelVarToIdMap.put(a.getTerms().get(k).toString(), numOfLabelVars++);
							}
							int labelVarId = labelVarToIdMap.get(a.getTerms().get(k).toString());
							exprs[k] = (i == 0) ? ctx.mkString(a.getTerms().get(k).toString()) : ctx.mkConst("l" + labelVarId, ctx.getStringSort());
						} else { // node var
							if (nodeVarToIdMap.containsKey(a.getTerms().get(k).toString()) == false) {
								nodeVarToIdMap.put(a.getTerms().get(k).toString(), numOfNodeVars++);
							}
							int nodeId = nodeVarToIdMap.get(a.getTerms().get(k).toString());
							exprs[k] = (i == 0) ? ctx.mkInt(nodeId) : ctx.mkIntConst("n" + nodeId);
							if (affectedVars.contains(a.getTerms().get(k).toString()) == true) {
								coloredNodeIds.get(i).add(nodeId);
							}
						}
					} else { // constant
						SMTConstraint.setLabelId(a.getTerms().get(k).toString());
						//exprs[k] = ctx.mkString(Util.removeQuotes(a.getTerms().get(k).toString()));
						exprs[k] = ctx.mkInt(SMTConstraint.getLabelId(Util.removeQuotes(a.getTerms().get(k).toString())));
					}
					if (k + 1 == a.getTerms().size()) {
						if (bExprSub == null) {
							bExprSub = (BoolExpr)rel.apply(exprs);
						} else {
							bExprSub = ctx.mkAnd(bExprSub, (BoolExpr)rel.apply(exprs));
						}
					}
				}
			}

			if (bExpr == null) {
				bExpr = bExprSub;
			} else {
				if (bExprSub != null) {
					bExpr = ctx.mkAnd(bExpr, bExprSub);
				}
			}
			
			// add ids to COLOR rel
			if (i == 0) {
				FuncDecl relColor = getFuncDeclFromRelName("NCOLOR");
				for (int id : coloredNodeIds.get(0)) {
					Expr exprColor = ctx.mkInt(id);
					if (bExpr == null) {
						bExpr = (BoolExpr)relColor.apply(exprColor);
					} else {
						bExpr = ctx.mkAnd(bExpr, (BoolExpr)relColor.apply(exprColor));
					}
				}
				relColor = getFuncDeclFromRelName("ECOLOR");
				for (int id : coloredEdgeIds.get(0)) {
					Expr exprColor = ctx.mkInt(id);
					if (bExpr == null) {
						bExpr = (BoolExpr)relColor.apply(exprColor);
					} else {
						bExpr = ctx.mkAnd(bExpr, (BoolExpr)relColor.apply(exprColor));
					}
				}
			}
		
			if (i == 0) {
				bExpr = ctx.mkAnd(bExpr, ctx.mkEq(ctx.mkIntConst("_N"), ctx.mkInt(numOfNodeVars)));
			} else {
				bExpr = ctx.mkAnd(bExpr, ctx.mkEq(ctx.mkIntConst("_M"), ctx.mkInt(numOfNodeVars)));				

				SMTConstraint.compareAndSetMaxNodeId(numOfNodeVars);
				SMTConstraint.compareAndSetMaxEdgeId(numOfEdgeVars);	
			}
			bExpr = ctx.mkAnd(bExpr, ctx.mkEq(ctx.mkIntConst("RID"+i), ctx.mkInt(0)));
		}
//		System.out.println("2rules: " + bExpr);
		return bExpr;
	}

	private static void populateRelatedRulesWithoutPruning() {
		newRulePairsList = new ArrayList<>();		
		TransRuleList transRules = GraphTransServer.getTransRuleList(viewName);
		
		for (int i = 0; i < transRules.getNumTransRuleList(); i++) {
			for (int j = i; j < transRules.getNumTransRuleList(); j++) {
//				if ( i==1&&j==1) {
					newRulePairsList.add(Pair.of(i, j));
//				}
			}
		}	
		
		System.out.println("[RuleOverlapCheck # of Rules: " + transRules.getNumTransRuleList() + " newRulePairsList.size(): " + newRulePairsList.size());
//		System.out.println("[RuleOverlapCheck newRulePairsList: " + newRulePairsList);
	}

	private static void populateRelatedRulesWithPruning() {
		ruleEgdsMap = new HashMap<>();
		newRulePairsList = new ArrayList<>();

//		if (Config.useTypeCheckSimplePruner() == true) {
		RuleOverlapCheckSimplePruner.prune(viewName, newRulePairsList, ruleEgdsMap);
		System.out.println("====> With Simple Pruning...");
//		} else {
//			System.out.println("====> With Old Pruning...");
//			RuleOverlapCheckPruner.prune(viewName, newRulePairsList, ruleEgdsMap);
//		}
	}

	private static void addRelatedEgds() {
		ArrayList<Egd> egdList = GraphTransServer.getEgdList();
		HashSet<Egd> egds = new HashSet<Egd>();
		if (Config.isTypeCheckPruningEnabled() == true) {
			HashSet<Integer> egdIds = new HashSet<Integer>();
			for (int ruleId : ruleEgdsMap.keySet()) {
				egdIds.addAll(ruleEgdsMap.get(ruleId));
			}
			for (int egdId : egdIds) {
				egds.add(egdList.get(egdId));
			}
		} else {
			egds.addAll(egdList);
		}		
		SMTConstraint.initialize();
		for (Egd e : egds) {
			Quantifier q = getEgd(e);
			fp.addRule(q, ctx.mkSymbol("Egd"));
		}		
	}
	
	private static void initializeSMTSolver() {
		HashMap<String, String> cfg = new HashMap<>();
		cfg.put("smtlib2_compliant",  "true");
		ctx = new Context(cfg);
		solver = ctx.mkSolver();    
		fp = ctx.mkFixedpoint();

		Params p = ctx.mkParams();
		p.add("engine", "datalog");
		p.add("print_fixedpoint_extensions", true);
		fp.setParameters(p);		
	}
	
	/**
	 * Statically check the well-behavedness of the set of transrules under EGDs.
	 * If the constraints to the SMT solver is unsatisfiable, it is well-behaved 
	 * as no rule pairs can interfere with each other. 
	 * @return true if it is well-behaved, false, otherwise.
	 */
	public static boolean check(String name) {
		viewName = name;
		
		if (Config.isTypeCheckEnabled() == false) {
			return true;
		}
		
		String OS = System.getProperty("os.name").toLowerCase();

		if (OS.startsWith("win") == true) {
			System.out.println("[RuleOverlapCheck] Windows - don't support Z3 yet.");
			return true;
		}

		// FIXME: if there exists starVars, throw exception!
		// ...
		
		int tc = Util.startTimer();

		System.out.println("[RuleOverlapCheck] DDDDDDDDDDDDDDDDDDDDDDDDDDD");

		if (Config.isTypeCheckPruningEnabled() == true) {
			populateRelatedRulesWithPruning();
		} else {
			populateRelatedRulesWithoutPruning();
		}
//    	System.out.println("[check] Elapsed time tc: " + Util.getElapsedTime(tc) + " pruning: " + Config.isTypeCheckPruningEnabled() + " rulePairList: " + rulePairsList.size() );
	
    	if (newRulePairsList.size() == 0) {
    		return true;
    	}
		initializeSMTSolver();
		addRelationDecl();
		addDefaultEgds();
		addRelatedEgds();
		addRulesForTransRules();
//    	Util.Console.logln("[check] Elapsed time tc2: " + Util.getElapsedTime(tc));
    	
		solver.add(fp.getRules());
//		System.out.println("[RuleOverlapCheck] final string: [\n" + solver.toString() + "\n]");
    	Util.Console.logln("[check] Elapsed time tc3: " + Util.getElapsedTime(tc));

		Status st = solver.check();
    	Util.Console.logln("[check] Elapsed time tc4: " + Util.getElapsedTime(tc));

//		System.out.println("[RuleOverlapCheck] result: " + fp.getAssertions());
		
		if (st == Status.SATISFIABLE) {
			Model m = solver.getModel();
//			System.out.println("m: " + m);
			for (FuncDecl f : m.getConstDecls()) {
				String var = f.getName().toString();
//				System.out.println("m.const: " + f);
//				System.out.println("m.constName: " + var);
//				System.out.println("m.constinterp: " + m.getConstInterp(f));
//				
				if (f.getRange().equals(ctx.getBoolSort())) {
					if (var.startsWith(prefixOfVarForViolationCheck) == true) {
						int varId = Integer.parseInt(var.substring(prefixOfVarForViolationCheck.length()));
						Pair<Integer, Integer> t = newRulePairsList.get(varId);
						TransRuleList transRules = GraphTransServer.getTransRuleList(viewName);

						System.out.println("=== Violation example ===");
						System.out.println("Different order of execution of following two rules may results in different output graph.");
						System.out.println("> rule[" + t.getLeft() + "] => " + transRules.getTransRule(t.getLeft()).getQuery());
						System.out.println("> rule[" + t.getRight() + "] => " + transRules.getTransRule(t.getRight()).getQuery());

						System.out.println(">> The match pattern of rule[" + t.getLeft() + "] overlaps with the pattern by rule[" + t.getRight() + "].");
					}
				}
			}
		} 
		
//		SMTConstraint.show(); 

//    	Util.Console.logln("[check] Elapsed time tc4: " + Util.getElapsedTime(tc));

		return (st == Status.UNSATISFIABLE);
	}
}
