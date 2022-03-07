package parcschedule.schedulers.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import parcschedule.graphGenerator.GXLGraphGenerator;
import parcschedule.schedule.BasicSchedule;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.ClassicHomogeneousSystem;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;
import parcschedule.schedulers.Scheduler;
import parcschedule.schedulers.clusterSchedulers.ClusterScheduler;
import parcschedule.schedulers.clusterSchedulers.ClusterMergers.GuidedLoadBalancingMerger;
import parcschedule.schedulers.clusterSchedulers.ClusterMergers.ListSchedulingMerger;
import parcschedule.schedulers.clusterSchedulers.Clusterers.Clusterer;
import parcschedule.schedulers.clusterSchedulers.Clusterers.DcpClusterer;
import parcschedule.schedulers.clusterSchedulers.Clusterers.DscClusterer;
import parcschedule.schedulers.clusterSchedulers.Clustering.Cluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;
import parcschedule.schedulers.listSchedulers.ListScheduler;
import parcschedule.schedulers.listSchedulers.placementSchemes.AllChildrenLatestEstLookahead;
import parcschedule.schedulers.listSchedulers.placementSchemes.AllChildrenWeightedEstLookahead;
import parcschedule.schedulers.listSchedulers.placementSchemes.CriticalChildEstLookahead;
import parcschedule.schedulers.listSchedulers.placementSchemes.MinEst;
import parcschedule.schedulers.listSchedulers.placementSchemes.MinEstWithCriticalPathOnOneProc;
import parcschedule.schedulers.listSchedulers.priorities.BLevelMinusEstPriority;
import parcschedule.schedulers.listSchedulers.priorities.BLevelPlusTLevelPriority;
import parcschedule.schedulers.listSchedulers.priorities.BLevelPriority;
import parcschedule.schedulers.listSchedulers.priorities.CPBasedBAndTLevelsPriority;
import parcschedule.schedulers.listSchedulers.priorities.CPBasedConstrainingParentsPriority;
import parcschedule.schedulers.listSchedulers.priorities.DlsWithCriticalChildLookahead;
import parcschedule.schedulers.listSchedulers.priorities.EarliestStartTimePriority;
import parcschedule.schedulers.listSchedulers.priorities.EtfWithCriticalChildLookahead;

public class Run {
	
	public static void main(String[] args) throws IOException
	{
		AlgoTest[] algoTests = new AlgoTest[] {
				
				new AlgoTest("bl-est", new ListScheduler<>(new MinEst<>(false), new BLevelPriority<>())),
				new AlgoTest("bl-cle", new ListScheduler<>(new AllChildrenLatestEstLookahead<>(false), new BLevelPriority<>())),
				new AlgoTest("bl-wle", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(false), new BLevelPriority<>())),
				new AlgoTest("bl-cc", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new BLevelPriority<>())),
				
				new AlgoTest("tlbl-est", new ListScheduler<>(new MinEst<>(false), new BLevelPlusTLevelPriority<>())),
				new AlgoTest("tlbl-wle", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(false), new BLevelPlusTLevelPriority<>())),

				new AlgoTest("cpn-est", new ListScheduler<>(new MinEst<>(false), new CPBasedBAndTLevelsPriority<>())),
				new AlgoTest("cpn-wle", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(false), new CPBasedBAndTLevelsPriority<>())),
				new AlgoTest("cpn-cc", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new CPBasedBAndTLevelsPriority<>())),
				
				new AlgoTest("dps-est", new ListScheduler<>(new MinEst<>(false), new CPBasedConstrainingParentsPriority<>())),
				new AlgoTest("dps-wle", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(false), new CPBasedConstrainingParentsPriority<>())),
				new AlgoTest("dps-cc", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new CPBasedConstrainingParentsPriority<>())),
				
				new AlgoTest("bl-est-ins", new ListScheduler<>(new MinEst<>(true), new BLevelPriority<>())),
				new AlgoTest("bl-cle-ins", new ListScheduler<>(new AllChildrenLatestEstLookahead<>(true), new BLevelPriority<>())),
				new AlgoTest("bl-wle-ins", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(true), new BLevelPriority<>())),
				new AlgoTest("bl-cc-ins", new ListScheduler<>(new CriticalChildEstLookahead<>(true), new BLevelPriority<>())),
				new AlgoTest("bl-cpop-ins", new ListScheduler<>(new MinEstWithCriticalPathOnOneProc<>(true), new BLevelPriority<>())),
				
				new AlgoTest("tlbl-wle-ins", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(true), new BLevelPlusTLevelPriority<>())),
				new AlgoTest("tlbl-cpop-ins", new ListScheduler<>(new MinEstWithCriticalPathOnOneProc<>(true), new BLevelPlusTLevelPriority<>())),
				new AlgoTest("tlbl-est-ins", new ListScheduler<>(new MinEst<>(true), new BLevelPlusTLevelPriority<>())),
				
				new AlgoTest("cpn-est-ins", new ListScheduler<>(new MinEst<>(true), new CPBasedBAndTLevelsPriority<>())),
				new AlgoTest("cpn-wle-ins", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(true), new CPBasedBAndTLevelsPriority<>())),
				new AlgoTest("cpn-cc-ins", new ListScheduler<>(new CriticalChildEstLookahead<>(true), new CPBasedBAndTLevelsPriority<>())),
				
				new AlgoTest("dps-est-ins", new ListScheduler<>(new MinEst<>(true), new CPBasedConstrainingParentsPriority<>())),
				new AlgoTest("dps-wle-ins", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(true), new CPBasedConstrainingParentsPriority<>())),
				new AlgoTest("dps-cc-ins", new ListScheduler<>(new CriticalChildEstLookahead<>(true), new CPBasedConstrainingParentsPriority<>())),
				
				new AlgoTest("dls-ins", new ListScheduler<>(new MinEst<>(true), new BLevelMinusEstPriority<>(true))),
				new AlgoTest("dls", new ListScheduler<>(new MinEst<>(false), new BLevelMinusEstPriority<>(false))),
				new AlgoTest("dlscc", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new DlsWithCriticalChildLookahead<>(false))),
