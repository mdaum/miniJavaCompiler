package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import miniJava.ErrorReporter;
//test

public class Scanner {
	  	private char currentChar;
	    private InputStream inputStream;
	    private ErrorReporter reporter;
	    private Position position;
	    private boolean eot = false;
	    private StringBuilder currentSpelling;
	    
	    
	    public Scanner(InputStream inputStream, ErrorReporter reporter) {
	        this.inputStream = inputStream;
	        this.reporter = reporter;
	        this.position=new Position(0,0);
	        readChar();//start it off
	    }
	    
	    public Token scan() {
	    	//skip whitespace
	    	while(!eot && isWhiteSpace(currentChar)){
	    		skipIt();
	    	}
	    	currentSpelling=new StringBuilder();
	    	TokenKind kind = scanToken();
	    	return new Token(kind,currentSpelling.toString()); //placeholder
	    }
	    
	    private TokenKind scanToken(){ //using a comment token for now instead of looping, might want to change later
	    	switch(currentChar){
	    	
	    	case '/': //comments handled here too....
	    		takeIt();
	    		if(currentChar=='/'){
	    			//clearing spelling
	    			currentSpelling.deleteCharAt(0);
	    			while(!isNewLine(currentChar)){
	    				skipIt();
	    			}
	    			while(isWhiteSpace(currentChar)){ //get rid of spaces after comment
	    				skipIt();
	    			}return TokenKind.comment;
	    		}
	    		else if(currentChar=='*'){
	    			//clearing
	    			currentSpelling.deleteCharAt(0);
	    			while(true){
	    				if(currentChar=='*')skipIt();
	    				if(currentChar=='/')break;
	    				skipIt();
	    			}
	    			skipIt();//getting rid of / after *
	    			while(isWhiteSpace(currentChar)){
	    				skipIt();
	    			}return TokenKind.comment;
	    		}
	    		else return TokenKind.binop;
	    	
	    	case '.':
	    		takeIt();
	    		return TokenKind.dot;	
	    		
	    	case ';':
	    		takeIt();
	    		return TokenKind.semicol; 		
	    		
	    	case ',':
	    		takeIt();
	    		return TokenKind.comma;
	    		
	    	case '+':
	    	case '*':
	    		takeIt();
	    		return TokenKind.binop;
	    	
	    	case '(':
	    		takeIt();
	    		return TokenKind.lparen;
	    		
	    	case ')':
	    		takeIt();
	    		return TokenKind.rparen;
	    		
	    		
	    	case '{':
	    		takeIt();
	    		return TokenKind.lcurly;
	    		
	    	case '}':
	    		takeIt();
	    		return TokenKind.rcurly;
	    	
	    	case '[':
	    		takeIt();
	    		return TokenKind.lbrack;
	    	
	    	case ']':
	    		takeIt();
	    		return TokenKind.rbrack;
	    	
	    	case '-':
	    		takeIt();
	    		return TokenKind.bunop;
	    		
	    	case '=': //either binop or assignment
	    		takeIt();
	    		if(currentChar=='='){
	    			takeIt();
	    			return TokenKind.binop;
	    		}
	    		return TokenKind.equals;
	    		
	    	case '>':
	    	case '<':
	    		takeIt();
	    		if(currentChar=='='){
	    			takeIt();
	    		}return TokenKind.binop;
	    	
	    	case '&':
	    		takeIt();
	    		if(currentChar=='&'){
	    			takeIt();
	    			return TokenKind.binop;
	    		}
	    		else scanError("& is not a valid token, && is");
	    		
	    	case '|':
	    		takeIt();
	    		if(currentChar=='|'){
	    			takeIt();
	    			return TokenKind.binop;
	    		}
	    		else scanError("| is not a valid token, || is");
	    		
	    	case '!':
	    		takeIt();
	    		if(currentChar=='='){
	    			takeIt();
	    			return TokenKind.binop;
	    		}
	    		return TokenKind.unop;
	    		
	    	default:
	    		if(isLetter(currentChar)){//start of identifier
	    			takeIt();
	    			while(isLetter(currentChar)||isDigit(currentChar)||currentChar=='_'){
	    				takeIt();
	    			} //check the reserved
	    			if(currentSpelling.toString().equals("public"))return TokenKind.pub;
	    			if(currentSpelling.toString().equals("static"))return TokenKind.statik;
	    			if(currentSpelling.toString().equals("private"))return TokenKind.priv;
	    			if(currentSpelling.toString().equals("int"))return TokenKind.interger;
	    			if(currentSpelling.toString().equals("boolean"))return TokenKind.bool;
	    			if(currentSpelling.toString().equals("return"))return TokenKind.ret;
	    			if(currentSpelling.toString().equals("new"))return TokenKind.nu;
	    			if(currentSpelling.toString().equals("class"))return TokenKind.clazz;
	    			if(currentSpelling.toString().equals("void"))return TokenKind.voyd;
	    			if(currentSpelling.toString().equals("this"))return TokenKind.thiz;
	    			if(currentSpelling.toString().equals("if"))return TokenKind.iff;
	    			if(currentSpelling.toString().equals("while"))return TokenKind.wile;
	    			if(currentSpelling.toString().equals("else"))return TokenKind.elsz;
	    			if(currentSpelling.toString().equals("true"))return TokenKind.tru;
	    			if(currentSpelling.toString().equals("false"))return TokenKind.fals;
	    			return TokenKind.id;
	    		}
	    		if(isDigit(currentChar)){//start of num
	    			takeIt();
	    			while(isDigit(currentChar)){
	    				takeIt();
	    			}
	    			return TokenKind.num;
	    		}
	    		scanError("Unrecognized character '" + currentChar + "' in input at: "+position.toString());
	    		return TokenKind.err;
	    	}
	    }
	    
	    private void takeIt(){
	    	currentSpelling.append(currentChar);
	    	nextChar();
	    }
	    
	    private void skipIt(){
	    	nextChar();
	    }
	    
	    private void nextChar(){
	    	if(!eot){
	    		readChar();
	    	}
	    }
		private final static char eolUnix = '\n';
		private final static char eolWindows = '\r';
	    private void readChar() {  //TODO make better lol
	        try { 
	            int rr = inputStream.read(); 
	            currentChar = (char) rr;  
	            if (rr == -1) 
	                eot = true;
	            else if(currentChar==eolUnix){// for positioning
	            	//System.out.println("hit");
	            	position.col=0;
	            	position.row+=1;
	            }
	            else if(currentChar==eolWindows){
	            	skipIt();
	            }
	            else position.col++;
	        } 
	        catch (IOException e) { 
	        	scanError(e.getMessage() + "at: "+position.toString());
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
	    private boolean isNewLine(char c){
	    	return (c == '\r' || c == '\n');
	    }
	    private boolean isWhiteSpace(char c){
	    	return (c == ' ' || c == '\t' || isNewLine(c));
	    }
}
