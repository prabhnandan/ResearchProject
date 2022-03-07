package parcschedule.schedulers.clusterSchedulers.Clustering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parcschedule.schedule.TaskVertex;

/**
 *  Addition and removal of tasks in constant time but no insertions. Iterates through tasks in the order they are added.
 */
public class LinkedSetCluster<V extends TaskVertex> extends Cluster<V>
{
	private Set<V> tasks = new LinkedHashSet<>();
	
	public void add(V task)
	{
		tasks.add(task);
	}
	
	public void remove(V task)
	{
		tasks.remove(task);
	}
	
	public Set<V> tasks()
	{
		return tasks;
	}

	@Override
	public boolean contains(V task)
	{
		return tasks.contains(task);
	}

	@Override
	public Iterator<V> iterator()
	{
		return tasks.iterator();
	}

	@Override
	public int size()
	{
		return tasks.size();
	}
	
	public void insertMaintainTopLevelOrder(V task, Map<V, Integer> topLevels)
	{
		if (tasks.isEmpty())
		{
			tasks.add(task);
		}
		else
		{
			List<V> newTasks = new ArrayList<>();
			boolean newTaskAdded = false;
			for (V oldTask : tasks)
			{
				if (!newTaskAdded && topLevels.get(oldTask) > topLevels.get(task))
				{
					newTasks.add(task);
					newTaskAdded = true;
				}
				newTasks.add(oldTask);
			}
			
			tasks.clear();
			for (V newTask : newTasks)
			{
				tasks.add(newTask);
			}
		}
	}
}
