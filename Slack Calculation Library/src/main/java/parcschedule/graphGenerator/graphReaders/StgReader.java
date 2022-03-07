package parcschedule.graphGenerator.graphReaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import parcschedule.schedule.BasicCommEdge;
import parcschedule.schedule.BasicTaskGraph;
import parcschedule.schedule.BasicTaskVertex;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

public class StgReader
{
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> read(File file) throws FileNotFoundException, IOException
	{
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph;
		Map<Integer, TaskVertex> taskIdInstanceMap = new HashMap<>();
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		int nodeCount = Integer.parseInt(br.readLine().replace(" ", ""));
		taskGraph = new BasicTaskGraph<>("", nodeCount);
		br.readLine(); // get rid of dummy node
	    String line;
	    int edgeId = 0;
	    while (true) 
	    {
	    	line = br.readLine();
	    	String[] words = line.replaceAll("\\s+", " ").substring(1).split(" ");
	    	int taskId = Integer.parseInt(words[0]) - 1;
	    	if (!taskIdInstanceMap.containsKey(taskId))
	    	{
	    		TaskVertex task = new BasicTaskVertex(Integer.toString(taskId), taskId);
	    		taskGraph.add(task);
	    		taskIdInstanceMap.put(taskId, task);
	    	}
	    	TaskVertex task = taskIdInstanceMap.get(taskId);
	    	task.setWeight(Integer.parseInt(words[1]));
	    	for (int i = 3; i < words.length; i++)
	    	{
	    		int parentId = Integer.parseInt(words[i]) - 1;
	    		if (parentId != -1)
	    		{
	    			taskGraph.add(new BasicCommEdge<TaskVertex>(Integer.toString(edgeId++), taskIdInstanceMap.get(parentId), task, true));
	    		}
	    	}
	    	if (taskId == nodeCount - 1) break;
	    }
		br.close();
		return taskGraph;
	}
}
