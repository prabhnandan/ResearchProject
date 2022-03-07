package parcschedule.schedulers.clusterSchedulers.Clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;;

public class SimpleClustering<V extends TaskVertex> implements Clustering<V>, ClusterChangedObserver
{
	protected TaskGraph<V, ?> taskGraph;
	
	protected Set<ArrayCluster<V>> clusterSet = new LinkedHashSet<>();
	protected Map<V, ArrayCluster<V>> clusterAssignments = new HashMap<>();
	
	protected Map<V, Integer> allocatedTopLevels = new HashMap<>();
	protected Map<V, Integer> allocatedBottomLevels = new HashMap<>();

	protected Map<V, V> constrainingParents = new HashMap<>();
	protected Map<V, V> criticalChildren = new HashMap<>();
	
	protected int scheduleLength;
	protected boolean levelsUpdated = false;
	
	public SimpleClustering(TaskGraph<V, ?> taskGraph)
	{
		this.taskGraph = taskGraph;
		updateLevels();
	}
	
	public SimpleClustering(Clustering<V> clustering, TaskGraph<V, ?> taskGraph)
	{
		this.taskGraph = taskGraph;
		
		for (Cluster<V> cluster : clustering.clusters())
		{
			ArrayCluster<V> arrayCluster = new ArrayCluster<>();
			for (V task : cluster)
			{
				arrayCluster.add(task);
				clusterAssignments.put(task, arrayCluster);
			}
			clusterSet.add(arrayCluster);
		}
		
		updateLevels();
	}
	
	public SimpleClustering(Clustering<V> clustering, TaskGraph<V, ?> taskGraph, Comparator<V> taskComparator)
	{
		this.taskGraph = taskGraph;

		for (Cluster<V> cluster : clustering.clusters())
		{
			List<V> tasks = new ArrayList<>();
			for (V task : cluster)
			{
				tasks.add(task);
			}
			Collections.sort(tasks, taskComparator);
			
			ArrayCluster<V> arrayCluster = new ArrayCluster<>();
			for (V task : tasks)
			{
				arrayCluster.add(task);
				clusterAssignments.put(task, arrayCluster);
			}
			clusterSet.add(arrayCluster);
		}
		
		updateLevels();
	}
	
	public void assignCluster(V task, ArrayCluster<V> cluster)
	{
		clusterAssignments.put(task, cluster);
		clusterSet.add(cluster);
		cluster.setObserver(this);
		levelsUpdated = false;
	}
	
	public ArrayCluster<V> cluster(V task)
	{
		return clusterAssignments.get(task);
	}
	
	public void removeCluster(ArrayCluster<V> cluster)
	{
		clusterSet.remove(cluster);
		cluster.removeObserver();
	}
	
	public void unassignCluster(V task)
	{
		ArrayCluster<V> cluster = clusterAssignments.get(task);
		clusterAssignments.put(task, null);
		if (cluster.size() == 0) clusterSet.remove(cluster);
		levelsUpdated = false;
	}
	
	public void updateLevels()
	{
		List<V> orderedVertices = topologicalOrder();
		
		// update b-levels and schedule length
		int maxBLevel = 0;

		allocatedTopLevels.clear();
		allocatedBottomLevels.clear();
		
		for (int i = orderedVertices.size() - 1; i >= 0; i--)
		{
			int bLevel = 0;
			V task = orderedVertices.get(i);
			ArrayCluster<V> cluster = cluster(task);
						
			for (CommEdge<V> outEdge : taskGraph.outEdges(task))
			{
				int remainingCost = allocatedBottomLevels.get(outEdge.to());
				if (cluster == null || cluster(outEdge.to()) != cluster)
				{
					remainingCost += outEdge.weight();
				}
				
				if (remainingCost > bLevel)
				{
					bLevel = remainingCost;
					criticalChildren.put(task, outEdge.to());
				}
			}
			
			V succedingTask = cluster == null ? null : cluster.succedingTask(task);
			if (succedingTask != null)
			{
				bLevel = Math.max(bLevel, allocatedBottomLevels.get(succedingTask));
			}
			
			bLevel += task.weight();
			
			allocatedBottomLevels.put(task, bLevel);
			maxBLevel = Math.max(maxBLevel, bLevel);
		}
		
		scheduleLength = maxBLevel;
		
		// update t-levels
		for (int i = 0; i < orderedVertices.size(); i++)
		{
			int tLevel = 0;
			V task = orderedVertices.get(i);
			ArrayCluster<V> cluster = cluster(task);
			
			for (CommEdge<V> inEdge : taskGraph.inEdges(task))
			{
				int edgeFinishTime = allocatedTopLevels.get(inEdge.from()) + inEdge.from().weight();
				
				if (cluster == null || cluster(inEdge.from()) != cluster)
				{
					edgeFinishTime += inEdge.weight();
				}
				
				if (edgeFinishTime > tLevel)
				{
					tLevel = edgeFinishTime;
					constrainingParents.put(task, inEdge.from());
				}
			}
			
			V precedingTask = cluster == null ? null : cluster.precedingTask(task);
			if (precedingTask != null)
			{
				tLevel = Math.max(tLevel, allocatedTopLevels.get(precedingTask) + precedingTask.weight());
			}
			
			allocatedTopLevels.put(task, tLevel);
		}
		
		levelsUpdated = true;
	}
	
