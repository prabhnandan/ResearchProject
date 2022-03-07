package parcschedule.graphGenerator.weightGenerators;

import java.util.ArrayList;
import java.util.List;

public class ErlangRandomGenerator extends WeightGenerator
{
	List<ComponentDistribution> distributions = new ArrayList<>();
	int weightTotal = 0;
	int lowerLimit = 0;
	int upperLimit = Integer.MAX_VALUE;
	
	public ErlangRandomGenerator(int shape, double rate) {
		add(shape, rate, 1);
	}

	/**
	 * Use add(shape, rate, weight) afterwards to configure the distribution
	 */
	public ErlangRandomGenerator() { }
	
	public ErlangRandomGenerator(List<ComponentDistribution> distributions, int lowerLimit, int upperLimit)
	{
		this.distributions = distributions;
		this.lowerLimit = lowerLimit;
		this.upperLimit = upperLimit;
		for (ComponentDistribution distribution : distributions)
		{
			weightTotal += distribution.weight;
		}
	}

	/**
	 * Adds a component distribution. Add multiple distributions to create a multimodal distribution.
	 * @param shape
	 * @param rate
	 * @param weight specifies the relative strength of the added distribution
	 * @return
	 */
	public ErlangRandomGenerator add(int shape, double rate, double weight)	{
		distributions.add(new ComponentDistribution(shape, rate, weight));
		weightTotal += weight;
		return this;
	}

	@Override
	public int get() {
		double random = Math.random() * weightTotal;
		int accumulatedWeight = 0;
		for (ComponentDistribution distribution : distributions) {
			if (random <= (accumulatedWeight += distribution.weight)) {
				int value = erlangRandom(distribution.shape, distribution.rate);
				if (value > upperLimit) return upperLimit;
				if (value < lowerLimit) return lowerLimit;
				return value;
			}
		}
		throw new RuntimeException("Use add(shape, rate, weight) to add one distribution.");
	}

	@Override
	public String subName() {
		StringBuilder name = new StringBuilder("Gamma");
		for (ComponentDistribution distribution : distributions)
		{
			name.append(String.format(" - %.2f%% E(%d %.2f)", distribution.weight / weightTotal * 100, distribution.shape, distribution.rate));
		}
		return name.toString();
	}
	
	public ErlangRandomGenerator setLimits(int lower, int upper)
	{
		lowerLimit = lower;
		upperLimit = upper;
		return this;
	}
	
	class ComponentDistribution
	{
		int shape;
		double rate;
		double weight;
		public ComponentDistribution(int shape, double rate, double weight) {
			this.shape = shape;
			this.rate = rate;
			this.weight = weight;
		}
	}
	
	/**
	 * @author Implementation - Guyver W, Nikhil P
	 */
	public static int erlangRandom(int shape,double lambda) {
		double value=1.0;
		double ran=Math.random();
		for(int j=0;j<shape;j++){
			ran=Math.random();
			while(ran==0){
				ran=Math.random();
			}
			
			value=value*ran;
		}
		value=(-1/lambda)*(Math.log(value));
		if(value<1){
			value=1.0;
		}else if((value-(int)value)>=0.5){
			value=Math.ceil(value);
		}else{
			value=Math.floor(value);
		}
		return (int)value;
	}

	@Override
	public WeightGenerator copy()
	{
		return new ErlangRandomGenerator(distributions, lowerLimit, upperLimit);
	}
}