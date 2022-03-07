package parcschedule.schedulers.clusterSchedulers.Clusterers;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedulers.clusterSchedulers.Clustering.ArrayCluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.Cluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;
import parcschedule.schedulers.clusterSchedulers.Clustering.SimpleClustering;

public class DcpClusterer<V extends TaskVertex> implements Clusterer<V>
{
	TaskGraph<V, ?> taskGraph;
	DcpClustering<V> clustering;
	boolean modify = false;
	
	public DcpClusterer(boolean modify)
	{
		this.modify = modify; // small changes that could benefit results
	}
	public DcpClusterer()
	{
		this(false);
	}

	@Override
	public Clustering<V> cluster(TaskGraph<V, ?> taskGraph)
	{
		this.taskGraph = taskGraph;
		clustering = new DcpClustering<>(taskGraph);
		
		while ( ! clustering.finished())
		{
			findCluster(clustering.priorityTask());
		}
		return clustering;
	}
	
	class CompositeEst
	{
		final int est;
		final boolean hasSlotForChild;
		final Slot slot;
		final Integer scheduleLength;
		
		public CompositeEst(int est, boolean slotForChild, Slot slot, Integer scheduleLength)
		{
			this.est = est;
			this.hasSlotForChild = slotForChild;
			this.slot = slot;
			this.scheduleLength = scheduleLength;
		}
		
		public CompositeEst(int est, boolean slotForChild, Slot slot)
		{
			this(est, slotForChild, slot, null);
		}
	}
	
	private CompositeEst compositeEst(V task, V criticalChild, ArrayCluster<V> cluster, boolean push)
	{
		Slot slot = findSlot(task, cluster, push);
		if (slot == null)
		{
			return null;
		}
		else
		{
			if (criticalChild == null)
			{
				return new CompositeEst(slot.startTime, false, slot);
			}
			
			CompositeEst compositeEst;
			
			cluster.add(task, slot.index);
			clustering.assignCluster(task, cluster);
			
			clustering.updateLevels();
			
			if (clustering.examined(criticalChild))
			{
				compositeEst = new CompositeEst(slot.startTime + clustering.est(criticalChild), true, slot, clustering.scheduleLength());
			}
			else
			{
				Slot childSlot = findSlot(criticalChild, cluster, false);
				if (childSlot != null)
				{
					compositeEst = new CompositeEst(slot.startTime + childSlot.startTime, true, slot);
				}
				else
				{
					compositeEst = new CompositeEst(slot.startTime, false, slot);
				}
			}
			cluster.remove(slot.index);
			clustering.unassignCluster(task);
			clustering.updateLevels();
			return compositeEst;
		}
	}