	public V constrainingParent(V task)
	{
		if (!levelsUpdated) updateLevels();
		return constrainingParents.get(task);
	}
	
	public V criticalChild(V task)
	{
		if (!levelsUpdated) updateLevels();
		return criticalChildren.get(task);
	}
	
	public int allocatedTopLevel(V task)
	{
		if (!levelsUpdated) updateLevels();
		return allocatedTopLevels.get(task);
	}
	
	public int allocatedBottomLevel(V task)
	{
		if (!levelsUpdated) updateLevels();
		return allocatedBottomLevels.get(task);
	}
	
	public int allocatedTopLevelInCluster(V task, Cluster<V> cluster)
	{
		if (!levelsUpdated) updateLevels();
		if (cluster == cluster(constrainingParents.get(task)))
		{
			int tLevel = 0;
			for (CommEdge<V> inEdge : taskGraph.inEdges(task))
			{
				V parent = inEdge.from();
				tLevel = Math.max(tLevel, allocatedTopLevels.get(parent) + parent.weight() + (cluster(parent) == cluster ? 0 : inEdge.weight()));
			}
			return tLevel;
		}
		else
		{
			return allocatedTopLevels.get(task);
		}
	}
	
	public int allocatedBottomLevelInCluster(V task, Cluster<V> cluster)
	{
		if (!levelsUpdated) updateLevels();
		if (cluster == cluster(criticalChildren.get(task)))
		{
			int bLevel = 0;
			for (CommEdge<V> outEdge : taskGraph.outEdges(task))
			{
				V child = outEdge.to();
				bLevel = Math.max(bLevel, allocatedBottomLevels.get(child) + (cluster(child) == cluster ? 0 : outEdge.weight()));
			}
			bLevel += task.weight();
			return bLevel;
		}
		else
		{
			return allocatedBottomLevels.get(task);
		}
	}
	
	public int scheduleLength()
	{
		if (!levelsUpdated) updateLevels();
		return scheduleLength;
	}

	// builds topological order including virtual edges 
	private List<V> topologicalOrder()
	{
		Set<V> expandedVertices = new HashSet<>();
		Set<V> finishedVertices = new HashSet<>();
		List<V> orderedVertices = new ArrayList<>(taskGraph.sizeVertices());
		
		Stack<V> stack = new Stack<>();
		for (V vertex : taskGraph.vertices())
		{
			if (!finishedVertices.contains(vertex))
			{
				stack.add(vertex);
			}
			
			while (!stack.empty())
			{
				V task = stack.peek();
								
				if (expandedVertices.contains(task))
				{
					if (! finishedVertices.contains(task))
					{
						
						orderedVertices.add(task);
						finishedVertices.add(task);
					}
					stack.pop();
				}
				else
				{
					ArrayCluster<V> cluster = cluster(task);
					V precedingTask = cluster == null ? null : cluster.precedingTask(task);
					boolean precedingTaskIsAParent = false;
					
					for (V parent : taskGraph.parents(task))
					{
						if (expandedVertices.contains(parent) && !finishedVertices.contains(parent)) throw new RuntimeException();
						if (! finishedVertices.contains(parent))
						{
							stack.add(parent);
						}
						if (parent == precedingTask)
						{
							precedingTaskIsAParent = true;
						}
					}
					
					if ( !precedingTaskIsAParent && precedingTask != null && !finishedVertices.contains(precedingTask))
					{
						if (expandedVertices.contains(precedingTask) && !finishedVertices.contains(precedingTask)) throw new RuntimeException();
						stack.add(precedingTask);
					}
					
					expandedVertices.add(task);
				}
			}
		}
		
		return orderedVertices;
	}
	
	public Set<V> descendants(V task)
	{
		Set<V> descendants = new HashSet<>();
		Stack<V> stack = new Stack<>();
		ArrayCluster<V> cluster;
		
		for (V child : taskGraph.children(task))
		{
			stack.push(child);
		}
		
		while (!stack.isEmpty())
		{
			V top = stack.pop();
			if (!descendants.contains(top))
			{
				descendants.add(top);
				for (V child : taskGraph.children(top))
				{
					stack.add(child);
				}
				cluster = cluster(top);
				V taskAfter = cluster == null ? null : cluster.succedingTask(top);
				if (taskAfter != null) stack.push(taskAfter);
			}
		}
		
		return descendants;
	}

	@Override
	public Collection<ArrayCluster<V>> clusters() 
	{
		return clusterSet;
	}

	@Override
	public void notifyClusterChanged()
	{
		levelsUpdated = false;
	}
}