//				new AlgoTest("dlscc", new ListScheduler<>(new CriticalChildEstLookahead<>(true), new DlsWithCriticalChildLookahead<>(true))),
				
				new AlgoTest("etf", new ListScheduler<>(new MinEst<>(false), new EarliestStartTimePriority<>())),
				new AlgoTest("etfcc", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new EtfWithCriticalChildLookahead<>(false))),
//				new AlgoTest("etfcc", new ListScheduler<>(new CriticalChildEstLookahead<>(true), new EtfWithCriticalChildLookahead<>(true))),

//
				new AlgoTest("dcp-glb-bl", new ClusterScheduler<>(new DcpClusterer<>(), new GuidedLoadBalancingMerger<>(false), ClusterScheduler.OrderingScheme.blevel)),
////			new AlgoTest("dcp-glb-blx", new ClusterScheduler<>(new DcpClusterer<>(), new GuidedLoadBalancingMerger<>(false), OrderingScheme.outBLevel)),
				new AlgoTest("dcp-glb-etf", new ClusterScheduler<>(new DcpClusterer<>(), new GuidedLoadBalancingMerger<>(false), ClusterScheduler.OrderingScheme.est)),
//////			new AlgoTest("dcp-ls-bl", new ClusterScheduler<>(new DcpClusterer<>(), new ListSchedulingMerger<>(false), OrderingScheme.bLevel)),
//////			new AlgoTest("dcp-ls-blx", new ClusterScheduler<>(new DcpClusterer<>(), new ListSchedulingMerger<>(false), OrderingScheme.outBLevel)),
				new AlgoTest("dcp-ls-etf", new ClusterScheduler<>(new DcpClusterer<>(), new ListSchedulingMerger<>(false), ClusterScheduler.OrderingScheme.est)),
		
//////			new AlgoTest("dcp-glbro-bl", new ClusterScheduler<>(new DcpClusterer<>(), new GuidedLoadBalancingMerger<>(true), OrderingScheme.bLevel)),
//////			new AlgoTest("dcp-glbro-blx", new ClusterScheduler<>(new DcpClusterer<>(), new GuidedLoadBalancingMerger<>(true), OrderingScheme.outBLevel)),
//////			new AlgoTest("dcp-glbro-etf", new ClusterScheduler<>(new DcpClusterer<>(), new GuidedLoadBalancingMerger<>(true), OrderingScheme.est)),
//////			new AlgoTest("dcp-lsro-bl", new ClusterScheduler<>(new DcpClusterer<>(), new ListSchedulingMerger<>(true), OrderingScheme.bLevel)),
//////			new AlgoTest("dcp-lsro-blx", new ClusterScheduler<>(new DcpClusterer<>(), new ListSchedulingMerger<>(true), OrderingScheme.outBLevel)),
//////			new AlgoTest("dcp-lsro-etf", new ClusterScheduler<>(new DcpClusterer<>(), new ListSchedulingMerger<>(true), OrderingScheme.est)),
////			
				new AlgoTest("dcpx-glb-etf", new ClusterScheduler<>(new DcpClusterer<>(true), new GuidedLoadBalancingMerger<>(false), ClusterScheduler.OrderingScheme.est)),
				new AlgoTest("dcpx-ls-etf", new ClusterScheduler<>(new DcpClusterer<>(true), new ListSchedulingMerger<>(false), ClusterScheduler.OrderingScheme.est)),

//				new AlgoTest("dsc-glb-bl", new ClusterScheduler<>(new DscClusterer<>(), new GuidedLoadBalancingMerger<>(false), OrderingScheme.bLevel)),
//				new AlgoTest("dsc-glb-blx", new ClusterScheduler<>(new DscClusterer<>(), new GuidedLoadBalancingMerger<>(false), OrderingScheme.outBLevel)),
				new AlgoTest("dsc-glb-etf", new ClusterScheduler<>(new DscClusterer<>(), new GuidedLoadBalancingMerger<>(false), ClusterScheduler.OrderingScheme.est)),
//				new AlgoTest("dsc-ls-bl", new ClusterScheduler<>(new DscClusterer<>(), new ListSchedulingMerger<>(false), OrderingScheme.bLevel)),
//				new AlgoTest("dsc-ls-blx", new ClusterScheduler<>(new DscClusterer<>(), new ListSchedulingMerger<>(false), OrderingScheme.outBLevel)),
				new AlgoTest("dsc-ls-etf", new ClusterScheduler<>(new DscClusterer<>(), new ListSchedulingMerger<>(false), ClusterScheduler.OrderingScheme.est)),
				
