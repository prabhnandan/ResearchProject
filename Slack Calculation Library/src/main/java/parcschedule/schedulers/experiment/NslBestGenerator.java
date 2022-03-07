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
import java.util.List;

public class NslBestGenerator {

	public static void main(String[] args) throws IOException
	{
		GraphSet[] graphSets = new GraphSet[] {
				
//				new GraphSet("Random"),
//				new GraphSet("Random (fan-in fan-out)"),
//				new GraphSet("Random (intersecting total orders)"),
				new GraphSet("Random (layer by layer)"),
				
////				new GraphSet("Fork-join"),
//				new GraphSet("In Tree"),
//				new GraphSet("Out Tree"),
//				new GraphSet("Series Parallel"),
//				
//				new GraphSet("Inspiral"),
//				new GraphSet("Montage"),
//				new GraphSet("Sipht"),
//				new GraphSet("Epigenomics"),
//				new GraphSet("CyberShake"),
//
//				new GraphSet("Pipeline", new int[] {49, 100, 196, 484, 961}),
//				new GraphSet("FFT", new int[] {39, 95, 223, 511, 1151}),
//				new GraphSet("Stencil", new int[] {49, 100, 196, 484, 961}),
//				new GraphSet("Gauss", new int[] {35, 135, 527}),
//				new GraphSet("Cholesky", new int[] {56, 120, 220, 560, 1140}),
//
//				new GraphSet("robot", new int[] {86}),
//				new GraphSet("fpppp", new int[] {332}),
//				new GraphSet("sparse", new int[] {94}),
				
			};
		
		double[] ccrs = new double[] {0.1, 1, 10};
		
		int[] procCounts = new int[] {2, 8, 32, 128, 256, 512};
//		int[] procCounts = new int[] {-1};
		
		Integer hashOfAlgorithmsToBePatched = 657129304;
		
		for (GraphSet graphSet : graphSets)
		{
			Result<List<Double>> result = new Result<>();
			List<String> algorithms = new ArrayList<>();
			
			String dirName = "RESULTS/Complete data points/Normalised to lower bound";
			String fileName = String.format("%s_NslLowerBound_hash%d.csv", graphSet.name, hashOfAlgorithmsToBePatched); //algoTests.hashCode());
			File file = new File(dirName + "/" + fileName);
			if (file.exists() && !file.isDirectory())
			{
				BufferedReader br = new BufferedReader(new FileReader(file));
				boolean algorithmsRead = false;
				
			    String line;
			    while ((line = br.readLine()) != null) 
			    {
			    	String[] words = line.split(",");
			    	if (!line.isEmpty())
			    	{
			    		String tag = words[0];
				    	String algoName = tag.substring(0, tag.indexOf("-R"));
				    	
			    		if (!algorithmsRead)
			    		{
			    			algorithms.add(algoName);
			    		}
	
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
				    	int procCount = Integer.parseInt(tag.substring(pCountIndex, tag.length() - 1));// tag.indexOf("-", pCountIndex)));
				    	
				    	result.put(algoName, ccr, nodeCount, procCount, data);
			    	}
			    	else
		    		{
			    		algorithmsRead = true;
		    		}
			    }
			    
			    br.close();
			}
			else throw new RuntimeException();
			
			for (double ccr : ccrs)
			{
				for (int nodeCount : graphSet.nodeCounts)
				{
					for (int procCount : procCounts)
					{
						if (procCount < nodeCount)
						{
							List<Double> bestResults = new ArrayList<>();
							for (int i = 0; i < result.get(algorithms.get(0), ccr, nodeCount, procCount).size(); i++)
							{
								double bestResult = Integer.MAX_VALUE;
								for (String algorithm : algorithms)
								{
									bestResult = Math.min(bestResult, result.get(algorithm, ccr, nodeCount, procCount).get(i));
								}
								bestResults.add(bestResult);
							}
							result.put("best", ccr, nodeCount, procCount, bestResults);
						}
					}
				}
			}
			
			dirName = "RESULTS/Complete data points/Normalised to best result";
			fileName = String.format("%s_NslBestResult_hash%d.csv", graphSet.name, hashOfAlgorithmsToBePatched);
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
								writer.printf("%s-R%.2f-N%d-P%d-", algorithm, ccr, nodeCount, procCount);
								List<Double> algoResults = result.get(algorithm, ccr, nodeCount, procCount);
								List<Double> bestResults = result.get("best", ccr, nodeCount, procCount);
								for (int i = 0; i < algoResults.size(); i++)
								{
									writer.printf(",%.4f", algoResults.get(i) / bestResults.get(i));
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
