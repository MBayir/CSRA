package session;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import core.Sequence;
import core.Session;

public class NavigationOriented extends LinkBasedConstructor {

	public NavigationOriented(String domainName, Mode runningMode, boolean skipSimpleSessions) {
		super(domainName, runningMode, skipSimpleSessions);
	}

	@Override
	public void processSession(Session candidateSession,
			List<Sequence> outputSequencesForPrediction,
			boolean skipSimpleSessions,
			float penalty) {
		if (isSimpleSession(candidateSession) && skipSimpleSessions) {
			return ;
		}
		List<String> originalSequence = new ArrayList<>();
		if (!candidateSession.getSequence().isEmpty()) {
			List<String> candidateSequence = candidateSession.getSequence();
			for (int i = 0; i < candidateSequence.size(); i++) {
				originalSequence.add(candidateSequence.get(i));
			}
		}
		List<Sequence> outputSequences = new ArrayList<>();

		Sequence currentSequence = null; 
		for (int i = 0; i < originalSequence.size(); i++) {
			if (i == 0) {
				currentSequence = new Sequence(candidateSession.getSequence().get(i));
			} else {
				String currentPage = originalSequence.get(i);
				// Check if i-1 has link towards.
				String previousPage = originalSequence.get(i-1);
				if (topology.checkLink(previousPage, currentPage)) {
					currentSequence.addPage(currentPage);
				} else {
					// Find most recent page that has link.
					boolean hasReferrer = false;
					for (int j = (i - 1); j >= 0; j--) {
						if (topology.checkLink(originalSequence.get(j), currentPage)) {
							hasReferrer = true;
							// Append all pages until j.
							for (int k = (i-1); k >= j; k--) {
								currentSequence.addPage(originalSequence.get(k));
							}
							break;
						}
					}
					if (hasReferrer) {
						currentSequence.addPage(currentPage);
					} else {
						outputSequences.add(currentSequence);
						currentSequence = new Sequence(currentPage);
					}
				}
				
			}
		}
		if (currentSequence != null) {
			outputSequences.add(currentSequence);
		}
		
		for(int i = 0; i < outputSequences.size(); i++) {
			if(outputSequences.get(i).getLength() >= 1 && outputSequences.get(i).isMaximal()) {
				outputStream.println(outputSequences.get(i));
				outputSequences.get(i).setPenalty(penalty);
				outputSequencesForPrediction.add(outputSequences.get(i));
			}
		}
	}

	@Override
	public void processSessionForPrediction(Session candidateSession,
			List<Sequence> sequences, boolean skipSimpleSessions, float penalty) {		
	}
	
	public static void main(String[] args) {
		if(args.length != 5) {
			System.out.println("Usage: NavigationOriented <inputDir> <topologyFile> <outputFile> <domainName>");
			return;
		}

		String inputFolder		= args[1];
		String topologyFile		= args[2];
		String outputFile		= args[3];
		String domainName		= args[4];

		NavigationOriented navigationOriented = new NavigationOriented(domainName, Mode.TOPOLOGYMODE, false);
		
		try {
			outputStream = new PrintStream(outputFile);
			navigationOriented.loadTopology(topologyFile);
		} catch (FileNotFoundException exception) {
			exception.printStackTrace();
		}
		navigationOriented.ProcessFiles(inputFolder);
	    outputStream.close();	
	}
}
