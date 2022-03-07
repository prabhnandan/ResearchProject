package parcschedule.schedule;

import parcschedule.schedule.model.Proc;

public interface ScheduledTaskObserver<V extends TaskVertex, P extends Proc>
{
	// used in plugin
	
	/**
	 * Receive update on changed processor
	 * @param oldProc
	 * @param changedTask
	 */
	public void handleProcessorChanged(P oldProc, ScheduledTask<V, P> changedTask);
	
	/**
	 * Receive update on changed start time
	 * @param oldStartTime
	 * @param changedTask
	 */
	public void handleStartTimeChanged(int oldStartTime, ScheduledTask<V, P> changedTask);
	
	/**
	 * Receive update on start time and processor changed together
	 * @param oldProc
	 * @param oldStartTime
	 * @param changedTask
	 */
	public void handleReschedule(P oldProc, int oldStartTime, ScheduledTask<V, P> changedTask);
	
}
