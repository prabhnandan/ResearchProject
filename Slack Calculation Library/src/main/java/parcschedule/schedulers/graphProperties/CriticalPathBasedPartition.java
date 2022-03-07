package parcschedule.schedulers.graphProperties;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

public class CriticalPathBasedPartition<V extends TaskVertex>
{
	public enum NodeType {OBN, IBN, CPN}
	
	private Map<TaskVertex, NodeType> nodeTypes = new HashMap<>();
	private List<V> CriticalPathNodes = new LinkedList<>();
	private List<V> outBranchNodes = new LinkedList<>();
	
	public CriticalPathBasedPartition(final TaskGraph<V, ?> taskGraph) 
	{
		// critical path length is highest b-level among source nodes
		long CPLength = 0;
		for (V task : taskGraph.sources())
		{
			CPLength = Math.max(taskGraph.getBottomLevel(task), CPLength);
		}
		
		for (V task : taskGraph.inverseTopologicalOrder())
		{
			// task is a CPN if its b-level + t-level is CP length
			if (taskGraph.getTopLevel(task) + taskGraph.getBottomLevel(task) == CPLength)
			{
				nodeTypes.put(task, NodeType.CPN);
				CriticalPathNodes.add(task);
			}
			else
			{
				// task is IBN if any of its children are CPN or IBN, otherwise it is an OBN
				boolean isIbn = false;
				for (TaskVertex child : taskGraph.children(task))
				{
					NodeType childType = nodeTypes.get(child);
					if (childType == NodeType.IBN || childType == NodeType.CPN)
					{
						nodeTypes.put(task, NodeType.IBN);
						isIbn = true;
					}
				}
				if (!isIbn) 
				{
					nodeTypes.put(task, NodeType.OBN);
					outBranchNodes.add(task);
				}
			}
		}
		
		Collections.sort(CriticalPathNodes, new Comparator<V>() {
			// want ascending order of b-level
			@Override
			public int compare(V A, V B) {
				int val = Long.signum(taskGraph.getBottomLevel(A) - taskGraph.getBottomLevel(B));
				return val != 0 ? val : A.index() - B.index();
			}
		});
		
		Collections.sort(outBranchNodes, new Comparator<V>() {
			// want descending order of b-level
			@Override
			public int compare(V A, V B) {
				int val = Long.signum(taskGraph.getBottomLevel(B) - taskGraph.getBottomLevel(A));
				return val != 0 ? val : A.index() - B.index();
			}
		});
	}
	
	public NodeType nodeType(TaskVertex task)
	{
		return nodeTypes.get(task);
	}
	
	/**
	 * @return Critical path nodes in order of increasing b-level (from sink to source), nodes from multiple critical paths are all included
	 */
	public Iterable<V> criticalPathNodes()
	{
		return CriticalPathNodes;
	}
	
	/**
	 * @return Out-branch nodes in order of decreasing b-level
	 */
	public Iterable<V> outBranchNodes()
	{
		return outBranchNodes;
	}
	
	// In-branch nodes are never needed to be iterated through separately because they are always processed with the CP nodes
}
