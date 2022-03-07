package parcschedule.schedulers.listSchedulers.priorities;

import parcschedule.schedule.Schedule;
import parcschedule.schedule.ScheduleAssistant;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;

public class BLevelMinusEstPriority<V extends TaskVertex, P extends Proc> extends DynamicTaskPriority<V, P>
{
	ScheduleAssistant<V, ?> assistant;
	boolean useInsertion;
	
	public BLevelMinusEstPriority(boolean useInsertion) 
	{
		this.useInsertion = useInsertion;
	}

	@Override
	protected void subclassLoadSchedule(Schedule<V, ?, P> schedule)
	{
		assistant = new ScheduleAssistant<>(schedule);
	}

	@Override
	public V next() 
	{
		V maxTask = null;
		long maxBLevelMinusEst = Integer.MIN_VALUE;
		if (useInsertion)
		{
			for (V task : schedule.freeTasks())
			{
				long bLevelMinusEst = taskGraph.getBottomLevel(task) - assistant.estWithInsertion(task);
				if (bLevelMinusEst > maxBLevelMinusEst)
				{
					maxBLevelMinusEst = bLevelMinusEst;
					maxTask = task;
				}
			}
		}
		else
		{
			for (V task : schedule.freeTasks())
			{
				long bLevelMinusEst = taskGraph.getBottomLevel(task) - assistant.estWithoutInsertion(task);
				if (bLevelMinusEst > maxBLevelMinusEst)
				{
					maxBLevelMinusEst = bLevelMinusEst;
					maxTask = task;
				}
			}
		}
		return maxTask;
	}

	@Override
	public String description()
	{
		return "Dynamic Level Scheduling (b-level - EST) (Sih & Lee)";
	}

	@Override
	public String code()
	{
		return "dls";
	}
	
}
