package session;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class SessionStats {

	public static int[] HISTOGRAM_XAXIS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

	public static void main(String[] args) throws IOException {
		if(args.length < 2) {
			System.out.println("Usage: SessionStats <sessionFile>");
			return;
		}
		for (int i = 1; i < args.length; i++) {
			System.out.println("File name = " + args[i]);
			SessionStats stat = new SessionStats(args[i]);
			stat.processSingleLogFile();
			stat.printHistogram();
		}
	}
	
	private String inputFile;
	
	/**
	 * A Length to Count Histogram.
	 */
	private Hashtable<Integer, Integer> histogram;

	public SessionStats(String inputFile) {
		this.inputFile = inputFile;
		this.histogram = new Hashtable<>();
	}
	
	public void printHistogram() {
		int total = 0;
		// Adjust Numbers
		Hashtable<Integer, Integer> consolidated = new Hashtable<>();
		consolidated.put(HISTOGRAM_XAXIS.length, 0);
		for (int key : histogram.keySet()) {
			if (key < HISTOGRAM_XAXIS.length) {
				consolidated.put(key, histogram.get(key));
			} else {
				consolidated.put(HISTOGRAM_XAXIS.length,
						consolidated.get(HISTOGRAM_XAXIS.length) + histogram.get(key));
			}
		}
		// Print Final Values.
		for (int key : HISTOGRAM_XAXIS) {
			System.out.println(String.format("%d,%d", key, consolidated.get(key)));
			total += histogram.get(key);
		}
		System.out.println(String.format("Total = %d", total));
	}

	public void processSingleLogFile() throws IOException{
		DataInputStream inputStream = null;
		BufferedReader bufferedReader = null;
		String line = null;
		System.out.println("Input File " + inputFile);
		FileInputStream fr =  new FileInputStream(inputFile);
		inputStream = new DataInputStream(fr);
		bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		while ((line = bufferedReader.readLine()) != null) {
			int length = line.split("-").length;
			if (histogram.containsKey(length)) {
				histogram.put(length, histogram.get(length) + 1);
			} else {
				histogram.put(length, 1);
			}
		}
			bufferedReader.close();
			inputStream.close();
			fr.close();
	}
}
