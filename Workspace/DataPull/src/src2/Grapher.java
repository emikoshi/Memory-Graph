package src2;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JFrame;


import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.sun.jdi.Value;

public class Grapher extends JFrame {

	/**
	 * 
	 */
	private ContentStructure cs;
	
	private static final long serialVersionUID = 1L;
	public Grapher(ContentStructure cs)
	{		
		super("Hello, World!");
		
		this.cs = cs;
		
		//mxGraph graph = new mxGraph();
		//Object parent = graph.getDefaultParent();
		mxIGraphModel model = new mxGraphModel();
		final mxGraph graph = new mxGraph(model);
		Object parent = graph.getDefaultParent();
		final mxCompactTreeLayout m = new mxCompactTreeLayout(graph, false);
		final mxGraphComponent graphComponent = new mxGraphComponent(graph);
		
		try
		{
			for (ContentStructure i:cs.contents) {
				HashSet<ContentStructure> seen = new HashSet<ContentStructure>();
				HashMap<ContentStructure, Object> graph_map = new HashMap<ContentStructure, Object>();
				
				dfs(parent, null, graph, i, 1, 1, seen, graph_map);
				System.out.println("done.");
			}
			
		}
		finally
		{
			graph.getModel().endUpdate();                        
		}

		//mxGraphComponent graphComponent = new mxGraphComponent(graph);
		getContentPane().add(graphComponent);         
		m.execute(parent);
	}

//	private int count_stacks() {
//		if (cs.type.equals("thread")) return cs.contents.size();
//		else {
//			for (ContentStructure i:cs.contents) count_stacks(i);
//		}
//		return 0;
//	}
	
	
	//public static int hash
	
	// defaultParent -- first node in the grpah (the one that is not displayed)
	// parent -- the parent node of this current node
	// graph -- the graph (global variable)
	// cs -- the current ContentStructure we are examining  (a member of the contents of the parent node)
	// x -- x coordinate
	// y -- y coordinate
	
	private void dfs(Object defaultParent, Object parent, mxGraph graph, ContentStructure cs, double x, double y, HashSet<ContentStructure> seen, HashMap<ContentStructure, Object> gm) {	
		Object newParent;
		// create a new parent, this is the new node we will now insert into the graph
		if (cs.type.equals("thread")) {
			newParent = graph.insertVertex(defaultParent, null, (cs.name.length() >= 10 ? cs.name.substring(0,10) : cs.name), 1, (720/2 - (150/2)), 150, 30, "fillColor=#7F15CB;fontColor=white", false);
			gm.put(cs, newParent);
		} else if(cs.type.equals("stack_frame")) {
			newParent = graph.insertVertex(defaultParent, null, (cs.name.length() >= 10 ? cs.name.substring(0,10) : cs.name), 160+x, y, 150, 30, "fillColor=#F4EC80;", false);
			gm.put(cs, newParent);
		} else {
			newParent = graph.insertVertex(defaultParent, null, (cs.name.length() >= 10 ? cs.name.substring(0,10) : cs.name), 350+x, y, 100, 25, "fillColor=#80B1F4;", false);
			gm.put(cs, newParent);
		}
		
		// if the parent isn't null, insert an edge between the parent and newParent
		if (parent != null) graph.insertEdge(defaultParent, null, "", parent, newParent);
		
		// right now our position calculation sets x and y to 0.
		double x_pos = 0, y_pos = 0;
		
		// counter variable to count how many nodes are within this stack frame
		int count = 0;
		
		if (cs.type.equals("thread")) {
			System.out.println("There are " + cs.contents.size() + " stacks");
		}
		if (cs.contents.size() > 100) {
			newParent = graph.insertVertex(defaultParent, null, (cs.name.length() >= 10 ? cs.name.substring(0,10) : cs.name)+"(...)", 350+x, y, 100, 25, "fillColor=#80B1F4;", false);
			graph.insertEdge(defaultParent, null, "", parent, newParent);
		} else {
			for (ContentStructure current_cs:cs.contents) {
				if (current_cs.type.equals("stack_frame")) {	
					System.out.println("Stack: " + current_cs.name + " value=" + cs.value + "type=" + cs.type + "hashcode=" + cs.hashCode());
					if (seen.contains(current_cs)) {
						System.out.println("there is a cycle .. drawing the edge only.");
						
						// draw edge
					} else {
						seen.add(current_cs);
						dfs(defaultParent, newParent, graph, current_cs, x, (720/cs.contents.size())*count + 5, seen, gm);						
					}
					count++;
				}
				
				if (!(current_cs.type.equals("stack_frame") || current_cs.type.equals("thread"))) {
					System.out.println("Now graphing " + current_cs.name + " type=" + current_cs.type + ", size=" + current_cs.contents.size() + "hashcode=" + cs.hashCode());
					if (seen.contains(current_cs)) {
						System.out.println("we are in: " + current_cs.name + " and there is a cycle .. drawing the edge only.");
						graph.insertEdge(defaultParent, null, "", newParent, gm.get(current_cs));
						System.out.println(gm);
						// draw edge
					} else {
						seen.add(current_cs);
						dfs(defaultParent, newParent, graph, current_cs, x+x_pos, y+y_pos, seen, gm);						
					}
					
					x_pos += 115;
					y_pos += 35;
				}
				else y_pos+=45;
			}
		}
	}
}
