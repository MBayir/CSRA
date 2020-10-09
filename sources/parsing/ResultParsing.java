package parsing;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.*;

public class ResultParsing {

	public enum Heuristic
	{
		TO(1, "TO val:"), 
		IP(2, "IP val:"),
		SSRA(3, "SmartSRA val:"),
		CSRA(4, "CSRA val:"),
		NO(5, "NO val:"),
		CTO(6, "CTO val:");
		
		private int idNumber;
		
		private String pattern;
		
		private Heuristic(int id, String pattern) {
			this.idNumber = id;
			this.pattern = pattern;
		}
		
		public int getId() {
			return this.idNumber;
		}
		
		public String getPattern()
		{
			return this.pattern;
		}
	}

	private static int parsePerformance(List<String> lines, Heuristic heuristic)
	{
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).indexOf(heuristic.pattern) != -1)
			{
				return Integer.parseInt(lines.get(i).substring(heuristic.getPattern().length()).trim());
			}
		}
		return -1;
	}

	
	private static void PopulatePredictionPerf(Hashtable<Heuristic, int[]> numericResults,
			Hashtable<Heuristic, double[]> rationalResults,
			List<String> lines,
			int index
			)
	{
		int totalAttempt = Integer.parseInt(lines.get(0).substring("Number Of Tries: ".length()).trim());
		for (Heuristic heuristic : Heuristic.values())
		{
			int currentValue = parsePerformance(lines, heuristic);
			numericResults.get(heuristic)[index] = currentValue;
			rationalResults.get(heuristic)[index] = (currentValue * 1.0d) / (totalAttempt * 1.0d);
		}
	}
	
	/**
	 * ./wum ResultParsing "C:\cygwin64\home\Murat\WUM\Results" 1 "C:\cygwin64\home\Murat\WUM\Results\Graph-T1.txt"
	 * folder: "C:\cygwin64\home\Murat\WUM\Results"
	 * tail size: 1
	 * output: "C:\cygwin64\home\Murat\WUM\Results\Graph-T1.txt"
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 4){
			System.out.println("Usage: ResultParsing <inputFolder> <TailSize> <outputFile>");
			return;
		}
		
		String inputFolder = args[1];
		String tailSize = args[2];
		String outputFile = args[3];
		
		ArrayList<String> predictionSizes = new ArrayList<String>(Arrays.asList("1", "2", "5"," 10", "50"));
		
		Hashtable<Heuristic, int[]> numericResults = new Hashtable<ResultParsing.Heuristic, int[]>();
		Hashtable<Heuristic, double[]> rationalResults = new Hashtable<ResultParsing.Heuristic, double[]>();
		for (Heuristic algo : Heuristic.values()) {
			numericResults.put(algo, new int[predictionSizes.size()]);
			rationalResults.put(algo, new double[predictionSizes.size()]);
		}
		PrintStream outputStream = null;
		try {
			System.out.println("O: " + outputFile);
			outputStream = new PrintStream(outputFile);
			for (int i = 0; i < predictionSizes.size(); i++)
			{
				String fileName = String.format("PredictionResults-%s-%s.txt",predictionSizes.get(i),tailSize);
				fileName = fileName.replaceAll("\\s+","");
				System.out.println(Paths.get(inputFolder, fileName).toString());
				List<String> lines = Files.readAllLines(Paths.get(inputFolder, fileName));
				System.out.println(lines.get(0));
				PopulatePredictionPerf(numericResults, rationalResults, lines, i);
			}
			
			StringBuilder numericBuilder = new StringBuilder("");
			StringBuilder ratioBuilder = new StringBuilder("");
			for (int i = 0; i < predictionSizes.size(); i++)
			{
				numericBuilder.append(i);
				ratioBuilder.append(i);
				Heuristic[] heuristics = Heuristic.values();
				for (int j = 0; j < heuristics.length; j++)
				{
					numericBuilder.append("\t");
					ratioBuilder.append("\t");
					numericBuilder.append(numericResults.get(heuristics[j])[i]);
					ratioBuilder.append(rationalResults.get(heuristics[j])[i]);
				}
				numericBuilder.append(System.lineSeparator());
				ratioBuilder.append(System.lineSeparator());
			}
			outputStream.println(numericBuilder.toString());
			outputStream.println();
			outputStream.println();
			outputStream.println(ratioBuilder.toString());
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		outputStream.close();
	}
}
