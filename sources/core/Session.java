package core;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains definition of single session for all heuristics.
 * 
 * @author Murat Ali Bayir.
 *
 */
public class Session {

	private String ipNumber;
	private List<String> sequence;
	private List<String> referenceSequence;
	private long endTime;
	private long initalTime;
	private boolean isMaximal;
	private long id;

	public Session() {
	}

	public Session(String ipNumber, String initalPage, long endTime, long initalTime, String initialReferrer, long id) {
		super();
		this.ipNumber = ipNumber;
		this.sequence = new ArrayList<>();
		this.endTime = endTime;
		this.initalTime = initalTime;
		this.referenceSequence = new ArrayList<>();
		this.isMaximal = true;
		this.id = id;
		sequence.add(initalPage);
		referenceSequence.add(initialReferrer);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getIpNumber() {
		return ipNumber;
	}

	public void setIpNumber(String ipNumber) {
		this.ipNumber = ipNumber;
	}

	public List<String> getSequence() {
		return sequence;
	}

	public void setSequence(List<String> sequence) {
		this.sequence = sequence;
	}

	public long getEndTime() {
		return endTime;
	}

	public boolean isMaximal() {
		return isMaximal;
	}

	public void setMaximal(boolean isMaximal) {
		this.isMaximal = isMaximal;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public long getInitalTime() {
		return initalTime;
	}

	public void setInitalTime(long initalTime) {
		this.initalTime = initalTime;
	}

	public boolean isExist(String page) {
		return sequence.contains(page);
	}

	public List<String> getRefSequence() {
		return referenceSequence;
	}

	public void setRefSequence(List<String> refSequence) {
		this.referenceSequence = refSequence;
	}

	public void appendPage(String newPage, String referrer, long newEnd) {
		if (!sequence.contains(newPage)) {
			sequence.add(newPage);
			referenceSequence.add(referrer);
			endTime = newEnd;
		}
	}

	public void appendReferrer(String referrer) {
		referenceSequence.add(referrer);
	}

	public boolean isEmpty() {
		return sequence.isEmpty();
	}

	public void removeItem(String item) {
		int index = sequence.indexOf(item);
		sequence.remove(item);
		referenceSequence.remove(index);
	}

	public String getReferrer(String item) {
		int index = sequence.indexOf(item);
		return index >= 0 ? referenceSequence.get(index) : null;
	}

	private String sequenceToString(List<String> list) {
		StringBuffer buffer = new StringBuffer("");
		for (int i = 0; i < list.size(); i++) {
			if (i != 0) {
				buffer.append(" - ");
			}
			buffer.append(list.get(i));
		}
		return buffer.toString();
	}

	public String getSequenceAsString() {
		return sequenceToString(sequence);
	}

	public String getReferenceSequenceAsString() {
		return sequenceToString(referenceSequence);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("");
		buffer.append("Id: " + id);
		buffer.append("\n");
		buffer.append("IP: " + this.ipNumber);
		buffer.append("\n");
		buffer.append("Sequence: " + sequenceToString(sequence));
		buffer.append("\n");
		buffer.append("Ref Sequence: " + sequenceToString(referenceSequence));
		return buffer.toString();
	}
}
