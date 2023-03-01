package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.scenes.scene2d.Actor;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CreditsMenu extends PaperMenu implements DisplaysText {
    @Inject
    public CreditsMenu(GuiSkinRepository skinRepository) {
        super(skinRepository);
    }

    @Override
    public void show() {
        reset();
    }

    @Override
    public void savedGamesUpdated() {
        //todo: shouldn't be in here
    }

    @Override
    protected Actor buildComponentLayer() {
        return null;
    }

    @Override
    public void rebuildUI() {
        rebuild();
    }
}
