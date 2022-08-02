package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;


public class PropertyEditorPane extends VisTable {

	@Inject
	public PropertyEditorPane() {
		this.background("window-bg");
		this.defaults().left().top();

		clear();
	}

	public void clear() {
		this.clearChildren();
		this.add(new VisLabel("Property Editor")).row();
		this.add(new VisTable()).expandY();
	}

	public void setControls(VisTable controls) {
		this.clearChildren();
		this.add(new VisLabel("Property Editor")).row();
		VisScrollPane editorScrollPane = new VisScrollPane(controls);
		this.add(editorScrollPane).pad(10).expandY().row();
	}

}
