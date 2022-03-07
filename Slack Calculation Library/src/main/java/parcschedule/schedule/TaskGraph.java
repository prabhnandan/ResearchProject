package parcschedule.schedule;

import parcgraph.graph.DirectedAcyclicGraph;

import java.util.Iterator;

public interface TaskGraph<V extends TaskVertex, E extends CommEdge<V>> extends DirectedAcyclicGraph<V,E> {

	public double getCCR();

	public int sizeTasks();

	public V task(int index);

	public Iterator<V> tasksIterator();

	public Iterable<V> tasks();

	public int getBottomLevel(V v);

	public int getBottomLevelComp(V v);

	public int getTopLevel(V v);

	public int getTopLevelComp(V v);

	public int getTopLoad(V v);

	public int getBottomLoad(V v);

	public TaskVertex getMinWeightTask();

	public TaskVertex getMaxWeightTask();




	//add some methods
	public CommEdge<V> getMinWeightEdge();

	public CommEdge<V> getMaxWeightEdge();

	/**
	 * The critical path is the maximum of the static bottom level value of all tasks and edges.
	 *
	 * @return   the sum weight of the critical path 
	 * @throws Exception
	 */
	public int getCriticalPathValue();

	/**
	 * Returns the branching factor of graph. 
	 *
	 * @return the branching factor
	 * @throws Exception
	 */
	public int getBranchingFactor();

	/**
	 * Returns the balanced status of graph. 
	 *
	 * @return the balanced
	 * @throws Exception
	 */
	public boolean getBalanced();

	public int getTotalComputationCost();

	public int getMaxOutDegree();

	public Iterator<V> virtualParents(V v);

	public Iterator<V> virtualChildren(V v);

	public Iterable<CommEdge<V>> allVirtualEdges();
	public Iterator<CommEdge<V>> allVirtualEdgesIterator();

	public int virtualInDegree(V v);

	public int virtualOutDegree(V v);

	public boolean addVirtual(CommEdge<V> e);

	public boolean changeEdgeWeight(CommEdge<V> e, int weight);

	public int getCriticalPathLength();

	public int getCompCriticalPathLength();

	public boolean removeVirtual(CommEdge<V> e);

	public void calculateValues();
}
