package parcschedule.graphGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import parcschedule.graphGenerator.graphReaders.DaxReader;
import parcschedule.graphGenerator.graphReaders.StgReader;
import parcschedule.graphGenerator.graphReaders.TggReader;
import org.xml.sax.SAXException;

import parcschedule.graphGenerator.weightGenerators.ConstantValueGenerator;
import parcschedule.graphGenerator.weightGenerators.ErlangRandomGenerator;
import parcschedule.graphGenerator.weightGenerators.UniformRandomGenerator;
import parcschedule.graphGenerator.weightGenerators.WeightGenerator;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

public class GeneratorApp
{
	static double[] ccrs;
	static int[] nodeCounts;
	static WeightGenerator[] weightGenerators;
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ParserConfigurationException, SAXException
	{
		ccrs = new double[] {0.1, 1, 10};
		nodeCounts = new int[] {50, 100, 200, 500, 1000};
		
		weightGenerators = new WeightGenerator[] {
				new ErlangRandomGenerator(20, 0.1).setLimits(100, 300),
				new UniformRandomGenerator(10, 150),
				new ErlangRandomGenerator(100, 0.5).add(50, 0.5, 1).setLimits(50, 300)
		};
		
		for (int nodeCount : nodeCounts)
		{
			for (double ccr : ccrs)
			{
				for (WeightGenerator weightGenerator : weightGenerators)
				{
//					generateRandomByMatrix(nodeCount, ccr, weightGenerator, weightGenerator);
//					generateRandomByMatrix(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5));
					
//					generateRandomFanInOut(nodeCount, ccr, weightGenerator, weightGenerator);
//					generateRandomFanInOut(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5));
//
//					generateRandomLayerByLayer(nodeCount, ccr, weightGenerator, weightGenerator);
//					generateRandomLayerByLayer(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5));
//					
//					generateRandomIntersection(nodeCount, ccr, weightGenerator, weightGenerator);
//					generateRandomIntersection(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5));
				}
			}
		}
		
//		generateSP();
//		generateTrees();
		generateForkJoin();
//		
//		generatePipeline();
//		generateStencil();
//		
//		generateFft();
//		generateGauss();
//		
//		generateCholesky();
//		generateStgs();
//		
//		generateWorkflows();
	}
	
	static void generateWorkflows() throws ParserConfigurationException, SAXException, IOException
	{
		for (String graph : new String[] {"CyberShake", "Epigenomics", "Inspiral", "Montage", "Sipht"})
		{
			for (int n : nodeCounts)
			{
				for (double ccr : ccrs)
				{
					String pathName = "DAGS/" + graph + "/Nodes " + n + "/CCR " + ccr;
					String graphName = graph + "_Nodes_" + n + "_CCR_" + ccr;
					new OutputGXL().generateGXLFile(pathName, graphName, DagGenerator.addEdgeCosts(
							multiplyNodeWeights(DaxReader.read(new File("workflows/" + graph + "_" + n + ".xml")), 10),
								new UniformRandomGenerator(10, 100).setSourceWeightInfluence(0.5), ccr));
				}
			}
		}
	}
	
	static void generateStgs() throws FileNotFoundException, IOException
	{
		for (double ccr : ccrs)
		{
			for (String graph : new String[] {"fpppp", "robot", "sparse"})
			{
				String pathName = "DAGS/" + graph + "/CCR " + ccr;
				String graphName = graph + "_CCR_" + ccr;
				new OutputGXL().generateGXLFile(pathName, graphName, DagGenerator.addEdgeCosts(
						multiplyNodeWeights(StgReader.read(new File("stg/" + graph + ".stg")), 10),
							new UniformRandomGenerator(10, 100).setSourceWeightInfluence(0.5), ccr));
			}
		}
	}
	
