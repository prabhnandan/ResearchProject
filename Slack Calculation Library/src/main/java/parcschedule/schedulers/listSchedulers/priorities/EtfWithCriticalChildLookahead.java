package parcschedule.schedulers.listSchedulers.priorities;

import java.util.ArrayList;

import parcschedule.schedule.AutoScheduler;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.Schedule;
import parcschedule.schedule.ScheduleAssistant;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.TimeSlot;
import parcschedule.schedule.model.Proc;

public class EtfWithCriticalChildLookahead<V extends TaskVertex, P extends Proc> extends DynamicTaskPriority<V, P>
{
	ScheduleAssistant<V, P> assistant;
	AutoScheduler<V, P> autoScheduler;
	boolean useInsertion;
	
	public EtfWithCriticalChildLookahead(boolean useInsertion)
	{
		this.useInsertion = useInsertion;
	}

	@Override
	public V next()
	{
		V bestTask = null;
		int minMinEstSum = Integer.MAX_VALUE;
		
		for (V task : new ArrayList<>(schedule.freeTasks()))
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

			int minEstSum = Integer.MAX_VALUE;
			
			if (criticalChild == null)
			{
				if (useInsertion)
				{
					minEstSum = assistant.estWithInsertion(task);
				}
				else
				{
					minEstSum = assistant.estWithoutInsertion(task);
				}
			}
			else
			{
				if (useInsertion)
				{
					for (P proc : schedule.system().processors())
					{
						TimeSlot<P> insertedSlot = assistant.insertTaskOnProc(task, proc, true);
						int childEst = assistant.estWithInsertion(criticalChild);
						
						minEstSum = Math.min(insertedSlot.startTime() + childEst, minEstSum);
						
						schedule.unScheduleTask(task);
					}
				}
				else 
				{
					for (P proc : schedule.system().processors())
					{
						int taskEst = assistant.appendTaskOnProc(task, proc, true);
						int childEst = assistant.estWithoutInsertion(criticalChild);
						
						minEstSum = Math.min(minEstSum, taskEst + childEst);
						
						schedule.unScheduleTask(task);
					}
				}
			}

			if (minEstSum < minMinEstSum)
			{
				minMinEstSum = minEstSum;
				bestTask = task;
			}
			
			if (bestTask == null) throw new RuntimeException();
		}
		
		return bestTask;
	}

	@Override
	public String description() {
		return "Earliest time first with critical child lookahead";
	}

	@Override
	public String code() {
		return "etfcc";
	}

	@Override
	protected void subclassLoadSchedule(Schedule<V, ?, P> schedule)
	{
		assistant = new ScheduleAssistant<>(schedule);
		autoScheduler = new AutoScheduler<>(schedule);
	}

}
