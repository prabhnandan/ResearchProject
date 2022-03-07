package parcschedule.schedulers.clusterSchedulers.Clusterers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedulers.clusterSchedulers.Clustering.Cluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;
import parcschedule.schedulers.clusterSchedulers.Clustering.LinkedSetCluster;

public class DscClusterer<V extends TaskVertex> implements Clusterer<V>
{
	
	@Override
	public DscClustering<V> cluster(TaskGraph<V, ?> taskGraph)
	{
		DscClustering<V> clustering = new DscClustering<>(taskGraph);
		
		for (V task : taskGraph.sources())
		{
			clustering.assignToUnitCluster(task);
		}
		
		Set<LinkedSetCluster<V>> reservedClusters = new HashSet<>();
		
		while ( ! clustering.freeTasks().isEmpty())
		{
			
			V priorityFreeTask = clustering.highPriorityFreeTask();
			
//			"dominant sequence reduction warranty"
//			
//			V priorityPartiallyFreeTask = clustering.highPriorityPartiallyFreeTask();
//			long highestPriorityForFreeTasks = clustering.topLevel(priorityFreeTask) + taskGraph.getBottomLevel(priorityFreeTask);
//			long highestPriorityForPartiallyFreeTasks = priorityPartiallyFreeTask == null ? 0 : clustering.topLevel(priorityPartiallyFreeTask) + taskGraph.getBottomLevel(priorityFreeTask);
//		
//			if (highestPriorityForPartiallyFreeTasks > highestPriorityForFreeTasks)
//			{
//				LinkedSetCluster<V> enablingCluster = clustering.enablingCluster(priorityPartiallyFreeTask); 
//				if (! reservedClusters.contains(enablingCluster) && 
//						(clustering.clusterFinishTime(enablingCluster) < clustering.topLevel(priorityPartiallyFreeTask)))
//				{
//					reservedClusters.add(enablingCluster);
//				}
//			}
//			else
//			{
//				reservedClusters.remove(clustering.enablingCluster(priorityPartiallyFreeTask));
//			}
			
			V task = priorityFreeTask;
			
			if (reservedClusters.contains(clustering.enablingCluster(task)))
			{
				clustering.assignToUnitCluster(task);
				continue;
			}
			
			// prepare sorted list of predecessors 
			List<CommEdge<V>> inEdges = new ArrayList<>(taskGraph.inDegree(task));
			for (CommEdge<V> inEdge : taskGraph.inEdges(task))
			{
				inEdges.add(inEdge);
			}
			// sort in order of decreasing edge finish time
			Collections.sort(inEdges, new Comparator<CommEdge<V>>() {

				@Override
				public int compare(CommEdge<V> A, CommEdge<V> B) {
					// invert priority to get decreasing order from sort method
					V parentA = A.from();
					V parentB = B.from();
					int val = clustering.topLevel(parentB) + parentB.weight() + B.weight() 
							- clustering.topLevel(parentA) - parentA.weight() - A.weight();
					return val != 0 ? val : A.from().index() - B.from().index();
				}
			});
			
			// find out if merging onto the constraining parent cluster will reduce t-level
			V constrainingParent = inEdges.get(0).from();
			LinkedSetCluster<V> enablingCluster = clustering.cluster(constrainingParent);
			int clusterFinishTime = clustering.clusterFinishTime(enablingCluster);
			
			// remove parents already in enabling cluster
			for (int i = inEdges.size() - 1; i > 0 /*keep first task*/; i--)
			{
				if (enablingCluster.contains(inEdges.get(i).from()))
				{
					inEdges.remove(i);
				}
			}
			
			if (clusterFinishTime >= clustering.topLevel(task))
			{
				// the latest arrival edge cannot be zeroed to reduced the task's est
				// task remains in unit cluster
				clustering.assignToUnitCluster(task);
				continue;
			}
			
			// find point k where all parents up to k [have no children other than the examined task] or [is in the enabling cluster]
			int k = 0;
			while (k < inEdges.size() - 1)
			{
				V parent = inEdges.get(k + 1).from();
				if (taskGraph.outDegree(parent) > 1) // && !enablingCluster.contains(parent)) already removed above
				{
					break;
				}
				k++;
			}
			
			Map<V, Integer> parentPriorityIndex = new HashMap<>();
			List<V> orderedMergeCandidates = new ArrayList<>(k);
			for (int i = 1; i <= k; i++)
			{
				V parent = inEdges.get(i).from();
				parentPriorityIndex.put(parent, i);
				orderedMergeCandidates.add(parent);
			}
						
			Collections.sort(orderedMergeCandidates, new Comparator<V>() {

				@Override
				public int compare(V A, V B) {
					// increasing order of t-level of the parents after being removed from their original clusters
					int val = clustering.estIfRelocated(A, enablingCluster) - clustering.estIfRelocated(B, enablingCluster);
					return val != 0 ? val : A.index() - B.index();
				}
			});
			

			int mergePoint = k;

			for (int i = 1; i <= k; i++)
			{
				CommEdge<V> inEdge = inEdges.get(i);
				clusterFinishTime = clustering.clusterFinishTime(constrainingParent);
				int unmergedEdgeArrivalTime = clustering.topLevel(inEdge.from()) + inEdge.from().weight() + inEdge.weight();
				
				for (V parent : orderedMergeCandidates)
				{
					if (parentPriorityIndex.get(parent) <= i)
					{
						clusterFinishTime = Math.max(clusterFinishTime, clustering.estIfRelocated(parent, enablingCluster)) + parent.weight();
					}
				}
				
				if (clusterFinishTime == unmergedEdgeArrivalTime)
				{
					mergePoint = i;
					break;
				}
				else if (clusterFinishTime > unmergedEdgeArrivalTime)
				{
					mergePoint = i - 1;
					break;
				}
			}
			
			for (V parent : orderedMergeCandidates)
			{
				if (parentPriorityIndex.get(parent) <= mergePoint) // if merge point is 0 none of the parents will satisfy this
				{
					clustering.moveToCluster(parent, enablingCluster);
				}
			}
			
			if (mergePoint < inEdges.size() - 1)
			{
				CommEdge<V> nextConstrainingEdge = inEdges.get(mergePoint + 1);
				clustering.assignToCluster(task, enablingCluster, Math.max(clustering.clusterFinishTime(constrainingParent), 
						clustering.topLevel(nextConstrainingEdge.from()) + nextConstrainingEdge.from().weight() + nextConstrainingEdge.weight()));
			}
			else
			{
				clustering.assignToCluster(task, enablingCluster, clustering.clusterFinishTime(enablingCluster));
			}
		}
		
		return clustering;
	}
	