//				new AlgoTest("dsc-glbro-bl", new ClusterScheduler<>(new DscClusterer<>(), new GuidedLoadBalancingMerger<>(true), OrderingScheme.bLevel)),
//				new AlgoTest("dsc-glbro-blx", new ClusterScheduler<>(new DscClusterer<>(), new GuidedLoadBalancingMerger<>(true), OrderingScheme.outBLevel)),
				new AlgoTest("dsc-glbro-etf", new ClusterScheduler<>(new DscClusterer<>(), new GuidedLoadBalancingMerger<>(true), ClusterScheduler.OrderingScheme.est)),
//				new AlgoTest("dsc-lsro-bl", new ClusterScheduler<>(new DscClusterer<>(), new ListSchedulingMerger<>(true), OrderingScheme.bLevel)),
//				new AlgoTest("dsc-lsro-blx", new ClusterScheduler<>(new DscClusterer<>(), new ListSchedulingMerger<>(true), OrderingScheme.outBLevel)),
//				new AlgoTest("dsc-lsro-etf", new ClusterScheduler<>(new DscClusterer<>(), new ListSchedulingMerger<>(true), OrderingScheme.est)),

//				new AlgoTest("ez-glb-bl", new ClusterScheduler<>(new EzClusterer<>(BLevelType.computation), new GuidedLoadBalancingMerger<>(), OrderingScheme.bLevel)),
//				new AlgoTest("ez-glb-etf", new ClusterScheduler<>(new EzClusterer<>(BLevelType.computation), new GuidedLoadBalancingMerger<>(true), OrderingScheme.est)),
//				new AlgoTest("ez-ls-bl", new ClusterScheduler<>(new EzClusterer<>(BLevelType.computation), new ListSchedulingMerger<>(), OrderingScheme.bLevel)),
//				new AlgoTest("ez-ls-etf", new ClusterScheduler<>(new EzClusterer<>(BLevelType.computation), new ListSchedulingMerger<>(true), OrderingScheme.est)),
//				
//				new AlgoTest("lc-glb-bl", new ClusterScheduler<>(new DcpClusterer<>(), new GuidedLoadBalancingMerger<>(), OrderingScheme.bLevel)),
				new AlgoTest("lc-glb-etf", new ClusterScheduler<>(new DcpClusterer<>(), new GuidedLoadBalancingMerger<>(false), ClusterScheduler.OrderingScheme.est)),
//				new AlgoTest("lc-ls-bl", new ClusterScheduler<>(new DcpClusterer<>(), new ListSchedulingMerger<>(), OrderingScheme.bLevel)),
				new AlgoTest("lc-ls-etf", new ClusterScheduler<>(new DcpClusterer<>(), new ListSchedulingMerger<>(false), ClusterScheduler.OrderingScheme.est))

				// clustering
				
//				new AlgoTestClustering("bl-est-nins", new ListScheduler<>(new MinEst<>(false), new BLevelPriority<>())),
//				new AlgoTestClustering("bl-cle-nins", new ListScheduler<>(new AllChildrenLatestEstLookahead<>(false), new BLevelPriority<>())),
//				new AlgoTestClustering("bl-wle-nins", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(false), new BLevelPriority<>())),
//				new AlgoTestClustering("bl-cc-nins", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new BLevelPriority<>())),
//				
//				new AlgoTestClustering("bl-est", new ListScheduler<>(new MinEst<>(true), new BLevelPriority<>())),
//				new AlgoTestClustering("bl-cle", new ListScheduler<>(new AllChildrenLatestEstLookahead<>(true), new BLevelPriority<>())),
//				new AlgoTestClustering("bl-wle", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(true), new BLevelPriority<>())),
//				new AlgoTestClustering("bl-cc", new ListScheduler<>(new CriticalChildEstLookahead<>(true), new BLevelPriority<>())),
//				new AlgoTestClustering("bl-cpop", new ListScheduler<>(new MinEstWithCriticalPathOnOneProc<>(true), new BLevelPriority<>())),
//				
//				new AlgoTestClustering("tlbl-est-nins", new ListScheduler<>(new MinEst<>(false), new BLevelPlusTLevelPriority<>())),
//				new AlgoTestClustering("tlbl-wle-nins", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(false), new BLevelPlusTLevelPriority<>())),
//				new AlgoTestClustering("tlbl-wle", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(true), new BLevelPlusTLevelPriority<>())),
//				new AlgoTestClustering("tlbl-cpop", new ListScheduler<>(new MinEstWithCriticalPathOnOneProc<>(true), new BLevelPlusTLevelPriority<>())),
//				new AlgoTestClustering("tlbl-est", new ListScheduler<>(new MinEst<>(true), new BLevelPlusTLevelPriority<>())),
//				
//				new AlgoTestClustering("cpn-est-nins", new ListScheduler<>(new MinEst<>(false), new CPBasedBAndTLevelsPriority<>())),
//				new AlgoTestClustering("cpn-wle-nins", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(false), new CPBasedBAndTLevelsPriority<>())),
//				new AlgoTestClustering("cpn-cc-nins", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new CPBasedBAndTLevelsPriority<>())),
//				new AlgoTestClustering("cpn-est", new ListScheduler<>(new MinEst<>(true), new CPBasedBAndTLevelsPriority<>())),
//				new AlgoTestClustering("cpn-wle", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(true), new CPBasedBAndTLevelsPriority<>())),
//				new AlgoTestClustering("cpn-cc", new ListScheduler<>(new CriticalChildEstLookahead<>(true), new CPBasedBAndTLevelsPriority<>())),
//				
//				new AlgoTestClustering("dps-est-nins", new ListScheduler<>(new MinEst<>(false), new CPBasedConstrainingParentsPriority<>())),
//				new AlgoTestClustering("dps-wle-nins", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(false), new CPBasedConstrainingParentsPriority<>())),
//				new AlgoTestClustering("dps-cc-nins", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new CPBasedConstrainingParentsPriority<>())),
//				new AlgoTestClustering("dps-est", new ListScheduler<>(new MinEst<>(true), new CPBasedConstrainingParentsPriority<>())),
//				new AlgoTestClustering("dps-wle", new ListScheduler<>(new AllChildrenWeightedEstLookahead<>(true), new CPBasedConstrainingParentsPriority<>())),
//				new AlgoTestClustering("dps-cc", new ListScheduler<>(new CriticalChildEstLookahead<>(true), new CPBasedConstrainingParentsPriority<>())),
//				
//				new AlgoTestClustering("etf-nins", new ListScheduler<>(new MinEst<>(false), new EarliestStartTimePriority<>())),
//				new AlgoTestClustering("etfcc-nins", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new EtfWithCriticalChildLookahead<>(false))),
//				
//				new AlgoTestClustering("dls-nins", new ListScheduler<>(new MinEst<>(false), new BLevelMinusEstPriority<>(false))),
//				new AlgoTestClustering("dlscc-nins", new ListScheduler<>(new CriticalChildEstLookahead<>(false), new DlsWithCriticalChildLookahead<>(false))),
//				new AlgoTestClustering("dls", new ListScheduler<>(new MinEst<>(true), new BLevelMinusEstPriority<>(true))),
				
