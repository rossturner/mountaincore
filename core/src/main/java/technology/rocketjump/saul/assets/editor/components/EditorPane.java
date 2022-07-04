package technology.rocketjump.saul.assets.editor.components;

import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;

public class EditorPane extends VisTable {

	private final VisTable editorTable;

	@Inject
	public EditorPane() {
		editorTable = new VisTable();
		editorTable.add(new VisLabel("TODO: Put stuff here")).pad(5);
		VisScrollPane editorScrollPane = new VisScrollPane(editorTable);

//		this.setDebug(true);
		this.background("window-bg");
		this.add(new VisLabel("Property Editor")).left().row();
		this.add(editorScrollPane).top().row();
		this.add(new VisTable()).expandY();
	}
	
}
