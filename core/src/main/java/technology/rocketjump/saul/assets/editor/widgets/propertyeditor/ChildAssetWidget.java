package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.model.EntityChildAssetDescriptor;

import java.util.Collection;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;

public class ChildAssetWidget extends VisTable {

	public ChildAssetWidget(EntityChildAssetDescriptor childDescriptor, Collection<EntityAssetType> applicableTypes) {
		VisSelectBox<EntityAssetType> assetTypeSelectBox = new VisSelectBox<>();
		assetTypeSelectBox.setItems(orderedArray(applicableTypes));
		if (childDescriptor.getType() == null) {
			childDescriptor.setType(applicableTypes.iterator().next());
		}
		assetTypeSelectBox.setSelected(childDescriptor.getType());
		assetTypeSelectBox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				childDescriptor.setType(assetTypeSelectBox.getSelected());
			}
		});
		this.add(new VisLabel("Asset type:")).left();
		this.add(assetTypeSelectBox).expandX().fillX().row();

		this.add(new OffsetPixelsWidget(childDescriptor.getOffsetPixels())).colspan(2).left().row();

		this.add(new VisLabel("Override Render Layer (optional)")).left();
		VisTextField renderLayerField = new VisTextField(childDescriptor.getOverrideRenderLayer() == null ? "" : String.valueOf(childDescriptor.getOverrideRenderLayer()));
		renderLayerField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					if (renderLayerField.getText().equals("")) {
						childDescriptor.setOverrideRenderLayer(null);
					} else {
						Integer newValue = Integer.valueOf(renderLayerField.getText());
						if (newValue != null) {
							childDescriptor.setOverrideRenderLayer(newValue);
						}
					}
				} catch (NumberFormatException e) {

				}
			}
		});
		this.add(renderLayerField).left().row();

		this.add(new VisLabel("Specified Asset Unique Name (optional):")).left().colspan(2).row();
		VisTextField specificNameField = new VisTextField(childDescriptor.getSpecificAssetName() == null ? "" : childDescriptor.getSpecificAssetName());
		specificNameField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String newValue = specificNameField.getText();
				if (newValue.equals("")) {
					childDescriptor.setSpecificAssetName(null);
				} else {
					childDescriptor.setSpecificAssetName(newValue);
				}
			}
		});
		this.add(specificNameField).left().expandX().fillX().colspan(2).row();

		this.addSeparator().colspan(2).row();
	}
}
