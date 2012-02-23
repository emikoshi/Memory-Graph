package src2;
import java.util.ArrayList;


public class ContentStructure {
	String name;
	String value;
	String type;
	/* types:
	 * 0 = head (only contains threads)
	 * 1 = thread (only contains stacks)
	 * 2 = stack (?)
	 * 3 = 
	 */
	long id;
	long uid;
	ArrayList<ContentStructure> contents;
	Object original;
	
	public ContentStructure(String n, String v, String t, long i,long ui, ArrayList<ContentStructure> c, Object o) {
		this.name = n;
		this.value = v;
		this.type = t;
		this.id = i;
		this.uid = ui;
		this.contents = c;
		this.original = o;
	}
	
}
