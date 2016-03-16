package miniJava.SyntacticAnalyzer;

public enum TokenKind {
	pub,statik,priv,interger,bool,ret,nu,clazz,voyd,dot, //reserved
	semicol,comma,thiz,iff,wile,elsz,tru,fals,equals,nul //reserved
	,binop // > < == <= >= != && || - + * /
	,unop, // !
	bunop, //-
	lparen,rparen, // () 
	lcurly,rcurly, // {}
	id,num	//char int
	,lbrack,rbrack //[]
	,eot//end of text
	,err//error
	,comment
	;
}
