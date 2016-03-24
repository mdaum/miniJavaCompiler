package miniJava.ContextualAnalyzer;

import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.TokenKind;

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
			hitReturn=false;
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
			checkTypeEquivalence(currReturnType, md.type, md.posn);
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
	public Type visitClassType(ClassType type, Object arg) {//no this!!!classes dont have types
		 
		return type;
	}

	@Override
	public Type visitArrayType(ArrayType type, Object arg) {//might be trouble
		 
		return type.eltType.visit(this, null);
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
		TypeCheckMethodCall(stmt.methodRef, stmt.argList);
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
		Type subject= expr.expr.visit(this, null);
		if(expr.operator.kind==TokenKind.bunop){
			if(!(subject.typeKind==TypeKind.INT||subject.typeKind==TypeKind.ERROR)){
				reporter.reportError("*** Type Checking Error: cannot - a boolean. Position: "+expr.posn);
				return genError;
			}
			return subject;
		}
		else if(expr.operator.kind==TokenKind.unop){
			if(!(subject.typeKind==TypeKind.BOOLEAN||subject.typeKind==TypeKind.ERROR)){
				reporter.reportError("*** Type Checking Error: cannot negate an integer. Position: "+expr.posn);
				return genError;
			}
			return subject;
		}
		else{
			reporter.reportError("IMPOSSIBLE");//should never hit
			System.exit(4);
		}
		return null;
	}

	@Override
	public Type visitBinaryExpr(BinaryExpr expr, Object arg) {
		Type lhs=expr.left.visit(this, null);
		Type rhs=expr.right.visit(this, null);
		//if(lhs.typeKind==TypeKind.ERROR||rhs.typeKind==TypeKind.ERROR)return genError;
		Type toReturn=null;
		switch(expr.operator.spelling){
		case "=="://either int or bool is ok...
		case "!=":
			if(checkTypeEquivalence(lhs, rhs, expr.posn)){
				toReturn=new BaseType(TypeKind.BOOLEAN,expr.posn);
				break;
			}
			else return genError;
		
		case "||":
		case "&&":
			if(lhs.typeKind==TypeKind.INT||rhs.typeKind==TypeKind.INT){
				reporter.reportError("*** Type Check Error: cannot use an int with || or &&. Position: "+expr.posn);
				return genError;
			}
			if(checkTypeEquivalence(lhs, rhs, expr.posn)){
				toReturn=new BaseType(TypeKind.BOOLEAN,expr.posn);
				break;
			}
			else return genError;
		
		case ">":
		case "<":
		case "<=":
		case ">=":
			if(lhs.typeKind==TypeKind.BOOLEAN||rhs.typeKind==TypeKind.BOOLEAN){
				reporter.reportError("*** Type Check Error: cannot use an int with >,<,>= or <=. Position: "+expr.posn);
				return genError;
			}
			if(checkTypeEquivalence(lhs, rhs, expr.posn)){
				toReturn=new BaseType(TypeKind.BOOLEAN,expr.posn);
				break;
			}
			else return genError;
		
		case "+":
		case "-":
		case "*":
		case "/":
			if(lhs.typeKind==TypeKind.BOOLEAN||rhs.typeKind==TypeKind.BOOLEAN){
				reporter.reportError("*** Type Check Error: cannot use an int with || or &&. Position: "+expr.posn);
				return genError;
			}
			if(checkTypeEquivalence(lhs, rhs, expr.posn)){
				toReturn=new BaseType(TypeKind.INT,expr.posn);
				break;
			}
			else return genError;
			
			
		default:
			throw new RuntimeException();
		}
		return toReturn;
	}

	@Override
	public Type visitRefExpr(RefExpr expr, Object arg) {
		
		return expr.ref.visit(this, arg);
	}

	@Override
	public Type visitCallExpr(CallExpr expr, Object arg) {
		return TypeCheckMethodCall(expr.functionRef, expr.argList);
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
		reporter.reportError("*** HIT THAT DRAGON!");
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
	
	public Type TypeCheckMethodCall(Reference method, ExprList args){
		MethodDecl m=(MethodDecl) method.d;
		ArrayList<Type> argTypes=new ArrayList<Type>();
		for (Expression e : args) {
			argTypes.add(e.visit(this, null));
		}
		if(args.size()!=m.parameterDeclList.size()){
			reporter.reportError("*** Type Check Error: not calling method "+m.name +" with right number of args "+method.posn);
			return genError;
		}
		boolean badMatch=false;
		for(int i=0;i<args.size();i++){
			if(!(checkTypeEquivalence(argTypes.get(i), m.parameterDeclList.get(i).type,method.posn))){
				reporter.reportError("*** that type mismatch was at parameter "+i+" of calling method "+method.d.name);
				badMatch=true;
			}
		}
		if(badMatch)return genError;
		return m.type;
	}
	
	public boolean checkTypeEquivalence(Type one,Type two, SourcePosition p){
		boolean toReturn = false;
		if(one.typeKind==TypeKind.UNSUPPORTED||two.typeKind==TypeKind.UNSUPPORTED){
			if(one.typeKind==TypeKind.UNSUPPORTED)reporter.reportError("*** Type Check Error: String is an unsupported type. Position: "+p);
			if(two.typeKind==TypeKind.UNSUPPORTED)reporter.reportError("*** Type Check Error: String is an unsupported type. Position: "+p);
			return false;
		}
		if(one.typeKind==TypeKind.ERROR||two.typeKind==TypeKind.ERROR)return true;
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
			if(one instanceof ClassType && two instanceof ClassType){
				reporter.reportError("*** Type Check Error: Class "+((ClassType)one).className.spelling+" and Class "+((ClassType)two).className.spelling+" are not the same Class Declaration! Position: "+p);
			}
			else reporter.reportError("*** Type Check Error: Type mismatch: "+one.typeKind+" "+two.typeKind+". Position: "+p);
		}
		return toReturn;
	
	}


}
