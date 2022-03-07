package parcschedule.schedulers.clusterSchedulers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;
import parcschedule.schedulers.Scheduler;
import parcschedule.schedulers.clusterSchedulers.ClusterMergers.ClusterMerger;
import parcschedule.schedulers.clusterSchedulers.ClusterMergers.ClusterMergerScheduler;
import parcschedule.schedulers.clusterSchedulers.Clusterers.Clusterer;
import parcschedule.schedulers.clusterSchedulers.Clustering.Cluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.ClusteredLevelCalculator;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;

public class ClusterScheduler<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> implements Scheduler<V, E, P>
{
	Clusterer<V> clusterer;
	ClusterMerger<V> clusterMerger;
	ClusterMergerScheduler<V, E, P> clusterMergerScheduler;
	OrderingScheme orderingScheme;
	
	@SuppressWarnings("unchecked")
	public ClusterScheduler(Clusterer<V> clusterer, ClusterMerger<V> clusterMerger, OrderingScheme orderingScheme)
	{
		if (clusterMerger instanceof ClusterMergerScheduler<?, ?, ?>)
		{
			this.clusterer = clusterer;
			this.clusterMergerScheduler = (ClusterMergerScheduler<V, E, P>) clusterMerger;
		}
		else
		{
			this.clusterer = clusterer;
			this.clusterMerger = clusterMerger;
			this.orderingScheme = orderingScheme;
		}
	}
	
	public ClusterScheduler(Clusterer<V> clusterer, ClusterMergerScheduler<V, E, P> clusterMergerScheduler)
	{
		this.clusterer = clusterer;
		this.clusterMergerScheduler = clusterMergerScheduler;
	}
	
	@Override
	public BasicSchedule<V, E, P> schedule(TaskGraph<V, E> taskGraph, TargetSystem<P> system)
	{
		if (clusterMergerScheduler == null)
		{
			return orderClusters(clusterMerger.merge(clusterer.cluster(taskGraph), taskGraph, system.processors().size()), taskGraph, system);
		}
		else
		{
			return clusterMergerScheduler.mergeAndSchedule(clusterer.cluster(taskGraph), taskGraph, system);
		}
	}
	
	private BasicSchedule<V, E, P> orderClusters(Clustering<V> clustering, TaskGraph<V, E> taskGraph, TargetSystem<P> system)
	{
		ClusterSchedule<V, E, P> schedule = new ClusterSchedule<>(clustering, taskGraph, system);
		ClusteredLevelCalculator<V> allocatedLevels = new ClusteredLevelCalculator<>(clustering, taskGraph); // allocated levels without virtual edges
		Comparator<TaskVertex> taskComparator;
		
		if (orderingScheme == OrderingScheme.blevel)
		{
			taskComparator = new Comparator<TaskVertex>() 
			{
				@Override
				public int compare(TaskVertex A, TaskVertex B) 
				{
					int val = allocatedLevels.bottom(A) - allocatedLevels.bottom(B);
					return val != 0 ? val : A.index() - B.index();
				}
			};
		}
		else if (orderingScheme == OrderingScheme.est) 
		{
			taskComparator = new Comparator<TaskVertex>() 
			{
				@Override
				public int compare(TaskVertex A, TaskVertex B)
				{
					int estDiff = schedule.est(B) - schedule.est(A);
					int val = estDiff != 0 ? estDiff : (allocatedLevels.bottom(A) - allocatedLevels.bottom(B));
					return val != 0 ? val : A.index() - B.index();
				}
			};
		}
//		else if (orderingScheme == OrderingScheme.outBLevel)
//		{
//			final OuterBLevel<V> outerBLevel = new OuterBLevel<>(clustering, taskGraph);
//			taskComparator = new Comparator<TaskVertex>() 
//			{
//				@Override
//				public int compare(TaskVertex A, TaskVertex B)
//				{
//					int val = outerBLevel.get(A) - outerBLevel.get(B);
//					return val != 0 ? val : A.index() - B.index();
//				}
//			};
//		}
		else throw new RuntimeException();
		
		while (!schedule.isFinished())
		{
			schedule.commit(Collections.max(schedule.freeTasks(), taskComparator));
		}
		return schedule;
	}
	
	public enum OrderingScheme
	{
		blevel,
		est,
		//outBLevel
	}

	@Override
	public String description() {
		// TODO Auto-generated method stub
		return null;
	}
}

class ClusterSchedule<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> extends BasicSchedule<V, E, P>
{
	Clustering<V> clustering;

	public ClusterSchedule(Clustering<V> clustering, TaskGraph<V, E> taskGraph, TargetSystem<P> system)
	{
		super(taskGraph, system);
		this.clustering = clustering;

		if (system.processors().size() < clustering.clusters().size()) throw new RuntimeException();
		
		List<P> procs = new ArrayList<>(system.processors());
		int i = 0;
		for (Cluster<V> cluster : clustering.clusters())
		{
			P proc = procs.get(i++);
			for (V task : cluster)
			{
				taskProcAllocations.put(task, proc);
			}
		}
		for (V task : taskGraph.vertices())
		{
			if (clustering.cluster(task) == null)
			{
				System.out.println(task.name() + " cluster unassigned");
			}
		}
		for (TaskVertex task : taskGraph.vertices())
		{
			if (!taskProcAllocations.containsKey(task))
			{
				System.out.println(task.name() + " proc unassigned");
			}
		}
		return;
	}

