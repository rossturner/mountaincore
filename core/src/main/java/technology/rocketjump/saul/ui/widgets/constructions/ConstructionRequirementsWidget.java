package technology.rocketjump.saul.ui.widgets.constructions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rooms.constructions.Construction;
import technology.rocketjump.saul.settlement.ItemAvailabilityChecker;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.views.RoomEditorItemMap;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringFactory;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringLabelFactory;

import java.util.LinkedList;
import java.util.List;

@Singleton
public class ConstructionRequirementsWidget extends Table implements DisplaysText, GameContextAware {

	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final ItemAvailabilityChecker itemAvailabilityChecker;
	private final I18nTranslator i18nTranslator;
	private final RoomEditorItemMap roomEditorItemMap;
	private final EntityRenderer entityRenderer;
	private final TooltipFactory tooltipFactory;
	private final DecoratedStringFactory dedecoratedStringFactory;
	private final DecoratedStringLabelFactory decoratedStringLabelFactory;

	private Construction selectedConstruction;
	private GameContext gameContext;

	private final Table requirementsSection = new Table();
	private List<ConstructionRequirementWidget> widgets = new LinkedList<>();

	@Inject
	public ConstructionRequirementsWidget(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
										  ItemAvailabilityChecker itemAvailabilityChecker, I18nTranslator i18nTranslator,
										  RoomEditorItemMap roomEditorItemMap,
										  EntityRenderer entityRenderer, TooltipFactory tooltipFactory,
										  DecoratedStringFactory dedecoratedStringFactory, DecoratedStringLabelFactory decoratedStringLabelFactory) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.itemAvailabilityChecker = itemAvailabilityChecker;
		this.i18nTranslator = i18nTranslator;
		this.roomEditorItemMap = roomEditorItemMap;
		this.entityRenderer = entityRenderer;
		this.tooltipFactory = tooltipFactory;
		this.decoratedStringLabelFactory = decoratedStringLabelFactory;
		this.dedecoratedStringFactory = dedecoratedStringFactory;

		requirementsSection.defaults().padRight(40);
	}

	public void setSelectedConstruction(Construction construction) {
		if (construction == null) {
			selectedConstruction = null;
			return;
		}

		if (!construction.equals(selectedConstruction)) {
			this.selectedConstruction = construction;
			rebuildUI();
		}
	}

	public void update() {
		widgets.forEach(ConstructionRequirementWidget::update);
	}

	@Override
	public void rebuildUI() {
		this.clearChildren();
		requirementsSection.clearChildren();

		if (selectedConstruction == null) {
			return;
		}

		rebuildMaterialSelections();
		this.add(requirementsSection).center();
	}

	private void rebuildMaterialSelections() {
		requirementsSection.clearChildren();
		widgets.clear();

		for (QuantifiedItemTypeWithMaterial requirement : selectedConstruction.getRequirements()) {
			if (requirement.isLiquid()) {
				continue;
			}

			ConstructionRequirementWidget constructionRequirementWidget = new ConstructionRequirementWidget(requirement, selectedConstruction, roomEditorItemMap.getByItemType(requirement.getItemType()),
					skin, messageDispatcher, itemAvailabilityChecker, i18nTranslator, entityRenderer,
					tooltipFactory, gameContext, dedecoratedStringFactory, decoratedStringLabelFactory);
			widgets.add(constructionRequirementWidget);

			requirementsSection.add(constructionRequirementWidget);
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
