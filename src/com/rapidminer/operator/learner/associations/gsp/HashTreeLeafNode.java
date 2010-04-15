/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;

import java.util.ArrayList;

/**
 * @author Sebastian Land
 *
 */
public class HashTreeLeafNode implements HashTreeNode {
	private int[] candidateIndices = new int[128];
	private int candidateIndicesSize = 0;

	@Override
	public void addSequence(Sequence candidate, int candidateIndex, int depth, HashTreeNode father, ArrayList<Sequence> allCandidates) {
		candidateIndicesSize++;
		if (candidateIndicesSize > candidateIndices.length) {
			if (depth < candidate.size() - 1) {
				// exchange this leaf node by inner node if it could become inner node
				HashTreeInnerNode newInner = new HashTreeInnerNode();
				father.replaceNode(candidate.getItem(depth - 1), newInner);
				
				// and adding all sequences and last candidate
				for (int i = 0; i < candidateIndices.length; i++) {
					newInner.addSequence(allCandidates.get(candidateIndices[i]), candidateIndices[i], depth, father, allCandidates);
				}
				newInner.addSequence(allCandidates.get(candidateIndex), candidateIndex, depth, father, allCandidates);
			} else {
				int[] newIndices = new int[candidateIndices.length * 2];
				System.arraycopy(candidateIndices, 0, newIndices, 0, candidateIndices.length);
				candidateIndices = newIndices;
				candidateIndices[candidateIndicesSize - 1] = candidateIndex;
			}
		} else {
			candidateIndices[candidateIndicesSize - 1] = candidateIndex;
		}
	}

	@Override
	public void countCoveredCandidates(DataSequence sequence, double t, CountingInformations counting) {
		//System.out.println(sequence.toString());
		for (int i = 0; i < candidateIndicesSize; i++) {
//			System.out.println("  " + Sequence.containsSequence(sequence, counting.allCandidates.get(candidateIndices[i]), counting) + ": "+ counting.allCandidates.get(candidateIndices[i]));
			
			counting.candidateCounter[candidateIndices[i]] = DataSequence.containsSequence(sequence, counting.allCandidates.get(candidateIndices[i]), counting);
		}
	}
	

	@Override
	public void replaceNode(Item which, HashTreeNode replacement) {
		// cannot occur in leaf node!
	}
}
