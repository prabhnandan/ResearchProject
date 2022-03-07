package parcschedule.graphGenerator;


import parcgraph.graph.DirectedEdge;
import parcgraph.graph.Utils;
import parcschedule.graphGenerator.weightGenerators.ConstantValueGenerator;
import parcschedule.graphGenerator.weightGenerators.WeightGenerator;
import parcschedule.schedule.BasicCommEdge;
import parcschedule.schedule.BasicTaskGraph;
import parcschedule.schedule.BasicTaskVertex;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class provides utility methods to generate various task graph structures.
 */
public class DagGenerator {

	private static int noOfNodes;
	// private static int[] nodeWeights;
	private static int noOfEdges;
	// private static int[] edgeWeights;
	private static double density;
	private static double ccr;
	// private static String weightType;
	
	//For In-tree/Out-tree
	private static int maxBranchingFactor;	//Also for SP
	private static int depth;
	private static boolean isBalanced;

	public static void convertRandomToUnitWeight(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph){
		double originalCCR = taskGraph.getCCR();

		int totalComp = taskGraph.getTotalComputationCost();

		int totalComm = 0;
		for(CommEdge<TaskVertex> e : taskGraph.edges()){
			totalComm += e.weight();
		}

		int newTaskWeight = totalComp / taskGraph.sizeTasks();
		int newEdgeWeight = totalComm / taskGraph.sizeEdges();

		for(TaskVertex t : taskGraph.tasks()){
			t.setWeight(newTaskWeight);
		}
		for(CommEdge<TaskVertex> e : taskGraph.edges()){
			e.setWeight(newEdgeWeight);
		}

		double newCCR = taskGraph.getCCR();
		System.out.println(originalCCR+":"+newCCR);
	}

	/**
	 * Generates an independent (no edges present) DAG (task graph) with the
	 * specified properties.
	 * 
	 * @param noOfNodesRequired
	 *            the number of nodes required in the DAG
	 * @param weightTypeRequired
	 *            the distribution of node and edge weights. Options: Unit and
	 *            Random
	 */
	public static void generateIndependent(int noOfNodesRequired, WeightGenerator nodeWeightGenerator) {
		DagGenerator.noOfNodes = noOfNodesRequired;
		DagGenerator.noOfEdges = 0;
		DagGenerator.density = 0;
		DagGenerator.ccr = 0;
		
		String pathName = "DAGS/Independent";
		pathName += "/Nodes " + noOfNodes;
		pathName += "/" + nodeWeightGenerator.name();
		
		String graphName = "Independent";
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_WeightType_" + nodeWeightGenerator.name();
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, DagGenerator.noOfNodes);
		taskGraph.setName(graphName);
		
