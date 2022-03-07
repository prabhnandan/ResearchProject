package parcschedule.schedulers.clusterSchedulers.Clusterers;

import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedulers.SchedulerComponent;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;

public interface Clusterer<V extends TaskVertex> extends SchedulerComponent
{
	Clustering<V> cluster(TaskGraph<V, ?> taskGraph);
}
