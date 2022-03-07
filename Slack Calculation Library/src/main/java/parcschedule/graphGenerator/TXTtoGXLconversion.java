package parcschedule.graphGenerator;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;


/**
 * Converts a text file of specified format to a GXL file.   
 * 
 */
public class TXTtoGXLconversion {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> g = GraphGenerator.generate(args[0]);
		OutputGXL output = new OutputGXL();
		output.generateGXLFile(args[1], args[1].substring(0,args[1].length()-4), g);
	}
}
