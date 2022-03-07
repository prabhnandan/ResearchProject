package parcschedule.schedulers.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReorderTool 
{
	public static void main(String[] args) throws NumberFormatException, IOException 
	{
		GraphSet[] graphSets = new GraphSet[] {
			
			new GraphSet("Random"),
			new GraphSet("Random (fan-in fan-out)"),
			new GraphSet("Random (intersecting total orders)"),
			new GraphSet("Random (layer by layer)"),
			
			new GraphSet("Fork-join"),
			new GraphSet("In Tree"),
			new GraphSet("Out Tree"),
			new GraphSet("Series Parallel"),
			
			new GraphSet("Inspiral"),
			new GraphSet("Montage"),
			new GraphSet("Sipht"),
			new GraphSet("Epigenomics"),
			new GraphSet("CyberShake"),

			new GraphSet("Pipeline", new int[] {49, 100, 196, 484, 961}),
			new GraphSet("FFT", new int[] {39, 95, 223, 511, 1151}),
			new GraphSet("Stencil", new int[] {49, 100, 196, 484, 961}),
			new GraphSet("Gauss", new int[] {35, 135, 527}),
			new GraphSet("Cholesky", new int[] {56, 120, 220, 560, 1140}),

			new GraphSet("robot", new int[] {86}),
			new GraphSet("fpppp", new int[] {332}),
			new GraphSet("sparse", new int[] {94}),
			
		};
	
		double[] ccrs = new double[] {0.1, 1, 10};
		
		int[] procCounts = new int[] {2, 8, 32, 128, 256, 512};
		
		String[] algorithms = new String[] { // new order
				
				"bl-est",
				"bl-cle",
				"bl-cwe",
				"bl-cc",
				"tlbl-est",
				"tlbl-cwe",
				"cpn-est",
				"cpn-cwe",
				"cpn-cc",
				"dps-est",
				"dps-cwe",
				"dps-cc",
				"bl-est-ins",
				"bl-cle-ins",
				"bl-cwe-ins",
				"bl-cc-ins",
				"bl-cpop-ins",
				"tlbl-est-ins",
				"tlbl-cpop-ins",
				"tlbl-cwe-ins",
				"cpn-est-ins",
				"cpn-cwe-ins",
				"cpn-cc-ins",
				"dps-est-ins",
				"dps-cwe-ins",
				"dps-cc-ins",
				"dls-ins",
				"dls",
				"dlscc",
				"etf",
				"etfcc",
				"dcp-glb-bl",
				"dcp-glb-etf",
				"dcp-ls-etf",
				"dcpx-glb-etf",
				"dcpx-ls-etf",
				"dsc-glb-etf",
				"dsc-ls-etf",
				"dsc-glbro-etf",
				"lc-glb-etf",
				"lc-ls-etf"
	
		};
		
		Map<String, String> newNames = new HashMap<>();
//		
//		for (String algoName : algorithms)
//		{
//			if (algoName.contains("wle"))
//			{
//				newNames.put(algoName, algoName.replace("wle", "cwe"));
//			}
//		}

//		for (String algoName : new String[] {
//				
//				"bl-est-nins",
//				"bl-cle-nins",
//				"bl-wle-nins",
//				"bl-cc-nins",
//				"bl-est",
//				"bl-cle",
//				"bl-wle",
//				"bl-cc",
//				"bl-cpop",
//				"tlbl-est-nins",
//				"tlbl-wle-nins",
//				"tlbl-wle",
//				"tlbl-cpop",
//				"tlbl-est",
//				"cpn-est-nins",
//				"cpn-wle-nins",
//				"cpn-cc-nins",
//				"cpn-est",
//				"cpn-wle",
//				"cpn-cc",
//				"dps-est-nins",
//				"dps-wle-nins",
//				"dps-cc-nins",
//				"dps-est",
//				"dps-wle",
//				"dps-cc",
//				"etf-nins",
//				"etfcc-nins",
//				"dls",
//				"dls-nins",
//				"dlscc-nins" })
//		{
//			int indexNins = algoName.indexOf("-nins");
//			if (indexNins > 0)
//			{
//				newNames.put(algoName, algoName.substring(0, indexNins));
//			}
//			else
//			{
//				newNames.put(algoName, algoName + "-ins");
//			}
//		}
		
		Integer hashOfAlgorithmsToBePatched = 657129304;
		
		for (String[] nslLabels : new String[][] {
			new String[] {"best result", "BestResult"}, 
			new String[] {"lower bound", "LowerBound"}})
		{
			for (GraphSet graphSet : graphSets)
			{
				Result<List<Double>> result = new Result<>();
				
				String dirName = "RESULTS/Complete data points/Normalised to " + nslLabels[0];
				String fileName = String.format("%s_Nsl%s_hash%d.csv", graphSet.name, nslLabels[1], hashOfAlgorithmsToBePatched); //algoTests.hashCode());
				File file = new File(dirName + "/" + fileName);
				if (file.exists() && !file.isDirectory())
				{
					BufferedReader br = new BufferedReader(new FileReader(file));
					
				    String line;
				    while ((line = br.readLine()) != null) 
				    {
				    	String[] words = line.split(",");
				    	if (!line.isEmpty())
				    	{
				    		String tag = words[0];
					    	String algoName = tag.substring(0, tag.indexOf("-R"));
		
					    	List<Double> data = new ArrayList<>();
					    	for (int i = 1; i < words.length; i++)
					    	{
					    		data.add(Double.parseDouble(words[i]));
					    	}
					    	int ccrIndex = tag.indexOf("-R") + 2;
					    	double ccr = Double.parseDouble(tag.substring(ccrIndex, tag.indexOf("-", ccrIndex)));
					    	int nCountIndex = tag.indexOf("-N") + 2;
					    	int nodeCount = Integer.parseInt(tag.substring(nCountIndex, tag.indexOf("-", nCountIndex)));
					    	int pCountIndex = tag.indexOf("-P") + 2;
					    	int procCount = Integer.parseInt(tag.substring(pCountIndex, tag.indexOf("-", pCountIndex)));
					    	
					    	result.put(algoName, ccr, nodeCount, procCount, data);
				    	}
				    }
				    
				    br.close();
				}
				else throw new RuntimeException();
				
				Files.createDirectories(Paths.get(dirName));
				PrintWriter writer = new PrintWriter(new FileOutputStream(dirName + "/" + fileName, false), true);
				for (double ccr : ccrs)
				{
					for (int nodeCount : graphSet.nodeCounts)
					{
						for (int procCount : procCounts)
						{
							if (procCount < nodeCount)
							{
								for (String algorithm : algorithms)
								{
									String newName = newNames.get(algorithm);
									
									writer.printf("%s-R%.2f-N%d-P%d-", newName != null ? newName : algorithm, ccr, nodeCount, procCount);
									for (double nsl : result.get(algorithm, ccr, nodeCount, procCount))
									{
										writer.printf(",%.4f", nsl);
									}
									writer.println();
								}
								writer.println();
							}
						}
					}
				}
				writer.close();
			}
		}
	}
}
