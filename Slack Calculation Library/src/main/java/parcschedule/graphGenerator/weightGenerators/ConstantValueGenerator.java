package parcschedule.graphGenerator.weightGenerators;

public class ConstantValueGenerator extends WeightGenerator
{
	int value;
	public ConstantValueGenerator(int value) {
		this.value = value;
	}
	
	@Override
	public int get() {
			return value;
	}

	@Override
	public String subName() {
		return "constant-" + value;
	}

	@Override
	public WeightGenerator copy()
	{
		return new ConstantValueGenerator(value);
	}
}