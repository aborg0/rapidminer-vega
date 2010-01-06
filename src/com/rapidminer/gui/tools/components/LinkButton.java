package com.rapidminer.gui.tools.components;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;

/** Can be used as a label that triggers an action event on every link activation click. 
 *  The {@link Action#NAME} property of the action will be the label text. Note that
 *  it must contain a &lt;a&gt;> tag for this class to do something useful. The
 *  icon property of the action is not interpreted. 
 * 
 * @author Simon Fischer
 *
 */
public class LinkButton extends ExtendedHTMLJEditorPane {
	
	private static final long serialVersionUID = 1L;

	public LinkButton(final Action action) {
		super("text/html", (String)action.getValue(Action.NAME));
		setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
		installDefaultStylesheet();
		setEditable(false);
		setOpaque(false);
		addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == EventType.ACTIVATED) {
					action.actionPerformed(new ActionEvent(LinkButton.this, ActionEvent.ACTION_PERFORMED, (String)action.getValue(Action.ACTION_COMMAND_KEY)));
				}
			}
		});		
	}
}