//				new AlgoTestClustering("dsc", new DscClusterer<>()),
//				new AlgoTestClustering("dcp", new DcpClusterer<>()),
//				new AlgoTestClustering("dcpx", new DcpClusterer<>(true)),
//				new AlgoTestClustering("lc", new LcClusterer<>()),
//				new AlgoTestClustering("ez", new EzClusterer<>(BLevelType.computation)),
//				new AlgoTestClustering("ezx", new EzClusterer<>(BLevelType.allocated))
		};
		
		GraphSet[] graphSets = new GraphSet[] {
				
//				new GraphSet("Random"),
//				new GraphSet("Random (fan-in fan-out)"),
//				new GraphSet("Random (intersecting total orders)"),
				new GraphSet("Random (layer by layer)"),
				
//				new GraphSet("Fork-join"),
//				new GraphSet("In Tree"),
//				new GraphSet("Out Tree"),
//				new GraphSet("Series Parallel"),
//				
//				new GraphSet("Inspiral"),
//				new GraphSet("Montage"),
//				new GraphSet("Sipht"),
//				new GraphSet("Epigenomics"),
//				new GraphSet("CyberShake"),
//				
//				new GraphSet("Pipeline", new int[] {49, 100, 196, 484, 961}),
//				new GraphSet("FFT", new int[] {39, 95, 223, 511, 1151}),
//				new GraphSet("Stencil", new int[] {49, 100, 196, 484, 961}),
//				new GraphSet("Gauss", new int[] {35, 135, 527}),
//				new GraphSet("Cholesky", new int[] {56, 120, 220, 560, 1140}),
//
//				new GraphSet("robot", new int[] {86}),
//				new GraphSet("fpppp", new int[] {332}),
//				new GraphSet("sparse", new int[] {94}),
				
		};
		
		
		
		double[] ccrs = new double[] {0.1, 1, 10};
		
		int[] procCounts = new int[] {2, 8, 32, 128, 256, 512};
