package miniJava.SyntacticAnalyzer;

public class Position {
	public int row,col;
	public Position(){
		this.row=1;
		this.col=1;
	}
	public Position(int row,int col){
		this.row=row;
		this.col=col;
	}
	public String toString(){
		return "line " + row + ", space " + col;
	}
}
