package miniJava.CodeGenerator;

public abstract class RuntimeEntity {
	public int size;
	public int address;
	
	public RuntimeEntity(){
		this.size=0;
		this.address=0;
	}
	public RuntimeEntity(int size, int address){
		this.size=size;
		this.address=address;
	}
}
