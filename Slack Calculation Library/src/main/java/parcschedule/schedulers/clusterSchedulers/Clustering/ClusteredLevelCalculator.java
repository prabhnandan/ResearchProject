package parcschedule.schedulers.clusterSchedulers.Clustering;

import java.util.HashMap;
import java.util.Map;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

/**
 * This class calculates allocated levels without virtual edges. 
 * SimpleClustering's allocated levels use virtual edges, which is the same as building a valid schedule, 
 * with t-level being the EST and b-level being scheduleLength - LST. Levels calculated this way are required by DCP, 
 * and their calculation also provide a schedule length.
 * 
 * however, allocated levels without virtual edges are needed for ordering tasks when merging clusters/re-ordering tasks, 
 * where existing orders should be ignored but SimpleClustering's allocated levels with virtual edges are based on those existing orders. 
 */
public class ClusteredLevelCalculator<V extends TaskVertex>
{
	TaskGraph<V, ?> taskGraph;
	Clustering<V> clustering;
	Map<TaskVertex, Integer> bLevels = new HashMap<>();
	Map<TaskVertex, Integer> tLevels = new HashMap<>();
	
	public ClusteredLevelCalculator(Clustering<V> clustering, TaskGraph<V, ?> taskGraph)
	{
		this.clustering = clustering;
		this.taskGraph = taskGraph;
		update();
	}
	
	public void update()
	{
		for (V task : taskGraph.topologicalOrder())
		{
			int tLevel = 0;
			for (CommEdge<V> inEdge : taskGraph.inEdges(task))
			{
				int edgeFinishTime = tLevels.get(inEdge.from()) + inEdge.from().weight();
				if (! clustering.cluster(task).contains(inEdge.from()))
				{
					edgeFinishTime += inEdge.weight();
				}
				
				tLevel = Math.max(tLevel, edgeFinishTime);
			}
			tLevels.put(task, tLevel);
		}
		
		for (V task : taskGraph.inverseTopologicalOrder())
		{
			int bLevel = 0;
			for (CommEdge<V> outEdge : taskGraph.outEdges(task))
			{
				int remainingCost = bLevels.get(outEdge.to());
				if (! clustering.cluster(task).contains(outEdge.to()))
				{
					remainingCost += outEdge.weight();
				}
				
				bLevel = Math.max(bLevel, remainingCost + task.weight());
			}
			bLevels.put(task, bLevel);
		}
	}
	
	public int top(TaskVertex task)
	{
		return tLevels.get(task);
	}
	
	public int bottom(TaskVertex task)
	{
		return bLevels.get(task);
	}
}
