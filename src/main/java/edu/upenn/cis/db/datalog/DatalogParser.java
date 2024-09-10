package edu.upenn.cis.db.datalog;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.*; //.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.db.ConjunctiveQuery.Atom;
import edu.upenn.cis.db.ConjunctiveQuery.Predicate;
import edu.upenn.cis.db.ConjunctiveQuery.Term;
import edu.upenn.cis.db.ConjunctiveQuery.Type;
import edu.upenn.cis.db.datalogRuleParser.DatalogRulesBaseVisitor;
import edu.upenn.cis.db.datalogRuleParser.DatalogRulesLexer;
import edu.upenn.cis.db.datalogRuleParser.DatalogRulesParser;
import edu.upenn.cis.db.datalogRuleParser.DatalogRulesParser.DatalogContext;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.parser.EgdParser;

/**
 * 
 * @author sbnet21
 *
 */
public class DatalogParser extends DatalogRulesBaseVisitor<Void> {
	final static Logger logger = LogManager.getLogger(DatalogParser.class);

//	private static DatalogParser instance = new DatalogParser();

	private DatalogProgram program;
	private DatalogClause clause;
	private Predicate predicate;

	private boolean isHead;
	private enum ClauseType {
		SCHEMA, RULE, FACT 
	};
	private ClauseType type;

	/**
	 * 
	 * @param p A datalog program object 
	 */
	public DatalogParser(DatalogProgram p) {
		program = p;
	}
	
	public DatalogParser() {
		program = null;
	}

	/**
	 * Parse a clause and insert it to the datalog program
	 * @param rule A datalog schema, rule, or fact.
	 * @return
	 */
	public void Parse(String rule) {
		if (rule.trim().contentEquals("") == true) {
			return;
		}

		CharStream input = CharStreams.fromString(rule);
		DatalogRulesLexer lexer = new DatalogRulesLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		DatalogRulesParser parser = new DatalogRulesParser(tokens);
		ParseTree tree = parser.datalog();

		visit(tree);

		if (type == ClauseType.SCHEMA) {
			program.addPredicate(predicate);
		} else {
			program.addRule(clause);
		}	
	}

	/**
	 * Parse rules and add them into a datalog program.
	 * @param p
	 * @param rules Rules split by '\n'
	 */
	public void ParseAndAddRules(String rules) {
		String[] logics = rules.split("\n");

		for (int i = 0; i < logics.length; i++) {
			if (logics[i].charAt(0) != '#' && logics[i].substring(0, 2).contentEquals("//") == false) {
				Parse(logics[i]);
			}
		}
	}

	public DatalogClause ParseQuery(String rule) {
		if (rule.trim().contentEquals("") == false) {
//			System.out.println("ParseQuery: " + rule);

//			System.out.println("[dp] rule: " + rule);
			clause = new DatalogClause();
			
			CharStream input = CharStreams.fromString(rule);
			DatalogRulesLexer lexer = new DatalogRulesLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			DatalogRulesParser parser = new DatalogRulesParser(tokens);
			ParseTree tree = parser.datalog();

			visit(tree);

			if (clause == null) {
				logger.info("clauses = null");	
			} else {
//				System.out.println("[ParseQuery] clause: " + clause);
				String relName = clause.getHead().getPredicate().getRelName();
				if (type == ClauseType.RULE) {
					program.getIDBs().add(relName);
				} else if (type == ClauseType.SCHEMA) {
					program.getEDBs().add(relName);
				}

				logger.info("claues: " + clause);
			}
		} else {
			clause = null;
		}
		return clause;		
	}

	@Override 
	public Void visitDatalog_clause(DatalogRulesParser.Datalog_clauseContext ctx) {
		if (ctx.datalog_schema() != null) { // schema
			type = ClauseType.SCHEMA;
		} else if (ctx.datalog_rule() != null) { // rule
			type = ClauseType.RULE;
		} else { // fact
			type = ClauseType.FACT;
		}
		return visitChildren(ctx); 
	}

	@Override 
	public Void visitDatalog_head(DatalogRulesParser.Datalog_headContext ctx) {
		if (type == ClauseType.SCHEMA) {
			if (ctx.atom().size() > 1) {
				throw new IllegalArgumentException("Schema rule should have only one atom in the head.");
			}
			String rel_name = ctx.atom().get(0).rel_name().getText();			
			predicate = new Predicate(rel_name);
		} else {
			clause = new DatalogClause();			
		}
		isHead = true;
		return visitChildren(ctx); 
	}

	@Override 
	public Void visitDatalog_body(DatalogRulesParser.Datalog_bodyContext ctx) {
		isHead = false;

		return visitChildren(ctx); 
	}

