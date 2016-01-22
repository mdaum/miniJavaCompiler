package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import miniJava.ErrorReporter;


public class Scanner {
	  	private char currentChar;
	    private InputStream inputStream;
	    private ErrorReporter reporter;
	    private Position position;
	    private boolean eot = false;
	    private StringBuilder currentString;
	    
	    
	    public Scanner(InputStream inputStream, ErrorReporter reporter) {
	        this.inputStream = inputStream;
	        this.reporter = reporter;
	        this.currentChar=' ';
	        this.currentString=new StringBuilder();
	        this.position=new Position(1,0);
	    }
	    
	    private void nextChar() {  //TODO make better lol
	        try { 
	            int rr = inputStream.read(); 
	            if (rr == -1) 
	                eot = true;
	            else{
	            	currentChar = (char) rr;
	            }
	        } 
	        catch (IOException e) { 
	        	System.out.println(e.getMessage());
	            eot = true;
	        }
	    }
	    
	    
	    
	    //util methods
	    private boolean isDigit(char c) {
	        return (c >= '0') && (c <= '9');
	    }
	    
	    private void scanError(String m) {
	    	reporter.reportError("Scan Error:  " + m);

	    }
	    private boolean isLetter(char c){
	    	return ((c >='A'&&c<='Z')||(c>='a'&&c<='z'));
	    }
}
