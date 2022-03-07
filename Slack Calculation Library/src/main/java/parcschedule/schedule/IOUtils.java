package parcschedule.schedule;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import net.sourceforge.gxl.GXLDocument;
import net.sourceforge.gxl.GXLEdge;
import net.sourceforge.gxl.GXLFloat;
import net.sourceforge.gxl.GXLGraph;
import net.sourceforge.gxl.GXLInt;
import net.sourceforge.gxl.GXLNode;
import parcschedule.schedule.model.Proc;

public class IOUtils 
{
	
	public static void writeScheduleToGXL(File file, OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule, StringBuilder sb)
	{
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = schedule.taskGraph();
		System.out.println("\nSlack calculated by previous app: "+ schedule.slackTime());
		GXLDocument document = new GXLDocument();
		GXLGraph gxlGraph = new GXLGraph("GRAPH");
		
		DecimalFormat df = new DecimalFormat("0.00");
		gxlGraph.setAttr("No of nodes", new GXLInt(taskGraph.sizeTasks()));
		gxlGraph.setAttr("No of edges", new GXLInt(taskGraph.sizeEdges()));
		gxlGraph.setAttr("Density", new GXLFloat(Float.parseFloat(df.format(taskGraph.density()))));
		gxlGraph.setAttr("CCR", new GXLFloat(Float.parseFloat(df.format(taskGraph.getCCR()))));
		gxlGraph.setAttr("Total sequential time",new GXLInt(taskGraph.getTotalComputationCost()));

		gxlGraph.setAttr("Number of Processors", new GXLInt(schedule.system().processors().size()));
		gxlGraph.setAttr("Total Schedule length", new GXLInt(schedule.scheduleLength()));

		sb.append(taskGraph.sizeTasks()+",");
		sb.append(taskGraph.sizeEdges()+",");
		sb.append(schedule.system().numProcs()+",");
		sb.append(schedule.scheduleLength() * schedule.system().numProcs()+",");

		for(TaskVertex t : taskGraph.tasks())
		{
			GXLNode n = new GXLNode(t.name());
			n.setAttr("Weight",new GXLInt(t.weight()));		
			n.setAttr("Start time", new GXLInt(schedule.taskStartTime(t)));
			n.setAttr("Processor", new GXLInt(Integer.parseInt(schedule.taskProcAllocation(t).id())));
			gxlGraph.add(n);
		}
		
		for(CommEdge<TaskVertex> e : taskGraph.edges())
		{
			GXLEdge edge = new GXLEdge(e.from().name(),e.to().name());
			edge.setAttr("Weight",new GXLInt(e.weight()));
			gxlGraph.add(edge);
		}
		
		document.getDocumentElement().add(gxlGraph);
		
		try 
		{
			document.write(file);
		}
		catch (IOException e) 
		{
			System.err.println("Error while writing to file: " + e);
		}		
	}
	
}
