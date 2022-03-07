package parcschedule.schedulers.clusterSchedulers.Clustering;

import parcschedule.schedule.TaskVertex;

public abstract class Cluster<V extends TaskVertex> implements Iterable<V>
{
	public abstract boolean contains(V task);
	public abstract int size();
	
	public boolean equalElements(Cluster<TaskVertex> other)
	{
		if (this.size() != other.size())
		{
			return false;
		}
		for (TaskVertex task : this)
		{
			if ( !other.contains(task) )
			{
				return false;
			}
		}
		return true;
	}
	
	public V first()
	{
		return this.iterator().next();
	}
}
