package parcschedule.schedule;

import parcschedule.schedule.model.Proc;

public class DrtInfo<P extends Proc>
{
	public final int drt;
	public final P enablingProc;
	public DrtInfo(int drt, P enablingProc)
	{
		this.drt = drt;
		this.enablingProc = enablingProc;
	}
}