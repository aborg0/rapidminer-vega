/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;

import java.util.ArrayList;


/**
 * @author Sebastian Land
 *
 */
public interface HashTreeNode {

	public void replaceNode(Item which, HashTreeNode replacement);

	public void addSequence(Sequence candidate, int candidateIndex, int depth, HashTreeNode father, ArrayList<Sequence> allCandidates);

	public void countCoveredCandidates(DataSequence sequence, double t, CountingInformations counting);
}
