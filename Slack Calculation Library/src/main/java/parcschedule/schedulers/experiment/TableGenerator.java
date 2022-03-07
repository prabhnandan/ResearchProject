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

public class TableGenerator {

	public static void main(String[] args) throws NumberFormatException, IOException
	{

		GraphSet[] graphSets = new GraphSet[] {
				
//				new GraphSet("Random"),
//				new GraphSet("Random (fan-in fan-out)"),
//				new GraphSet("Random (intersecting total orders)"),
				new GraphSet("Random (layer by layer)"),
				
//				new GraphSet("Fork-join"),
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
//				new GraphSet("Pipeline"),
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
		
		for (GraphSet graphSet : graphSets)
		{
			Map<String, List<Double>> data = new HashMap<>();
			List<String> algorithms = new ArrayList<>();
			
			String dirName = "RESULTS/Complete data points/Normalised to best result";
			String fileName = String.format("%s_NslBestResult_hash%d.csv", graphSet.name, hash); //algoTests.hashCode());
			File file = new File(dirName + "/" + fileName);
			if (file.exists() && !file.isDirectory())
			{
				boolean algorithmsRead = false;
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
			
			/*int rowSize = data.get(algorithms.get(0) + 0.1).size();
			for (int i = 1; i < algorithms.size(); i++)
			{
				if (data.get(algorithms.get(i) + 0.1).size() != rowSize) throw new RuntimeException();
				if (data.get(algorithms.get(i) + 1.0).size() != rowSize) throw new RuntimeException();
				if (data.get(algorithms.get(i) + 10.0).size() != rowSize) throw new RuntimeException();
			}
			
			Map<String, Double> winRateMap = new HashMap<>();
			Map<String, Double> drawRateMap = new HashMap<>();
			
			for (double ccr : new double[] {0.1, 1, 10})
			{
				for (String algo1 : algorithms)
				{
					for (String algo2 : algorithms)
					{
						if (! algo1.equals(algo2))
						{
							
							int algo1Wins = 0;
							int draws = 0;
							List<Double> algo1Results = data.get(algo1 + ccr);
							List<Double> algo2Results = data.get(algo2 + ccr);
							for (int i = 0; i < rowSize; i++)
							{
								if (algo1Results.get(i) < algo2Results.get(i))
								{
									algo1Wins++;
								}
								else if ( algo1Results.get(i).equals(algo2Results.get(i)) )
								{
									draws++;
								}
							}
							winRateMap.put(algo1 + algo2 + ccr, Math.log( (algo1Wins + draws) / (double) (rowSize - algo1Wins) ));
							
							double drawRate = draws / (double) rowSize;
							if (drawRateMap.containsKey(algo1 + algo2 + ccr))
							{
								if (drawRateMap.get(algo1 + algo2 + ccr) != drawRate) throw new RuntimeException();
							}
							else
							{
								drawRateMap.put(algo1 + algo2 + ccr, drawRate);
							}
						}
					}
				}
			}*/

			// separate piece of code for separating results by CCR
			for (double ccr : new double[] {0.1, 1, 10})
			{
				dirName = "RESULTS/NSL Best Results by CCR/CCR " + ccr;
				fileName = String.format("%s_NslBest_CCR%.2f_hash%d.csv", graphSet.name, ccr, hash);
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
			// -----------------------------------------------------
			
//			dirName = "RESULTS/Comparison Tables";
//			fileName = String.format("%s_Comparisons_hash%d.csv", graphSet.name, hash);
//			Files.createDirectories(Paths.get(dirName));
//			PrintWriter writer = new PrintWriter(new FileOutputStream(dirName + "/" + fileName, false), true);
//
//			for (double ccr : new double[] {0.1, 1, 10})
//			{
//				writer.println("CCR " + ccr);
//				writer.print("win\\loss");
//				for (String algo : algorithms)
//				{
//					writer.print("," + algo);
//				}
//				writer.println();
//				for (String algo1 : algorithms)
//				{
//					writer.print(algo1);
//					for (String algo2 : algorithms)
//					{
//						
//						if (algo1.equals(algo2))
//						{
//							writer.print(",NA");
//						}
//						else
//						{
//							writer.printf(",%.2f", winRateMap.get(algo2 + algo1 /*win for column*/ + ccr));
//						}
//					}
//					writer.println();
//				}
//				writer.println();
//				
//				writer.print("draws");
//				for (String algo : algorithms)
//				{
//					writer.print("," + algo);
//				}
//				writer.println();
//				for (String algo1 : algorithms)
//				{
//					writer.print(algo1);
//					for (String algo2 : algorithms)
//					{
//						if (algo1.equals(algo2))
//						{
//							writer.print(",NA");
//						}
//						else
//						{
//							writer.printf(",%.3f", drawRateMap.get(algo1 + algo2 + ccr));
//						}
//					}
//					writer.println();
//				}
//			}
//			
//			writer.close();
		}
	}

}