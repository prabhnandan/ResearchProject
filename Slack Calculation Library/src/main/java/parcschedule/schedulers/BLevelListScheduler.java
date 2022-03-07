package parcschedule.schedulers;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskVertex;
import parcschedule.schedule.model.Proc;
import parcschedule.schedulers.listSchedulers.ListScheduler;
import parcschedule.schedulers.listSchedulers.placementSchemes.MinEst;
import parcschedule.schedulers.listSchedulers.priorities.BLevelPriority;

public class BLevelListScheduler<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> extends ListScheduler<V, E, P>
{
	public BLevelListScheduler() 
	{
		super(new MinEst<>(), new BLevelPriority<>());
	}
}

//This is the same as using "new ListScheduler<>(new MinEst<>(), new BLevelPriority<>())". Use this scheduler or make your own with "new ListScheduler<>(*placement scheme*, *priority scheme*)", or "new ClusterScheduler<>(...)".