	@Override
	protected void updateChildren(V task)
	{
	    Collection<V> newFreeTasks = new ArrayList<>(taskGraph.outDegree(task));
		for (CommEdge<V> outEdge : taskGraph.outEdges(task))
		{
			V child = outEdge.to();
			// update children's DRTs
		    int edgeFinishTime = taskStartTimes.get(task) + task.weight() + (clustering.cluster(task).contains(outEdge.to()) ? 0 : outEdge.weight());
		    if (DRTs.get(child) < edgeFinishTime)
		    {
		    	DRTs.put(child, edgeFinishTime);
		    }
			
			// update 'free' status of children
			int scheduledParentCount = scheduledParentCounts.get(child) + 1;
			scheduledParentCounts.put(child, scheduledParentCount);
			if (scheduledParentCount == taskGraph.inDegree(child))
			{
				freeTasks.add(child);
				newFreeTasks.add(child);
			}
		}
		if (freeTasksObserver != null) freeTasksObserver.handleFreeTasksAdded(newFreeTasks);
	}
	
	@Override
	public int drt(TaskVertex task)
	{
		return DRTs.get(task);
	}
	
	public int est(TaskVertex task)
	{
		return Math.max(DRTs.get(task), procFinishTimes.get(taskProcAllocations.get(task)));
	}
	
	public void commit(V task)
	{
		P proc = taskProcAllocations.get(task);
		put(task, proc, Math.max(DRTs.get(task), procFinishTimes.get(proc)), false);
	}
}

class OuterBLevel<V extends TaskVertex>
{
	Clustering<V> clustering;
	TaskGraph<V, ?> taskGraph;
	Map<TaskVertex, Map<Cluster<V>, ClusterLoad>> taskClusterLoads = new HashMap<>();
	Map<TaskVertex, Integer> bLevels = new HashMap<>();
	Map<TaskVertex, Integer> outerBLevels = new HashMap<>();
	
	public OuterBLevel(Clustering<V> clustering, TaskGraph<V, ?> taskGraph)
	{
		this.clustering = clustering;
		this.taskGraph = taskGraph;
		update();
	}
	
	public void update()
	{
		for (V task : taskGraph.inverseTopologicalOrder())
		{
			Map<Cluster<V>, ClusterLoad> clusterLoads = new HashMap<>();
			if (taskGraph.outDegree(task) == 0)
			{
				for (Cluster<V> cluster : clustering.clusters())
				{
					clusterLoads.put(cluster, new ClusterLoad());
				}
				bLevels.put(task, task.weight());
				clusterLoads.get(clustering.cluster(task)).computation += task.weight();
			}
			else
			{
				Cluster<V> taskCluster = clustering.cluster(task);
				CommEdge<V> criticalOutEdge = null;
				int bLevel = 0;
				for (CommEdge<V> outEdge : taskGraph.outEdges(task))
				{
					V child = outEdge.to();
					int level = bLevels.get(child) + (clustering.cluster(child) == taskCluster ? 0 : outEdge.weight());
					if (level > bLevel)
					{
						bLevel = level;
						criticalOutEdge = outEdge;
					}
				}
				bLevel += task.weight();
				bLevels.put(task, bLevel);
				
				Map<Cluster<V>, ClusterLoad> childClusterLoads = taskClusterLoads.get(criticalOutEdge.to());
				for (Cluster<V> cluster : clustering.clusters())
				{
					clusterLoads.put(cluster, new ClusterLoad(childClusterLoads.get(cluster)));
				}
				
				clusterLoads.get(taskCluster).computation += task.weight();
				Cluster<V> criticalChildProc = clustering.cluster(criticalOutEdge.to());
				if (criticalChildProc != taskCluster)
				{
					clusterLoads.get(criticalChildProc).inCommunication += criticalOutEdge.weight();
				}
			}
			
			taskClusterLoads.put(task, clusterLoads);
			
			int outerBLevel = 0;
			Cluster<V> taskCluster = clustering.cluster(task);
			
			for (Cluster<V> cluster : clustering.clusters())
			{
				ClusterLoad load = clusterLoads.get(cluster);
				outerBLevel += load.inCommunication;
				
				if (cluster != taskCluster)
				{
					outerBLevel += load.computation;
				}
			}
			outerBLevels.put(task, outerBLevel);
			
			if (outerBLevel > bLevels.get(task)) throw new RuntimeException();
		}
	}
	
	public int get(TaskVertex task)
	{
		return outerBLevels.get(task);
	}
	
	class ClusterLoad
	{
		int computation;
		int inCommunication;
		
		public ClusterLoad()
		{
			this(0, 0);
		}
		
		public ClusterLoad(ClusterLoad load)
		{
			this(load.computation, load.inCommunication);
		}
		
		public ClusterLoad(int computation, int inCommunication)
		{
			this.computation = computation;
			this.inCommunication = inCommunication;
		}
	}
}