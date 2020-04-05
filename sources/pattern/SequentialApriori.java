package pattern;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import utils.Utils;

public class SequentialApriori {
	
	public static int MIN_LENGTH	= 1;
	
	/**
	 * The minimum support for a pattern to become frequent pattern.
	 */
	private float threshold;

	/**
	 * Stores the neighbor relationship between items.
	 */
	private Hashtable<String, HashSet<String>> itemTopology;
	
	/**
	 * Stores the maximal patterns as string to pattern objects where
	 * string is represented by 'item{1}-item{2}-...-item{N}'.
	 */
	private Hashtable<String, Pattern> maximalPatterns;
	
	/**
	 * Stores the set of frequent atoms.
	 */
	private Set<String> frequentAtoms;
	
	/**
	 * Keeps the number of sequences.
	 */
	private long numberOfSequences;
	
	/**
	 * Stores whether any pattern is extended in current iteration.
	 */
	private boolean isExtended;
	
	/**
	 * Stores the list of all patterns.
	 */
	private List<Pattern> allPatterns;	
	
	/**
	 * Stores the distribution of maximal patterns over
	 * Length -> (# of patterns) for generating histogram.
	 */
	private Hashtable<Integer, Long> maximalLengthCount;
	
	/**
	 * Constructs an instance of {@link SequentialApriori} class.
	 * 
	 * @param threshold the minimum threshold for pattern to become frequent.
	 */
	public SequentialApriori(float threshold) {
		this.threshold = threshold;
		itemTopology = new Hashtable<>();
		frequentAtoms = new HashSet<>();
		maximalPatterns = new Hashtable<>();
		maximalLengthCount = new Hashtable<>();
		allPatterns = new ArrayList<>();
		numberOfSequences = 0;
		isExtended = true;
	}

	private void calculateMaximalHistogram() {
		double avgTot = 0.0f;
		long totalElement = 0;
		for (Integer len : maximalLengthCount.keySet()) {
			long currentSum = maximalLengthCount.get(len);
			avgTot += (1.0f * len * currentSum);	
			totalElement += currentSum;
		}

		avgTot = avgTot / (1.0f * totalElement);
	}
	
	private void writeAllPatterns(String wholePatternFile) throws FileNotFoundException {
		PrintStream wholeStream = new PrintStream(wholePatternFile);
		Pattern[] wholePatterns = new Pattern[allPatterns.size()];
		for(int i = 0; i < allPatterns.size(); i++) {
			wholePatterns[i] = allPatterns.get(i);
		}
		Arrays.sort(wholePatterns);
		for(int i = 0; i < wholePatterns.length; i++){
			if(wholePatterns[i].getLength() >= MIN_LENGTH) {
				wholeStream.println(wholePatterns[i].toPrint());
			}
		}	
		wholeStream.close();
	}
	
	private void writeMaximals(String maximalFile)
			throws FileNotFoundException {
		PrintStream maximalStream = new PrintStream(maximalFile);
		Pattern[] wholeMaximals = new Pattern[maximalPatterns.size()];
		int counter = 0;
		for (String maximalKey : maximalPatterns.keySet()) {
			Pattern pattern = maximalPatterns.get(maximalKey);
			wholeMaximals[counter] = pattern;
			counter++;
		}
		Arrays.sort(wholeMaximals);
		
		// Update the length histogram.
		for(int i = 0; i < wholeMaximals.length; i++){
			if(wholeMaximals[i].getLength() >= MIN_LENGTH)
				maximalStream.println(wholeMaximals[i].toPrint());
				int len = wholeMaximals[i].getLength();
				if(maximalLengthCount.containsKey(len)) {
					long count = maximalLengthCount.get(len) + 1;
					maximalLengthCount.put(len, count);
				} else {
					maximalLengthCount.put(len, 1L);
				}
		}
		maximalStream.close();
	}
	
