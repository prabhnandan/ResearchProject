package parcschedule.schedule;

import parcschedule.graphGenerator.graphReaders.DotReader;
import parcschedule.schedule.model.ClassicHomogeneousSystem;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;

import java.io.File;
import java.util.*;

public class DuplicationSchedule<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> implements Schedule<V, E, P>, ScheduledTaskObserver<V, P>
{
	public final TaskGraph<V, E> taskGraph;
	public final TargetSystem<P> system;

	protected List<ScheduledTask<V, P>> taskSchedules = new ArrayList<>();

	protected Map<V, List<ScheduledTask<V, P>>> taskScheduledTasks = new HashMap<>();
	protected Map<P, List<ScheduledTask<V, P>>> procScheduledTasks = new HashMap<>();

	protected ScheduleObserver<V, P> scheduledTasksObserver;
	protected FreeTasksObserver<V> freeTasksObserver;

	protected Map<V, Integer> scheduledParentCounts = new HashMap<>();
	protected Set<V> freeTasks = new HashSet<>();

	Comparator<ScheduledTask<V, P>> startTimeComparator = new Comparator<ScheduledTask<V, P>>() {
		@Override
		public int compare(ScheduledTask<V, P> o1, ScheduledTask<V, P> o2) {
			int val = o1.startTime() - o2.startTime();
			return val != 0 ? val : o1.task().index() - o1.task().index();
		}
	};

	public DuplicationSchedule(TaskGraph<V, E> taskGraph, TargetSystem<P> system)
	{
		this.taskGraph = taskGraph;
		this.system = system;
		for (V task : taskGraph.vertices())
		{
			taskScheduledTasks.put(task, new ArrayList<>());
			scheduledParentCounts.put(task, 0);
		}
		for (P proc : system.processors())
		{
			procScheduledTasks.put(proc, new ArrayList<>());
		}
	}

//	public DuplicationSchedule(BasicSchedule<V, E, P> schedule)
//	{
//		taskGraph = schedule.taskGraph();
//		system = schedule.system();
//		copyScheduleFrom(schedule);
//	}
//
//	public void copyScheduleFrom(BasicSchedule<V, E, P> schedule)
//	{
//		if (! taskGraph.equals(schedule.taskGraph())) return;
//
//		for (V task : taskGraph.vertices())
//		{
//			addScheduledTask(new ScheduledTask<>(task, schedule.taskProcAllocation(task), schedule.taskStartTime(task)));
//		}
//	}

	public void setSheduledTasksObserver(ScheduleObserver<V, P> observer)
	{
		scheduledTasksObserver = observer;
	}

	public void setFreeTasksObserver(FreeTasksObserver<V> observer)
	{
		freeTasksObserver = observer;
	}

	public List<ScheduledTask<V, P>> schedulesForTask(V task)
	{
		return taskScheduledTasks.get(task);
	}

	public List<ScheduledTask<V, P>> taskSchedules()
	{
		return taskSchedules;
	}

	public void addScheduledTask(ScheduledTask<V, P> scheduledTask)
	{
		addScheduledTask(scheduledTask, null);
	}

