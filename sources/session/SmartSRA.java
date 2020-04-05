package session;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Sequence;
import core.Session;

public class SmartSRA extends LinkBasedConstructor {

	private static Map<String, Mode> modeMap;

	static {
		modeMap = new HashMap<String, SmartSRA.Mode>();
		modeMap.put("topology", Mode.TOPOLOGYMODE);
		modeMap.put("referrer", Mode.REFERRERMODE);
	}

	/**
	 * Constructor for Complete SRA
	 * 
	 * @param domainName          The domain name that current logs belongs to.
	 * @param runningMode.        The running mode the of the sessions
	 *                            reconstruction method.
	 * @param skipSimpleSessions. Flag to skip simple sessions ie where input is
	 *                            already path on graph.
	 */
	public SmartSRA(String domainName, Mode runningMode, boolean skipSimpleSessions) {
		super(domainName, runningMode, skipSimpleSessions);
	}

	@Override
	public void processSessionForPrediction(Session candidateSession, List<Sequence> sequences,
			boolean skipSimpleSessions, float penalty) {
		processSession(candidateSession, sequences, skipSimpleSessions, penalty);
	}

	@Override
	public void processSession(Session candidateSession, List<Sequence> outputSequencesForPrediction,
			boolean skipSimpleSessions, float penalty) {
		if (isSimpleSession(candidateSession) && skipSimpleSessions) {
			return;
		}

		ArrayList<Sequence> outputSequences = new ArrayList<Sequence>();
		while (!candidateSession.isEmpty()) {
			ArrayList<Sequence> tempSequences = new ArrayList<Sequence>();
			List<String> items = candidateSession.getSequence();
			ArrayList<String> pagesWithoutReferrer = new ArrayList<String>();

			// Find the set of pages which does not have any referrer in the current
			// session.
			for (int i = 0; i < items.size(); i++) {
				String toPage = items.get(i);
				boolean startPageFlag = true;
				for (int j = 0; j < i; j++) {
					String fromPage = items.get(j);
					if (isReferrer(fromPage, toPage, candidateSession)) {
						startPageFlag = false;
						break;
					}
				}
				if (startPageFlag) {
					pagesWithoutReferrer.add(toPage);
				}
			} // End of First For!.. // we find startPages...

			// Check if the new session set is empty.
			if (outputSequences.isEmpty()) {
				for (int i = 0; i < pagesWithoutReferrer.size(); i++) {
					Sequence tempSequence = new Sequence(pagesWithoutReferrer.get(i));
					tempSequences.add(tempSequence);
				}
			} else {
				for (int i = 0; i < pagesWithoutReferrer.size(); i++) {
					String currentPage = pagesWithoutReferrer.get(i);
					for (int j = 0; j < outputSequences.size(); j++) {
						Sequence currentSequence = outputSequences.get(j);
						String lastElement = currentSequence.getLastElement();
						if (isReferrer(lastElement, currentPage, candidateSession)) {
							Sequence tempSeq = currentSequence.copy();
							tempSeq.setMaximal(true);
							tempSeq.addPage(currentPage);
							tempSequences.add(tempSeq);
							currentSequence.setMaximal(false); // since extended
						}
					}
				} // End
			} // End of if newSessionSize

			// Add the maximal sequences that are not extended in this round.
			for (int i = 0; i < outputSequences.size(); i++) {
				Sequence currentSequence = outputSequences.get(i);
				if (currentSequence.isMaximal()) {
					tempSequences.add(currentSequence);
				}
			}

			outputSequences = tempSequences;

			// Remove the processed pages from the candidate session.
			for (int i = 0; i < pagesWithoutReferrer.size(); i++) {
				candidateSession.removeItem(pagesWithoutReferrer.get(i));
			}
		} // End of Whole While

		for (int i = 0; i < outputSequences.size(); i++) {
			if (outputSequences.get(i).getLength() >= 1 && outputSequences.get(i).isMaximal()) {
				outputStream.println(outputSequences.get(i));
				outputSequences.get(i).setPenalty(penalty);
				outputSequencesForPrediction.add(outputSequences.get(i));
			}
			// System.out.println(newSessionSet.get(i).getSequence());
		}
	}

	public static void main(String[] args) {
		if (args.length != 6) {
			System.out.println("Usage: SmartSRA <inputDir> <topologyFile> <outputFile> <runMode> <domainName>");
			System.out.println("Where <runMode> is either 'topology' or 'referer'");
			return;
		}

		String inputFolder = args[1];
		String topologyFile = args[2];
		String outputFile = args[3];
		String runningMode = args[4];
		String domainName = args[5];

		SmartSRA smartSRA = new SmartSRA(domainName, modeMap.get(runningMode), false);

		try {
			outputStream = new PrintStream(outputFile);
			smartSRA.loadTopology(topologyFile);
		} catch (FileNotFoundException exception) {
			exception.printStackTrace();
		}
		smartSRA.ProcessFiles(inputFolder);
		outputStream.close();
	}
}