	/**
	 * Writes the all patterns and maximal patterns to output files. Calculates
	 * the length -> (# of patterns) histogram for maximal patterns.
	 * 
	 * @param maximalFile the file that maximal patterns are written to
	 * @param allPatternsFile the file that all patterns are written to
	 * @throws FileNotFoundException if any of the file is not found
	 */
	public void WriteResults(String maximalFile, String allPatternsFile)
			throws FileNotFoundException {
		writeMaximals(maximalFile);
		writeAllPatterns(allPatternsFile);
		calculateMaximalHistogram();
	}
	
	/**
	 * Construct item topology based on neighbour relation in sequence database.
	 * If an item S = [S{1}, S{2}, ... S{i}, S{i+1}, ...,S{N}]. This function adds
	 * an edge from S{i} to S{i+1} in topology for all i.
	 * 
	 * @param fileName the file name that contains the sequences
	 * @throws IOException if an error occurs while opening and reading file
	 */
	private void createTopology(String fileName) throws IOException {
		
		FileInputStream fstream = null;
		DataInputStream in = null;
		BufferedReader br = null;
		String strLine = null;
		
		fstream = new FileInputStream(fileName);
		in = new DataInputStream(fstream);
		br = new BufferedReader(new InputStreamReader(in));
		
		while ((strLine = br.readLine()) != null) {
			numberOfSequences++;
			String[] items = strLine.split("-");
			if(items.length==1) {
				continue;
			}
			String previousNode = items[0].trim();
			for (int i = 1; i < items.length; i++) {
				String latterNode = items[i].trim();
				if(!previousNode.equals(latterNode)) {
					if(itemTopology.containsKey(previousNode)){
						HashSet<String> neighbours = itemTopology.get(previousNode);
						neighbours.add(latterNode);
						itemTopology.put(previousNode, neighbours);
					} else {	
						HashSet<String> neighbours = new HashSet<String>();
						neighbours.add(latterNode);
						itemTopology.put(previousNode, neighbours);
					}
				}
				previousNode=items[i];
			}
		}
		fstream.close();
		br.close();
		in.close();
	}
	
	public void calculateFrequentAtoms(String fileName, Hashtable<String, Pattern> patternTable)
			throws IOException {
		Hashtable<String, Long> supportCount = new Hashtable<String, Long>();
		FileInputStream fstream  = new FileInputStream(fileName);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String sequence = null;

		while ((sequence = br.readLine()) != null) {
			HashSet<String> itemSet = new HashSet<String>();
			String[] items = sequence.split("-");
			for (int i = 0; i < items.length; i++) {
				itemSet.add(items[i].trim());
			}
			
			for (String item : itemSet) {
				if(supportCount.containsKey(item)){
					long val = supportCount.get(item);
					val++;
					supportCount.put(item, val);
				} else {
					supportCount.put(item, 1L);
				}
			}
		}
		
		for (String key : supportCount.keySet()) {
			long count   		= supportCount.get(key);
			float support  		= (1.0f * count) / (1.0f * numberOfSequences);
			if(support >= threshold){
				frequentAtoms.add(key);
				Pattern atomicPattern = new Pattern(key, support, true);
				patternTable.put(key, atomicPattern);
				maximalPatterns.put(key, atomicPattern);
				isExtended = true;
				allPatterns.add(atomicPattern);
			}
		}

		fstream.close();
		br.close();
		in.close();
	}
	
	/**
	 * Generates the candidate patterns by using patterns in previous rounds and 
	 * {@code itemTopology}. Stores the result to {@code candidatePatterns}.
	 * @param candidatePatterns
	 * @param patternsInPreviousRound
	 */
	private void generateCandidatePatterns(Hashtable<String, Long> candidatePatterns,
			Hashtable<String, Pattern> patternsInPreviousRound) {
		for (String key : patternsInPreviousRound.keySet()) {
			Pattern pattern = patternsInPreviousRound.get(key);
			String lastItem = pattern.getLastItem();
			if(itemTopology.containsKey(lastItem)) {
				Set<String> neighbours = itemTopology.get(lastItem);
				for (String neighbour : neighbours) {
					if (frequentAtoms.contains(neighbour)
							&& !pattern.getSequence().contains(neighbour)) {
						Pattern newPattern = pattern.copy();
						newPattern.setMaximal(true);
						newPattern.getSequence().add(neighbour);
						candidatePatterns.put(newPattern.getKey(), 0L);
					}
				}
			}
		}
	}
	
