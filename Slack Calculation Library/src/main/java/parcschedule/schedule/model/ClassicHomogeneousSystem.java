package parcschedule.schedule.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassicHomogeneousSystem implements TargetSystem<Proc>
{
	List<Proc> processors;

	/**
	 * fills object with processors automatically
	 */
	public ClassicHomogeneousSystem(int processorCount)
	{
		processors = new ArrayList<>(processorCount);
		for (int i = 0; i < processorCount; i++)
		{
			processors.add(new BasicProc(Integer.toString(i)));
		}
	}

	/**
	 * processors need to be added after instantiation
	 */
	public ClassicHomogeneousSystem()
	{
		processors = new ArrayList<>();
	}

	public void addProc(Proc proc)
	{
		processors.add(proc);
	}

	@Override
	public Collection<Proc> processors()
	{
		return processors;
	}

	@Override
	public int numProcs() {
		return processors.size();
	}

	@Override
	public Proc getProc(int index) {
		return processors.get(index);
	}
}

