package session;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Sequence;
import core.Session;


/**
 * Base class for link based session construction methods.
 * 
 * @author Murat Ali Bayir
 *
 */
public class CompleteSRA extends LinkBasedConstructor {

	private static Map<String, Mode> modeMap;
	private Map<Integer, Integer> lengthMap;
	private Map<Integer, Integer> sequenceCountMap;

	static {
		modeMap = new HashMap<String, CompleteSRA.Mode>();
		modeMap.put("topology", Mode.TOPOLOGYMODE);
		modeMap.put("referrer", Mode.REFERRERMODE);
	}

	private int maxExtensionCount;

	/**
	 * Constructor for Complete SRA.
	 * 
	 * @param domainName          The domain name that current logs belongs to.
	 * @param runningMode.        The running mode the of the sessions
	 *                            reconstruction method.
	 * @param maxExtensionCount.  The threshold to control extension count per
	 *                            temporary session.
	 * @param skipSimpleSessions. Flag to skip simple sessions ie where input is
	 *                            already path on graph.
	 */
	public CompleteSRA(String domainName, Mode runningMode, int maxExtensionCount, boolean skipSimpleSessions) {
		super(domainName, runningMode, skipSimpleSessions);
		this.maxExtensionCount = maxExtensionCount;
		this.sequenceCountMap = new HashMap<Integer, Integer>();
		this.lengthMap = new HashMap<Integer, Integer>();
	}

	private boolean canExtend(Sequence inputSequence) {
		if (runningMode == Mode.REFERRERMODE) {
			return true;
		} else {
			return inputSequence.getNumberOfExtension() < inputSequence.getOutDegree()
					&& inputSequence.getNumberOfExtension() < maxExtensionCount;
		}
	}

	private Sequence extendSequence(Sequence inputSequence, String webPage, Session candidateSession) {
		String lastElement = inputSequence.getLastElement();
		if (isReferrer(lastElement, webPage, candidateSession)) {
			if (canExtend(inputSequence)) {
				inputSequence.setMaximal(false);
				inputSequence.setNumberOfExtension(inputSequence.getNumberOfExtension() + 1);
				Sequence newSequence = inputSequence.copy();
				newSequence.setPenalty(1.0f);
				newSequence.setMaximal(true);
				newSequence.addPage(webPage);
				newSequence.setOutDegree(topology.getNeighBours(webPage).size());
				newSequence.setNumberOfExtension(0);
				return newSequence;
			}
		}
		return null;
	}

	private Sequence createSequence(String webPage) {
		int outdegree = topology.getNeighBours(webPage).size();
		return new Sequence(webPage, outdegree);
	}

	@Override
	public void processSessionForPrediction(Session candidateSession,
			List<Sequence> sequences, boolean skipSimpleSessions, float penalty) {
		processSession(candidateSession, sequences, skipSimpleSessions, penalty);
	}

	/**
	 * Function to process single session coming from time oriented heuristics.
	 */
	@Override
	public void processSession(
			Session candidateSession,
			List<Sequence> outputSequencesForPrediction,
			boolean skipSimpleSessions,
			float penalty) {

		if (isSimpleSession(candidateSession) && skipSimpleSessions) {
			return;
		}
		ArrayList<Sequence> outputSequences = new ArrayList<Sequence>();
		List<String> webPages = candidateSession.getSequence();

		for (int i = 0; i < webPages.size(); i++) {
			String currentPage = webPages.get(i);
			boolean isAnyExtended = false;
			List<Sequence> tempSequences = new ArrayList<>();
			for (int j = 0; j < outputSequences.size(); j++) {
				Sequence newSequence = extendSequence(
						outputSequences.get(j),
						currentPage,
						candidateSession);
				isAnyExtended = (!isAnyExtended) ? (newSequence != null) : true;
				if (newSequence != null) {
					tempSequences.add(newSequence);
				}
			}
			if (!isAnyExtended) {
				tempSequences.add(createSequence(currentPage));
			}
			outputSequences.addAll(tempSequences);
		}
				
		int maximalCount = 0;
		for (int i = 0; i < outputSequences.size(); i++) {
			if (outputSequences.get(i).getLength() >= 1 && outputSequences.get(i).isMaximal()) {
				outputStream.println(outputSequences.get(i));
				outputSequences.get(i).setPenalty(penalty);
				outputSequencesForPrediction.add(outputSequences.get(i));
				maximalCount++;
				int len = outputSequences.get(i).getLength();
				if (lengthMap.containsKey(len)) {
					lengthMap.put(len, (lengthMap.get(len) + 1));
				} else {
					lengthMap.put(len, 1);
				}
			}
		}
		if (sequenceCountMap.containsKey(maximalCount)) {
			sequenceCountMap.put(maximalCount, (sequenceCountMap.get(maximalCount) + 1));
		} else {
			sequenceCountMap.put(maximalCount, 1);
		}
	}

	public void PrintStats() {
		System.out.println("Printing Len stats!");
		for (Integer key : lengthMap.keySet()) {
			System.out.println("Len " + key + " Count " + lengthMap.get(key));
		}
		System.out.println("Printing seq count stats!");
		for (Integer key : sequenceCountMap.keySet()) {
			System.out.println("Len " + key + " Count " + sequenceCountMap.get(key));
		}
	}

	public static void main(String[] args) {
		if (args.length != 6) {
			System.out.println("Usage: CompleteSRA <inputDir> <topologyFile> <outputFile> <runMode> <domainName>");
			System.out.println("Where <runMode> is either 'topology' or 'referer'");
			return;
		}

		String inputFolder		= args[1];
		String topologyFile		= args[2];
		String outputFile		= args[3];
		String runningMode 		= args[4];
		String domainName		= args[5];

		CompleteSRA completeSRA = new CompleteSRA(domainName,
				modeMap.get(runningMode),
				Integer.MAX_VALUE,
				false);
		
		try {
			outputStream = new PrintStream(outputFile);
			System.out.println("Topology File: " + topologyFile);
			completeSRA.loadTopology(topologyFile);
		} catch (FileNotFoundException exception) {
			exception.printStackTrace();
		}
		completeSRA.ProcessFiles(inputFolder);
		completeSRA.PrintStats();
		outputStream.close();
	}
}