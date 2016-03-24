package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class IDTable {
	ArrayList<HashMap<String,Declaration>> table = new ArrayList<HashMap<String,Declaration>>(); //stack of hash tables, nested block structure
	ErrorReporter reporter;
	boolean addedPredefMembers;
	MemberDecl printStreamMember;
	MemberDecl SystemMember;
	ClassDecl printStreamDecl;
	HashMap<String,ArrayList<Declaration>> members=new HashMap<String,ArrayList<Declaration>>();//need to handle Member dups
	public IDTable(ErrorReporter e){
		openScope();//opening predefined level
		//creating predefined classes...
		this.reporter=e;
		this.addedPredefMembers=false;
		SourcePosition predefPos=new SourcePosition(-1,-1);//predefined sourcePos
		
/*		MemberDecl member = new FieldDecl(isVis,isAccess,t,name,currMemberPosn);
		MethodDecl method = new MethodDecl(member, params, statements, currMemberPosn);*/
		
		//starting with _PrintStream
		MethodDeclList printStreamMethods=new MethodDeclList();
		ParameterDeclList printlnParam=new ParameterDeclList();
		printlnParam.add(new ParameterDecl(new BaseType(TypeKind.INT,predefPos),"n", predefPos));
		printStreamMember=new FieldDecl(false,false,new BaseType(TypeKind.VOID,predefPos),"println",predefPos);
		printStreamMethods.add(new MethodDecl(printStreamMember, printlnParam, new StatementList(), predefPos));
		printStreamDecl = new ClassDecl("_PrintStream", new FieldDeclList(), printStreamMethods, predefPos);
		printStreamMember.c=printStreamDecl;//forcing identification here
		printStreamMethods.get(0).c=printStreamDecl;//same thing?
		//now String
		ClassDecl stringDecl= new ClassDecl("String",new FieldDeclList(),new MethodDeclList(),predefPos);
		
		//now System
		FieldDeclList systemfdl= new FieldDeclList();
		Token systemToken = new Token(TokenKind.id,"_PrintStream",predefPos);
		Identifier systemfdlID=new Identifier(systemToken,predefPos);
		systemfdlID.d=printStreamDecl;
		systemfdl.add(new FieldDecl(false,true,new ClassType(systemfdlID,predefPos),"out",predefPos));
		SystemMember=systemfdl.get(0);
		ClassDecl systemDecl= new ClassDecl("System",systemfdl,new MethodDeclList(),predefPos);
		systemfdl.get(0).c=systemDecl;
		
		//now add them to current scope
		try{
			enter(printStreamDecl);
			enter(stringDecl);
			enter(systemDecl);
		}catch (SyntaxError err){
			System.out.println("impossible!");
		}
		openScope();//now opening level 1: ClassDecl
	}
	/**
	 * Throws a new scope on the stack
	 */
	public void openScope(){//also adding predefined Members here....
		table.add(new HashMap<String,Declaration>());
		if(table.size()==3&&!addedPredefMembers){
			enter(printStreamDecl.methodDeclList.get(0));
			enter(SystemMember);
			addedPredefMembers=true;
		}
		
	}
	/**
	 * pops off top of stack
	 */
	public void closeScope(){
		if(table.size()< 2)//can't close predefined level
			throw new RuntimeException("Cannot close predefined scope!");
		table.remove(table.size()-1);
	}
	
	public void enter(Declaration decl){ //will report error within enter, so I can get both pieces of decl....
		if(decl instanceof MemberDecl){
			enterMember(decl);
			return;
		}
		String name = decl.name;		
		for(int i =3;i<table.size();i++){ //cannot hide from parameter and higher if you are local
			Declaration previousDecl = table.get(i).get(name);
			if(previousDecl!=null){
				reporter.reportError("*** Identification error: duplicate variable name: "+name+" already declared at "+previousDecl.posn+" Position:"+decl.posn.toString());
				throw new SyntaxError();
			}
		} //not found in local or rather local scope not even created yet
		HashMap<String,Declaration> currentScope=table.get(table.size()-1);
		Declaration previousDecl=currentScope.get(name);
		if(previousDecl!=null){//checking current scope if in scopes 0,1, 2 handled separately
			reporter.reportError("*** Identification error: duplicate class name: "+name+" already declared at "+previousDecl.posn+" Position:"+decl.posn);
			throw new SyntaxError();
		}
		//we are good, add to current scope
		currentScope.put(name, decl);
		
	}

private void enterMember(Declaration decl) {
		if(!(decl instanceof MemberDecl))return;
		ArrayList<Declaration> list=members.get(decl.name);
		if(list==null){
			list=new ArrayList<Declaration>();
			list.add(decl);
			members.put(decl.name, list);
			return;
		}
			System.out.println("dup Member!");
			MemberDecl param=(MemberDecl)decl;
			for (Declaration declaration : list) {
				MemberDecl m = (MemberDecl)declaration;
				if(m.c==param.c){
					reporter.reportError("*** Identification error: duplicate member name in same class: "+m.name+" already declared at "+declaration.posn+" Position:"+decl.posn);
					throw new SyntaxError();
				}
			}
			list.add(decl);
			return;
		
		
	}
	/*if(previousDecl instanceof MemberDecl && decl instanceof MemberDecl){//scope 2
	if(((MemberDecl) previousDecl).c.name.equals(((MemberDecl)decl).c.name)){
		reporter.reportError("*** Identification error: duplicate member name in same class: "+name+" already declared at "+previousDecl.posn+" Position:"+decl.posn);
		throw new SyntaxError();
	}
	else{ //we are ok, same member name but different classes
		currentScope.put(name, decl);
		return;
	}*/
	public Declaration retrieve(String id,ClassDecl currClass,MethodDecl currMethod,ClassDecl currQClass){
		Declaration d = null;
		for(int i=(table.size()-1);i>=0;i--){
			d=table.get(i).get(id);
			if(d!=null){
				System.out.println("Retrieved decl "+d.name+"at level "+i);
				return d;
			}
		}
		
		return retrieveMember(id,currClass,currMethod,currQClass);
	}
	public Declaration retrieve(String id,int scope,ClassDecl currClass,MethodDecl currMethod,ClassDecl currQClass){
		if(scope==2)return retrieveMember(id,currClass,currMethod,currQClass);
		Declaration d = null;
		d=table.get(scope).get(id);
		if(d!=null){
			System.out.println("Retrieved decl "+d.name+"at level "+scope);
			return d;
		}
		return null;
	}
	
	private Declaration retrieveMember(String id,ClassDecl currClass,MethodDecl currMethod,ClassDecl currQClass) {
		ArrayList<Declaration> d=members.get(id);
		if(d==null)return null;
		MemberDecl check=(MemberDecl)d.get(0);
		if(d.size()==1&&check.c!=currQClass&&currQClass!=null)return printStreamDecl;//returning classDecl to throw error
		if(currQClass!=null){
			for (Declaration declaration : d) {
				MemberDecl m=(MemberDecl)declaration;
				if(m.c==currQClass)return m;
			}
		}
		else{
			for (Declaration declaration : d) {
				MemberDecl m=(MemberDecl)declaration;
				if(m.c==currClass)return m;
			}
		}
		return null;
	}
	class SyntaxError extends Error{
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID=1L;
	}
	public void printTable(){
		System.out.print("\n ====TABLE DUMP====");
		for(int i=0;i<table.size();i++){
			System.out.println();
			System.out.print("Scope "+i+ ":");
			for (String d : table.get(i).keySet()) {
				System.out.print(d+",");
			}
		}
		System.out.println();
		System.out.println("====END TABLE DUMP==== \n");
		System.out.println("===MEMBER DUMP===");
		for(String d: members.keySet()){
			System.out.println(d+": "+members.get(d).size());
		}
	}
	
}
