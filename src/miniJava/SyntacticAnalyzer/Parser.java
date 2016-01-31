package miniJava.SyntacticAnalyzer;

import java.beans.Expression;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;


public class Parser {
	private Scanner scanner;
	private ErrorReporter reporter;
	private Token currentToken;

	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}
	class SyntaxError extends Error{
		private static final long serialVersionUID=1L;
	}
	
	public Expression parse(){
		currentToken=scanner.scan();
		try{
			return parseProgram();
		}
		catch(SyntaxError e){
			return null;
		}
	}

	private Expression parseProgram() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseClassDeclaration() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseFieldDeclaration() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseMethodDeclaration() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseVisibility() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseAccess() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseType() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseParameterList() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseArgumentList() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseReference() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseArrayReference() throws SyntaxError {
		
		return null;
	}
	
	private Expression Statement() throws SyntaxError {
		
		return null;
	}
	
	private Expression parseExpression() throws SyntaxError {
		
		return null;
	}
	
}
