package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.OperatorDocImporter;
import com.rapidminer.gui.OperatorDocViewer;

public class ShowHelpTextAction extends ToggleAction {

	private final OperatorDocViewer operatorDocViewer;
	private static final long serialVersionUID = -8604443336707110762L;

	public ShowHelpTextAction(OperatorDocViewer operatorDocViewer) {
		super(true, "rapid_doc_bot_importer_offline");
		this.operatorDocViewer = operatorDocViewer;
		this.showNewHelptextFirstTime();
	}


	@Override
	public void actionToggled(ActionEvent e) {
		this.changeToolTip();
		this.showHelpText();
	}
	
	private void showNewHelptextFirstTime() {
		if (OperatorDocImporter.hostHasNetworkConnection()) {
			setSelected(true);
		}
		this.operatorDocViewer.refresh();
//		
//			RapidDocBotImporter.showNewHelptext(true, true, this.operatorDocViewer, null);
//		} else {
//			RapidDocBotImporter.showNewHelptext(false, false, this.operatorDocViewer, null);
//		}
	}

	private void showHelpText() {
		this.operatorDocViewer.refresh();
//		if (isSelected()) {
//			RapidDocBotImporter.showNewHelptext(true, true, this.operatorDocViewer, null);
//		} else {
//			RapidDocBotImporter.showNewHelptext(false, false, this.operatorDocViewer, null);
//		}
	}

	private void changeToolTip() {
	}

}
