package ml;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import parsing.LogParser;
import pattern.Pattern;
import session.CompleteSRA;
import session.IntegerProgramming;
import session.LinkBasedConstructor;
import session.NavigationOriented;
import session.SmartSRA;
import session.TimeOriented;
import core.Sequence;
import core.Session;

/**
 * Predicts the next page in the session by using Bayesian Predictor for each
 * session construction algorithms.
 * 
 * @author Murat Ali Bayir
 *
 */
public class PagePredictor extends LinkBasedConstructor {
	public static int MAX_TAIL_COUNT = -1;
	private static PrintStream resultStream = null;
	private static double PENALTY_COEFFICIENT = 0.1d;

	private TimeOriented timeOriented;
	private SmartSRA smartSRA;
	private CompleteSRA completeSRA;
	private IntegerProgramming integerProgramming;
	private NavigationOriented navigationOriented;
	private int numberOfTry = 0;
	private int[] successCount;
	private int[] emptyPredictor;
	private int numberOfTrivialSequences;
	private int numberOfComplexSequences;

	public enum Algorithm {
		TO(0), SmartSRA(1), CSRA(2), IP(3), NO(4);

		private int id;

		private Algorithm(int id) {
			this.id = id;
		}

		private int getId() {
			return id;
		}
	}

	private BayesianPredictor toPredictor;
	private BayesianPredictor smartSRAPredictor;
	private BayesianPredictor completeSRAPredictor;
	private BayesianPredictor ipPredictor;
	private BayesianPredictor noPredictor;

	public PagePredictor(String domainName, int numberOfPredictedItem) {
		super(domainName, Mode.TOPOLOGYMODE, true);
		timeOriented = new TimeOriented(domainName, true);
		smartSRA = new SmartSRA(domainName, Mode.TOPOLOGYMODE, true);
		completeSRA = new CompleteSRA(domainName, Mode.TOPOLOGYMODE, Integer.MAX_VALUE, true);
		integerProgramming = new IntegerProgramming(domainName, Mode.TOPOLOGYMODE, Integer.MAX_VALUE, true);
		navigationOriented = new NavigationOriented(domainName, Mode.TOPOLOGYMODE, true);
		toPredictor = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		smartSRAPredictor = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		completeSRAPredictor = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		ipPredictor = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		noPredictor = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		successCount = new int[Algorithm.values().length];
		for (int i = 0; i < Algorithm.values().length; i++) {
			successCount[i] = 0;
		}
		emptyPredictor = new int[Algorithm.values().length];
		for (int i = 0; i < Algorithm.values().length; i++) {
			emptyPredictor[i] = 0;
		}
		numberOfComplexSequences = 0;
		numberOfTrivialSequences = 0;
	}

	public void loadSessionGenerators() {
		smartSRA.setTopology(topology);
		completeSRA.setTopology(topology);
		integerProgramming.setTopology(topology);
		navigationOriented.setTopology(topology);
	}

	public void loadModels(String toPatterns, String ssraPatterns, String csraPatterns, String ipPatterns,
			String noPatterns) throws IOException {
		toPredictor.loadModel(toPatterns);
		System.out.println("Time oriented Predictor model is loaded!");
		smartSRAPredictor.loadModel(ssraPatterns);
		System.out.println("Smart SRA Predictor model is loaded!");
		completeSRAPredictor.loadModel(csraPatterns);
		System.out.println("Complete SRA Predictor model is loaded!");
		ipPredictor.loadModel(ipPatterns);
		System.out.println("IP Predictor model is loaded!");
		noPredictor.loadModel(noPatterns);
		System.out.println("NO Predictor model is loaded!");
	}

	private Session cutSession(Session candidateSession, int cutPoint) {
		List<String> visitedPages = new ArrayList<>();
		List<String> referrers = new ArrayList<>();
		for (int i = 0; i < cutPoint; i++) {
			visitedPages.add(candidateSession.getSequence().get(i));
			referrers.add(candidateSession.getRefSequence().get(i));
		}
		Session shortSession = new Session();
		shortSession.setSequence(visitedPages);
		shortSession.setRefSequence(referrers);
		shortSession.setId(candidateSession.getId());
		shortSession.setIpNumber(candidateSession.getIpNumber());
		shortSession.setInitalTime(candidateSession.getInitalTime());
		return shortSession;
	}

