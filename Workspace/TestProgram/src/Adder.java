import java.io.*; 
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Adder {
	
	public static void main(String args[]) throws IOException {
		Integer x = 6;
		Integer y = 6;
		int a = 8;
		int b = 10;
		String name; 
		//BufferedReader reader; 
		int[] arr = {15, 15, 15, 16};
		ArrayList<Integer> arr2 = new ArrayList<Integer> ();
		arr2.add(1);
		arr2.add(5);
		arr2.add(7);
		//ArrayList<ArrayList<Integer>> al = new ArrayList<ArrayList<Integer>>();
		//al.add(arr2);
		Integer z = x + y;
		
		ContentStructure cs = new ContentStructure("test", "test", "test", 1, 1, new ArrayList<ContentStructure>(), null);
		ContentStructure cs2 = new ContentStructure("test2", "tes2t", "te2st", 2, 21, new ArrayList<ContentStructure>(), null);
		cs.contents.add(cs2);
		cs2.contents.add(cs);
		cs2.contents.add(new ContentStructure("test32", "te3s2t", "te23st", 32, 231, new ArrayList<ContentStructure>(), null));
		cs2.contents.add(cs);
		System.out.println(z);
		System.in.read();
		
		
		//reader = new BufferedReader(new InputStreamReader(System.in)); 
		//name = reader.readLine(); 
	}
}
