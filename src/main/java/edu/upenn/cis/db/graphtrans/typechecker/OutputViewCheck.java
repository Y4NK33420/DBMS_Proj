package edu.upenn.cis.db.graphtrans.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.FuncDecl;
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

public class OutputViewCheck {
	final static Logger logger = LogManager.getLogger(OutputViewCheck.class);

	private static Context ctx = null;
	private static HashMap<String, FuncDecl> funcDeclMaps;
	private static Fixedpoint fp = null;
	private static Solver solver = null;
	private static String viewName = null;
	private static boolean isBefore = true;

	private static void initializeSMTSolver() {
		SMTConstraint.initialize();

		funcDeclMaps = new HashMap<String, FuncDecl>();
		isBefore = true;
		
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
	 * Add rules to declare relations for N, E, NP, and EP.
	 */
	private static void addRelationDecl() {
		Sort[] domainForN = new Sort[] {ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForE = new Sort[] {ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForN1 = new Sort[] {ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForE1 = new Sort[] {ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForNP = new Sort[] {ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort()};
		Sort[] domainForEP = new Sort[] {ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort()};
		Sort range = ctx.getBoolSort();

		funcDeclMaps.put("N", ctx.mkFuncDecl("N", domainForN, range));
		funcDeclMaps.put("E", ctx.mkFuncDecl("E", domainForE, range));
		funcDeclMaps.put("N1", ctx.mkFuncDecl("N1", domainForN1, range));
		funcDeclMaps.put("E1", ctx.mkFuncDecl("E1", domainForE1, range));
		funcDeclMaps.put("NP", ctx.mkFuncDecl("NP", domainForNP, range));
		funcDeclMaps.put("EP", ctx.mkFuncDecl("EP", domainForEP, range));
		

		fp.registerRelation(funcDeclMaps.get("N"));
		fp.registerRelation(funcDeclMaps.get("E"));
		fp.registerRelation(funcDeclMaps.get("N1"));
		fp.registerRelation(funcDeclMaps.get("E1"));
		fp.registerRelation(funcDeclMaps.get("NP"));
		fp.registerRelation(funcDeclMaps.get("EP"));
	}
	
	/**
	 * Add assertions to the solver that express EGDs 
	 * stating that the two nodes (or edges) having the same id also have the same label.
	 */
	private static void addDefaultEgds() {
		String relN = (isBefore == true) ? "N" : "N1";
		String relE = (isBefore == true) ? "E" : "E1";
		Expr[] varN = new Expr[] {
				ctx.mkConst("a", funcDeclMaps.get(relN).getDomain()[0]),
				ctx.mkConst("t1", funcDeclMaps.get(relN).getDomain()[1]),
				ctx.mkConst("t2", funcDeclMaps.get(relN).getDomain()[1])
		};
		Expr exprN = ctx.mkImplies(ctx.mkAnd((BoolExpr)funcDeclMaps.get(relN).apply(varN[0], varN[1]),
				(BoolExpr)funcDeclMaps.get(relN).apply(varN[0], varN[2])), ctx.mkEq(varN[1], varN[2]));
		Quantifier qN = ctx.mkForall(varN, exprN, 1, null, null, null, null);
		fp.addRule(qN, ctx.mkSymbol("UniqueNode"));

		Expr[] varE = new Expr[] {
				ctx.mkConst("e", funcDeclMaps.get(relE).getDomain()[0]),
				ctx.mkConst("a1", funcDeclMaps.get(relE).getDomain()[1]),
				ctx.mkConst("b1", funcDeclMaps.get(relE).getDomain()[2]),
				ctx.mkConst("t1", funcDeclMaps.get(relE).getDomain()[3]),
				ctx.mkConst("a2", funcDeclMaps.get(relE).getDomain()[1]),
				ctx.mkConst("b2", funcDeclMaps.get(relE).getDomain()[2]),
				ctx.mkConst("t2", funcDeclMaps.get(relE).getDomain()[3])
		};
		Expr exprE = ctx.mkImplies(ctx.mkAnd((BoolExpr)funcDeclMaps.get(relE).apply(varE[0], varE[1], varE[2], varE[3]),
				(BoolExpr)funcDeclMaps.get(relE).apply(varE[0], varE[4], varE[5], varE[6])), 
				ctx.mkAnd(ctx.mkEq(varE[1], varE[4]), ctx.mkEq(varE[2], varE[5]), ctx.mkEq(varE[3], varE[6])));
		Quantifier qE = ctx.mkForall(varE, exprE, 1, null, null, null, null);

		fp.addRule(qE, ctx.mkSymbol("UniqueEdge"));
	}	
	
	private static void addRelatedEgds() {
		ArrayList<Egd> egdList = GraphTransServer.getEgdList();
		HashSet<Egd> egds = new HashSet<Egd>();
//		if (Config.isTypeCheckPruningEnabled() == true) {
//			HashSet<Integer> egdIds = new HashSet<Integer>();
//			for (Pair<Integer, Integer> ruleId : ruleEgdsMap.keySet()) {
//				egdIds.addAll(ruleEgdsMap.get(ruleId));
//			}
//			for (int egdId : egdIds) {
//				egds.add(egdList.get(egdId));
//			}
//		} else {
			egds.addAll(egdList);
//		}		
		BoolExpr egdOr = null;
		for (Egd e : egds) {
			Quantifier q = getEgd(e);
			
			if (isBefore == true) {
				fp.addRule(q, ctx.mkSymbol("Egd"));
			} else {
				if (egdOr == null) {
					egdOr = q;
				} else {
					egdOr = ctx.mkOr(egdOr, q);
				}
			}
		}		
		if (isBefore == false && egdOr != null) {
			fp.addRule(egdOr, ctx.mkSymbol("EgdOr"));
		}
	}
	
//	private static FuncDecl getFuncDeclFromRelName(String relName) {
//		FuncDecl rel = null;
//		if (relName.contentEquals(Config.relname_node) == true) {
//			rel = relN;
//		} else if (relName.contentEquals(Config.relname_edge) == true) {
//			rel = relE;
//		} else if (relName.contentEquals(Config.relname_nodeprop) == true) {
//			rel = relNP;
//		} else if (relName.contentEquals(Config.relname_edgeprop) == true) {
//			rel = relEP;
//		}
//		return rel;
//	}	
	
	/** 
	 * Create constraints from EGDs.
	 * @param egd
	 */
	private static Quantifier getEgd(Egd egd) {
		HashMap<String, Expr> exprMap = new HashMap<String, Expr>();

		BoolExpr lhsExprs = null;
		for (Atom a : egd.getLhs()) {
			String relName = (isBefore == true) ? a.getPredicate().getRelName() : a.getPredicate().getRelName() + "1";
			
			FuncDecl rel = funcDeclMaps.get(relName);			
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
						exprs[i] = ctx.mkInt(SMTConstraint.getLabelId(Util.removeQuotes(val)));
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
						throw new IllegalArgumentException("[ERROR] RHS predicates with only variables are supported yet.");
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
		BoolExpr bExpr = null;
		Quantifier q = null; 
		if (isBefore == true) {
			bExpr = ctx.mkImplies(lhsExprs, rhsExprs);
			Expr[] exprs = exprMap.values().toArray(new Expr[exprMap.values().size()]);
			q = ctx.mkForall(exprs, bExpr, 1, null, null, null, null);
		} else {
			bExpr = ctx.mkAnd(lhsExprs, ctx.mkNot(rhsExprs));			
			Expr[] exprs = exprMap.values().toArray(new Expr[exprMap.values().size()]);
			q = ctx.mkExists(exprs, bExpr, 1, null, null, null, null);
		}


		return q;
	}
	
	private static void addRulesForViolationCheck(TransRule rule) {	
		/*
		 * 1. create node/edge set from pattern (before)
		 * 2. create not determined ones (before augmented only)
		 * 
		 */
		int numOfLabelVars = 0;
		int numOfNodeVars = 0;
		int numOfEdgeVars = 0;

		BoolExpr bExpr = null;
		
		HashMap<String, Integer> nodeVarToIdMap = new HashMap<String, Integer>();
		HashMap<String, Integer> edgeVarToIdMap = new HashMap<String, Integer>();
		HashMap<String, Integer> labelVarToIdMap = new HashMap<String, Integer>();
		
		ArrayList<Atom> atoms;
	
		HashSet<String> idFromBasePatternSet = new HashSet<String>();

		atoms = rule.getPatternBefore();
		for (int j = 0; j < atoms.size(); j++) {
			Atom a = atoms.get(j);
			idFromBasePatternSet.add(a.getTerms().get(0).getVar());
		}	
		
		BoolExpr bExprNodeOr = null; 
		BoolExpr bExprEdgeOr = null;
		
		HashSet<String> newVars = new HashSet<String>();
		for (int i = 0; i < 2; i++) {
			if (i == 0) {
				atoms = rule.getPatternBefore();
			} else {
				atoms = rule.getPatternAugmentedBefore();
			}

			BoolExpr bExprSub = null;
			for (int j = 0; j < atoms.size(); j++) {
				Atom a = atoms.get(j);

				if (i == 0) {
					idFromBasePatternSet.add(a.getTerms().get(0).getVar());
				}
					
				String relName = (isBefore == true) ? a.getPredicate().getRelName() : a.getPredicate().getRelName() + "1";
				FuncDecl rel = funcDeclMaps.get(relName);
				Expr[] exprs = new Expr[rel.getDomainSize()];
	
				for (int k = 0; k < a.getTerms().size(); k++) {
					if (i == 1) {
						if (idFromBasePatternSet.contains(a.getTerms().get(0).getVar()) == true) {
							continue;
						}
					}
					if (a.getTerms().get(k).isVariable() == true) { // variable
						String var = a.getTerms().get(k).getVar();
						if (a.getPredicate().equals(Config.predE) == true && k == 0) { // edge var
							if (i == 0) {
								if (edgeVarToIdMap.containsKey(a.getTerms().get(k).toString()) == false) { // always true
									edgeVarToIdMap.put(a.getTerms().get(k).toString(), numOfEdgeVars++);
								}
								int edgeId = edgeVarToIdMap.get(a.getTerms().get(k).toString());
								exprs[k] = ctx.mkInt(edgeId);
							} else {
								exprs[k] = ctx.mkIntConst(var);
								newVars.add(var);
							}
						} else if (k + 1 == a.getTerms().size()) { // label var
							if (i == 0) {
//								System.out.println("a: " + a + " k: " + k);
								if (labelVarToIdMap.containsKey(a.getTerms().get(k).toString()) == false) {
									labelVarToIdMap.put(a.getTerms().get(k).toString(), numOfLabelVars++);
								}
								int labelVarId = labelVarToIdMap.get(a.getTerms().get(k).toString());
//								System.out.println("FFFFF");
//								System.exit(0);
								exprs[k] = ctx.mkInt(labelVarId);//String(a.getTerms().get(k).toString());
							} else {
//								if (a.getPredicate().equals(Config.predE) == true) {
//									exprs[k] = ctx.mkInt(1); // 10001
//								} else {
								if (labelVarToIdMap.containsKey(var) == true) {
									int labelId = labelVarToIdMap.get(var);
									exprs[k] = ctx.mkInt(labelId); // Const(var, ctx.getIntSort());
								} else {
									exprs[k] = ctx.mkConst(var, ctx.getIntSort());
								}
//								}
								newVars.add(var);
								
							}
						} else { // node var
							if (i == 0) {
								if (nodeVarToIdMap.containsKey(a.getTerms().get(k).toString()) == false) {
									nodeVarToIdMap.put(a.getTerms().get(k).toString(), numOfNodeVars++);
								}
								int nodeId = nodeVarToIdMap.get(a.getTerms().get(k).toString());
								exprs[k] = ctx.mkInt(nodeId);
							} else {
								if (nodeVarToIdMap.containsKey(var) == true) {
									int varId = nodeVarToIdMap.get(var);
									exprs[k] = ctx.mkInt(varId); // Const(var, ctx.getIntSort());
								} else {
									exprs[k] = ctx.mkIntConst(var);
								}
								newVars.add(var);
							}
						}
					} else { // constant
						SMTConstraint.setLabelId(a.getTerms().get(k).toString());
						//exprs[k] = ctx.mkString(Util.removeQuotes(a.getTerms().get(k).toString()));
						exprs[k] = ctx.mkInt(SMTConstraint.getLabelId(Util.removeQuotes(a.getTerms().get(k).toString())));
					}
//					logger.info("labelVarToIdMap: " + labelVarToIdMap);
					if (k + 1 == a.getTerms().size()) {
						BoolExpr sexp = null;
						for (int u = 0; u <= k; u++) {
							if (sexp == null) {
								sexp = ctx.mkEq(ctx.mkIntConst("v"+u), exprs[u]);	
							} else {
								sexp = ctx.mkAnd(sexp, ctx.mkEq(ctx.mkIntConst("v"+u), exprs[u]));
							}
						}
						
						if (bExprSub == null) {
							bExprSub = (BoolExpr)rel.apply(exprs);
						} else {
							bExprSub = ctx.mkAnd(bExprSub, (BoolExpr)rel.apply(exprs));
						}

						if (a.getPredicate().equals(Config.predN) == true) {
							bExprNodeOr = (bExprNodeOr == null) ? sexp : ctx.mkOr(bExprNodeOr, sexp);
						} else {
							bExprEdgeOr = (bExprEdgeOr == null) ? sexp : ctx.mkOr(bExprEdgeOr, sexp);
						}

					}
				}
			}	
			bExpr = bExprSub;
			fp.addRule(bExpr, ctx.mkSymbol("transrules"));
		}
				
//		System.out.println("ggg: " + fp.toString());
		
		String relN = (isBefore == true) ? "N" : "N1";
		String relE = (isBefore == true) ? "E" : "E1";

		Expr[] varN = new Expr[] {
				ctx.mkConst("v0", funcDeclMaps.get(relN).getDomain()[0]),
				ctx.mkConst("v1", funcDeclMaps.get(relN).getDomain()[1])
		};
		bExprNodeOr = ctx.mkImplies((BoolExpr)funcDeclMaps.get(relN).apply(varN[0], varN[1]), bExprNodeOr);
		Quantifier qN = ctx.mkForall(varN, bExprNodeOr, 1, null, null, null, null);
		fp.addRule(qN, ctx.mkSymbol("nodeOr"));

		Expr[] varE = new Expr[] {
				ctx.mkConst("v0", funcDeclMaps.get(relE).getDomain()[0]),
				ctx.mkConst("v1", funcDeclMaps.get(relE).getDomain()[1]),
				ctx.mkConst("v2", funcDeclMaps.get(relE).getDomain()[2]),
				ctx.mkConst("v3", funcDeclMaps.get(relE).getDomain()[3])
		};
		bExprEdgeOr = ctx.mkImplies((BoolExpr)funcDeclMaps.get(relE).apply(varE[0], varE[1], varE[2], varE[3]), bExprEdgeOr);
		Quantifier qE = ctx.mkForall(varE, bExprEdgeOr, 1, null, null, null, null);
		fp.addRule(qE, ctx.mkSymbol("edgeOr"));


		for (HashMap.Entry<String, Integer> e : nodeVarToIdMap.entrySet()) {
			BoolExpr notExpr = null;
			notExpr = ctx.mkNot(ctx.mkEq(ctx.mkInt(e.getValue()), ctx.mkIntConst(e.getKey()+"_i0")));
			notExpr = ctx.mkAnd(notExpr, ctx.mkNot(ctx.mkEq(ctx.mkInt(e.getValue()), ctx.mkIntConst(e.getKey()+"_i1"))));
			notExpr = ctx.mkAnd(notExpr, ctx.mkNot(ctx.mkEq(ctx.mkInt(e.getValue()), ctx.mkIntConst(e.getKey()+"_o0"))));
			notExpr = ctx.mkAnd(notExpr, ctx.mkNot(ctx.mkEq(ctx.mkInt(e.getValue()), ctx.mkIntConst(e.getKey()+"_o1"))));
			fp.addRule(notExpr, ctx.mkSymbol("notExpr"));
		}
		
//		BoolExpr newVarsExpr = null;
//		for (String var : newVars) {
//			BoolExpr e = ctx.mkAnd(ctx.mkLt(ctx.mkIntConst(var), ctx.mkInt(20)), ctx.mkGe(ctx.mkIntConst(var), ctx.mkInt(0)));
//			if (newVarsExpr == null) {
//				newVarsExpr = e;
//			} else {
//				newVarsExpr = ctx.mkAnd(newVarsExpr, e);
//			}
//		}
//		fp.addRule(newVarsExpr, ctx.mkSymbol("newVarsConstraint"));

	}	
	
	
	public static boolean check(String name) {
		/*
		 * Required
		 * 	graph schema for input/output (maybe the same)
		 *  EGDs for input/output (maybe the same)
		 *  a transrule (for beforePattern/afterPattern)
		 * 1. Construct an augmented pattern (augBeforePattern) from beforePattern
		 * 2. Construct an augmented pattern (augAfterPattern)from afterPattern
		 * 3. Valid if there exists any constraint violation of augAfterPattern where its augBeforePattern satisfying constraints  
		 */
		viewName = name;
		if (1==1)
			return true;
		
		if (Config.isTypeCheckEnabled() == false) {
			return true;
		}

		if (Config.isTypeCheckPruningEnabled() == true) {
//			populateRelatedRulesWithPruning();
		} else {
//			populateRelatedRulesWithoutPruning();
		}
		
		
		TransRuleList transRules = GraphTransServer.getTransRuleList(viewName);
//		System.out.println("viewName: " + viewName + " transRules#: " + transRules.getNumTransRuleList());
		for (int i = 0; i < transRules.getNumTransRuleList(); i++) {
			initializeSMTSolver();

			addRelationDecl();
			
			addDefaultEgds();
			addRelatedEgds();
			addRulesForViolationCheck(transRules.getTransRule(i));

			isBefore = false;
			addDefaultEgds();
			addRelatedEgds();
			addRulesForViolationCheck(transRules.getTransRule(i));
//			transRules.getTransRule(i).show();
		
			solver.add(fp.getRules());
//			System.out.println("final string: [\n" + solver.toString() + "\n]");

			int tc1 = Util.startTimer();
			Status st = solver.check();
//			System.out.println("viewName: " + viewName + " i: " + i + " ==> st == Status.SATISFIABLE: " + (st == Status.SATISFIABLE) + " time: " + Util.getElapsedTime(tc1));
			
			if (st == Status.SATISFIABLE) {
				return false;
			}
		}
		
//		logger.info("st == Status.SATISFIABLE: " + (st == Status.SATISFIABLE));

		return true; //(st == Status.SATISFIABLE);
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