	public void logFailingPrediction(Algorithm algo, List<Sequence> sequences, List<Pattern> matchedPatterns,
			Set<String> result) {
		System.out.println("**** " + algo + " Sequences***");
		for (int i = 0; i < sequences.size(); i++) {
			System.out.println(sequences.get(i).toString());
		}
		System.out.println("**** " + algo + " Matched Patterns ***");
		Collections.sort(matchedPatterns);
		for (Pattern pattern : matchedPatterns) {
			System.out.println(pattern.toPrint());
		}
		System.out.println("**** " + algo + " Results ***");
		for (String prediction : result) {
			System.out.println(prediction);
		}
	}

	/***
	 * Predicts the page at position index for the given candidate session.
	 * 
	 * @param candidateSession
	 * @param index
	 */
	private void predict(Session candidateSession, int index) {
		String target = candidateSession.getSequence().get(index);
		boolean isTrivial = isTrivial(candidateSession);

		List<Sequence> toSequences = new ArrayList<>();
		for (int i = index; i >= 1; i--) {
			Session cutSession = cutSession(candidateSession, i);
			float penalty = (float) Math.pow(PENALTY_COEFFICIENT, (index - i));
			timeOriented.processSessionForPrediction(cutSession, toSequences, false, penalty);
		}
		List<Pattern> toMatchedPatterns = new ArrayList<>();
		Set<String> toSet = toPredictor.predictNextItem(toSequences, toMatchedPatterns);
		emptyPredictor[Algorithm.TO.getId()] += toSet.isEmpty() ? 1 : 0;
		successCount[Algorithm.TO.getId()] += toSet.contains(target.trim()) ? 1 : 0;

		List<Sequence> smartSRASequences = new ArrayList<>();
		for (int i = index; i >= 1; i--) {
			Session cutSession = cutSession(candidateSession, i);
			float penalty = (float) Math.pow(PENALTY_COEFFICIENT, (index - i));
			smartSRA.processSession(cutSession, smartSRASequences, false, penalty);
		}
		List<Pattern> ssraMatchedPatterns = new ArrayList<>();
		Set<String> ssraSet = smartSRAPredictor.predictNextItem(smartSRASequences, ssraMatchedPatterns);
		ssraSet = isTrivial ? toSet : ssraSet;
		emptyPredictor[Algorithm.SmartSRA.getId()] += ssraSet.isEmpty() ? 1 : 0;
		successCount[Algorithm.SmartSRA.getId()] += ssraSet.contains(target.trim()) ? 1 : 0;

		List<Sequence> csraSequences = new ArrayList<>();
		for (int i = index; i >= 1; i--) {
			Session cutSession = cutSession(candidateSession, i);
			float penalty = (float) Math.pow(PENALTY_COEFFICIENT, (index - i));
			completeSRA.processSession(cutSession, csraSequences, false, penalty);
		}
		List<Pattern> csraMatchedPatterns = new ArrayList<>();
		Set<String> csraSet = completeSRAPredictor.predictNextItem(csraSequences, csraMatchedPatterns);
		csraSet = isTrivial ? toSet : csraSet;
		emptyPredictor[Algorithm.CSRA.getId()] += csraSet.isEmpty() ? 1 : 0;
		successCount[Algorithm.CSRA.getId()] += csraSet.contains(target.trim()) ? 1 : 0;

		List<Sequence> ipSequences = new ArrayList<>();
		for (int i = index; i >= 1; i--) {
			Session cutSession = cutSession(candidateSession, i);
			float penalty = (float) Math.pow(PENALTY_COEFFICIENT, (index - i));
			integerProgramming.processSession(cutSession, ipSequences, false, penalty);
		}
		List<Pattern> ipMatchedPatterns = new ArrayList<>();
		Set<String> ipSet = ipPredictor.predictNextItem(ipSequences, ipMatchedPatterns);
		ipSet = isTrivial ? toSet : ipSet;
		emptyPredictor[Algorithm.IP.getId()] += ipSet.isEmpty() ? 1 : 0;
		successCount[Algorithm.IP.getId()] += ipSet.contains(target.trim()) ? 1 : 0;
		List<Sequence> noSequences = new ArrayList<>();
		for (int i = index; i >= 1; i--) {
			Session cutSession = cutSession(candidateSession, i);
			float penalty = (float) Math.pow(PENALTY_COEFFICIENT, (index - i));
			navigationOriented.processSession(cutSession, noSequences, false, penalty);
		}
		List<Pattern> noMatchedPatterns = new ArrayList<>();
		Set<String> noSet = noPredictor.predictNextItem(noSequences, noMatchedPatterns);
		noSet = isTrivial ? toSet : noSet;
		emptyPredictor[Algorithm.NO.getId()] += noSet.isEmpty() ? 1 : 0;
		successCount[Algorithm.NO.getId()] += noSet.contains(target.trim()) ? 1 : 0;
		numberOfTry++;
	}

