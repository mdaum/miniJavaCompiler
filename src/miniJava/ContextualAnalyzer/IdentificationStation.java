package miniJava.ContextualAnalyzer;

import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.IDTable.SyntaxError;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;


public class IdentificationStation implements Visitor<IDTable,Object>{
	
	int levelPassCount;
	public MethodDecl mainMethod;//used for entry point
	ErrorReporter reporter;
	private ClassDecl currClass;
	private MethodDecl currMethod;
	private ClassDecl currQualifiedClass;
	boolean log;
	ArrayList<String>currDeclaredVars;
	boolean checkingMethodfromExpr;//
	boolean checkingMethodfromStmt;
	boolean inQRef;
	boolean hitMethod=false;//used in qualifiedRef
	ArrayList<Declaration> memberDecls=new ArrayList<Declaration>();
	boolean checkIncompleteRef; //for vardecl incompleteRef edge case
	public AST Decorate(AST ast, ErrorReporter reporter){ //drives identification process
		try{IDTable t = new IDTable(reporter);
			levelPassCount=0;
			mainMethod=null;
			checkingMethodfromExpr=false;
			checkingMethodfromStmt=false;
			checkIncompleteRef=false;
			this.reporter=reporter;
			this.log=false;
			currDeclaredVars=new ArrayList<String>();
			ast.visit(this, t);
			//t.printTable();
			return ast;
		}
		catch(SyntaxError e){
			return null;
		}
	}
	@Override
	public Object visitPackage(Package prog, IDTable arg) { //goal right now is to get down traversal....
		for (ClassDecl c: prog.classDeclList){
			addDeclaration(arg,c); //this should finish class level stuff
		}
		arg.openScope();//now in members
		for (ClassDecl c: prog.classDeclList){
			System.out.println(c.name+" Round 1");
			currClass=c;
			c.visit(this,arg);
		}
		levelPassCount++;//added fieldDecls
		for(ClassDecl c: prog.classDeclList){ 
			System.out.println(c.name+" Round 2");
			currClass=c;
			c.visit(this, arg);
		} //added methodDecls
		
		levelPassCount++;
		for(ClassDecl c: prog.classDeclList){ //shimmy down MethodDecls
			System.out.println(c.name+" Round 3");
			currClass=c;
			c.visit(this, arg);
		}
		if(mainMethod==null){
			reporter.reportError("*** Identification Error: no main method found.");
			throw new SyntaxError();
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, IDTable arg) {
		if(levelPassCount==0){
		for(FieldDecl fd: cd.fieldDeclList){ //visiting ie adding fields on first pass
			fd.c=cd;
			//System.out.println(fd.name);
			fd.visit(this, arg);
		}
		return null;
		}
		
		else if(levelPassCount==1){ //second pass, adding method decls
			for (MethodDecl d: cd.methodDeclList){
				//doSomething
				currMethod=d;
				d.c=cd;
				//System.out.println(d.name);
				addDeclaration(arg,d);
			}
			return null;
		}
		
		else{ //we shimmy down Method Decl ie visit
			//System.out.println("good so far..Starting to shimmy down MethodDecls");
			for(MethodDecl d: cd.methodDeclList){
				currMethod=d;
				currDeclaredVars.clear();//need to clear declared vars...
				d.visit(this, arg);
			}
			return null;
		}
		
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, IDTable arg)  { //here we add the decl
		//System.out.println("hit visitFieldDecl");
		addDeclaration(arg,fd);	
		fd.type.visit(this, arg);
		//System.out.println("type ok");
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, IDTable arg) { //now we visit param and open up scope
		//starting...
		//  NOT TESTED
		md.type.visit(this, arg);//first check return type
		//System.out.println("Method Type good");
		arg.openScope();//now in params, level 3
		for(ParameterDecl pd : md.parameterDeclList){
			pd.visit(this, arg);
		}
		if(!md.isPrivate&&md.isStatic&&md.type.typeKind==TypeKind.VOID&&md.name.equals("main")){
			if(md.parameterDeclList.size()==1){
				if(md.parameterDeclList.get(0).type.typeKind==TypeKind.ARRAY){
					ArrayType outer = (ArrayType) md.parameterDeclList.get(0).type;
					if(outer.eltType.typeKind==TypeKind.UNSUPPORTED){//String
						//we have main method
						if(mainMethod!=null){
							reporter.reportError("*** Identification Error: two or more main methods!!");
							throw new SyntaxError();
						}
						else mainMethod=md;
					}
				}
			}
		}
		arg.openScope();//now in local vars, level 4
		int numHit=0;
		for(Statement s: md.statementList){
			//System.out.println(s.posn);
			s.visit(this, arg);
			numHit++;
			if(numHit==md.statementList.size()){//on last statement
				if(!(s instanceof ReturnStmt)){
					if(md.type.typeKind!=TypeKind.VOID){
						reporter.reportError("*** Identification Error: last statement in non-void method is not a return statement! Position: "+s.posn);
						throw new SyntaxError();
					}//otherwise we add a blank return stmt
					md.statementList.add(new ReturnStmt( null,s.posn));
					break;
				}
			}
		}
		arg.closeScope();//now at params level 3
		arg.closeScope();//closing params, now at member scope level 2
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, IDTable arg) {
		
		pd.type.visit(this, arg);//checking type
		//System.out.println(pd.name+" of type "+pd.type.typeKind.toString()+" is ok.");
		addDeclaration(arg, pd);
		currDeclaredVars.add(pd.name);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, IDTable arg) {//will handle in varDeclStatement
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, IDTable arg) { //don't care doesn't point anywhere
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, IDTable arg) { //Identifier here....
		Identifier i = type.className;
		if(i.spelling.equals("_PrintStream")){ //no printstream....question on this pending
			reporter.reportError("*** Identification error:  _PrintStream is not allowed to be accessed! Position: "+type.posn);
			throw new SyntaxError();
		}
		Declaration d = arg.table.get(1).get(i.spelling);
		if(!(d instanceof ClassDecl)){
			//then check predefined
			d=arg.table.get(0).get(i.spelling);
			if(!(d instanceof ClassDecl)){//not in predefined either
				reporter.reportError("*** Identification error:  "+i.spelling+" cannot be resolved to a type "+ "Position: "+type.posn);
				throw new SyntaxError();
			}
			if(d.name.equals("String"))type.typeKind=TypeKind.UNSUPPORTED;//predefined String Class
		}
		i.d=d;
		LinkDump(i,i.d,log);
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, IDTable arg) {
		type.eltType.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, IDTable arg) {
		arg.openScope();//new scope added
		for(Statement s: stmt.sl){
			s.visit(this, arg);
		}
		arg.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, IDTable arg) {//working on this...
		stmt.varDecl.type.visit(this, arg);
		addDeclaration(arg, stmt.varDecl);//add this before going into the expression....handles int x=x+2;
		checkIncompleteRef=true;
		stmt.initExp.visit(this, arg);//if calling idRef we have flag now set...
		checkIncompleteRef=false;
		currDeclaredVars.add(stmt.varDecl.name);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, IDTable arg) {
		stmt.val.visit(this, arg);
		stmt.ref.visit(this, arg);
		if(stmt.ref.d instanceof MethodDecl){
			reporter.reportError("*** Identification error:  Cannot assign a method declaration! Position: "+stmt.ref.posn);
			throw new SyntaxError();
		}
		if(stmt.ref.d instanceof FieldDecl && ((FieldDecl)(stmt.ref.d)).isArrayLength){
			reporter.reportError("*** Identification error: Cannot assign the length of an array! Position: "+stmt.ref.posn);
			throw new SyntaxError();
		}
/*		if(stmt.val instanceof RefExpr){ //wanna hand off pointer
			stmt.ref.d=((RefExpr)stmt.val).ref.d;
			LinkDump(stmt.ref,stmt.ref.d,log);
		}*/
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, IDTable arg) {//questions here!!
		stmt.val.visit(this, arg);
		stmt.ixRef.visit(this, arg);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, IDTable arg) {
		checkingMethodfromStmt=true;
		stmt.methodRef.visit(this, arg);
		checkingMethodfromStmt=false;
		if(!(stmt.methodRef.d instanceof MethodDecl)){
			reporter.reportError("*** IdentificationError: not using a method in a call! Position: "+stmt.posn);
			throw new SyntaxError();
		}
		for(Expression e : stmt.argList){
			e.visit(this, arg);
		}
		return null;
	}
	
	@Override
	public Object visitReturnStmt(ReturnStmt stmt, IDTable arg) {
		if(stmt.returnExpr!=null && currMethod.type.typeKind.equals(TypeKind.VOID)){
			reporter.reportError("*** Identification error:  method "+currMethod.name+" should not have a return type! Position: "+stmt.posn);
			throw new SyntaxError();
		}
		if(stmt.returnExpr!=null)stmt.returnExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, IDTable arg) {
		stmt.cond.visit(this, arg);
		if(stmt.thenStmt instanceof VarDeclStmt){
			reporter.reportError("*** Identification error:  VarDeclStmt cannot be the only statement following conditional Position: "+stmt.thenStmt.posn);
			throw new SyntaxError();
		}
		else stmt.thenStmt.visit(this, arg);
		if(stmt.elseStmt instanceof VarDeclStmt){
			reporter.reportError("*** Identification error:  VarDeclStmt cannot be the only statement following conditional Position: "+stmt.elseStmt.posn);
			throw new SyntaxError();
		}
		else if(stmt.elseStmt != null)stmt.elseStmt.visit(this, arg);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, IDTable arg) {
		// NOT TESTED
		stmt.cond.visit(this, arg);
		if(stmt.body instanceof VarDeclStmt){
			reporter.reportError("*** Identification error:  VarDeclStmt cannot be the only statement following conditional Position: "+stmt.body.posn);
			throw new SyntaxError();
		}
		else stmt.body.visit(this, arg);
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, IDTable arg) {//don't care about op right?
		// NOT TESTED
		expr.expr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, IDTable arg) {
		//  NOT TESTED
		expr.left.visit(this, arg);
		expr.right.visit(this, arg);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, IDTable arg) {
		//NOT TESTED
		expr.ref.visit(this, arg);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, IDTable arg) {
		//NOT TESTED
		checkingMethodfromExpr=true;
		expr.functionRef.visit(this, arg);
		checkingMethodfromExpr=false;
		if(!(expr.functionRef.d instanceof MethodDecl)){
			reporter.reportError("*** IdentificationError: not using a method in a call! Position: "+expr.posn);
			throw new SyntaxError();
		}
		for(Expression e : expr.argList){
			e.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, IDTable arg) { //don't care?
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, IDTable arg) { 
		//  NOT TESTED
		expr.classtype.visit(this, arg);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, IDTable arg) {
		//NOT TESTED
		expr.eltType.visit(this, arg);
		expr.sizeExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitQualifiedRef(QualifiedRef q, IDTable arg) { //have no clue yet ughhhh
		inQRef=true;
		
		RefKind parent=(RefKind)q.ref.visit(this, arg);
		inQRef=true;
		if(q.ref.d!=null){
			if(q.ref.d.type instanceof ArrayType){
				if(!q.id.spelling.equals("length")){
				reporter.reportError("*** Identification Error: cannot access an array member! Position "+q.posn);
				throw new SyntaxError();
				}
				FieldDecl len = new FieldDecl(false, false, new BaseType(TypeKind.INT, new SourcePosition()), "length", new SourcePosition());
				len.isArrayLength=true;
				q.id.d=len;
				q.d=len;
				return RefKind.ArrayLength;
			}
			else if(q.ref.d.type instanceof BaseType){
				reporter.reportError("*** Identification Error: not referencing "+q.id.spelling+" from an instance of a class Position: "+q.posn);
				throw new SyntaxError();
			}
			else{//ClassType
					
				if(parent==RefKind.Static){
					currQualifiedClass=(ClassDecl)q.ref.d;
					q.id.visit(this,arg);
					if(q.id.d!=null){
						MemberDecl m = (MemberDecl)q.id.d;
						if(m.isPrivate||!(m.isStatic)){
							reporter.reportError("*** Identification Error: cannot access class member "+q.id.spelling+" b/c it is either not static or it is private. Position: "+q.id.posn);
							throw new SyntaxError();
						}
					}
					currQualifiedClass=null;
				}
				if(parent==RefKind.ArrayLength){
					reporter.reportError("*** Identification Error: cannot access ArrayLength. Position: "+q.id.posn);
					throw new SyntaxError();
				}
				if(parent==RefKind.This){
					currQualifiedClass=currClass;
					q.id.visit(this, arg);
				}
				if(parent==RefKind.Instance){ //check private here
					currQualifiedClass=(ClassDecl) arg.retrieve(((ClassType)q.ref.d.type).className.spelling,currClass,currMethod,currQualifiedClass);
					q.id.visit(this,arg);//come back here
					if(q.id.d!=null){
						MemberDecl m=(MemberDecl) q.id.d;
						if(m.isPrivate && currQualifiedClass!=currClass){
							reporter.reportError("*** Identification Error: Cannot access private member of class "+currQualifiedClass.name+" Position: "+q.posn);
						throw new SyntaxError();
						}
					}
				}
				currQualifiedClass=null;
			}
		}
		q.d=q.id.d;
		if(q.d instanceof MethodDecl &&!checkingMethodfromExpr&&!checkingMethodfromStmt){
			reporter.reportError("*** Identification Error: Referencing Method "+q.d.name+" without invokation! Position: "+q.id.posn);
			throw new SyntaxError();
		}
		if(q.d!=null)LinkDump(q,q.id.d,log); //this is it!
		inQRef=false;
		return RefKind.Instance;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, IDTable arg) {//can do this it just passes along?
		//  NOT TESTED
		ref.indexExpr.visit(this, arg);
		ref.idRef.visit(this, arg);
		ref.d=ref.idRef.d;
		LinkDump(ref,ref.d,log);
		return RefKind.Instance;
	}

	@Override
	public Object visitIdRef(IdRef ref, IDTable arg) {//will deal with id not called in qualified here....
		String name=ref.id.spelling;
		Declaration d=arg.retrieve(name,currClass,currMethod,currQualifiedClass);
		if(d==null){
			reporter.reportError("*** Identification error:  idRef "+ref.id.spelling+" cannot be resolved, may not be undeclared Position: "+ref.posn);
			throw new SyntaxError();
		}
		if(d instanceof LocalDecl){ //scope 3+
			if(!currDeclaredVars.contains(d.name)){
				reporter.reportError("*** Identification error:  you cannot reference a variable that is currently being declared Position: "+d.posn);
				throw new SyntaxError();
			}
			ref.d=d;
			ref.id.d=d;
			LinkDump(ref.id,ref.id.d,log);
			LinkDump(ref,ref.d,log);
			return RefKind.Instance;
		}
		if(d instanceof MemberDecl){ //scope 2
			MemberDecl member=(MemberDecl)d;
			if(currMethod.isStatic&&!(member.isStatic)){//static check...will check qualified part in qualified ref visit
				reporter.reportError("*** Identification error:  cannot reference non-static member "+member.name+" from static method "+currMethod.name+" Position: "+ref.posn);
				throw new SyntaxError();
			}
			//we should be good at this point
			ref.d=d;
			ref.id.d=d;
			LinkDump(ref.id,ref.id.d,log);
			LinkDump(ref,ref.d,log);
			return RefKind.Instance;
		}
		if(d instanceof ClassDecl){//classname w/o type, but if from varDecl you have error
			if(checkIncompleteRef&&!inQRef){
				reporter.reportError("*** Identification error: cannot just use a classname in varDecl! Position: "+ref.posn);
				throw new SyntaxError();
			}
			ref.d=d;
			ref.id.d=d;
			LinkDump(ref.id,ref.id.d,log);
			LinkDump(ref,ref.d,log);
			return RefKind.Static;
		}
		else{//we got a problem
			reporter.reportError("*** Identification error:  impossible??!!! cannot reach");
			throw new SyntaxError();
			
		}
		
	}

	@Override
	public Object visitThisRef(ThisRef ref, IDTable arg) {
		ref.d=currClass;
		Token ClassToken= new Token(TokenKind.id,currClass.name,currClass.posn);
		ref.d.type=new ClassType(new Identifier(ClassToken,ClassToken.posn),ClassToken.posn);//for type checking...
		((ClassType)ref.d.type).className.d=ref.d;
		LinkDump(ref,ref.d,log);
		return RefKind.This;
	}

	@Override
	public Object visitIdentifier(Identifier id, IDTable arg) {//Only visiting from qualifiedRef
		Declaration d = arg.retrieve(id.spelling,2,currClass,currMethod,currQualifiedClass);
		if(d==null){
			reporter.reportError("*** Identification Error: cannot find id "+id.spelling+" in scoped table! Position: "+id.posn);
			throw new SyntaxError();
		}
		else if(!(d instanceof MemberDecl)||((MemberDecl)d).c!=currQualifiedClass){
			reporter.reportError("*** Identification Error: not referencing member "+ d.name+" of class "+currQualifiedClass.name+" or class does not exist! Position: "+id.posn);
			throw new SyntaxError();
		}
		else {
			id.d=d;
			LinkDump(id,d,log);
		}
		
		return null;
	}

	@Override
	public Object visitOperator(Operator op, IDTable arg) { //don't care
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, IDTable arg) { //don't care
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, IDTable arg) { //don't care
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, IDTable arg) { //don't care
		return null;
	}
	public void addDeclaration(IDTable table, Declaration declaration)throws SyntaxError {
		try {
			table.enter(declaration);
		} catch (Error e) {
			
		}
	}
	class SyntaxError extends Error{
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID=1L;
	}
	
	public void LinkDump(Identifier i, Declaration d,boolean doit){ //debugging purposes
		if(doit){
		System.out.print("\n ====LINK ALERT==== \n");
		System.out.println(i.spelling+" at "+"\nPosition "+i.posn+"\n is now linked to "+d.toString()+" "+d.name+" at \n Position: "+d.posn);	
		System.out.println("====END====");
		}
	}
	public void LinkDump(Reference r, Declaration d,boolean doit){
		if(doit){
		System.out.print("\n ====LINK ALERT==== \n");
		System.out.println(r.toString()+" at "+"\nPosition "+r.posn+"\n is now linked to "+d.toString()+" "+d.name+" at \n Position: "+d.posn);
		System.out.println("====END====");
		}
	}

}

