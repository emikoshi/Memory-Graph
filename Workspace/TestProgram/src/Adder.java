import java.io.*; 

public class Adder {
	public static void main(String args[]) throws IOException {
		int x = 5;
		int y = 6;
		String name; 
		BufferedReader reader; 
		
		int z = x + y;
		reader = new BufferedReader(new InputStreamReader(System.in)); 
		name = reader.readLine(); 
	}
}
