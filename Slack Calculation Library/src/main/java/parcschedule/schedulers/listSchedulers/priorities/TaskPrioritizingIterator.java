package parcschedule.schedulers.listSchedulers.priorities;

import java.util.Iterator;

import parcschedule.schedule.Schedule;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedulers.SchedulerComponent;

public interface TaskPrioritizingIterator<V extends TaskVertex, P extends Proc> extends Iterator<V>, SchedulerComponent
{
	public void load(Schedule<V, ?, P> schedule);
	boolean isDynamic();
}
