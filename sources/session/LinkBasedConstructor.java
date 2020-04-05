package session;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.Sequence;
import core.Session;

public abstract class LinkBasedConstructor extends SessionConstructor {

	public enum Mode {
		TOPOLOGYMODE,
		REFERRERMODE;
	}

	protected Mode runningMode;
	protected static PrintStream outputStream;
	protected static long DURATION_THRESHOLD   = 30; //15 min
	protected static long PAGESTAY_THRESHOLD = 10;  //1 min

	public LinkBasedConstructor(String domainName, Mode runningMode, boolean skipSimpleSessions) {
		super(domainName, skipSimpleSessions);
		this.runningMode = runningMode;
	}

	/**
	 * Checks whether the {@code fromPage} is referrer of {@code toPage}.
	 * 
	 * @param fromPage the from url
	 * @param toPage the to url
	 * @param session the session contains from and to urls
	 * @return true if the {@code fromPage} is referrer of {@code fromPage} otherwise returns false
	 */
	protected boolean isReferrer(String fromPage, String toPage, Session session) {
		if (runningMode.equals(Mode.REFERRERMODE)) {
			return session.getReferrer(toPage).equals(fromPage);
		} else {
			return topology.checkLink(fromPage, toPage);
		}
	}
	
	@Override
	public void CreateSessionOrAppendPage(String ipNo, String visitedPage, String refUrl, long visitTime) {
		if(ipToSessions.containsKey(ipNo)) {
			Session temp = ipToSessions.get(ipNo);
			if((visitTime - temp.getInitalTime()) <= DURATION_THRESHOLD) {
				temp.appendPage(visitedPage, refUrl, visitTime);
				ipToSessions.put(ipNo, temp);
			} else {
				Session expiredSession = ipToSessions.get(ipNo);
				processSession(expiredSession, skipSimpleSessions);
				numberOfTOSessions ++;
				//System.out.println("Constructed Session: " + numberOfTOSessions);
				ipToSessions.put(ipNo, new Session(ipNo, visitedPage, visitTime, visitTime, refUrl, numberOfTOSessions));
			}
		} else {
			numberOfTOSessions ++;
			//System.out.println("Constructed Session: " + numberOfTOSessions);
			ipToSessions.put(ipNo, new Session(ipNo, visitedPage, visitTime, visitTime, refUrl, numberOfTOSessions));
		}
	}
	
	/**
	 * Processes the candidate session by applying current link based session construction
	 * algorithm and stores the generated sequences in outputsequences.
	 * 
	 * @param candidateSession the input session to process
	 * @param outputSequences stores the generated sequences
	 * @param skipSimpleSessions the flag to skip simple sessions
	 * @param penalty the penalty coefficient for all sequences generated from candidate session
	 */
	public abstract void processSession(
			Session candidateSession,
			List<Sequence> outputSequences,
			boolean skipSimpleSessions,
			float penalty);

	@Override
	public void processSession(Session candidateSession, boolean skipSimpleSessions) {
		List<Sequence> outputSequences 	= new ArrayList<>();
		processSession(candidateSession, outputSequences, skipSimpleSessions, 1.0f);
	}

	public void loadTopology(String topologyFile) {
		topology.readTopology(topologyFile);
	}
	
	@Override
	public void processExpiredSessions(long currentTime) {
		Set<String> markToBeRemoved = new HashSet<>();
		for (String ipAdress : ipToSessions.keySet()) {
			Session expiredSession 	= ipToSessions.get(ipAdress);
			if((currentTime - expiredSession.getInitalTime()) > DURATION_THRESHOLD) {
				markToBeRemoved.add(ipAdress);
				processSession(expiredSession, skipSimpleSessions);
			}
		}
		for (String ip : markToBeRemoved) {
			ipToSessions.remove(ip);
		}
	}
}
