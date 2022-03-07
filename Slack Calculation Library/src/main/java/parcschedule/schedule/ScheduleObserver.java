package parcschedule.schedule;

import parcschedule.schedule.model.Proc;

public interface ScheduleObserver<V extends TaskVertex, P extends Proc>
{
	
	/**
	 * Receive information on newly scheduled task executions (ScheduledTask objects)
	 * @param scheduledTask
	 */
	public void handleTaskScheduleAdded(ScheduledTask<V, P> scheduledTask);
	
	/**
	 * Receive information on newly removed task executions
	 * @param scheduledTask
	 */
	public void handleTaskScheduleRemoved(ScheduledTask<V, P> scheduledTask);

}
