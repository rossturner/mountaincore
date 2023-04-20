package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.gamecontext.GameState;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.screens.ManagementScreenName;
import technology.rocketjump.mountaincore.screens.ResourceManagementScreen;
import technology.rocketjump.mountaincore.settlement.SettlementItemTracker;
import technology.rocketjump.mountaincore.ui.GuiArea;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.I18nWord;
import technology.rocketjump.mountaincore.ui.i18n.I18nWordClass;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.skins.MainGameSkin;
import technology.rocketjump.mountaincore.ui.skins.ManagementSkin;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;
import technology.rocketjump.mountaincore.ui.widgets.EnhancedScrollPane;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static technology.rocketjump.mountaincore.ui.i18n.I18nWordClass.NOUN;
import static technology.rocketjump.mountaincore.ui.i18n.I18nWordClass.PLURAL;

@Singleton
public class ResourceOverview implements GuiView, GameContextAware {

    public static final int OVERALL_INDENT_SPACING = 40;
    private final MainGameSkin mainGameSkin;
    private final ManagementSkin managementSkin;
    private final SettlementItemTracker settlementItemTracker;
    private final I18nTranslator i18nTranslator;
    private final MessageDispatcher messageDispatcher;
    private final TooltipFactory tooltipFactory;
    private final Comparator<TreeNodeValue> treeOrder;
    private final ButtonFactory buttonFactory;
    private final ResourceManagementScreen resourceManagementScreen;


    private Table containerTable;
    private TreeNode rootNode;
    private final Map<StockpileGroup, Label> stockpileGroupLabels = new HashMap<>();
    private final Map<ItemType, Label> itemTypeLabels = new HashMap<>();
    private boolean currentlyVisible = false;

    @Inject
    public ResourceOverview(GuiSkinRepository guiSkinRepository, SettlementItemTracker settlementItemTracker, I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher,
                            TooltipFactory tooltipFactory, ButtonFactory buttonFactory, ResourceManagementScreen resourceManagementScreen) {
        this.mainGameSkin = guiSkinRepository.getMainGameSkin();
        this.managementSkin = guiSkinRepository.getManagementSkin();
        this.settlementItemTracker = settlementItemTracker;
        this.i18nTranslator = i18nTranslator;
        this.messageDispatcher = messageDispatcher;
        this.tooltipFactory = tooltipFactory;

        treeOrder = Comparator.comparing((TreeNodeValue v) -> i18nTranslator.translate(v.stockpileGroup().getI18nKey()))
                .thenComparing((TreeNodeValue v) -> {
                    if (v.itemType() == null) {
                        return "";
                    } else {
                        return i18nTranslator.translate(v.itemType().getI18nKey());
                    }
                });
        this.buttonFactory = buttonFactory;
        this.resourceManagementScreen = resourceManagementScreen;
    }

    @Override
    public GuiViewName getName() {
        return GuiViewName.RESOURCE_OVERVIEW;
    }

    @Override
    public GuiViewName getParentViewName() {
        return null;
    }


    @Override
    public void onContextChange(GameContext gameContext) {
        stockpileGroupLabels.clear();
        itemTypeLabels.clear();
        rebuildTree();
    }

    @Override
    public void clearContextRelatedState() {
        stockpileGroupLabels.clear();
        itemTypeLabels.clear();
        containerTable.clearChildren();
    }


    @Override
    public void populate(Table containerTable) {
        this.containerTable = containerTable;
    }

    @Override
    public void update() {
        if (rootNode != null) {
            updateTree();
        }
    }

    public void toggleVisibility(GameContext gameContext, GuiViewName currentViewName, Set<GuiArea> hiddenGuiAreas) {
        boolean shouldShow = true;
        if (gameContext != null && gameContext.getSettlementState().getGameState() == GameState.SELECT_SPAWN_LOCATION) {
            shouldShow = false;
        } else if (currentViewName == GuiViewName.SQUAD_SELECTED) {
            shouldShow = false;
        } else if (hiddenGuiAreas.contains(GuiArea.RESOURCE_OVERVIEW)) {
            shouldShow = false;
        }

        if (currentlyVisible != shouldShow) {
            currentlyVisible = shouldShow;
            rebuildTree();
        }
    }

