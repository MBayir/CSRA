package ml;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;

import core.Sequence;
import pattern.Pattern;

public class BayesianPredictor {
	/**
	 * Stores pattern -> support pairs.
	 */
	private Hashtable<String, Float> patternToSupport;

	/**
	 * Stores prefix -> pattern pairs.
	 */
	private Hashtable<String, Set<String>> prefixToPatternSet;

	/**
	 * Stores the maximum number of predicted items.
	 */
	private int numberOfPredictedItem;

	/**
	 * Random object to select random numbers for Bayesian predictor.
	 */
	private Random random;

	/**
	 * Maximum number of iterations while taking tail of the current sequence if
	 * there is no prefix with the current sequence.
	 */
	private int maxTailCount;

	public BayesianPredictor(int numberOfPredictedItem, int numberOfStepsBack) {
		patternToSupport = new Hashtable<>();
		prefixToPatternSet = new Hashtable<>();
		random = new Random();
		this.numberOfPredictedItem = numberOfPredictedItem;
		this.maxTailCount = numberOfStepsBack;
	}

	public BayesianPredictor(int numberOfPredictedItem, Hashtable<String, Set<String>> prefixToPatternSet,
			Hashtable<String, Float> patternToSupport) {
		this.numberOfPredictedItem = numberOfPredictedItem;
		this.prefixToPatternSet = prefixToPatternSet;
		this.patternToSupport = patternToSupport;
		random = new Random();
	}

	/**
	 * Returns the prefix of the given {@code pattern}. The prefix of pattern
	 * 'Item{1}-Item{2}-...-Item{N}' is 'Item{1}-Item{2}-...-Item{N-1}'.
	 */
	private String getPrefix(String pattern) {
		String[] items = pattern.split("-");
		StringBuffer buffer = new StringBuffer(items[0]);
		for (int i = 1; i < (items.length - 1); i++) {
			buffer.append("-");
			buffer.append(items[i]);
		}
		return items.length > 1 ? buffer.toString() : "";
	}

	/**
	 * Returns the last item of the pattern.
	 */
	private String getLast(String pattern) {
		String[] items = pattern.split("-");
		if (items.length >= 1) {
			return items[items.length - 1];
		} else {
			return "";
		}
	}

	/**
	 * Returns the tail of the pattern.
	 */
	private String getTail(String pattern) {
		String[] items = pattern.split("-");
		if (items.length >= 2) {
			StringBuffer buffer = new StringBuffer("");
			for (int i = 1; i < items.length; i++) {
				if (i != 1) {
					buffer.append("-");
				}
				buffer.append(items[i].trim());
			}
			return buffer.toString();
		} else {
			return "";
		}
	}

	/**
	 * Binary search to find index i^th in numbers such that: (target <= numbers[i]
	 * && target > numbers[i-1])
	 * 
	 * @param numbers the array of numbers to search
	 * @param target  the target number to find in an array
	 * @param min     the left side index
	 * @param max     the right side index
	 * @return
	 */
	private int findIndex(long[] numbers, long target, int min, int max) {
		int mid = (min + max) / 2;
		if (numbers[mid] >= target && (mid > 0 ? target > numbers[mid - 1] : true)) {
			return mid;
		}
		if (mid == numbers.length - 1) {
			return mid;
		}

		if (numbers[mid] >= target) {
			return findIndex(numbers, target, min, mid - 1);
		} else {
			return findIndex(numbers, target, mid + 1, max);
		}
	}

	/**
	 * Select n items where n = {@code numberOfItems} from the key set of
	 * candidateToSupport table. Each key has selection probability that is
	 * proportional to their support value.
	 * 
	 * @param candidateToSupport the candidate pattern to support table
	 * @param result             the result of selected patterns
	 * @param numberOfItems      to determine how many patterns will be selected
	 */
	private void applySoftMaxAndSelect(Hashtable<String, Float> candidateToSupport, Set<String> result,
			int numberOfItems) {
		if (numberOfItems >= 1 && candidateToSupport.size() >= 1) {
			String[] candidates = new String[candidateToSupport.size()];
			candidateToSupport.keySet().toArray(candidates);
			long[] values = new long[candidateToSupport.size()];
			long totalSum = 0;
			for (int i = 0; i < candidates.length; i++) {
				totalSum += (long) (candidateToSupport.get(candidates[i]) * Math.pow(10.0, 12.0));
				values[i] = totalSum;

			}
			long randomNumber = (long) (random.nextDouble() * totalSum);
			int index = findIndex(values, randomNumber, 0, values.length - 1);
			result.add(candidates[index]);
			candidateToSupport.remove(candidates[index]);
			applySoftMaxAndSelect(candidateToSupport, result, numberOfItems - 1);
		}
	}