	int clusterLength(Clustering<V> clustering, TaskGraph<V, ? extends CommEdge<V>> taskGraph)
	{
		Map<V, Integer> topLevels = topLevels(clustering, taskGraph);
		int sl = 0;
		for (V task : taskGraph.vertices())
		{
			sl = Math.max(sl, topLevels.get(task) + task.weight());
		}
		return sl;
	}
	
	Map<V, Integer> topLevels(Clustering<V> clustering, TaskGraph<V, ? extends CommEdge<V>> taskGraph)
	{
		Map<V, V> taskBefore = new HashMap<>();
		Map<V, Integer> taskStartTimes = new HashMap<>();
		
		for (Cluster<V> cluster : clustering.clusters())
		{
			V prevTask = null;
			for (V task : cluster)
			{
				if (prevTask != null)
				{
					taskBefore.put(task, prevTask);
				}
				prevTask = task;
			}
		}
		
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
					Cluster<V> cluster = clustering.cluster(task);
					V precedingTask = cluster == null ? null : taskBefore.get(task);
					boolean precedingTaskIsAParent = false;
					
					for (V parent : taskGraph.parents(task))
					{
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
						stack.add(precedingTask);
					}
					
					expandedVertices.add(task);
					continue;
				}
			}
		}
		
		int scheduleLength = 0;

		for (int i = 0; i < orderedVertices.size(); i++)
		{
			int tLevel = 0;
			V task = orderedVertices.get(i);
			
			Cluster<V> cluster = clustering.cluster(task);
			for (CommEdge<V> inEdge : taskGraph.inEdges(task))
			{
				int edgeFinishTime = taskStartTimes.get(inEdge.from()) + inEdge.from().weight();
				
				if (clustering.cluster(inEdge.from()) != cluster || cluster == null)
				{
					edgeFinishTime += inEdge.weight();
				}
				
				if (edgeFinishTime > tLevel)
				{
					tLevel = edgeFinishTime;
				}
			}
			TaskVertex precedingTask = cluster == null ? null : taskBefore.get(task);
			if (precedingTask != null)
			{
				tLevel = Math.max(tLevel, taskStartTimes.get(precedingTask) + precedingTask.weight());
			}
			taskStartTimes.put(task, tLevel);
			scheduleLength = Math.max(scheduleLength, tLevel + task.weight());
		}
		
		return taskStartTimes;
	}

	@Override
	public String description()
	{
		return "Dominant Sequence Clustering (Yang & Gerasoulis)";
	}

	@Override
	public String code()
	{
		return "dsc";
	}
}

