package parcschedule.graphGenerator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import parcschedule.schedule.BasicCommEdge;
import parcschedule.schedule.BasicTaskGraph;
import parcschedule.schedule.BasicTaskVertex;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

/**
 * Responsible for generating a task graph object from a text file 
 * representing a task graph in a specified format.
 */
public class GraphGenerator {

	private static BufferedReader fRead;
	private static final String EXCLAMATION = "!";

	/**
	 * Returns a task graph object by parsing a given text file.
	 * 
	 * @param fileName	the text file containing the task graph
	 * @return			the TaskGraph object
	 */
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> generate(String fileName) {
		String line = "";
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> graph = null;
		if(fileIsReadable(fileName)){	
			try {
				line = fRead.readLine();
				ArrayList<TaskVertex> taskList = new ArrayList<TaskVertex>();	//temporary list
				int noOfTasks = 0;
				while(!line.equals(EXCLAMATION) && !line.equals("")){
					int index = line.indexOf(",");
					TaskVertex t = new BasicTaskVertex(line.substring(0,index),Integer.parseInt(line.substring(index+1,line.length())));
					taskList.add(t);
					noOfTasks++;
					line = fRead.readLine();
				}
				graph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(fileName.substring(0,fileName.length()-4), noOfTasks);
				
				for(int i=0; i<noOfTasks; i++){
					graph.add(taskList.get(i));		//Add all the Tasks in the TaskGraph
				}
				
				line = fRead.readLine();
				while(!line.equals(EXCLAMATION)){
					int index = line.indexOf(",");
					int lastIndex = line.lastIndexOf(",");
					TaskVertex from = getTask(taskList, line.substring(0,index));
					TaskVertex to = getTask(taskList, line.substring(index+1,lastIndex));
					CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(from.name()+"-"+to.name(), from, to, true);
					edge.setWeight(Integer.parseInt(line.substring(lastIndex+1,line.length())));
					graph.add(edge);
					line = fRead.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			System.exit(1);		//Abnormal exit
		}
		System.gc();
		return graph;
	}
	
	/**
	 * 
	 * @param tasks
	 * @param id
	 * @return	the task with the given ID
	 */
	private static TaskVertex getTask(ArrayList<TaskVertex> tasks, String id){
		TaskVertex t = null;
		for(Iterator<TaskVertex> it = tasks.iterator(); it.hasNext();){
			t = it.next();
			if(t.name().equals(id)){
				return t;
			}
		}
		return t;
	}
	
	/**
	 * Returns whether the provided file is readable
	 * 
	 * @param file	the file to be read
	 * @return		whether the file is readable
	 */
	private static boolean fileIsReadable(String file) {
		for(;;){
			try{
				fRead = new BufferedReader(new FileReader(file));
				return true;
			}
			catch(FileNotFoundException e){
				System.out.println("File "+file+" could not be opened.");
				return false;
			}
		}
	}
}
