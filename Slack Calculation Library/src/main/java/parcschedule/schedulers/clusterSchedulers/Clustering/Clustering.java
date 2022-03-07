package parcschedule.schedulers.clusterSchedulers.Clustering;

import java.util.Collection;

import parcschedule.schedule.TaskVertex;

public interface Clustering<V extends TaskVertex>
{
	Collection<? extends Cluster<V>> clusters();
	Cluster<V> cluster(V task);
}
