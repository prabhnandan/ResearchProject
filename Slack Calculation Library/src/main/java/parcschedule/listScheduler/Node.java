package parcschedule.listScheduler;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Represents a Node (Task) object.
 */
public class Node {
	private String node;
	private int weight;
	private LinkedList parents = new LinkedList();
	private LinkedList children = new LinkedList();
	private Node succ;
	private int b; 
	private int pNo;
	private int finishTime;
	private int parentsProcessor;
	private boolean allParentsInSameProcessor;
	
	/**
	 * Initialises a Node (Task) object.
	 * 
	 * @param node		the string identifying the node
	 * @param weight	the computation cost (weight) of the node
	 */
	public Node(String node, int weight) {
		this.node = node;
		this.weight = weight;
		this.b = weight;
		this.pNo = -1;
		this.finishTime = 0;
		allParentsInSameProcessor = true; 
	}
	
	/**
	 * Initialises a default Node (Task) object with string id -1 
	 * and computation cost (weight) of -1
	 *
	 */
	public Node(){
		this("-1",-1);
	}
	
	/**
	 *Returns this Node object.
	 *
	 * @return	the Node
	 */
	public String getNode() {
		return node;
	}
	
	/**
	 * Returns the computation cost (weight) of the node.
	 * 
	 * @return	the weight
	 */
	public int getWeight() {
		return weight;
	}

	/**
	 * Sets this Node object's identifying string.
	 * 
	 * @param node	the Node identifying string
	 */
	public void setNode(String node) {
		this.node = node;
	}

	/**
	 * Sets the computation cost (weight) of this Node.
	 * 
	 * @param weight	the weight of this Node
	 */
	public void setWeight(int weight) {
		this.weight = weight;
	}

	/**
	 * Returns the bottom-level value (b-value) of this Node in the task graph.
	 * 
	 * @return		the b-value
	 */
	public int getB() {
		return b;
	}

	/**
	 * Returns a list of all the successors from this Node in the task graph. 
	 * 
	 * @return		the list of successors
	 */
	public LinkedList getChildren() {
		return children;
	}

	/**
	 * Returns a list of all the predecessors to this Node in the task graph.
	 * 
	 * @return		the list of predecessors
	 */
	public LinkedList getParents() {
		return parents;
	}
	
	/**
	 * Returns the number of predecessors to this Node in the task graph.
	 * 
	 * @return		the number of predecessors
	 */
	public int getNoOfParents() {
		return parents.size();
	}

	/**
	 * Returns the successor for this Node which is the path that needs to be taken
	 * for the longest path to an exit node.  
	 * 
	 * @return		the successor Node
	 */
	public Node getSucc() {
		return succ;
	}

	/**
	 * Sets the bottom-level value for this Node in the task graph.
	 * 
	 * @param bValue	the bottom-level value
	 */
	public void setB(int bValue) {
		this.b = bValue;
	}

	/**
	 * Adds a successor to this Node in the task graph.
	 * 
	 * @param node		the successor to be added
	 */
	public void addChildren(Node node) {
		children.add(node);
	}

	/**
	 * Adds a predecessor to this Node in the task graph.
	 * 
	 * @param node		the predecessor to be added
	 */
	public void addParents(Node node) {
		parents.add(node);
	}

	/**
	 * Sets the successor for this Node which is the path that needs to be taken
	 * for the longest path (the b-value path) to an exit node.
	 * 
	 * @param successor		the Successor Node
	 */
	public void setSucc(Node successor) {
		succ = successor;
	}
	
	/**
	 * Returns a string representation of this Node.
	 * 
	 */
	public String toString(){
		String s = "\n";
		s += "Node index: "+node+"\n";
		s += "Weight: "+weight+"\n";
		s += "Node parents: ";
		for(Iterator it = parents.iterator(); it.hasNext();){
			Node n = (Node) it.next();
			s += n.getNode()+" ";
		}
		s += "\n";
		s += "Node children: ";
		for(Iterator it = children.iterator(); it.hasNext();){
			Node n = (Node) it.next();
			s += n.getNode()+" ";
		}
		s += "\n"; 
		s += "B-value: "+b+"\n";
		return s;
	}
	
	/**
	 * Returns the processor to which this node is scheduled to.
	 * 
	 * @return		the processor number
	 */
	public int getPNo() {
		return pNo;
	}

	/**
	 * Sets which processor this Node has been scheduled to.
	 * 
	 * @param pNo		the processor number
	 */
	public void setPNo(int pNo) {
		this.pNo = pNo;
	}

	/**
	 * Returns the finish time of this Node in the schedule.
	 * 
	 * @return			the finish time
	 */
	public int getFinishTime() {
		return finishTime;
	}

	/**
	 * Sets the finish time of this node in the schedule.
	 * 
	 * @param finishTime		the finish time
	 */
	public void setFinishTime(int finishTime) {
		this.finishTime = finishTime;
	}
	
	/**
	 * Returns whether all the predecessors of this node are scheduled to the same
	 * processor.
	 *   
	 * @return		whether the predecessors are scheduled to the same processor
	 */
	public boolean areAllParentsInSameProcessor() {
		allParentsInSameProcessor = true;
		
		if(parents.size() != 0){
			Node n = (Node)(parents.get(0));
			int no = n.getPNo();
				//Gets the processor this node is assigned to
		
			for(Iterator it = parents.iterator(); it.hasNext();){
				Node node = (Node) it.next();
				if(node.getPNo() != no){
					allParentsInSameProcessor = false;
					break;
				}
			}
		}
		Node n = (Node)(parents.get(0));
		parentsProcessor = n.getPNo();
		return allParentsInSameProcessor;
	}
	
	/**
	 * Returns the greatest finish time of all the predecessors of this node.
	 * 
	 * @return		the greatest finish time
	 */
	public int getParentsGreatestTime(){
		int time = 0;
		for(Iterator it = parents.iterator(); it.hasNext();){
			Node parent = (Node) it.next();
			time = Math.max(time,parent.getFinishTime()+ListScheduler.getTransitionWeight(parent,this));
		}
		return time;
	}
	
	
	/**
	 * Returns the greatest finish time of the predecessors of this node that are
	 * scheduled to the given processor.
	 * 
	 * @param processor		the processor for which the greatest predecessor
	 * 						time needs to be calculated
	 * @return				the greatest finish time of the predecessors scheduled
	 * 						to the given processor.
	 */
	public int getParentsGreatestTime(int processor){
		int time = 0;
		for(Iterator it = parents.iterator(); it.hasNext();){
			Node parent = (Node) it.next();
			if(parent.getPNo() != processor){
				time = Math.max(time,parent.getFinishTime()+ListScheduler.getTransitionWeight(parent,this));
			}
		}
		return time;
	}
	

	/**
	 * Returns the processor to which all the predecessors of this node has been
	 * scheduled to. 
	 * 
	 * @return		the processor to which the node's predecessors are scheduled
	 */
	public int getParentsProcessor() {
		return parentsProcessor;
	}

}