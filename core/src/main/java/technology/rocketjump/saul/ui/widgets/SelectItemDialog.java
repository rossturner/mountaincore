package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nText;

import java.util.List;

public class SelectItemDialog extends GameDialog {
	private static final int ITEMS_PER_ROW = 6;

	public static abstract class Option {
		private final I18nText tooltipText;

		public Option(I18nText tooltipText) {
			this.tooltipText = tooltipText;
		}

		public I18nText getTooltipText() {
			return tooltipText;
		}

		public abstract void addSelectionComponents(Table innerTable);

		public void reloadView() {
//			SettlerProfessionFactory.this.addProfessionComponents(settler, wholeTable, onProfessionChange);
		}

		public abstract void onSelect(Option option);

	}

	public SelectItemDialog(I18nText titleText, Skin skin, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary,
	                        TooltipFactory tooltipFactory, List<Option> options) {
		super(titleText, skin, messageDispatcher, soundAssetDictionary);


		Table selectionTable = new Table();
		selectionTable.left().top();
		ScrollPane scrollPane = new EnhancedScrollPane(selectionTable, skin);

		int numAdded = 0;
		for (Option option : options) {
			Table innerTable = new Table();

			option.addSelectionComponents(innerTable);


			innerTable.setTouchable(Touchable.enabled);
			innerTable.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					option.onSelect(option);
					option.reloadView();
					close();
				}
			});

			innerTable.addListener(new ChangeCursorOnHover(innerTable, GameCursor.SELECT, messageDispatcher));
			innerTable.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
			tooltipFactory.simpleTooltip(innerTable, option.getTooltipText(), TooltipLocationHint.BELOW);

			selectionTable.add(innerTable).spaceRight(40).spaceLeft(40);
			numAdded++;

			if (numAdded % ITEMS_PER_ROW == 0) {
				selectionTable.row();
			}
		}

		contentTable.add(scrollPane).expand().fill();
	}

	@Override
	public void dispose() {

	}
}
