package parcschedule.schedulers.listSchedulers.placementSchemes;

import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.Schedule;
import parcschedule.schedule.ScheduleAssistant;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;
import parcschedule.schedulers.graphProperties.OptimisticBLevelTable;

public class MinEstPlusEstimatedBLevel<V extends TaskVertex, P extends Proc> implements TaskPlacementScheme<V, P>
{
	boolean useInsertion;
	ScheduleAssistant<V, P> assistant;
	OptimisticBLevelTable<V, ?> bLevelEstimates;
	TargetSystem<P> system;
	
	public MinEstPlusEstimatedBLevel(Schedule<V, ?, P> schedule, boolean useInsertion, OptimisticBLevelTable<V, ?> precomputedTable) 
	{
		this.useInsertion = useInsertion;
		assistant = new ScheduleAssistant<>(schedule);
		bLevelEstimates = precomputedTable;
		system = schedule.system();
	}

	public MinEstPlusEstimatedBLevel(Schedule<V, ?, P> schedule, boolean useInsertion)
	{
		this(schedule, useInsertion, new OptimisticBLevelTable<>(schedule.taskGraph(), schedule.system()));
	}

	@Override
	public void load(BasicSchedule<V, ?, P> schedule) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scheduleTask(V task)
	{
		int minEstPlusEstimate = Integer.MAX_VALUE;
		P bestProc = null;
		
		if (useInsertion)
		{
			for (P proc : system.processors())
			{
				int estPlusEstimate = assistant.estOnProcWithInsertion(task, proc).startTime() + bLevelEstimates.get(task, proc);
				if (estPlusEstimate < minEstPlusEstimate)
				{
					minEstPlusEstimate = estPlusEstimate;
					bestProc = proc;
				}
			}
			assistant.insertTaskOnProc(task, bestProc, false);
		}
		else
		{
			for (P proc : system.processors())
			{
				int estPlusEstimate = assistant.estWithoutInsertion(task) + bLevelEstimates.get(task, proc);
				if (estPlusEstimate < minEstPlusEstimate)
				{
					minEstPlusEstimate = estPlusEstimate;
					bestProc = proc;
				}
			}
			assistant.appendTaskOnProc(task, bestProc, false);
		}
	}

	@Override
	public String description()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String code()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInsertion(boolean useInsertion) {
		// TODO Auto-generated method stub
		
	}
}
