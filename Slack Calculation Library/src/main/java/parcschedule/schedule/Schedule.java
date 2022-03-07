package parcschedule.schedule;

import java.util.List;
import java.util.Set;

import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;

/**
 * Interface to a schedule that allows duplication. Tasks can be scheduled to execute multiple times (duplicated) on different processors and start times.
 * Each allocation of a task to a processor and start time is represented by a ScheduledTask object (a task-schedule).
 * @param <V> Vertex class
 * @param <E> Edge class
 * @param <P> Processor class
 */
public interface Schedule <V extends TaskVertex, E extends CommEdge<V>, P extends Proc>
{
	/**
	 * @return taskGraph that is being scheduled
	 */
	public TaskGraph<V, E> taskGraph();

	/**
	 * @return system of processors being scheduled to
	 */
	public TargetSystem<P> system();

	/**
	 * @throws Exception if schedule is invalid
	 */
	public void checkValidity() throws Exception;

	/**
	 * @param task
	 * @return data ready time of the task - with no assumption of the task's processor, so that no incoming communications are zeroed
	 */
	public int drt(V task);

	/**
	 * Returns object containing the data ready time and enabling processor of a task. (The enabling processor contains the parent sending the latest arriving communication.)
	 * This method exists for a little optimization. Computing them separately takes double the time so use the method if you need both.
	 * @param task
	 * @return DrtInfo object containing the data ready time and enabling processor
	 */
	public DrtInfo<P> drtAndEnablingProc(V task);

	/**
	 * @param task
	 * @param processor
	 * @return data ready time of the task on the specified processor. This is the data ready time where communciations from parents on the specified processor take zero time to reach the task
	 */
	public int drtOnProc(V task, P proc);

	/**
	 * @return processor with the earliest time at which it become idle indefinitely
	 */
	public P earliestFinishingProc();

	/**
	 * @param task
	 * @return processor holding the parent P of the task which sends the latest arriving communication to the task.
	 * (Start time of P + weight of P + weight of edge P-task) is maximum out of all parents. Therefore communications sent from this processor "enables" execution of the task.
	 */
	public P enablingProc(V task);

	/**
	 * @return set of tasks which have all of their parents scheduled but have not yet been scheduled themselves
	 */
	public Set<V> freeTasks();

	/**
	 * @return whether the schedule is finished
	 */
	public boolean isFinished();

	/**
	 * @param processor
	 * @return time at which the processor becomes idle indefinitely
	 */
	public int procFinishTime(P proc);

	/**
	 * Schedules a task, allocating a start time and processor
	 * @param task to be scheduled
	 * @param processor to schedule the task to
	 * @param startTime of the task
	 * @param movable specifies whether the scheduling of this task can be undone. If no, it makes operations faster
	 */
	public void put(V task, P proc, int startTime, boolean movable);

	/**
	 * Schedules a task, allocating a start time and processor. Contains extra parameter indexOnProc which is not important and in most cases other overloads of this method can be used
	 * @param task to be scheduled
	 * @param processor to schedule the task to
	 * @param startTime of the task
	 * @param indexOnProc indicates the position of the task the processor, i.e. how many tasks are executed before it. Saves computation if known
	 * @param movable specifies whether the scheduling of this task can be undone. If no, it can make the object faster
	 */
	public void put(V task, P proc, int startTime, int indexOnProc, boolean movable);

	/**
	 * Schedules a task using information from a TimeSlot object
	 * @param task to be scheduled
	 * @param slot containing information about how the task is to be scheduled
	 * @param movable specifies whether the scheduling of this task can be undone. If no, it can make the object faster
	 */
	public void put(V task, TimeSlot<P> slot, boolean movable);

	/**
	 * undoes the specified schedule of a task
	 * @param scheduledTask
	 */
	public void removeTaskSchedule(ScheduledTask<V, P> scheduledTask);

	/**
	 * removes all schedules of the task
	 * @param task
	 */
	public void unScheduleTask(V task);

	/**
	 * @param task
	 * @return whether the task has a schedule
	 */
	public boolean isScheduled(V task);

	/**
	 * @param task
	 * @return list of schedules for the task
	 */
	public List<ScheduledTask<V, P>> schedulesForTask(V task);

	/**
	 * @return list of task-schedules on the processor, in their order of execution, may contain multiple schedules of the same task
	 */
	public List<ScheduledTask<V, P>> taskSchedulesOnProc(P proc);

	/**
	 * @return list of all task-schedules in the entire schedule
	 */
	public List<ScheduledTask<V, P>> taskSchedules();

	/**
	 * @return length of schedule from start to finish
	 */
	public int scheduleLength();

	/**
	 * @param observer is updated (callback) when free tasks are added
	 */
	public void setFreeTasksObserver(FreeTasksObserver<V> observer);

	/**
	 * @return total reclamable slack time in the schedule
	 */
	public int slackTime();

	public void calculateSlack(StringBuilder sb);
}
