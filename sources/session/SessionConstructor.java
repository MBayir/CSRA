package session;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.List;

import parsing.LogParser;
import topology.Topology;
import core.Sequence;
import core.Session;

public abstract class SessionConstructor {
	
	protected String domainName;
	protected Hashtable<String, Session> ipToSessions;
	protected LogParser parser;
	protected Topology topology;
	protected long numberOfTOSessions;
	protected boolean skipSimpleSessions;
	protected float stepPenalty = 0.1f;
	
	public SessionConstructor(String domainName, boolean skipSimpleSessions) {
		this.skipSimpleSessions = skipSimpleSessions;
		this.domainName = domainName;
		ipToSessions = new Hashtable<>();
		topology = new Topology();
		parser = new LogParser(domainName);
		numberOfTOSessions = 0;
		
	}
	
	public void setTopology(Topology topology) {
		this.topology = topology;
	}
	
	public void setIpToSessions(Hashtable<String, Session> ipToSessions) {
		this.ipToSessions = ipToSessions;
	}

	/**
	 * Process the session according to current session construction algorithm.
	 * 
	 * @param expiredSession the session to process
	 * @param skipSimpleSessions the flag to skip simple sessions
	 */
	public abstract void processSession(Session expiredSession,  boolean skipSimpleSessions);

	/**
	 * Process the session according to current session construction algorithm. And populate
	 * all potential prefix sequences before last visited page in the session.
	 * 
	 * @param candidateSession the candidate session to process
	 * @param sequences the all of the potential prefix sequences before last visited page in the session
	 * @param skipSimpleSessions the flag to skip simple sessions
	 * @param penalty the penalty coefficient for all sequences generated from candidate session
	 */
	public abstract void processSessionForPrediction(
			Session candidateSession,
			List<Sequence> sequences,
			boolean skipSimpleSessions,
			float penalty);
	
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

	/**
	 * Creates session with the current {@code visitedPage} or append this page to an exiting session
	 * with the same IP if the new page is visited in close proximity.
	 * 
	 * @param ipNo the ip number in the current web request
	 * @param visitedPage the visited page in the current web request
	 * @param refUrl the url that is visited before the current web request
	 * @param visitTime the visit time of the page
	 */
	public abstract void CreateSessionOrAppendPage(String ipNo, String visitedPage, String refUrl, long visitTime);
	
	
	/**
	 * Process the expired sessions based on current time.
	 * 
	 * @param currentTime the current time for checking expiration condition of
	 * 		  sessions constructed so far.
	 */
	public abstract void processExpiredSessions(long currentTime);
	
	/**
	 * Process single log and construct link based sessions.
	 * 
	 * @param fileName the file that contains raw logs
	 * @throws IOException if file is not valid
	 */
	public void processSingleLogFile(String fileName) throws IOException{
		FileInputStream fstream = null;
		DataInputStream in = null;
		BufferedReader br = null;
		String strLine = null;
		
		try {
			fstream = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		in = new DataInputStream(fstream);
		br = new BufferedReader(new InputStreamReader(in));
		int counter=0;
		while ((strLine = br.readLine()) != null) {
				String visitedUrl = parser.getRawUrl(parser.getUrl(strLine));
				String referenceUrl = parser.omitExternal(parser.getRawUrl(
						parser.getReferrerField(strLine)));
				String ipNum  = parser.getIPNumber(strLine);
				long time = parser.getAccessTime(strLine);
				if(visitedUrl.indexOf("-") != -1 || referenceUrl.indexOf("-") != -1) {
					continue;
				}
				CreateSessionOrAppendPage(ipNum, visitedUrl, referenceUrl, time);
				if((counter % 1000) == 0) {
					processExpiredSessions(time);
				}
				counter++;
		}
			br.close();
			in.close();
			fstream.close();
	}
	
	/**
	 * Checks whether a session is graph in web topology.
	 * 
	 * @param candidateSession
	 * @return
	 */
	 boolean isSimpleSession(Session candidateSession) {
		if (candidateSession.getSequence().size() <= 1) {
			return true;
		} else {
			List<String> visitedPages = candidateSession.getSequence();
			List<String> references = candidateSession.getRefSequence();
			for (int i = 1; i < visitedPages.size(); i++) {
				if (!references.get(i).trim().equals(visitedPages.get(i - 1).trim())) {
					if (!references.get(i).trim().equals(LogParser.EXTERNAL_PLACEHOLDER)) {
						String reference = references.get(i);
						if (visitedPages.indexOf(reference) < (i - 1) && visitedPages.indexOf(reference) >= 0) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Process each raw log file in the input folder.
	 * 
	 * @param inputFolder that contains all log files.
	 */
	public void ProcessFiles(String inputFolder) {
		File dir = new File(inputFolder);
	    File[] children = dir.listFiles();
	    if (children == null) {
	    	System.out.println("Either dir does not exist or is not a directory!");
	    } else {
	        for (int i = 0; i < children.length; i++) {
	            String filename = children[i].getPath();
	            System.out.println(i + " th: "  + filename + " is completed!");
	            try {
					processSingleLogFile(filename);
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	        processExpiredSessions(Long.MAX_VALUE);
	    }
	}
}