	private void findCluster(V task)
	{
		Set<ArrayCluster<V>> clusterList;
		V criticalChild;
		boolean push;
				
		if (clustering.est(task) == clustering.lst(task))
		{
			clusterList = new LinkedHashSet<>();
			push = true;
			
			if (modify)
			{
				criticalChild = clustering.criticalChild(task);
				clusterList.add(clustering.cluster(clustering.constrainingParent(task)));
				
				if (clustering.examined(criticalChild))
				{
					clusterList.add(clustering.cluster(criticalChild));
				}
				else
				{
					V nextConstrainingParent = null;
					int latestArrivingEdge = 0;
					for (CommEdge<V> inEdge : taskGraph.inEdges(criticalChild))
					{
						if (inEdge.from() != task)
						{
							int edgeTime = clustering.est(inEdge.from()) + inEdge.from().weight() + inEdge.weight();
							if (edgeTime > latestArrivingEdge)
							{
								latestArrivingEdge = edgeTime;
								nextConstrainingParent = inEdge.from();
							}
						}
					}
					clusterList.add(clustering.cluster(nextConstrainingParent));
				}
			}
			else
			{
				criticalChild = unexaminedCriticalChild(task);
				
				for (V parent : taskGraph.parents(task))
				{
					clusterList.add(clustering.cluster(parent));
				}
				
				for (V child : taskGraph.children(task))
				{
					clusterList.add(clustering.cluster(child));
				}
			}

			clusterList.add(new ArrayCluster<>());
		}
		else
		{
			criticalChild = unexaminedCriticalChild(task);
			clusterList = new HashSet<>();
			for (V parent : taskGraph.parents(task))
			{
				clusterList.add(clustering.cluster(parent));
			}
			push = false;
		}
		
		CompositeEst bestCompositeEst = null;
		for (ArrayCluster<V> cluster : clusterList)
		{
			CompositeEst compositeEst = compositeEst(task, criticalChild, cluster, push);
			
			if (compositeEst == null)
			{
				continue;
			}
			if (bestCompositeEst == null)
			{
				bestCompositeEst = compositeEst;
				continue;
			}
			
			if (compositeEst.scheduleLength != null)
			{
				if (compositeEst.scheduleLength < bestCompositeEst.scheduleLength)
				{
					bestCompositeEst = compositeEst;
				}
				else if (compositeEst.scheduleLength == bestCompositeEst.scheduleLength)
				{
					if (compositeEst.est < bestCompositeEst.est)
					{
						bestCompositeEst = compositeEst;
					}
				}
			}
			else
			{
				if (compositeEst.hasSlotForChild ^ bestCompositeEst.hasSlotForChild)
				{
					if (compositeEst.hasSlotForChild)
					{
						bestCompositeEst = compositeEst;
					}
				}
				else
				{
					if (compositeEst.est < bestCompositeEst.est)
					{
						bestCompositeEst = compositeEst;
					}
				}
			}
		}
		
		
		if (bestCompositeEst != null)
		{
			Slot bestSlot = bestCompositeEst.slot;
			bestSlot.cluster.add(task, bestSlot.index);
			clustering.assignCluster(task, bestSlot.cluster);
		}
		else
		{
			ArrayCluster<V> newCluster = new ArrayCluster<>();
			newCluster.add(task);
			clustering.assignCluster(task, newCluster);
		}
		
		
		clustering.updateLevels();
		clustering.markExamined(task);
	}
	
	private V unexaminedCriticalChild(V task)
	{
		V criticalChild = null;
		int minMobility = Integer.MAX_VALUE;
		for (CommEdge<V> outEdge : taskGraph.outEdges(task))
		{
			if ( !clustering.examined(outEdge.to()))
			{
				int mobility = clustering.lst(outEdge.to()) - clustering.est(outEdge.to());
				if (mobility < minMobility)
				{
					minMobility = mobility;
					criticalChild = outEdge.to();
				}
			}
		}
		return criticalChild;
	}
	
