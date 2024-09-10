package edu.upenn.cis.db.Z3Solver;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.helper.Util;

import com.microsoft.z3.*;

/*
 * Z3SMTSolver.
 */
public class Z3SMTSolver {
	final static Logger logger = LogManager.getLogger(Z3SMTSolver.class);

	/**
	 * SMTSolver.
	 * 
	 * @return true if SAT, false if UNSAT.
	 */
	public static boolean check(String constraint) {
		/**
		 * How to set up dyld in OSX
		 * https://stackoverflow.com/questions/18855488/eclipse-on-mac-dyld-library-not-loaded-image-not-found
		 * 
		 * DYLD_LIBRARY_PATH 
		 *
		 * LINUX: https://github.com/Z3Prover/z3/tree/master/examples/java
		 * 
		 */
	    HashMap<String, String> cfg = new HashMap<>();
	    Context ctx = new Context(cfg);
//	    Fixedpoint fp = ctx.mkFixedpoint();
		
		//constraint = "(true)\n(true)";
		
//	    BoolExpr[] fsresult = ctx.parseSMTLIB2String(constraint, null, null, null, null);
	    System.out.println("=============");

	    Util.resetTimer();
	    Solver solver = ctx.mkSolver();
//	    BoolExpr[] fsresult = fp.ParseFile("/Users/sbnet21/Tools/z3-4.8.7-x64-osx-10.14.6/bin/new-two.txt");
	    
//	    for (int i = 0; i < fsresult.length; i++) {
//	    	System.out.println(fsresult[i].toString());
//	    }
	    solver.fromString(constraint);;
//	    solver.fromFile("/Users/sbnet21/Tools/z3-4.8.7-x64-osx-10.14.6/bin/new-one.txt");
	    
//	    BoolExpr [] 	parseSMTLIB2File (String fileName, Symbol[] sortNames/, Sort[] sorts, Symbol[] declNames, FuncDecl[] decls)
//	    BoolExpr[] b = ctx.parseSMTLIB2File("/Users/sbnet21/Tools/z3-4.8.7-x64-osx-10.14.6/bin/new-two.txt", null, null, null, null);
//	    solver.add(fsresult);
	    Status s = solver.check();
	    logger.info("Elapsed time: " + Util.getLapTime());
	    logger.info("Output: " + s);
		
        ctx.close();

	    return s == Status.SATISFIABLE;
	}
	
	public static void z3test() {
	    HashMap<String, String> cfg = new HashMap<>();
	    Context ctx = new Context(cfg);
	    
//	    
	    Fixedpoint fp = ctx.mkFixedpoint();
	    FuncDecl fd = ctx.mkFuncDecl("R", ctx.getIntSort(), ctx.getBoolSort());
	    fp.registerRelation(fd);
	    System.out.println("fp: " + fp.toString());
	    
	   
//	    fp.ParseString("(decalre-rel R() Int)");
//	    System.out.println("fp: " + fp.toString());
//	    

	    Params p = ctx.mkParams();
        p.add("engine", "datalog");
        p.add("datalog.default_relation", "doc");
        p.add("print_answer", true);
        Fixedpoint fix = ctx.mkFixedpoint();
        fix.setParameters(p);
	    String c = "(declare-rel mc (Int Int))\n" + 
	    		"\n" + 
	    		"(assert (mc 1 2))\n" + 
	    		"";
	    
	    Sort[] domain = { ctx.getIntSort() };
        FuncDecl fds = ctx.mkFuncDecl("R", domain, ctx.getBoolSort());
        fix.registerRelation(fds);
	    BoolExpr[] rr = fix.getAssertions();
	    System.out.println("fix: " + fix);
	    System.out.println("rr:: " + rr.length);

	    BoolExpr[] bb = ctx.parseSMTLIB2String(c, null, null, null, null);
	    System.out.println("---");
	    Solver solver = ctx.mkSolver();
	    solver.add(rr);
	    System.out.println("==> " + solver.toString());
	    System.out.println("---");

//	    Symbol[] sortNames[] = new Symbol([]);
//	    Sort[] sorts;
//	    Symbol[] declNames;
//	    FuncDecl[] decls;
//	    ctx.parseSMTLIB2String(c, sortNames, sorts, declNames, decls);

//	    solver.fromString(c);
//	    solver.add(bb);
	    System.out.println("---");

	    Status s = solver.check();
	    System.out.println("---");

	    logger.info("Elapsed time: " + Util.getLapTime());
	    logger.info("Output: " + s);
		
        ctx.close();
	}
	
	public static boolean checkByBinary(String filename) {
		String cmd = "/Users/sbnet21/Tools/z3-4.8.7-x64-osx-10.14.6/bin/z3 "; // osx
//		cmd = "/home1/s/soonbo/Tools/z3-4.8.7-x64-ubuntu-16.04/bin/z3 "; // linux
		cmd = cmd + filename + " -smt2";
		
//		Util.resetTimer();
		System.out.println("cmd: " + cmd);
		ArrayList<String> res = Util.getExternalCommand(cmd);
//	    logger.info("Elapsed time: " + Util.getLapTime());

	    for (int i = 0; i < res.size(); i++) {
			logger.info("i: " + i + " => " + res.get(i));			
		}

	    return res.get(0).contentEquals("unsat");
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.setProperty("jna.library.path", "lib");

		
		String constraint = "(assert (= 1 2))" + 
				"(check-sat)";
		boolean result = Z3SMTSolver.check(constraint);
		
		System.out.println("result: " + result);
//		String filename = "smt2_test.txt";
//		Util.writeToFile(filename, constraint);
//		
//		Util.resetTimer();
//		boolean unsat = checkByBinary(filename);
//		System.out.println("Elapsed Time: " + Util.getLapTime());
//		
//		System.out.println("unsat? : " + unsat);
		
		//checkByBinary();
	}

}

