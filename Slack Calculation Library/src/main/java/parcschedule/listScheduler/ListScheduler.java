package parcschedule.listScheduler;

import java.util.*;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

/**
 * A List scheduler that is based on a bottom-value heuristic. 
 */
public class ListScheduler {
	
	private static LinkedList nodes;	
	private static LinkedList transitions;
	private static LinkedList priorityQ;
	private static LinkedList topSorted;	
	private static LinkedList schedulingList;
	private static Processor[] processor;
	private static TaskGraph<TaskVertex, CommEdge<TaskVertex>> graph;
	
	/**
	 * Schedules a given task graph on the given number of processors using 
	 * the List scheduling algorithm. It is a greedy heuristic algorithm 
	 * and in this instance uses the bottom-level of the tasks as the heuristic.  
	 * Returns the schedule length of the complete schedule.
	 * 
	 * @param taskGraph			the task graph to be scheduled
	 * @param noOfProcessors	Number of processors available for scheduling
	 * @return					the schedule length
	 */
	public static int schedule(TaskGraph taskGraph, int noOfProcessors){ 
		nodes = new LinkedList();	
		transitions = new LinkedList();		
		priorityQ = new LinkedList();
		topSorted = new LinkedList();	
		schedulingList = new LinkedList();
		
		graph = taskGraph;
		processor = new Processor[noOfProcessors];
		readInput();
		//Initialising elements in processor array
		for(int i = 0; i<processor.length;i++){
			Processor p = new Processor(i);
			processor[i] = p;	
		}
				
		//Scheduling begins	
		while(schedulingList.size() != 0){
			Node node = (Node)schedulingList.removeFirst();
			if(node.getNoOfParents() == 0){
				
				int time = Integer.MAX_VALUE;
				int processorNo = -1;
				for(int i = 0; i<processor.length;i++){
					if(processor[i].getTime()<time){
						time = processor[i].getTime();
						

						processorNo = i;
					}
				}
				processor[processorNo].addNode(node,processor[processorNo].getTime());
				continue;
			}
			
			if(node.areAllParentsInSameProcessor()){	
				allParentsInSameProcessor(node);
			}
			else{
				allParentsNotInSameProcessor(node);
			}
		}
		
		//Schedule time is max of processor times.
		int scheduleTime = 0;
		for(int i = 0; i < processor.length; i++){
			scheduleTime = Math.max(scheduleTime,processor[i].getTime());
		}

		//writeOutput();
		return scheduleTime;
	}
	
	/**
	 * 
	 * @param node
	 */
	private static void allParentsInSameProcessor(Node node){
		for(int i = 0; i<processor.length;i++){
			processor[i].setMyTime(Math.max(processor[i].getTime(),node.getParentsGreatestTime()));
		}
		int refToProcessor = 0;
		int time = Integer.MAX_VALUE;
		for(int i = 0; i<processor.length;i++){
			if(processor[i].getMyTime() < time){
				time = processor[i].getMyTime();
				refToProcessor = i;		
			}	
		}
		if(time < processor[node.getParentsProcessor()].getTime()){
			processor[refToProcessor].addNode(node,processor[refToProcessor].getMyTime());
		}
		else{
			processor[node.getParentsProcessor()].addNode(node,processor[node.getParentsProcessor()].getTime());
		}		
	}
	
	/**
	 * 
	 * @param node
	 */
	private static void allParentsNotInSameProcessor(Node node){
		for(int i = 0; i<processor.length;i++){
			processor[i].setMyTime(Math.max(processor[i].getTime(),node.getParentsGreatestTime(i)));
		}
		int refToProcessor = 0;
		int time = Integer.MAX_VALUE;
		for(int i = 0; i<processor.length;i++){
			if(processor[i].getMyTime() < time){
				time = processor[i].getMyTime();
				refToProcessor = i;		
			}	
		}
		processor[refToProcessor].addNode(node,processor[refToProcessor].getMyTime());
	}
	
