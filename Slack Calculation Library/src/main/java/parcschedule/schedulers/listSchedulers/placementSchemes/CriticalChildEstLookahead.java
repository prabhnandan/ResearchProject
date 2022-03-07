package parcschedule.schedulers.listSchedulers.placementSchemes;

import parcschedule.schedule.AutoScheduler;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.Schedule;
import parcschedule.schedule.ScheduleAssistant;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.TimeSlot;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;

public class CriticalChildEstLookahead<V extends TaskVertex, P extends Proc> implements TaskPlacementScheme<V, P>
{
	
	boolean useInsertion;
	ScheduleAssistant<V, P> assistant;
	AutoScheduler<V, P> autoScheduler;
	Schedule<V, ?, P> schedule;
	TaskGraph<V, ?> taskGraph;
	TargetSystem<P> system;

	public CriticalChildEstLookahead(boolean useInsertion) 
	{
		this.useInsertion = useInsertion;
	}
	
	public CriticalChildEstLookahead()
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
		
		long maxCost = 0;
		V criticalChild = null;
		for (CommEdge<V> outEdge : taskGraph.outEdges(task))
		{
			if (taskGraph.getBottomLevel(outEdge.to()) + outEdge.weight() > maxCost)
			{
				maxCost = taskGraph.getBottomLevel(outEdge.to()) + outEdge.weight();
				criticalChild = outEdge.to();
			}
		}
		
		if (criticalChild == null)
		{
			if (useInsertion) autoScheduler.scheduleAtEstWithInsertion(task, false);
			else autoScheduler.scheduleAtEstWithoutInsertion(task, false);
		}
		else
		{
			int minEstSum = Integer.MAX_VALUE;
			if (useInsertion)
			{
				TimeSlot<P> bestSlot = null;
				for (P proc : system.processors())
				{
					TimeSlot<P> insertedSlot = assistant.insertTaskOnProc(task, proc, true);
					int childEst = assistant.estWithInsertion(criticalChild);
					if (insertedSlot.startTime() + childEst < minEstSum)
					{
						minEstSum = insertedSlot.startTime() + childEst;
						bestSlot = insertedSlot;
					}
					schedule.unScheduleTask(task);
				}
				schedule.put(task, bestSlot, false);
			}
			else 
			{
				P bestProc = null;
				for (P proc : system.processors())
				{
					int taskEst = assistant.appendTaskOnProc(task, proc, true);
					int childEst = assistant.estWithoutInsertion(criticalChild);
					if (taskEst + childEst < minEstSum)
					{
						minEstSum = taskEst + childEst;
						bestProc = proc;
					}
					schedule.unScheduleTask(task);
				}
				assistant.appendTaskOnProc(task, bestProc, false);
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
		return "minimises sum of ESTs of the task and its critical child (Kwok & Ahmad)";
	}

	@Override
	public String code()
	{
		return "critical-child-est";
	}
}