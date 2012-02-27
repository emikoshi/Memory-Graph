package src2;
import javax.swing.JFrame;


import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class Grapher extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Grapher(ContentStructure cs)
	{
		super("Hello, World!");
		mxGraph graph = new mxGraph();
		Object parent = graph.getDefaultParent();
		
		
		try
		{
			for (ContentStructure i:cs.contents) {
				dfs(parent, null, graph, i, 1, 1);
			}
			
		}
		finally
		{
			graph.getModel().endUpdate();                        
		}

		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		getContentPane().add(graphComponent);            
	}

	public static int count_stacks(ContentStructure cs) {
		if (cs.type.equals("thread")) return cs.contents.size();
		else {
			for (ContentStructure i:cs.contents) count_stacks(i);
		}
		return 0;
	}
	
	public static void dfs(Object defaultParent, Object parent, mxGraph graph, ContentStructure cs, double x, double y) {
		Object newParent;
		if (cs.type.equals("thread")) {
			newParent = graph.insertVertex(defaultParent, null, (cs.name.length() >= 10 ? cs.name.substring(0,10) : cs.name), 1, (720/2 - (150/2)), 150, 30, "fillColor=#7F15CB;fontColor=white", false);
		} else if(cs.type.equals("stack_frame")) {
			newParent = graph.insertVertex(defaultParent, null, (cs.name.length() >= 10 ? cs.name.substring(0,10) : cs.name), 160+x, y, 150, 30, "fillColor=#F4EC80;", false);
		} else {
			newParent = graph.insertVertex(defaultParent, null, (cs.name.length() >= 10 ? cs.name.substring(0,10) : cs.name), 350+x, y, 100, 25, "fillColor=#80B1F4;", false);
		}
		
		if (parent != null) graph.insertEdge(defaultParent, null, "", parent, newParent);
		double x_pos = 0, y_pos = 0;
		int count = 0;
		
		for (ContentStructure i:cs.contents) {
			if (i.type.equals("thread")) {
				
			}
			if (i.type.equals("stack_frame")) {
				dfs(defaultParent, newParent, graph, i, x, (720/cs.contents.size())*count + 5);
				count++;
			}
			if (!(i.type.equals("stack_frame") || i.type.equals("thread"))) { 
				dfs(defaultParent, newParent, graph, i, x+x_pos, y+y_pos);
				x_pos += 115;
				y_pos += 35;
			}
			else y_pos+=45;
		}
	}
}
