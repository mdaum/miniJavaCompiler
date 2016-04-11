package miniJava.CodeGenerator;

public class ClassEntity extends RuntimeEntity{
	public int size;
	public int numMethods;
	public int displacement;
	public ClassEntity(){
		this.size=0;
	}
	public ClassEntity(int size, int numMethods, int displacement){
		this.size=size;
		this.numMethods=numMethods;
		this.displacement=displacement;
	}
}
