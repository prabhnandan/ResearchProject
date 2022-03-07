package parcschedule.graphGenerator.weightGenerators;

import java.text.DecimalFormat;

import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskVertex;

public abstract class WeightGenerator
{
	double incidentWeightInfluence = 0;
	/**
	 * generates value normally
	 * @return
	 */
	public abstract int get();
	/**
	 * takes incident computation weights into consideration
	 * @param incidentComputationWeight
	 */
	public int get(CommEdge<TaskVertex> edge)
	{
		return (int) Math.round(Math.pow(edge.from().weight() + edge.to().weight(), incidentWeightInfluence) * get());
	}
	/**
	 * For letting the computation cost of a task influence the weights of its outgoing edges.
	 * Use like this "WeightGenerator wg = new [subclass]().setSourceWeightInfluence(0.5);"
	 * @param sourceWeightInfluence 
	 */
	public WeightGenerator setSourceWeightInfluence(Double incidentWeightInfluence)
	{
		this.incidentWeightInfluence = incidentWeightInfluence;
		return this;
	}
	
	public String name()
	{
		if (incidentWeightInfluence == 0)
		{
			return subName();
		}
		else
		{
			return subName() + " IWF-" + new DecimalFormat("#.00").format(incidentWeightInfluence);
		}
	}
	
	public abstract WeightGenerator copy();
	
	protected abstract String subName();
}