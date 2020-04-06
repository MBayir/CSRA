package utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that contains string or collection related functionality.
 * 
 * @author Murat Ali Bayir
 *
 */
public class Utils {

	private static String[] Months = {"Jan","Feb","Mar","Apr","May","Jun","Jul",
			"Aug","Sep","Oct","Nov","Dec"};
	
	/**
	 * @param inputMonth. The string representation of the month that contains only 3 letters.
	 * @return numeric representation of the input month, returns -1 otherwise.
	 */
	public static int returnNumericMonth(String inputMonth){
		for(int i = 0; i < Months.length; i++){
			if(Months[i].equals(inputMonth)){
				return i;
			}
		}
		return -1;
	}

	  /**
	   * Extract all n-grams of given sequence.
	   * 
	   * @param sequence the sequence that contains 'Item{1}-Item{2}-...-Item{N}'
	   * @param nGram the length of n-grams
	   * @return the set of n-grams extracted from {@code sequence}
	   */
	  public static Set<String> getNGrams(String sequence, int nGram) {
			HashSet<String> nGramSet = new HashSet<String>();
			String[] items = sequence.split("-");
								
			for (int i = 0; i < items.length; i++) {
				if ((i + nGram - 1) < items.length) {
					StringBuffer itemBuffer = new StringBuffer(items[i].trim());
					for(int j = i + 1; j <= (i + nGram - 1); j++) {
						itemBuffer.append("-");
						itemBuffer.append(items[j].trim());
					}
					nGramSet.add(itemBuffer.toString());
				}
			}
			return nGramSet;
		}
}