package parcschedule.graphGenerator.weightGenerators;

public class UniformRandomGenerator extends WeightGenerator
{
	int lowerBound, upperBound;
	public UniformRandomGenerator(int lowerBound, int upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	@Override
	public int get() {
			return (int) Math.ceil((Math.random() * (upperBound - lowerBound)) + lowerBound);
	}

	@Override
	public String subName() {
		return "uniform " + lowerBound + "-" + upperBound;
	}

	@Override
	public WeightGenerator copy() {
		return new UniformRandomGenerator(lowerBound, upperBound);
	}
	
}
