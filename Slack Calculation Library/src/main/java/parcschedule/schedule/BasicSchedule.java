package parcschedule.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;

public class BasicSchedule<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> extends OnePerTaskSchedule<V, E, P>
{
	
	public final TargetSystem<P> system;
	public final TaskGraph<V, E> taskGraph;
	
	protected Set<V> freeTasks = new LinkedHashSet<>();
	protected Set<V> partiallyFreeTasks = new LinkedHashSet<>();
	
	protected Map<V, P> taskProcAllocations = new HashMap<>();
	protected Map<Proc, ArrayList<V>> procTaskLists = new HashMap<>();
	protected Map<V, Integer> taskStartTimes = new HashMap<>();
	
	protected Map<V, P> enablingProcs = new HashMap<>();
	protected Map<V, Integer> DRTs = new HashMap<>();
	protected Map<V, Integer> scheduledParentCounts = new HashMap<>();

	protected Set<V> temporaryTasks = new LinkedHashSet<>();
	protected Map<V, Map<V, E>> temporaryTasksChildren = new HashMap<>();

	protected Map<Proc, Integer> procFinishTimes = new HashMap<>();
	protected ProcFinishMinHeap procHeap;
	
	protected FreeTasksObserver<V> freeTasksObserver;
	
	public BasicSchedule(TaskGraph<V, E> taskGraph, TargetSystem<P> system)
	{
		this.taskGraph = taskGraph;
		this.system = system;
		
		for (P p : system.processors())
		{
			procTaskLists.put(p, new ArrayList<V>());
			procFinishTimes.put(p, 0);
		}
		for (V task : taskGraph.vertices())
		{
			scheduledParentCounts.put(task, 0);
			DRTs.put(task, 0);
			if (taskGraph.inDegree(task) == 0) freeTasks.add(task);
		}
		procHeap = new ProcFinishMinHeap(system.processors());
	}
	
	/**
	 * Registers an observer that recieves updates on newly freed up tasks. (observer could be a static task priority 
	 * not complying with task precedences and needing to maintain a priority queue of free tasks to ensure that compliance)
	 */
	public void setFreeTasksObserver(FreeTasksObserver<V> observer)
	{
		freeTasksObserver = observer;
		observer.handleFreeTasksAdded(freeTasks);
	}
	
	/**
	 * Incrementally updates properties of the task's children. The updates are irreversible and this method is 
	 * only called on permanent tasks
	 */
	protected void updateChildren(V task)
	{

	    Collection<V> newFreeTasks = new ArrayList<>(taskGraph.outDegree(task));
		for (E outEdge : taskGraph.outEdges(task))
		{
			V child = outEdge.to();
			
			// update children's DRTs
		    int actualEdgeReadyTime = taskStartTimes.get(task) + task.weight() + outEdge.weight();
		    if (DRTs.get(child) < actualEdgeReadyTime)
		    {
		    	DRTs.put(child, actualEdgeReadyTime);
		    	enablingProcs.put(child, taskProcAllocations.get(task));
		    }
			
			// update 'free' status of children
			int scheduledParentCount = scheduledParentCounts.get(child) + 1;
			scheduledParentCounts.put(child, scheduledParentCount);
			if (scheduledParentCount == taskGraph.inDegree(child))
			{
				freeTasks.add(child);
				newFreeTasks.add(child);
				partiallyFreeTasks.remove(child);
			}
			else partiallyFreeTasks.add(child);
		}
		if (freeTasksObserver != null) freeTasksObserver.handleFreeTasksAdded(newFreeTasks);
	}
	
	/**
	 * Gets the data ready time with all *scheduled parents* in consideration, which means this is an 
	 * optimistic estimate unless the task is completely free
	 * @return Data ready time without any zeroed incoming communication
	 */
	public int drt(V task)
	{
		return drtAndEnablingProc(task).drt;
	}
	
	/**
	 * Gets the processor on which the parent providing the latest arriving communication is assigned,
	 * only takes into account the already scheduled parents
	 * @return enabling processor 
	 */
	public P enablingProc(V task)
	{
		return drtAndEnablingProc(task).enablingProc;
	}
	
