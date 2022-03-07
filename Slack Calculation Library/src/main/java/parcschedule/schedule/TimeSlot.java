package parcschedule.schedule;

import parcschedule.schedule.model.Proc;

/**
 * Contains information about a time slot on a processor to which a task's execution can be allocated
 * @param <P> Processor type
 */
public class TimeSlot<P extends Proc>
{
	P proc;
	int startTime;
	int indexOnProc;
	int slotLength;
	
	public TimeSlot(P proc, int startTime, int indexOnProc, int slotLength)
	{
		this.proc = proc;
		this.startTime = startTime;
		this.indexOnProc = indexOnProc;
		this.slotLength = slotLength;
	}
	
	/**
	 * @return processor for execution
	 */
	public P proc() { return proc; }
	
	/**
	 * @return start time for execution
	 */
	public int startTime() { return startTime; }
	
	/**
	 * @return the position of the task the processor, i.e. how many tasks are executed before it
	 */
	public int indexOnProc() { return indexOnProc; }
	
	/**
	 * @return size (time available) of an insertion slot, -1 means appending to the end of the processor (infinite size)
	 */
	public int slotLength() { return slotLength; }
}