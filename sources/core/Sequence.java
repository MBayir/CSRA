package core;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a sequence of page views that forms path on
 * web graph. 
 * 
 * @author Murat Ali Bayir
 */
public class Sequence implements Comparable<Sequence> {
	
	private List<String> sequence;
	private int length;
	private boolean isMaximal;
	private int outDegree;
	private int numberOfExtension;
	private float penalty;
	private int step;

	private Sequence() {
		super();
		sequence = new ArrayList<>();
		isMaximal = true;
		outDegree = 0;
		numberOfExtension = 0;
		penalty = 1.0f;
	}
	
	public Sequence(String initialPage) {
		this();
		sequence.add(initialPage);
	}
	
	public Sequence(String initialPage, int outdegree) {
		this(initialPage);
		this.outDegree = outdegree;
	}
	
	public Sequence(String sequenceAsString, float penalty) {
		this();
		this.penalty = penalty;
		String[] items = sequenceAsString.split("-");
		for (int i = 0; i < items.length; i++) {
			sequence.add(items[i].trim());
		}
	}
	
	public Sequence(List<String> input) {
		this();
		for (int i = 0; i < input.size(); i++) {
			sequence.add(input.get(i));
		}
	}

	public float getPenalty() {
		return penalty;
	}

	public void setPenalty(float penalty) {
		this.penalty = penalty;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public String getLastElement(){
		return sequence.get(sequence.size() - 1);
	}
	
	public List<String> getSequence() {
		return sequence;
	}

	public void setSequence(List<String> sequence) {
		this.sequence = sequence;
	}

	public int getLength() {
		return sequence.size();
	}

	public boolean isMaximal() {
		return isMaximal;
	}

	public void addPage(String newPage){
		sequence.add(newPage);
	}
	
	public void setMaximal(boolean isMaximal) {
		this.isMaximal = isMaximal;
	}

	public int getOutDegree() {
		return outDegree;
	}

	public void setOutDegree(int outDegree) {
		this.outDegree = outDegree;
	}

	public int getNumberOfExtension() {
		return numberOfExtension;
	}

	public void setNumberOfExtension(int numberOfExtension) {
		this.numberOfExtension = numberOfExtension;
	}
	
	public boolean isExtensible() {
		return numberOfExtension < outDegree;
	}

	public Sequence copy(){
		Sequence tempSequence = new Sequence();
		for (int i = 0; i < sequence.size(); i++) {
			tempSequence.addPage(sequence.get(i));
		}
		tempSequence.setMaximal(this.isMaximal);
		return tempSequence;
	}

	@Override
	public int compareTo(Sequence o) {
		if(this.length > ((Sequence)o).getLength())
		{
			return -1;	
		}
		else
		{
			return 1;
		}
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("");
		for (int i = 0; i < sequence.size(); i++) {
			if (i != 0) {
				buffer.append("-");
			}
			buffer.append(sequence.get(i).trim());
		}
		return buffer.toString();
	}
}
