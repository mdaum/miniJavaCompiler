package miniJava.ContexualAnalyzer;

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
		//creating predifined classes...
		this.reporter=e;
		
		SourcePosition predefPos=new SourcePosition(-1,-1);//predefined sourcePos
		
		//starting with _PrintStream
		MethodDeclList printStreamMethods=new MethodDeclList();
		ParameterDeclList printlnParam=new ParameterDeclList();
		printlnParam.add(new ParameterDecl(new BaseType(TypeKind.INT,predefPos),"n", predefPos));
		MemberDecl printStreamMember=new FieldDecl(false,false,new BaseType(TypeKind.VOID,predefPos),"println",predefPos);
		printStreamMethods.add(new MethodDecl(printStreamMember, printlnParam, new StatementList(), predefPos));
		ClassDecl printStreamDecl = new ClassDecl("_PrintStream", new FieldDeclList(), printStreamMethods, predefPos);
		
		//now String
		ClassDecl stringDecl= new ClassDecl("String",new FieldDeclList(),new MethodDeclList(),predefPos);
		
		//now System
		FieldDeclList systemfdl= new FieldDeclList();
		Token systemToken = new Token(TokenKind.id,"_PrintStream",predefPos);
		systemfdl.add(new FieldDecl(false,true,new ClassType(new Identifier(systemToken),predefPos),"out",predefPos));
		ClassDecl systemDecl= new ClassDecl("System",systemfdl,new MethodDeclList(),predefPos);
		
		//now add them to current scope
		try{
			enter(printStreamDecl);
			enter(stringDecl);
			enter(systemDecl);
		}catch (SyntaxError err){
			System.out.println("impossible!");
		}
		
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
	
	public void enter(Declaration decl){ //will report within enter, so I can get both pieces of decl....
		String name = decl.name;
		for(int i =3;i<table.size();i++){ //cannot hide from parameter and higher if you are local
			Declaration previousDecl = table.get(i).get(name);
			if(previousDecl!=null){
				reporter.reportError("*** Identification error: duplicate variable name: "+name+" \n already declared at "+previousDecl.posn+"\n Position:"+decl.posn);
				throw new SyntaxError();
			}
		} //not in previous scopes if currently in scopes 3+
		HashMap<String,Declaration> currentScope=table.get(table.size()-1);
		Declaration previousDecl=currentScope.get(name);
		if(previousDecl!=null){//checking current scope if in scopes 0,1,or 2
			reporter.reportError("*** Identification error: duplicate variable name: "+name+" \n already declared at "+previousDecl.posn+"\n Position:"+decl.posn);
			throw new SyntaxError();
		}
		//we are good, add to current scope
		currentScope.put(name, decl);
		
	}
	
	class SyntaxError extends Error{
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID=1L;
	}
	
}
