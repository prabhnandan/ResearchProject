package parcschedule.graphGenerator.graphReaders;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class GraphReadTest
{
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException
	{
//		(new OutputGXL()).generateGXLFile("test_out.gxl", "graph", DagGenerator.addEdgeCostsToTaskGraph(StgReader.read(new File("stg/robot.stg")),
//				new ErlangDistribution(10, 1), 1));
		//(new OutputGXL()).generateGXLFile("test_out.gxl", "graph", StgReader.read(new File("stg/robot.stg")));
//		 DagGenerator.generateLaplace(5, 0.5, new ErlangRandomGenerator(10, 1));
//		 DagGenerator.generateLaplace(6, 1, new ErlangRandomGenerator(10, 1));
		// DagGenerator.generateRandomFanInFanOut(50, 4, 4, new ErlangRandomGenerator(5, 2), new ErlangRandomGenerator(3, 1).add(10, 1, 1), 2);
		// DagGenerator.generateRandomIntersectingTotalOrders(100, 4, new ErlangRandomGenerator(20, 0.1).setLimits(100, 300), new ErlangRandomGenerator(20, 1).setLimits(10, 30), 1.5);
		// DagGenerator.generateRandomLayerByLayer(50, 10, 0.1, new ErlangRandomGenerator(20, 0.1).setLimits(100, 300), new ErlangRandomGenerator(20, 1).setLimits(10, 30), 1.5);
		// (new OutputGXL()).generateGXLFile("test_out.gxl", "graph", TggReader.addWeightsToGaussGraph(TggReader.read(new File("shape.dat"), new File("cost.dat")), 10));
		//(new OutputGXL()).generateGXLFile("test_out.gxl", "graph", GXLGraphGenerator.generate(new File("graph.gxl")));
	}
}
