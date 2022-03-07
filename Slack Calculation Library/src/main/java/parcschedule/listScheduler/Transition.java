package parcschedule.listScheduler;

/**
 * Represents an edge (dependency) between tasks.
 */
public class Transition {
	
	private Node from;
	private Node to;
	private int weight;
	
	/**
	 * Initialises a Transition (edge) object.
	 * 
	 * @param from		the source Node object where this transition begins
	 * @param to		the destination Node object where this transition ends
	 * @param weight	the communication cost (weight) of this transition
	 */
	public Transition(Node from, Node to, int weight) {
		this.from = from;
		this.to = to;
		this.weight = weight;
	}
	
	/**
	 * Initialises a default Transition object.
	 * from and to Node objects are created using default Node constructor.
	 * Weight is set to -1.
	 *
	 */
	public Transition() {
		this(new Node(), new Node(), -1);
	}
	
	/**
	 * Returns the source Node object where this transition begins.
	 * 
	 * @return		the Node object where this transition begins
	 */
	public Node getFrom() {
		return from;
	}

	/**
	 * Returns the destination Node object where this transition ends.
	 * 
	 * @return		the Node object where this transition ends
	 */
	public Node getTo() {
		return to;
	}

	/**
	 * Returns the weight of this Transition object.
	 * The weight represents the communication cost between the 
	 * source and destination Node objects that this transition object links. 
	 * 
	 * @return		the weight
	 */
	public int getWeight() {
		return weight;
	}

	/**
	 * Sets the source Node object where this transition begins from.
	 * 
	 * @param sourceNode		the source Node object 
	 */
	public void setFrom(Node sourceNode) {
		from = sourceNode;
	}

	/**
	 * Sets the destination Node object where this transition ends.
	 * 
	 * @param destinationNode		the destination Node object 
	 */
	public void setTo(Node destinationNode) {
		to = destinationNode;
	}

	/**
	 * Sets the weight of this Transition (edge).
	 * 
	 * @param weight		the weight
	 */
	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	/**
	 * Returns the sum of weights of this transition and its source node.
	 * 
	 * @return		the sum of weights of this transition and its source node
	 */
	public int getTotalCost(){
		int total = from.getWeight()+weight;
		return total;
	}
}
