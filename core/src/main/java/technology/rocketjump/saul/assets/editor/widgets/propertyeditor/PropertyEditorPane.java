package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;


//TODO: Kill me?
public class PropertyEditorPane extends VisTable {

	@Inject
	public PropertyEditorPane() {
		this.background("window-bg");
		clear();
		this.setDebug(true);
	}

	public void clear() {
		this.clearChildren();
		this.add(new VisLabel("Property Editor")).left().row();
	}

	public void setControls(VisTable controls) {
		this.clear();
		VisScrollPane editorScrollPane = new VisScrollPane(controls);
		this.add(editorScrollPane).pad(10).top().row();
	}

}
