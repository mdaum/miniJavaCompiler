package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGenerator.CodeFarm;
import miniJava.ContextualAnalyzer.IdentificationStation;
import miniJava.ContextualAnalyzer.TypeChecker;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;


public class Compiler {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		InputStream inputStream = null;
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = System.in;
		} else {
			try {
				inputStream = new FileInputStream(args[0]);
			} catch (FileNotFoundException e) {
				System.out.println("Input file " + args[0] + " not found");
				System.exit(1);
			}
		}

		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);

		System.out.println("Syntactic analysis ... ");
		/*
		 * //begin scanning-only Token curr= scanner.scan();
		 * while(!curr.kind.equals(TokenKind.eot)&&!curr.kind.equals(TokenKind.
		 * err)){ System.out.println(curr.toString()); curr=scanner.scan(); }
		 * //end scanning-only
		 */
		AST goo = parser.parse();
		ASTDisplay display = new ASTDisplay();
		// display.showTree(goo);
		System.out.print("Syntactic analysis complete:  ");
			System.out.println("Contextual Analysis ... ");
			IdentificationStation i = new IdentificationStation();
			if (goo != null) goo = i.Decorate(goo, reporter);
			if (reporter.hasErrors()) {
				System.out.println("INVALID MiniJava");
				System.exit(4);
			}
			TypeChecker t = new TypeChecker(goo, reporter);
			t.typeCheck();

			System.out.println("Contextual Analysis complete: ");
			

			if (reporter.hasErrors()) {
				System.out.println("INVALID MiniJava");
				System.exit(4);
			} else {
				System.out.println("valid MiniJava");
			}
			System.out.println("Generating Code...");
			CodeFarm farm= new CodeFarm(reporter,i.mainMethod);
			farm.generateCode(goo);
			if(reporter.hasErrors()){
				System.out.println("Error During Code Generation");
				System.exit(4);
			}
			System.out.println("Code Generation complete!");
			
			String objFileName = args[0].substring(0, args[0].lastIndexOf("."));
			
			ObjectFile objF = new ObjectFile(objFileName+".mJAM");
			
			System.out.print("Writing object code file " + objFileName + " ... ");
			if (objF.write()) {
				System.out.println("FAILED!");
				System.exit(4);
			}
			else System.out.println("SUCCEEDED");
			
			System.exit(0);
		
	}
}