	/**
	 * Gets an object containing the DRT and the enabling processor. calculating them together takes the 
	 * same effort as doing it separately which is why this method exists
	 * @return DrtInfo object containing the drt and enabling processor
	 */
	public DrtInfo<P> drtAndEnablingProc(V task)
	{
		int maxDrt = DRTs.get(task);
		
		if (!temporaryTasks.isEmpty())
		{
			if (temporaryTasks.size() < taskGraph.inDegree(task))
			{
				// iterates through temporary tasks to correct the temporary drt as needed
				V tempConstrainingParent = null;
				for (V tempTask : temporaryTasks)
				{
					if (temporaryTasksChildren.get(tempTask).containsKey(task))
					{
						int edgeWeight =  temporaryTasksChildren.get(tempTask).get(task).weight();
						int edgeArrivalTime = taskStartTimes.get(tempTask) + tempTask.weight() + edgeWeight;
						if (edgeArrivalTime > maxDrt)
						{
							maxDrt = edgeArrivalTime;
							tempConstrainingParent = tempTask;
						}
					}
				}
				if (tempConstrainingParent == null)
				{
					return new DrtInfo<>(maxDrt, enablingProcs.get(task));
				}
				else 
				{
					return new DrtInfo<>(maxDrt, taskProcAllocations.get(tempConstrainingParent));
				}
			}
			else // there are more temporary tasks than there are parents of the task so it's faster to iterate through parents
			{
				V constrainingParent = null;
				maxDrt = 0;
				for (E inEdge : taskGraph.inEdges(task))
				{
					V parent = inEdge.from();
					if (!taskProcAllocations.containsKey(parent)) continue;
					int drt = taskStartTimes.get(parent) + parent.weight() + inEdge.weight();
					if (drt > maxDrt)
					{
						maxDrt = drt;
						constrainingParent = parent;
					}
				}
				return new DrtInfo<>(maxDrt, taskProcAllocations.get(constrainingParent));
			}
		}
		else
		{
			return new DrtInfo<>(maxDrt, enablingProcs.get(task));
		}
	}

	/**
	 * @return The time at which the processor becomes available indefinitely
	 */
	public int procFinishTime(P proc) 
	{
		if (!procFinishTimes.containsKey(proc)) throw new RuntimeException();
		return procFinishTimes.get(proc);
	}
	
	/**
	 * @return Data ready time of the task with communications from the given processor voided
	 */
	public int drtOnProc(V task, P proc)
	{
		int maxDrt = 0;
		for (E inEdge : taskGraph.inEdges(task))
		{
			V parent = inEdge.from();
			if (!taskProcAllocations.containsKey(parent)) continue; // Drt is partial estimate using only scheduled parents
			int drt = taskStartTimes.get(parent) + parent.weight();
			if (taskProcAllocations.get(parent) != proc) drt += inEdge.weight();
			maxDrt = Math.max(maxDrt, drt);
		}
		return maxDrt;
	}
	
	/**
	 * Puts a task in the schedule
	 * @param movable states if the task's position in the schedule can be changed or removed, permanent tasks give better performance
	 */
	public void put(V task, P proc, int startTime, boolean movable)
	{
		put(task, proc, startTime, -1, movable);
	}

