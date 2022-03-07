package parcschedule.schedulers.listSchedulers.priorities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import parcschedule.schedule.Schedule;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedulers.graphProperties.CriticalPathBasedPartition;

public class CPBasedBAndTLevelsPriority<V extends TaskVertex, P extends Proc> extends StaticTaskPriority<V, P>
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
		CPPartition = new CriticalPathBasedPartition<>(taskGraph);
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
				
				List<V> unlistedParents = new ArrayList<>();
				for (V parent : taskGraph.parents(taskStack.peek()))
				{
					if (!listedTasks.contains(parent))
					{
						unlistedParents.add(parent);
					}
				}
				// sort unlisted parents in ascending order of priority, which is the order they will be pushed,
				// so they will be popped in decreasing order of priority
				Collections.sort(unlistedParents, new Comparator<V>() {

					// priority metric is highest b-level first with ties broken by smallest t-level,
					@Override
					public int compare(V A, V B) 
					{
						if (taskGraph.getBottomLevel(A) != taskGraph.getBottomLevel(B))
						{
							int val = Long.signum(taskGraph.getBottomLevel(A) - taskGraph.getBottomLevel(B));
							return val != 0 ? val : A.index() - B.index();
						}
						else
						{
							int val = Long.signum(taskGraph.getTopLevel(B) - taskGraph.getTopLevel(A));
							return val != 0 ? val : A.index() - B.index();
						}
					}
				});
				
				for (V task : unlistedParents)
				{
					taskStack.push(task);
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
	protected void subclassLoad(Schedule<V, ?, ?> schedule) { }

	@Override
	public String description()
	{
		return "Critical-Path-Node-Directed (Kwok & Ahamad)";
	}

	@Override
	public String code()
	{
		return "cp-tlevel";
	}

}
