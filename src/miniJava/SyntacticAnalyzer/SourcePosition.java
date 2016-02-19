package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	public int row,col;
	public SourcePosition(){
		this.row=1;
		this.col=1;
	}
	public SourcePosition(int row,int col){
		this.row=row;
		this.col=col;
	}
	public String toString(){
		return "line " + row + ", space " + col;
	}
}