	/**
	 * Puts a task in the schedule
	 * @param movable states if the task's position in the schedule can be changed or removed, permanent tasks give better performance
	 */
	public void put(V task, TimeSlot<P> slot, boolean movable)
	{
		put(task, slot.proc, slot.startTime, slot.indexOnProc, movable);
	}
	
	
	/**
	 * @param movable states if the task's position in the schedule can be changed or removed, permanent tasks give better performance
	 * -1 means to append
	 */
	public void put(V task, P proc, int startTime, int insertionIndex, boolean movable)
	{		
		if (temporaryTasks.contains(task)) throw new IllegalArgumentException();
		if (taskStartTimes.containsKey(task)) throw new IllegalArgumentException();
		
		taskProcAllocations.put(task, proc);
		taskStartTimes.put(task, startTime);
		
		if (insertionIndex == -1)
		{
			procTaskLists.get(proc).add(task);
		}
		else 
		{
			procTaskLists.get(proc).add(insertionIndex, task);;
		}
		updateProc(proc);
		
		if (!movable) 
		{
			updateChildren(task);
			freeTasks.remove(task);
		}
		else
		{
			temporaryTasks.add(task);
			// the temporaryTasksChildren(children of temporary tasks) sets are used to take temporary nodes into account
			// when calculating DRT. Because it is incrementally updated and are non-backtrackable, temporary nodes do not 
			// update them and are instead separately examined to make corrections to non-temporary values.
			Map<V, E> taskChildren = new HashMap<>();
			temporaryTasksChildren.put(task, taskChildren);
			for (E outEdge : taskGraph.outEdges(task))
			{
				taskChildren.put(outEdge.to(), outEdge);
			}
		}
	}
	
	/**
	 * updates processor finish time and its place in the minimum finish time heap
	 */
	protected void updateProc(P proc)
	{
		List<V> taskList = procTaskList(proc);
		if (taskList.isEmpty()) 
		{
			procFinishTimes.put(proc, 0);
		}
		else
		{
			V lastTask = taskList.get(taskList.size() - 1);
			procFinishTimes.put(proc, taskStartTime(lastTask) + lastTask.weight());
		}
		procHeap.update(proc);
	}
	
	/**
	 * Removes a scheduled task that was not scheduled permanently
	 */
	public void unScheduleTask(V task)
	{
		if (!taskProcAllocations.containsKey(task)) throw new NoSuchElementException();
		if (!temporaryTasks.contains(task)) throw new NoSuchElementException();

		// task lists are maintained for getting proc finish times and making insertions
		P proc = taskProcAllocations.get(task);
		List<V> tasksOnProc = procTaskLists.get(proc);
		if (tasksOnProc.get(tasksOnProc.size() - 1) == task)
		{
			// this would be the case for 1-step lookahead methods without insertion and this removal costs nothing
			tasksOnProc.remove(tasksOnProc.size() - 1);
		}
		else
		{
			// binary search is used to locate the task but removal from the list still takes linear time
			int locationOnProc = Collections.binarySearch(tasksOnProc, task, new Comparator<V>() {

				@Override
				public int compare(V A, V B) {
					int val = taskStartTimes.get(A) - taskStartTimes.get(B);
					return val != 0 ? val : A.index() - B.index();
				}
			});
			
			if (tasksOnProc.get(locationOnProc) != task) throw new NoSuchElementException("task was not on processor task list");
			tasksOnProc.remove(locationOnProc);
		}
		updateProc(proc);
		
		taskProcAllocations.remove(task);
		taskStartTimes.remove(task);
		temporaryTasks.remove(task);
	}
	
	/**
	 * Re-schedules a task that was not scheduled permanently
	 */
	public void relocate(V task, P newProc, int newStartTime, int insertionIndex, boolean movable)
	{
		unScheduleTask(task);
		put(task, newProc, newStartTime, insertionIndex, movable);		
	}
	
	public boolean isFinished() 
	{ 
		return freeTasks.isEmpty(); 
	}
	
	public void checkValidity() throws Exception
	{
		for (V task : taskGraph.tasks())
		{
			if (taskStartTimes.get(task) < drtOnProc(task, taskProcAllocations.get(task)))
			{
				int a = taskStartTimes.get(task);
				int b = drtOnProc(task, taskProcAllocations.get(task));
				throw new Exception(task.name() + " begins before drt");
			}
		}
		Set<V> taskListsUnion = new HashSet<>();
		for (P proc : system.processors())
		{
			List<V> taskList = procTaskLists.get(proc);
			for (int i = 0; i < taskList.size() - 1; i++)
			{
				V task = taskList.get(i);
				if (taskListsUnion.contains(task))
				{
					throw new Exception(task.name() + " appears in multiple task lists");
				}
				taskListsUnion.add(task);
				
				if (taskStartTimes.get(task) + task.weight() > taskStartTimes.get(taskList.get(i + 1)))
				{
					V t2 = taskList.get(i + 1);
					int s1 = taskStartTimes.get(task);
					int s2 = taskStartTimes.get(taskList.get(i + 1));
					throw new Exception("possible execution overlap"); // or incorrect task list ordering
				}
			}
		}
	}
	
