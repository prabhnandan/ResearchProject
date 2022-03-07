package parcschedule.schedulers.clusterSchedulers.Clusterers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedulers.clusterSchedulers.Clustering.ArrayCluster;
import parcschedule.schedulers.clusterSchedulers.Clustering.Clustering;
import parcschedule.schedulers.clusterSchedulers.Clustering.SimpleClustering;

public class EzClusterer<V extends TaskVertex> implements Clusterer<V>
{
	BLevelType bLevelType;
	public EzClusterer(BLevelType bLevelType)
	{
		this.bLevelType = bLevelType;
	}

	@Override
	public Clustering<V> cluster(TaskGraph<V, ?> taskGraph)
	{
		SimpleClustering<V> clustering = new SimpleClustering<>(taskGraph);
		for (V task : taskGraph.vertices())
		{
			ArrayCluster<V> cluster = new ArrayCluster<>();
			cluster.add(task);
			clustering.assignCluster(task, cluster);
		}
		List<CommEdge<V>> edges = new ArrayList<>();
		
		BLevel<V> bLevel = null;
		if (bLevelType == BLevelType.allocated)
		{
			bLevel = new BLevel<V>() {
				
				@Override
				public long get(V task) 
				{
					return clustering.allocatedBottomLevel(task);
				}
			};
		}
		else if (bLevelType == BLevelType.normal)
		{
			bLevel = new BLevel<V>() {
				
				@Override
				public long get(V task) 
				{
					return taskGraph.getBottomLevel(task);
				}
			};
		}
		else if (bLevelType == BLevelType.computation)
		{
			bLevel = new BLevel<V>() {
				
				@Override
				public long get(V task) 
				{
					return taskGraph.getBottomLevelComp(task);
				}
			};
		}
		
		for (CommEdge<V> edge : taskGraph.edges())
		{
			edges.add(edge);
		}
		Collections.sort(edges, new Comparator<CommEdge<V>>() {
			
			@Override
			public int compare(CommEdge<V> A, CommEdge<V> B) 
			{
				int val = A.weight() - B.weight();
				return val != 0 ? val : A.from().index() - B.from().index();
			}
		});
		
		for (CommEdge<V> edge : edges)
		{
			ArrayCluster<V> clusterA = clustering.cluster(edge.from());
			ArrayCluster<V> clusterB = clustering.cluster(edge.to());
			
			if (clusterA == clusterB)
			{
				continue;
			}
			
			ArrayCluster<V> mergedCluster = new ArrayCluster<>();
			
			int indexA = 0;
			int indexB = 0;
			while (indexA < clusterA.size() && indexB < clusterB.size())
			{
				if (bLevel.get(clusterA.get(indexA)) > bLevel.get(clusterB.get(indexB)))
				{
					mergedCluster.add(clusterA.get(indexA++));
				}
				else
				{
					mergedCluster.add(clusterB.get(indexB++));
				}
			}
			
			for (; indexB < clusterB.size(); indexB++)
			{
				mergedCluster.add(clusterB.get(indexB));
			}
			for (; indexA < clusterA.size(); indexA++)
			{
				mergedCluster.add(clusterA.get(indexA));
			}
			
			for (V task : clusterA)
			{
				clustering.assignCluster(task, mergedCluster);
			}
			for (V task : clusterB)
			{
				clustering.assignCluster(task, mergedCluster);
			}
			
			int scheduleLengthBefore = clustering.scheduleLength();
			clustering.updateLevels();
			int scheduleLengthAfter = clustering.scheduleLength();
			if (scheduleLengthAfter <= scheduleLengthBefore)
			{
				clustering.removeCluster(clusterA);
				clustering.removeCluster(clusterB);
			}
			else
			{
				for (V task : clusterA)
				{
					clustering.assignCluster(task, clusterA);
				}
				for (V task : clusterB)
				{
					clustering.assignCluster(task, clusterB);
				}
				clustering.removeCluster(mergedCluster);
			}
		}
		
		return clustering;
	}
	
	interface BLevel<V extends TaskVertex>
	{
		long get(V task);
	}

	public enum BLevelType
	{
		normal,
		allocated,
		computation
	}

	@Override
	public String description()
	{
		return "Edge Zeroing (Sarkar)";
	}

	@Override
	public String code()
	{
		return "ez";
	}
}