//		int[] procCounts = new int[] {-1};
		
		
		for (GraphSet graphSet : graphSets)
		{
//			Result<Double> resultNormalisedAveraged = new Result<>();
			Result<List<Double>> resultNormalised = new Result<>();
//			Result<List<Double>> resultNormalisedToBest = new Result<>();
			
//			Map<AlgoTest, Map<Double, Map<Integer, Map<Integer, Integer>>>> averagedResults = new HashMap<>();
//			Map<AlgoTest, Map<Double, Map<Integer, Map<Integer, List<Double>>>>> resultsNslBest = new HashMap<>();
//			Map<AlgoTest, Map<Double, Map<Integer, Map<Integer, List<Double>>>>> resultsNslLowerBound = new HashMap<>();
			// algorithm, ccr, node count, processor count
			
//			for (AlgoTest test : algoTests)
//			{
//				Map<Double, Map<Integer, Map<Integer, Integer>>> mapAveraged = new HashMap<>();
//				Map<Double, Map<Integer, Map<Integer, List<Double>>>> mapNslBest = new HashMap<>();
//				Map<Double, Map<Integer, Map<Integer, List<Double>>>> mapNslLowerBound = new HashMap<>();
//				for (double ccr : ccrs)
//				{
//					Map<Integer, Map<Integer, Integer>> map1Averaged = new HashMap<>();
//					Map<Integer, Map<Integer, List<Double>>> map1NslBest = new HashMap<>();
//					Map<Integer, Map<Integer, List<Double>>> map1NslLowerBound = new HashMap<>();
//					for (int nodeCount : graphSet.nodeCounts)
//					{
//						map1Averaged.put(nodeCount, new HashMap<>());
//						Map<Integer, List<Double>> map2NslBest = new HashMap<>();
//						Map<Integer, List<Double>> map2NslLowerBound = new HashMap<>();
//						for (int procCount : procCounts)
//						{
//							map2NslBest.put(procCount, new ArrayList<>());
//							map2NslLowerBound.put(procCount, ArrayList<>());
//						}
//						map1NslBest.put(nodeCount, map2NslBest);
//						map1NslLowerBound.put(nodeCount, map2NslLowerBound);
//					}
//					map.put(ccr, map1);
//				}
//				averagedResults.put(test, averagedResults);
//				averagedResults.put(test, averagedResults);
//				averagedResults.put(test, averagedResults);
//			}
			
			for (double ccr : ccrs)
			{
				for (int nodeCount : graphSet.nodeCounts)
				{
					System.out.println(graphSet.name + " ccr " + ccr + " nodes " + nodeCount);
					
					List<TaskGraph<TaskVertex, CommEdge<TaskVertex>>> taskGraphs = new ArrayList<>();
//					Map<TaskGraph<TaskVertex, CommEdge<TaskVertex>>, Map<Integer, Integer>> lowerBounds = new HashMap<>();
					String path = "DAGS/" + graphSet.name + "/Nodes " + nodeCount + "/CCR " + ccr;
					for (File file : FileUtils.listFiles(new File(path), FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter()))
					{	
						TaskGraph<TaskVertex, CommEdge<TaskVertex>> graph = GXLGraphGenerator.generate(file, null);
						taskGraphs.add(graph);
//						lowerBounds.put(graph, new HashMap<>());
//						System.out.println(++graphCount);
					}
					
					for (int procCount : procCounts)
					{
						if (procCount < nodeCount)
						{
							TargetSystem<Proc> system = null;
							if (procCount > 0)
							{
								system = new ClassicHomogeneousSystem(procCount);
							}
							
							for (AlgoTest test : algoTests)
							{
								resultNormalised.put(test.name, ccr, nodeCount, procCount, new ArrayList<>());
//								resultNormalisedToBest.put(test.name, ccr, nodeCount, procCount, new ArrayList<>());
							}
							
							for (TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph : taskGraphs)
							{
								System.out.println(procCount + taskGraph.name());
								Map<AlgoTest, Integer> localResults = new HashMap<>();
								int bestSL = Integer.MAX_VALUE;
								
								if (procCount <= 0)
								{
									system = new ClassicHomogeneousSystem(taskGraph.sizeVertices());
								}
								double lowerBound = procCount > 0 ? lowerBound(taskGraph, procCount) : maxCompBlevel(taskGraph);
								
								for (AlgoTest algo : algoTests)
								{
									long startTime = System.nanoTime();
									int sl = algo.getSL(taskGraph, system);
									long timeElapsed = (System.nanoTime() - startTime) / 1000000000;
									if (timeElapsed > 1) System.out.println(algo.name() + " " + timeElapsed);
									
									if (sl < lowerBound) throw new RuntimeException();
									localResults.put(algo, sl);
									bestSL = Math.min(sl, bestSL);
									resultNormalised.get(algo.name, ccr, nodeCount, procCount).add(sl / lowerBound);
								}
								
//								for (AlgoTest algo : algoTests)
//								{
//									resultNormalisedToBest.get(algo.name, ccr, nodeCount, procCount).add(localResults.get(algo) / (double) bestSL);
//								}
							}
							
//							for (AlgoTest algo : algoTests)
//							{
//								double nslSum = 0;
//								int n = 0;
//								for (double nsl : resultNormalised.get(algo.name, ccr, nodeCount, procCount))
//								{
//									nslSum += nsl;
//									n += 1;
//								}
//								resultNormalisedAveraged.put(algo.name, ccr, nodeCount, procCount, nslSum / n);
//							}
						}
					}
				}
			}

			
			Integer hashOfAlgorithmsToBePatched = 657129304;
			AlgoTest[] patchedAlgorithms = algoTests;
			
			String dirName = "RESULTS/Complete data points/Normalised to lower bound";
			String fileName = String.format("%s_NslLowerBound_hash%d.csv", graphSet.name, hashOfAlgorithmsToBePatched); //algoTests.hashCode());
			File file = new File(dirName + "/" + fileName);
			if (file.exists() && !file.isDirectory())
			{
				List<AlgoTest> algorithmsInFile = new ArrayList<>();
				boolean algorithmsRead = false;
				BufferedReader br = new BufferedReader(new FileReader(file));
				
			    String line;
			    while ((line = br.readLine()) != null) 
			    {
			    	String[] words = line.split(",");
			    	if (!line.isEmpty())
			    	{
			    		String tag = words[0];
				    	String algoName = tag.substring(0, tag.indexOf("-R"));
			    		
			    		if (!algorithmsRead)
			    		{
			    			algorithmsInFile.add(new AlgoTest(algoName, null));
			    		}
			    		
			    		boolean isPatchedAlgorithm = false;
			    		for (AlgoTest algorithm : patchedAlgorithms)
			    		{
			    			if (algorithm.name.equals(algoName))
			    			{
			    				isPatchedAlgorithm = true;
			    				break;
			    			}
			    		}
			    		
			    		if (!isPatchedAlgorithm)
			    		{
					    	List<Double> data = new ArrayList<>();
					    	for (int i = 1; i < words.length; i++)
					    	{
					    		data.add(Double.parseDouble(words[i]));
					    	}
					    	int ccrIndex = tag.indexOf("-R") + 2;
					    	double ccr = Double.parseDouble(tag.substring(ccrIndex, tag.indexOf("-", ccrIndex)));
					    	int nCountIndex = tag.indexOf("-N") + 2;
					    	int nodeCount = Integer.parseInt(tag.substring(nCountIndex, tag.indexOf("-", nCountIndex)));
					    	int pCountIndex = tag.indexOf("-P") + 2;
					    	int procCount = Integer.parseInt(tag.substring(pCountIndex, tag.length() - 1));// tag.indexOf("-", pCountIndex))); kept "-" for a reason
					    	
					    	resultNormalised.put(algoName, ccr, nodeCount, procCount, data);
			    		}
			    	}
			    	else if (!algorithmsRead)
		    		{
			    		algorithmsRead = true;
			    		algoTests = new AlgoTest[algorithmsInFile.size()];
			    		for (int i = 0; i < algoTests.length; i++)
			    		{
			    			algoTests[i] = algorithmsInFile.get(i);
			    		}
		    		}
			    }
			    
			    br.close();
			}
			else
			{
				@SuppressWarnings("unused")
				String a = "warning";
			}
			
			Files.createDirectories(Paths.get(dirName));
			PrintWriter writer = new PrintWriter(new FileOutputStream(dirName + "/" + fileName, false), true);
			for (double ccr : ccrs)
			{
				for (int nodeCount : graphSet.nodeCounts)
				{
					for (int procCount : procCounts)
					{
						if (procCount < nodeCount)
						{
							for (AlgoTest algo : algoTests)
							{
								writer.printf("%s-R%.2f-N%d-P%d-", algo.name, ccr, nodeCount, procCount);
								for (double nsl : resultNormalised.get(algo.name, ccr, nodeCount, procCount))
								{
									writer.printf(",%.4f", nsl);
								}
								writer.println();
							}
							writer.println();
						}
					}
				}
			}
			writer.close();
			
			
			
			
			
//			dirName = "RESULTS/Complete data points/Normalised to best result";
//			fileName = String.format("%s_NslBestResult_hash%d.csv", graphSet.name, hashOfAlgorithmsToBePatched); //algoTests.hashCode());
//			file = new File(dirName + "/" + fileName);
//			if (file.exists() && !file.isDirectory())
//			{
//				List<AlgoTest> newAlgoTests = new ArrayList<>();
//				boolean algorithmsRead = false;
//				try (BufferedReader br = new BufferedReader(new FileReader(file))) 
//				{
//				    String line;
//				    while ((line = br.readLine()) != null) 
//				    {
//				    	String[] words = line.split(",");
//				    	if (!line.isEmpty())
//				    	{
//				    		String tag = words[0];
//					    	String algoName = tag.substring(0, tag.indexOf("-R"));
//				    		
//				    		if (!algorithmsRead)
//				    		{
//				    			newAlgoTests.add(new AlgoTest(algoName, null));
//				    		}
//
//				    		if (!algoName.equals("dsc"))
//				    		{
//						    	List<Double> data = new ArrayList<>();
//						    	for (int i = 1; i < words.length; i++)
//						    	{
//						    		data.add(Double.parseDouble(words[i]));
//						    	}
//						    	int ccrIndex = tag.indexOf("-R") + 2;
//						    	double ccr = Double.parseDouble(tag.substring(ccrIndex, tag.indexOf("-", ccrIndex)));
//						    	int nCountIndex = tag.indexOf("-N") + 2;
//						    	int nodeCount = Integer.parseInt(tag.substring(nCountIndex, tag.indexOf("-", nCountIndex)));
//						    	int pCountIndex = tag.indexOf("-P") + 2;
//						    	int procCount = Integer.parseInt(tag.substring(pCountIndex)); //, tag.indexOf("-", pCountIndex)));
//						    	
//						    	resultNormalisedToBest.put(algoName, ccr, nodeCount, procCount, data);
//				    		}
//				    	}
//				    	else if (!algorithmsRead)
//			    		{
//				    		algorithmsRead = true;
//				    		algoTests = new AlgoTest[newAlgoTests.size()];
//				    		for (int i = 0; i < algoTests.length; i++)
//				    		{
//				    			algoTests[i] = newAlgoTests.get(i);
//				    		}
//			    		}
//				    }
//				}
//			}
//			
//			Files.createDirectories(Paths.get(dirName));
//			writer = new PrintWriter(new FileOutputStream(dirName + "/" + fileName, false), true);
//			for (double ccr : ccrs)
//			{
//				for (int nodeCount : graphSet.nodeCounts)
//				{
//					for (int procCount : procCounts)
//					{
//						if (procCount < nodeCount)
//						{
//							for (AlgoTest algo : algoTests)
//							{
//								writer.printf("%s-R%.2f-N%d-P%d-", algo.name, ccr, nodeCount, procCount);
//								for (double nsl : resultNormalisedToBest.get(algo.name, ccr, nodeCount, procCount))
//								{
//									writer.printf(",%.4f", nsl);
//								}
//								writer.println();
//							}
//							writer.println();
//						}
//					}
//				}
//			}
//			writer.close();
			
			algoTests = patchedAlgorithms;
			
//			for (double ccr : ccrs)
//			{
//				for (int procCount : procCounts)
//				{
//					dirName = "RESULTS/Averaged/proc count categorised - CCR " + ccr;
//					fileName = String.format("%s_CCR%f_Procs%d_hash%d.csv", graphSet.name, ccr, procCount, algoTests.hashCode());
//					
//					Files.createDirectories(Paths.get(dirName));
//					writer = new PrintWriter(new FileOutputStream(dirName + "/" + fileName), true);
//					
//					writer.print("nodeCounts");
//					for (int count : graphSet.nodeCounts)
//					{
//						if (count > procCount) writer.print("," + count);
//					}
//					writer.println();
//					
//					for (AlgoTest algoTest : algoTests)
//					{
//						writer.print(algoTest.name());
//						for (int nodeCount : graphSet.nodeCounts)
//						{
//							if (nodeCount > procCount)
//							{
//								writer.printf(",%.4f", resultNormalisedAveraged.get(algoTest.name, ccr, nodeCount, procCount));
//							}
//						}
//						writer.println();
//					}
//					
//					writer.close();
//				}
//			}
			
			
//			dirName = "RESULTS/Averaged";
//			fileName = String.format("%s_averaged_hash%d.csv", graphSet.name, algoTests.hashCode());
//			
//			Files.createDirectories(Paths.get(dirName));
//			writer = new PrintWriter(new FileOutputStream(dirName + "/" + fileName), true);
//			
//			for (double ccr : ccrs)
//			{
//				writer.println("CCR " + ccr);
//				writer.print("nodeCounts");
//				for (int count : graphSet.nodeCounts)
//				{
//					writer.print("," + count);
//				}
//				writer.println();
//				
//				for (AlgoTest algoTest : algoTests)
//				{					
//					writer.print(algoTest.name());
//					for (int nodeCount : graphSet.nodeCounts)
//					{
//						int noOfProcCounts = 0;
//						double nslSum = 0;
//						for (int procCount : procCounts)
//						{
//							if (procCount < nodeCount)
//							{
//								nslSum += resultNormalisedAveraged.get(algoTest.name, ccr, nodeCount, procCount);
//								noOfProcCounts += 1;
//							}
//						}
//						double averagedNsl = nslSum / noOfProcCounts;
//						writer.printf(",%.4f", averagedNsl);
//					}
//					writer.println();
//				}
//			}
//			writer.close();
		}
	}
	
	static void checkScheduleValid(BasicSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule)
	{
		for (TaskVertex task : schedule.taskGraph.tasks())
		{
			if (schedule.taskStartTime(task) < schedule.drtOnProc(task, schedule.taskProcAllocation(task)))
			{
				throw new RuntimeException(task.name() + " begins before drt" + schedule.taskGraph.name());
			}
		}
		Set<TaskVertex> taskListsUnion = new HashSet<>();
		for (Proc proc : schedule.system.processors())
		{
			List<TaskVertex> taskList = schedule.procTaskList(proc);
			for (int i = 0; i < taskList.size() - 1; i++)
			{
				TaskVertex task = taskList.get(i);
				if (taskListsUnion.contains(task))
				{
					throw new RuntimeException(task.name() + " appears in multiple task lists" + schedule.taskGraph.name());
				}
				taskListsUnion.add(task);
				
				if (schedule.taskStartTime(task) + task.weight() > schedule.taskStartTime(taskList.get(i + 1)))
				{
					throw new RuntimeException("possible execution overlap" + schedule.taskGraph.name()); // or incorrect task list ordering
				}
			}
		}
	}
	
	static int lowerBound(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph, int procCount)
	{
		return (int) Math.max(maxCompBlevel(taskGraph), taskGraph.getTotalComputationCost() / procCount);
	}
	static int maxCompBlevel(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph)
	{
		int computationCpLength = 0;
		for (TaskVertex taskVertex : taskGraph.vertices())
		{
			computationCpLength = (int) Math.max(computationCpLength, taskGraph.getBottomLevelComp(taskVertex));
		}
		return computationCpLength;
	}
	
}

