package miniJava.ContextualAnalyzer;

import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.IDTable.SyntaxError;


public class IdentificationStation implements Visitor<IDTable,Object>{
	/*General Notes for traversal:
	Look at DisplayAST.java, super helpful stuff there.
	So we need to make a data structure (maybe array of hash maps?) in order to track our id process
	openscope and closescope can be choosing which hash map we are messing with
	then we can shimmy down AST just like DisplayAST and we can start populating table instead of printing..
	IMPORTANT: Check if this traverses in a way that can handle local decl vs class and member decls!!
	for above: check this....https://en.wikipedia.org/wiki/Tree_traversal#In-order
	UPDATE: level-order for two levels, because the contents of methods should be read in order
	linking should happen on this pass
	this identification station will implemement Visitor<idtable structure, Object>
	so, every visit method will look like: public Object visitXXX(XXX name, idtable structure) and will return null
	pass the idtable down each level of traversal and it will become populated*/
	
	
	/*
	 Running questions:
	 1. For fields and params do I need to enter in all decls b4 checking type of each? can't i do it sequentially??
	 sequentially for now...
	 2.what if someone wants to create their own _PrintStream class? would we need to restrict that? Or would we just allow it to exist just like we would allow someone to create their own String class...thanks!
		(its in scope 1 not 0 so its not using an instance of the _PrintStream or String in scope 0) for now just restricting _PrintStream
	3.Does this always refer to the class you are in?

	 */
	
