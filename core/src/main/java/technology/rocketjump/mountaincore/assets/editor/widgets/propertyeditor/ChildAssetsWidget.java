package technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.entities.model.EntityChildAssetDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ChildAssetsWidget extends VisTable {

	private final List<EntityChildAssetDescriptor> sourceData;
	private final Collection<EntityAssetType> applicableTypes;
	private final VisTextButton addButton;
	private final Set<String> animationNames;

	public ChildAssetsWidget(List<EntityChildAssetDescriptor> sourceData, Collection<EntityAssetType> applicableTypes, Set<String> animationNames) {
		this.sourceData = sourceData;
		this.applicableTypes = applicableTypes;
		this.animationNames = animationNames;

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
			this.add(new ChildAssetWidget(childDescriptor, applicableTypes, animationNames)).left().expandX().fillX().row();
		}

		this.add(addButton).left().row();
	}
}
