package miniJava.CodeGenerator;

import java.util.ArrayList;

public class MethodEntity extends RuntimeEntity{

	public MethodEntity(){
		super();
		this.addresses=new ArrayList<Integer>();
		this.patched=false;
	}
	public ArrayList<Integer> addresses;
	public boolean patched;
	public int realAddress;
}
