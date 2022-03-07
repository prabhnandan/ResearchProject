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

public class ListSchedulingMerger<V extends TaskVertex> implements ClusterMerger<V>
{
	boolean ignoreOriginalTaskOrders;
	
	public ListSchedulingMerger(boolean ignoreOriginalClusterOrder)
	{
		this.ignoreOriginalTaskOrders = ignoreOriginalClusterOrder;
	}
	
	@Override
	public Clustering<V> merge(Clustering<V> inputClusters, TaskGraph<V, ?> taskGraph, int targetCount)
	{
		if (targetCount >= inputClusters.clusters().size()) return inputClusters; // different situation if this method has to order it
		
		ClusteredLevelCalculator<V> allocatedLevels = new ClusteredLevelCalculator<>(inputClusters, taskGraph);
		

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
		
		
		Map<Cluster<V>, Integer> clusterMaxBLevels = new HashMap<>();
		Map<V, Integer> snapshotBlevels = new HashMap<>();
		
		for (Cluster<V> cluster : clustering.clusters())
		{
			int maxBLevel = 0;
			for (V task : cluster)
			{
				maxBLevel = Math.max(maxBLevel, allocatedLevels.bottom(task) /*clustering.allocatedBottomLevel(task)*/);
			}
			clusterMaxBLevels.put(cluster, maxBLevel);
		}
		
		Comparator<Cluster<V>> clusterComparator = new Comparator<Cluster<V>>() {
			
			@Override
			public int compare(Cluster<V> A, Cluster<V> B) {

				//return allocatedLevels.bottom(A.first()) - allocatedLevels.bottom(B.first());
				int val = clusterMaxBLevels.get(A) - clusterMaxBLevels.get(B);
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
		
		
		// clusters sorted by priority
		List<ArrayCluster<V>> clusters = new ArrayList<>(clustering.clusters());
		Collections.sort(clusters, clusterComparator);
		// [targetCount] number of clusters with highest priority would occupy a processor each, rest of the clusters would merge onto them
		Set<ArrayCluster<V>> procClusters = new LinkedHashSet<>(clusters.subList(0, targetCount));
		
		for (ArrayCluster<V> cluster : clusters.subList(targetCount, clusters.size()))
		{
			clustering.updateLevels();
			for (V task : taskGraph.vertices())
			{
				snapshotBlevels.put(task, clustering.allocatedBottomLevel(task));
			}
			
			ArrayCluster<V> bestTargetCluster = null;
			ArrayCluster<V> bestMergedCluster = null;
			int bestScheduleLength = Integer.MAX_VALUE;
			
			for (ArrayCluster<V> targetCluster : procClusters)
			{
//				for (V task : cluster)
//				{
//					if (targetCluster.contains(task)) throw new RuntimeException();
//				}
				
				ArrayCluster<V> mergedCluster = new ArrayCluster<>();

//				// merge examined clusters in a zipline kind of way and using the comparator
//				int index = 0;
//				int indexT = 0;
//				while (indexT < targetCluster.size() && index < cluster.size())
//				{
//					if (taskComparator.compare(targetCluster.get(indexT), cluster.get(index)) > 0)
//					{
//						mergedCluster.add(targetCluster.get(indexT++));
//					}
//					else
//					{
//						mergedCluster.add(cluster.get(index++));
//					}
//				}
//				for (; index < cluster.size(); index++)
//				{
//					mergedCluster.add(cluster.get(index));
//				}
//				for (; indexT < targetCluster.size(); indexT++)
//				{
//					mergedCluster.add(targetCluster.get(indexT));
//				}
				
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
				
				// take note of the new schedule length
				clustering.updateLevels();
				
				if (clustering.scheduleLength() < bestScheduleLength)
				{
					bestScheduleLength = clustering.scheduleLength();
					bestTargetCluster = targetCluster;
					bestMergedCluster = mergedCluster;
				}
				
				// assign tasks back to original clusters to try different clusterings
				for (V task : targetCluster)
				{
					clustering.assignCluster(task, targetCluster);
				}
				for (V task : cluster)
				{
					clustering.assignCluster(task, cluster);
				}
				clustering.removeCluster(mergedCluster);
			}
			
			// carry out the best cluster merging
			for (V task : bestTargetCluster)
			{
				clustering.assignCluster(task, bestMergedCluster);
			}
			for (V task : cluster)
			{
				clustering.assignCluster(task, bestMergedCluster);
			}
			
			clustering.removeCluster(cluster);
			clustering.removeCluster(bestTargetCluster);
			// update set of clusters to merge onto
			procClusters.remove(bestTargetCluster);
			procClusters.add(bestMergedCluster);
		}
		
		return clustering;
	}

	@Override
	public String description()
	{
		return "cluster merging by list scheduling (Sarkar)";
	}

	@Override
	public String code()
	{
		return "list";
	}
}
