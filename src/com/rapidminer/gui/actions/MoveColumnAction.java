package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.ResourceAction;

/**
 * Start the corresponding action.
 * 
 * Provided as a patch in bugreport #310.
 * 
 * @author Tobias Schlitt <tobias@schlitt.info>
 */
public class MoveColumnAction extends ResourceAction {

       private static final long serialVersionUID = -8676231093844470601L;

       private ExtendedJTable table;

       private int moveTo;

       public MoveColumnAction(ExtendedJTable table, IconSize size, int moveTo) {
               super("move_column", moveTo+1);
               this.table = table;
               this.moveTo = moveTo;
       }

       public void actionPerformed(ActionEvent e) {
               this.table.moveColumn(table.getSelectedColumn(), moveTo);
       }
}
