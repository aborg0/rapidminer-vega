package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.OperatorDocViewer;

public class ShowHelpTextAction extends ToggleAction {

	private final OperatorDocViewer operatorDocViewer;
	private static final long serialVersionUID = -8604443336707110762L;

	public ShowHelpTextAction(OperatorDocViewer operatorDocViewer) {
		super(true, "rapid_doc_bot_importer_offline");
		this.operatorDocViewer = operatorDocViewer;
		setSelected(true);
	}

	@Override
	public void actionToggled(ActionEvent e) {
		this.operatorDocViewer.refresh();
	}
}
