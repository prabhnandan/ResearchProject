package parcschedule.schedulers.clusterSchedulers.ClusterMergers;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;

public abstract class ClusterMergerScheduler<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> implements ClusterMerger<V>
{
	public abstract BasicSchedule<V, E, P> mergeAndSchedule(Clustering<V> clustering, TaskGraph<V, E> taskGraph, TargetSystem<P> system);

	@Override
	public Clustering<V> merge(Clustering<V> inputClusters, TaskGraph<V, ?> taskGraph, int targetCount)
	{
		throw new UnsupportedOperationException(); // ClusterScheduler detects this class and calls the other method instead
	}
}
