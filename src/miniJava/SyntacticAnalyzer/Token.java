package miniJava.SyntacticAnalyzer;

public class Token {
	public TokenKind kind;
	public String spelling;

	public Token(TokenKind kind, String spelling) {
		this.kind = kind;
		this.spelling = spelling;
	}
	
	public String toString(){
		return this.spelling+" is a "+this.kind;
	}
}
