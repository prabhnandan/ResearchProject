package parcschedule.schedulers.listSchedulers.priorities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.Schedule;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedulers.graphProperties.CriticalPathBasedPartition;

public class CPBasedConstrainingParentsPriority<V extends TaskVertex, P extends Proc> extends StaticTaskPriority<V, P>
{

	private CriticalPathBasedPartition<V> CPPartition;

	@Override
	protected boolean compliesWithPrecedence() 
	{
		return true;
	}

	@Override
	protected List<V> buildPriorityList() 
	{
		List<V> priorityList = new ArrayList<>(taskGraph.sizeVertices());
		Set<TaskVertex> expandedTasks = new HashSet<>();
		Set<TaskVertex> listedTasks = new HashSet<>();
		Stack<V> taskStack = new Stack<>();
		
		for (V task : CPPartition.criticalPathNodes())
		{
			taskStack.push(task);
		}
		
		while (!taskStack.empty())
		{
			if (listedTasks.contains(taskStack.peek()))
			{
				taskStack.pop();
			}
			else if (expandedTasks.contains(taskStack.peek()))
			{
				// task's parents have already been pushed before
				priorityList.add(taskStack.peek());
				listedTasks.add(taskStack.pop());
			}
			else // push all of task's unlisted parents onto stack
			{
				expandedTasks.add(taskStack.peek());
				
				List<CommEdge<V>> unlistedParentEdges = new ArrayList<>();
				for (CommEdge<V> inEdge : taskGraph.inEdges(taskStack.peek()))
				{
					if (!listedTasks.contains(inEdge.from()))
					{
						unlistedParentEdges.add(inEdge);
					}
				}
				// sort unlisted parents in ascending order of priority, which is the order they will be pushed,
				// so they will be popped in decreasing order of priority
				// edges are used here instead of parent vertices for quick access to edge weights
				Collections.sort(unlistedParentEdges, new Comparator<CommEdge<V>>() {

					// priority metric is constraining parent first, 
					// we extended it to be highest (t-level + weight of edge to the task)
					@Override
					public int compare(CommEdge<V> A, CommEdge<V> B) 
					{
						int val = Long.signum(taskGraph.getTopLevel(A.from()) + A.weight() - taskGraph.getTopLevel(B.from()) - B.weight());
						return val != 0 ? val : A.first().index() - B.first().index();
					}
				});
				
				for (CommEdge<V> inEdge : unlistedParentEdges)
				{
					taskStack.push(inEdge.from());
				}
			}
		}

		// adds out-branch nodes, which were not processed with the CP nodes. outBranchNodes() provides them in order of decreasing b-level
		for (V task : CPPartition.outBranchNodes())
		{
			priorityList.add(task);
		}
		
		return priorityList;
	}

	@Override
	protected void subclassLoad(Schedule<V, ?, ?> schedule)
	{
		CPPartition = new CriticalPathBasedPartition<>(taskGraph);
	}

	@Override
	public String description() 
	{
		return "Decisive Path (Park et al.) (modified version of Kwok & Ahmad CPND)";
	}

	@Override
	public String code()
	{
		return "cp-critical-parent";
	}

}