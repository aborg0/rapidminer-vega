package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.repository.RepositoryLocation;

/** Replaces an absolute reference to a repository entry by an entry resolved relative to
 *  the current process.
 * 
 * @author Simon Fischer
 *
 */
public class RelativizeRepositoryLocationQuickfix extends AbstractQuickFix {

	private String key;
	private Operator operator;

	public RelativizeRepositoryLocationQuickfix(Operator operator, String key, String value) {
		super(10, false, "relativize_repository_location", key, value);
		this.key = key;
		this.operator = operator;
	}

	@Override
	public void apply() {
		RepositoryLocation absLoc;
		try {
			absLoc = operator.getParameterAsRepositoryLocation(key);
			final RepositoryLocation processLoc = operator.getProcess().getRepositoryLocation().parent();
			if (processLoc == null) {
				SwingTools.showVerySimpleErrorMessage("quickfix_failed", "Process is not stored in repository.");
			} else {
				String relative = absLoc.makeRelative(processLoc);
				operator.setParameter(key, relative);
			}
		} catch (UserError e) {
			// Should not happen. Parameter should be set, otherwise we would not have created this prefix.
			SwingTools.showVerySimpleErrorMessage("quickfix_failed", e.toString());
		}		
	}

}
