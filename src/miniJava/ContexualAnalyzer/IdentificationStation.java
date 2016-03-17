package miniJava.ContexualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

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
	int levelPassCount;
	boolean levelPassDone;
	public AST Decorate(AST ast, ErrorReporter reporter){ //drives identification process
		IDTable t = new IDTable(reporter);
		levelPassDone=false;
		levelPassCount=0;
		ast.visit(this, t);
		return ast;
	}
	@Override
	public Object visitPackage(Package prog, IDTable arg) { //goal right now is to get down traversal....
		for (ClassDecl c: prog.classDeclList){
			System.out.println(c.name);
			c.visit(this,arg);
		}
		levelPassDone=true;
		for(ClassDecl c: prog.classDeclList){ //not adding classDecl here....but am simply revisiting to go down 
			System.out.println(c.name+" Round 2");
			c.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, IDTable arg) {
		if(!levelPassDone){
		for(FieldDecl fd: cd.fieldDeclList){ //visiting fields on first pass
			//doSomething
			System.out.println(fd.name);
			fd.visit(this, arg);
		}
		return null;
		}
		
		else{ //second pass, go onto visiting methods
			for (MethodDecl d: cd.methodDeclList){
				//doSomething
				System.out.println(d.name);
				d.visit(this, arg);
			}
			return null;
		}
		
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitQualifiedRef(QualifiedRef ref, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitOperator(Operator op, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, IDTable arg) {
		// TODO Auto-generated method stub
		return null;
	}

}