	//TODO need to get rid of \n in errors..... 
	//TODO fix var x = ClassRef  should be an error
	int levelPassCount;
	ErrorReporter reporter;
	private ClassDecl currClass;
	private MethodDecl currMethod;
	ArrayList<String>currDeclaredVars;
	public AST Decorate(AST ast, ErrorReporter reporter){ //drives identification process
		try{IDTable t = new IDTable(reporter);
			levelPassCount=0;
			this.reporter=reporter;
			currDeclaredVars=new ArrayList<String>();
			ast.visit(this, t);
			t.printTable();
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
		arg.openScope();//now in local vars, level 4
		for(Statement s: md.statementList){
			s.visit(this, arg);
		}
		arg.closeScope();//now at params level 3
		arg.closeScope();//closing params, now at member scope level 2
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, IDTable arg) {
		// TODO Auto-generated method stub
		pd.type.visit(this, arg);//checking type
		//System.out.println(pd.name+" of type "+pd.type.typeKind.toString()+" is ok.");
		addDeclaration(arg, pd);
		currDeclaredVars.add(pd.name);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, IDTable arg) {//will handle in varDeclStatement
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, IDTable arg) { //don't care doesn't point anywhere
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, IDTable arg) { //Identifier here....
		// TODO Auto-generated method stub what about strings???
		Identifier i = type.className;
		if(i.spelling.equals("_PrintStream")){ //no printstream....question on this pending
			reporter.reportError("*** Identification error:  _PrintStream is not allowed to be accessed! \n Position"+type.posn);
			return null;
		}
		Declaration d = arg.table.get(1).get(i.spelling);
		if(!(d instanceof ClassDecl)){
			//then check predefined
			d=arg.table.get(0).get(i.spelling);
			if(!(d instanceof ClassDecl)){//not in predefined either
				reporter.reportError("*** Identification error:  "+i.spelling+" cannot be resolved to a type"+"\n Position: "+type.posn);
				return null;
			}
			if(d.name.equals("String"))type.typeKind=TypeKind.UNSUPPORTED;//predefined String Class
		}
		i.d=d;
		LinkDump(i,i.d);
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, IDTable arg) {
		type.eltType.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub NOT TESTED
		arg.openScope();//new scope added
		for(Statement s: stmt.sl){
			s.visit(this, arg);
		}
		arg.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, IDTable arg) {//working on this...
		// TODO Auto-generated method stub NOT TESTED
		stmt.varDecl.type.visit(this, arg);
		addDeclaration(arg, stmt.varDecl);//add this before going into the expression....handles int x=x+2;
		stmt.initExp.visit(this, arg);
		currDeclaredVars.add(stmt.varDecl.name);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub NOT TESTED
		stmt.val.visit(this, arg);
		stmt.ref.visit(this, arg);
		if(stmt.ref.d instanceof MethodDecl){
			reporter.reportError("*** Identification error:  Cannot assign a method declaration! \n Position: "+stmt.ref.posn);
			return null;
		}
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, IDTable arg) {//questions here!!
		// TODO Auto-generated method stub
		stmt.val.visit(this, arg);
		stmt.ixRef.visit(this, arg);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub NOT TESTED
		stmt.methodRef.visit(this, arg);
		for(Expression e : stmt.argList){
			e.visit(this, arg);
		}
		return null;
	}
	
	@Override
	public Object visitReturnStmt(ReturnStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub NOT TESTED
		if(stmt.returnExpr!=null && currMethod.type.typeKind.equals(TypeKind.VOID)){
			reporter.reportError("*** Identification error:  method "+currMethod.name+" should not have a return type! \nPosition:"+stmt.posn);
			return null;
		}
		if(stmt.returnExpr!=null)stmt.returnExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub NOT TESTED
		stmt.cond.visit(this, arg);
		if(stmt.thenStmt instanceof VarDeclStmt){
			reporter.reportError("*** Identification error:  VarDeclStmt cannot be the only statement following conditional \nPosition: "+stmt.thenStmt.posn);
		}
		else stmt.thenStmt.visit(this, arg);
		if(stmt.elseStmt instanceof VarDeclStmt){
			reporter.reportError("*** Identification error:  VarDeclStmt cannot be the only statement following conditional \nPosition: "+stmt.elseStmt.posn);
		}
		else if(stmt.elseStmt != null)stmt.elseStmt.visit(this, arg);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, IDTable arg) {
		// NOT TESTED
		stmt.cond.visit(this, arg);
		if(stmt.body instanceof VarDeclStmt){
			reporter.reportError("*** Identification error:  VarDeclStmt cannot be the only statement following conditional \nPosition: "+stmt.body.posn);
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
		expr.functionRef.visit(this, arg);
		for(Expression e : expr.argList){
			e.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, IDTable arg) { //don't care?
		// TODO Auto-generated method stub
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
	public Object visitQualifiedRef(QualifiedRef ref, IDTable arg) { //have no clue yet ughhhh
		// TODO Auto-generated method stub
		System.out.println("Hit unimplemented Qualified Ref");	
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, IDTable arg) {//can do this it just passes along?
		//  NOT TESTED
		ref.indexExpr.visit(this, arg);
		ref.idRef.visit(this, arg);
		ref.d=ref.idRef.d;
		LinkDump(ref,ref.d);
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, IDTable arg) {//will deal with idRef not called in qualified here....
		// TODO Auto-generated method stub
		String name=ref.id.spelling;
		Declaration d=arg.retrieve(name);
		if(d==null){
			reporter.reportError("*** Identification error:  idRef "+ref.id.spelling+" cannot be resolved, may not be undeclared \nPosition: "+ref.posn);
			return null;
		}
		if(d instanceof LocalDecl){ //scope 3+
			if(!currDeclaredVars.contains(d.name)){
				reporter.reportError("*** Identification error:  you cannot reference a variable that is currently being declared \n Position: "+d.posn);
				return null;
			}
			ref.d=d;
			ref.id.d=d;
			LinkDump(ref.id,ref.id.d);
			LinkDump(ref,ref.d);
			return null;
		}
		if(d instanceof MemberDecl){ //scope 2
			MemberDecl member=(MemberDecl)d;
			if(member.isPrivate&&member.c!=currClass){ //private check
				reporter.reportError("*** Identification error:  cannot access private field/method "+member.name+" of class "+member.c.name+"!! \n Position: "+ref.posn);
				return null;
			}
			if(currMethod.isStatic&&!(member.isStatic)){//static check...will check qualified part in qualified ref visit
				reporter.reportError("*** Identification error:  cannot reference non-static member "+member.name+" from static method "+currMethod.name+" \n Position: "+ref.posn);
				return null;
			}
			//we should be good at this point
			ref.d=d;
			ref.id.d=d;
			LinkDump(ref.id,ref.id.d);
			LinkDump(ref,ref.d);
			return null;
		}
		if(d instanceof ClassDecl){//classname w/o type
			ref.d=d;
			ref.id.d=d;
			LinkDump(ref.id,ref.id.d);
			LinkDump(ref,ref.d);
			return null;
		}
		else{//we got a problem
			reporter.reportError("*** Identification error:  impossible??!!! cannot reach");
			return null;
		}
		
	}

	@Override
	public Object visitThisRef(ThisRef ref, IDTable arg) {//2ez
		// TODO Auto-generated method stub 	NOT TESTED
		ref.d=currClass;
		LinkDump(ref,ref.d);
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, IDTable arg) {//I handle Ids in other places
		// TODO Auto-generated method stub
		System.out.println("Shouldn't have hit Identifier!!");
		return null;
	}

	@Override
	public Object visitOperator(Operator op, IDTable arg) { //don't care?
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, IDTable arg) { //don't care?
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, IDTable arg) { //don't care?
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, IDTable arg) { //don't care?
		// TODO Auto-generated method stub
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
	
	public void LinkDump(Identifier i, Declaration d){ //debugging purposes
		System.out.print("\n ====LINK ALERT==== \n");
		System.out.println(i.spelling+" at "+"\nPosition "+i.posn+"\n is now linked to "+d.toString()+" "+d.name+" at \n Position: "+d.posn);	
		System.out.println("====END====");
	}
	public void LinkDump(Reference r, Declaration d){
		System.out.print("\n ====LINK ALERT==== \n");
		System.out.println(r.toString()+" at "+"\nPosition "+r.posn+"\n is now linked to "+d.toString()+" "+d.name+" at \n Position: "+d.posn);
		System.out.println("====END====");
	}

}

