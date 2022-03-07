package parcschedule.schedulers.listSchedulers.placementSchemes;

import java.util.HashSet;
import java.util.Set;

import parcschedule.schedule.AutoScheduler;
import parcschedule.schedule.ScheduleAssistant;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;

public class MinEstWithCriticalPathOnOneProc<V extends TaskVertex, P extends Proc> implements TaskPlacementScheme<V, P>
{
	boolean useInsertion;
	Set<TaskVertex> criticalPath;
	AutoScheduler<V, ?> autoScheduler;
	ScheduleAssistant<V, P> assistant;
	private P cpProc = null;
	BasicSchedule<V, ?, P> schedule;
	TaskGraph<V, ?> taskGraph;

	public MinEstWithCriticalPathOnOneProc(boolean useInsertion) 
	{
		this.useInsertion = useInsertion;
	}
	
	public MinEstWithCriticalPathOnOneProc()
	{
		this(true);
	}

	@Override
	public void setInsertion(boolean useInsertion)
	{
		this.useInsertion = useInsertion;
	}

	@Override
	public void load(BasicSchedule<V, ?, P> schedule)
	{
		assistant = new ScheduleAssistant<>(schedule);
		autoScheduler = new AutoScheduler<>(schedule);
		this.schedule = schedule;
		taskGraph = schedule.taskGraph();
		criticalPath = new HashSet<>();
		cpProc = null;
		
		// finds a set of nodes belonging to the critical path. If there are multiple CPs, only one is chosen arbitrarily
		long CPLength = 0;
		V cpNode = null;
		for (V task : taskGraph.sources())
		{
			if (taskGraph.getBottomLevel(task) > CPLength)
			{
				CPLength = taskGraph.getBottomLevel(task);
				cpNode = task;
			}
		}
		criticalPath.add(cpNode);
		while (taskGraph.outDegree(cpNode) != 0)
		{
			for (V child : taskGraph.children(cpNode))
			{
				if (taskGraph.getTopLevel(child) + taskGraph.getBottomLevel(child) == CPLength)
				{
					criticalPath.add(child);
					cpNode = child;
					break;
				}
			}
		}
	}
	
	@Override
	public void scheduleTask(V task)
	{
		
		if (useInsertion)
		{
			if (criticalPath.contains(task))
			{
				if (cpProc == null)
				{
					autoScheduler.scheduleAtEstWithInsertion(task, false);
					cpProc = schedule.taskProcAllocation(task);
				}
				else
				{
					assistant.insertTaskOnProc(task, cpProc, false);
				}
			}
			else
			{
				autoScheduler.scheduleAtEstWithInsertion(task, false);
			}
		}
		else
		{
			if (criticalPath.contains(task))
			{
				if (cpProc == null)
				{
					autoScheduler.scheduleAtEstWithoutInsertion(task, false);
					cpProc = schedule.taskProcAllocation(task);
				}
				else
				{
					assistant.appendTaskOnProc(task, cpProc, false);
				}
			}
			else
			{
				autoScheduler.scheduleAtEstWithoutInsertion(task, false);
			}
		}
	}

	@Override
	public String description()
	{
		return "Critical Path On a Processor (Ilavarasan & Thambidurai)";
	}

	@Override
	public String code()
	{
		return "cpop";
	}
}
