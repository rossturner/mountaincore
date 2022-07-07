package technology.rocketjump.saul.assets.editor.components.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.model.EntityChildAssetDescriptor;

import java.util.Collection;
import java.util.List;

public class ChildAssetsComponent extends VisTable {

	private final List<EntityChildAssetDescriptor> sourceData;
	private Collection<EntityAssetType> applicableTypes;
	private final VisTextButton addButton;

	public ChildAssetsComponent(List<EntityChildAssetDescriptor> sourceData, Collection<EntityAssetType> applicableTypes) {
		this.sourceData = sourceData;
		this.applicableTypes = applicableTypes;

		addButton = new VisTextButton("Add another");
		addButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				sourceData.add(new EntityChildAssetDescriptor());
				reload();
			}
		});

		reload();
	}


	private void reload() {
		this.clearChildren();

		for (EntityChildAssetDescriptor childDescriptor : sourceData) {
			this.add(new ChildAssetComponent(childDescriptor, applicableTypes)).left().expandX().fillX().row();
		}

		this.add(addButton).left().row();
	}
}
