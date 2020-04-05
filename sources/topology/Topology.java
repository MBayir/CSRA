package topology;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class Topology {
	
	private Hashtable<String, HashSet<String>> topology;
	
	public Topology() {
		topology = new Hashtable<>();
	}
	
	public void addPair(String from, String to) {
		if (topology.containsKey(from)) {
			topology.get(from).add(to);
		} else {
			HashSet<String> toSet = new HashSet<>();
			toSet.add(to);
			topology.put(from, toSet);
		}
	}

	public Set<String> getKeySet() {
		return topology.keySet();
	}
	
	public boolean checkLink(String from, String to) {
		return topology.containsKey(from) && topology.get(from).contains(to);
	}

	public HashSet<String> getNeighBours(String from) {
		if (topology.containsKey(from)) {
			return topology.get(from);
		} else {
			return new HashSet<>();
		}
	}

	public void readTopology(String fileName) {
		FileInputStream fileStream = null;
		DataInputStream dataInputStream = null;
		BufferedReader reader = null;
		String strLine = null;
		
		try {
			fileStream = new FileInputStream(fileName);
			dataInputStream = new DataInputStream(fileStream);
			reader = new BufferedReader(new InputStreamReader(dataInputStream));
			while ((strLine = reader.readLine()) != null) {
				String[] pages = strLine.split(",");
				String fromPage = pages[0];
				HashSet<String> neighbours = new HashSet<String>();
				for(int i = 1; i < pages.length; i++) {
					neighbours.add(pages[i]);
				}
				topology.put(fromPage, neighbours);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
				dataInputStream.close();
				fileStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
