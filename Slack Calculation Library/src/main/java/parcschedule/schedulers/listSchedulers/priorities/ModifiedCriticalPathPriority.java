package parcschedule.schedulers.listSchedulers.priorities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import parcschedule.schedule.Schedule;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;

public class ModifiedCriticalPathPriority<V extends TaskVertex, P extends Proc> extends StaticTaskPriority<V, P>
{
	@Override
	protected boolean compliesWithPrecedence() 
	{
		return true;
	}

	@Override
	protected List<V> buildPriorityList() 
	{
		List<V> priorityList = new ArrayList<>(taskGraph.sizeVertices());
		for (V task : taskGraph.vertices())
		{
			priorityList.add(task);
		}
		Collections.sort(priorityList, new Comparator<V>() {
			// want *descending* order of priority
			@Override
			public int compare(V A, V B) 
			{
				if (taskGraph.getBottomLevel(A) != taskGraph.getBottomLevel(B))
				{
					// reversed priority for descending order
					int val = Long.signum(taskGraph.getBottomLevel(B) - taskGraph.getBottomLevel(A));
					return val != 0 ? val : A.index() - B.index();
				}
				else
				{
					PriorityQueue<V> AChildren = new UniqueTaskHeap(new BLevelComparator());
					PriorityQueue<V> BChildren = new UniqueTaskHeap(new BLevelComparator());
					for (V child : taskGraph.children(A)) AChildren.add(child);
					for (V child : taskGraph.children(B)) BChildren.add(child);
					
					while (true)
					{
						if (AChildren.isEmpty()) return 1; // reversed
						if (BChildren.isEmpty()) return -1;
						if (taskGraph.getBottomLevel(AChildren.peek()) == taskGraph.getBottomLevel(BChildren.peek()))
						{
							for (V descendent : taskGraph.children(AChildren.poll())) AChildren.add(descendent);
							for (V descendent : taskGraph.children(BChildren.poll())) BChildren.add(descendent);
							continue;
						}
						else
						{
							// reversed priority for descending order
							int val = Long.signum(taskGraph.getBottomLevel(BChildren.peek()) - taskGraph.getBottomLevel(AChildren.peek()));
							return val != 0 ? val : A.index() - B.index();
						}
					}
				}
			}
		});
		return priorityList;
	}
	
	class BLevelComparator implements Comparator<V>
	{
		@Override
		public int compare(V A, V B) 
		{
			int val = Long.signum(taskGraph.getBottomLevel(A) - taskGraph.getBottomLevel(B));
			return val != 0 ? val : A.index() - B.index();
		}
	}

	class UniqueTaskHeap extends PriorityQueue<V>
	{
		private static final long serialVersionUID = 1L;
		
		private Set<TaskVertex> queuedTasks = new HashSet<TaskVertex>();
		
		public UniqueTaskHeap(Comparator<V> comparator) 
		{
			super(comparator);
		}
		
		@Override
		public boolean add(V task) 
		{
			if (queuedTasks.contains(task)) return false;
			queuedTasks.add(task);
			return super.add(task);
		}
	}

	@Override
	protected void subclassLoad(Schedule<V, ?, ?> schedule) { }

	@Override
	public String description()
	{
		return "Modified Critical Path (Wu & Gajski)";
	}

	@Override
	public String code()
	{
		return "mcp";
	}
}
