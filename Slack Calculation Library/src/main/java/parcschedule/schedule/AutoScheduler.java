package parcschedule.schedule;

import parcschedule.schedule.model.Proc;

public class AutoScheduler<V extends TaskVertex, P extends Proc> {

	Schedule<V, ?, P> schedule;
	ScheduleAssistant<V, P> assistant;
	
	public AutoScheduler(Schedule<V, ?, P> schedule)
	{
		this.schedule = schedule;
		assistant = new ScheduleAssistant<V, P>(schedule);
	}
	
	/**
	 * Schedules a task at its earliest start time, using insertion
	 * @param movable specifies whether the task's position in the schedule can be changed or removed
	 * @return the scheduled start time
	 */
	public int scheduleAtEstWithInsertion(V task, boolean movable)
	{
		TimeSlot<P> earliestSlot = null;
		for (P proc : schedule.system().processors())
		{
			TimeSlot<P> slot = assistant.estOnProcWithInsertion(task, proc, earliestSlot == null ? -1 : earliestSlot.startTime);
			if (slot != null)
			{
				earliestSlot = slot;
			}
		}
		schedule.put(task, earliestSlot.proc, earliestSlot.startTime, earliestSlot.indexOnProc, movable);
		return earliestSlot.startTime;
	}

	/**
	 * Schedules a task at its earliest start time, without using insertion
	 * @param movable specifies whether the task's position in the schedule can be changed or removed
	 * @return the scheduled start time
	 */
	public int scheduleAtEstWithoutInsertion(V task, boolean movable)
	{
		DrtInfo<P> drtInfo = schedule.drtAndEnablingProc(task);
		int drt = drtInfo.drt;
		P enablingProc = drtInfo.enablingProc;
		
		if (drt == 0 ^ enablingProc == null) throw new RuntimeException();
		
		int procFinishTime = (drt == 0 ? -1 : schedule.procFinishTime(enablingProc));
		if (drt != 0 && drt >= procFinishTime)
		{
			int reducedDrt = schedule.drtOnProc(task, enablingProc);
			int est = Math.max(reducedDrt, procFinishTime);
			schedule.put(task, enablingProc, est, movable);
			return est;
		}
		else
		{
			P earliestProc = schedule.earliestFinishingProc();
			int est = Math.max(drt, schedule.procFinishTime(earliestProc));
			schedule.put(task, earliestProc, est, movable);
			return est;
		}
	}
}
