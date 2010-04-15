/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;

import java.util.TreeSet;

import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Tupel;

/**
 * @author Sebastian Land
 *
 */
public class GSPSet extends ResultObjectAdapter {

	private static final long serialVersionUID = 4739128489323129098L;

	private TreeSet<Tupel<Sequence, Double>> sequences = new TreeSet<Tupel<Sequence, Double>>();

	private int maxTransactions = 0;
	
	public GSPSet() {}
	
	public void addSequence(Sequence sequence, double support) {
		sequences.add(new Tupel<Sequence, Double>(sequence, support));
		maxTransactions = Math.max(maxTransactions, sequence.size());
	}
	
	/**
	 * @return the maxTransactions
	 */
	public int getMaxTransactions() {
		return maxTransactions;
	}

	public int getNumberOfSequences() {
		return sequences.size();
	}

	public Sequence[] getSequenceArray() {
		Sequence[] sequences = new Sequence[this.sequences.size()];
		int i = 0;
		for (Tupel<Sequence, Double> tupel: this.sequences) {
			sequences[i] = tupel.getFirst();
			i++;
		}
		return sequences;
	}
	
	public double[] getSupportArray() {
		double[] supports = new double[sequences.size()];
		int i = 0;
		for (Tupel<Sequence, Double> tupel: sequences) {
			supports[i] = tupel.getSecond();
			i++;
		}
		return supports;
	}
	
	@Override
	public String toString() {
		return "Set of generalized sequential patterns" + Tools.getLineSeparator() + "Set contains " + sequences.size() + " patterns.";
	}
	
	@Override
	public String toResultString() {
		StringBuffer buffer = new StringBuffer();
		for (Tupel<Sequence, Double> sequencePair: sequences) {
			buffer.append(Tools.formatNumber(sequencePair.getSecond(), 3) + ": " + sequencePair.getFirst().toString() + Tools.getLineSeparator());
		}
		return buffer.toString();
	}
}
