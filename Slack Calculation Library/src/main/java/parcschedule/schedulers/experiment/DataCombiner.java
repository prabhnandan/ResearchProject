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

public class DataCombiner 
{
	public static void main(String[] args) throws NumberFormatException, IOException
	{
		// 1. select graphs
		// 2. NAME OUTPUT
//		String output = "Real-Random-SP";
		String output = "AllRandom";
		
		GraphSet[] graphSets = new GraphSet[] {
				
				new GraphSet("Random"),
				new GraphSet("Random (fan-in fan-out)"),
				new GraphSet("Random (intersecting total orders)"),
				new GraphSet("Random (layer by layer)"),
//				
////				new GraphSet("Fork-join"),
////				new GraphSet("In Tree"),
////				new GraphSet("Out Tree"),
//				new GraphSet("Series Parallel"),
//				
//				new GraphSet("Inspiral"),
//				new GraphSet("Montage"),
//				new GraphSet("Sipht"),
//				new GraphSet("Epigenomics"),
//				new GraphSet("CyberShake"),
////
////				new GraphSet("Pipeline"),
//				new GraphSet("FFT"),
//				new GraphSet("Stencil"),
//				new GraphSet("Gauss"),
//				new GraphSet("Cholesky"),
//
//				new GraphSet("robot"),
//				new GraphSet("fpppp"),
//				new GraphSet("sparse"),
				
		};
		
		Integer hash = 657129304;

		boolean algorithmsRead = false;
		List<String> algorithms = new ArrayList<>();
		Map<String, List<Double>> data = new HashMap<>();
		
		for (GraphSet graphSet : graphSets)
		{
			
			String dirName = "RESULTS/Complete data points/Normalised to best result";
			String fileName = String.format("%s_NslBestResult_hash%d.csv", graphSet.name, hash); //algoTests.hashCode());
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

				    	int ccrIndex = tag.indexOf("-R") + 2;
				    	double ccr = Double.parseDouble(tag.substring(ccrIndex, tag.indexOf("-", ccrIndex)));
				    	
				    	if (! data.containsKey(algoName + ccr)) data.put(algoName + ccr, new ArrayList<>());
			    		
			    		if (!algorithmsRead)
			    		{
			    			algorithms.add(algoName);
			    		}

				    	List<Double> entries = data.get(algoName + ccr);
				    	for (int i = 1; i < words.length; i++)
				    	{
				    		entries.add(Double.parseDouble(words[i]));
				    	}
			    	}
			    	else
		    		{
			    		algorithmsRead = true;
		    		}
			    }
			    
			    br.close();
			}
		}

		for (double ccr : new double[] {0.1, 1, 10})
		{
			String dirName = "RESULTS/NSL Best Results by CCR/CCR " + ccr;
			String fileName = String.format("%s_NslBest_CCR%.2f_hash%d.csv", output, ccr, hash);
			Files.createDirectories(Paths.get(dirName));
			PrintWriter writer = new PrintWriter(new FileOutputStream(dirName + "/" + fileName, false), true);
			
			for (String algo : algorithms)
			{
				writer.print(algo);
				for (double value : data.get(algo + ccr))
				{
					writer.printf(",%.4f", value);
				}
				writer.println();
			}
			
			writer.close();
		}
	}
}
