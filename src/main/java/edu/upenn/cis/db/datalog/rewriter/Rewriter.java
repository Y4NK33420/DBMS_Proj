package edu.upenn.cis.db.datalog.rewriter;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.graphtrans.Config;

public class Rewriter {
	final static Logger logger = LogManager.getLogger(Rewriter.class);
	private static DatalogProgram program; /* given program */
	private static DatalogProgram rewrittenProgram;

	public static DatalogProgram getProgram() {
		return program;
	}
	
	public static DatalogProgram getRewrittenProgram() {
		return rewrittenProgram;
	}
	
	/**
	 * Take a datalog program and a query, and return a rewritten query with additional (intermediate) rules.
	 */
	public static HashSet<Atom> getRewrittenBody(HashSet<String> headVars, Set<Atom> body) {
		HashSet<Atom> rwBody = new LinkedHashSet<Atom>(body);
		
		Handler.handleAllPositiveIDBs(headVars, rwBody);
		if (rwBody.isEmpty() == false) {
			Handler.handleAllUDFs(headVars, rwBody);
			Handler.handleAllNegativeIDBs(headVars, rwBody);
			
			Helper.integrityCheckBody(rwBody);
		}		
		return rwBody;
	}

	/**
	 * Get a datalog program of the rewritten queries (entry point)
	 */
	public static DatalogProgram getProgramForRewrittenQuery(DatalogProgram p, DatalogClause q) {		
		// 1. prepare 
		if (q.getHeads().size() > 1) {
			throw new UnsupportedOperationException("Query should have 1 head.");
		}
		program = p;
		rewrittenProgram = new DatalogProgram();
		
		HashSet<String> headVars = new LinkedHashSet<String>();
		for (Atom h : q.getHeads()) {
			headVars.addAll(h.getVars());
		}
		Set<Atom> body = new LinkedHashSet<Atom>();
		body.addAll(q.getBody());

		// 2. compute rewriting body
		HashSet<Atom> rwBody = getRewrittenBody(headVars, body);

		// 3. construct head and add the final rule to rewriting
		if (rwBody.isEmpty() == false) {
			Atom newHead = new Atom(new Predicate(Config.relname_query));
			for (String v : headVars) {
				newHead.appendTerm(new Term(v, true));
			}
			DatalogClause c7 = new DatalogClause(newHead, rwBody);
			rewrittenProgram.addRule(c7, "QUERY");			
		} else { // FIXME: return empty result
			throw new IllegalArgumentException("rwBody is empty");
		}

		return rewrittenProgram;
	}	

}