    private void rebuildTree() {
        containerTable.clearChildren();
        if (!currentlyVisible) {
            return;
        }

        float iconSpacing = 2.0f;
        Tree<TreeNode, TreeNodeValue> tree = new Tree<>(mainGameSkin, "resource_overview_tree");
        tree.setIconSpacing(iconSpacing, iconSpacing);
        tree.addListener(new ClickListener() {
            boolean enteredIcon = false;
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                TreeNode overNode = tree.getOverNode();
                if (overNode != null) {
                    float rowX = overNode.getActor().getX();
                    if (overNode.getIcon() != null) rowX -= iconSpacing + overNode.getIcon().getMinWidth();
                    if (x < rowX) {
                        if (!enteredIcon) {
                            messageDispatcher.dispatchMessage(MessageType.SET_HOVER_CURSOR, GameCursor.SELECT);
                            enteredIcon = true;
                        }
                    } else if (enteredIcon) {
                        messageDispatcher.dispatchMessage(MessageType.SET_HOVER_CURSOR, null);
                        enteredIcon = false;
                    }
                }

                return true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                messageDispatcher.dispatchMessage(MessageType.SET_HOVER_CURSOR, null);
                enteredIcon = false;
            }
        });

        Table rootNodeTable = new Table();
        rootNodeTable.add(new Image(mainGameSkin.getDrawable("Inv_Overview_Top_Level")));
        rootNode = new TreeNode();
        rootNode.setActor(rootNodeTable);
        tree.add(rootNode);

        updateTree();

