package parcschedule.schedulers.clusterSchedulers.ClusterMergers;

import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedulers.SchedulerComponent;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;

public interface ClusterMerger<V extends TaskVertex> extends SchedulerComponent
{
	Clustering<V> merge(Clustering<V> inputClusters, TaskGraph<V, ?> taskGraph, int targetCount);
}
