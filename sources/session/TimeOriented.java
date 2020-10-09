package session;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.Sequence;
import core.Session;

public class TimeOriented extends SessionConstructor {

	private static PrintStream outputStream;
	
	/**
	 * Session duration threshold for time oriented heuristics in minutes.
	 */
	private static long DURATION_THRESHOLD   = 30;

	public TimeOriented(String domainName, boolean skipSimpleSessions) {
		super(domainName, skipSimpleSessions);
	}

	@Override
	public void processSessionForPrediction(Session candidateSession,
			List<Sequence> sequences, boolean skipSimpleSessions, float penalty) {
		sequences.add(new Sequence(candidateSession.getSequence()));
	}
	
	@Override
	public void processSession(Session candidateSession,
			List<Sequence> sequences, boolean skipSimpleSessions, float penalty) {
		int lastIndex = candidateSession.getSequence().size();		
		for (int i = lastIndex; i <= lastIndex; i++) {
			StringBuffer prefix = new StringBuffer("");
			for (int j = 0; j < i; j++ ) {
				if (j != 0) {
					prefix.append("-");
				}
				prefix.append(candidateSession.getSequence().get(j).trim());
			}
			sequences.add(new Sequence(prefix.toString(), penalty));
		}
	}

	@Override
	public void processSession(Session candidateSession, boolean skipSimpleSessions) {
		if (!isSimpleSession(candidateSession) || !skipSimpleSessions) {
			outputStream.println(candidateSession.getSequenceAsString());
		}
	}

	/**
	 * Check whether the time criteria for appending current page to the 
	 * {@code session} is satisfied.
	 * 
	 * @param session the session to check time criteria
	 * @param visitTime the access time of new page
	 * @return true if the time criteria is satisfied, otherwise return false
	 */
	private boolean isTimeCriteriaSatisfied(Session session, long visitTime) {
		long duration = visitTime - session.getInitalTime();
		return duration <= DURATION_THRESHOLD;
	}

	@Override
	public void CreateSessionOrAppendPage(String ipNo, String visitedPage, String refUrl, long visitTime) {
		if(ipToSessions.containsKey(ipNo)) {
			Session existingSession = ipToSessions.get(ipNo);
			if(isTimeCriteriaSatisfied(existingSession, visitTime)) {
				existingSession.appendPage(visitedPage, refUrl, visitTime);
				ipToSessions.put(ipNo, existingSession);
			} else {
				Session expiredSession = ipToSessions.get(ipNo);
				processSession(expiredSession, skipSimpleSessions);
				numberOfTOSessions ++;
				//System.out.println("Constructed Session: " + numberOfTOSessions);
				ipToSessions.put(ipNo, new Session(ipNo, visitedPage, visitTime, visitTime, refUrl, numberOfTOSessions));
			}
		} else {
			numberOfTOSessions++;
			//System.out.println("Constructed Session: " + numberOfTOSessions);
			ipToSessions.put(ipNo, new Session(ipNo, visitedPage, visitTime, visitTime, refUrl, numberOfTOSessions));
		}
	}

	@Override
	public void processExpiredSessions(long currentTime) {
		Set<String> markToBeRemoved = new HashSet<>();
		for (String ipAdress : ipToSessions.keySet()) {	
			Session expiredSession 	= ipToSessions.get(ipAdress);
			if((currentTime - expiredSession.getInitalTime()) > DURATION_THRESHOLD) {
				processSession(expiredSession, skipSimpleSessions);
				markToBeRemoved.add(ipAdress);
			}
		}
		for (String ip : markToBeRemoved) {
			ipToSessions.remove(ip);
		}
	}

	public static void main(String[] args) {
		if(args.length != 4) {
			System.out.println("Usage: TimeOriented <inputDir> <outputFile> <domainName>");
			return;
		}

		String inputFolder		= args[1];
		String outputFile		= args[2];
		String domainName		= args[3];

		TimeOriented timeOriented = new TimeOriented(domainName, false);
		
		try {
			outputStream = new PrintStream(outputFile);
		} catch (FileNotFoundException exception) {
			exception.printStackTrace();
		}
		timeOriented.ProcessFiles(inputFolder);
	    outputStream.close();	
	}
}