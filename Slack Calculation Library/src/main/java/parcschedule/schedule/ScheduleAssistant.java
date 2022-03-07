package parcschedule.schedule;

import java.util.List;

import parcschedule.schedule.model.Proc;

public class ScheduleAssistant<V extends TaskVertex, P extends Proc>
{
	Schedule<V, ?, P> schedule;
	
	public ScheduleAssistant(Schedule<V, ?, P> schedule)
	{
		this.schedule = schedule;
	}

	/**
	 * Schedules a task to a specified processor at the earliest start time, without using insertion
	 * @param movable states if the task's position in the schedule can be changed or removed
	 * @return the scheduled start time
	 */
	public int appendTaskOnProc(V task, P proc, boolean movable)
	{
		int est = estOnProc(task, proc);
		schedule.put(task, proc, est, movable);
		return est;
	}
	
	/**
	 * Schedules a task to a specified processor at the earliest start time, using insertion
	 * @param movable states if the task's position in the schedule can be changed or removed
	 * @return the scheduled slot
	 */
	public TimeSlot<P> insertTaskOnProc(V task, P proc, boolean movable)
	{
		TimeSlot<P> slot = estOnProcWithInsertion(task, proc, -1);
		schedule.put(task, proc, slot.startTime, slot.indexOnProc, movable);
		return slot;
	}
	
	/**
	 * gives the earliest start time of a task on a specified processor without insertion.
	 * @param task
	 * @param processor
	 * @return earliest start time of the task
	 */
	public int estOnProc(V task, P proc)
	{
//		DrtInfo<P> drtInfo = schedule.drtAndEnablingProc(task);
//		if (drtInfo.drt == 0)
//		{
//			return schedule.procFinishTime(proc);
//		}
//		else
//		{
//			int drt = drtInfo.enablingProc == proc ? schedule.drtOnProc(task, proc) : drtInfo.drt;
//			return Math.max(schedule.procFinishTime(proc), drt);
//		}
		//TODO: is there a difference between this and the more complicated logic above?
		return Math.max(schedule.procFinishTime(proc), schedule.drtOnProc(task, proc));
	}
	
	/**
	 * gives information about the earliest start time of a task on a specified processor, when insertion is used
	 * @return TimeSlot object containing start time, processor, insertion index, and slot length
	 * @param insertedTask
	 * @param processor
	 */
	public TimeSlot<P> estOnProcWithInsertion(V insertedTask, P proc)
	{
		return estOnProcWithInsertion(insertedTask, proc, -1);
	}
	
