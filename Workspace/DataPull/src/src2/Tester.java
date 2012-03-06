package src2;

import java.util.ArrayList;

import javax.swing.JFrame;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxIGraphModel;

public class Tester {
	public static void main(String args[]) {
		ContentStructure test = RandomContentStructures();
		//Main.print_content_structure(test, "");
		Grapher frame = new Grapher(test);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1000, 900);
		frame.setVisible(true);
		System.out.println("WEGSBFDSBDHDHDSFHDSFHD");
	}
	public static ContentStructure RandomContentStructures(/*ContentStructure cs1/*,ContentStructure cs2, ContentStructure cs3, ContentStructure cs4,ContentStructure cs5*/ ){
		// for(ContentStructure current_cs : cs){}
		ArrayList<ContentStructure> arrRandom = new ArrayList<ContentStructure> ();
		ContentStructure jesus = new ContentStructure("Jesus", "asdfjkl", "string", 8, 9,new ArrayList<ContentStructure>(), null);
		arrRandom.add(new ContentStructure("Sam", "175", "stack_frame", 1, 2,new ArrayList<ContentStructure>(), null));
		arrRandom.add(new ContentStructure("Steve", "15","stack_frame", 5, 7,new ArrayList<ContentStructure>(), null));

		

		ArrayList<ContentStructure> arrRandom2 = new ArrayList<ContentStructure>();
		arrRandom2.add(jesus);
		arrRandom.add(new ContentStructure("Jane", "hahha", "stack_frame", 1, 2,new ArrayList<ContentStructure>(), null));
		arrRandom.add(new ContentStructure("Mike",  "55","stack_frame", 3, 4, arrRandom2, null));
		
		
		ContentStructure cs1 = new ContentStructure("bobby", "5", "thread", 5, 7, arrRandom , null);
		jesus.contents.add(cs1);
		jesus.contents.add(new ContentStructure("Samerwqfd", "175", "string", 1, 2,new ArrayList<ContentStructure>(), null));
		jesus.contents.add(new ContentStructure("Samerwqf324d", "175", "string", 1, 2,new ArrayList<ContentStructure>(), null));
		ContentStructure loop = new ContentStructure("loop", "x", "object", 1, 2, new ArrayList<ContentStructure>(), null);
		loop.contents.add(jesus);
		jesus.contents.add(new ContentStructure("Samerwqfd234", "175", "string", 1, 2,new ArrayList<ContentStructure>(), null));
		jesus.contents.add(loop);
		
		ContentStructure head = new ContentStructure("bob", "head", "5", 5, 8, new ArrayList<ContentStructure>() , null);
		head.contents.add(cs1);
		System.out.println(head.contents);
		System.out.println(jesus.contents);
		//cs2 = new ContentStructure("jack", "44", "67", 2, 66, ArrayList<ContentStructure> c, null) ;
		// cs3 = new ContentStructure("jill", "7", "235", 11, 99, ArrayList<ContentStructure> b, null) ;
		//cs4 = new ContentStructure("hanzel", "19", "854", 2, 13, ArrayList<ContentStructure> d, null) ;
		//cs5 = new ContentStructure("grettle", "12", "51", 55, 41, ArrayList<ContentStructure> c, null) ;
		 
		return head;
		 
		 
	}
}
