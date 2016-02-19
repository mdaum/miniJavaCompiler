package miniJava.SyntacticAnalyzer;

public class Token {
	public TokenKind kind;
	public String spelling;
	public SourcePosition posn;

	public Token(TokenKind kind, String spelling, SourcePosition p) {
		this.kind = kind;
		this.spelling = spelling;
		this.posn=p;
	}
	
	public String toString(){
		return this.spelling+" is a "+this.kind;
	}
}
