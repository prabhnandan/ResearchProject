package parcschedule.schedulers.listSchedulers.placementSchemes;

import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedulers.SchedulerComponent;

public interface TaskPlacementScheme<V extends TaskVertex, P extends Proc> extends SchedulerComponent
{
	public void load(BasicSchedule<V, ?, P> schedule);
	public void setInsertion(boolean useInsertion);
	public void scheduleTask(V task);
}