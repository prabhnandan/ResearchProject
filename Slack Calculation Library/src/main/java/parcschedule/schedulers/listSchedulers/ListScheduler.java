package parcschedule.schedulers.listSchedulers;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;
import parcschedule.schedulers.Scheduler;
import parcschedule.schedulers.listSchedulers.placementSchemes.TaskPlacementScheme;
import parcschedule.schedulers.listSchedulers.priorities.TaskPrioritizingIterator;

public class ListScheduler<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> implements Scheduler<V, E, P>
{
	TaskPrioritizingIterator<V, P> taskIterator;
	TaskPlacementScheme<V, P> taskPlacementScheme;
	
	public ListScheduler(TaskPlacementScheme<V, P> taskPlacementScheme, TaskPrioritizingIterator<V, P> taskIterator)
	{
		this.taskIterator = taskIterator;
		this.taskPlacementScheme = taskPlacementScheme;
	}
	
	@Override
	public BasicSchedule<V, E, P> schedule(TaskGraph<V, E> taskGraph, TargetSystem<P> system)
	{
		BasicSchedule<V, E, P> schedule = new BasicSchedule<>(taskGraph, system);
		taskIterator.load(schedule);
		taskPlacementScheme.load(schedule);
		
		while (taskIterator.hasNext())
		{
			V next = taskIterator.next();
			// System.out.print(next.name() + ", ");
			taskPlacementScheme.scheduleTask(next);
		}
		
		return schedule;
	}

	@Override
	public String description()
	{
		return null;
	}
}
