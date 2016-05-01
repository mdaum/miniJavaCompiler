##Comp 520 Project

###AST Changelog:
- **null literal added**
	1. added nullLiteral.java
	2. added visitNullLiteral to Visitor interface
	3. updated ASTDisplay to reflect this
- **Declaration attributes added to Identifier and Reference**

- **added SourcePosition to Identifier for better Error Reporting**
- **added isArrayLength bool to FieldDecl for easier checking of edge case**
- **added RuntimeEntity entity to Declaration.java for codeGen**