class DscClustering<V extends TaskVertex> implements Clustering<V>
{
	private TaskGraph<V, ?> taskGraph;
	
	private Map<V, LinkedSetCluster<V>> clusterAssignments = new HashMap<>();
	private Map<LinkedSetCluster<V>, Integer> clusterFinishTimes = new LinkedHashMap<>();
	
	private Map<V, LinkedSetCluster<V>> enablingClusters = new HashMap<>();
	private Map<V, Integer> ests = new HashMap<>();
//	private Map<V, Integer> estIfRelocated = new HashMap<>();

	private Set<V> examinedTasks = new HashSet<>();
	private Map<V, Integer> examinedParentCounts = new HashMap<>();
	
	private TreeSet<V> freeTasks;
	private TreeSet<V> partiallyFreeTasks;
	
//	private Map<V, LinkedSetCluster<V>> originallyAssignedClusters = new HashMap<>();
//	private Set<V> relocatedTasks = new HashSet<>();
	
	public DscClustering(TaskGraph<V, ?> taskGraph)
	{
		
		Comparator<V> taskPriorityComparator = new Comparator<V>() {
			// ascending order of priority
			@Override
			public int compare(V A, V B) {
				
				int val = Long.signum(ests.get(A) + taskGraph.getBottomLevel(A) - ests.get(B) - taskGraph.getBottomLevel(B));
				return val != 0 ? val : A.index() - B.index();
			}
		};
		
		freeTasks = new TreeSet<>(taskPriorityComparator);
		partiallyFreeTasks = new TreeSet<>(taskPriorityComparator);
		
		this.taskGraph = taskGraph;
		for (V task : taskGraph.vertices())
		{
			ests.put(task, 0);
			examinedParentCounts.put(task, 0);
		}
	}
	
//	protected void returnToOriginalCluster(V task)
//	{
//		if (!relocatedTasks.contains(task)) throw new RuntimeException();
//		clusterAssignments.get(task).remove(task);
//		LinkedSetCluster<V> cluster = originallyAssignedClusters.get(task);
//		if (cluster == null)
//		{
//			cluster = new LinkedSetCluster<>();
//		}
//		cluster.insertMaintainTopLevelOrder(task, topLevels);
//		clusterFinishTimes.put(cluster, topLevels.get(task) + task.weight());
//		clusterAssignments.put(task, cluster);
//	}
	
	public void moveToCluster(V task, LinkedSetCluster<V> cluster)
	{
		if ( ! clusterAssignments.containsKey(task)) throw new RuntimeException();
		
		LinkedSetCluster<V> oldCluster = clusterAssignments.get(task);
		oldCluster.remove(task);
		if (oldCluster.tasks().isEmpty())
		{
			clusterFinishTimes.remove(oldCluster);
		}
		
//		if (oldCluster.size() > 0)
//		{
//			originallyAssignedClusters.put(task, oldCluster);
//		}
//		relocatedTasks.add(task);
//		
//		for (V parent : taskGraph.parents(task))
//		{
//			if (relocatedTasks.contains(parent))
//			{
//				returnToOriginalCluster(parent);
//			}
//		}
			
		int est = attachToCluster(task, cluster, -1);
		ests.put(task, est);
	}
	
