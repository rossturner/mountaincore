package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
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

		public abstract void onSelect();

	}

	public SelectItemDialog(I18nText titleText, Skin skin, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary,
	                        TooltipFactory tooltipFactory, List<? extends Option> options, int optionsPerRow) {
		super(titleText, skin, messageDispatcher, soundAssetDictionary);


		Table selectionTable = new Table();
		selectionTable.top();
		ScrollPane scrollPane = new EnhancedScrollPane(selectionTable, skin);

		int numAdded = 0;
		int numRows = 0;
		for (Option option : options) {
			Table innerTable = new Table();

			option.addSelectionComponents(innerTable);


			innerTable.setTouchable(Touchable.enabled);
			innerTable.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					option.onSelect();
					close();
				}
			});

			innerTable.addListener(new ChangeCursorOnHover(innerTable, GameCursor.SELECT, messageDispatcher));
			innerTable.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
			if (option.getTooltipText() != I18nText.BLANK) {
				tooltipFactory.simpleTooltip(innerTable, option.getTooltipText(), TooltipLocationHint.BELOW);
			}

			selectionTable.add(innerTable).spaceRight(40).spaceLeft(40);
			numAdded++;

			if (numAdded % optionsPerRow == 0) {
				selectionTable.row();
				numRows++;
			}
		}

		if (numRows > 4) {
			scrollPane.setForceScroll(false, true);
			scrollPane.setFadeScrollBars(false);
			scrollPane.setScrollbarsVisible(true);
		}

		Cell<ScrollPane> cell = contentTable.add(scrollPane).growX();
		if (numRows > 4) {
			cell.height(1000);
		} else {
			cell.growY();
		}
	}

	@Override
	public void dispose() {

	}
}
