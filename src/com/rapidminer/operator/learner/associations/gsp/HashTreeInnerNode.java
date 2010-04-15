/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Sebastian Land
 *
 */
public class HashTreeInnerNode implements HashTreeNode {

	protected HashMap<Item, HashTreeNode> children = new HashMap<Item, HashTreeNode>(); 
	
	@Override
	public void addSequence(Sequence candidate, int candidateIndex, int depth, HashTreeNode father, ArrayList<Sequence> allCandidates) {
		Item currentDepthItem = candidate.getItem(depth);
		HashTreeNode child = children.get(currentDepthItem);
		if (child == null) {
			child = new HashTreeLeafNode();
			children.put(currentDepthItem, child);
		}
		child.addSequence(candidate, candidateIndex, depth + 1, this, allCandidates);
	}

	@Override
	public void replaceNode(Item whichItem, HashTreeNode replacement) {
		children.put(whichItem, replacement);
	}

	@Override
	public void countCoveredCandidates(DataSequence sequence, double t, CountingInformations counting) {
		double minTransactionTime = t - counting.windowSize;
		double maxTransactionTime = t + Math.max(counting.windowSize, counting.maxGap);
		
		for (Transaction transaction: sequence) {
			double transactionTime = transaction.getTime();
			if (transactionTime < maxTransactionTime && transactionTime > minTransactionTime) {
				for (Item item: transaction) {
					HashTreeNode child = children.get(item);
					if (child != null) {
						child.countCoveredCandidates(sequence, transactionTime, counting);
					}
				}
			}
		}
	}
}
