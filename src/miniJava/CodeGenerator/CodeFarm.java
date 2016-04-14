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
		patchAll();
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
		// TODO Auto-generated method stub
		pd.entity=new KnownAddress(charSize,arg*-1);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

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

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

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
	
	private void patchAll(){
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