	/**
	 * @param upperTimeBound specifies a start time after which the search will not continue (-1 means no limit). Useful when it is known that the task can start at upperTimeBound on another processor
	 * @return TimeSlot object containing start time, processor, insertion index, and slot length, null if time bound is reached before finding an available slot
	 */
	public TimeSlot<P> estOnProcWithInsertion(V insertedTask, P proc, int upperTimeBound)
	{
		List<ScheduledTask<V, P>> scheduledTasksOnProc = schedule.taskSchedulesOnProc(proc);
		int procFinishTime = schedule.procFinishTime(proc);
		DrtInfo<P> drtInfo = schedule.drtAndEnablingProc(insertedTask);
		
		int drt;
		P enablingProc = drtInfo.enablingProc;
		if (enablingProc == proc) 
		{
			drt = schedule.drtOnProc(insertedTask, enablingProc);
		}
		else drt = drtInfo.drt;
		
		if (upperTimeBound < 0)
		{
			upperTimeBound = Integer.MAX_VALUE;
		}
		else if (drt >= upperTimeBound) 
		{
			return null;
		}
		
		int searchBegin;
		ScheduledTask<V, P> previousTask;
		ScheduledTask<V, P> currentTask;
		
		if (scheduledTasksOnProc.isEmpty()) // no previous tasks on proc
		{
			return new TimeSlot<>(proc, drt, 0, -1);
		}
		
		if (procFinishTime <= drt) // no need to insert, can only append
		{
			return new TimeSlot<>(proc, drt, -1, -1);
		}
		
		if (drt < scheduledTasksOnProc.get(0).startTime()) // begin looking for gap from very start of the scheduled task list
		{
			int slotLength = scheduledTasksOnProc.get(0).startTime() - drt;
			if (insertedTask.weight() <= slotLength)
			{
				return new TimeSlot<>(proc, drt, 0, slotLength);
			}
			else searchBegin = 1;
		}
		else // binary search through task list to find the point where gaps will be after the drt
		{
			int lower = 0;
			int upper = scheduledTasksOnProc.size() - 1;
			int mid = lower + (upper - lower) / 2;
			while (true)
			{
				if (upper < lower)
				{
					searchBegin = lower;
					break;
				}
				else if (upper == lower)
				{
					if (scheduledTasksOnProc.get(lower).startTime() > drt)
					{
						searchBegin = lower;
						break;
					}
					else
					{
						searchBegin = lower + 1;
						break;
					}
				}
				
				int taskStart = scheduledTasksOnProc.get(mid).startTime();
				if (taskStart == drt)
				{
					searchBegin = mid + 1;
					break;
				}
				else if (taskStart > drt)
				{
					upper = mid - 1;
				}
				else
				{
					lower = mid + 1;
				}
				
				mid = lower + (upper - lower) / 2;
			}
		}
		
		if (searchBegin < scheduledTasksOnProc.size())
		{
			previousTask = scheduledTasksOnProc.get(searchBegin - 1);
			currentTask = scheduledTasksOnProc.get(searchBegin);
			// examines first gap (the one before the task at searchBegin) where data will be ready, 
			// which could be part-way through the gap
			int est = Math.max(previousTask.startTime() + previousTask.weight(), drt);
			if (est >= upperTimeBound) return null;
			int slotLength = currentTask.startTime() - est;
			if (insertedTask.weight() <= slotLength)
			{
				return new TimeSlot<>(proc, est, searchBegin, slotLength);
			}
			// examines rest of the gaps until finish or the given upper time bound
			// will be skipped when searchBegin was at the last element
			for (int i = searchBegin + 1; i < scheduledTasksOnProc.size(); i++)
			{
				previousTask = currentTask;
				currentTask = scheduledTasksOnProc.get(i);
				est = previousTask.startTime() + previousTask.weight();
				if (est >= upperTimeBound) return null;
				slotLength = currentTask.startTime() - est;
				if (insertedTask.weight() <= slotLength)
				{
					return new TimeSlot<>(proc, est, i, slotLength);
				}
			}
		}
		int est = schedule.procFinishTime(proc);
		if (est >= upperTimeBound) return null;
		return new TimeSlot<P>(proc, est, -1, -1);
	}
	
	/**
	 * @param task
	 * @return the earliest start time of the task when insertion is used
	 */
	public int estWithInsertion(V task)
	{
		TimeSlot<P> earliestSlot = null;
		for (P proc : schedule.system().processors())
		{
			TimeSlot<P> slot = estOnProcWithInsertion(task, proc, earliestSlot == null ? -1 : earliestSlot.startTime);
			if (slot != null)
			{
				earliestSlot = slot;
			}
		}
		return earliestSlot.startTime;
	}
	
	/**
	 * @param task
	 * @return the earliest start time of the task when insertion is not used
	 */
	public int estWithoutInsertion(V task)
	{
		DrtInfo<P> drtInfo = schedule.drtAndEnablingProc(task);
		int drt = drtInfo.drt;
		P enablingProc = drtInfo.enablingProc;

		int finishTimeOnEnablingProc = enablingProc == null ? Integer.MAX_VALUE : schedule.procFinishTime(enablingProc);
		if (drt >= finishTimeOnEnablingProc)
		{
			int reducedDrt = schedule.drtOnProc(task, enablingProc);
			return Math.max(reducedDrt, finishTimeOnEnablingProc);
		}
		else
		{
			return Math.max(drt, schedule.procFinishTime(schedule.earliestFinishingProc()));
		}
	}
}
