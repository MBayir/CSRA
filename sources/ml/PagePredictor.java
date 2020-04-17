package ml;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import pattern.Pattern;
import session.CompleteSRA;
import session.IntegerProgramming;
import session.LinkBasedConstructor;
import session.NavigationOriented;
import session.SessionConstructor;
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
	
	private BayesianPredictor[] predictors;
	private SessionConstructor[] sessionConstructors;
	

	public PagePredictor(String domainName, int numberOfPredictedItem) {
		super(domainName, Mode.TOPOLOGYMODE, true);
		
		sessionConstructors = new SessionConstructor[Algorithm.values().length];
		sessionConstructors[Algorithm.TO.id] = new TimeOriented(domainName, true);
		sessionConstructors[Algorithm.SmartSRA.id] = new SmartSRA(domainName, Mode.TOPOLOGYMODE, true);
		sessionConstructors[Algorithm.CSRA.id] = new CompleteSRA(domainName, Mode.TOPOLOGYMODE, Integer.MAX_VALUE, true);
		sessionConstructors[Algorithm.IP.id] = new IntegerProgramming(domainName, Mode.TOPOLOGYMODE, Integer.MAX_VALUE, true);
		sessionConstructors[Algorithm.NO.id] = new NavigationOriented(domainName, Mode.TOPOLOGYMODE, true);
				
		predictors = new BayesianPredictor[Algorithm.values().length];
		predictors[Algorithm.TO.id] = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		predictors[Algorithm.SmartSRA.id] = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		predictors[Algorithm.CSRA.id] = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		predictors[Algorithm.IP.id] = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		predictors[Algorithm.NO.id] = new BayesianPredictor(numberOfPredictedItem, MAX_TAIL_COUNT);
		
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
		sessionConstructors[Algorithm.SmartSRA.id].setTopology(topology);
		sessionConstructors[Algorithm.CSRA.id].setTopology(topology);
		sessionConstructors[Algorithm.IP.id].setTopology(topology);
		sessionConstructors[Algorithm.NO.id].setTopology(topology);
	}

	public void loadModels(String toPatterns, String ssraPatterns, String csraPatterns, String ipPatterns,
			String noPatterns) throws IOException {
		predictors[Algorithm.TO.id].loadModel(toPatterns);
		System.out.println("Time oriented Predictor model is loaded!");
		predictors[Algorithm.SmartSRA.id].loadModel(ssraPatterns);
		System.out.println("Smart SRA Predictor model is loaded!");
		predictors[Algorithm.CSRA.id].loadModel(csraPatterns);
		System.out.println("Complete SRA Predictor model is loaded!");
		predictors[Algorithm.IP.id].loadModel(ipPatterns);
		System.out.println("IP Predictor model is loaded!");
		predictors[Algorithm.NO.id].loadModel(noPatterns);
		System.out.println("NO Predictor model is loaded!");
	}

	/**
	 * Creates a session that has subset of session original sessions from [0...cutPoint].
	 * 
	 * @param inputSession
	 * @param cutPoint
	 * @return the sub session that contains pages from [0...cutpoint]
	 */
	private Session cutSession(Session inputSession, int cutPoint) {
		List<String> visitedPages = new ArrayList<>();
		List<String> referrers = new ArrayList<>();
		for (int i = 0; i < cutPoint; i++) {
			visitedPages.add(inputSession.getSequence().get(i));
			referrers.add(inputSession.getRefSequence().get(i));
		}
		Session shortSession = new Session();
		shortSession.setSequence(visitedPages);
		shortSession.setRefSequence(referrers);
		shortSession.setId(inputSession.getId());
		shortSession.setIpNumber(inputSession.getIpNumber());
		shortSession.setInitalTime(inputSession.getInitalTime());
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
	 * @param candidateSession The candidate session that current function is predicting for.
	 * @param index The index in candidate session we're predicting for.
	 */
	private void predict(Session candidateSession, int index) {
		String target = candidateSession.getSequence().get(index);
		for (Algorithm algo : Algorithm.values()) {
			List<Sequence> possibleSequences = new ArrayList<>();
			for (int i = index; i >= 1; i--) {
				// Sub session from [0...cutpoint = i].
				Session cutSession = cutSession(candidateSession, i);
				float penalty = (float) Math.pow(PENALTY_COEFFICIENT, (index - i));
				sessionConstructors[algo.id].processSession(cutSession, possibleSequences, false, penalty);
			}
			List<Pattern> matchedPatterns = new ArrayList<>();
			Set<String> predictions = predictors[algo.id].predictNextItem(possibleSequences, matchedPatterns);
			emptyPredictor[algo.getId()] += predictions.isEmpty() ? 1 : 0;
			successCount[algo.getId()] += predictions.contains(target.trim()) ? 1 : 0;
		}
		numberOfTry++;
	}

	/** 
	 * If session size is 1, we skip as we can not cut this session to predict
	 * next page. 
	 * 
	 * @param candidateSession
	 */
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
