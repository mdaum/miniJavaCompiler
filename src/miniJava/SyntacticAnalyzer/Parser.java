package miniJava.SyntacticAnalyzer;



import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;



/**
 * The Class Parser.
 * <p>Note:<br>
 * bold means terminal<br>
 * underline means non-terminal<br>
 * italicized means set of terminals (id and num)
 */
public class Parser {
	
	/** The scanner. */
	private Scanner scanner;
	
	/** The reporter. */
	private ErrorReporter reporter;
	
	/** The current token. */
	private Token currentToken;

	/**
	 * Instantiates a new parser.
	 *
	 * @param scanner the scanner
	 * @param reporter the reporter
	 */
	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}
	
	/**
	 * The Class SyntaxError.
	 */
	class SyntaxError extends Error{
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID=1L;
	}
	
	/**
	 * Kicks off Parsing.
	 */
	public void parse(){
		currentToken=scanner.scan();
		while(currentToken.kind==TokenKind.comment)currentToken=scanner.scan();
		try{
			parseProgram();
		}
		catch(SyntaxError e){
		}
	}
	
	/**
	 * Parses Program.
	 * <p>
	 *	<u>Program</u> ::= (<u>ClassDeclaration</u>)*<b>eot</b> </p>
	 * @throws SyntaxError the syntax error
	 */
	
	private void parseProgram() throws SyntaxError {
		ClassDeclList c= new ClassDeclList();
		while(currentToken.kind!=TokenKind.eot){
			c.add(parseClassDeclaration());
		}
		accept(TokenKind.eot);
		Package p= new Package(c,scanner.position);
	}
	
	/**
	 * Parses ClassDeclaration.
	 *	<p>
	 *Original: <br><u>ClassDeclaration</u> ::= <b>class </b><i>id</i> <b>{</b> (<u>FieldDeclaration</u>|<u>MethodDeclaration</u>)* <b>}</b></p>
	 *<p> Note: Got rid of Field Declaration and Method Declaration</p>
	 *
	 *<p> New: <br> <u>ClassDeclaration</u> ::= <b>class </b><i>id</i> <b>{</b> (<u>Visibility</u> <u>Access</u>((<u>Type</u> <i>id</i>) | ((<u>Type</u> | <b>void</b>)<i> id</i> <b>(</b> <u>ParameterList</u>? <b>)</b> <b>{</b> <u>Statement</u>* <b>}</b>))* <b>}</b></p>
	 * @throws SyntaxError the syntax error
	 */
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		accept(TokenKind.clazz);
		String cn;
		cn=currentToken.spelling;//classname
		accept(TokenKind.id);
		accept(TokenKind.lcurly);
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList(); 
		while(currentToken.kind!=TokenKind.rcurly){ // constructing MemberDecl first...
			boolean isVis=parseVisibility();//laying out all i need for either field or method decl
			boolean isAccess=parseAccess();
			Type t = null;
			String name;
			ParameterDeclList params;
			StatementList statements = new StatementList();
			if(currentToken.kind==TokenKind.voyd){//method declaration
				t= new BaseType(TypeKind.VOID,scanner.position); //save off void type
				acceptIt();
				name=currentToken.spelling; //save off name
				accept(TokenKind.id);
				accept(TokenKind.lparen);
				if(currentToken.kind!=TokenKind.rparen){
					params=parseParameterList(); //get param list
				}
				accept(TokenKind.rparen);
				accept(TokenKind.lcurly);
				while(currentToken.kind!=TokenKind.rcurly){
					statements.add(parseStatement());
				}
				accept(TokenKind.rcurly); //taking rcurly
				MemberDecl member = new FieldDecl(isVis,isAccess,t,name,scanner.position);
				MethodDecl method = new MethodDecl(member, params, statements, scanner.position);
				mdl.add(method);
			}
			else{ //don't know yet
				t=parseType();
				name=currentToken.spelling;
				accept(TokenKind.id);
				if(currentToken.kind==TokenKind.semicol){//field declaration
					FieldDecl field= new FieldDecl(isVis,isAccess,t,name,scanner.position);
					fdl.add(field);
					acceptIt();
				}
				else if(currentToken.kind==TokenKind.lparen){//method declaration
					acceptIt();
					if(currentToken.kind!=TokenKind.rparen){
						params=parseParameterList();
					}
					accept(TokenKind.rparen);
					accept(TokenKind.lcurly);
					while(currentToken.kind!=TokenKind.rcurly){
						statements.add(parseStatement());
					}
					acceptIt(); //taking rcurly
				}
				else parseError("Expected lparen or semicol but found "+currentToken.kind+"\n Postion: "+scanner.position.toString());
				
			}
		}
		acceptIt();  //accepting last rcurly
		ClassDecl c = new ClassDecl(cn, fdl, mdl, scanner.position);
	}

	
	/**
	 * Parses Visibility.
	 * 
	 *<p> <u>Visibility</u> ::= (<b>public</b>|<b>private</b>)?</p>
	 *if it isprivate, return true, else false, will consume any pub or priv tokens
	 * @throws SyntaxError the syntax error
	 */
	private boolean parseVisibility() throws SyntaxError {
		
				
		if(currentToken.kind==TokenKind.priv){
			acceptIt();
			return true;
		}
		if(currentToken.kind==TokenKind.pub)acceptIt();
			return false;
	}
	/**
	 * Parses Access.
	 * 
	 *<p> <u>Access</u> ::= <b>static</b> ?</p>
	 * if kind is static return true else false
	 * @throws SyntaxError the syntax error
	 */
	private boolean parseAccess() throws SyntaxError {
		if(currentToken.kind==TokenKind.statik){
			acceptIt();
			return true;
		}return false;
	
	}
	
	/**
	 * Parses Type.
	 *<p> <u>Type</u> ::= <b>int</b> | <b>boolean</b> | <i>id</i> | (<b>int</b>|<i>id</i>)<b>[]</b>
	 * @throws SyntaxError the syntax error
	 */
	private Type parseType() throws SyntaxError { //where I left off
		switch (currentToken.kind){
		case bool:
			acceptIt();
			return new BaseType(TypeKind.BOOLEAN,scanner.position);
		case interger:
		case id:
			Type temp; //making sure to grab potential element type
			if(currentToken.kind==TokenKind.interger){
				temp=new BaseType(TypeKind.INT,scanner.position);
			}
			else  temp = new ClassType(new Identifier(currentToken),scanner.position);
			acceptIt();
			if(currentToken.kind==TokenKind.lbrack){
			acceptIt();
			accept(TokenKind.rbrack);
			return new ArrayType(temp,scanner.position);
			}
			return temp;
		
			
		default: //shouldn't be hit
			parseError("Expecting type but found " +currentToken.toString()+"\n Postion: "+scanner.position.toString());
			return new BaseType(TypeKind.ERROR,scanner.position);
		}
		
	}
	
	/**
	 * Parses ParameterList.
	 *<p><u>ParameterList</u> ::= <u>Type</u> <i>id</i> (<b>,</b> <u>Type</u> <i>id</i>)*</p>
	 * @throws SyntaxError the syntax error
	 */
	private ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList toReturn=new ParameterDeclList();
		toReturn.add(new ParameterDecl(parseType(),currentToken.spelling,scanner.position));//grabbing first param...
		accept(TokenKind.id);
		while(currentToken.kind==TokenKind.comma){
			acceptIt();
			toReturn.add(new ParameterDecl(parseType(),currentToken.spelling,scanner.position)); //grabbing rest if exists
			accept(TokenKind.id);
		}
		return toReturn;
	}
	
	/**
	 * Parses ArgumentList.
	 * <p> <u>ArgumentList</u> ::= <u>Expression</u> (<b>,</b><u>Expression</u>)*<p>
	 * @throws SyntaxError the syntax error
	 */
	private ExprList parseArgumentList() throws SyntaxError {
		ExprList toReturn= new ExprList();
		ExprList.add(parseExpression());
		while(currentToken.kind==TokenKind.comma){
			acceptIt();
			ExprList.add(parseExpression());
		}
		return toReturn;
		
	}
	
	/**
	 * Parses Reference.
	 * <p>Note: requires refactor due to left recursion<p>
	 * <p>Original:<br>
	 * <u> Reference</u> ::= <u>Reference</u> <b>.</b> <i>id</i> | (<b>this</b> | <i>id</i>)
	 *</p>
	 *<p> New: <br>
	 *<u>Reference</u> ::= (<b>this</b> | <i>id</i>) (<b>.</b><i>id</i>)*</p>
	 * @throws SyntaxError the syntax error
	 */
	private Reference parseReference() throws SyntaxError { //might be problem area
		Reference root = null;
		if(currentToken.kind==TokenKind.thiz||currentToken.kind==TokenKind.id){
			if(currentToken.kind==TokenKind.thiz)root=new ThisRef(scanner.position);
			else root=new IdRef(new Identifier(currentToken),scanner.position);
			acceptIt();
		}else parseError("was expecting this or an id and found "+currentToken.toString()+"\n");
		while(currentToken.kind==TokenKind.dot){
			acceptIt();
			root=new QualifiedRef(root,new Identifier(currentToken),scanner.position);
			accept(TokenKind.id);
			
		}
		return root;
		
	}
	
	/**
	 * Parses the array reference.
	 *<p><u>ArrayReference</u> ::= <i>id</i> <b>[</b> <u>Expression</u> <b>]</b></p>
	 * @throws SyntaxError the syntax error
	 */
	private IndexedRef parseArrayReference() throws SyntaxError {
		IdRef idr = new IdRef(new Identifier(currentToken),scanner.position);
		accept(TokenKind.id);
		accept(TokenKind.lbrack);
		Expression expr = parseExpression();
		accept(TokenKind.rbrack);
		return new IndexedRef(idr,expr,scanner.position);
	}
	
	/**
	 * Parses the Statement.
	 *<p><u>Statement</u> ::=<br> <b>{</b><u>Statement</u>*<b>}</b><br>
	 *| <b>int</b> | <b>boolean</b> | <i>id</i> | (<b>int</b>|<i>id</i>)<b>[]</b> <i>id</i> <b>=</b> <u>Expression</u><b>;</b><br>
	 *| (<b>this</b> | <i>id</i>) (<b>.</b><i>id</i>)* <b>=</b> <u>Expression</u><b>;</b><br>
	 *|  <i>id</i> <b>[</b> <u>Expression</u> <b>] =</b> <u>Expression</u><b>;</b><br>
	 *| (<b>this</b> | <i>id</i>) (<b>.</b><i>id</i>)*<b> (</b><u>ArgumentList</u>?<b>);</b><br>
	 *| <b>return</b> <u>Expression</u>?<b>;</b><br>
	 *| <b>if (</b><u>Expression</u><b>)</b> <u>Statement</u> (<b>else</b> <u>Statement</u>)?<br>
	 *| <b>while (</b><u>Expression</u><b>)</b> <u>Statement</u></p>
	 *
	 * @throws SyntaxError the syntax error
	 */
	private void parseStatement() throws SyntaxError {
		switch(currentToken.kind){
		case lcurly:
			acceptIt();
			while(currentToken.kind!=TokenKind.rcurly){
			parseStatement();	
			}
			acceptIt();//getting rcurly
			break;
		case bool:
			acceptIt();
			accept(TokenKind.id);
			accept(TokenKind.equals);
			parseExpression();
			accept(TokenKind.semicol);
			break;
		case ret:
			acceptIt();
			if(currentToken.kind!=TokenKind.semicol)parseExpression();
			accept(TokenKind.semicol);
			break;
		case iff:
			acceptIt();
			accept(TokenKind.lparen);
			parseExpression();
			accept(TokenKind.rparen);
			parseStatement();
			if(currentToken.kind==TokenKind.elsz){
				acceptIt();
				parseStatement();
			}
			break;
		case wile:
			acceptIt();
			accept(TokenKind.lparen);
			parseExpression();
			accept(TokenKind.rparen);
			parseStatement();
			break;
		case thiz:
			acceptIt();
			while(currentToken.kind==TokenKind.dot){
				acceptIt();
				accept(TokenKind.id);
			}
			if(currentToken.kind==TokenKind.equals){ //=Expression;
				acceptIt();
				parseExpression();
				accept(TokenKind.semicol);
				break;
			}
			else if(currentToken.kind==TokenKind.lparen){//(ArgumentList?)
				acceptIt();
				if(currentToken.kind!=TokenKind.rparen)parseArgumentList();
				accept(TokenKind.rparen);
				accept(TokenKind.semicol);
				break;
			}
			else {
				parseError("Expecting term but found "+currentToken);
				break;
			}
		case interger:
			acceptIt();
			if(currentToken.kind==TokenKind.lbrack){
				acceptIt();
				accept(TokenKind.rbrack);
			}
			accept(TokenKind.id);
			accept(TokenKind.equals);
			parseExpression();
			accept(TokenKind.semicol);
			break;
		case id:	//this one is nasty
			acceptIt();
			
			switch(currentToken.kind){
			case id:
				acceptIt();
				accept(TokenKind.equals);
				parseExpression();
				accept(TokenKind.semicol);
				return;
			case lbrack:
				acceptIt();
				if(currentToken.kind==TokenKind.rbrack){//id[] id = Expression; 
					acceptIt();
					accept(TokenKind.id);
					accept(TokenKind.equals);
					parseExpression();
					accept(TokenKind.semicol);
					return;
				}
				//id[Expression]=Expression;
				parseExpression();
				accept(TokenKind.rbrack);
				accept(TokenKind.equals);
				parseExpression();
				accept(TokenKind.semicol);
				return;
			default://else
				while(currentToken.kind==TokenKind.dot){
					acceptIt();
					accept(TokenKind.id);
				}
				if(currentToken.kind==TokenKind.equals){ //=Expression;
					acceptIt();
					parseExpression();
					accept(TokenKind.semicol);
					return;
				}
				else if(currentToken.kind==TokenKind.lparen){//(ArgumentList?)
					acceptIt();
					if(currentToken.kind!=TokenKind.rparen)parseArgumentList();
					accept(TokenKind.rparen);
					accept(TokenKind.semicol);
					return;
				}
				else {
					parseError("Expecting term but found "+currentToken);
				}
				
			}
			
		default:
			parseError("Expecting term but found "+currentToken);
		}
		
			
		
		
	}
	
	/**
	 * Parses the expression.
	 *<p><u>Expression</u> ::= <br>
	 *(<b>this</b> | <i>id</i>) (<b>.</b><i>id</i>)* ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)?<br>
	 *|  <i>id</i> <b>[</b> <u>Expression</u> <b>]</b> ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)?<br>
	 *| (<b>this</b> | <i>id</i>) (<b>.</b><i>id</i>)*<b>(</b><u>ArgumentList</u>?<b>)</b> ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)?<br>
	 *| (<i>unop</i>|<i>bunop</i>) <u>Expression</u> ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)?<br>
	 *| <b>(</b><u>Expression</u><b>)</b> ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)?<br>
	 *| <i>num</i> ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)?<br>
	 *| <b>true</b> ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)? <br>
	 *| <b>false</b> ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)?<br>
	 *| <b>new</b> (<i>id</i><b>()</b> | <b>int [</b><u>Expression</u><b>]</b> | <i>id</i> <b>[</b><u>Expression</u><b>]</b>) ((<i>binop</i>|<i>bunop</i>) <u>Expression</u>)?<br>
	 *|  
	 *
	 * @throws SyntaxError the syntax error
	 */
	private void parseExpression() throws SyntaxError {
		switch(currentToken.kind){
		case num:
		case tru:
		case fals:
			acceptIt();
			break;
		case unop:
		case bunop:
			acceptIt();
			parseExpression();
			break;
		case lparen:
			acceptIt();
			parseExpression();
			accept(TokenKind.rparen);
			break;
		case nu:
			acceptIt();
			if(currentToken.kind==TokenKind.interger){ //int[Expression]
				acceptIt();
				accept(TokenKind.lbrack);
				parseExpression();
				accept(TokenKind.rbrack);
				break;
			}
			else{ //starts with id
				accept(TokenKind.id);
				if(currentToken.kind==TokenKind.lparen){
					acceptIt();
					accept(TokenKind.rparen);
					break;
				}
				else{
					accept(TokenKind.lbrack);
					parseExpression();
					accept(TokenKind.rbrack);
					break;
				}
			}
		case thiz:
			acceptIt();
			while(currentToken.kind==TokenKind.dot){
				acceptIt();
				accept(TokenKind.id);
			}
			if(currentToken.kind!=TokenKind.lparen)break;
			accept(TokenKind.lparen);
			if(currentToken.kind!=TokenKind.rparen)parseArgumentList();
			accept(TokenKind.rparen);
			break;
		case id:
			acceptIt();
			if(currentToken.kind==TokenKind.lbrack){//id[Expression]
				acceptIt();
				parseExpression();
				accept(TokenKind.rbrack);
				break;
			}
			while(currentToken.kind==TokenKind.dot){
				acceptIt();
				accept(TokenKind.id);
			}
			if(currentToken.kind!=TokenKind.lparen)break;
			accept(TokenKind.lparen);
			if(currentToken.kind!=TokenKind.rparen)parseArgumentList();
			accept(TokenKind.rparen);
			break;
			
		default:
			parseError("Expecting term but found "+currentToken);
		}
		if(currentToken.kind==TokenKind.binop||currentToken.kind==TokenKind.bunop){
			acceptIt();
			parseExpression();
			return;
		}
		
	}
	
	/**
	 * Accept it (unconditional)
	 *
	 * @throws SyntaxError the syntax error
	 */
	private void acceptIt() throws SyntaxError{
		accept(currentToken.kind);
	}
	
	/**
	 * Accept.
	 *
	 * @param expectedTokenKind the expected token kind
	 * @throws SyntaxError the syntax error
	 */
	private void accept(TokenKind expectedTokenKind) throws SyntaxError{
		if(currentToken.kind==expectedTokenKind){
			currentToken=scanner.scan();
			while(currentToken.kind==TokenKind.comment)currentToken=scanner.scan();
		}
		else{
			parseError("expecting '"+expectedTokenKind+"' but found '" +
		currentToken.kind+ "'");
		}
	}
	
	/**
	 * Parses the error.
	 *
	 * @param e the e
	 * @throws SyntaxError the syntax error
	 */
	private void parseError(String e) throws SyntaxError {
		reporter.reportError("Parse error: "+e+"\n Postion: "+scanner.position.toString());
		throw new SyntaxError();
	}

}
