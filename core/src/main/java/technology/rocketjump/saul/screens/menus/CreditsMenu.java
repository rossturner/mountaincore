package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
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
    private final I18nTranslator i18nTranslator;

    @Inject
    public CreditsMenu(GuiSkinRepository skinRepository, I18nTranslator i18nTranslator) throws IOException {
        super(skinRepository);
        this.i18nTranslator = i18nTranslator;
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

        Others - 76px (152px) title ribbon, 62px secondary for name

        Title_military_ribbon is 130px tall in 4k
        asset_secondary_banner_title_bg is 116px tall in 4k

         */


        Label backersTitle = i18nTitleRibbon("GUI.CREDITS.FOUNDING_BACKERS_TITLE"); //Scaled to fit or wrap
        Label patreonKickstarterTitle = i18nTitleRibbon("GUI.CREDITS.PATREON_KICKSTARTER_TITLE"); //Scaled to fit or wrap
        Label andYouTitle = i18nTitleRibbon("GUI.CREDITS.AND_YOU_TITLE"); //Scaled to fit or wrap
        Table backersTable = thankYouTable(foundingBackers);
        Table patreonKickstarterTable = thankYouTable(patreonKickstarters);

        /*
        102px title ribbon
        46px for name

         */

        Table table = new Table();
        //todo: Lead Design & Programming
        //todo:Lead 2D & UI Artist                  Programming
        //todo: Music and sound effects             Concept Artwork
        //todo: character artists       Environment artists
        //todo: music tracks
        //TODO: special thanks



        table.add(backersTitle).padTop(68f).padBottom(68).width(2600).row();
        table.add(backersTable).row();
        table.add(patreonKickstarterTitle).padTop(68f).padBottom(68).width(2600).row();
        table.add(patreonKickstarterTable).row();
        table.add(andYouTitle).padTop(68f).padBottom(68).width(2200).row();
        ScrollPane scrollPane = new EnhancedScrollPane(table, skin);
        return scrollPane;
    }

    private Table thankYouTable(List<String> names) {
        Table backersTable =  new Table();
        int c = 0;
        for (String name : names) {
            Label label = thankYouLabel(name, 500);
            backersTable.add(label).width(680).growY().pad(18);
            if (c == 3) {
                backersTable.row();
                c = 0;
            } else {
                c++;
            }
        }
        return backersTable;
    }

    private Label i18nTitleRibbon(String i18nKey) {
        Label label = new ScaledToFitLabel(i18nTranslator.translate(i18nKey), skin, "title_ribbon", 2000);
        label.setAlignment(Align.center);
        return label;
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
