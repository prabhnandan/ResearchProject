package parcschedule.schedulers.listSchedulers.priorities;

import parcschedule.schedule.Schedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;

public abstract class DynamicTaskPriority<V extends TaskVertex, P extends Proc> implements TaskPrioritizingIterator<V, P>
{
	Schedule<V, ?, P> schedule;
	TaskGraph<V, ?> taskGraph;
	
	@Override
	public boolean hasNext() 
	{
		return !schedule.freeTasks().isEmpty();
	}
	
	@Override
	public boolean isDynamic() 
	{
		return true;
	}

	@Override
	public void load(Schedule<V, ?, P> schedule)
	{
		this.schedule = schedule;
		taskGraph = schedule.taskGraph();
		subclassLoadSchedule(schedule);
	}

	protected abstract void subclassLoadSchedule(Schedule<V, ?, P> schedule);
}
