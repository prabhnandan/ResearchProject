package parcschedule.schedulers.clusterSchedulers.Clusterers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedulers.clusterSchedulers.Clustering.Cluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;
import parcschedule.schedulers.clusterSchedulers.Clustering.LinkedSetCluster;

public class LcClusterer<V extends TaskVertex> implements Clusterer<V>
{
	
	@Override
	public Clustering<V> cluster(TaskGraph<V, ?> taskGraph)
	{
		return new LcClustering<>(taskGraph);
	}

	@Override
	public String description()
	{
		return "Linear Clustering (Kim)";
	}

	@Override
	public String code()
	{
		return "lc";
	}
}

class LcClustering<V extends TaskVertex> implements Clustering<V>
{
	TaskGraph<V, ?> taskGraph;
	Set<Cluster<V>> clusters = new HashSet<>();
	Set<V> examinedTasks = new HashSet<>();
	Map<V, Integer> bottomLevels = new HashMap<>();
	Map<V, Cluster<V>> clusterAssignments = new HashMap<>();

	public LcClustering(TaskGraph<V, ?> taskGraph) 
	{
		this.taskGraph = taskGraph;
		
		while (examinedTasks.size() < taskGraph.sizeVertices())
		{
			int scheduleLength = 0;
			V criticalPathHead = null;
			for (V task : taskGraph.inverseTopologicalOrder())
			{
				if (examinedTasks.contains(task))
				{
					continue;
				}
				
				int bLevel = 0;
				for (CommEdge<V> outEdge : taskGraph.outEdges(task))
				{
					if (examinedTasks.contains(outEdge.to()))
					{
						continue;
					}
					bLevel = Math.max(bLevel, outEdge.weight() + bottomLevels.get(outEdge.to()));
				}
				
				bLevel += task.weight();
				
				if (bLevel > scheduleLength)
				{
					scheduleLength = bLevel;
					criticalPathHead = task;
				}
				bottomLevels.put(task, bLevel);
			}
			
			LinkedSetCluster<V> cluster = new LinkedSetCluster<>();
			
			while (criticalPathHead != null)
			{
				cluster.add(criticalPathHead);
				clusterAssignments.put(criticalPathHead, cluster);
				examinedTasks.add(criticalPathHead);
				V nextCriticalPathHead = null;
				
				int maxCost = 0;
				for (CommEdge<V> outEdge : taskGraph.outEdges(criticalPathHead))
				{
					if (examinedTasks.contains(outEdge.to()))
					{
						continue;
					}
					
					int cost = outEdge.weight() + bottomLevels.get(outEdge.to());
					if (cost < maxCost)
					{
						maxCost = cost;
						nextCriticalPathHead = outEdge.to();
					}
				}
				criticalPathHead = nextCriticalPathHead;
			}
			
			clusters.add(cluster);
		}
	}
	
	@Override
	public Collection<Cluster<V>> clusters() 
	{
		return clusters;
	}

	@Override
	public Cluster<V> cluster(V task)
	{
		return clusterAssignments.get(task);
	}
}