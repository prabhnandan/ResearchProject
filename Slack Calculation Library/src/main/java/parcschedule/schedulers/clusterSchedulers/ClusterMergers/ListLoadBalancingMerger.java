package parcschedule.schedulers.clusterSchedulers.ClusterMergers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.DrtInfo;
import parcschedule.schedule.FreeTasksObserver;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;
import parcschedule.schedulers.clusterSchedulers.Clustering.Cluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.ClusteredLevelCalculator;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;

public class ListLoadBalancingMerger<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> extends ClusterMergerScheduler<V, E, P>
{
	
	@Override
	public BasicSchedule<V, E, P> mergeAndSchedule(Clustering<V> clustering, TaskGraph<V, E> taskGraph, TargetSystem<P> system)
	{
		LlbSchedule<V, E, P> schedule = new LlbSchedule<>(clustering, taskGraph, system);
		Map<Proc, PriorityQueue<V>> procFreeTasks = new HashMap<>();
		Map<Cluster<V>, P> clusterProcMap = new HashMap<>();
		ClusteredLevelCalculator<V> allocatedLevels = new ClusteredLevelCalculator<>(clustering, taskGraph);
		
		Comparator<V> levelComparator = new Comparator<V>() {

			@Override
			public int compare(V A, V B) {
				// inversed priority because PriorityQueue maintains minimum priority
				int val = allocatedLevels.bottom(B) - allocatedLevels.bottom(A);
				return val != 0 ? val : A.index() - B.index();
			}
		};
		
		for (P proc : system.processors())
		{
			procFreeTasks.put(proc, new PriorityQueue<>(levelComparator));
		}
		procFreeTasks.put(null, new PriorityQueue<>(levelComparator)); // null key leads to list of unmapped ready tasks
		
		schedule.setFreeTasksObserver(new FreeTasksObserver<V>() {
			
			@Override
			public void handleFreeTasksAdded(Collection<V> addedTasks) {

				for (V task : addedTasks)
				{
					procFreeTasks.get(clusterProcMap.get(clustering.cluster(task))).add(task);
				}
			}

			@Override
			public void handleFreeTasksRemoved(Collection<V> removedTasks) {
				throw new UnsupportedOperationException();
			}
		});
		
		while (!schedule.isFinished())
		{
			P idleProc = schedule.earliestFinishingProc();
			
			V mappedTask = procFreeTasks.get(idleProc).peek();
			V unmappedTask = procFreeTasks.get(null).peek();
			V task;
			
			if (mappedTask == null && unmappedTask == null) // current idle processor does not have free tasks
			{
				task = null;
				for (P proc : system.processors())
				{
					V urgentTaskOnProc = procFreeTasks.get(proc).peek();
					if (urgentTaskOnProc != null)
					{
						if (task == null || allocatedLevels.bottom(urgentTaskOnProc) > allocatedLevels.bottom(task))
						{
							task = urgentTaskOnProc;
						}
					}
				}
				
				if (schedule.isScheduled(task)) throw new RuntimeException();
				schedule.put(task, clusterProcMap.get(clustering.cluster(task)), schedule.est(task), false);
			}
			else 
			{
				int est;
				
				if (mappedTask == null)
				{
					task = unmappedTask;
					est = schedule.estOnProc(unmappedTask, idleProc);
				}
				else if (unmappedTask == null)
				{
					task = mappedTask;
					est = schedule.est(mappedTask);
				}
				else
				{
					task = schedule.est(mappedTask) <= schedule.estOnProc(unmappedTask, idleProc) ? mappedTask : unmappedTask;
					if (schedule.est(mappedTask) <= schedule.estOnProc(unmappedTask, idleProc))
					{
						task = mappedTask;
						est = schedule.est(mappedTask);
					}
					else
					{
						task = unmappedTask;
						est = schedule.estOnProc(unmappedTask, idleProc);
					}
				}

				if (schedule.isScheduled(task)) throw new RuntimeException();
				schedule.put(task, idleProc, est, false);
			}
			
			Cluster<V> cluster = clustering.cluster(task);
			procFreeTasks.get(clusterProcMap.get(cluster)).poll(); // remove scheduled task from processor free tasks list
			
			if (clusterProcMap.get(cluster) == null)
			{
				clusterProcMap.put(cluster, idleProc);
				schedule.mapClusterToProc(cluster, idleProc);
				
				PriorityQueue<V> estProcFreeTasks = procFreeTasks.get(idleProc);
				PriorityQueue<V> oldUnmappedFreeTasks = procFreeTasks.get(null);
				PriorityQueue<V> newUnmappedFreeTasks = new PriorityQueue<>(levelComparator);
				// rebuild unmapped free-tasks list (priority queue) to avoid removing newly mapped tasks one by one because that takes O(N) each
				for (V unmappedFreeTask : oldUnmappedFreeTasks)
				{
					if (cluster.contains(unmappedFreeTask))
					{
						// move unmapped free tasks in the same cluster as the scheduled task to their new mapped processor's free task list
						estProcFreeTasks.add(unmappedFreeTask);
					}
					else
					{
						newUnmappedFreeTasks.add(unmappedFreeTask);
					}
				}
				procFreeTasks.put(null, newUnmappedFreeTasks);
			}
		}
		return schedule;
	}

	@Override
	public String description()
	{
		return "List Load Balancing (Radulescu et al.)";
	}

	@Override
	public String code()
	{
		return "llb";
	}
}

class LlbSchedule<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> extends BasicSchedule<V, E, P>
{

	public LlbSchedule(Clustering<V> clustering, TaskGraph<V, E> taskGrpah, TargetSystem<P> system) 
	{
		super(taskGrpah, system);
	}
	
	@Override
	protected void updateChildren(V task)
	{
	    Collection<V> newFreeTasks = new ArrayList<>(taskGraph.outDegree(task));
		for (CommEdge<V> outEdge : taskGraph.outEdges(task))
		{
			V child = outEdge.to();
			// update children's DRTs
		    int edgeFinishTime = taskStartTimes.get(task) + task.weight() + 
		    		(taskProcAllocations.get(task) == taskProcAllocations.get(outEdge.to()) ? 0 : outEdge.weight());
		    if (DRTs.get(child) < edgeFinishTime)
		    {
		    	DRTs.put(child, edgeFinishTime);
		    	enablingProcs.put(child, taskProcAllocations.get(task));
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
	
	public void mapClusterToProc(Cluster<V> cluster, P proc)
	{
		for (V task : cluster)
		{
			taskProcAllocations.put(task, proc);
		}
	}
	
	public int est(V task)
	{
		return Math.max(DRTs.get(task), procFinishTimes.get(taskProcAllocations.get(task)));
	}
	
	public int estOnProc(V task, P proc)
	{
		if (taskProcAllocations.containsKey(task)) throw new RuntimeException();
		
		DrtInfo<P> drtInfo = drtAndEnablingProc(task);
		if (drtInfo.drt == 0) 
		{
			return procFinishTime(proc);
		}
		else
		{
			int drt = drtInfo.enablingProc == proc ? drtOnProc(task, proc) : drtInfo.drt;
			return Math.max(procFinishTime(proc), drt);
		}
	}
}
