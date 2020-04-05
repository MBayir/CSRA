package pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the basic definition of the pattern object
 * 
 * @author Murat Ali Bayir
 */
public class Pattern implements Comparable<Pattern> {
	private List<String> sequence;
	private float support;
	private boolean maximal;

	public Pattern() {
		super();
		sequence = new ArrayList<>();
		support = 0.0f;
		maximal = true;
	}

	public Pattern(String sequence, float support, boolean maximal) {
		this();
		String[] items = sequence.split("-");
		for (int i = 0; i < items.length; i++) {
			this.sequence.add(items[i].trim());
		}
		this.support = support;
		this.maximal = maximal;
	}

	public List<String> getSequence() {
		return sequence;
	}

	public void setSequence(List<String> sequence) {
		this.sequence = sequence;
	}

	public float getSupport() {
		return support;
	}

	public void setSupport(float support) {
		this.support = support;
	}

	public int getLength() {
		return sequence.size();
	}

	public boolean isMaximal() {
		return maximal;
	}

	public void setMaximal(boolean maximal) {
		this.maximal = maximal;
	}

	public String getLastItem() {
		return sequence.size() >= 1 ? sequence.get(sequence.size() - 1).trim() : null;
	}

	public String processSupport() {
		if (String.valueOf(this.support).length() > 5) {
			return String.valueOf(this.support).substring(0, 5);
		} else {
			return String.valueOf(this.support);
		}
	}

	public String toPrint() {

		StringBuffer buffer = new StringBuffer("");
		buffer.append(support);
		buffer.append(",");
		buffer.append(sequenceToString(sequence));
		return buffer.toString();
	}

	public String getKey() {
		StringBuffer buffer = new StringBuffer("");
		for (int i = 0; i < sequence.size(); i++) {
			if (i != 0) {
				buffer.append("-");
			}
			buffer.append(sequence.get(i));
		}
		return buffer.toString();
	}

	private String sequenceToString(List<String> list) {
		StringBuffer buffer = new StringBuffer("");
		for (int i = 0; i < list.size(); i++) {
			if (i != 0) {
				buffer.append("-");
			}
			buffer.append(list.get(i));
		}
		return buffer.toString();
	}

	public String head() {
		if (sequence.size() <= 1) {
			return sequence.isEmpty() ? "" : sequence.get(0);
		} else {
			StringBuffer buffer = new StringBuffer(sequence.get(0));
			for (int i = 0; i < (sequence.size() - 1); i++) {
				buffer.append("-");
				buffer.append(sequence.get(i));
			}
			return buffer.toString();
		}
	}

	public String tail() {
		if (sequence.size() <= 1) {
			return "";
		} else {
			StringBuffer buffer = new StringBuffer(sequence.get(1));
			for (int i = 2; i < sequence.size(); i++) {
				buffer.append("-");
				buffer.append(sequence.get(i));
			}
			return buffer.toString();
		}
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("");
		buffer.append("Sequence: " + sequenceToString(sequence));
		buffer.append("\n");
		buffer.append("Support: " + support);
		return buffer.toString();
	}

	@Override
	public int compareTo(Pattern that) {
		if (this.support > that.getSupport()) {
			return -1;
		} else if (support == that.getSupport()) {
			return 0;
		} else {
			return 1;
		}
	}

	public Pattern copy() {
		Pattern tempPattern = new Pattern();
		for (int i = 0; i < sequence.size(); i++) {
			tempPattern.getSequence().add(sequence.get(i));
		}
		tempPattern.setMaximal(isMaximal());
		return tempPattern;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (maximal ? 1231 : 1237);
		result = prime * result + ((sequence == null) ? 0 : sequence.hashCode());
		result = prime * result + Float.floatToIntBits(support);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Pattern other = (Pattern) obj;
		if (maximal != other.maximal) {
			return false;
		}
		if (sequence == null) {
			if (other.sequence != null) {
				return false;
			}
			;
		} else if (!sequence.equals(other.sequence))
			return false;
		if (Float.floatToIntBits(support) != Float.floatToIntBits(other.support)) {
			return false;
		}
		return true;
	}
}