	/**
	 * @return tasks with all parents scheduled
	 */
	public Set<V> freeTasks() 
	{ 
		return freeTasks; 
	}
	
	/**
	 * @return tasks with at least one parent scheduled
	 */
	public Set<V> partiallyFreeTasks() 
	{ 
		return partiallyFreeTasks; 
	}
	
	public P taskProcAllocation(V task) 
	{ 
		return taskProcAllocations.get(task); 
	}
	
	public int taskStartTime(V task)
	{ 
		return taskStartTimes.get(task); 
	}
	
	/**
	 * @return list of tasks to be executed on the given processor, in their order of execution
	 */
	public List<V> procTaskList(P proc)
	{
		return procTaskLists.get(proc);
	}
	
	public P earliestFinishingProc()
	{
		return procHeap.earliestFinishProc();
	}
	
	public int scheduleLength()
	{
		int scheduleLength = 0;
		for (P proc : system.processors())
		{
			scheduleLength = Math.max(scheduleLength, procFinishTimes.get(proc));
		}
		return scheduleLength;
	}
	
	public boolean isScheduled(V task)
	{
		return taskStartTimes.containsKey(task);
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

	/**
	 * Minimum heap with processors compared by finish time
	 */
	class ProcFinishMinHeap
	{
		private List<P> heap;
		private Map<Proc, Integer> procIndices = new HashMap<>();
		
		public ProcFinishMinHeap(Collection<P> procs)
		{
			heap = new ArrayList<>(procs.size());
			// assuming processors start empty, fill heap arbitrarily and record indices
			int i = 0;
			for (P proc : procs)
			{
				heap.add(proc);
				procIndices.put(proc, i++);
			}
		}
		
		public void update(P proc)
		{
			int index = procIndices.get(proc);
			
			boolean shiftUp = false;
			// shifting up
			while (index != 0) // while not at top of heap
			{
				int parentIndex = (index + 1) / 2 - 1;
				P parent = heap.get(parentIndex);
				if (procFinishTime(proc) < procFinishTime(parent))
				{
					shiftUp = true;
					heap.set(index, parent);
					heap.set(parentIndex, proc);
					procIndices.put(parent, index);
					index = parentIndex;
				}
				else 
				{
					break;
				}
			}
			
			if (!shiftUp)
			{
				// shifting down
				while (true)
				{
					int leftChildIndex = index * 2 + 1;
					if (leftChildIndex >= heap.size())
					{
						break; // task is at bottom of heap, no children
					}
					P leftChild = heap.get(leftChildIndex);
					
					int rightChildIndex = leftChildIndex + 1;
					if (rightChildIndex >= heap.size()) // no right child
					{
						if (procFinishTime(leftChild) < procFinishTime(proc))
						{
							heap.set(index, leftChild);
							heap.set(leftChildIndex, proc);
							procIndices.put(leftChild, index);
							index = leftChildIndex;
						}
						
						break; // task will be at bottom of heap if it shifted down this iteration, so either way it stops
					}
					else
					{
						P rightChild = heap.get(rightChildIndex);
						if (Math.min(procFinishTime(leftChild), procFinishTime(rightChild)) < procFinishTime(proc))
						{
							if (procFinishTime(rightChild) < procFinishTime(leftChild))
							{
								heap.set(index, rightChild);
								heap.set(rightChildIndex, proc);
								procIndices.put(rightChild, index);
								index = rightChildIndex;
							}
							else
							{
								heap.set(index, leftChild);
								heap.set(leftChildIndex, proc);
								procIndices.put(leftChild, index);
								index = leftChildIndex;
							}
						}
						else
						{
							break; // stop when children no longer finish earlier
						}
					}
				}
			}
			procIndices.put(proc, index);
		}
		
		public P earliestFinishProc()
		{
			return heap.get(0);
		}
	}
}