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

        Label leadDesignTitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.LEAD_DESIGN", 1000);
        Label leadUITitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.LEAD_UI", 1000);
        Label programmingTitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.PROGRAMMING", 1000);
        Label musicTitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.MUSIC", 1000);
        Label conceptArtTitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.CONCEPT_ART", 1000);
        Label characterArtTitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.CHARACTER_ART", 1000);
        Label environmentArtTitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.ENVIRONMENT_ART", 1000);
        Label additionalMusicTitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.MUSIC_ADDITIONAL", 1000);
        Label specialThanksTitle = smallI18nTitleRibbon("GUI.CREDITS.DEVELOPERS.SPECIAL_THANKS", 1000);



        Label backersTitle = i18nTitleRibbon("GUI.CREDITS.FOUNDING_BACKERS_TITLE"); //Scaled to fit or wrap
        Label patreonKickstarterTitle = i18nTitleRibbon("GUI.CREDITS.PATREON_KICKSTARTER_TITLE"); //Scaled to fit or wrap
        Label andYouTitle = i18nTitleRibbon("GUI.CREDITS.AND_YOU_TITLE"); //Scaled to fit or wrap
        Table backersTable = thankYouTable(foundingBackers);
        Table patreonKickstarterTable = thankYouTable(patreonKickstarters);

        //todo:Lead 2D & UI Artist                  Programming
        //todo: Music and sound effects             Concept Artwork
        //todo: character artists       Environment artists
        //todo: music tracks
        //TODO: special thanks

        Table firstRow = new Table();
        firstRow.add(leadDesignTitle).row();
        firstRow.add(developerNameLabel("Ross Taylor-Turner", "http://rocketjump.technology/")).row();

        Table secondRow = new Table();
        secondRow.add(leadUITitle);
        secondRow.add(programmingTitle).row();
        secondRow.add(developerNameLabel("Ellen Elliott-Brown", "https://twitter.com/ebro__"));
        secondRow.add(developerNameLabel("Michael Rocke", null));

        Table thirdRow = new Table();
        thirdRow.add(musicTitle);
        thirdRow.add(conceptArtTitle).row();
        thirdRow.add(developerNameLabel("Jordan Chin", "http://www.jordanchinmusic.com/"));
        thirdRow.add(developerNameLabel("Anthony Avon", "https://www.artstation.com/artist/anthonyavon"));

        Table fourthRow = new Table();
        fourthRow.add(characterArtTitle);
        fourthRow.add(environmentArtTitle).row();
            Table characterArtistsTable = new Table();
            characterArtistsTable.add(developerNameLabel("Dave Rigley", null));
            characterArtistsTable.add(developerNameLabel("Katie-Beth Tutt", "https://katietutt.wixsite.com/website")).row();
            characterArtistsTable.add(developerNameLabel("Derek Restivo", "https://www.deviantart.com/derekrestivo")).colspan(2);
        fourthRow.add(characterArtistsTable);
            Table environmentArtistsTable = new Table();
            environmentArtistsTable.add(developerNameLabel("Rizal Zulkifli", "http://amade.deviantart.com/")).row();
            environmentArtistsTable.add(developerNameLabel("Katie-Beth Tutt", "https://katietutt.wixsite.com/website")).row();
        fourthRow.add(environmentArtistsTable);

        Table fifthRow = new Table();
        fifthRow.add(additionalMusicTitle).row();
            Table additionalMusicArtistsTable = new Table();
            additionalMusicArtistsTable.add(developerNameLabel("Juan I. Goncebat", "https://aerjaan.wordpress.com/"));
            additionalMusicArtistsTable.add(developerNameLabel("Francisco Rivera", "https://www.franciscosound.com/")).row();
            additionalMusicArtistsTable.add(developerNameLabel("Hannah (rimosound)", "http://rimosound.com/"));
            additionalMusicArtistsTable.add(developerNameLabel("Bettina Calmon", "http://www.bettinacalmon.com/")).row();
        fifthRow.add(additionalMusicArtistsTable);

        Table sixthRow = new Table();
        sixthRow.add(specialThanksTitle).row();
            Table specialThanksTable = new Table();
            specialThanksTable.add(developerNameLabel("Kenney Vleugels", "http://kenney.nl"));
            specialThanksTable.add(developerNameLabel("GoSquared (Flag icon set)", "https://www.gosquared.com/resources/flag-icons/")).row();
            specialThanksTable.add(developerNameLabel("Amit Patel", "http://www.redblobgames.com/"));
            specialThanksTable.add(developerNameLabel("Dennis Russell (Sprite DLight)", "http://www.2deegameart.com/p/sprite-dlight.html")).row();
            specialThanksTable.add(developerNameLabel("Azagaya (Laigter)", "https://github.com/azagaya/laigter")).colspan(2).row();
        sixthRow.add(specialThanksTable);
        //embark_ribbon 50pt font


        //TODO: hyperlinks to developers
        Table table = new Table();
        table.add(firstRow).row();
        table.add(secondRow).row();
        table.add(thirdRow).row();
        table.add(fourthRow).row();
        table.add(fifthRow).row();
        table.add(sixthRow).row();


        table.add(backersTitle).padTop(68f).padBottom(68).row();
        table.add(backersTable).row();
        table.add(patreonKickstarterTitle).padTop(68f).padBottom(68).row();
        table.add(patreonKickstarterTable).row();
        table.add(andYouTitle).padTop(68f).padBottom(68).row();
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
        Label label = new Label(i18nTranslator.translate(i18nKey), managementSkin, "military_title_ribbon"); //TODO: switch labels
        label.setAlignment(Align.center);
        return label;
    }

    private Label smallI18nTitleRibbon(String i18nKey, int maxWidth) {
        //todo: different style, Title_military_ribbon
        Label label = new Label(i18nTranslator.translate(i18nKey), managementSkin, "military_title_ribbon");
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

    private Label developerNameLabel(String name, String link) {
        Label label = new Label(name, managementSkin, "military_subtitle_ribbon");
        return label;
    }
}
