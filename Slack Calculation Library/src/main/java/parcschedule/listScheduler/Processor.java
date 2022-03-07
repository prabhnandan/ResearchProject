package parcschedule.listScheduler;

import java.util.LinkedList;

/**
 * Represents the abstract processor that is available for scheduling tasks.
 */
public class Processor {
	private LinkedList nodes = new LinkedList();
	private int time;	//All tasks end at this time
	private int id;	//Processor ID id. starts at 0
	private int myTime;
	
	/**
	 * Initialises a Processor object.
	 * 
	 * @param id	the unique id for this processor
	 */
	public Processor(int id) {
		this.time = 0;
		this.id = id;
	}
	
	/**
	 * Initialises a default Processor object with an id of -1.
	 *
	 */
	public Processor() {
		this(-1);
	}

	/**
	 * Adds a Node to this processor at the specified start time.
	 * 
	 * @param node			the Node to be scheduled on this processor
	 * @param startTime		the start time of the Node on the processor
	 */
	public void addNode(Node node,int startTime) {
		nodes.add(node);
		time = startTime+node.getWeight();
		node.setPNo(id);
		node.setFinishTime(time); 
	}

	/**
	 * Returns the number of nodes scheduled to this processor
	 * 
	 * @return	the number of nodes
	 */
	public int getNoOfNodes(){
		return nodes.size();
	}

	/**
	 * Returns the time at which all the nodes scheduled to this processor ends.
	 * 
	 * @return	the maximum finish time of all the nodes scheduled on this processor
	 */
	public int getTime() {
		return time;
	}

	/**
	 * Set time at which all the nodes scheduled to this processor ends.
	 *
	 */
	public void setTime(int time) {
		this.time= time;
	}
	/**
	 * Returns the unique Id of this processor
	 * 
	 * @return	the unique Id of this processor
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Returns if the given time is feasible on this processor. 
	 * Feasibility in this context is that the getTime() method should return 
	 * a value less than or equal to parameter t.
	 * 
	 * @param t		the time
	 * @return		whether the given time is feasible
	 */
	public boolean allows(int t){
		return time <= t;
	}
	
	/**
	 * The earliest start time for a Node object. Set and reset temporarily 
	 * during runtime.
	 * 
	 * @return	the time for a Node object
	 */
	public int getMyTime() {
		return myTime;
	}

	/**
	 * Sets the earliest start time for a Node object. This is a temporary value
	 * set and reset during runtime.  
	 * 
	 * @param t
	 */
	public void setMyTime(int t) {
		myTime = t;
	}
	
	/**
	 * remove a Node to this processor
	 * 
	 * @param node			the Node to be remove from this processor
	 */
	public void removeNode(Node node) {
		nodes.remove(node); 
	}

}