	private Slot findSlot(V task, ArrayCluster<V> cluster, boolean squeeze)
	{
		
		if (cluster == null) return null;
		int drtInCluster = clustering.estInCluster(task, cluster);
		int lstInCluster = clustering.lstInCluster(task, cluster);
		
		if (cluster.size() == 0) // no previous tasks on proc
		{
			return new Slot(cluster, drtInCluster, 0);
		}
		
		int searchBeginInsertion = cluster.size();
		int searchBeginSqueeze = cluster.size();
		// find places to begin examining tasks for insertion or squeeze
		int taskEst = drtInCluster;
		for (int i = 0; i < cluster.size(); i++)
		{
			V taskInCluster = cluster.get(i);
			if (clustering.est(taskInCluster) > taskEst)
			{
				searchBeginInsertion = i;
				searchBeginSqueeze = i;
				break;
			}
			if (clustering.est(taskInCluster) + taskInCluster.weight() > taskEst)
			{
				searchBeginInsertion = i + 1;
				searchBeginSqueeze = i;
				break;
			}
		}
		
		if (searchBeginSqueeze == cluster.size()) // the case where all tasks finish before the top level of the examined task
		{
			return new Slot(cluster, taskEst, cluster.size());
		}
		// quick lookup of descendants
		Set<V> descendants = clustering.descendants(task);
		boolean descendantPassed = false;
		
		if (!squeeze)
		{
			if (searchBeginInsertion == 0)
			{
				if (clustering.est(cluster.get(0)) - taskEst >= task.weight())
				{
					return new Slot(cluster, taskEst, 0);
				}
				searchBeginInsertion++;
			}
			for (int i = searchBeginInsertion; i < cluster.size(); i++)
			{
				V taskBefore = cluster.get(i - 1);
				if (descendants.contains(taskBefore))
				{
					descendantPassed = true;
					break;
				}
				
				V taskAfter = cluster.get(i);
				
				if (i == searchBeginInsertion)
				{
					taskEst = Math.max(drtInCluster, clustering.est(taskBefore) + taskBefore.weight());
				}
				else 
				{
					taskEst = clustering.est(taskBefore) + taskBefore.weight();
				}
				
				if (taskEst > lstInCluster)
				{
					break;
				}
				
				if (clustering.est(taskAfter) - taskEst >= task.weight())
				{
					return new Slot(cluster, taskEst, i);
				}
			}
			if (! (descendantPassed || descendants.contains(cluster.last())))
			{
				taskEst = clustering.est(cluster.last()) + cluster.last().weight();
				if (taskEst <= lstInCluster)
				{
					return new Slot(cluster, Math.max(taskEst, drtInCluster), cluster.size());
				}
			}
		}
		// squeeze
		else
		{
			for (int i = searchBeginSqueeze; i < cluster.size(); i++)
			{
				V taskAfter = cluster.get(i);
				
				if (i == searchBeginSqueeze)
				{
					taskEst = drtInCluster;
				}
				else
				{
					V taskBefore = cluster.get(i - 1);
					if (descendants.contains(taskBefore))
					{
						break;
					}
					
					taskEst = clustering.est(taskBefore) + taskBefore.weight();
				}
				
				if (taskEst > lstInCluster)
				{
					break;
				}
				
				if (taskEst + task.weight() <= clustering.lst(taskAfter))
				{
					return new Slot(cluster, taskEst, i);
				}
			}
		}
		
		return null;
	}
	
	class Slot
	{
		final ArrayCluster<V> cluster;
		final int startTime;
		final int index;
		
		public Slot(ArrayCluster<V> cluster, int startTime, int index)
		{
			this.cluster = cluster;
			this.startTime = startTime;
			this.index = index;
		}
	}

	@Override
	public String description()
	{
		return "Dynamic Critical Path (Ahmad & Kwok)";
	}
	@Override
	public String code()
	{
		return "dcp";
	}
}

class DcpClustering<V extends TaskVertex> extends SimpleClustering<V>
{
	Set<V> unExaminedTasks = new LinkedHashSet<>();
	
	Comparator<V> taskPriorityComparator = new Comparator<V>() {

		@Override
		public int compare(V A, V B) {
			
			int val = allocatedTopLevels.get(A) + allocatedBottomLevels.get(A) - allocatedTopLevels.get(B) - allocatedBottomLevels.get(B);
			int val2 = val == 0 ? allocatedTopLevels.get(B) - allocatedTopLevels.get(A) : val;
			return val2 != 0 ? val2 : A.index() - B.index();
		}
	};
	
	public DcpClustering(TaskGraph<V, ?> taskGraph)
	{
		super(taskGraph);
		for (V task : taskGraph.vertices())
		{
			unExaminedTasks.add(task);
		}
	}
	
	public V priorityTask()
	{
		return Collections.max(unExaminedTasks, taskPriorityComparator);
	}
	
	public boolean finished()
	{
		return unExaminedTasks.isEmpty();
	}
	
	public boolean examined(V task)
	{
		return ! unExaminedTasks.contains(task);
	}
	
	public void markExamined(V task)
	{
		unExaminedTasks.remove(task);
	}
	
	public int est(V task)
	{
		return allocatedTopLevels.get(task);
	}
	
	public int estInCluster(V task, Cluster<V> cluster)
	{
		return allocatedTopLevelInCluster(task, cluster);
	}
	
	public int lst(V task)
	{
		return scheduleLength - allocatedBottomLevels.get(task);
	}

	public int lstInCluster(V task, Cluster<V> cluster)
	{
		return scheduleLength - allocatedBottomLevelInCluster(task, cluster);
	}
	
	public Set<ArrayCluster<V>> clusterSet()
	{
		return clusterSet;
	}
}