package miniJava.ContexualAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class IDTable {
	
	ArrayList<HashMap<String,Declaration>> table = new ArrayList<HashMap<String,Declaration>>(); //stack of hash tables, nested block structure
	
	public IDTable(){
		openScope();//opening predefined level
		//creating predifined classes...
		
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
	
}
