package parcschedule.schedulers.clusterSchedulers.ClusterMergers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedulers.clusterSchedulers.Clustering.ArrayCluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.Cluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.ClusteredLevelCalculator;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;
import parcschedule.schedulers.clusterSchedulers.Clustering.SimpleClustering;

public class GuidedLoadBalancingMerger<V extends TaskVertex> implements ClusterMerger<V>
{
	boolean ignoreOriginalTaskOrders;
	
	public GuidedLoadBalancingMerger(boolean ignoreOriginalClusterOrder)
	{
		this.ignoreOriginalTaskOrders = ignoreOriginalClusterOrder;
	}
	
	@Override
	public Clustering<V> merge(Clustering<V> inputClusters, TaskGraph<V, ?> taskGraph, int targetCount)
	{
		if (targetCount >= inputClusters.clusters().size()) return inputClusters; 
		
		ClusteredLevelCalculator<V> allocatedLevels = new ClusteredLevelCalculator<>(inputClusters, taskGraph);
		Map<Cluster<V>, Integer> clusterLoads = new HashMap<>();
		
		
		SimpleClustering<V> clustering;
		if (ignoreOriginalTaskOrders)
		{
			clustering = new SimpleClustering<V>(inputClusters, taskGraph, new Comparator<V>() {
				@Override
				public int compare(V A, V B) {
					int val = allocatedLevels.bottom(B) - allocatedLevels.bottom(A);
					return val != 0 ? val : A.index() - B.index();
				}
			});
		}
		else
		{
			clustering = new SimpleClustering<>(inputClusters, taskGraph);
		}
		
		
		Set<ArrayCluster<V>> unmergedClusters = new LinkedHashSet<>(clustering.clusters());
		Set<ArrayCluster<V>> procClusters = new LinkedHashSet<>(targetCount);
		for (int i = 0; i < targetCount; i++)
		{
			ArrayCluster<V> cluster = new ArrayCluster<>();
			procClusters.add(cluster);
			clusterLoads.put(cluster, 0);
		}
		
		for (Cluster<V> cluster : clustering.clusters())
		{
			int load = 0;
			for (V task : cluster)
			{
				load += task.weight();
			}
			clusterLoads.put(cluster, load);
		}
		
		Comparator<Cluster<V>> clusterMinLoadComparator = new Comparator<Cluster<V>>() {
			
			@Override
			public int compare(Cluster<V> A, Cluster<V> B) {

				int val = clusterLoads.get(B) - clusterLoads.get(A);
				if (val != 0)
				{
					return val;
				}
				else
				{
					return A.hashCode() - B.hashCode();
//					if (A.size() != B.size()) return A.size() - B.size();
//					if (A.size() == 0) return 0;
//					return A.first().index() - B.first().index();
				}
			}
		};
		
		Comparator<Cluster<V>> clusterMinEstComparator = new Comparator<Cluster<V>>() {
			
			@Override
			public int compare(Cluster<V> A, Cluster<V> B) {

				int estDiff = clustering.allocatedTopLevel(B.first()) - clustering.allocatedTopLevel(A.first());
				int val = estDiff != 0 ?  estDiff : (allocatedLevels.bottom(A.first()) - allocatedLevels.bottom(B.first()));
				if (val != 0)
				{
					return val;
				}
				else
				{
					return A.hashCode() - B.hashCode();
//					if (A.size() != B.size()) return A.size() - B.size();
//					if (A.size() == 0) return 0;
//					return A.first().index() - B.first().index();
				}
			}
		};
		
		
		Comparator<V> taskComparator;
		if (ignoreOriginalTaskOrders)
		{
			taskComparator = new Comparator<V>() {
				@Override
				public int compare(V A, V B) {
					int val = allocatedLevels.bottom(B) - allocatedLevels.bottom(A);
					return val != 0 ? val : A.index() - B.index();
				}
			};
		}
		else
		{
			taskComparator = new Comparator<V>() {
	
				@Override
				public int compare(V A, V B) {
					int val = clustering.allocatedBottomLevel(B) - clustering.allocatedBottomLevel(A);
					return val != 0 ? val : A.index() - B.index();
				}
			};
		}
		
		
		while (!unmergedClusters.isEmpty())
		{
			
			ArrayCluster<V> cluster = Collections.max(unmergedClusters, clusterMinEstComparator);
			unmergedClusters.remove(cluster);
			ArrayCluster<V> targetCluster = Collections.max(procClusters, clusterMinLoadComparator);
			ArrayCluster<V> mergedCluster = new ArrayCluster<>();

			// merge examined clusters using task comparator
//			int index = 0;
//			int indexT = 0;
//			while (indexT < targetCluster.size() && index < cluster.size())
//			{
//				if (taskComparator.compare(targetCluster.get(indexT), cluster.get(index)) > 0)
//				{
//					mergedCluster.add(targetCluster.get(indexT));
//					indexT++;
//				}
//				else
//				{
//					mergedCluster.add(cluster.get(index));
//					index++;
//				}
//			}
//			for (; index < cluster.size(); index++)
//			{
//				mergedCluster.add(cluster.get(index));
//			}
//			for (; indexT < targetCluster.size(); indexT++)
//			{
//				mergedCluster.add(targetCluster.get(indexT));
//			}
			
			List<V> tasks = new ArrayList<>();
			for (V task : cluster) tasks.add(task);
			for (V task : targetCluster) tasks.add(task);
			Collections.sort(tasks, taskComparator);
			for (V task : tasks) mergedCluster.add(task);
			
			// assign merged cluster to tasks, in the clustering object
			for (V task : mergedCluster)
			{
				clustering.assignCluster(task, mergedCluster);
			}
			
			clustering.updateLevels();
			
			clustering.removeCluster(cluster);
			clustering.removeCluster(targetCluster);
			// update set of clusters to merge onto
			procClusters.remove(targetCluster);
			procClusters.add(mergedCluster);
			// update cluster loads
			clusterLoads.put(mergedCluster, clusterLoads.get(cluster) + clusterLoads.get(targetCluster));
			clusterLoads.remove(targetCluster);
			clusterLoads.remove(cluster);
		}
		
		return clustering;
	}

	@Override
	public String description()
	{
		return "Guided Load Balancing (Radulescu et al.)";
	}

	@Override
	public String code()
	{
		return "glb";
	}
}
