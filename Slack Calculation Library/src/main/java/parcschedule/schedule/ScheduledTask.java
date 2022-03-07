package parcschedule.schedule;

import java.util.ArrayList;
import java.util.List;

import parcschedule.schedule.model.Proc;

/**
 * Instance of a scheduled execution of a task, used in a schedule with duplicated executions
 * @param <V> Vertex type
 * @param <P> Processor type
 */
public class ScheduledTask<V extends TaskVertex, P extends Proc>
{
	protected V task;
	protected P proc = null;
	protected int startTime = -1;
	private List<ScheduledTaskObserver<V, P>> observers = new ArrayList<>(2);
	
	public ScheduledTask(V task, P proc, int startTime)
	{
		this.task = task;
		this.proc = proc;
		this.startTime = startTime;
	}
	
	public void addObserver(ScheduledTaskObserver<V, P> observer)
	{
		observers.add(observer);
	}
	
	public ScheduledTask(V task)
	{
		this.task = task;
	}
	
	public V task()
	{
		return task;
	}
	
	public int weight()
	{
		return task.weight();
	}
	
	public P processor()
	{
		return proc;
	}
	
	public int startTime()
	{
		return startTime;
	}
	
	public int finishTime()
	{
		return startTime + task.weight();
	}
	
	public void setProcessor(P proc)
	{
		P oldProc = this.proc;
		this.proc = proc;
		for (ScheduledTaskObserver<V, P> obs : observers) obs.handleProcessorChanged(oldProc, this);
	}
	
	public void setStartTime(int startTime)
	{
		int oldStartTime = this.startTime;
		this.startTime = startTime;
		for (ScheduledTaskObserver<V, P> obs : observers) obs.handleStartTimeChanged(oldStartTime, this);
	}
	
	public void reSchedule(P proc, int startTime)
	{
		int oldStartTime = this.startTime;
		P oldProc = this.proc;
		this.proc = proc;
		this.startTime = startTime;
		for (ScheduledTaskObserver<V, P> obs : observers) obs.handleReschedule(oldProc, oldStartTime, this);
	}
}
