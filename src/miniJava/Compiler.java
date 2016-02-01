package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Compiler {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		InputStream inputStream = null;
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = System.in;
		}
		else {
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
/*	//begin scanning-only
		Token curr= scanner.scan();
		while(!curr.kind.equals(TokenKind.eot)&&!curr.kind.equals(TokenKind.err)){
			System.out.println(curr.toString());
			curr=scanner.scan();
		}
		//end scanning-only
*/		
		parser.parse();
		System.out.print("Syntactic analysis complete:  ");
		if (reporter.hasErrors()) {
			System.out.println("INVALID MiniJava");
			System.exit(4);
		}
		else {
			System.out.println("valid MiniJava");
			System.exit(0);
		}
	}

}
