package parcschedule.schedulers.graphProperties;

import java.util.HashMap;
import java.util.Map;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;

public class OptimisticBLevelTable<V extends TaskVertex, E extends CommEdge<V>>
{
	TaskGraph<V, E> taskGraph;
	TargetSystem<?> system;
	Map<TaskVertex, Map<Proc, Integer>> table = new HashMap<>();
	
	public OptimisticBLevelTable(TaskGraph<V, E> taskGraph, TargetSystem<?> system)
	{
		this.taskGraph = taskGraph;
		this.system = system;
		
		for (V task : taskGraph.inverseTopologicalOrder())
		{
			if (taskGraph.outDegree(task) == 0)
			{
				Map<Proc, Integer> column = new HashMap<>();
				for (Proc proc : system.processors())
				{
					column.put(proc, 0);
				}
				table.put(task, column);
			}
			else
			{
				Map<Proc, Integer> column = new HashMap<>();
				for (Proc proc : system.processors())
				{
					int maxMinBLevel = 0;
					for (E outEdge : taskGraph.outEdges(task))
					{
						int minBLevel = Integer.MAX_VALUE;
						TaskVertex child = outEdge.to();
						Map<Proc, Integer> childColumn = table.get(child);
						for (Proc procForChild : system.processors())
						{
							minBLevel = Math.min(minBLevel, childColumn.get(procForChild) + child.weight() + (proc == procForChild ? 0 : outEdge.weight()));
						}
						maxMinBLevel = Math.max(maxMinBLevel, minBLevel);
					}
					column.put(proc, maxMinBLevel);
				}
				table.put(task, column);
			}
		}

		System.out.println("--- print table ---");
		for (TaskVertex task : taskGraph.inverseTopologicalOrder())
		{
			System.out.print(task.name() + ": | ");
			Map<Proc, Integer> column = table.get(task);
			for (Proc proc : system.processors())
			{
				System.out.print(column.get(proc) + "| ");
			}
			System.out.println();
		}
	}
	
	public int get(TaskVertex task, Proc proc)
	{
		return table.get(task).get(proc);
	}
	
}
