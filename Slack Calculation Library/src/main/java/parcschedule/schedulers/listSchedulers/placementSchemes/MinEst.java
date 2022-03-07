package parcschedule.schedulers.listSchedulers.placementSchemes;

import parcschedule.schedule.AutoScheduler;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;

public class MinEst<V extends TaskVertex, P extends Proc> implements TaskPlacementScheme<V, P>
{
	AutoScheduler<V, P>  scheduler;
	boolean useInsertion;
	
	public MinEst(boolean insertion)
	{
		this.useInsertion = insertion;
	}
	
	public MinEst()
	{
		this(true);
	}

	@Override
	public void load(BasicSchedule<V, ?, P> schedule)
	{
		scheduler = new AutoScheduler<>(schedule);
	}

	@Override
	public void setInsertion(boolean useInsertion)
	{
		this.useInsertion = useInsertion;
	}
	
	@Override
	public void scheduleTask(V task)
	{
		if (useInsertion)
		{
			scheduler.scheduleAtEstWithInsertion(task, false);
		}
		else
		{
			scheduler.scheduleAtEstWithoutInsertion(task, false);
		}
	}

	@Override
	public String description()
	{
		return "minimises task's own EST (i.e. no lookahead)";
	}

	@Override
	public String code()
	{
		return "norm";
	}
}