		for (int i = 0; i < DagGenerator.noOfNodes; i++) {
			TaskVertex task = new BasicTaskVertex("" + i, i);
			taskGraph.add(task);
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates a Fork DAG with the specified properties.
	 * 
	 * @param noOfNodesRequired
	 *            the number of nodes required in the DAG
	 * @param ccrRequired
	 *            the CCR required for the DAG
	 * @param weightTypeRequired
	 *            the distribution of node and edge weights. Options: Unit and
	 *            Random
	 */
	public static void generateFork(int noOfNodesRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, String identifier) {
		DagGenerator.noOfNodes = noOfNodesRequired;
		DagGenerator.noOfEdges = DagGenerator.noOfNodes - 1;
		DagGenerator.density = (noOfEdges * 1.0) / noOfNodes;
		DagGenerator.ccr = ccrRequired;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Fork";
		pathName += "/Nodes " + noOfNodes;
		pathName += "/CCR " + ccrRequired;	//Coz folder names are static. Hence use ccrRequired
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "Fork";
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);		//Use actual CCR
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		graphName += "_" + identifier;
		
		pathName += "/" + graphName + ".gxl";
		
		//TaskGraph taskGraph = new TaskGraphImpl(graphName, noOfNodes);
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
		
		TaskVertex source = new BasicTaskVertex(0);
		taskGraph.add(source);
		for (int i = 1; i < noOfNodesRequired; i++)
		{
			TaskVertex task = new BasicTaskVertex(i);
			taskGraph.add(task);
			taskGraph.add(new BasicCommEdge<TaskVertex>("0-"+task.index(), source, task, true));
		}
		
		if(!validateOutTree(taskGraph)){	//Fork is an Out Tree with depth 1
			throw new RuntimeException("Invalid Fork graph generated!");
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates a Join DAG with the specified properties.
	 * 
	 * @param noOfNodesRequired
	 * @param ccrRequired
	 * @param weightTypeRequired
	 */
	public static void generateJoin(int noOfNodesRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, String identifier) {
		DagGenerator.noOfNodes = noOfNodesRequired;
		DagGenerator.noOfEdges = noOfNodes - 1;
		DagGenerator.density = (noOfEdges * 1.0) / noOfNodes;
		DagGenerator.ccr = ccrRequired;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Join";
		pathName += "/Nodes " + noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "Join";
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		graphName += "_" + identifier;
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
		
		int last = noOfNodesRequired - 1;
		TaskVertex sink = new BasicTaskVertex(last);
		taskGraph.add(sink);
		for (int i = 0; i < last; i++)
		{
			TaskVertex task = new BasicTaskVertex(i);
			taskGraph.add(task);
			taskGraph.add(new BasicCommEdge<TaskVertex>(task.index()+"-"+last, task, sink, true));
		}
		
		if(!validateInTree(taskGraph)){		//Join is an In Tree with depth 1
			throw new RuntimeException("Invalid Join graph generated!");
		}

		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates a Fork-join DAG with the specified properties.
	 * 
	 * @param noOfNodesRequired
	 * @param ccrRequired
	 * @param weightTypeRequired
	 */
	public static void generateForkJoin(int noOfNodesRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator) {
		DagGenerator.noOfNodes = noOfNodesRequired;
		DagGenerator.noOfEdges = (noOfNodes - 2) * 2;
		DagGenerator.density = (noOfEdges * 1.0) / noOfNodes;
		DagGenerator.ccr = ccrRequired;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Fork Join";
		pathName += "/Nodes " + noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "Fork_Join";
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
		taskGraph.setName(graphName);
		
		for (int i = 0; i < noOfNodes; i++) {		//Setting all tasks
			TaskVertex task = new BasicTaskVertex("" + i, i);
			taskGraph.add(task);
		}
		
		TaskVertex[] tasks = (TaskVertex[]) Utils.iterableToArray(taskGraph.vertices(), new TaskVertex[taskGraph.sizeVertices()]);
		for (int i = 0; i < noOfEdges; i++) {			//Setting all edges
			CommEdge<TaskVertex> edge; 
			if (i < (noOfEdges / 2)) {	//Fork edges
				edge = new BasicCommEdge<TaskVertex>(tasks[0].name()+"-"+tasks[i+1].name(), tasks[0], tasks[i+1], true);
			} else {	//Join edges
				edge = new BasicCommEdge<TaskVertex>(tasks[i - (noOfEdges / 2) + 1].name()+"-"+tasks[noOfNodes - 1].name(), tasks[i - (noOfEdges / 2) + 1], tasks[noOfNodes - 1], true);
			}
			
			taskGraph.add(edge);
		}
		
		if(!validateForkJoin(taskGraph)){
			throw new RuntimeException("Invalid FOrk Join graph generated!");
		}

		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates an in tree by the number of nodes and with the specified properties.
	 * 
	 * @param noOfNodesRequired
	 * @param ccrRequired
	 * @param weightTypeRequired
	 * @param maxBfRequired
	 * @param isBalanced
	 */
	public static void generateInTreeByNodes(int noOfNodesRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, int maxBfRequired, boolean isBalanced) {
		DagGenerator.maxBranchingFactor = maxBfRequired;	//If 1 we have a chain
		DagGenerator.isBalanced = isBalanced;
		DagGenerator.ccr = ccrRequired;
		
		DagGenerator.noOfNodes = noOfNodesRequired;		//At least 2 to make a chain atleast
		DagGenerator.noOfEdges = noOfNodes - 1;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/In Tree";
		pathName += "/Nodes " + DagGenerator.noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "InTree";
		if(DagGenerator.isBalanced){
			graphName += "-Balanced";
		}
		else{
			graphName += "-Unbalanced";
		}
		graphName += "-MaxBf-" + DagGenerator.maxBranchingFactor;
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
		taskGraph.setName(graphName);
		
		for(int i=0; i<noOfNodes; i++){		//Setting all tasks
			TaskVertex task = new BasicTaskVertex("" + i, i);
			taskGraph.add(task);
		}
		
		TaskVertex[] tasks = (TaskVertex[]) Utils.iterableToArray(taskGraph.vertices(), new TaskVertex[taskGraph.sizeVertices()]);
		if(DagGenerator.isBalanced){	//MaxBF
			int edgeCounter = 0;
			for (int i = 0; i < noOfNodes; i++) {			
				for(int j=(i*maxBranchingFactor+1); j<(i*maxBranchingFactor+1+maxBranchingFactor); j++){
					if (edgeCounter < noOfEdges) {
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(tasks[j].name()+"-"+tasks[i].name(), tasks[j], tasks[i], true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		else{ //Varying BF
			int bf = 0;
			int edgeCounter = 0;
			
			for (int i = 0; i < noOfNodes; i++) {
				if(i == 0){
					bf = getRandomNumber(1, maxBranchingFactor);	//Atleast 1
				}
				else{
					bf = getRandomNumber(0, maxBranchingFactor);
				}
				for (int j = 0; j < bf; j++) {
					if(edgeCounter < noOfEdges){
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(tasks[edgeCounter+1].name()+"-"+tasks[i].name(), tasks[edgeCounter+1], tasks[i], true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		
		if(!validateInTree(taskGraph)){
			throw new RuntimeException("Invalid In-tree generated!");
		}

		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
				
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates an in tree by depth and with the specified properties.
	 * 
	 * @param depthRequired
	 * @param ccrRequired
	 * @param weightTypeRequired
	 * @param maxBfRequired
	 * @param isBalanced
	 */
	public static void generateInTreeByDepth(int depthRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, int maxBfRequired, boolean isBalanced) {
		DagGenerator.maxBranchingFactor = maxBfRequired;	//If 1 we have a chain
		DagGenerator.isBalanced = isBalanced;
		DagGenerator.ccr = ccrRequired;
		
		DagGenerator.depth = depthRequired;		//If 0, we get independent graphs, 1 we get forks or joins
		
		TaskGraph taskGraph = null;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/In Tree";
		pathName += "/Depth " + depth;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "InTree";
		if(DagGenerator.isBalanced){
			graphName += "-Balanced";
		}
		else{
			graphName += "-Unbalanced";
		}
		graphName += "-MaxBf-" + maxBranchingFactor;
		graphName += "_Depth_" + depth;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		pathName += "/" + graphName + ".gxl";
		
		if(DagGenerator.isBalanced){	//MaxBF
			DagGenerator.noOfNodes = getMaxNoOfNodes(maxBranchingFactor, depth);
			DagGenerator.noOfEdges = noOfNodes - 1;
			taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);

			
			for(int i=0; i<noOfNodes; i++){		//Setting all tasks
				TaskVertex task = new BasicTaskVertex(""+i,i);
				taskGraph.add(task);
			}
			
			Iterable<TaskVertex> tasks = taskGraph.tasks();
			int edgeCounter = 0;
			for(int i=0; i<noOfNodes; i++){			
				for(int j=(i*maxBranchingFactor+1); j<(i*maxBranchingFactor+1+maxBranchingFactor); j++){
					if (edgeCounter < noOfEdges) {
						TaskVertex primus = taskGraph.task(j);
						TaskVertex secundus = taskGraph.task(i);
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(primus.name()+"-"+secundus.name(), taskGraph.task(j), taskGraph.task(i), true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		else{ //Varying BF
			DagGenerator.noOfNodes = getRandomNumber(getMaxNoOfNodes(maxBranchingFactor, (depth-1))+1, getMaxNoOfNodes(maxBranchingFactor, depth));
			DagGenerator.noOfEdges = DagGenerator.noOfNodes - 1;
			taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
			
			for(int i=0; i<noOfNodes; i++){		//Setting all tasks
				TaskVertex task = new BasicTaskVertex("" + i, i);
				taskGraph.add(task);
			}
			
			int bf = 0;
			int edgeCounter = 0;
			
			
			for (int i = 0; i < noOfNodes; i++) {
				if(i == 0){
					bf = getRandomNumber(1, maxBranchingFactor);
				}
				else{
					bf = getRandomNumber(0, maxBranchingFactor);
				}
				for (int j = 0; j < bf; j++) {
					if(edgeCounter < noOfEdges){
						TaskVertex primus = taskGraph.task(edgeCounter+1);
						TaskVertex secundus = taskGraph.task(i);
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(primus.name()+"-"+secundus.name(), taskGraph.task(j), taskGraph.task(i), true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
				
		if(!validateInTree(taskGraph)){
			throw new RuntimeException("Invalid InTree generated!");
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.setName(graphName);
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates an out tree by nodes and with the specified properties.
	 * 
	 * @param noOfNodesRequired
	 * @param ccrRequired
	 * @param weightTypeRequired
	 * @param maxBfRequired
	 * @param isBalanced
	 */
	public static void generateOutTreeByNodes(int noOfNodesRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, int maxBfRequired, boolean isBalanced) {
		// this segment of code is copied from generateInTrees, the generated graph is reversed to produce the outtree
		DagGenerator.maxBranchingFactor = maxBfRequired;	//If 1 we have a chain
		DagGenerator.isBalanced = isBalanced;
		DagGenerator.ccr = ccrRequired;
		
		DagGenerator.noOfNodes = noOfNodesRequired;		//At least 2 to make a chain atleast
		DagGenerator.noOfEdges = noOfNodes - 1;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Out Tree";
		pathName += "/Nodes " + DagGenerator.noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "OutTree";
		if(DagGenerator.isBalanced){
			graphName += "-Balanced";
		}
		else{
			graphName += "-Unbalanced";
		}
		graphName += "-MaxBf-" + DagGenerator.maxBranchingFactor;
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
		taskGraph.setName(graphName);
		
		for(int i=0; i<noOfNodes; i++){		//Setting all tasks
			TaskVertex task = new BasicTaskVertex("" + i, i);
			taskGraph.add(task);
		}
		
		TaskVertex[] tasks = (TaskVertex[]) Utils.iterableToArray(taskGraph.vertices(), new TaskVertex[taskGraph.sizeVertices()]);
		if(DagGenerator.isBalanced){	//MaxBF
			int edgeCounter = 0;
			for (int i = 0; i < noOfNodes; i++) {			
				for(int j=(i*maxBranchingFactor+1); j<(i*maxBranchingFactor+1+maxBranchingFactor); j++){
					if (edgeCounter < noOfEdges) {
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(tasks[j].name()+"-"+tasks[i].name(), tasks[j], tasks[i], true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		else{ //Varying BF
			int bf = 0;
			int edgeCounter = 0;
			
			for (int i = 0; i < noOfNodes; i++) {
				if(i == 0){
					bf = getRandomNumber(1, maxBranchingFactor);	//Atleast 1
				}
				else{
					bf = getRandomNumber(0, maxBranchingFactor);
				}
				for (int j = 0; j < bf; j++) {
					if(edgeCounter < noOfEdges){
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(tasks[edgeCounter+1].name()+"-"+tasks[i].name(), tasks[edgeCounter+1], tasks[i], true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		
		reverse(taskGraph);
		
		if(!validateOutTree(taskGraph)){
			throw new RuntimeException("Invalid Out-tree generated!");
		}

		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
				
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates an out tree by depth and with the specified properties.
	 *  
	 * @param depthRequired
	 * @param ccrRequired
	 * @param weightTypeRequired
	 * @param maxBfRequired
	 * @param isBalanced
	 */
	public static void generateOutTreeByDepth(int depthRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, int maxBfRequired, boolean isBalanced) {
		DagGenerator.maxBranchingFactor = maxBfRequired;	//If 1 we have a chain
		DagGenerator.isBalanced = isBalanced;
		DagGenerator.ccr = ccrRequired;
		
		DagGenerator.depth = depthRequired;		//If 0, we get independent graphs, 1 we get forks or joins
		
		DagGenerator.noOfNodes = getRandomNumber(getMaxNoOfNodes(maxBranchingFactor, (depth-1))+1, getMaxNoOfNodes(maxBranchingFactor, depth));
		DagGenerator.noOfEdges = DagGenerator.noOfNodes - 1;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Out Tree";
		pathName += "/Depth " + depth;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "OutTree";
		if(DagGenerator.isBalanced){
			graphName += "-Balanced";
		}
		else{
			graphName += "-Unbalanced";
		}
		graphName += "-MaxBf-" + maxBranchingFactor;
		graphName += "_Depth_" + depth;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph taskGraph = null;
		
		if(DagGenerator.isBalanced){	//MaxBF
			DagGenerator.noOfNodes = getMaxNoOfNodes(maxBranchingFactor, depth);
			DagGenerator.noOfEdges = noOfNodes-1;
			taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);

			
			for(int i=0; i<noOfNodes; i++){		//Setting all tasks
				TaskVertex task = new BasicTaskVertex(""+i,i);
				taskGraph.add(task);
			}
			
			int edgeCounter = 0;
			for(int i=0; i<noOfNodes; i++){			
				for(int j=(i*maxBranchingFactor+1); j<(i*maxBranchingFactor+1+maxBranchingFactor); j++){
					if (edgeCounter < noOfEdges) {
						TaskVertex primus = taskGraph.task(i);
						TaskVertex secundus = taskGraph.task(j);
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(primus.name()+"-"+secundus.name(), taskGraph.task(j), taskGraph.task(i), true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		else{ //Varying BF
			
			taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
			
			for(int i=0; i<noOfNodes; i++){		//Setting all tasks
				TaskVertex task = new BasicTaskVertex(""+i,i);
				taskGraph.add(task);
			}
			
			int bf = 0;
			int edgeCounter = 0;
			
			
			for(int i=0; i<noOfNodes; i++){
				if(i == 0){
					bf = getRandomNumber(1, maxBranchingFactor);
				}
				else{
					bf = getRandomNumber(0, maxBranchingFactor);
				}
				for(int j=0; j<bf; j++){
					if(edgeCounter < noOfEdges){
						TaskVertex primus = taskGraph.task(i);
						TaskVertex secundus = taskGraph.task(edgeCounter+1);
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(primus.name()+"-"+secundus.name(), taskGraph.task(j), taskGraph.task(i), true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		
		
		
		if(!validateOutTree(taskGraph)){
			throw new RuntimeException("Invalid out tree generated!");
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.setName(graphName);
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates a random DAG with the specified properties. (by matrix)
	 * 
	 * @param noOfNodesRequired
	 * @param densityRequired
	 * @param ccrRequired
	 * @param weightTypeRequired
	 */
	public static void generateRandom(int noOfNodesRequired, double densityRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, String identifier) {
		DagGenerator.noOfNodes = noOfNodesRequired;
		DagGenerator.density = densityRequired;
		DagGenerator.noOfEdges = Math.min((int)Math.ceil(noOfNodes*density), (noOfNodes*(noOfNodes-1)/2));
		DagGenerator.ccr = ccrRequired;
		
		
		TaskGraph taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>("newGraph", noOfNodes);
		
		for(int i=0; i<noOfNodes; i++){		//Setting all tasks with unit weights
			TaskVertex task = new BasicTaskVertex("" + i, i);
			taskGraph.add(task);
		}
				
		double probOfEdge = DagGenerator.noOfEdges/(noOfNodes*(noOfNodes-1)/2.0);
		
		for (int i = 0; i < noOfNodes; i++) {			//Setting all edges with unit weights
			for (int j = i + 1; j < noOfNodes; j++) {
				if (Math.random() < probOfEdge) {
					TaskVertex primus = taskGraph.task(i);
					TaskVertex secundus = taskGraph.task(j);
					CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(primus.name()+"-"+secundus.name(), taskGraph.task(j), taskGraph.task(i), true);
					taskGraph.add(edge);
				}
			}
		}
		
		//To get accurate CCR
		DagGenerator.noOfEdges = taskGraph.sizeEdges();
		
		for(int i=0; i< taskGraph.sizeTasks(); i++){	//Assigning appropriate node weights
			taskGraph.task(i).setWeight(nodeWeightGenerator.get());
		}
				//Assigning appropriate edge weights
		for(CommEdge<TaskVertex> e: (Iterable<CommEdge<TaskVertex>>) taskGraph.edges()){
			e.setWeight(edgeWeightGenerator.get(e));
		}
		
		DecimalFormat df = new DecimalFormat("0.00");
		
		String pathName = "DAGS/Random";
		pathName += "/Nodes " + noOfNodes;
//		pathName += "/Density " + densityRequired;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "Random";				//Changing graph name to accurately reflect the task graph
		graphName += "_Nodes_" + noOfNodes;
//		graphName += "_Density_" + df.format(taskGraph.density());
		graphName += "_CCR_" + df.format(ccrRequired);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		graphName += "_" + identifier;
		
		pathName += "/" + graphName + ".gxl";
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.setName(graphName);
		taskGraph.calculateValues();	//NOTE: Need to set graph name prior to calculating values
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates a SP-graph with the specified properties.
	 * 
	 * @param noOfNodesRequired
	 * @param ccrRequired
	 * @param weightTypeRequired
	 * @param maxBfRequired
	 */
	public static void generateSeriesParallel(int noOfNodesRequired, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, int maxBfRequired, String identifier) {
		DagGenerator.noOfNodes = noOfNodesRequired;
		DagGenerator.maxBranchingFactor = maxBfRequired;
		DagGenerator.ccr = ccrRequired;
		
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Series Parallel";
		pathName += "/Nodes " + DagGenerator.noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "SeriesParallel";
		graphName += "-MaxBf-" + DagGenerator.maxBranchingFactor;
		graphName += "_Nodes_" + DagGenerator.noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		graphName += "_" + identifier;
		
		pathName += "/" + graphName + ".gxl";
		
		int nodeCounter = 0;
		TaskGraph taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, DagGenerator.noOfNodes);
		
		TaskVertex source = new BasicTaskVertex(""+nodeCounter, nodeCounter);	//Source
		source.setWeight(getRandomNumber(1, 10));
		nodeCounter++;
		taskGraph.add(source);
		LinkedList list = new LinkedList();
		list.add(source);
		
		while (nodeCounter < DagGenerator.noOfNodes) {
			TaskVertex currentTask = (TaskVertex)list.getFirst();
			if(Math.random() < 0.5){		//Series
				TaskVertex task = new BasicTaskVertex(""+nodeCounter, nodeCounter);
				task.setWeight(getRandomNumber(1, 10));
				nodeCounter++;
				taskGraph.add(task);
				list.add(task);
				if (taskGraph.outDegree(currentTask) > 0) {	//currentTask --> task --> dest
					TaskVertex dest = (TaskVertex)taskGraph.childrenIterator(currentTask).next();
					DirectedEdge<TaskVertex> edge = taskGraph.edgeBetween(currentTask, dest);
					boolean removedEdge = taskGraph.remove(edge);
					/*if(!removedEdge){
						System.out.println("Failed to remove edge");;
						System.exit(1);
					}*/
					CommEdge<TaskVertex> e = new BasicCommEdge<TaskVertex>(task.name()+"-"+dest.name(), task, dest, true);
					e.setWeight(getRandomNumber(1, 10));
					taskGraph.add(e);
				}
				CommEdge<TaskVertex> e = new BasicCommEdge<TaskVertex>(currentTask.name()+"-"+task.name(), currentTask, task, true);
				e.setWeight(getRandomNumber(1, 10));
				taskGraph.add(e);
				taskGraph.add(e);
				list.remove(currentTask);
			}
			else if(taskGraph.outDegree(currentTask) <= 1 && DagGenerator.noOfNodes-nodeCounter > 2){			//Parallel	//At least 3 coz there will be at least 3 nodes that are added in the parallel case
				int randomBranchingFactor = getRandomNumber(2, Math.min(
						DagGenerator.noOfNodes - nodeCounter - 1,
						maxBranchingFactor));	//-1 for sink that you create
				TaskVertex sink = new BasicTaskVertex(""+nodeCounter, nodeCounter);
				sink.setWeight(getRandomNumber(1, 10));
				nodeCounter++;
				taskGraph.add(sink);
				if (taskGraph.outDegree(currentTask) > 0) {
					TaskVertex dest = (TaskVertex)taskGraph.childrenIterator(currentTask).next();
					DirectedEdge<TaskVertex> edge = taskGraph.edgeBetween(currentTask, dest);
					boolean removedEdge = taskGraph.remove(edge);
					/*if(!removedEdge){
						System.out.println("Failed to remove edge");;
						System.exit(1);
					}*/
					edge = new BasicCommEdge<TaskVertex>(sink.name()+"-"+dest.name(), sink, dest, true);
					taskGraph.add(edge);
				}
				for (int i = 0; i < randomBranchingFactor; i++) {
					TaskVertex task2 = new BasicTaskVertex(""+nodeCounter, nodeCounter);
					task2.setWeight(getRandomNumber(1, 10));
					nodeCounter++;
					taskGraph.add(task2);
					CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(currentTask.name()+"-"+task2.name(), currentTask, task2, true);
					edge.setWeight(getRandomNumber(1, 10));
					taskGraph.add(edge);
					edge = new BasicCommEdge<TaskVertex>(task2.name()+"-"+sink.name(), task2, sink, true);
					edge.setWeight(getRandomNumber(1, 10));
					taskGraph.add(edge);
					list.add(task2);
				}
				list.add(sink);
				list.remove(currentTask);
			}
		}
		
		if(taskGraph.sizeTasks() != DagGenerator.noOfNodes){
			System.out.println("No of Nodes not equal");
			generateSeriesParallel(noOfNodesRequired, ccrRequired, nodeWeightGenerator, edgeWeightGenerator, maxBfRequired, identifier);
		}
		
		DagGenerator.noOfEdges = taskGraph.sizeEdges();
		
		if(!validateSeriesParallel(taskGraph)){
			System.out.println("INVALID SP Graph generated!");
			System.exit(1);
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.setName(graphName);
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	/**
	 * Generates a pipeline graph.
	 * 
	 * v -> v -> v  ...
	 * |    |    |        
	 * v -> v -> v  ...
	 * .    .    .
	 * .    .    .
	 * 
	 * @param height
	 * @param width
	 * @param ccrRequired
	 * @param weightTypeRequired
	 */
	public static void generatePipeline(int height, int width, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator){  
		if(height < 1 || width < 1){
			System.out.println("Height and Width must be greater than 0");
			System.exit(1);
		}
		
		DagGenerator.noOfNodes = height * width;
		DagGenerator.noOfEdges = height * (width - 1) + width * (height - 1);
		DagGenerator.density = (noOfEdges * 1.0) / noOfNodes;
		DagGenerator.ccr = ccrRequired;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Pipeline";
		pathName += "/Nodes " + DagGenerator.noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "Pipeline";
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
		
		for (int i = 0; i < DagGenerator.noOfNodes; i++) {		//Setting all tasks
			TaskVertex task = new BasicTaskVertex("" + i, i);
			taskGraph.add(task);
		}

		int counter = 0;
		for(int i=0; i<height; i++){		//Setting all edges
			for(int j=0; j<width; j++){
				if (i > 0) {	//vertical edges
					TaskVertex source = taskGraph.task(((i-1)*width)+j);
					TaskVertex dest = taskGraph.task(i*width+j);
					CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(source.name()+"-"+dest.name(), source, dest, true);
					taskGraph.add(edge);	
					counter++;
				}
	    		if(j > 0){		//Horizontal edges
	    			TaskVertex source = taskGraph.task(i*width+(j-1));
					TaskVertex dest = taskGraph.task(i*width+j);
					CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(source.name()+"-"+dest.name(), source, dest, true);
					taskGraph.add(edge);
	    			counter++;
	    		}
			}
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}

	/**
	 * Generates a Stencil/Diamond graph.
	 * 
	 * @param height
	 * @param width
	 * @param ccrRequired
	 * @param weightTypeRequired
	 */
	public static void generateStencil(int height, int width, double ccrRequired, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator){  
		if(height < 1 || width < 1){
			System.out.println("Height and Width must be greater than 0");
			System.exit(1);
		}
		
		DagGenerator.noOfNodes = height * width;
		DagGenerator.noOfEdges = width*(height-1) + (width-1)*(height-1)*2;
		DagGenerator.density = (noOfEdges * 1.0) / noOfNodes;
		DagGenerator.ccr = ccrRequired;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Stencil";
		pathName += "/Nodes " + noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "Stencil";
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, DagGenerator.noOfNodes);
		
		for(int i=0; i<DagGenerator.noOfNodes; i++){		//Setting all tasks
			TaskVertex task = new BasicTaskVertex(""+i,i);
			taskGraph.add(task);
		}
		
		//NOTE: Correct node concurrency calculation depends on stencil graph being set as follows, i.e. increasing id along a row
		//0,1,2
		//3,4,5,
		//6,7,8
		int counter = 0;
		for (int i = 1; i < height; i++) {
			for (int j = 0; j < width; j++) {	//vertical edges
				TaskVertex source = taskGraph.task(((i-1)*width)+j);
				TaskVertex dest = taskGraph.task(i*width+j);
				CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(source.name()+"-"+dest.name(), source, dest, true);
				taskGraph.add(edge);	
				counter++;
				
				if (j > 0) {	//Forward edges
					source = taskGraph.task((i-1)*width+j-1);
					dest = taskGraph.task(i*width+j);
					edge = new BasicCommEdge<TaskVertex>(source.name()+"-"+dest.name(), source, dest, true);
					taskGraph.add(edge);	
					counter++;
				}
				
				if (j + 1 < width) {	//Backward edges
					source = taskGraph.task((i-1)*width+j+1);
					dest = taskGraph.task(i*width+j);
					edge = new BasicCommEdge<TaskVertex>(source.name()+"-"+dest.name(), source, dest, true);
					taskGraph.add(edge);
					counter++;
				}
			}
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccrRequired);
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
//	/**
//	 * Generates node and edge weight arrays based on noOfNodes, noOfEdges and
//	 * weightType global variables. Also assigns new CCR value to the global
//	 * "ccr" variable.
//	 */
//	private static void generateWeights(WeightGenerator generator) {
//		DagGenerator.nodeWeights = new int[noOfNodes];
//		DagGenerator.edgeWeights = new int[noOfEdges];
//		for (int i = 0; i < noOfNodes; i++) nodeWeights[i] = generator.get();
//		for (int i = 0; i < noOfEdges; i++) edgeWeights[i] = generator.get();
//		
//		double nodeWeightSum = getSum(nodeWeights);
//		double edgeWeightSum = getSum(edgeWeights);
//		
//		double newCcr = edgeWeightSum / nodeWeightSum;
//		double scalingFactor = DagGenerator.ccr / newCcr;
//		
//		if (scalingFactor > 1.1) {
//			double newEdgeWeightSum = 0;
//			for (int i = 0; i < edgeWeights.length; i++) {
//				edgeWeights[i] = (int)Math.round(edgeWeights[i]*scalingFactor);
//				newEdgeWeightSum += edgeWeights[i];
//			}
//			newCcr = newEdgeWeightSum / nodeWeightSum * noOfNodes / noOfEdges;
//		}	
//		else if(scalingFactor < 0.9){		//If very close to 1 then do nothing
//			double newNodeWeightSum = 0;
//			for (int i = 0; i < nodeWeights.length; i++) {
//				nodeWeights[i] = (int)Math.round(nodeWeights[i]*(1.0/scalingFactor));
//				newNodeWeightSum += nodeWeights[i];
//			}
//			
//			newCcr = edgeWeightSum / newNodeWeightSum;
//		}
//		DagGenerator.ccr = newCcr;
//	}
	
	/**
	 * 
	 * @param weightType
	 * @param howMany
	 * @return	an array of integers
	 */
	private static int[] getWeights(String weightType, int howMany){
		int[] weights = new int[howMany];
		
		if (weightType.equalsIgnoreCase("Unit")) {
			int value = getRandomNumber(1, 10);	//1 to 10
			//int value = (int) Math.ceil((Math.random() * 9) + 1); //1 to 10
			for (int i = 0; i < weights.length; i++) {
				weights[i] = value;
			}
		}
		else if(weightType.equalsIgnoreCase("Random")){
			for(int i=0; i<weights.length; i++){
				weights[i] = getRandomNumber(1, 10);
				//weights[i] = (int)Math.ceil((Math.random()*9)+1);	
			}
		}
		else if (weightType.equalsIgnoreCase("erlang")) {
			for(int i=0; i<weights.length; i++) {
				weights[i] = getRandomNumber(1, 10);
			}
		}
		else{
			System.out.println("Weight type is not recognised. Please enter one of the following for input parameter weightType:\n \"Unit\", \"Random\" ");
			System.exit(1);
		}
		
		return weights;
	}
	
	/**
	 * Checks that there is only 1 source and 1 sink. Also, all tasks have
	 * exactly one parent and child excluding the source and sink.
	 * 
	 * 
	 * @param taskGraph
	 * @return	whether the given task graph is a valid fork-join graph
	 */
	private static boolean validateForkJoin(TaskGraph taskGraph){
		int source = 0;
		int sink = 0;
		
		for(int i=0; i<taskGraph.sizeTasks(); i++){
			if(taskGraph.inDegree(taskGraph.task(i)) == 0){
				source++;
			}
			else if(taskGraph.outDegree(taskGraph.task(i)) == 0){
				sink++;
			}
			else if (taskGraph.inDegree(taskGraph.task(i)) != 1
					|| taskGraph.outDegree(taskGraph.task(i)) != 1) {	//All tasks should have exactly one parent & child except source and sink
				return false;
			}
		}
		
		return (source == 1 && sink == 1);		//Exactly one source and sink
	}
	
	/**
	 * Checks that there is exactly one source. Also checks that all tasks have
	 * exactly one parent except the source and there are exactly (Number of
	 * nodes - 1) edges.
	 * 
	 * @param taskGraph
	 * @return whether the given task graph is a valid out-tree
	 */
	private static boolean validateOutTree(TaskGraph taskGraph){
		int source = 0;
		
		if((taskGraph.sizeTasks()-1) != taskGraph.sizeEdges()){	//n-1 edges
			throw new RuntimeException();
		}
		
		for(int i=0; i<taskGraph.sizeTasks(); i++){
			if(taskGraph.inDegree(taskGraph.task(i)) == 0){
				source++;
			}
			else if(taskGraph.inDegree(taskGraph.task(i)) != 1){	//All tasks should have one parent except root
				throw new RuntimeException();
			}
		}
		
		return source == 1;		//Exactly one root
	}
	
	/**
	 * Checks that all tasks have exactly one child except the sink.
	 * Also checks that there are exactly (Number of nodes - 1) edges.
	 * 
	 * @param taskGraph
	 * @return	 whether the given task graph is a valid in-tree
	 */
	private static boolean validateInTree(TaskGraph taskGraph){
		
		int sink = 0;
		
		if((taskGraph.sizeTasks()-1) != taskGraph.sizeEdges()){	//n-1 edges
			return false;
		}
		
		for(int i=0; i<taskGraph.sizeTasks(); i++){
			if(taskGraph.outDegree(taskGraph.task(i)) == 0){
				sink++;
			}
			else if(taskGraph.outDegree(taskGraph.task(i)) != 1){	//All tasks have one child except sink
				return false;
			}
		}
		
		return sink == 1;		//Exactly one sink
	}
	
	/**
	 * Checks that the DAG has exactly one source and sink
	 * 
	 * @param taskGraph
	 * @return	whether the given task graph is a valid SP graph
	 */
	private static boolean validateSeriesParallel(TaskGraph taskGraph){
		int source = 0;
		int sink = 0;
		
		for(int i=0; i<taskGraph.sizeTasks(); i++){
			if(taskGraph.inDegree(taskGraph.task(i)) == 0){
				source++;
			}
			else if(taskGraph.outDegree(taskGraph.task(i)) == 0){
				sink++;
			}
		}
		
		return (source == 1 && sink == 1 && taskGraph.getMaxOutDegree() <= DagGenerator.maxBranchingFactor);		//Exactly one source and sink
	}
	
	/**
	 * Returns the maximum number of nodes that can be present in a task graph
	 * given a certain branching factor and depth.
	 * 
	 * @param maxBf		the maximum branching factor in the task graph
	 * @param depth		the maximum depth of the task graph
	 * @return			the maximum number of nodes that can be present in the task graph
	 */
	private static int getMaxNoOfNodes(int maxBf, int depth){
		if (depth == 0) {
			return 1;
		}
		else{
			return getMaxNoOfNodes(maxBf, (depth-1)) + (int)Math.pow(maxBf, depth);
		}
	}
	
	/**
	 * Returns the sum of the int array
	 * 
	 * @param array
	 * @return	the sum of the int array
	 */
	private static double getSum(int[] array){
		double sum = 0;
		for(int i=0; i<array.length; i++){
			sum += array[i];
		}
		return sum;
	}
	
	/**
	 * Returns a random int from start to end (inclusive). Uses
	 * the Math.random method
	 * 
	 * @param start
	 * @param end
	 * @return	a random int value
	 */
	private static int getRandomNumber(int start, int end){
		int randomNo = (int) Math.ceil((Math.random() * (end-start)) + start); //start to end
		return randomNo;
	}
	
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> addEdgeCosts(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph, WeightGenerator generator, double ccr)
	{
		for (CommEdge<TaskVertex> edge : taskGraph.edges())
		{
			edge.setWeight(generator.get(edge));
		}
		
		long totalComputation = 0;
		for (TaskVertex task : taskGraph.vertices())
		{
			totalComputation += task.weight();
		}
		long totalCommunication = 0;
		for (CommEdge<TaskVertex> edge : taskGraph.edges())
		{
			totalCommunication += edge.weight();
		}
		double scalingFactor = ccr * totalComputation / totalCommunication; // * taskGraph.sizeEdges() / taskGraph.sizeVertices();
		
		for (CommEdge<TaskVertex> edge : taskGraph.edges())
		{
			edge.setWeight((int) Math.round( edge.weight() * scalingFactor ));
		}
		
		//check ccr
		totalComputation = 0;
		for (TaskVertex task : taskGraph.vertices())
		{
			totalComputation += task.weight();
		}
		totalCommunication = 0;
		for (CommEdge<TaskVertex> edge : taskGraph.edges())
		{
			totalCommunication += edge.weight();
		}
		double newCcr = totalCommunication / (double) totalComputation;
		
		if (Math.abs(newCcr - ccr) / ccr > 0.5) throw new RuntimeException();
		
		return taskGraph;
	}
	
	/**
	 * Is same as pipeline
	 */
	public static void generateLaplace(int levels, double ccr, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator)
	{
		DagGenerator.noOfNodes = levels * levels;
		DagGenerator.noOfEdges = 2 * (levels * levels - levels);
		
		DagGenerator.density = (noOfEdges * 1.0) / noOfNodes;
		DagGenerator.ccr = ccr;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Laplace";
		pathName += "/Nodes " + noOfNodes;
		pathName += "/CCR " + ccr;	//Coz folder names are static. Hence use ccrRequired
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "Laplace";
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);		//Use actual CCR
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		pathName += "/" + graphName + ".gxl";
		
		//TaskGraph taskGraph = new TaskGraphImpl(graphName, noOfNodes);
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<>(graphName, noOfNodes);
		
		TaskVertex[] previousRow = null;
		int taskIndex = 0;
		int edgeIndex = 0;
		for (int i = 0; i < levels; i++)
		{
			TaskVertex[] currentRow = new TaskVertex[levels];
			for (int j = 0; j < levels; j++)
			{
				currentRow[j] = new BasicTaskVertex(Integer.toString(taskIndex), taskIndex);
				taskGraph.add(currentRow[j]);
				
				if (j > 0)
				{
					CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>((taskIndex - 1) + "-" + taskIndex, currentRow[j - 1], currentRow[j], true);
					taskGraph.add(edge);
				}
				if (previousRow != null)
				{
					CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(previousRow[j].name() + "-" + currentRow[j].name(), previousRow[j], currentRow[j], true);
					taskGraph.add(edge);
				}
				taskIndex++;
			}
			previousRow = currentRow;
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccr);
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	public static void generateRandomFanInFanOut(int minNodeCount, int maxOutDegree, int maxInDegree, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, double ccr, String identifier)
	{
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<>("graph", minNodeCount + Math.max(maxOutDegree, maxInDegree));
		
		int taskIndex = 0;
		taskGraph.add(new BasicTaskVertex(Integer.toString(taskIndex), taskIndex++));
		
		int prev = 0;
		while (taskGraph.sizeVertices() < minNodeCount)
		{
			if (Math.random() > 0.5)
			{
				int minOutDegree = Integer.MAX_VALUE;
				TaskVertex minOutDegreeTask = null;
				for (TaskVertex task : taskGraph.vertices())
				{
					if (taskGraph.outDegree(task) < minOutDegree)
					{
						minOutDegreeTask = task;
						minOutDegree = taskGraph.outDegree(task);
					}
				}
				int edgeAddCount = (int) Math.ceil(Math.random() * (maxOutDegree - minOutDegree));
				for (int i = 0; i < edgeAddCount; i++)
				{
					TaskVertex newTask = new BasicTaskVertex(Integer.toString(taskIndex), taskIndex++);
					taskGraph.add(newTask);
					taskGraph.add(new BasicCommEdge<TaskVertex>(minOutDegreeTask.index()+"-"+newTask.index(), minOutDegreeTask, newTask, true));
				}
				
				if (taskGraph.outDegree(minOutDegreeTask) > maxOutDegree) throw new RuntimeException();
				if (taskGraph.sizeVertices() <= prev) throw new RuntimeException();
				prev = taskGraph.sizeVertices();
			}
			else
			{
				List<TaskVertex> availableSourceTasks = new ArrayList<>();
				for (TaskVertex task : taskGraph.vertices())
				{
					if (taskGraph.outDegree(task) < maxOutDegree)
					{
						availableSourceTasks.add(task);
					}
				}
				TaskVertex newTask = new BasicTaskVertex(Integer.toString(taskIndex), taskIndex++);
				taskGraph.add(newTask);
				int edgeAddCount = Math.min(availableSourceTasks.size(), (int) Math.round(Math.random() * (maxOutDegree)));
				for (int i = 0; i < edgeAddCount; i++)
				{
					TaskVertex tail = availableSourceTasks.remove((int) Math.round(Math.random() * (availableSourceTasks.size() - 1) ));
					taskGraph.add(new BasicCommEdge<TaskVertex>(tail.index()+"-"+newTask.index(), tail, newTask, true));
				}
				
				if (taskGraph.inDegree(newTask) > maxInDegree) throw new RuntimeException();
				if (taskGraph.sizeVertices() <= prev) throw new RuntimeException();
				prev = taskGraph.sizeVertices();
			}
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccr);
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Random (fan-in fan-out)";
		pathName += "/Nodes " + minNodeCount;
		pathName += "/CCR " + ccr;	//Coz folder names are static. Hence use ccrRequired
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "RandomFanInOut";
		graphName += "_Nodes_" + taskGraph.sizeVertices();
		graphName += "_CCR_" + df.format(ccr);		//Use actual CCR
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		graphName += "_" + identifier;
		
		pathName += "/" + graphName + ".gxl";
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	public static void generateRandomLayerByLayer(int nodeCount, int layerCount, double edgeProbability, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, double ccr, String identifier)
	{
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<>("random", nodeCount);
		List<Collection<TaskVertex>> layers = new ArrayList<>(layerCount);
		for (int i = 0; i < layerCount; i++)
		{
			layers.add(new ArrayList<>());
		}
		for (int i = 0; i < nodeCount; i++)
		{
			TaskVertex task = new BasicTaskVertex(Integer.toString(i), i);
			taskGraph.add(task);
			layers.get( ThreadLocalRandom.current().nextInt(0, layerCount) ).add(task);
		}
		
		for (int i = 0; i < layerCount; i++)
		{
			for (TaskVertex task : layers.get(i))
			{
				for (int j = i + 1; j < layerCount; j++)
				{
					for (TaskVertex prospectiveChild : layers.get(j))
					{
						if (Math.random() < edgeProbability)
						{
							taskGraph.add(new BasicCommEdge<TaskVertex>(task.index()+"-"+prospectiveChild.index(), task, prospectiveChild, true));
						}
					}
				}
			}
		}
		
		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccr);
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Random (layer by layer)";
		pathName += "/Nodes " + taskGraph.sizeVertices();
		pathName += "/CCR " + ccr;	//Coz folder names are static. Hence use ccrRequired
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "RandomLayered";
		graphName += "_Nodes_" + taskGraph.sizeVertices();
		graphName += "_CCR_" + df.format(ccr);		//Use actual CCR
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		graphName += "_" + identifier;
		
		pathName += "/" + graphName + ".gxl";
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	public static void generateRandomIntersectingTotalOrders(int nodeCount, int totalOrderCount, WeightGenerator nodeWeightGenerator, WeightGenerator edgeWeightGenerator, double ccr, String identifier)
	{
		List<Integer> shuffleList = new ArrayList<>(nodeCount);
		for (int i = 0; i < nodeCount; i++)
		{
			shuffleList.add(i);
		}
		// first total order, every ordered pair associated with it is stored in a Set
		Collections.shuffle(shuffleList);
		Set<int[]> orderedPairs = new LinkedHashSet<>();
		for (int i = 0; i < nodeCount; i++)
		{
			for (int j = i + 1; j < nodeCount; j++)
			{
				orderedPairs.add(new int[] {shuffleList.get(i), shuffleList.get(j)});
			}
		}
		// the rest of the total orders, stored in arrays where an element at index x represents the position of task x in the order
		int[][] totalOrders = new int[totalOrderCount - 1][nodeCount];
		for (int i = 0; i < totalOrderCount - 1; i++)
		{
			Collections.shuffle(shuffleList);
			for (int j = 0; j < nodeCount; j++)
			{
				totalOrders[i][j] = shuffleList.get(j);
			}
		}
		// intersect total orders by removing ordered pairs that contradicts any total order
		for (int i = 0; i < totalOrderCount - 1; i++)
		{
			for (Iterator<int[]> iterator = orderedPairs.iterator(); iterator.hasNext();)
			{
				int[] orderedPair = iterator.next();
				int[] totalOrder = totalOrders[i];
				if (totalOrder[orderedPair[0]] > totalOrder[orderedPair[1]])
				{
					iterator.remove();
				}
			}
		}
		// add remaining ordered pairs to task graph and apply a transitive reduction
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<>("random", nodeCount);
		for (int i = 0; i < nodeCount; i++)
		{
			taskGraph.add(new BasicTaskVertex(Integer.toString(i), i));
		}
		for (int[] orderedPair : orderedPairs)
		{
			String tailId = Integer.toString(orderedPair[0]);
			String headId = Integer.toString(orderedPair[1]);
			taskGraph.add(new BasicCommEdge<TaskVertex>(tailId+"-"+headId, taskGraph.vertexForName(tailId), taskGraph.vertexForName(headId), true));
		}
		transitiveReduction(taskGraph);

		addNodeCosts(taskGraph, nodeWeightGenerator);
		addEdgeCosts(taskGraph, edgeWeightGenerator, ccr);
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Random (intersecting total orders)";
		pathName += "/Nodes " + taskGraph.sizeVertices();
		pathName += "/CCR " + ccr;	//Coz folder names are static. Hence use ccrRequired
		pathName += "/" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		
		String graphName = "RandomIntersect";
		graphName += "_Nodes_" + taskGraph.sizeVertices();
		graphName += "_CCR_" + df.format(ccr);		//Use actual CCR
		graphName += "_WeightType_" + String.format("nodes[%s]-edges[%s]", nodeWeightGenerator.name(), edgeWeightGenerator.name());
		graphName += "_" + identifier;
		
		pathName += "/" + graphName + ".gxl";
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> transitiveReduction(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph)
	{
		Map<TaskVertex, Set<TaskVertex>> taskDescendantsMap = new HashMap<>();
		for (TaskVertex task : taskGraph.inverseTopologicalOrder())
		{
			Set<TaskVertex> descendants = new HashSet<>();
			for (TaskVertex child : taskGraph.children(task))
			{
				descendants.addAll(taskDescendantsMap.get(child));
			}
			List<TaskVertex> removableChildren = new ArrayList<>();
			for (TaskVertex child : taskGraph.children(task))
			{
				if (descendants.contains(child))
				{
					removableChildren.add(child);
				}
				else
				{
					descendants.add(child);
				}
			}
			for (TaskVertex child : removableChildren)
			{
				taskGraph.remove(taskGraph.edgeBetween(task, child));
			}
			taskDescendantsMap.put(task, descendants);
		}
		return taskGraph;
	}
	
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> addNodeCosts(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph, WeightGenerator generator)
	{
		for (TaskVertex task : taskGraph.vertices())
		{
			task.setWeight(generator.get());
		}
		return taskGraph;
	}
	
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> addDecreasingNodeWeights(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph)
	{
		for (TaskVertex task : taskGraph.inverseTopologicalOrder())
		{
			if (taskGraph.outDegree(task) == 0) 
			{
				task.setWeight(5);
			}
			else 
			{
				int weight = 0;
				for (TaskVertex child : taskGraph.children(task))
				{
					weight += child.weight();
				}
				task.setWeight(weight);
			}
		}
		return taskGraph;
	}
	
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> addIncreasingNodeWeights(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph)
	{
		for (TaskVertex task : taskGraph.topologicalOrder())
		{
			if (taskGraph.inDegree(task) == 0) 
			{
				task.setWeight(5);
			}
			else 
			{
				int weight = 0;
				for (TaskVertex parent : taskGraph.parents(task))
				{
					weight += parent.weight();
				}
				task.setWeight(weight);
			}
		}
		return taskGraph;
	}
	
	public static void generateForkJoin(int nodeCount, double ccr, WeightGenerator inEdgesWeightGen, WeightGenerator outEdgesWeightGen, 
			WeightGenerator middleNodesWeightGen, int sourceWeight, int sinkWeight, String identifier)
	{
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<>("fork-join", nodeCount);
		
		TaskVertex sourceNode = new BasicTaskVertex("0", 0);
		TaskVertex sinkNode = new BasicTaskVertex(Integer.toString(nodeCount - 1), nodeCount - 1);
		taskGraph.add(sourceNode);
		taskGraph.add(sinkNode);
		sourceNode.setWeight(sourceWeight);
		sinkNode.setWeight(sinkWeight);
		
		for (int taskIndex = 1; taskIndex < nodeCount - 1; taskIndex++)
		{
			TaskVertex task = new BasicTaskVertex(Integer.toString(taskIndex), taskIndex);
			task.setWeight(middleNodesWeightGen.get());
			CommEdge<TaskVertex> inEdge = new BasicCommEdge<TaskVertex>("0-"+taskIndex, sourceNode, task, true);
			CommEdge<TaskVertex> outEdge = new BasicCommEdge<TaskVertex>(taskIndex+"-"+(nodeCount-1), task, sinkNode, true);
			inEdge.setWeight(inEdgesWeightGen.get(inEdge));
			outEdge.setWeight(outEdgesWeightGen.get(outEdge));
			taskGraph.add(task);
			taskGraph.add(inEdge);
			taskGraph.add(outEdge);
		}

		long totalComputation = 0;
		for (TaskVertex task : taskGraph.vertices())
		{
			totalComputation += task.weight();
		}
		long totalCommunication = 0;
		for (CommEdge<TaskVertex> edge : taskGraph.edges())
		{
			totalCommunication += edge.weight();
		}
		double scalingFactor = ccr * totalComputation / totalCommunication; // * taskGraph.sizeEdges() / taskGraph.sizeVertices();
		
		for (CommEdge<TaskVertex> edge : taskGraph.edges())
		{
			edge.setWeight((int) Math.round( edge.weight() * scalingFactor ));
		}
		
		//check ccr
		totalComputation = 0;
		for (TaskVertex task : taskGraph.vertices())
		{
			totalComputation += task.weight();
		}
		totalCommunication = 0;
		for (CommEdge<TaskVertex> edge : taskGraph.edges())
		{
			totalCommunication += edge.weight();
		}
		double newCcr = totalCommunication / (double) totalComputation;
		
		if (Math.abs(newCcr - ccr) / ccr > 0.5) throw new RuntimeException();
		
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Fork-join";
		pathName += "/Nodes " + taskGraph.sizeVertices();
		pathName += "/CCR " + ccr;	//Coz folder names are static. Hence use ccrRequired
		pathName += "/" + String.format("inEdges[%s]-outEdges[%s]-node[%s]", inEdgesWeightGen.name(), outEdgesWeightGen.name(), middleNodesWeightGen.name());
		
		String graphName = "Fork-join";
		graphName += "_Nodes_" + taskGraph.sizeVertices();
		graphName += "_CCR_" + df.format(ccr);		//Use actual CCR
		graphName += "_WeightType_" + String.format("inEdges[%s]-outEdges[%s]-node[%s]", inEdgesWeightGen.name(), outEdgesWeightGen.name(), middleNodesWeightGen.name());
		graphName += "_" + identifier;
		
		pathName += "/" + graphName + ".gxl";
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	
	public static void generateInTreeByNodesIncreasingWeights(int noOfNodesRequired, double ccrRequired, int maxBfRequired, boolean isBalanced) {
		DagGenerator.maxBranchingFactor = maxBfRequired;	//If 1 we have a chain
		DagGenerator.isBalanced = isBalanced;
		DagGenerator.ccr = ccrRequired;
		
		DagGenerator.noOfNodes = noOfNodesRequired;		//At least 2 to make a chain atleast
		DagGenerator.noOfEdges = noOfNodes - 1;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/In Tree";
		pathName += "/Nodes " + DagGenerator.noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + "Increasing weights";
		
		String graphName = "InTree";
		if(DagGenerator.isBalanced){
			graphName += "-Balanced";
		}
		else{
			graphName += "-Unbalanced";
		}
		graphName += "-MaxBf-" + DagGenerator.maxBranchingFactor;
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + "increasing";
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
		taskGraph.setName(graphName);
		
		for(int i=0; i<noOfNodes; i++){		//Setting all tasks
			TaskVertex task = new BasicTaskVertex("" + i, i);
			taskGraph.add(task);
		}
		
		TaskVertex[] tasks = (TaskVertex[]) Utils.iterableToArray(taskGraph.vertices(), new TaskVertex[taskGraph.sizeVertices()]);
		if(DagGenerator.isBalanced){	//MaxBF
			int edgeCounter = 0;
			for (int i = 0; i < noOfNodes; i++) {			
				for(int j=(i*maxBranchingFactor+1); j<(i*maxBranchingFactor+1+maxBranchingFactor); j++){
					if (edgeCounter < noOfEdges) {
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(tasks[j].name()+"-"+tasks[i].name(), tasks[j], tasks[i], true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		else{ //Varying BF
			int bf = 0;
			int edgeCounter = 0;
			
			for (int i = 0; i < noOfNodes; i++) {
				if(i == 0){
					bf = getRandomNumber(1, maxBranchingFactor);	//Atleast 1
				}
				else{
					bf = getRandomNumber(0, maxBranchingFactor);
				}
				for (int j = 0; j < bf; j++) {
					if(edgeCounter < noOfEdges){
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(tasks[edgeCounter+1].name()+"-"+tasks[i].name(), tasks[edgeCounter+1], tasks[i], true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		
		if(!validateInTree(taskGraph)){
			throw new RuntimeException("Invalid Out-tree generated!");
		}

		addIncreasingNodeWeights(taskGraph);
		addEdgeCosts(taskGraph, new ConstantValueGenerator(1).setSourceWeightInfluence(1.0), ccrRequired);
				
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	public static void generateOutTreeByNodesDecreasingWeights(int noOfNodesRequired, double ccrRequired, int maxBfRequired, boolean isBalanced) {
		// this segment of code is copied from generateInTrees, the generated graph is reversed to produce outtree, no time to refactor
		DagGenerator.maxBranchingFactor = maxBfRequired;	//If 1 we have a chain
		DagGenerator.isBalanced = isBalanced;
		DagGenerator.ccr = ccrRequired;
		
		DagGenerator.noOfNodes = noOfNodesRequired;		//At least 2 to make a chain atleast
		DagGenerator.noOfEdges = noOfNodes - 1;
		
		DecimalFormat df = new DecimalFormat("0.00");
		String pathName = "DAGS/Out Tree";
		pathName += "/Nodes " + DagGenerator.noOfNodes;
		pathName += "/CCR " + ccrRequired;
		pathName += "/" + "Decreasing weights";
		
		String graphName = "OutTree";
		if(DagGenerator.isBalanced){
			graphName += "-Balanced";
		}
		else{
			graphName += "-Unbalanced";
		}
		graphName += "-MaxBf-" + DagGenerator.maxBranchingFactor;
		graphName += "_Nodes_" + noOfNodes;
		graphName += "_CCR_" + df.format(ccr);
		graphName += "_WeightType_" + "Decreasing";
		
		pathName += "/" + graphName + ".gxl";
		
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(graphName, noOfNodes);
		taskGraph.setName(graphName);
		
		for(int i=0; i<noOfNodes; i++){		//Setting all tasks
			TaskVertex task = new BasicTaskVertex("" + i, i);
			taskGraph.add(task);
		}
		
		TaskVertex[] tasks = (TaskVertex[]) Utils.iterableToArray(taskGraph.vertices(), new TaskVertex[taskGraph.sizeVertices()]);
		if(DagGenerator.isBalanced){	//MaxBF
			int edgeCounter = 0;
			for (int i = 0; i < noOfNodes; i++) {			
				for(int j=(i*maxBranchingFactor+1); j<(i*maxBranchingFactor+1+maxBranchingFactor); j++){
					if (edgeCounter < noOfEdges) {
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(tasks[j].name()+"-"+tasks[i].name(), tasks[j], tasks[i], true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		else{ //Varying BF
			int bf = 0;
			int edgeCounter = 0;
			
			for (int i = 0; i < noOfNodes; i++) {
				if(i == 0){
					bf = getRandomNumber(1, maxBranchingFactor);	//Atleast 1
				}
				else{
					bf = getRandomNumber(0, maxBranchingFactor);
				}
				for (int j = 0; j < bf; j++) {
					if(edgeCounter < noOfEdges){
						CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(tasks[edgeCounter+1].name()+"-"+tasks[i].name(), tasks[edgeCounter+1], tasks[i], true);
						taskGraph.add(edge);
						edgeCounter++;
					}
				}
			}
		}
		
		reverse(taskGraph);
		
		if(!validateOutTree(taskGraph)){
			throw new RuntimeException("Invalid In-tree generated!");
		}
		
		addDecreasingNodeWeights(taskGraph);
		addEdgeCosts(taskGraph, new ConstantValueGenerator(1).setSourceWeightInfluence(1.0), ccrRequired);
		
		taskGraph.calculateValues();
		(new OutputGXL()).generateGXLFile(pathName, graphName, taskGraph);
	}
	
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> reverse(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph)
	{
		List<CommEdge<TaskVertex>> edges = new ArrayList<>();
		for (CommEdge<TaskVertex> edge : taskGraph.edges())
		{
			edges.add(edge);
		}
		for (CommEdge<TaskVertex> edge : edges)
		{
			taskGraph.remove(edge);
			taskGraph.add(new BasicCommEdge<>(edge.to().name()+"-"+edge.from().name(), edge.to(), edge.from(), true));
		}
		return taskGraph;
	}
}