	/**
	 * Calculates the support of candidate patterns by scanning all sequences in the database.
	 * This function extract the {@code step}-Grams of each sequence in database to match with
	 * patterns in the key set of candidate patterns table.
	 * 
	 * @param candidatePatterns the candidate pattern table to store frequency of candidate
	 * 	      patterns
	 * @param inputFile the input file that contains all sequences in the database.
	 * @param step the length of patterns in current step.
	 * @throws IOException if any error occurs while opening and reading {@code inputFile}
	 */
	private void calculateSupport(Hashtable<String, Long> candidatePatterns, String inputFile, int step)
			throws IOException {
		// Calculate the support of candidate patterns.
		FileInputStream fstream = new FileInputStream(inputFile);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String sequence = null;

		while ((sequence = br.readLine()) != null) {
			Set<String> nGramSet = Utils.getNGrams(sequence, step);
			for (String nGram : nGramSet) {
				if (candidatePatterns.containsKey(nGram)) {
					long count = candidatePatterns.get(nGram);
					count++;
					candidatePatterns.put(nGram, count);
				}
			}
		}
		br.close();
		in.close();
		fstream.close();
	}

	/**
	 * Finds the frequent patterns by processing sequences in dbFile.
	 * 
	 * @param dbFile the file that contains all transactions (sequences)
	 * @throws IOException if any error occurs during opening and reading dbfile.
	 */
	public void findFrequentPatterns(String dbFile) throws IOException{
		
		int step  = 1;
		Hashtable<String, Pattern> patternsInPreviousRound = new Hashtable<>();
		
		while(isExtended) {
			isExtended = false;
			if(step == 1) {
				calculateFrequentAtoms(dbFile, patternsInPreviousRound);
			} else {
				System.out.println("step: " + step);
				Hashtable<String, Pattern> patternsInCurrentRound = new Hashtable<>();
				Hashtable<String, Long> candidatePatterns = new Hashtable<>();
				
				// Generate candidate patterns.
				generateCandidatePatterns(candidatePatterns, patternsInPreviousRound);

				// Calculate the support of candidate patterns.
				calculateSupport(candidatePatterns, dbFile, step);
				
				for (String candidate : candidatePatterns.keySet()) {
					long count	= candidatePatterns.get(candidate);
					float support  = (1.0f * count) / (1.0f * numberOfSequences);
					if(support >= threshold) {
						Pattern newPattern = new Pattern(candidate, support, true);
						patternsInCurrentRound.put(candidate, newPattern);
						maximalPatterns.put(candidate, newPattern);
						maximalPatterns.remove(newPattern.getLastItem()); // Remove appended cell.
						maximalPatterns.remove(newPattern.head());	// Remove head sequence.
						maximalPatterns.remove(newPattern.tail()); // Remove the tail sequence.
						isExtended = true;
						allPatterns.add(newPattern);
					}	
				}
				patternsInPreviousRound.clear();
				patternsInPreviousRound = patternsInCurrentRound;
			} // Else we are at least step2.
			step++;
		} // While Loop
	}
	
	public static void main(String[] args){
		
		if(args.length != 5){
			System.out.println("Usage: Apriori <inputFile> <threshold> <maximalPatternFile> <wholePatternFile>");
			return;
		}
		
		String inputFile			= args[1];
		String threshold			= args[2];
		String maximalPatternFile	= args[3];
		String allPatternsFile		= args[4];
		float thresholdAsFloat 		= Float.parseFloat(threshold);

		SequentialApriori apriori = new SequentialApriori(thresholdAsFloat);
		
		try {
			apriori.createTopology(inputFile);
			apriori.findFrequentPatterns(inputFile);
			apriori.WriteResults(maximalPatternFile, allPatternsFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}