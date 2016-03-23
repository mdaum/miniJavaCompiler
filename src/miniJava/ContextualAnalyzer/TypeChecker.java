package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class TypeChecker implements Visitor<Object,Type> {
	ErrorReporter reporter;
	AST ast;
	Type genError;
	public TypeChecker(AST ast,ErrorReporter reporter ){
		this.reporter=reporter;
		this.ast=ast;
		this.genError=new BaseType(TypeKind.ERROR,new SourcePosition());
	}
	public void typeCheck(){
		ast.visit(this, null);
	}
	
	@Override
	public Type visitPackage(Package prog, Object arg) {
		// TODO Auto-generated method stub
			return null;
		}

	@Override
	public Type visitClassDecl(ClassDecl cd, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitBaseType(BaseType type, Object arg) {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public Type visitClassType(ClassType type, Object arg) {
		// TODO Auto-generated method stub
		return type.className.d.type;
	}

	@Override
	public Type visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		return new ArrayType(type.eltType.visit(this, null),type.posn);
	}

	@Override
	public Type visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitReturnStmt(ReturnStmt stmt, Object arg) {//only stmt that returns a type if return expr exists!
		// TODO Auto-generated method stub
		if(stmt.returnExpr!=null)return stmt.returnExpr.visit(this, null);
		return null; //meaning return w no expr/type..
	}

	@Override
	public Type visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		Type cond = stmt.cond.visit(this, null);
		if(cond.typeKind!=TypeKind.BOOLEAN){
			reporter.reportError("*** Type Check Error: Conditional in if statement not of type boolean! Position: "+stmt.cond.posn);
		}
		stmt.thenStmt.visit(this, null);
		if(stmt.elseStmt!=null)stmt.elseStmt.visit(this, null);
		return null;
	}

	@Override
	public Type visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		Type cond = stmt.cond.visit(this, null);
		if(cond.typeKind!=TypeKind.BOOLEAN){
			reporter.reportError("*** Type Check Error: Conditional in while loop not of type boolean! Position: "+stmt.cond.posn);
		}
		stmt.body.visit(this, null);//seeing if we find error in body
		return null;
	}

	@Override
	public Type visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return expr.ref.visit(this, arg);
	}

	@Override
	public Type visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return expr.lit.visit(this, arg);
	}

	@Override
	public Type visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return expr.classtype.visit(this, arg);
	}

	@Override
	public Type visitNewArrayExpr(NewArrayExpr expr, Object arg) {//should hand back arrayType
		// TODO Auto-generated method stub
		Type size = expr.sizeExpr.visit(this, arg);
		Type inner = expr.eltType.visit(this, arg);
		if(size.typeKind!=TypeKind.INT){
			reporter.reportError("*** Type Checking Error: Size of array must be an int! Position: "+expr.sizeExpr.posn);
			return genError;
		}
		return new ArrayType(inner,expr.posn);
	}

	@Override
	public Type visitQualifiedRef(QualifiedRef ref, Object arg) {//coming back
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitIndexedRef(IndexedRef ref, Object arg) {
		// TODO Auto-generated method stub
		Type toReturn=null;
		Type e=ref.indexExpr.visit(this,null);
		Type r = ref.idRef.visit(this, arg);
		if(e.typeKind!=TypeKind.INT){
			toReturn=genError;
			reporter.reportError("*** Type Checking Error: Cannot have a "+e.typeKind+" as an index! Position: "+ref.indexExpr.posn);
		}
		else toReturn=r;
		return toReturn;
	}

	@Override
	public Type visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		Type id = ref.id.visit(this, null);
		if(id.typeKind==TypeKind.ERROR){
			return genError; //will check error all the way up...
		}
		return id;
	}

	@Override
	public Type visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		return ref.d.type;
	}

	@Override
	public Type visitIdentifier(Identifier id, Object arg) { //pass on type
		// TODO Auto-generated method stub
		return id.d.type.visit(this, null);
	}

	@Override
	public Type visitOperator(Operator op, Object arg) { //will handle in binary expr
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.INT,num.posn);
	}

	@Override
	public Type visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.BOOLEAN,bool.posn);
	}

	@Override
	public Type visitNullLiteral(NullLiteral nullLiteral, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.NULL,nullLiteral.posn);
	}


}
