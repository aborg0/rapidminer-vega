/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;


/**
 * @author Sebastian Land
 *
 */
public class HashTreeRootNode extends HashTreeInnerNode {
	/**
	 * This method implements the root node behavior of counting. t is ignored, depth assumed to be
	 * zero anyway.
	 */
	@Override
	public void countCoveredCandidates(DataSequence sequence, double t, CountingInformations counting) {
		for (Transaction transaction: sequence) {
			for (Item item: transaction) {
				HashTreeNode child = children.get(item);
				if (child != null) {
					child.countCoveredCandidates(sequence, transaction.getTime(), counting);
				}
			}
		}
	}
}
