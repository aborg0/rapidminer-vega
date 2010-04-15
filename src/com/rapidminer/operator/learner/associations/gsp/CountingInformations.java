/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Simple data holding class to avoid shifting to much information over stack
 * 
 * @author Sebastian Land
 *
 */
public class CountingInformations implements Serializable {
	private static final long serialVersionUID = 8189264534462569310L;
	
	public double windowSize;
	public double maxGap;
	public double minGap;
	public ArrayList<Sequence> allCandidates;
	public boolean[] candidateCounter;
	
	public CountingInformations(boolean[] candidateCounter, ArrayList<Sequence> allCandidates, double windowSize, double maxGap, double minGap) {
		this.candidateCounter = candidateCounter;
		this.windowSize = windowSize;
		this.maxGap = maxGap;
		this.minGap = minGap;
		this.allCandidates = allCandidates;
	}
}