	@Override 
	public Void visitAtom_type(DatalogRulesParser.Atom_typeContext ctx) {
		String var = ctx.var().getText();

		if (ctx.datalog_type().type_int() != null) {
			predicate.setArg(var, Type.Integer);
		} else {
			predicate.setArg(var, Type.String);
		}

		return visitChildren(ctx); 
	}

	@Override 
	public Void visitAtom(DatalogRulesParser.AtomContext ctx) {
		Atom a = null;

		if (type != ClauseType.SCHEMA) {
			String rel_name = ctx.rel_name().getText();

			if (program.getPredicate(rel_name) == null) {
				a = new Atom(new Predicate(rel_name));
			} else {
				a = new Atom(program.getPredicate(rel_name));
			}

			if (ctx.neg() != null) {
				a.setNegated(true);
			}
		}

		for (int i = 0; i < ctx.terms().var_or_constant().size(); i++) {
			String var = ctx.terms().var_or_constant(i).getText();

			if (type == ClauseType.SCHEMA) {
				if (isHead == true) { // always true
					predicate.addArg(var);
				}
			} else {
				if (ctx.terms().var_or_constant(i).var() != null) { // variable
					a.appendTerm(new Term(var, true));
				} else { // constant
					a.appendTerm(new Term(var, false));
				}
			}

		}

		if (type != ClauseType.SCHEMA) {
			if (isHead == true) {
				clause.setHead(a);
				clause.addAtomToHeads(a); // FIXME:
			} else {
				clause.addAtomToBody(a);
			}
		}

		return visitChildren(ctx); 
	}

	@Override 
	public Void visitInterpreted_atom(DatalogRulesParser.Interpreted_atomContext ctx) {
		String lop = ctx.lop().getText();
		String rop = ctx.rop().getText();

		boolean rVariable = true;
		if (ctx.rop().var() == null) {
			rVariable = false;
		} 
		
		// op: '=' | '<' | '>' | '!=' | '>=' | '<=';

		String op = ctx.op().getText();
		Atom a = null;
		switch(op) {
		case "=":
			a = new Atom(Config.predOpEq);
			break;
		case ">":
			a = new Atom(Config.predOpGt);
			break;
		case "<":
			a = new Atom(Config.predOpLt);
			break;
		case ">=":
			a = new Atom(Config.predOpGe);
			break;
		case "<=":
			a = new Atom(Config.predOpLe);
			break;
		case "!=":
			a = new Atom(Config.predOpNeq);
			break;				
		default:
			throw new UnsupportedOperationException("op[" + op + "] is not supported.");
		}
		a.getPredicate().setInterpreted(true);
		a.appendTerm(new Term(lop, true));
		a.appendTerm(new Term(rop, rVariable));
		
		clause.addAtomToBody(a);

		return visitChildren(ctx); 
	}