	public void addScheduledTask(ScheduledTask<V, P> scheduledTask, Integer insertionPoint)
	{
		V task = scheduledTask.task();
		P proc = scheduledTask.processor();

		taskSchedules.add(scheduledTask);

		// add to list of schedules on processor
		List<ScheduledTask<V, P>> procTaskList = procScheduledTasks.get(proc);
		if (procTaskList == null)
		{
			procTaskList = new ArrayList<>();
			procScheduledTasks.put(proc, procTaskList);
		}
		if (insertionPoint == null)
		{
			// find appropriate insertion point
			int searchResult = Collections.binarySearch(procTaskList, scheduledTask, startTimeComparator);
			if (searchResult >= 0) throw new RuntimeException("instance exists");
			else
			{
				insertionPoint = -(searchResult + 1);
			}
		}
		else if (insertionPoint == -1) // (append)
		{
			insertionPoint = procTaskList.size();
		}
		procTaskList.add(insertionPoint, scheduledTask);

		// update free status of task's children if the task was unscheduled before
		if (taskScheduledTasks.get(task).isEmpty())
		{
			Collection<V> newFreeTasks = new ArrayList<>();
			for (V child : taskGraph.children(task))
			{
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
		// add to list of schedules for the task
		taskScheduledTasks.get(task).add(scheduledTask);

		if (scheduledTasksObserver != null) scheduledTasksObserver.handleTaskScheduleAdded(scheduledTask);
		scheduledTask.addObserver(this);
	}

	public List<ScheduledTask<V, P>> taskSchedulesOnProc(P proc)
	{
		return procScheduledTasks.get(proc);
	}

	public void removeTaskSchedule(ScheduledTask<V, P> scheduledTask)
	{
		V task = scheduledTask.task();
		P proc = scheduledTask.processor();

		taskSchedules.remove(scheduledTask);

		// remove from list of schedules on processor
		List<ScheduledTask<V, P>> procTaskList = procScheduledTasks.get(proc);
		int searchResult = Collections.binarySearch(procTaskList, scheduledTask, startTimeComparator);
		if (searchResult < 0)
		{
			throw new NoSuchElementException("instance of scheduled task does not exist");
		}
		procTaskList.remove(searchResult);

		//remove from list of schedules for the task
		taskScheduledTasks.get(task).remove(scheduledTask);

		// update free status of task's children if the task has now become unscheduled
		if (taskScheduledTasks.get(task).isEmpty())
		{
			Collection<V> removedFreeTasks = new ArrayList<>();
			for (V child : taskGraph.children(task))
			{
				int scheduledParentCount = scheduledParentCounts.get(child) - 1;
				scheduledParentCounts.put(child, scheduledParentCount);

				if (scheduledParentCount == 0)
				{
					freeTasks.remove(child);
					removedFreeTasks.add(child);
				}
			}
			if (freeTasksObserver != null) freeTasksObserver.handleFreeTasksRemoved(removedFreeTasks);
		}

		if (scheduledTasksObserver != null) scheduledTasksObserver.handleTaskScheduleRemoved(scheduledTask);
	}

	public int drt(V task)
	{
		return drtAndEnablingProc(task).drt;
	}

	public DrtInfo<P> drtAndEnablingProc(V task)
	{
		int drt = 0;
		P enablingProc = null;
		for (E inEdge : taskGraph.inEdges(task))
		{
			V parent = inEdge.from();
			P bestProcProvidingComm = null;
			int minScheduledEdgeArrival = Integer.MAX_VALUE;
			for (ScheduledTask<V, P> scheduledParent : taskScheduledTasks.get(parent))
			{
//				minScheduledEdgeArrival = Math.min(minScheduledEdgeArrival,
//						scheduledParent.finishTime() + inEdge.weight());
				if (scheduledParent.finishTime() + inEdge.weight() < minScheduledEdgeArrival)
				{
					minScheduledEdgeArrival = scheduledParent.finishTime() + inEdge.weight();
					bestProcProvidingComm = scheduledParent.processor();
				}
			}
			if (minScheduledEdgeArrival > drt && bestProcProvidingComm != null)
			{
				drt = minScheduledEdgeArrival;
				enablingProc = bestProcProvidingComm;
			}
		}
		return new DrtInfo<>(drt, enablingProc);
	}

	public int drtOnProc(V task, P proc)
	{
		int drt = 0;
		for (E inEdge : taskGraph.inEdges(task))
		{
			V parent = inEdge.from();
			int minScheduledEdgeArrival = Integer.MAX_VALUE;

			for (ScheduledTask<V, P> scheduledParent : taskScheduledTasks.get(parent))
			{
				minScheduledEdgeArrival = Math.min(minScheduledEdgeArrival,
						scheduledParent.finishTime() + (scheduledParent.processor().equals(proc) ? 0 : inEdge.weight()));
			}
			if (! taskScheduledTasks.get(parent).isEmpty())
			{
				drt = Math.max(minScheduledEdgeArrival, drt);
			}
		}
		return drt;
	}

	public P earliestFinishingProc()
	{
		int earliestFinish = Integer.MAX_VALUE;
		P earliestProc = null;
		for (P proc : system.processors())
		{
			int finishTime = procFinishTime(proc);
			if (finishTime < earliestFinish)
			{
				earliestFinish = finishTime;
				earliestProc = proc;
			}
		}
		return earliestProc;
	}

	public P enablingProc(V task)
	{
		return drtAndEnablingProc(task).enablingProc;
	}

	public Set<V> freeTasks()
	{
		return freeTasks;
	}

	public boolean isFinished()
	{
		return freeTasks().isEmpty();
	}

	public int procFinishTime(P proc)
	{
		List<ScheduledTask<V, P>> taskList = procScheduledTasks.get(proc);
		if (! taskList.isEmpty())
		{
			return taskList.get(taskList.size() - 1).finishTime();
		}
		else return 0;
	}

	public int scheduleLength()
	{
		int scheduleLength = 0;
		for (P proc : system.processors())
		{
			scheduleLength = Math.max(scheduleLength, procFinishTime(proc));
		}
		return scheduleLength;
	}

	@Override
	public TaskGraph<V, E> taskGraph()
	{
		return taskGraph;
	}

	@Override
	public TargetSystem<P> system()
	{
		return system;
	}

	@Override
	public void handleProcessorChanged(P oldProc, ScheduledTask<V, P> changedTask)
	{
		// remove from list of schedules on old processor
		List<ScheduledTask<V, P>> procTaskList = procScheduledTasks.get(oldProc);
		int searchResult = Collections.binarySearch(procTaskList, changedTask, startTimeComparator);
		if (searchResult < 0)
		{
			throw new NoSuchElementException();
		}
		procTaskList.remove(searchResult);

		// add to list of schedules on new processor
		procTaskList = procScheduledTasks.get(changedTask.processor());
		searchResult = Collections.binarySearch(procTaskList, changedTask, startTimeComparator);
		if (searchResult >= 0)
		{
			throw new RuntimeException();
		}
		procTaskList.add(-(searchResult + 1), changedTask);
	}

	@Override
	public void handleStartTimeChanged(int oldTime, ScheduledTask<V, P> changedTask)
	{
		// remove from list of schedules on the processor, which is no longer in correct order
		List<ScheduledTask<V, P>> procTaskList = procScheduledTasks.get(changedTask.processor());
		procTaskList.remove(changedTask);
		// add to the list again
		int searchResult = Collections.binarySearch(procTaskList, changedTask, startTimeComparator);
		if (searchResult >= 0)
		{
			throw new RuntimeException();
		}
		procTaskList.add(-(searchResult + 1), changedTask);
	}

	@Override
	public void handleReschedule(P oldProc, int oldTime, ScheduledTask<V, P> changedTask)
	{
		// remove from list of schedules on the old processor, which is no longer in correct order
		List<ScheduledTask<V, P>> procTaskList = procScheduledTasks.get(oldProc);
		procTaskList.remove(changedTask);

		// add to list of schedules on new processor
		procTaskList = procScheduledTasks.get(changedTask.processor());
		int searchResult = Collections.binarySearch(procTaskList, changedTask, startTimeComparator);
		if (searchResult >= 0)
		{
			throw new RuntimeException();
		}
		procTaskList.add(-(searchResult + 1), changedTask);
	}

	public void put(V task, P proc, int startTime, boolean movable)
	{
		addScheduledTask(new ScheduledTask<V, P>(task, proc, startTime), null);
	}

	public void put(V task, P proc, int startTime, int indexOnProc, boolean movable)
	{
		addScheduledTask(new ScheduledTask<V, P>(task, proc, startTime), indexOnProc);
	}

	public void put(V task, TimeSlot<P> slot, boolean movable)
	{
		addScheduledTask(new ScheduledTask<V, P>(task, slot.proc(), slot.startTime()), slot.indexOnProc);
	}

	public boolean isScheduled(V task)
	{
		return !taskScheduledTasks.get(task).isEmpty();
	}

	public void unScheduleTask(V task)
	{
		for (ScheduledTask<V, P> scheduledTask : new ArrayList<>(taskScheduledTasks.get(task)))
		{
			removeTaskSchedule(scheduledTask);
		}
	}

	public boolean isEmpty()
	{
		return taskSchedules.isEmpty();
	}

	public void handleSystemChanged()
	{
		if (!isEmpty()) throw new RuntimeException();
		procScheduledTasks.clear();
		for (P proc : system.processors())
		{
			procScheduledTasks.put(proc, new ArrayList<>());
		}
	}

	public void checkValidity()
	{
		// TODO
	}

	@Override
	public int slackTime()
	{
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public void calculateSlack(StringBuilder sb) {
		
	}

	public static void main(String[] args) {

		TaskGraph<TaskVertex, CommEdge<TaskVertex>> graph = DotReader.read(new File("DAGS/DupeTest_ChainToFork_Nodes_5.dot"));

		DuplicationSchedule duplicationSchedule;
		ClassicHomogeneousSystem system = new ClassicHomogeneousSystem(4);
		duplicationSchedule = new DuplicationSchedule<>(graph, system);

		ScheduleAssistant scheduleAssistant = new ScheduleAssistant(duplicationSchedule);

		scheduleAssistant.appendTaskOnProc(graph.task(0), system.getProc(0), false);
		scheduleAssistant.appendTaskOnProc(graph.task(0), system.getProc(1), false);
		scheduleAssistant.appendTaskOnProc(graph.task(1), system.getProc(0), false);
		scheduleAssistant.appendTaskOnProc(graph.task(1), system.getProc(1), false);
		scheduleAssistant.appendTaskOnProc(graph.task(2), system.getProc(0), false);
		scheduleAssistant.appendTaskOnProc(graph.task(2), system.getProc(1), false);
		scheduleAssistant.appendTaskOnProc(graph.task(3), system.getProc(0), false);
		scheduleAssistant.appendTaskOnProc(graph.task(4), system.getProc(1), false);


		int length = duplicationSchedule.scheduleLength();

		System.out.println(length);
	}
}
