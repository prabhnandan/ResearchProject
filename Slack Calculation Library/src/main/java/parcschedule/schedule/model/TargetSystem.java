package parcschedule.schedule.model;

import java.util.Collection;

public interface TargetSystem<P extends Proc>
{
	public Collection<P> processors();

	public int numProcs();

	public P getProc(int index);
}