        tree.setIndentSpacing(OVERALL_INDENT_SPACING);
        ScrollPane scrollPane = new EnhancedScrollPane(tree, mainGameSkin);
        scrollPane.setScrollBarPositions(true, false);
        scrollPane.setScrollbarsVisible(false);
        scrollPane.setForceScroll(false, true);
        scrollPane.layout(); //needed for first displaying scrollpane cuts off half of the root node, as it is fantastically stupid
        containerTable.add(scrollPane);
    }

    private void updateTree() {
        List<TreeNodeValue> byItemType = getSettlementItemTypeValues();
        List<TreeNodeValue> byStockpile = byItemType.stream()
                .map(node -> new TreeNodeValue(node.stockpileGroup, null, node.count))
                .collect(ArrayList::new,
                        (values, v) -> foldNodeValue(values, v, existing -> existing.stockpileGroup == v.stockpileGroup),
                        ArrayList::addAll);

        if (byStockpile.isEmpty() && rootNode.hasChildren()) {
            rootNode.clearChildren();
            itemTypeLabels.clear();
            stockpileGroupLabels.clear();
        }

        //hideous code to figure out inserting/removing nodes in the stockpile level of tree, without rebuilding whole tree
        for (int i = 0; i < byStockpile.size(); i++) {
            TreeNodeValue stockpileValue = byStockpile.get(i);
            StockpileGroup stockpileGroup = stockpileValue.stockpileGroup;
            Image stockpileImage = new Image(mainGameSkin.getDrawable(stockpileGroup.getOverviewDrawableName()));
            buttonFactory.attachClickCursor(stockpileImage, GameCursor.SELECT);
            stockpileImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    resourceManagementScreen.setSelectedStockpileGroup(stockpileGroup);
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, ManagementScreenName.RESOURCES.name());
                }
            });

            if (rootNode.getChildren().size > i) {
                TreeNode existingTreeNode = rootNode.getChildren().get(i);
                TreeNodeValue existingValue = existingTreeNode.getValue().get(0);//list type is horrible, but needed for the layout "continuous" black bg
                int orderComparison = treeOrder.compare(stockpileValue, existingValue);

                if (orderComparison == 0) { //if same stockpile, then update
                    Label label = stockpileGroupLabels.get(stockpileGroup);
                    if (label != null) {
                        label.setText(stockpileValue.count);
                    } else if (GlobalSettings.DEV_MODE) {
                        Logger.error("Fix me: stockpile group label not found for " + stockpileGroup);
                    }

                    updateItemTypeLabels(stockpileGroup, stockpileImage, existingTreeNode, byItemType);
                } else if (orderComparison < 0) { //if before entry, then insert child before
                    TreeNode stockpileNode = new TreeNode();
                    stockpileNode.setActor(stockpileTable(stockpileGroup, stockpileValue.count, stockpileImage));
                    stockpileNode.setValue(List.of(stockpileValue));

                    rootNode.insert(i, stockpileNode);
                    i--; //to compare again
                } else { //else delete tree node
                    rootNode.remove(existingTreeNode);
                    itemTypeLabels.keySet().removeIf(itemType -> Objects.equals(itemType.getStockpileGroup(), existingValue.stockpileGroup()));
                    i--; //to compare again
                }
            } else { //append to end
                TreeNode stockpileNode = new TreeNode();
                stockpileNode.setActor(stockpileTable(stockpileGroup, stockpileValue.count, stockpileImage));
                stockpileNode.setValue(List.of(stockpileValue));
                rootNode.add(stockpileNode);

                updateItemTypeLabels(stockpileGroup, stockpileImage, stockpileNode, byItemType);
            }
        }
        //trim tail of tree
        for (TreeNode child : new Array<>(rootNode.getChildren())) {
            TreeNodeValue value = child.getValue().get(0);
            if (byStockpile.stream().noneMatch(tnv -> Objects.equals(tnv.stockpileGroup, value.stockpileGroup))) {
                rootNode.remove(child);
                itemTypeLabels.keySet().removeIf(itemType -> Objects.equals(itemType.getStockpileGroup(), value.stockpileGroup()));
            }
        }

        //something gone wrong so rebuild tree, shouldn't be called
        if (byStockpile.size() != rootNode.getChildren().size) {
            stockpileGroupLabels.clear();
            itemTypeLabels.clear();
            rebuildTree();
        }

    }

    private void updateItemTypeLabels(StockpileGroup stockpileGroup, Image stockpileImage, TreeNode stockpileNode, List<TreeNodeValue> allItemTypeValues) {
        List<TreeNodeValue> itemTypeValuesForStockpile = allItemTypeValues.stream().filter(itemTypeValue -> itemTypeValue.stockpileGroup == stockpileGroup).toList();
        List<ItemType> itemTypes = itemTypeValuesForStockpile.stream().map(TreeNodeValue::itemType).toList();
        Set<ItemType> existingItemTypes = itemTypeLabels.keySet()
                .stream()
                .filter(itemType -> Objects.equals(stockpileGroup, itemType.getStockpileGroup()))
                .collect(Collectors.toSet());

        if (!stockpileNode.hasChildren()) {
            itemTypeLabels.keySet().removeAll(existingItemTypes);
            existingItemTypes = Collections.emptySet();
        }

        if (existingItemTypes.equals(new HashSet<>(itemTypes))) {
            for (TreeNodeValue itemTypeValue : itemTypeValuesForStockpile) {
                itemTypeLabels.get(itemTypeValue.itemType()).setText(getItemTypeText(itemTypeValue).toString());
            }
        } else {
            //lazily clear child and re-add, messing with tables is a nightmare
            Table itemTypesTable = new Table();
            itemTypesTable.background(mainGameSkin.getDrawable("Inv_Overview_BG"));

            Table itemTypesIndentedTable = new Table();
            itemTypesIndentedTable.add(new Container<>()).width(stockpileImage.getWidth() - OVERALL_INDENT_SPACING);
            itemTypesIndentedTable.add(itemTypesTable);
            TreeNode itemTypeNode = new TreeNode();
            itemTypeNode.setActor(itemTypesIndentedTable);
            itemTypeNode.setValue(itemTypeValuesForStockpile);

            stockpileNode.clearChildren();
            stockpileNode.add(itemTypeNode);

            for (TreeNodeValue itemTypeValue : itemTypeValuesForStockpile) {
                Label itemTypeLabel = getItemTypeLabel(itemTypeValue);
                Container<Label> labelContainer = new Container<>(itemTypeLabel);
                labelContainer.left();
                buttonFactory.attachClickCursor(labelContainer, GameCursor.SELECT);
                labelContainer.addListener(new InputListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        labelContainer.setBackground(managementSkin.getDrawable("accent_bg"));
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        if (pointer == -1) {
                            labelContainer.setBackground(null);
                        }
                    }
                });

                labelContainer.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        resourceManagementScreen.setSelectedStockpileGroup(stockpileGroup);
                        I18nWordClass i18nWordClass = itemTypeValue.count > 0 ? PLURAL : NOUN;
                        resourceManagementScreen.setSearchBarText(i18nTranslator.getTranslatedString(itemTypeValue.itemType().getI18nKey(), i18nWordClass).toString());
                        messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, ManagementScreenName.RESOURCES.name());
                    }
                });

                itemTypesTable.add(labelContainer).minWidth(350).growX().left().row();
            }
        }
    }

    private Label getItemTypeLabel(TreeNodeValue itemTypeValue) {
        I18nText labelText = getItemTypeText(itemTypeValue);

        Label itemTypeLabel = new Label(labelText.toString(), mainGameSkin);
        itemTypeLabel.setAlignment(Align.left);
        itemTypeLabels.put(itemTypeValue.itemType, itemTypeLabel);
        return itemTypeLabel;
    }

    private I18nText getItemTypeText(TreeNodeValue itemTypeValue) {
        I18nWordClass i18nWordClass = itemTypeValue.count > 0 ? PLURAL : NOUN;
        I18nText itemTypeText = i18nTranslator.getTranslatedString(itemTypeValue.itemType().getI18nKey(), i18nWordClass);
        return i18nTranslator.getTranslatedWordWithReplacements("GUI.RESOURCE_MANAGEMENT.QUANTIFIED_ITEM", Map.of(
                "quantity", new I18nWord(String.valueOf(itemTypeValue.count)),
                "item", itemTypeText
        ));
    }

    private Table stockpileTable(StockpileGroup stockpileGroup, int quantity, Image stockpileImage) {
        Label stockpileLabel = new Label(String.valueOf(quantity), mainGameSkin);
        stockpileLabel.setAlignment(Align.left);
        stockpileGroupLabels.put(stockpileGroup, stockpileLabel);

        tooltipFactory.simpleTooltip(stockpileImage, stockpileGroup.getI18nKey(), TooltipLocationHint.ABOVE);

        Table stockpileBackgroundTable = new Table();
        stockpileBackgroundTable.setBackground(mainGameSkin.getDrawable("Inventory_Overview_Ribbon_BG"));

        Table backgroundContainer = new Table();
        backgroundContainer.add(new Container<>()).width(stockpileImage.getWidth() / 2.0f).expand();
        backgroundContainer.add(stockpileBackgroundTable);

        Table stockpileContentsTable = new Table();
        stockpileBackgroundTable.left();
        stockpileContentsTable.add(stockpileImage);
        stockpileContentsTable.add(stockpileLabel).growX();

        Stack stockpileStack = new Stack();
        stockpileStack.add(backgroundContainer);
        stockpileStack.add(stockpileContentsTable);

        Table stockpileTable = new Table();
        stockpileTable.add(stockpileStack);
        return stockpileTable;
    }

    private List<TreeNodeValue> getSettlementItemTypeValues() {
        List<TreeNodeValue> individualEntities = settlementItemTracker.getAll(false)
                .stream()
                .map(entity -> {
                    if (entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemEntityAttributes) {
                        return itemEntityAttributes;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(itemEntityAttributes -> itemEntityAttributes.getItemType().getStockpileGroup() != null)
                .map(attributes -> {
                    final int totalQuantity = attributes.getQuantity();
                    final ItemType itemType = attributes.getItemType();
                    final StockpileGroup stockpileGroup = itemType.getStockpileGroup();
                    return new TreeNodeValue(stockpileGroup, itemType, totalQuantity);
                })
                .toList();


        ArrayList<TreeNodeValue> leaves = individualEntities.stream()
                .collect(ArrayList::new,
                        (values, v) -> foldNodeValue(values, v, existing -> existing.stockpileGroup == v.stockpileGroup && existing.itemType == v.itemType),
                        ArrayList::addAll);
        //java type erasure is ugly
        return leaves.stream().sorted(treeOrder).toList();
    }

    private void foldNodeValue(ArrayList<TreeNodeValue> values, TreeNodeValue v, Predicate<TreeNodeValue> predicate) {
        values.stream()
                .filter(predicate)
                .findAny().ifPresentOrElse(treeNodeValue -> {
                    int idx = values.indexOf(treeNodeValue);
                    TreeNodeValue newNodeValue = new TreeNodeValue(v.stockpileGroup, v.itemType, treeNodeValue.count + v.count);
                    values.set(idx, newNodeValue);
                }, () -> values.add(v));
    }

    static class TreeNode extends Tree.Node<TreeNode, List<TreeNodeValue>, Table> { }

    record TreeNodeValue(StockpileGroup stockpileGroup, ItemType itemType, int count) { }
}