	private String bringExistingTail(String sequence) {
		if (prefixToPatternSet.containsKey(sequence.trim())) {
			return sequence;
		} else {
			int step = 1;
			while (!prefixToPatternSet.containsKey(sequence) && !sequence.isEmpty()) {
				if (step == maxTailCount) {
					return "";
				}
				sequence = getTail(sequence);
				step++;
			}
			return sequence;
		}
	}

	private List<Sequence> bringExistingTail(List<Sequence> sequence) {
		for (Sequence item : sequence) {
			if (prefixToPatternSet.containsKey(item.toString().trim())) {
				return sequence;
			}
		}

		int step = 1;
		while (true) {
			List<Sequence> temp = new ArrayList<>();
			for (Sequence item : sequence) {
				String tail = getTail(item.toString());
				if (!tail.equals("") && prefixToPatternSet.containsKey(tail)) {
					temp.add(new Sequence(tail, item.getPenalty()));
				}
			}
			if (!temp.isEmpty()) {
				return temp;
			}
			if (step >= maxTailCount) {
				return sequence;
			}
			step++;
		}
	}

	/**
	 * Predict the next item that can come after sequence where sequence is
	 * 'Item{1}-Item{2}-...-Item{N}. This function returns the set of possible items
	 * for position Item{N+1}.
	 * 
	 * @param sequence the sequence for which the next item to be predicted
	 * @return the set of possible items that can come after {@code sequence}
	 */
	public Set<String> predictNextItem(List<Sequence> inputSequences, List<Pattern> matchedPatternsOutput) {
		Set<String> result = new HashSet<>();
		List<Sequence> sequences = bringExistingTail(inputSequences);
		// List<String> sequences = inputSequences;
		Hashtable<String, Float> candidateToSupportTable = new Hashtable<>();
		for (int i = 0; i < sequences.size(); i++) {
			Sequence sequence = sequences.get(i);
			if (prefixToPatternSet.containsKey(sequence.toString())) {
				Set<String> matchedPatterns = prefixToPatternSet.get(sequence.toString());
				for (String matchedPatternString : matchedPatterns) {
					Float support = patternToSupport.get(matchedPatternString);
					support *= sequence.getPenalty();
					matchedPatternsOutput.add(new Pattern(matchedPatternString, support, true));
				}
				for (String matched : matchedPatterns) {
					Float support = patternToSupport.get(matched);
					support *= sequence.getPenalty();
					String candidateItem = getLast(matched);
					if (candidateToSupportTable.containsKey(candidateItem)) {
						Float value = candidateToSupportTable.get(candidateItem);
						value += support;
						candidateToSupportTable.put(candidateItem, value);
					} else {
						candidateToSupportTable.put(candidateItem, support);
					}
				}
			}
		}
		applySoftMaxAndSelect(candidateToSupportTable, result, numberOfPredictedItem);
		return result;
	}

	/**
	 * Predict the next item that can come after sequence where sequence is
	 * 'Item{1}-Item{2}-...-Item{N}. This function returns the set of possible items
	 * for position Item{N+1}.
	 * 
	 * @param sequence the sequence for which the next item to be predicted
	 * @return the set of possible items that can come after {@code sequence}
	 */
	public Set<String> predictNextItem(String sequence, Set<String> matchedPatternsOutput) {
		Set<String> result = new HashSet<>();
		sequence = bringExistingTail(sequence);
		if (prefixToPatternSet.containsKey(sequence) && !sequence.equals("")) {
			Set<String> matchedPatterns = prefixToPatternSet.get(sequence);
			matchedPatternsOutput.addAll(matchedPatterns);
			Hashtable<String, Float> candidateToSupportTable = new Hashtable<>();
			for (String matched : matchedPatterns) {
				Float support = patternToSupport.get(matched);
				String candidateItem = getLast(matched);
				candidateToSupportTable.put(candidateItem, support);
			}
			applySoftMaxAndSelect(candidateToSupportTable, result, numberOfPredictedItem);
		}
		return result;
	}

	/**
	 * Loads the Bayesian predictor model from frequent patterns file.
	 * 
	 * @param inputFile the input file that constrains all of the patterns.
	 * @throws IOException if an error occurs while opening or reading from input
	 *                     file
	 */
	public void loadModel(String inputFile) throws IOException {
		// Calculate the support of candidate patterns.
		FileInputStream fstream = new FileInputStream(inputFile);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String supportAndPattern = null;
		while ((supportAndPattern = br.readLine()) != null) {
			String[] supportAndPatternArray = supportAndPattern.split(",");
			String pattern = supportAndPatternArray[1].trim();
			Float support = Float.parseFloat(supportAndPatternArray[0].trim());
			patternToSupport.put(pattern, support);
			String prefix = getPrefix(pattern);
			if (prefixToPatternSet.containsKey(prefix)) {
				prefixToPatternSet.get(prefix).add(pattern);
			} else {
				Set<String> patterns = new HashSet<>();
				patterns.add(pattern);
				prefixToPatternSet.put(prefix, patterns);
			}
		}
		br.close();
		in.close();
		fstream.close();
	}
}