	/**
	 * Reads the given TaskGraph object and converts it into a set of objects
	 * that the listScheduler package can work on.
	 *
	 */
	private static void readInput() {
		Iterable<TaskVertex> allTasks;
		allTasks = graph.tasks();
		for(TaskVertex t : allTasks){		//Adding all nodes - from the taskGraph object.
			Node n = new Node(t.name(),t.weight());
			nodes.add(n);
		}
		
		Iterable<CommEdge<TaskVertex>> allEdges = graph.edges();
		for(CommEdge<TaskVertex> e : allEdges){		//Adding all transitions (edges)
			Transition trans = new Transition();
			for(Iterator i = nodes.iterator(); i.hasNext();){
				Node n = (Node) i.next();
				if(n.getNode().equals(e.from().name())){
					trans.setFrom(n);
				}
				if(n.getNode().equals(e.to().name())){
					trans.setTo(n);
				}
			}
			trans.setWeight(e.weight());
			transitions.add(trans);
		}
		
		for(Iterator it = transitions.iterator(); it.hasNext();){	//Setting the parents and children for all the node objects
			Transition t = (Transition) it.next();
			t.getTo().addParents(t.getFrom());
			t.getFrom().addChildren(t.getTo());
		}			
					
		processInput();
	}
	
	private static void processInput() {
		topSorted = topologicalSort();
		bAndSucc();		
		sortBValues();
	}
	
	/**
	 * 
	 * @return	the topologically sorted LinkedList
	 */
	private static LinkedList topologicalSort(){
		LinkedList l = new LinkedList();
		Node node = new Node();
	
		for(Iterator it = nodes.iterator(); it.hasNext();){
			Node n = (Node) it.next();
			if (n.getParents().size() == 0){
				priorityQ.add(n);
			}
		}
		while(priorityQ.size() != 0){
			int weight = 0;
			for(Iterator it = priorityQ.iterator(); it.hasNext();){
				Node n = (Node) it.next();
				if(n.getWeight() > weight){
					weight = n.getWeight();
					node = n;
				}
			}			
			priorityQ.remove(node);
			l.add(node);
			for(Iterator i = node.getChildren().iterator(); i.hasNext();){
				boolean allParentsInL = true;
				Node nc = (Node) i.next();
				for(Iterator j = nc.getParents().iterator(); j.hasNext();){
					Node np = (Node) j.next();
					if(!l.contains(np)){
						allParentsInL = false;
					}
				}
				if(allParentsInL){
					priorityQ.add(nc);
				}
			}
		}
		return l;
	}
	
	/**
	 * Calculates the bottom-level and the successor that is in the path of 
	 * the longest path from the node to an exit node.
	 *
	 */
	private static void bAndSucc(){
		int c = 0;
		for(int i = topSorted.size()-1; i>=0; i--){
			
			Node n = (Node)topSorted.get(i);

			for(Iterator j = n.getChildren().iterator();j.hasNext();){
				Node child = (Node)j.next();
				for(Iterator k = transitions.iterator(); k.hasNext();){
					Transition t = (Transition) k.next();
					if(t.getFrom().equals(n) && t.getTo().equals(child)){
						c = t.getWeight();
					}
				}
				if(n.getB() < n.getWeight()+c+child.getB()){
					n.setB(n.getWeight()+c+child.getB());
					n.setSucc(child);
				}
			}
		}
	}
	
	
	/**
	 * Sorts the nodes based on bottom-level values 
	 *
	 */
	private static void sortBValues(){
		LinkedList copyOfNodes = new LinkedList();
		copyOfNodes = (LinkedList)nodes.clone();
				
		int position = 0;
		while(copyOfNodes.size() != 0){
			int b = 0;			
			for(Iterator it = copyOfNodes.iterator(); it.hasNext();){
				Node n = (Node) it.next();
				if(n.getB() > b){
					b = n.getB();
					position = copyOfNodes.indexOf(n);
				}
			}
			schedulingList.add(copyOfNodes.remove(position));
		}
	}

	/**
	 * Returns the transition (edge) weight from the source (from) node to
	 * the destination (to) node
	 * 
	 * @param from	the source node (task) of the transition (edge)
	 * @param to	the destination node (task) of the transition (edge)
	 * @return		the transition weight
	 */
	public static int getTransitionWeight(Node from, Node to) {
		for(Iterator it = transitions.iterator(); it.hasNext();){
			Transition trans = (Transition) it.next();
			if(trans.getFrom().equals(from) && trans.getTo().equals(to)){
				return trans.getWeight();
			}
		}
		return -1;
	}
	
	/**
	 * Writes the schedule to a file
	 *
	 */
	/*private static void writeOutput(){
		for(Iterator it=nodes.iterator();it.hasNext();){
			Node node = (Node)it.next();
			System.out.println(node.getNode());
			System.out.println("Processor: "+node.getPNo());
			System.out.println("Start: "+(node.getFinishTime()-node.getWeight()));
			System.out.println("Finish: "+node.getFinishTime());
			System.out.println("===============");
		}
	}*/
}

