package parcschedule.schedulers.clusterSchedulers.Clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parcschedule.schedule.TaskVertex;;

/**
 *  uses an ArrayList and keeps track of preceding and succeding tasks which work as virtual edges
 */
public class ArrayCluster<V extends TaskVertex> extends Cluster<V>
{
	private List<V> tasks = new ArrayList<>();
	private Set<V> taskSet = new HashSet<>();

	private Map<V, V> precedingTasks = new HashMap<>();
	private Map<V, V> succedingTasks = new HashMap<>();
	
	private ClusterChangedObserver observer;
	
	public void add(V task)
	{
		add(task, tasks.size());
	}
	
	public void add(V task, int index)
	{
		if (index != 0)
		{
			V taskBefore = tasks.get(index - 1);
			succedingTasks.put(taskBefore, task);
			precedingTasks.put(task, taskBefore);
		}
		
		if (index != tasks.size())
		{
			V taskAfter = tasks.get(index);
			precedingTasks.put(taskAfter, task);
			succedingTasks.put(task, taskAfter);
		}
		
		tasks.add(index, task);
		taskSet.add(task);
		if (observer != null) observer.notifyClusterChanged();
	}
	
	public void remove(int index)
	{
		V taskBefore = index == 0 ? null : tasks.get(index - 1);
		V taskAfter = (index == tasks.size() - 1) ? null : tasks.get(index + 1);

		if (taskBefore != null)
		{
			succedingTasks.put(taskBefore, taskAfter);
		}
		if (taskAfter != null)
		{
			precedingTasks.put(taskAfter, taskBefore);
		}
		
		taskSet.remove(tasks.get(index));
		tasks.remove(index);
		if (observer != null) observer.notifyClusterChanged();
	}
	
	public V get(int index)
	{
		return tasks.get(index);
	}
	
	public V precedingTask(V task)
	{
		return precedingTasks.get(task);
	}
	
	public V succedingTask(V task)
	{
		return succedingTasks.get(task);
	}

	public V last()
	{
		return tasks.get(tasks.size() - 1);
	}

	public int size()
	{
		return tasks.size();
	}
	
	public void setObserver(ClusterChangedObserver observer)
	{
		this.observer = observer;
	}
	
	public void removeObserver()
	{
		observer = null;
	}
	
	@Override
	public boolean contains(V task) 
	{
		return taskSet.contains(task);
	}

	@Override
	public Iterator<V> iterator()
	{
		return tasks.iterator();
	}
}

interface ClusterChangedObserver
{
	void notifyClusterChanged();
}