	private int attachToCluster(V task, LinkedSetCluster<V> cluster, int providedStartTime)
	{
		cluster.add(task);
		clusterAssignments.put(task, cluster);
		if (providedStartTime == -1)
		{
			int est = Math.max(clusterFinishTimes.get(cluster), estIfRelocated(task, cluster));
			clusterFinishTimes.put(cluster, est + task.weight());
			return est;
		}
		else
		{
			clusterFinishTimes.put(cluster, providedStartTime + task.weight());
			return providedStartTime;
		}
	}
	
	public LinkedSetCluster<V> cluster(V task)
	{
		return clusterAssignments.get(task);
	}
	
	public int topLevel(V task)
	{
		return ests.get(task);
	}
	
	public int estIfRelocated(V task, Cluster<V> toCluster)
	{
		int est = 0;
		for (CommEdge<V> inEdge : taskGraph.inEdges(task))
		{
			est = Math.max(est, ests.get(inEdge.from()) + inEdge.from().weight() + (cluster(inEdge.from()) == toCluster ? 0 : inEdge.weight()));
		}
		return est;
	}
	
	public void assignToUnitCluster(V task)
	{
		assignToCluster(task, null, 0);
	}
	
	public void assignToCluster(V task, LinkedSetCluster<V> cluster, int newTopLevel)
	{
		examinedTasks.add(task);
		freeTasks.remove(task);
		
		if (cluster != null)
		{
//			int est = 0;
//			for (CommEdge<V> inEdge : taskGraph.inEdges(task))
//			{
//				est = Math.max(est, ests.get(inEdge.from()) + inEdge.weight());
//			}
//			estIfRelocated.put(task, est);
			
			ests.put(task, newTopLevel);
		}
		else
		{
			newTopLevel = ests.get(task);
			cluster = new LinkedSetCluster<>();
			clusterFinishTimes.put(cluster, 0); // gets updated in attach- method
		}
		
		attachToCluster(task, cluster, newTopLevel);
		
		for (CommEdge<V> outEdge : taskGraph.outEdges(task))
		{
			V child = outEdge.to();
			// update t-levels of task's children
			int edgeFinishTime = newTopLevel + task.weight() + outEdge.weight();
			if (ests.get(child) < edgeFinishTime)
			{
				partiallyFreeTasks.remove(child);
				ests.put(child, edgeFinishTime);
				partiallyFreeTasks.add(child);
				
				enablingClusters.put(child, cluster);
			}
			
			// update free and partially free task sets
			int examinedParentCount = examinedParentCounts.get(child) + 1;
			examinedParentCounts.put(child, examinedParentCount);
			
			if (examinedParentCount == taskGraph.inDegree(child))
			{
				freeTasks.add(child);
				partiallyFreeTasks.remove(child);
			}
		}
	}
	
	public V highPriorityFreeTask()
	{
		return freeTasks.last();
	}
	
	public V highPriorityPartiallyFreeTask()
	{
		return partiallyFreeTasks.isEmpty() ? null : partiallyFreeTasks.last();
	}
	
	public int clusterFinishTime(V task)
	{
		return clusterFinishTimes.get(clusterAssignments.get(task));
	}
	
	public int clusterFinishTime(LinkedSetCluster<V> cluster)
	{
		return clusterFinishTimes.get(cluster);
	}
	
	public Set<V> freeTasks()
	{
		return freeTasks;
	}
	
	public Set<V> examinedTasks()
	{
		return examinedTasks;
	}

	public LinkedSetCluster<V> enablingCluster(V task) 
	{
		return enablingClusters.get(task);
	}

	@Override
	public Collection<LinkedSetCluster<V>> clusters() 
	{
		return clusterFinishTimes.keySet();
	}
}
