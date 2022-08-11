package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.entities.model.StorableVector2;

public class OffsetPixelsWidget extends VisTable {


	public OffsetPixelsWidget(StorableVector2 sourceVector) {
		this.add(new VisLabel("Offset:")).left();

		this.add(new VisLabel("x")).right();
		this.add(WidgetBuilder.floatSpinner(sourceVector.getX(), -Float.MAX_VALUE, Float.MAX_VALUE, sourceVector::setX)).expandX().fillX().left();

		this.add(new VisLabel("y")).right();
		this.add(WidgetBuilder.floatSpinner(sourceVector.getY(), -Float.MAX_VALUE, Float.MAX_VALUE, sourceVector::setY)).expandX().fillX().left();
	}
}
