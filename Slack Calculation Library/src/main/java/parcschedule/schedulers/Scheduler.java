package parcschedule.schedulers;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.OnePerTaskSchedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;

public interface Scheduler<V extends TaskVertex, E extends CommEdge<V>, P extends Proc>
{
	OnePerTaskSchedule<V, E, P> schedule(TaskGraph<V, E> taskGraph, TargetSystem<P> system); 
	String description();
}