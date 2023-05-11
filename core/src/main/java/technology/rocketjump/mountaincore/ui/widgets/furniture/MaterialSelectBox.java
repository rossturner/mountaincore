package technology.rocketjump.mountaincore.ui.widgets.furniture;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;

import java.util.function.Consumer;

public class MaterialSelectBox {
    public static SelectBox<GameMaterial> create(GuiSkinRepository guiSkinRepository, I18nTranslator i18nTranslator,
                                                 MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary,
                                                 Consumer<GameMaterial> changeListener) {
        SelectBox<GameMaterial> materialSelect = new SelectBox<>(guiSkinRepository.getMenuSkin(), "select_narrow_alt") {
            @Override
            protected String toString(GameMaterial item) {
                if (item == GameMaterial.NULL_MATERIAL) {
                    return i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY").toString();
                } else {
                    return i18nTranslator.getTranslatedString(item.getI18nKey()).toString();
                }
            }

            @Override
            public void setItems(Array<GameMaterial> newItems) {
                super.setItems(newItems);

                if (newItems.size == 1) {
                    setDisabled(true);
                    setTouchable(Touchable.disabled);
                    getColor().a = 0.5f;
                } else {
                    setDisabled(false);
                    setTouchable(Touchable.enabled);
                    getColor().a = 1.0f;
                }
            }
        };
        materialSelect.setAlignment(Align.center);
        materialSelect.getList().setAlignment(Align.center);
        materialSelect.addListener(new ChangeCursorOnHover(materialSelect, GameCursor.SELECT, messageDispatcher));
        materialSelect.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary, "VeryLightHover", "ConfirmVeryLight"));
        materialSelect.getSelection().setProgrammaticChangeEvents(false);
        materialSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameMaterial selected = materialSelect.getSelected();
                if (selected == GameMaterial.NULL_MATERIAL) {
                    selected = null;
                }
                changeListener.accept(selected);
            }
        });

        return materialSelect;
    }
}