class Result<T>
{
	Map<String, T> values = new HashMap<>();
	
	public T get(String algorithm, double ccr, int nodeCount, int procCount)
	{
		return values.get(algorithm + ccr + nodeCount + procCount);
	}
	
	public boolean put(String algorithm, double ccr, int nodeCount, int procCount, T value)
	{
		return null != values.put(algorithm + ccr + nodeCount + procCount, value);
	}
}

class AlgoTest
{
	String name;
	Scheduler<TaskVertex, CommEdge<TaskVertex>, Proc> scheduler;
	
	public AlgoTest(String name, Scheduler<TaskVertex, CommEdge<TaskVertex>, Proc> scheduler) {
		this.name = name;
		this.scheduler = scheduler;
	}
	
	public String name()
	{
		return name;
	}
	
	public int getSL(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph, TargetSystem<Proc> system)
	{
		BasicSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule = (BasicSchedule<TaskVertex, CommEdge<TaskVertex>, Proc>) scheduler.schedule(taskGraph, system);
		Run.checkScheduleValid(schedule);
		return schedule.scheduleLength();
	}
}

class AlgoTestClustering extends AlgoTest
{
	ListScheduler<TaskVertex, CommEdge<TaskVertex>, Proc> listScheduler;
	Clusterer<TaskVertex> clusterer;
	
