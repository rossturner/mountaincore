package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.model.EntityChildAssetDescriptor;
import technology.rocketjump.saul.entities.model.Entity;

import java.util.Collection;
import java.util.List;

public class ChildAssetsWidget extends VisTable {

	private final List<EntityChildAssetDescriptor> sourceData;
	private Collection<EntityAssetType> applicableTypes;
	private final VisTextButton addButton;

	public ChildAssetsWidget(List<EntityChildAssetDescriptor> sourceData, Collection<EntityAssetType> applicableTypes, MessageDispatcher messageDispatcher, Entity currentEntity) {
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
			this.add(new ChildAssetWidget(childDescriptor, applicableTypes)).left().expandX().fillX().row();
		}

		this.add(addButton).left().row();
	}
}
