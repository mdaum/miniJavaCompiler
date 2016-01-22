package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;

/*
 * Program ::= (ClassDeclaration)* eot
 * ClassDeclaration ::= class id { ( FieldDeclaration | MethodDeclaration )* }
 * FieldDeclaration ::= Visibility Access Type id ;
 * MethodDeclaration ::= Visibility Access ( Type | void ) id ( ParameterList? ) {Statement*}
 * Visibility ::= ( public | private )?
 * Access ::= static ?
 * Type ::= int |  boolean |  id | ( int | id ) []
 * ParameterList ::= Type id ( , Type id )*
 * ArgumentList ::= Expression ( , Expression )*
 * Reference ::= Reference . id | (this | id)
 * ArrayReference ::= id [ Expression ]
 * Statement ::=
		{ Statement* }
		| Type id = Expression ;
		| Reference = Expression ;
		| ArrayReference = Expression ;
		| Reference ( ArgumentList? ) ;
		| return Expression? ;
		| if ( Expression ) Statement (else Statement)?
		| while ( Expression ) Statement
 * Expression ::=
		Reference
		| ArrayReference
		| Reference ( ArgumentList? )
		| unop Expression
		| Expression binop Expression
		| ( Expression )
		| num | true | false
		| new ( id ()  | int [ Expression ] |  id [ Expression ] )*/
public class Parser {
	private Scanner scanner;
	private ErrorReporter reporter;
	private Token currentToken;

	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}
}
