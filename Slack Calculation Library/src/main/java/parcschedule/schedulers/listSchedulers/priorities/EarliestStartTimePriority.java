package parcschedule.schedulers.listSchedulers.priorities;

import parcschedule.schedule.Schedule;
import parcschedule.schedule.ScheduleAssistant;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;

public class EarliestStartTimePriority<V extends TaskVertex, P extends Proc> extends DynamicTaskPriority<V, P>
{
	ScheduleAssistant<V, ?> assistant;

	@Override
	public V next() 
	{
		V estTask = null;
		int minEst = Integer.MAX_VALUE;
		long maxBLevelAtEst = 0;
		for (V task : schedule.freeTasks())
		{
			int est = assistant.estWithoutInsertion(task);
			long bLevel = taskGraph.getBottomLevel(task);
			if (est < minEst || (est == minEst && bLevel > maxBLevelAtEst))
			{
				minEst = est;
				maxBLevelAtEst = bLevel;
				estTask = task;
			}
		}
		return estTask;
	}

	@Override
	protected void subclassLoadSchedule(Schedule<V, ?, P> schedule)
	{
		assistant = new ScheduleAssistant<>(schedule);
	}

	@Override
	public String description()
	{
		return "Earliest Time First (Huang et al.)";
	}

	@Override
	public String code()
	{
		return "etf";
	}
	
}
