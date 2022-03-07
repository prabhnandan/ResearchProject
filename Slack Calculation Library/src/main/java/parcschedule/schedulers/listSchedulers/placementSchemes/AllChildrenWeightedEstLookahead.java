package parcschedule.schedulers.listSchedulers.placementSchemes;

import parcschedule.schedule.AutoScheduler;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.Schedule;
import parcschedule.schedule.ScheduleAssistant;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.TimeSlot;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;

public class AllChildrenWeightedEstLookahead<V extends TaskVertex, P extends Proc> implements TaskPlacementScheme<V, P>
{
	boolean useInsertion;
	ScheduleAssistant<V, P> assistant;
	AutoScheduler<V, P> autoScheduler;
	Schedule<V, ?, P> schedule;
	TaskGraph<V, ?> taskGraph;
	TargetSystem<P> system;

	public AllChildrenWeightedEstLookahead(boolean useInsertion) 
	{
		this.useInsertion = useInsertion;
	}
	
	public AllChildrenWeightedEstLookahead()
	{
		this(true);
	}

	@Override
	public void setInsertion(boolean useInsertion)
	{
		this.useInsertion = useInsertion;
	}

	@Override
	public void scheduleTask(V task)
	{
		
		long minProductSum = Long.MAX_VALUE;

		if (useInsertion) // insertion only applies to the task itself, not to its children which would be too expensive
		{
			if (taskGraph.outDegree(task) > 0)
			{
				TimeSlot<P> bestSlot = null;
				for (P proc : system.processors())
				{
					TimeSlot<P> insertedSlot = assistant.insertTaskOnProc(task, proc, true);
					
					long estBLevelProductSum = 0;
					for (V child : taskGraph.children(task))
					{
						estBLevelProductSum += assistant.estWithInsertion(child) * taskGraph.getBottomLevel(child);
					}
					
					if (estBLevelProductSum < minProductSum)
					{
						minProductSum = estBLevelProductSum;
						bestSlot = insertedSlot;
					}
					schedule.unScheduleTask(task);
				}
				schedule.put(task, bestSlot, false);
			}
			else
			{
				autoScheduler.scheduleAtEstWithInsertion(task, false);
			}
		}
		else 
		{
			if (taskGraph.outDegree(task) > 0)
			{
				P bestProc = null;
				for (P proc : system.processors())
				{
					assistant.appendTaskOnProc(task, proc, true);
	
					long estBLevelProductSum = 0;
					for (V child : taskGraph.children(task))
					{
						estBLevelProductSum += assistant.estWithoutInsertion(child) * taskGraph.getBottomLevel(child);
					}
					
					if (estBLevelProductSum < minProductSum)
					{
						minProductSum = estBLevelProductSum;
						bestProc = proc;
					}
					schedule.unScheduleTask(task);
				}
				assistant.appendTaskOnProc(task, bestProc, false);
			}
			else
			{
				autoScheduler.scheduleAtEstWithoutInsertion(task, false);
			}
		}
	}

	@Override
	public void load(BasicSchedule<V, ?, P> schedule)
	{
		assistant = new ScheduleAssistant<>(schedule);
		autoScheduler = new AutoScheduler<>(schedule);
		this.schedule = schedule;
		taskGraph = schedule.taskGraph();
		system = schedule.system();
	}

	@Override
	public String description()
	{
		return "minimises weighted ESTs of all children (Bittencourt et al.)";
	}

	@Override
	public String code()
	{
		return "weighted-children-est";
	}
}