	public static void main(String[] args) {
		DatalogProgram p = new DatalogProgram();
		DatalogParser parser = new DatalogParser(p);
		
		// TODO Auto-generated method stub
		Config.initialize();
		String rule = "V1(a,b) <- R(a,c), S(c,b).\n"
				+ "V2(a,b) <- R(a,c), R(c,b).";

		parser.ParseAndAddRules(rule);
		System.out.println(p);
		
		DatalogClause q = parser.ParseQuery("Q(b) <- V2(1,b).");
		System.out.println("q: " + q);
		
		
//		DatalogClause q = parser.ParseQuery("TEMP(a) <- R(a,b,1),A(a,b,\"tell\").");
//		parser.Parse("delta_v1v(id, id_l, id_r, label, 1) <- MAP_v1v(id, id_l, id_r, label, mid, mid_l, mid_r, \"tt\").");

		
//		System.out.println("p: " + p);
		
	}

//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		Config.initialize();
//
//		DatalogProgram p = new DatalogProgram();
//		DatalogParser parser = new DatalogParser(p);
//		//		parser.Parse("R(c,d) -> int(c), int(d).");
//		//		parser.Parse("S(e,f) -> int(e), int(f).");
//		//		parser.Parse("T(e,f) -> int(e), int(f).");
//		//		parser.Parse("A(a,b) -> int(a), int(b).");
//		//		parser.Parse("B(a,b) -> int(a), int(b).");
//		//		parser.Parse("C(a,b) -> int(a), int(b).");
//		//
//		//		parser.Parse("A(a,b) <- R(a,c),S(c,b).");
//		//		parser.Parse("A(c,d) <- S(c,e),R(e,d).");
//		//		parser.Parse("B(e,f) <- S(e,g),S(g,f).");
//		//		parser.Parse("C(e1,f1) <- S(e1,h),S(h,f1).");
//
//
//		//		parser.Parse("N0(n1,n2,n3,l) -: int(n1), int(n2), int(n3), int(l).");
//		//		parser.Parse("E0(e1,e2,e3,f1,f2,f3,t1,t2,t3,l) -: int(e1), " +
//		//				"int(e2), int(e3), int(f1), int(f2), int(f3), int(t1), int(t2), int(t3), int(l).");
//		//		
//		//		parser.Parse("M0(a,b,c,d) -: int(a), int(b).");
//		//		
//		//		parser.Parse("A(c,d) :- S(c,e),R(e,d).");
//
//		//		parser.Parse("B(e,f) :- A(e,h), R(h,f).");
//		//		parser.Parse("C(e,f) :- T(e,h),!B(h,f).");
//
//		//		DatalogClause q = parser.ParseQuery("Q(a,b) :- A(a,c),!B(c,d),C(d,b), C(w,v), C(v,f), a=5.");
//
//
//		//		DatalogClause q = parser.ParseQuery("Q(a,b) :- A(m,n), R(a,b),!A(a,b), a=5.");
//		//		DatalogClause q = parser.ParseQuery("Q(a,b) :- R(a,b),!A(a,b).");
//		//		DatalogClause q = parser.ParseQuery("Q(a,b) :- R(a,b),!A(a,b).");
//
//		//		DatalogClause q = parser.ParseQuery("Q(a,b) :- R(a,c),S(c,b).");
//
//		//		parser.Parse("U(a,b,c) -> int(a), int(b), int(c).");
//		//		parser.Parse("G1(a,b) -> int(a), int(b).");
//		//		parser.Parse("G2(a) -> int(a).");
//		//		parser.Parse("G3(a) -> int(a).");
//		//		parser.Parse("G4(a) -> int(a).");
//		//		parser.Parse("G5(a) -> int(a).");
//		//		
//		//		parser.Parse("G1(a,b) <- S(a,c),S(c,b).");
//		//		parser.Parse("G1(a,b) <- T(a,c),T(c,b).");
//		//		parser.Parse("G2(a) <- S(a,_n).");
//		//		parser.Parse("G2(a) <- T(a,_n).");
//		//		parser.Parse("G3(a) <- G2(a), !G1(a,_n).");
//		//		parser.Parse("G4(a) <- U(a,_n1,_n2).");
//		//		parser.Parse("G5(a) <- U(a,_n1,_n2), !U(a,_n3,_n4).");
//
//		//		DatalogClause q = parser.ParseQuery("Q(a) <- G1(a,_n), a=3.");
//		//		DatalogClause q = parser.ParseQuery("Q(a) <- G3(a), a=3.");
//		//		DatalogClause q = parser.ParseQuery("Q(a) <- G1(a,b), a=3, b=2");
//		//		DatalogClause q = parser.ParseQuery("Q(a) <- G1(a,b),c=3,b=3.");
//		//		DatalogClause q = parser.ParseQuery("Q(a) <- G5(a),a=3.");
//
//		p.addEDB("R");
//		p.addEDB("S");
//		
//		parser.Parse("R(a,b) -> int(a), int(b).");
//		parser.Parse("S(a,b) -> int(a), int(b).");
//		parser.Parse("A(a,b) <- R(a,c), R(c,b)."); // 2hop
//		parser.Parse("B(a,b) <- A(a,c), A(c,b)."); // 4hop
//
////		DatalogClause q = parser.ParseQuery("Q(a) <- B(a,b), !A(a,_b), a=5, b=3");
//		DatalogClause q = parser.ParseQuery("Q(a) <- B(a,b), !A(a,_b).");
//
//		/*
//		 * Q(a) <- B(a,b), !A(a,_b)
//		 * 	A(a,c),A(c,b), !A(a,_b)
//		 *  R(a,_1),R(_1,c),R(c,_2),R(_2,b), !A(a,_b)
//		 *  	$S0(a) <- R(a,_1),R(_1,c),R(c,_2),R(_2,b)
//		 *  	S(a,_b) <- R(a,_1),R(_1,c),R(c,_2),R(_2,b),  A(a,_b)
//		 *      S(a,_b) <- R(a,_1),R(_1,c),R(c,_2),R(_2,b),  R(a,_3),R(_3,_b)
//		 *      $S(a) <_ S0(a), R(a,_3),R(_3,_b).
//		 *  R(a,_1),R(_1,c),R(c,_2),A(_2,b), !S(a,_b).
//		 *  $Q(a) <- S0(a),!S(a).
//		 */
//		logger.info("program: " + p);
//
//
//
//		Util.resetTimer();
//		DatalogProgram pp = DatalogQueryRewriter.getProgramForRewrittenQuery(p, q);
//		logger.info("Elaspsed Time: " + Util.getLapTime());
//		//pp.create()
//		//pp.getAnswer(Q)
//
//		logger.info("Rewritten Program: " + pp);
//		logger.info("Query: " + q);
//
//	}

}
