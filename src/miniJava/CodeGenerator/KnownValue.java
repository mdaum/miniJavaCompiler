package miniJava.CodeGenerator;

public class KnownValue extends RuntimeEntity {
	public int value;
	
	public KnownValue(){
		super();
		this.value=0;
	}
	public KnownValue(int size, int value){
		super(size);
		this.value=value;
	}
}
