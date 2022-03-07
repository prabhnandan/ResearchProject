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

public class TggReader 
{
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> read(File shape) throws FileNotFoundException, IOException
	{
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph;
		Map<Integer, TaskVertex> taskIdInstanceMap = new HashMap<>();
		
		BufferedReader br = new BufferedReader(new FileReader(shape));
		taskGraph = new BasicTaskGraph<>("", Integer.parseInt(br.readLine()));
	    br.readLine();
	    String line;
	    int edgeId = 0;
	    while ((line = br.readLine()) != null) 
	    {
	    	String[] words = line.split("	");
	    	int tailId = Integer.parseInt(words[0].replaceAll("[^\\d.]", ""));
	    	int headId = Integer.parseInt(words[1].replaceAll("[^\\d.]", ""));
	    	if (!taskIdInstanceMap.containsKey(tailId))
	    	{
	    		TaskVertex task = new BasicTaskVertex(Integer.toString(tailId), tailId);
	    		taskIdInstanceMap.put(tailId, task);
	    		taskGraph.add(task);
	    	}
	    	if (!taskIdInstanceMap.containsKey(headId))
	    	{
	    		TaskVertex task = new BasicTaskVertex(Integer.toString(headId), headId);
	    		taskIdInstanceMap.put(headId, task);
	    		taskGraph.add(task);
	    	}
	    	CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(Integer.toString(edgeId++), taskIdInstanceMap.get(tailId), taskIdInstanceMap.get(headId), true);
	    	edge.setWeight(Integer.parseInt(words[2].replaceAll("[^\\d.]", "")));
	    	taskGraph.add(edge);
	    }
	    
//		br = new BufferedReader(new FileReader(cost));
//		int taskNo = 1;
//	    while (taskNo <= taskGraph.sizeVertices()) 
//	    {
//	    	taskIdInstanceMap.get(taskNo++).setWeight(Integer.parseInt((br.readLine()).replace("\t", "")));
//	    }
	    
		return addWeightsToGaussGraph(taskGraph, 10);
	}
	
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> addWeightsToGaussGraph(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph, int unitWeight)
	{
		double largeWeightFactor = 2;
		boolean ambiguousLargeNodeFound = false;
		for (TaskVertex task : taskGraph.vertices())
		{
			if (taskGraph.outDegree(task) == 0)
			{
				task.setWeight(unitWeight);
			}
			else if (taskGraph.outDegree(task) == 1)
			{
				if (!ambiguousLargeNodeFound && taskGraph.inDegree(task) == 1 && taskGraph.outDegree(taskGraph.parentsIterator(task).next()) == 1)
				{
					ambiguousLargeNodeFound = true;
					task.setWeight((int) Math.round(unitWeight * largeWeightFactor));
				}
				else task.setWeight(unitWeight * taskGraph.outDegree(taskGraph.parentsIterator(task).next()));
			}
			else task.setWeight((int) Math.round(unitWeight * largeWeightFactor * taskGraph.outDegree(task)));
		}
		return taskGraph;
	}
}
