package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.OperatorDocLoader;
import com.rapidminer.gui.OperatorDocViewer;
import com.rapidminer.gui.tools.ResourceAction;

public class RefreshHelpTextFromWikiAction extends ResourceAction {

	private static final long serialVersionUID = 1L;
	
	private OperatorDocViewer operatorDocViewer;

	public RefreshHelpTextFromWikiAction(boolean smallIcon, String i18nKey, Object[] i18nArgs, OperatorDocViewer operatorDocViewer) {
		super(smallIcon, i18nKey, i18nArgs);
		this.operatorDocViewer = operatorDocViewer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		OperatorDocLoader.clearOperatorCache();
		this.operatorDocViewer.refresh();
	}

}
