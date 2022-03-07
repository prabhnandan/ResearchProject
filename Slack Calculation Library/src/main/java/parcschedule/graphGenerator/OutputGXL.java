package parcschedule.graphGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

import net.sourceforge.gxl.*;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

/**
 * Responsible for generating an output file representing 
 * the optimal schedule in GXL format.
 */
public class OutputGXL {
	
	private GXLGraph GXLGraph;
	
	/**
	 * Generates a GXL file from a TaskGraph object. 
	 * 
	 * @param fileName		the name of the GXL file that needs to be generated
	 * @param graphName		the name of the task graph
	 * @param taskGraph		the task graph
	 */
	public void generateGXLFile(String fileName,String graphName, TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph){
		GXLDocument document = new GXLDocument();
		GXLGraph = new GXLGraph(graphName);
		
		DecimalFormat df = new DecimalFormat("0.00");
		GXLGraph.setAttr("No of nodes", new GXLInt(taskGraph.sizeTasks()));
		GXLGraph.setAttr("No of edges", new GXLInt(taskGraph.sizeEdges()));
		GXLGraph.setAttr("Density", new GXLFloat(Float.parseFloat(df.format(taskGraph.density()))));
		GXLGraph.setAttr("CCR", new GXLFloat(Float.parseFloat(df.format(taskGraph.getCCR()))));
		GXLGraph.setAttr("Total sequential time",new GXLInt(taskGraph.getTotalComputationCost()));
		
		Iterable<TaskVertex> tasks = taskGraph.tasks();
		for(TaskVertex t : tasks){
			addTask(t);
		}
		
		Iterable<CommEdge<TaskVertex>> edges = taskGraph.edges();
		for(CommEdge<TaskVertex> e : edges){
			addEdge(e);
		}
		
		document.getDocumentElement().add(GXLGraph);
		try {
			if (fileName.endsWith(".gxl")) {
				if (fileName.indexOf('/') > 0)
				{
					Files.createDirectories(Paths.get(fileName).getParent());
				}
				new File(fileName).createNewFile();
				document.write(new File(fileName));
			}
			else { // fileName is actually path name TODO: make this consistent.
				(new File(fileName)).mkdirs();
				document.write(new File(fileName+"/"+graphName+".gxl"));
			}
		}
		catch (IOException ioe) {
			System.err.println("Error while writing to file: "+ioe);
		}
		
		System.out.println(fileName + "/" + graphName);
	}
	
	/**
	 * 
	 * @param e		the Edge object that needs to be added
	 */
	private void addEdge(CommEdge<TaskVertex> e) {
		GXLEdge edge = new GXLEdge(e.from().name(),e.to().name());
		edge.setAttr("Weight",new GXLInt(e.weight()));
		GXLGraph.add(edge);
	}
	
	/**
	 * Adds the Task to the GXL file.
	 * 
	 * @param t		the Task to be added
	 */
	private void addTask(TaskVertex t){
		GXLNode n = new GXLNode(t.name());
		n.setAttr("Weight",new GXLInt(t.weight()));
		GXLGraph.add(n);
	}
}
