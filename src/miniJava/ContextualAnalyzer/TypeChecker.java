package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class TypeChecker implements Visitor<Object,Type> {
	ErrorReporter reporter;
	AST ast;
	Type genError;
	boolean hitReturn;//this is in case return is hidden in block 
	Type currReturnType; //in case return is hidden in block
	
	public TypeChecker(AST ast,ErrorReporter reporter ){
		this.reporter=reporter;
		this.ast=ast;
		this.genError=new BaseType(TypeKind.ERROR,new SourcePosition());
		this.hitReturn=false;
		this.currReturnType=null;
	}
	public void typeCheck(){
		ast.visit(this, null);
	}
	
	@Override
	public Type visitPackage(Package prog, Object arg) {
		
			for(ClassDecl c : prog.classDeclList){
				c.visit(this, null);
			}
			return null;
		}

	@Override
	public Type visitClassDecl(ClassDecl cd, Object arg) {
		
		for(MethodDecl m : cd.methodDeclList){
			m.visit(this, null);
		}
		return null;
	}

	@Override
	public Type visitFieldDecl(FieldDecl fd, Object arg) {
		
		return null;
	}

	@Override
	public Type visitMethodDecl(MethodDecl md, Object arg) {
		
		for(Statement s :md.statementList){
			if(hitReturn)break; //no unreachable code checked
			s.visit(this, null);
		}
		//now we check return type if any
		if(md.type.typeKind==TypeKind.VOID){ //already checked for bad cases in identification here
			return null;
		}
		//not void
		if(currReturnType==null){
			reporter.reportError("*** Type Check Error: non-void method "+md.name+" has to return something! Position: "+md.posn);
		}
		else {
			if(!checkTypeEquivalence(currReturnType, md.type, md.posn)){
				reporter.reportError("*** Type Check Error: must assign method "+md.name+" a return type of "+md.type+" Position: "+md.posn);
			}
		}
		return null;
	}

	@Override
	public Type visitParameterDecl(ParameterDecl pd, Object arg) {
		
		return pd.type.visit(this, null);
		
	}

	@Override
	public Type visitVarDecl(VarDecl decl, Object arg) {//will handle in vardeclstmt
		// don't care
		return null;
	}

	@Override
	public Type visitBaseType(BaseType type, Object arg) {
		 
		return type;
	}

	@Override
	public Type visitClassType(ClassType type, Object arg) {
		 
		return type.className.d.type;
	}

	@Override
	public Type visitArrayType(ArrayType type, Object arg) {//might be trouble
		 
		return new ArrayType(type.eltType.visit(this, null),type.posn);
	}

	@Override
	public Type visitBlockStmt(BlockStmt stmt, Object arg) {
		
		for(Statement s: stmt.sl){
			if(hitReturn)return null;
			if(s!=null)s.visit(this, null);
		}
		return null;//no return in there
	}

	@Override
	public Type visitVardeclStmt(VarDeclStmt stmt, Object arg) {//already have type of varDecl
		
		Type rhs = stmt.initExp.visit(this, null);
		checkTypeEquivalence(rhs, stmt.varDecl.type, stmt.posn);
		
		return null;
	}

	@Override
	public Type visitAssignStmt(AssignStmt stmt, Object arg) {
		
		if(stmt.ref.d instanceof ClassDecl||stmt.ref.d instanceof MethodDecl){
			reporter.reportError("*** Type Check Error: cannot use Class or method on lhs of assign stmt! Position: "+stmt.posn);
			return null;
		}
		Type lhs = stmt.ref.visit(this, null);
		Type rhs = stmt.val.visit(this,null);
		checkTypeEquivalence(lhs, rhs, stmt.posn);
		return null;
	}

	@Override
	public Type visitIxAssignStmt(IxAssignStmt stmt, Object arg) { //come back
		
		Type inner = stmt.ixRef.visit(this,null);
		Type rhs = stmt.val.visit(this, null);
		checkTypeEquivalence(inner, rhs, stmt.posn);
		return null;
	}

	@Override
	public Type visitCallStmt(CallStmt stmt, Object arg) {//coming back
		//TODO
		return null;
	}

	@Override
	public Type visitReturnStmt(ReturnStmt stmt, Object arg) {//only stmt that returns a type if return expr exists!
		 
		hitReturn=true;
		if(stmt.returnExpr!=null)currReturnType= stmt.returnExpr.visit(this, null);
		return null; //meaning return w no expr/type..
	}

	@Override
	public Type visitIfStmt(IfStmt stmt, Object arg) {
		 
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
		
		return expr.ref.visit(this, arg);
	}

	@Override
	public Type visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type visitLiteralExpr(LiteralExpr expr, Object arg) {
		 
		return expr.lit.visit(this, arg);
	}

	@Override
	public Type visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		 
		return expr.classtype.visit(this, arg);
	}

	@Override
	public Type visitNewArrayExpr(NewArrayExpr expr, Object arg) {//should hand back arrayType
		 
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
		System.out.println("HIT QUALIFIED REF!");
		return null;
	}

	@Override
	public Type visitIndexedRef(IndexedRef ref, Object arg) {
		 
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
		 
		Type id = ref.id.visit(this, null);
		if(id.typeKind==TypeKind.ERROR){
			return genError; //will check error all the way up...
		}
		return id;
	}

	@Override
	public Type visitThisRef(ThisRef ref, Object arg) {
		 
		return ref.d.type;
	}

	@Override
	public Type visitIdentifier(Identifier id, Object arg) { //pass on type
		 
		return id.d.type.visit(this, null);
	}

	@Override
	public Type visitOperator(Operator op, Object arg) { //will handle in binary expr
		 
		return null;
	}

	@Override
	public Type visitIntLiteral(IntLiteral num, Object arg) {
		 
		return new BaseType(TypeKind.INT,num.posn);
	}

	@Override
	public Type visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		 
		return new BaseType(TypeKind.BOOLEAN,bool.posn);
	}

	@Override
	public Type visitNullLiteral(NullLiteral nullLiteral, Object arg) {
		 
		return new BaseType(TypeKind.NULL,nullLiteral.posn);
	}
	
	public boolean checkTypeEquivalence(Type one,Type two, SourcePosition p){
		boolean toReturn = false;
		if(one.typeKind==TypeKind.UNSUPPORTED||two.typeKind==TypeKind.UNSUPPORTED){
			if(one.typeKind==TypeKind.UNSUPPORTED)reporter.reportError("*** Type Check Error: "+one.typeKind+" is an unsupported type. Position: "+p);
			if(two.typeKind==TypeKind.UNSUPPORTED)reporter.reportError("*** Type Check Error: "+two.typeKind+" is an unsupported type. Position: "+p);
			toReturn=false;
		}
		if(one.typeKind==TypeKind.ERROR||two.typeKind==TypeKind.ERROR)toReturn=true;
		if(one instanceof ArrayType &&two instanceof ArrayType){
			toReturn= checkTypeEquivalence(((ArrayType)one).eltType,((ArrayType)two).eltType,p); //see if this fucks up array of arrays
		}
		if(one instanceof BaseType && two instanceof BaseType){
			toReturn= ((BaseType)one).typeKind==((BaseType)two).typeKind;
		}
		if(one instanceof ClassType && two instanceof ClassType){
			toReturn= ((ClassType)one).className.d.equals(((ClassType)two).className.d);
		}
		if(!toReturn){
			reporter.reportError("*** Type Check Error: Type mismatch: "+one+" "+two+". Position: "+p);
		}
		return toReturn;
	
	}


}
