package miniJava.CodeGenerator;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;

import java.util.ArrayList;

import mJAM.Machine;
import mJAM.Machine.*;

public class CodeFarm implements Visitor<Integer,Object>{
	private ErrorReporter reporter;
	private MethodDecl main;
	private ArrayList<MemberDecl> toPatch; //toPatch and patchAddr are parallel
	private ArrayList<Integer> patchAddr;
	int mainAddr;
	private int displacement;
	private boolean currPrintln;
	private final int charSize = Machine.characterSize; //all the same in miniJava, yay!
	public CodeFarm(ErrorReporter reporter,MethodDecl m){
		this.reporter=reporter;
		this.main=m;
		this.displacement=0;
		this.toPatch=new ArrayList<MemberDecl>();
		this.patchAddr=new ArrayList<Integer>();
		currPrintln=false;
	}
	
	
	public void generateCode(AST ast){
		Machine.initCodeGen();
		ast.visit(this, -7);
		patchRest();
	}
	
	/*Integer arg in visitor implementation represents certain flags (or sometimes just offsets, will note), mappings are:
		-7 = don't care
		 9 = fetch
		11 = store
	
	*/

	@Override
	public Object visitPackage(Package prog, Integer arg) {
		int offFromSB = 0;
		//static vars
		for(ClassDecl c: prog.classDeclList){ //assign entities to all fields, emit static ones...
			int currNSF=0;//will use for displacements of non static fields
			for(FieldDecl f: c.fieldDeclList){
				if(f.isStatic){
					Machine.emit(Op.PUSH,1);//pushing onto global segment
					f.entity=new KnownAddress(charSize,offFromSB+1);
					offFromSB++;
				}
				else {
					f.visit(this, currNSF); //offset visit
					currNSF++;
				}
			}
		}
		mainAddr= Machine.nextInstrAddr();//grabbing for patching main soon...
		//call main
		Machine.emit(Op.CALL,Reg.CB,-1); //-1 will act as unknown addr
		Machine.emit(Op.POP,0,0,offFromSB);
		Machine.emit(Op.HALT,0,0,0);//done, now just patch main
		
		for(ClassDecl c: prog.classDeclList){ //now looking at methods of each class
			c.visit(this, -7);
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Integer arg) {
		for(MethodDecl m: cd.methodDeclList){
			m.visit(this, -7);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Integer arg) {
		fd.entity= new KnownAddress(charSize,arg);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Integer arg) {
		int paramNum=md.parameterDeclList.size();
		for(ParameterDecl p:md.parameterDeclList){ //make entities for params
			p.visit(this, paramNum);//offset visit
			paramNum--;
		}
		md.entity=new KnownAddress(charSize,Machine.nextInstrAddr());
		if(md.equals(main)){
			Machine.patch(mainAddr, Machine.nextInstrAddr());
		}
		displacement=3; //making room on frame
		currPrintln=isPrintln(md);
		for(Statement s: md.statementList){
			s.visit(this, md.parameterDeclList.size());//offest visit is only used on return stmt
		}
		
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Integer arg) {
		pd.entity=new KnownAddress(charSize,arg*-1);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Integer arg) {
		decl.entity = new KnownAddress(charSize,displacement);
		return null;
	}
	
	//types don't matter
	
	@Override
	public Object visitBaseType(BaseType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	//statementsx
	
	@Override
	public Object visitBlockStmt(BlockStmt stmt, Integer arg) {
		int numVarDecls=0;
		for(Statement s: stmt.sl){
			if(s instanceof VarDeclStmt)numVarDecls++;
			s.visit(this, -7);
		}
		Machine.emit(Op.POP,0,0,numVarDecls);//vars now not accessible
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		stmt.varDecl.visit(this, -7);
		stmt.initExp.visit(this, 9);
		displacement++;
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Integer arg) {
		if(stmt.ref instanceof QualifiedRef){
			QualifiedRef q = (QualifiedRef)stmt.ref;
			if(q.id.d instanceof FieldDecl&&((FieldDecl)q.id.d).isStatic){
				int offBy=(int)(stmt.ref.visit(this, 11));//store
					stmt.val.visit(this, 9);//fetch
					Machine.emit(Op.STORE,Reg.SB,offBy);
					return null;
				
			}
			else{
				stmt.ref.visit(this, 11);
				stmt.val.visit(this, 9);
				Machine.emit(Prim.fieldupd);
			}
		}
		else if(stmt.ref instanceof IdRef){
			int offBy=(int)(stmt.ref.visit(this, 11));//store
			stmt.val.visit(this, 9);//fetch
			IdRef i = (IdRef)stmt.ref;
			if(i.id.d instanceof FieldDecl){
				FieldDecl f = (FieldDecl)i.id.d;
				if(f.isStatic){
					Machine.emit(Op.STORE,Reg.SB,offBy);
				}
				else{
					Machine.emit(Op.STORE,Reg.OB,offBy);
				}
			}
			else Machine.emit(Op.STORE,Reg.LB,offBy);
		}
		else{
			codeGenError("Unrecognized reference in lhs of assign statement", stmt.ref.posn);
		}
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Integer arg) { //will handle later....
		stmt.ixRef.visit(this, 11);
		stmt.val.visit(this, 9);
		Machine.emit(Prim.arrayupd);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Integer arg) { //after this can start testing incrementally
		for(Expression e: stmt.argList){
			e.visit(this, 9);//fetch
		}
		if(stmt.methodRef instanceof QualifiedRef){ //check println
			QualifiedRef q = (QualifiedRef)stmt.methodRef;
			if(q.id.d instanceof MethodDecl){
				MethodDecl m = (MethodDecl)q.id.d;
				if(isPrintln(m)){
					Machine.emit(Prim.putintnl);
					return null;
				}
			}
		}//everything up to here is just fine....rest might be issue
		if(stmt.methodRef instanceof IdRef){ //come back maybe
			IdRef i = (IdRef)stmt.methodRef;
			if(i.id.d instanceof MemberDecl &&((MemberDecl)i.id.d).isStatic){
				toPatch.add((MemberDecl)stmt.methodRef.d);//might be an issue
				patchAddr.add(Machine.nextInstrAddr());
				Machine.emit(Op.CALL,Reg.CB,-1);//unknown addr for now
			}
			else{//is par of this class
				Machine.emit(Op.LOAD,Reg.LB,0);
				toPatch.add((MemberDecl)stmt.methodRef.d);//might be an issue
				patchAddr.add(Machine.nextInstrAddr());
				Machine.emit(Op.CALLI,Reg.CB,-1);//unknown for now
			}
		}
		else{
			stmt.methodRef.visit(this, 9);
			toPatch.add((MemberDecl)stmt.methodRef.d);//might be an issue
			patchAddr.add(Machine.nextInstrAddr());
			Machine.emit(Op.CALLI,Reg.CB,-1);//unknown for now
		}
		
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Integer arg) {
		if(stmt.returnExpr==null){
			if(!currPrintln)Machine.emit(Op.RETURN,0,0,arg);
		}
		else{
			stmt.returnExpr.visit(this, 9);
			if(!currPrintln) Machine.emit(Op.RETURN,1,0,arg);
		}
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Integer arg) {
		stmt.cond.visit(this, 9);
		int ifAddr=Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF,0,Reg.CB,-1);
		//then
		stmt.thenStmt.visit(this,-7);
		int thenAddr=Machine.nextInstrAddr();
		Machine.emit(Op.JUMP,Reg.CB,-1);
		//else
		int elseAddr=Machine.nextInstrAddr();
		Machine.patch(ifAddr, elseAddr);
		if(stmt.elseStmt!=null){
			stmt.elseStmt.visit(this, -7);
		}
		//end
		int endAddr=Machine.nextInstrAddr();
		Machine.patch(thenAddr, endAddr);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Integer arg) {
		int whileAddr=Machine.nextInstrAddr();
		stmt.cond.visit(this, 9);//fetch if needed
		int afterAddr=Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF,0,Reg.CB,-1);
		//body
		stmt.body.visit(this, -7);
		Machine.emit(Op.JUMP,Reg.CB,whileAddr);
		
		//end
		int endAddr=Machine.nextInstrAddr();
		Machine.patch(afterAddr, endAddr);
		return null;
	}
	
	//Expressions

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Integer arg) {
		expr.expr.visit(this, 9);
		switch(expr.operator.kind){
		case bunop:
			Machine.emit(Prim.neg);
			break;
		case unop:
			Machine.emit(Op.LOADL,0);
			Machine.emit(Prim.eq);
			break;
		default:
			codeGenError("Unexpected operator "+expr.operator.spelling,expr.operator.posn);
			break;
		}
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Integer arg) {
		expr.left.visit(this, 9);
		expr.right.visit(this, 9);
		switch(expr.operator.spelling){
		case "+":
			Machine.emit(Prim.add);
			break;
		case "-":
			Machine.emit(Prim.sub);
			break;
		case "*":
			Machine.emit(Prim.mult);
			break;
		case "/":
			Machine.emit(Prim.div);
			break;
		case "==":
			Machine.emit(Prim.eq);
			break;
		case "!=":
			Machine.emit(Prim.ne);
			break;
		case ">":
			Machine.emit(Prim.gt);
			break;
		case "<":
			Machine.emit(Prim.lt);
			break;
		case "<=":
			Machine.emit(Prim.le);
			break;
		case ">=":
			Machine.emit(Prim.ge);
			break;
		case "&&":
			Machine.emit(Prim.and);
			break;
		case "||":
			Machine.emit(Prim.or);
			break;
		default:
			codeGenError("Unexpected operator "+expr.operator.spelling, expr.operator.posn);
			break;
		}
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Integer arg) {
		expr.ref.visit(this, arg);//pass on the flag
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Integer arg) {
		for(Expression e: expr.argList){
			e.visit(this, 9);//fetch
		}
		if(expr.functionRef instanceof QualifiedRef){ //check println
			QualifiedRef q = (QualifiedRef)expr.functionRef;
			if(q.id.d instanceof MethodDecl){
				MethodDecl m = (MethodDecl)q.id.d;
				if(isPrintln(m)){
					Machine.emit(Prim.putintnl);
					return null;
				}
			}
		}//everything up to here is just fine....rest might be issue
		if(expr.functionRef instanceof IdRef){ //come back maybe
			IdRef i = (IdRef)expr.functionRef;
			if(i.id.d instanceof MemberDecl &&((MemberDecl)i.id.d).isStatic){
				toPatch.add((MemberDecl)expr.functionRef.d);//might be an issue
				patchAddr.add(Machine.nextInstrAddr());
				Machine.emit(Op.CALL,Reg.CB,-1);//unknown addr for now
			}
			else{//is par of this class
				Machine.emit(Op.LOADA,Reg.OB,0);
				toPatch.add((MemberDecl)expr.functionRef.d);//might be an issue
				patchAddr.add(Machine.nextInstrAddr());
				Machine.emit(Op.CALLI,Reg.CB,-1);//unknown for now
			}
		}
		else{
			expr.functionRef.visit(this, 9);
			toPatch.add((MemberDecl)expr.functionRef.d);//might be an issue
			patchAddr.add(Machine.nextInstrAddr());
			Machine.emit(Op.CALLI,Reg.CB,-1);//unknown for now
		}
		
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Integer arg) {
		switch(expr.lit.kind){
		case num:
			Machine.emit(Op.LOADL,Integer.parseInt(expr.lit.spelling));
			break;
		case tru:
			Machine.emit(Op.LOADL,Machine.trueRep);
			break;
		case nul:
			Machine.emit(Op.LOADL,Machine.nullRep);
			break;
		case fals:
			Machine.emit(Op.LOADL,Machine.falseRep);
			break;
		default:
			codeGenError("Unrecognized literal expression "+expr.lit.spelling, expr.posn);
			Machine.emit(Op.LOADL,-666);//bad! bad!
			break;
		}
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
		Machine.emit(Op.LOADL,-1);//no inheritance
		Machine.emit(Op.LOADL,((ClassDecl)expr.classtype.className.d).fieldDeclList.size());
		Machine.emit(Prim.newobj);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
		expr.sizeExpr.visit(this, 9);//fetch in case not literal....
		Machine.emit(Prim.newarr);
		return null;
	}
	
	
	//Refs

	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Integer arg) {
		
		if(arg==11){//store
			
			if(ref.id.d instanceof FieldDecl && ((FieldDecl)ref.id.d).isArrayLength){
				codeGenError("array.length is read-only", ref.id.posn);
			}
			else if(ref.id.d instanceof FieldDecl &&((FieldDecl)ref.id.d).isStatic){
				return ((KnownAddress)ref.id.d.entity).displacement;
			}
			else{
				ref.ref.visit(this, 9);
				Machine.emit(Op.LOADL,((KnownAddress)ref.id.d.entity).displacement);
			}
		}
		else if(arg==9){//fetch
			if(ref.id.d instanceof FieldDecl && ((FieldDecl)ref.id.d).isArrayLength){ 
				ref.ref.visit(this, 9);//fetching arrayref
				Machine.emit(Prim.arraylen);
			}
			else if(ref.id.d instanceof FieldDecl){
				FieldDecl f = (FieldDecl)ref.id.d;
				if(f.isStatic){
					Machine.emit(Op.LOAD,Reg.SB,((KnownAddress)ref.id.d.entity).displacement);
				}
				else{
					ref.ref.visit(this, 9);//fetch
					Machine.emit(Op.LOADL,((KnownAddress)ref.id.d.entity).displacement);
					Machine.emit(Prim.fieldref);
				}
			}
			else if(ref.id.d instanceof MethodDecl){
				ref.ref.visit(this, 9);
			}
			else codeGenError("bad ref type in qualified ref", ref.id.posn);
		}
		else codeGenError("unrecognized signal to visitQualifiedRef",ref.posn );
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, Integer arg) {
		if(arg==11){
		ref.idRef.visit(this,9);
		ref.indexExpr.visit(this, 9);
		}
		else if(arg==9){
			ref.idRef.visit(this, 9);
			ref.indexExpr.visit(this, 9);
			Machine.emit(Prim.arrayref);
		}
		else codeGenError("unrecognized signal passed to visitIndexedRef", ref.posn);
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Integer arg) {
		int displacement = ((KnownAddress)ref.id.d.entity).displacement;
		if(arg==11)return displacement; //store
		if(arg==9){ //fetch
			if(ref.id.d instanceof FieldDecl){
				FieldDecl f = (FieldDecl)ref.id.d;
				if(f.isStatic){
					Machine.emit(Op.LOAD,Reg.SB,displacement);
				}
				else{
					Machine.emit(Op.LOAD,Reg.OB,displacement);
				}
			}
			else{//not field
				Machine.emit(Op.LOAD,Reg.LB,displacement);
			}
		}
		else codeGenError("unrecognized signal to visitIdRef "+arg , ref.id.posn);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Integer arg) {
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return null;
	}

	//Literals don't matter
	
	@Override
	public Object visitIdentifier(Identifier id, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
	//Error Stuff
	
	private void codeGenError(String e,SourcePosition pos) throws GenError {
		reporter.reportError("*** code generation error: "+e+". Postion: "+pos);
		throw new GenError();
	}
	
	class GenError extends Error{
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID=1L;
	}
	
	//Auxillary Logic...
	
	private void patchRest(){
		if(toPatch.size()!=patchAddr.size()){
			codeGenError("WHATTTT??? patching mess-up", new SourcePosition());
			throw new GenError();
		}
		for(int i=0;i<toPatch.size();i++){
			Machine.patch(patchAddr.get(i),((KnownAddress)toPatch.get(i).entity).displacement);
		}
		
	}
	private boolean isPrintln(MethodDecl m){
		if(m.c.name.equals("_PrintStream"))return true;
		return false;
	}
	 

}
