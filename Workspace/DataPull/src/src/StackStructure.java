package src;

import java.util.ArrayList;

public class StackStructure extends ContentStructure {
	int index;
	
	public StackStructure(String n, String v, String t, long i, ArrayList<ContentStructure> c, Object o, int in) {
		super(n, v, t, i, c, o);
		this.index = in;
		// TODO Auto-generated constructor stub
	}
}