	static void generateCholesky()
	{
		for (double ccr : ccrs)
		{
			for (int n : new int[] {56, 120, 220, 560, 1140})
			{
				String pathName = "DAGS/Cholesky/Nodes " + n + "/CCR " + ccr;
				String graphName = "Cholesky_Nodes_" + n + "_CCR_" + ccr;
				new OutputGXL().generateGXLFile(pathName, graphName, DagGenerator.addEdgeCosts(
						multiplyNodeWeights(GXLGraphGenerator.generate(new File("cholesky structures/" + n + "N_cholesky.gxl"), null), 10),
							new ConstantValueGenerator(1), ccr));
			}
		}
	}
	
	static void generateGauss() throws FileNotFoundException, IOException
	{
		for (double ccr : ccrs)
		{
			for (int n : new int[] {35, 135, 527, 2079})
			{
				String pathName = "DAGS/Gauss/Nodes " + n + "/CCR " + ccr;
				String graphName = "Gauss_Nodes_" + n + "_CCR_" + ccr;
				new OutputGXL().generateGXLFile(pathName, graphName, DagGenerator.addEdgeCosts(
						DagGenerator.addNodeCosts(TggReader.read(new File("gauss structures/" + n + "N_gauss.tgg")), new ConstantValueGenerator(100)),
							new ConstantValueGenerator(1), ccr));
			}
		}
	}
	
	static void generateFft() throws FileNotFoundException, IOException
	{

		for (double ccr : ccrs)
		{
			for (int n : new int[] {39, 95, 223, 511, 1151})
			{
				String pathName = "DAGS/FFT/Nodes " + n + "/CCR " + ccr;
				String graphName = "FFT_Nodes_" + n + "_CCR_" + ccr;
				new OutputGXL().generateGXLFile(pathName, graphName, DagGenerator.addEdgeCosts(
						DagGenerator.addNodeCosts(TggReader.read(new File("fft structures/" + n + "N_fft.tgg")), new ConstantValueGenerator(100)), 
							new ConstantValueGenerator(1), ccr));
			}
		}
	}
	
	static void generateRandomByMatrix(int nodeCount, double ccr, WeightGenerator nodeWeightGen, WeightGenerator edgeWeightGen)
	{
		DagGenerator.generateRandom(nodeCount, 2, ccr, nodeWeightGen, edgeWeightGen, "2");
		DagGenerator.generateRandom(nodeCount, 4, ccr, nodeWeightGen, edgeWeightGen, "4");
		DagGenerator.generateRandom(nodeCount, 8, ccr, nodeWeightGen, edgeWeightGen, "8");
	}
	
	static void generateRandomFanInOut(int nodeCount, double ccr, WeightGenerator nodeWeightGen, WeightGenerator edgeWeightGen)
	{
		DagGenerator.generateRandomFanInFanOut(nodeCount, 2, 2, nodeWeightGen, edgeWeightGen, ccr, "2");
		DagGenerator.generateRandomFanInFanOut(nodeCount, 4, 4, nodeWeightGen, edgeWeightGen, ccr, "4");
		DagGenerator.generateRandomFanInFanOut(nodeCount, 8, 8, nodeWeightGen, edgeWeightGen, ccr, "8");
	}
	
	static void generateRandomLayerByLayer(int nodeCount, double ccr, WeightGenerator nodeWeightGen, WeightGenerator edgeWeightGen)
	{
		DagGenerator.generateRandomLayerByLayer(nodeCount, nodeCount/10 + 1, 3.5/nodeCount, nodeWeightGen, edgeWeightGen, ccr, "1");
		DagGenerator.generateRandomLayerByLayer(nodeCount, nodeCount/20 + 1, 3.5/nodeCount, nodeWeightGen, edgeWeightGen, ccr, "2");
		DagGenerator.generateRandomLayerByLayer(nodeCount, nodeCount/40 + 1, 3.5/nodeCount, nodeWeightGen, edgeWeightGen, ccr, "4");
	}
	
	static void generateRandomIntersection(int nodeCount, double ccr, WeightGenerator nodeWeightGen, WeightGenerator edgeWeightGen)
	{
		DagGenerator.generateRandomIntersectingTotalOrders(nodeCount, 3, nodeWeightGen, edgeWeightGen, ccr, "3");
		DagGenerator.generateRandomIntersectingTotalOrders(nodeCount, 4, nodeWeightGen, edgeWeightGen, ccr, "4");
		DagGenerator.generateRandomIntersectingTotalOrders(nodeCount, 5, nodeWeightGen, edgeWeightGen, ccr, "5");
	}
	
