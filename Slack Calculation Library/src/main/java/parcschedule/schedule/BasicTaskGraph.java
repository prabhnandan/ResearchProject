package parcschedule.schedule;

import parcgraph.graph.BasicDirectedAcyclicGraph;
import parcgraph.graph.IteratorToIterableWrapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BasicTaskGraph<V extends TaskVertex, E extends CommEdge<V>> extends BasicDirectedAcyclicGraph<V, E> implements
		TaskGraph<V, E> {

	private int[] bottomLevel;
	private int[] bottomLevelComp;
	private int[] topLevel;
	private int[] topLevelComp;

	private int[] topLoad;
	private int[] bottomLoad;

	private List<CommEdge<V>> virtualEdges;
	private List<V>[] virtualParents;
	private List<V>[] virtualChildren;

	private boolean isBottomLevelUpdated;
	private boolean isBottomLevelCompUpdated;
	private boolean isTopLevelUpdated;
	private boolean isTopLevelCompUpdated;
	private boolean isTopLoadUpdated;
	private boolean isBottomLoadUpdated;

	public BasicTaskGraph(String name, int size) {
		super(name);
		this.virtualEdges = new ArrayList<CommEdge<V>>();
		this.virtualChildren = new ArrayList[size];
		this.virtualParents = new ArrayList[size];
		bottomLevel = new int[size];
		bottomLevelComp = new int[size];
		topLevel = new int[size];
		topLevelComp = new int[size];
		topLoad = new int[size];
		bottomLoad = new int[size];
		for (int i = 0; i < size; i++) {
			this.virtualParents[i] = new ArrayList<V>();
			this.virtualChildren[i] = new ArrayList<V>();
		}
		resetUpdateStatus();
	}

	/*private int evaluateBottomLevel(V v){
		int max = 0;
		for (Iterator<V> children = this.childrenIterator(v); children.hasNext();) {
			V child=children.next();
			max = Math.max(max, evaluateBottomLevel(child)+
					this.edgeBetween(v, child).weight());
		}
		return max + v.weight();
	}

	private int evaluateBottomLevelComp(V v){
		int max = 0;
		for (Iterator<V> children = this.childrenIterator((V)v); children.hasNext();) {
			V child=children.next();
			max = Math.max(max, evaluateBottomLevelComp(child));
		}
		return max +v.weight();
	}

	private int evaluateTopLevel(V v){
		int max = 0;
		for (Iterator<V> parents =this.parentsIterator(v); parents.hasNext();) {
			V parent=parents.next();
			max = Math.max(max, evaluateTopLevel(parent)+parent.weight());
		}

		return max;
	}

	private int evaluateTopLevelComp(V v){
		int max = 0;

		for (Iterator<V> parents =this.parentsIterator((V)v); parents.hasNext();) {
			V parent=parents.next();
			max = Math.max(max, evaluateTopLevelComp(parent)+parent.weight());
		}
		return max;
	}*/

	public int getBottomLevel(V v) {
		if (!isBottomLevelUpdated) {
			updateBottomLevel();
		}
		return bottomLevel[v.index()];
	}


	// what does "getBottomLevelComp" mean?
	public int getBottomLevelComp(V v) {
		if (!isBottomLevelCompUpdated) {
			updateBottomLevelComp();
		}
		return bottomLevelComp[v.index()];
	}

	public double getCCR() {
		Iterator<V> wrapped_vertices= this.verticesIterator();
		Iterator<E> wrapped_edges=this.edgesIterator();
		double SumOfComputations=0;
		double SumOfCommunications=0;
		double ccr=-1;
		while(wrapped_vertices.hasNext())
			SumOfComputations+=wrapped_vertices.next().weight();
		while(wrapped_edges.hasNext())
			SumOfCommunications+=wrapped_edges.next().weight();
		if(SumOfCommunications!=0)
		{
			ccr=SumOfCommunications/SumOfComputations;
		}

		return ccr;
	}

	public TaskVertex getMaxWeightTask() {
		Iterator<V> wrapped_vertices= this.verticesIterator();
		V temp1=null;
		int max=0;
		while(wrapped_vertices.hasNext())
		{
			V temp=wrapped_vertices.next();
			if(max<temp.weight())
			{
				temp1=temp;
				max=temp.weight();
			}

		}
		return temp1;
	}

	public TaskVertex getMinWeightTask() {
		Iterator<V> wrapped_vertices= this.verticesIterator();
		V temp=null;
		int min=Integer.MAX_VALUE;
		while(wrapped_vertices.hasNext())
		{
			V temp1=wrapped_vertices.next();

			//System.out.println(wrapped_vertices.next().name());
			if(temp1.weight()<min)
			{
				temp=temp1;
				min=temp1.weight();
			}

		}
		return temp;

	}






	public int getTopLevel(V v) {
		if (!isTopLevelUpdated) {
			updateTopLevel();
		}
		return topLevel[v.index()];
	}



	public int getTopLevelComp(V v) {

		if (!isTopLevelCompUpdated) {
			updateTopLevelComp();
		}
		return topLevelComp[v.index()];

	}

	public int getTopLoad(V v){
		if(!isTopLoadUpdated){
			updateTopLoad();
		}
		return topLoad[v.index()];
	}

	public int getBottomLoad(V v){
		if(!isBottomLoadUpdated){
			updateBottomLoad();
		}
		return bottomLoad[v.index()];
	}

	private void updateBottomLevel() {

		for (V task : inverseTopologicalOrder())
		{
			int bLevel = 0; // except minus task weight
			for (E outEdge : outEdges(task))
			{
				bLevel = Math.max(bLevel, bottomLevel[outEdge.to().index()] + outEdge.weight());
			}
			bottomLevel[task.index()] = bLevel + task.weight();
		}

		isBottomLevelUpdated = true;
	}

	private void updateBottomLevelComp() {

		for (V task : inverseTopologicalOrder())
		{
			int bLevel = 0; // except minus task weight
			for (V child : children(task))
			{
				bLevel = Math.max(bLevel, bottomLevelComp[child.index()]);
			}
			bottomLevelComp[task.index()] = bLevel + task.weight();
		}

		isBottomLevelCompUpdated = true;
	}

	private void updateTopLevelComp() {

		for (V task : topologicalOrder())
		{

			int tLevel = 0;
			for (V parent : parents(task))
			{
				tLevel = Math.max(tLevel, topLevelComp[parent.index()] + parent.weight());
			}
			topLevelComp[task.index()] = tLevel;
		}

		isTopLevelCompUpdated = true;
	}

	private void updateTopLevel() {

		for (V task : topologicalOrder())
		{
			int tLevel = 0;
			for (E inEdge : inEdges(task))
			{
				V parent = inEdge.from();
				tLevel = Math.max(tLevel, topLevel[parent.index()] + parent.weight() + inEdge.weight());
			}
			topLevel[task.index()] = tLevel;
		}

		isTopLevelUpdated = true;
	}

	private void updateTopLoad(){

		for(V task : topologicalOrder()){
			int load = 0;
			for(V ancestor : ancestors(task)){
				load += ancestor.weight();
			}
			this.topLoad[task.index()] = load;
		}

		isTopLoadUpdated = true;
	}

	private void updateBottomLoad(){
		for(V task : topologicalOrder()){
			int load = 0;
			for(V descendant: descendents(task)){
				load += descendant.weight();
			}
			this.bottomLoad[task.index()] = load;
		}

		isBottomLoadUpdated = true;
	}

	public int[] getBottomLevel() {
		if (!isBottomLevelUpdated) {
			updateBottomLevel();
		}
		return bottomLevel;
	}

	public int[] getBottomLevelComp() {
		if (!isBottomLevelCompUpdated) {
			updateBottomLevelComp();
		}
		return bottomLevelComp;
	}

	public int[] getTopLevel() {
		if (!isTopLevelUpdated) {
			updateTopLevel();
		}
		return topLevel;
	}

	public int[] getTopLevelComp() {
		if (!isTopLevelCompUpdated) {
			updateTopLevelComp();
		}
		return topLevelComp;
	}

	public int[] getTopLoad(){
		if(!isTopLoadUpdated){
			updateTopLoad();
		}
		return topLoad;
	}

	public int[] getBottomLoad(){
		if(!isBottomLoadUpdated){
			updateBottomLoad();
		}
		return bottomLoad;
	}


	/**
	 * Reads a graph from an GXLReader
	 * @param input a GXLReader that has been initialised with the file that the 
	 * graph to be loaded has been saved to.
	 * @return a graph of the type that was saved, containing all the nodes and 
	 * vertices.
	 */
	//public Graph doLoad(GXLReader input) {
	//		return null;
	//	}

	/**
	 * Saves this graph to a GXLWriter
	 * @param output a GXLWriter that has been initialised to send the gxl output to the right place.
	 */
	//public void doSave(GXLWriter writer) throws IOException {
//	}

	/**
	 * Returns the description of the graph as an xml top level node.
	 * The format of the string is as follows:
	 * &lt;graph id="NAMEHERE" edgeids="false" edgemode="directed | undirected"
	 *  hypergraph="true|false"&gt;
	 *
	 *  The corresponding &lt;/graph&gt; tag need not be supplied.
	 * @return
	 */
	public String toXML() {

		return null;

	}

	public CommEdge<V> getMinWeightEdge() {

		Iterator<E> wrapped_edges= this.edgesIterator();
		E temp=null;
		int min=1000000;
		while(wrapped_edges.hasNext())
		{
			E temp1=wrapped_edges.next();

			//System.out.println(wrapped_vertices.next().name());
			if(temp1.weight()<min)
			{
				temp=temp1;
				min=temp1.weight();
			}


		}
		return temp;

	}

	public CommEdge<V> getMaxWeightEdge() {
		Iterator<E> wrapped_edges= this.edgesIterator();
		E temp=null;
		int max=0;
		while(wrapped_edges.hasNext())
		{
			E temp1=wrapped_edges.next();

			//System.out.println(wrapped_vertices.next().name());
			if(temp1.weight()>max)
			{
				temp=temp1;
				max=temp1.weight();
			}


		}
		return temp;

	}

	/*	public int getCriticalPathValue(){

		Iterator<V> wrapped_vertices= this.verticesIterator();
		V VertexWithMaxBottomLevel=null;
		int MaxBottomLevel=0;
		while(wrapped_vertices.hasNext())	
		{  V temp1=wrapped_vertices.next();
			if(MaxBottomLevel<getBottomLevel(temp1))
			{
				VertexWithMaxBottomLevel=temp1;
				MaxBottomLevel=getBottomLevel(temp1);
			}
		}
		return MaxBottomLevel;
	}*/

	public int getCriticalPathValue(){

		Iterator<V> wrapped_vertices= this.verticesIterator();
		V VertexWithMaxBottomLevel=null;
		int MaxBottomLevel=0;
		while(wrapped_vertices.hasNext())
		{  V temp1=wrapped_vertices.next();
			if(MaxBottomLevel<getBottomLevelComp(temp1))
			{
				VertexWithMaxBottomLevel=temp1;
				MaxBottomLevel=getBottomLevelComp(temp1);
			}
		}
		return MaxBottomLevel;
	}

	public int getBranchingFactor(){
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("This function has not been implemented");
	}

	public boolean getBalanced(){
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("This function has not been implemented");

	}

	public int getTotalComputationCost() {
		int totalComp = 0;
		for(V v : vertices()){
			totalComp += v.weight();
		}
		return totalComp;
	}

	public int getMaxOutDegree() {
		int maxOutDegree = 0;
		for(V v : vertices()){
			int outDegree = this.outDegree(v);
			if(outDegree > maxOutDegree){
				maxOutDegree = outDegree;
			}
		}
		return maxOutDegree;
	}

	public int sizeTasks() {
		return this.sizeVertices();
	}

	public Iterator<V> virtualParents(V v) {
		return virtualParents[v.index()].iterator();
	}

	public Iterator<V> virtualChildren(V v) {
		return virtualChildren[v.index()].iterator();
	}

	public Iterator<CommEdge<V>> allVirtualEdgesIterator() {
		return virtualEdges.iterator();
	}

	public Iterable<CommEdge<V>> allVirtualEdges() {
		return new IteratorToIterableWrapper<CommEdge<V>>(virtualEdges.iterator());
	}

	public int virtualInDegree(V v) {
		return virtualParents[v.index()].size();
	}

	public int virtualOutDegree(V v) {
		return virtualChildren[v.index()].size();
	}

	public boolean addVirtual(CommEdge<V> e) {
		if (e != null
				&& !this.containsCongruent(e)
				&& this.contains(e.from())
				&& this.contains(e.to())
				&& this.edgeForName(e.name()) == null
				&& e.isDirected()) {
			virtualEdges.add(e);
			virtualParents[e.to().index()].add(e.from());
			virtualChildren[e.from().index()].add(e.to());
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean add(V v){
		boolean isAdded = super.add(v);
		if (isAdded) {
			resetUpdateStatus();
			// calculateValues();
			// recalculating values with every added node is going to take too much time, the values will be updated when they're requested anyway
		}
		return isAdded;
	}

	@Override
	public boolean add(E e){
		boolean isAdded = super.add(e);
		if (isAdded) {
			resetUpdateStatus();
			// calculateValues();
		}
		return isAdded;
	}

	private void resetUpdateStatus(){
		isBottomLevelCompUpdated = false;
		isBottomLevelUpdated = false;
		isTopLevelCompUpdated = false;
		isTopLevelCompUpdated = false;
	}

	public boolean changeEdgeWeight(CommEdge<V> e, int weight) {
		if(e.weight()>-1){
			e.setWeight(weight);
			resetUpdateStatus();
			calculateValues();
			return true;
		}
		return false;
	}

	public int getCriticalPathLength() {
		if (!isBottomLevelUpdated)
		{
			updateBottomLevel();
		}
		int cpLength = Integer.MIN_VALUE;
		for(int i=0; i < bottomLevel.length; i++){
			cpLength = Math.max(cpLength, bottomLevel[i]);
		}
		return cpLength;
	}

	public int getCompCriticalPathLength() {
		int cpLength = Integer.MIN_VALUE;
		for(int i=0; i < bottomLevelComp.length; i++){
			cpLength = Math.max(cpLength, bottomLevelComp[i]);
		}
		return cpLength;
	}

	public boolean removeVirtual(CommEdge<V> e) {
		if (virtualEdges.contains(e)){
			virtualEdges.remove(e);
			virtualParents[e.to().index()].remove(e.from());
			virtualChildren[e.from().index()].remove(e.to());
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean remove(E e){
		boolean isRemoved = super.remove(e);
		if (isRemoved) {
			resetUpdateStatus();
		}
		return isRemoved;
	}

	public Iterator<V> tasksIterator() {
		return this.verticesIterator();
	}

	public Iterable<V> tasks() {
		return this.vertices();
	}

	public V task(int index) {
		for (V v : this.tasks()) {
			if (v.index() == index){
				return v;
			}
		}
		return null;
	}

	public void calculateValues(){
		updateBottomLevel();
		updateBottomLevelComp();
		updateTopLevel();
		updateTopLevelComp();
	}

}
