/**
 * Define a grammar called DatalogRules
 */
grammar DatalogRules;

datalog: (datalog_clause PERIOD) | comment;
comment: COMMENT;
datalog_clause: datalog_schema | datalog_rule | datalog_fact;
datalog_schema: datalog_head '->' atom_type (',' atom_type)*;
atom_type: datalog_type '(' var ')';
datalog_type: type_int | type_string;
datalog_rule: datalog_head '<-' datalog_body;
datalog_head: atom (',' atom)*;
datalog_body: any_atom (',' any_atom)*;
datalog_fact : atom;
any_atom: atom | interpreted_atom;
atom: neg? rel_name '(' terms ')';
terms: var_or_constant (',' var_or_constant)*;
interpreted_atom: lop op rop;

var_or_constant: var | constant;
constant: integer | literal;

neg: '!';
op: '=' | '<' | '>' | '!=' | '>=' | '<=';
lop: var;
rop: var | integer | literal;

type_int: 'int';
type_string: ' string';

rel_name: ID;
var: ID;
literal: TEXT_STRING;

integer: INTEGER;
number:	(PLUS | MINUS)? (INTEGER) ;

PERIOD: '.';
PLUS: '+';
MINUS: '-';
INTEGER : [0-9]+;

// Some are from https://github.com/leonchen83/antlr4-mysql/blob/master/MySQL.g4

ID : [_a-zA-Z][_a-zA-Z0-9]* ; // match lower-case identifiers

COMMENT: '#' ~[\r\n]* -> skip ;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines

TEXT_STRING:
	(
		(  '\'' ( ('\\' '\\') | ('\'' '\'') | ('\\' '\'') | ~('\'') )* '\''  )
		|
		(  '"' ( ('\\' '\\') | ('"' '"') | ('\\' '"') | ~('"') )* '"'  ) 
	)
; //'"' [\s-.:/_a-zA-Z0-9]* '"';


