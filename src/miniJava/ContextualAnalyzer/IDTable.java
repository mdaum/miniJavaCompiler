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
	public IDTable(ErrorReporter e){
		openScope();//opening predefined level
		//creating predefined classes...
		this.reporter=e;
		
		SourcePosition predefPos=new SourcePosition(-1,-1);//predefined sourcePos
		
		//starting with _PrintStream
		MethodDeclList printStreamMethods=new MethodDeclList();
		ParameterDeclList printlnParam=new ParameterDeclList();
		printlnParam.add(new ParameterDecl(new BaseType(TypeKind.INT,predefPos),"n", predefPos));
		MemberDecl printStreamMember=new FieldDecl(false,false,new BaseType(TypeKind.VOID,predefPos),"println",predefPos);
		printStreamMethods.add(new MethodDecl(printStreamMember, printlnParam, new StatementList(), predefPos));
		ClassDecl printStreamDecl = new ClassDecl("_PrintStream", new FieldDeclList(), printStreamMethods, predefPos);
		printStreamMember.c=printStreamDecl;//forcing identification here
		printStreamMethods.get(0).c=printStreamDecl;
		//now String
		ClassDecl stringDecl= new ClassDecl("String",new FieldDeclList(),new MethodDeclList(),predefPos);
		
		//now System
		FieldDeclList systemfdl= new FieldDeclList();
		Token systemToken = new Token(TokenKind.id,"_PrintStream",predefPos);
		Identifier systemfdlID=new Identifier(systemToken,predefPos);
		systemfdlID.d=printStreamDecl;
		systemfdl.add(new FieldDecl(false,true,new ClassType(systemfdlID,predefPos),"out",predefPos));
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
	public void openScope(){
		table.add(new HashMap<String,Declaration>());
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
		if(previousDecl!=null){//checking current scope if in scopes 0,1,or 2
			if(previousDecl instanceof MemberDecl && decl instanceof MemberDecl){//scope 2
				if(((MemberDecl) previousDecl).c.name.equals(((MemberDecl)decl).c.name)){
					reporter.reportError("*** Identification error: duplicate member name in same class: "+name+" already declared at "+previousDecl.posn+" Position:"+decl.posn);
					throw new SyntaxError();
				}
				else{ //we are ok, same member name but different classes
					currentScope.put(name, decl);
					return;
				}
			}//otherwise we are dealing with scope 1 so class name issues
			reporter.reportError("*** Identification error: duplicate class name: "+name+" already declared at "+previousDecl.posn+" Position:"+decl.posn);
			throw new SyntaxError();
		}
		//we are good, add to current scope
		currentScope.put(name, decl);
		
	}
	public Declaration retrieve(String id){
		Declaration d = null;
		for(int i=(table.size()-1);i>=0;i--){
			d=table.get(i).get(id);
			if(d!=null){
				System.out.println("Retrieved decl "+d.name+"at level "+i);
				return d;
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
	}
	
}