	public AlgoTestClustering(String name, Clusterer<TaskVertex> clusterer)
	{
		super(name, null);
		this.clusterer = clusterer;
	}
	
	public AlgoTestClustering(String name, ListScheduler<TaskVertex, CommEdge<TaskVertex>, Proc> listScheduler)
	{
		super(name, null);
		this.listScheduler = listScheduler;
	}
	
	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
	
	@Override
	public int getSL(TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph, TargetSystem<Proc> system)
	{
		if (clusterer == null)
		{
			if (system.processors().size() < taskGraph.sizeVertices())
			{
				system = new ClassicHomogeneousSystem(taskGraph.sizeVertices());
			}
			BasicSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule = listScheduler.schedule(taskGraph, system);
			Run.checkScheduleValid(schedule);
			return schedule.scheduleLength();
		}
		else
		{
			return clusterLength(clusterer.cluster(taskGraph), taskGraph);
		}
	}

	static int clusterLength(Clustering<TaskVertex> clustering, TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph)
	{
		Map<TaskVertex, TaskVertex> taskBefore = new HashMap<>();
		Map<TaskVertex, Integer> taskStartTimes = new HashMap<>();
		
		for (Cluster<TaskVertex> cluster : clustering.clusters())
		{
			TaskVertex prevTask = null;
			for (TaskVertex task : cluster)
			{
				if (prevTask != null)
				{
					taskBefore.put(task, prevTask);
				}
				prevTask = task;
			}
		}
		
		Set<TaskVertex> expandedVertices = new HashSet<>();
		Set<TaskVertex> finishedVertices = new HashSet<>();
		List<TaskVertex> orderedVertices = new ArrayList<>(taskGraph.sizeVertices());
		
		Stack<TaskVertex> stack = new Stack<>();
		for (TaskVertex vertex : taskGraph.vertices())
		{
			if (!finishedVertices.contains(vertex))
			{
				stack.add(vertex);
			}
			
			while (!stack.empty())
			{
				TaskVertex task = stack.peek();
				if (expandedVertices.contains(task))
				{
					if (! finishedVertices.contains(task))
					{
						orderedVertices.add(task);
						finishedVertices.add(task);
					}
					stack.pop();
				}
				else
				{
					Cluster<TaskVertex> cluster = clustering.cluster(task);
					TaskVertex precedingTask = cluster == null ? null : taskBefore.get(task);
					boolean precedingTaskIsAParent = false;
					
					for (TaskVertex parent : taskGraph.parents(task))
					{
						if (! finishedVertices.contains(parent))
						{
							stack.add(parent);
						}
						if (parent == precedingTask)
						{
							precedingTaskIsAParent = true;
						}
					}
					
					if ( !precedingTaskIsAParent && precedingTask != null && !finishedVertices.contains(precedingTask))
					{
						stack.add(precedingTask);
					}
					
					expandedVertices.add(task);
					continue;
				}
			}
		}
		
		int scheduleLength = 0;

		for (int i = 0; i < orderedVertices.size(); i++)
		{
			int tLevel = 0;
			TaskVertex task = orderedVertices.get(i);
			Cluster<TaskVertex> cluster = clustering.cluster(task);
			for (CommEdge<TaskVertex> inEdge : taskGraph.inEdges(task))
			{
				int edgeFinishTime = taskStartTimes.get(inEdge.from()) + inEdge.from().weight();
				
				if (clustering.cluster(inEdge.from()) != cluster || cluster == null)
				{
					edgeFinishTime += inEdge.weight();
				}
				
				if (edgeFinishTime > tLevel)
				{
					tLevel = edgeFinishTime;
				}
			}
			TaskVertex precedingTask = cluster == null ? null : taskBefore.get(task);
			if (precedingTask != null)
			{
				tLevel = Math.max(tLevel, taskStartTimes.get(precedingTask) + precedingTask.weight());
			}
			taskStartTimes.put(task, tLevel);
			scheduleLength = Math.max(scheduleLength, tLevel + task.weight());
		}
		
		return scheduleLength;
	}
}

class GraphSet
{
	String name;
	int[] nodeCounts = new int[] {50, 100, 200, 500, /*1000*/};
	
	public GraphSet(String name)
	{
		this.name = name;
	}
	
	public GraphSet(String name, int[] nodeCounts)
	{
		this.name = name;
		this.nodeCounts = nodeCounts;
	}
}