	static void generateTrees()
	{
		for (int nodeCount : nodeCounts)
		{
			for (double ccr : ccrs)
			{
				for (WeightGenerator weightGenerator : weightGenerators)
				{
					DagGenerator.generateInTreeByNodes(nodeCount, ccr, weightGenerator, weightGenerator, 5, false);
					DagGenerator.generateInTreeByNodes(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5), 5, false);

					DagGenerator.generateOutTreeByNodes(nodeCount, ccr, weightGenerator, weightGenerator, 5, false);
					DagGenerator.generateOutTreeByNodes(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5), 5, false);
				}
				
				DagGenerator.generateInTreeByNodesIncreasingWeights(nodeCount, ccr, 5, false);
				DagGenerator.generateOutTreeByNodesDecreasingWeights(nodeCount, ccr, 5, false);
			}
		}
	}
	
	static void generateSP()
	{
		for (int nodeCount : nodeCounts)
		{
			for (double ccr : ccrs)
			{
				for (WeightGenerator weightGenerator : weightGenerators)
				{
					DagGenerator.generateSeriesParallel(nodeCount, ccr, weightGenerator, weightGenerator, 3, "3");
					DagGenerator.generateSeriesParallel(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5), 3, "3");
					
					DagGenerator.generateSeriesParallel(nodeCount, ccr, weightGenerator, weightGenerator, 6, "6");
					DagGenerator.generateSeriesParallel(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5), 6, "6");

					DagGenerator.generateSeriesParallel(nodeCount, ccr, weightGenerator, weightGenerator, 9, "9");
					DagGenerator.generateSeriesParallel(nodeCount, ccr, weightGenerator, weightGenerator.copy().setSourceWeightInfluence(0.5), 9, "9");
				}
			}
		}
	}
	
	static void generateForkJoin()
	{
		int sourceWeight = 10;
		int sinkWeight = 10;
		
		WeightGenerator increasingWeightGen = new WeightGenerator() {
			
			int increment = 10;
			int weight = 0;
			
			@Override
			protected String subName() {
				return "increasing";
			}
			
			@Override
			public int get() {
				weight += increment;
				return weight;
			}
			
			@Override
			public WeightGenerator copy() {
				return null;
			}
		};
		
		for (int nodeCount : nodeCounts)
		{
			for (double ccr : ccrs)
			{
				for (WeightGenerator weightGenerator : weightGenerators)
				{
					DagGenerator.generateForkJoin(nodeCount, ccr, weightGenerator, weightGenerator, weightGenerator, sourceWeight, sinkWeight, "1");
					DagGenerator.generateForkJoin(nodeCount, ccr, weightGenerator, weightGenerator, weightGenerator, sourceWeight, sinkWeight, "2");
				}
			}
		}
	}
	
	static void generateStencil()
	{
		for (int nodeCount : nodeCounts)
		{
			for (double ccr : ccrs)
			{
				int side = (int) Math.sqrt(nodeCount);
				DagGenerator.generateStencil(side, side, ccr, new ConstantValueGenerator(100), new ConstantValueGenerator(1));
			}
		}
	}
	
	static void generatePipeline()
	{
		for (int nodeCount : nodeCounts)
		{
			for (double ccr : ccrs)
			{
				int side = (int) Math.sqrt(nodeCount);
				DagGenerator.generatePipeline(side, side, ccr, new ConstantValueGenerator(100), new ConstantValueGenerator(1));
			}
		}
	}
	
	static TaskGraph<TaskVertex, CommEdge<TaskVertex>> multiplyNodeWeights(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph, double factor)
	{
		for (TaskVertex taskVertex : taskGraph.vertices())
		{
			taskVertex.setWeight((int) (taskVertex.weight() * factor));
		}
		return taskGraph;
	}
}