	private boolean isTrivial(Session candidateSession) {
		if (candidateSession.getSequence().size() <= 1) {
			return true;
		} else {
			List<String> visitedPages = candidateSession.getSequence();
			List<String> references = candidateSession.getRefSequence();
			for (int i = 1; i < visitedPages.size(); i++) {
				if (!references.get(i).trim().equals(visitedPages.get(i - 1).trim())) {
					if (!references.get(i).trim().equals(LogParser.EXTERNAL_PLACEHOLDER)) {
						String reference = references.get(i);
						if (visitedPages.indexOf(reference) < (i - 1) 
								&& visitedPages.indexOf(reference) >= 0
								&& !reference.equals(visitedPages.get(i))) {
							if (i >= 2) {
								return false;
							}
						}
					}
				}
			}
		}
		return true;
	}

	private void processNonTrivialSession(Session candidateSession) {
		if (candidateSession.getSequence().size() <= 1) {
			numberOfTrivialSequences++;
			return;
		} else {
			List<String> visitedPages = candidateSession.getSequence();
			for (int i = 1; i < visitedPages.size(); i++) {
				predict(candidateSession, i);
			}
			numberOfComplexSequences++;
		}
	}

	public void printPerformance() {
		resultStream.println("Number Of Tries: " + numberOfTry);
		Algorithm[] algos = Algorithm.values();
		for (int i = 0; i < algos.length; i++) {
			resultStream.println(algos[i].toString() + " val: " + successCount[algos[i].getId()]);
		}
		System.out.println("Empty Predictor");
		for (int i = 0; i < algos.length; i++) {
			resultStream.println(algos[i].toString() + " empty val: " + emptyPredictor[algos[i].getId()]);
		}
		System.out.println(String.format("Number of Trivial: %1$s", numberOfTrivialSequences));
		System.out.println(String.format("Number of Complex: %1$s", numberOfComplexSequences));
	}

	@Override
	public void processSession(Session candidateSession, boolean skipSimpleSessions) {
		processNonTrivialSession(candidateSession);
	}

	@Override
	public void processSession(Session candidateSession, List<Sequence> outputSequences, boolean skipSimpleSessions,
			float penalty) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void processSessionForPrediction(Session candidateSession, List<Sequence> sequences,
			boolean skipSimpleSessions, float penalty) {
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) {
		if (args.length != 12) {
			System.out.print("Usage: PagePredictor <inputDir> <topologyFile> <outputFile> <numberOfPredictedItems> ");
			System.out.print("<maxTailCount> <TO-PatternsFile> <SmartSRA-PatternsFile> <CompleteSRA-PatternsFile> ");
			System.out.println("<IP-PatternsFile> <NO-PatternsFile> <domainName>");
			return;
		}

		String inputFolder = args[1];
		String topologyFile = args[2];
		String outputFile = args[3];
		String numberOfPredictedItems = args[4];
		String maxTailCount = args[5];
		String toPatterns = args[6];
		String ssraPatterns = args[7];
		String csraPatterns = args[8];
		String ipPatterns = args[9];
		String noPatterns = args[10];
		String domainName = args[11];

		MAX_TAIL_COUNT = Integer.parseInt(maxTailCount);
		PagePredictor pagePredictor = new PagePredictor(domainName, Integer.parseInt(numberOfPredictedItems));

		String resultFile = "Results\\PredictionResults-" + Integer.parseInt(numberOfPredictedItems) + "-"
				+ Integer.parseInt(maxTailCount) + ".txt";

		try {
			resultStream = new PrintStream(resultFile);
			outputStream = new PrintStream(outputFile);
			pagePredictor.loadTopology(topologyFile);
			pagePredictor.loadSessionGenerators();
			pagePredictor.loadModels(toPatterns, ssraPatterns, csraPatterns, ipPatterns, noPatterns);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		System.out.println("processing files");
		pagePredictor.ProcessFiles(inputFolder);
		pagePredictor.printPerformance();
		outputStream.close();
		resultStream.close();
	}
}
