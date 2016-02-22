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
	public AST parse(){
		currentToken=scanner.scan();
		while(currentToken.kind==TokenKind.comment)currentToken=scanner.scan();
		try{
			return parseProgram();
		}
		catch(SyntaxError e){
			return null;
		}
	}
	
	/**
	 * Parses Program.
	 * <p>
	 *	<u>Program</u> ::= (<u>ClassDeclaration</u>)*<b>eot</b> </p>
	 * @throws SyntaxError the syntax error
	 */
	
	private Package parseProgram() throws SyntaxError {
		ClassDeclList c= new ClassDeclList();
		while(currentToken.kind!=TokenKind.eot){
			c.add(parseClassDeclaration());
		}
		accept(TokenKind.eot);
		Package p= new Package(c,scanner.position);
		return p;
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
			Type t;
			String name;
			ParameterDeclList params=new ParameterDeclList();
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
					MemberDecl member = new FieldDecl(isVis,isAccess,t,name,scanner.position);
					MethodDecl method = new MethodDecl(member, params, statements, scanner.position);
					mdl.add(method);
					acceptIt(); //taking rcurly
				}
				else parseError("Expected lparen or semicol but found "+currentToken.kind+"\n Postion: "+scanner.position.toString());
				
			}
		}
		acceptIt();  //accepting last rcurly
		ClassDecl c = new ClassDecl(cn, fdl, mdl, scanner.position);
		return c;
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
		toReturn.add(parseExpression());
		while(currentToken.kind==TokenKind.comma){
			acceptIt();
			toReturn.add(parseExpression());
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
		Reference root;
		if(currentToken.kind==TokenKind.thiz||currentToken.kind==TokenKind.id){
			if(currentToken.kind==TokenKind.thiz)root=new ThisRef(scanner.position);
			else root=new IdRef(new Identifier(currentToken),scanner.position);
			acceptIt();
		}else {
			parseError("was expecting this or an id and found "+currentToken.toString()+"\n");
			return null;
		}
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
	private Statement parseStatement() throws SyntaxError {
		switch(currentToken.kind){
		case lcurly: //block statement
			acceptIt();
			StatementList sllcurly = new StatementList();
			while(currentToken.kind!=TokenKind.rcurly){
			sllcurly.add(parseStatement());	
			}
			acceptIt();//getting rcurly
			return new BlockStmt(sllcurly,scanner.position);
		case bool: // vardecl statement
			acceptIt();
			VarDecl vdbool = new VarDecl(new BaseType(TypeKind.BOOLEAN,scanner.position),currentToken.spelling,scanner.position);
			accept(TokenKind.id);
			accept(TokenKind.equals);
			Expression ebool = parseExpression();
			accept(TokenKind.semicol);
			return new VarDeclStmt(vdbool,ebool,scanner.position);
		case ret: //return statement COME BACK....
			acceptIt();
			Expression eret=null;
			boolean haseret=false;
			if(currentToken.kind!=TokenKind.semicol){
				eret=parseExpression();
				haseret=true;
			}
			accept(TokenKind.semicol);
			return new ReturnStmt(eret,scanner.position);
		case iff: //IfStmt
			acceptIt();
			accept(TokenKind.lparen);
			Expression eiff = parseExpression();
			accept(TokenKind.rparen);
			Statement s0iff =parseStatement();
			if(currentToken.kind==TokenKind.elsz){
				acceptIt();
				Statement s1iff=parseStatement();
				return new IfStmt(eiff,s0iff,s1iff,scanner.position);
			}
			return new IfStmt(eiff,s0iff,scanner.position);
		case wile: //while statement
			acceptIt();
			accept(TokenKind.lparen);
			Expression ewile=parseExpression();
			accept(TokenKind.rparen);
			Statement swile=parseStatement();
			return new WhileStmt(ewile,swile,scanner.position);
		case thiz: //either assignStmt or CallStmt
			acceptIt();
			Reference rootthiz = new ThisRef(scanner.position); //will always start with reference
			while(currentToken.kind==TokenKind.dot){
				acceptIt();
				rootthiz=new QualifiedRef(rootthiz,new Identifier(currentToken),scanner.position);
				accept(TokenKind.id);
			}
			//now I have full reference made
			if(currentToken.kind==TokenKind.equals){ //=Expression; ie assignStmt
				acceptIt();
				Expression ethiz =parseExpression();
				accept(TokenKind.semicol);
				return new AssignStmt(rootthiz,ethiz,scanner.position);
			}
			else if(currentToken.kind==TokenKind.lparen){//(ArgumentList?) ie callStmt
				acceptIt();
				ExprList elthiz = new ExprList();
				if(currentToken.kind!=TokenKind.rparen){
					elthiz=parseArgumentList();
				}
				accept(TokenKind.rparen);
				accept(TokenKind.semicol);
				return new CallStmt(rootthiz,elthiz,scanner.position);
			}
			else {
				parseError("Expecting term but found "+currentToken);
				return null; //don't care about tree anymore...
			}
		case interger: //VarDeclStmt
			acceptIt();
			Type tinterger;
			if(currentToken.kind==TokenKind.lbrack){
				acceptIt();
				accept(TokenKind.rbrack);
				tinterger=new ArrayType(new BaseType(TypeKind.INT,scanner.position),scanner.position);
			}
			else {tinterger=new BaseType(TypeKind.INT,scanner.position);}
			VarDecl vdinterger=new VarDecl(tinterger,currentToken.spelling,scanner.position);
			accept(TokenKind.id);
			accept(TokenKind.equals);
			Expression einterger=parseExpression();
			accept(TokenKind.semicol);
			return new VarDeclStmt(vdinterger,einterger,scanner.position);
		case id://this one is nasty		can be varDeclStmt,assignStmt,IxAssignStmt,or CallStmt
			Token idroot = currentToken;
			acceptIt();
			
			switch(currentToken.kind){
			case id: //varDeclStmt
				Type tidid=new ClassType(new Identifier(idroot),scanner.position);
				VarDecl vdidid=new VarDecl(tidid,currentToken.spelling,scanner.position);
				acceptIt();
				accept(TokenKind.equals);
				Expression eidid=parseExpression();
				accept(TokenKind.semicol);
				return new VarDeclStmt(vdidid,eidid,scanner.position);
			case lbrack://either varDeclStmt with arrayType, or IxAssignStmt
				acceptIt();
				Type tidlbrack;
				if(currentToken.kind==TokenKind.rbrack){//id[] id = Expression; we now know it is varDeclStmt
					acceptIt();
					tidlbrack=new ArrayType(new ClassType(new Identifier(idroot),scanner.position),scanner.position);
					VarDecl vdidlbrack=new VarDecl(tidlbrack,currentToken.spelling,scanner.position);
					accept(TokenKind.id);
					accept(TokenKind.equals);
					Expression eidlbrack=parseExpression();
					accept(TokenKind.semicol);
					return new VarDeclStmt(vdidlbrack,eidlbrack,scanner.position);
				}
				//id[Expression]=Expression; we now know this is IxAssignStmt
				IdRef idridlbrack=new IdRef(new Identifier(idroot),scanner.position);
				Expression eidlbrack=parseExpression();
				IndexedRef iridlbrack=new IndexedRef(idridlbrack,eidlbrack,scanner.position);
				accept(TokenKind.rbrack);
				accept(TokenKind.equals);
				Expression eidlbrack2=parseExpression();
				accept(TokenKind.semicol);
				return new IxAssignStmt(iridlbrack,eidlbrack2,scanner.position);
			default://else could be assignStmt, CallStmt
				Reference rootid=new IdRef(new Identifier(idroot),scanner.position);
				while(currentToken.kind==TokenKind.dot){
					acceptIt();
					rootid=new QualifiedRef(rootid,new Identifier(currentToken),scanner.position);
					accept(TokenKind.id);
				}
				//now have full reference in rootid
				if(currentToken.kind==TokenKind.equals){ //=Expression; now know it is assignStmt
					acceptIt();
					Expression eiddefault=parseExpression();
					accept(TokenKind.semicol);
					return new AssignStmt(rootid,eiddefault,scanner.position);
				}
				else if(currentToken.kind==TokenKind.lparen){//(ArgumentList?) //now know is callStmt
					acceptIt();
					ExprList eliddefault=new ExprList();
					if(currentToken.kind!=TokenKind.rparen){
						eliddefault=parseArgumentList();
					}
					accept(TokenKind.rparen);
					accept(TokenKind.semicol);
					return new CallStmt(rootid,eliddefault,scanner.position);
				}
				else {
					parseError("Expecting term but found "+currentToken);
					return null;//don't care about tree anymore
				}
				
			}
			
		default:
			parseError("Expecting term but found "+currentToken);
			return null;//don't care bout tree anyomore....
		}
		
			
		
		
	}
	
	private Expression parseExpression(){
		return ParseD();
	}
	private Expression ParseD(){
		Expression D = ParseC();
		while(currentToken.spelling.equals("||")){
			Token temp = currentToken;
			acceptIt();
			D=new BinaryExpr(new Operator(temp),D,ParseC(),scanner.position);
		}
		return D;
	}
	private Expression ParseC(){
		Expression C = ParseEq();
		while(currentToken.spelling.equals("&&")){
			Token temp = currentToken;
			acceptIt();
			C=new BinaryExpr(new Operator(temp),C,ParseEq(),scanner.position);
		}
		return C;
	}
	private Expression ParseEq(){
		Expression Eq = ParseRe();
		while(currentToken.spelling.equals("==")||currentToken.spelling.equals("!=")){
			Token temp = currentToken;
			acceptIt();
			Eq=new BinaryExpr(new Operator(temp),Eq,ParseRe(),scanner.position);
		}
		return Eq;
	}
	private Expression ParseRe(){
		Expression Re=ParseAdd();
		while(currentToken.spelling.equals("<=")||currentToken.spelling.equals("<")||currentToken.spelling.equals(">")||currentToken.spelling.equals(">=")){
			Token temp =currentToken;
			acceptIt();
			Re=new BinaryExpr(new Operator(temp),Re,ParseAdd(),scanner.position);
		}
		return Re;
	}
	private Expression ParseAdd(){
		Expression Add=ParseMult();
		while(currentToken.spelling.equals("+")||currentToken.spelling.equals("-")){
			Token temp =currentToken;
			acceptIt();
			Add=new BinaryExpr(new Operator(temp),Add,ParseMult(),scanner.position);
		}
		return Add;
	}
	private Expression ParseMult(){
		Expression Mult = ParseUnary();
		while(currentToken.spelling.equals("*")||currentToken.spelling.equals("/")){
			Token temp =currentToken;
			acceptIt();
			Mult=new BinaryExpr(new Operator(temp),Mult,ParseUnary(),scanner.position);
		}
		return Mult;
	}
	private Expression ParseUnary(){
		Expression E;
		if(currentToken.spelling.equals("-")||currentToken.spelling.equals("!")){
			Operator ounbun=new Operator(currentToken);
			acceptIt();
			E=new UnaryExpr(ounbun,ParseUnary(),scanner.position);
		}
		else E=ParseRest();
		return E;
	}
	/**
	 * Parses the expression. Note this now does rest due to precedence implementation
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
	private Expression ParseRest() throws SyntaxError {
		Expression e0=null;
		switch(currentToken.kind){
		//literalExpr
		case num:
			e0=new LiteralExpr(new IntLiteral(currentToken),scanner.position);
			acceptIt();
			break;
		case tru:
		case fals:
			e0=new LiteralExpr(new BooleanLiteral(currentToken),scanner.position);
			acceptIt();
			break;
		//UnaryExpr
		/*case unop:
		case bunop:
			Operator ounbun=new Operator(currentToken);
			acceptIt();
			e0=new UnaryExpr(ounbun,parseExpression(),scanner.position);
			break;*/
			//????? TODO
		case lparen: //not sure yet......will have to handle along with appending (binop|bunop Expression)?
			acceptIt();
			e0=parseExpression();
			accept(TokenKind.rparen);
			break;
		case nu: //either NewObjectExpr or NewArrayExpr
			acceptIt();
			if(currentToken.kind==TokenKind.interger){ //int[Expression] //NewArrayExpr
				acceptIt();
				accept(TokenKind.lbrack);
				e0=new NewArrayExpr(new BaseType(TypeKind.INT,scanner.position),parseExpression(),scanner.position);
				accept(TokenKind.rbrack);
				break;
			}
			else{ //starts with id
				Token nuid=currentToken;
				accept(TokenKind.id);
				if(currentToken.kind==TokenKind.lparen){ //NewObjectExpr
					acceptIt();
					accept(TokenKind.rparen);
					e0=new NewObjectExpr(new ClassType(new Identifier(nuid),scanner.position),scanner.position);
					break;
				}
				else{ //NewArrayExpr
					accept(TokenKind.lbrack);
					e0=new NewArrayExpr(new ClassType(new Identifier(nuid),scanner.position),parseExpression(),scanner.position);
					accept(TokenKind.rbrack);
					break;
				}
			}
		case thiz: //either RefExpr or CallExpr
			acceptIt();
			Reference rootthiz = new ThisRef(scanner.position); //will always start with reference
			while(currentToken.kind==TokenKind.dot){
				acceptIt();
				rootthiz=new QualifiedRef(rootthiz,new Identifier(currentToken),scanner.position);
				accept(TokenKind.id);
			}
			//now have current Reference
			if(currentToken.kind!=TokenKind.lparen){//RefExpr
				e0=new RefExpr(rootthiz,scanner.position);
				break;
			}
			//CallExpr
			accept(TokenKind.lparen);
			if(currentToken.kind!=TokenKind.rparen){
				e0=new CallExpr(rootthiz,parseArgumentList(),scanner.position);
			}
			accept(TokenKind.rparen);
			break;
		case id: //RefExpr or CallExpr again
			Reference rootid = new IdRef(new Identifier(currentToken),scanner.position); //will always start with reference
			IdRef temp= (IdRef) rootid;
			acceptIt();
			if(currentToken.kind==TokenKind.lbrack){//id[Expression] //RefExpr...but Indexed
				acceptIt();
				e0=new RefExpr(new IndexedRef(temp,parseExpression(),scanner.position),scanner.position);
				accept(TokenKind.rbrack);
				break;
			}
			while(currentToken.kind==TokenKind.dot){
				acceptIt();
				rootid=new QualifiedRef(rootid,new Identifier(currentToken),scanner.position);
				accept(TokenKind.id);
			}
			//now have full Ref
			if(currentToken.kind!=TokenKind.lparen){//Ref Expr, non-Indexed
				e0=new RefExpr(rootid,scanner.position);
				break;
			}
			accept(TokenKind.lparen);//CallExpr
			ExprList elid=new ExprList();
			if(currentToken.kind!=TokenKind.rparen){
				elid=parseArgumentList();
			}
			e0=new CallExpr(rootid,elid,scanner.position);
			accept(TokenKind.rparen);
			break;
			
		default:
			parseError("Expecting term but found "+currentToken);
			return null;//done....
		}
		return e0;
		
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
