package parcschedule.schedule.model;

public class BasicProc implements Proc
{
	private String ID;
	
	public BasicProc(String ID)
	{
		this.ID = ID;
	}
	
	@Override
	public String id()
	{
		return ID;
	}
}
