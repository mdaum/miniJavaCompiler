package miniJava.CodeGenerator;

public class KnownAddress extends RuntimeEntity{
	public int displacement;//really displacement
	
	  public KnownAddress () {
		    super();
		    displacement = -1; //will act as bad address...or rather unknown...
		  }
	public KnownAddress(int size,int address){//for var decl normally
		super(size);
		this.displacement=address;
	}
}
