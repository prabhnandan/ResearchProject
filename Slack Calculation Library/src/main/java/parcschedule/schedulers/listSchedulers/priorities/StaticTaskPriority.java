package parcschedule.schedulers.listSchedulers.priorities;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import parcschedule.schedule.FreeTasksObserver;
import parcschedule.schedule.Schedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;

public abstract class StaticTaskPriority<V extends TaskVertex, P extends Proc> implements TaskPrioritizingIterator<V, P>, FreeTasksObserver<V>
{
	protected TaskGraph<V, ?> taskGraph;
	private List<V> priorityList;
	private Map<TaskVertex, Integer> taskPriorities;
	private PriorityQueue<V> freeTasks;
	boolean loaded;
	int index;
	
	protected abstract boolean compliesWithPrecedence();
	protected abstract List<V> buildPriorityList();
	protected abstract void subclassLoad(Schedule<V, ?, ?> schedule);
	
	public void load(Schedule<V, ?, P> schedule)
	{
		index = 0;		
		this.taskGraph = schedule.taskGraph();
		subclassLoad(schedule);
		taskPriorities = new HashMap<>();
		priorityList = buildPriorityList();
		
		if (!compliesWithPrecedence()) // initialize priority queue
		{
			// sometimes a priority list isn't built with pairwise comparisons, and it's easier to obtain 
			// comparisons by using list indices after the list is built
			for (int i = 0; i < priorityList.size(); i++)
			{
				taskPriorities.put(priorityList.get(i), i);
			}
			
			freeTasks = new PriorityQueue<>(new Comparator<TaskVertex>() {
				// priority list is in descending order, higher priorities were assigned lower indices
				// PriorityQueue maintains the *least* value, whereas the highest is wanted 
				@Override
				public int compare(TaskVertex A, TaskVertex B) 
				{
					int val = taskPriorities.get(A) - taskPriorities.get(B);
					return val != 0 ? val : A.index() - B.index();
				}
			});
		}
		
		schedule.setFreeTasksObserver(this);
		loaded = true;
	}

	@Override
	public boolean hasNext()
	{
		if (!loaded) throw new RuntimeException();
		
		if (compliesWithPrecedence())
		{
			return index < priorityList.size();
		}
		else
		{
			return !freeTasks.isEmpty();
		}
	}

	@Override
	public V next() 
	{
		if (!loaded) throw new RuntimeException();
		
		if (compliesWithPrecedence())
		{
			return priorityList.get(index++);
		}
		else
		{
			return freeTasks.poll();
		}
	}

	@Override
	public void handleFreeTasksAdded(Collection<V> newFreeTasks)
	{
		if (compliesWithPrecedence()) 
		{
			return;
		}
		for (V task : newFreeTasks)
		{
			freeTasks.add(task);
		}
	}
	
	@Override
	public void handleFreeTasksRemoved(Collection<V> newFreeTasks) 
	{ 
		freeTasks.remove(newFreeTasks); 
	}

	@Override
	public boolean isDynamic() 
	{
		return false;
	}
}
