package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.EnhancedScrollPane;
import technology.rocketjump.saul.ui.widgets.ScaledToFitLabel;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;

@Singleton
public class CreditsMenu extends PaperMenu implements DisplaysText {

    private final List<String> foundingBackers;
    private final List<String> patreonKickstarters;

    @Inject
    public CreditsMenu(GuiSkinRepository skinRepository) throws IOException {
        super(skinRepository);
        foundingBackers = FileUtils.readLines(Gdx.files.internal("assets/text/credits/founding_backers.csv").file());
        patreonKickstarters = FileUtils.readLines(Gdx.files.internal("assets/text/credits/patreon_kickstarter.csv").file());
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
        Table developerCredits = new Table();
        //remember mockup is 1080
        /*
        Ross -  108 px title ribbon, 82px secondary for name, Ross happy to use same sized ribbon as each of us

        Others - 76px title ribbon, 62px secondary for name

        asset_secondary_banner_title_bg is 116px tall in 4k

         */


        Table backersTable =  new Table();
        int c = 0;
        for (String foundingBacker : foundingBackers) {
            Label label = thankYouLabel(foundingBacker, 500);
            backersTable.add(label).width(680).growY().pad(18);


            if (c == 3) {
                backersTable.row();
                c = 0;
            } else {
                c++;
            }
        }

        /*
        102px title ribbon
        46px for name

        Secondary_Ribbon_Military is 80px tall in 4k
         */

        Table table = new Table();
        table.add(backersTable);
        ScrollPane scrollPane = new EnhancedScrollPane(table, skin);
        return scrollPane;
    }

    @Override
    public void rebuildUI() {
        rebuild();
    }

    private Label thankYouLabel(String name, int maxWidth) {
        ScaledToFitLabel label = new ScaledToFitLabel(name, managementSkin, "military_subtitle_ribbon", maxWidth);
        label.setAlignment(Align.center);
        return label;
    }
}
