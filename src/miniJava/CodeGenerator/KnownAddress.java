package miniJava.CodeGenerator;

public class KnownAddress extends RuntimeEntity {
	public int address;
	public KnownAddress(){
		super();
		this.address=0;
	}
	public KnownAddress(int size, int address){
		super(size);
		this.address=address;
	}
}
