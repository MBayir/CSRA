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
 * Finds all possible session sequences based on link information
 * and optimize for the length of the session.
 * 
 * @author Murat Ali Bayir
 */
public class IntegerProgramming extends LinkBasedConstructor {

	private static Map<String, Mode> modeMap;
	
	static {
		modeMap = new HashMap<String, IntegerProgramming.Mode>();
		modeMap.put("topology", Mode.TOPOLOGYMODE);
		modeMap.put("referrer", Mode.REFERRERMODE);
	}
	
	private int maxExtensionCount;
	
	public IntegerProgramming(String domainName, Mode runningMode, int maxExtensionCount, boolean skipSimpleSessions) {
		super(domainName, runningMode, skipSimpleSessions);
		this.maxExtensionCount = maxExtensionCount;
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

	@Override
	public void processSession(
			Session candidateSession,
			List<Sequence> outputSequencesForPrediction,
			boolean skipSimpleSessions,
			float penalty) {

		if (isSimpleSession(candidateSession) && skipSimpleSessions) {
			return ;
		}

		ArrayList<Sequence> outputSequences 	= new ArrayList<Sequence>();
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

		Sequence longestSequence = null;
		for (int i = 0; i < outputSequences.size(); i++) {
			if (outputSequences.get(i).getLength() >= 1 && outputSequences.get(i).isMaximal()) {
				if (longestSequence == null) {
					longestSequence = outputSequences.get(i);
				} else if (longestSequence.getLength() < outputSequences.get(i).getLength()) {
					longestSequence = outputSequences.get(i);
				}
			}
		}
		if (longestSequence != null) {
			outputStream.println(longestSequence);
			longestSequence.setPenalty(penalty);
			outputSequencesForPrediction.add(longestSequence);
		}
	}

	public static void main(String[] args) {
		if(args.length != 6) {
			System.out.println("Usage: IntegerProgramming <inputDir> <topologyFile> <outputFile> <runMode> <domainName>");
			System.out.println("Where <runMode> is either 'topology' or 'referer'");
			return;
		}

		String inputFolder		= args[1];
		String topologyFile		= args[2];
		String outputFile		= args[3];
		String runningMode 		= args[4];
		String domainName		= args[5];

		IntegerProgramming integerProgramming =
				new IntegerProgramming(domainName, modeMap.get(runningMode), Integer.MAX_VALUE, false);
		
		try {
			outputStream = new PrintStream(outputFile);
			System.out.println("Topology File: " + topologyFile);
			integerProgramming.loadTopology(topologyFile);
		} catch (FileNotFoundException exception) {
			exception.printStackTrace();
		}
		integerProgramming.ProcessFiles(inputFolder);
	    outputStream.close();	
	}
}