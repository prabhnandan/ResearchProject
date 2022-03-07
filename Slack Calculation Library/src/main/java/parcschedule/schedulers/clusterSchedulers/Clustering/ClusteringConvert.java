package parcschedule.schedulers.clusterSchedulers.Clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.DrtInfo;
import parcschedule.schedule.FreeTasksObserver;
import parcschedule.schedule.OnePerTaskSchedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.TimeSlot;
import parcschedule.schedule.model.ClassicHomogeneousSystem;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;

public class ClusteringConvert 
{
	
	public static OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> toSchedule(Clustering<TaskVertex> clustering, TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph)
	{
		Map<TaskVertex, TaskVertex> taskBefore = new HashMap<>();
		Map<TaskVertex, Integer> taskStartTimes = new HashMap<>();
		
		for (Cluster<TaskVertex> cluster : clustering.clusters())
		{
			TaskVertex prevTask = null;
			for (TaskVertex task : cluster)
			{
				if (prevTask != null)
				{
					taskBefore.put(task, prevTask);
				}
				prevTask = task;
			}
		}
		
		Set<TaskVertex> expandedVertices = new HashSet<>();
		Set<TaskVertex> finishedVertices = new HashSet<>();
		List<TaskVertex> orderedVertices = new ArrayList<>(taskGraph.sizeVertices());
		
		Stack<TaskVertex> stack = new Stack<>();
		for (TaskVertex vertex : taskGraph.vertices())
		{
			if (!finishedVertices.contains(vertex))
			{
				stack.add(vertex);
			}
			
			while (!stack.empty())
			{
				TaskVertex task = stack.peek();
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
					Cluster<TaskVertex> cluster = clustering.cluster(task);
					TaskVertex precedingTask = cluster == null ? null : taskBefore.get(task);
					boolean precedingTaskIsAParent = false;
					
					for (TaskVertex parent : taskGraph.parents(task))
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
			TaskVertex task = orderedVertices.get(i);
			Cluster<TaskVertex> cluster = clustering.cluster(task);
			for (CommEdge<TaskVertex> inEdge : taskGraph.inEdges(task))
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
		
		final int finalLength = scheduleLength;
		
		return new OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc>() {
			
			TargetSystem<Proc> tSystem;
			Map<Cluster<TaskVertex>, Proc> clusterAsProc = new HashMap<>();
			
			{
				tSystem = new ClassicHomogeneousSystem(clustering.clusters().size());
				Iterator<Proc> Procs = tSystem.processors().iterator();
				for (Cluster<TaskVertex> cluster : clustering.clusters())
				{
					clusterAsProc.put(cluster, Procs.next());
				}
			}
			
			@Override
			public TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph() {
				return taskGraph;
			}

			@Override
			public TargetSystem<Proc> system() {
				return tSystem;
			}
			
			@Override
			public Proc taskProcAllocation(TaskVertex task) {
				return clusterAsProc.get(clustering.cluster(task));
			}

			@Override
			public int taskStartTime(TaskVertex task) {
				return taskStartTimes.get(task);
			}
			
			@Override
			public int scheduleLength() {
				return finalLength;
			}

			@Override
			public List<TaskVertex> procTaskList(Proc proc) { throw new UnsupportedOperationException(); }
			@Override
			public Proc earliestFinishingProc() { throw new UnsupportedOperationException(); }
			@Override
			public boolean isScheduled(TaskVertex task) { throw new UnsupportedOperationException(); }
			@Override
			public void setFreeTasksObserver(FreeTasksObserver<TaskVertex> observer) { throw new UnsupportedOperationException(); }
			@Override
			public int drt(TaskVertex task) { throw new UnsupportedOperationException(); }
			@Override
			public Proc enablingProc(TaskVertex task) { throw new UnsupportedOperationException(); }
			@Override
			public DrtInfo<Proc> drtAndEnablingProc(TaskVertex task) { throw new UnsupportedOperationException(); }
			@Override
			public int procFinishTime(Proc proc) { throw new UnsupportedOperationException(); }
			@Override
			public int drtOnProc(TaskVertex task, Proc proc) { throw new UnsupportedOperationException(); }
			@Override
			public void put(TaskVertex task, Proc proc, int startTime, boolean movable) { throw new UnsupportedOperationException(); }
			@Override
			public void put(TaskVertex task, TimeSlot<Proc> slot, boolean movable) { throw new UnsupportedOperationException(); }
			@Override
			public void put(TaskVertex task, Proc proc, int startTime, int insertionIndex, boolean movable) { throw new UnsupportedOperationException(); }
			@Override
			public void unScheduleTask(TaskVertex task) { throw new UnsupportedOperationException(); }
			@Override
			public void relocate(TaskVertex task, Proc newProc, int newStartTime, int insertionIndex, boolean movable) { throw new UnsupportedOperationException(); }
			@Override
			public boolean isFinished() { throw new UnsupportedOperationException(); }
			@Override
			public void checkValidity() throws Exception { throw new UnsupportedOperationException(); }
			@Override
			public Set<TaskVertex> freeTasks() { throw new UnsupportedOperationException(); }
		};
	}
}
