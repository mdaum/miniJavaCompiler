package miniJava.ContexualAnalyzer;

public class IdentificationStation {

}
/*General Notes for traversal:
	Look at DisplayAST.java, super helpful stuff there.
	So we need to make a data structure (maybe array of hash maps?) in order to track our id process
	openscope and closescope can be choosing which hash map we are messing with
	then we can shimmy down AST just like DisplayAST and we can start populating table instead of printing..
	IMPORTANT: Check if this traverses in a way that can handle local decl vs class and member decls!!
	for above: check this....https://en.wikipedia.org/wiki/Tree_traversal#In-order
	linking should happen on this pass
	this identification station will implemement Visitor<idtable structure, Object>
	so, every visit method will look like: public Object visitXXX(XXX name, idtable structure) and will return null
	pass the idtable down each level of traversal and it will become populated*/
	
