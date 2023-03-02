package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CreditsMenu extends PaperMenu implements DisplaysText {
    @Inject
    public CreditsMenu(GuiSkinRepository skinRepository) {
        super(skinRepository);
//        FileHandle foundingBackers = Gdx.files.internal("core/assets/text/credits/founding_backers.txt");
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

        Table foundingBackers =  new Table();

        Table table = new Table();
        table.add(foundingBackers);
        return table;
    }

    @Override
    public void rebuildUI() {
        rebuild();
    }
}
