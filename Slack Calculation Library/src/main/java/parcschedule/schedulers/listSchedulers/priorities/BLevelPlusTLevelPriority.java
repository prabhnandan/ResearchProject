package parcschedule.schedulers.listSchedulers.priorities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import parcschedule.schedule.Schedule;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;

public class BLevelPlusTLevelPriority<V extends TaskVertex, P extends Proc> extends StaticTaskPriority<V, P>
{

	@Override
	public String description()
	{
		return "blevel + tlevel (used in cpop)";
	}

	@Override
	public String code() 
	{
		return "bltl";
	}

	@Override
	protected boolean compliesWithPrecedence() {
		// TODO Auto-generated method stub
		return false;
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
			// want descending order of b-level
			@Override
			public int compare(V A, V B) 
			{
				int val = Long.signum(taskGraph.getBottomLevel(B) + taskGraph.getTopLevel(B) - taskGraph.getBottomLevel(A) - taskGraph.getTopLevel(A));
				return val != 0 ? val : A.index() - B.index();
			}
		});
		return priorityList;
	}
	
	@Override
	protected void subclassLoad(Schedule<V, ?, ?> schedule) { }

}
