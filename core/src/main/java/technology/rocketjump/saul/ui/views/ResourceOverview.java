package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.screens.ManagementScreenName;
import technology.rocketjump.saul.screens.ResourceManagementScreen;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.i18n.I18nWordClass;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;
import technology.rocketjump.saul.ui.widgets.EnhancedScrollPane;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

import static technology.rocketjump.saul.ui.i18n.I18nWordClass.NOUN;
import static technology.rocketjump.saul.ui.i18n.I18nWordClass.PLURAL;

@Singleton
public class ResourceOverview implements GuiView, GameContextAware {

    public static final int OVERALL_INDENT_SPACING = 40;
    private final MainGameSkin mainGameSkin;
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

    @Inject
    public ResourceOverview(GuiSkinRepository guiSkinRepository, SettlementItemTracker settlementItemTracker, I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher,
                            TooltipFactory tooltipFactory, ButtonFactory buttonFactory, ResourceManagementScreen resourceManagementScreen) {
        this.mainGameSkin = guiSkinRepository.getMainGameSkin();
        this.settlementItemTracker = settlementItemTracker;
        this.i18nTranslator = i18nTranslator;
        this.messageDispatcher = messageDispatcher;
        this.tooltipFactory = tooltipFactory;

        treeOrder = Comparator.comparing((TreeNodeValue v) -> v.stockpileGroup().getSortOrder())
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
        rebuildTree();
    }

    @Override
    public void clearContextRelatedState() {
        stockpileGroupLabels.clear();
        containerTable.clearChildren();
    }


    @Override
    public void populate(Table containerTable) {
        this.containerTable = containerTable;
    }

    @Override
    public void update() {
        if (rootNode != null) {
            updateTree(rootNode, getSettlementItemTypeValues());
        }
    }

    private void rebuildTree() {
        containerTable.clearChildren();

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

        updateTree(rootNode, getSettlementItemTypeValues());

        tree.setIndentSpacing(OVERALL_INDENT_SPACING);
        ScrollPane scrollPane = new EnhancedScrollPane(tree, mainGameSkin);
        scrollPane.setScrollBarPositions(true, false);
        scrollPane.setScrollbarsVisible(false);
        scrollPane.setForceScroll(false, true);
        scrollPane.layout(); //needed for first displaying scrollpane cuts off half of the root node, as it is fantastically stupid
        containerTable.add(scrollPane);
    }

    private void updateTree(TreeNode rootNode, List<TreeNodeValue> byItemType) { //todo: don't really need these args
        List<TreeNodeValue> byStockpile = byItemType.stream()
                .map(node -> new TreeNodeValue(node.stockpileGroup, null, node.count))
                .collect(ArrayList::new,
                        (values, v) -> foldNodeValue(values, v, existing -> existing.stockpileGroup == v.stockpileGroup),
                        ArrayList::addAll);

        if (byStockpile.isEmpty() && rootNode.hasChildren()) {
            rootNode.clearChildren();
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
                    stockpileGroupLabels.get(stockpileGroup).setText(stockpileValue.count);

                    updateItemTypeLabels(stockpileGroup, stockpileImage, existingTreeNode, byItemType);
                } else if (orderComparison < 0) { //if before entry, then insert child before
                    TreeNode stockpileNode = new TreeNode();
                    stockpileNode.setActor(stockpileTable(stockpileGroup, stockpileValue.count, stockpileImage));
                    stockpileNode.setValue(List.of(stockpileValue));

                    rootNode.insert(i, stockpileNode);
                    i--; //to compare again
                } else { //else delete tree node
                    rootNode.remove(existingTreeNode);
                    stockpileGroupLabels.remove(stockpileGroup);
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

    }

//todo: feels rubbish passing in the same image over and over to get the width, images probably hsould be singletons
    private void updateItemTypeLabels(StockpileGroup stockpileGroup, Image stockpileImage, TreeNode stockpileNode, List<TreeNodeValue> allItemTypeValues) {
        List<TreeNodeValue> itemTypeValuesForStockpile = allItemTypeValues.stream().filter(itemTypeValue -> itemTypeValue.stockpileGroup == stockpileGroup).toList();


        Table itemTypesTable = new Table();
        itemTypesTable.background(mainGameSkin.getDrawable("Inv_Overview_BG"));

        Table itemTypesIndentedTable = new Table();
        itemTypesIndentedTable.add(new Container<>()).width(stockpileImage.getWidth() - OVERALL_INDENT_SPACING);
        itemTypesIndentedTable.add(itemTypesTable);
        TreeNode itemTypeNode = new TreeNode();
        itemTypeNode.setActor(itemTypesIndentedTable);
        itemTypeNode.setValue(itemTypeValuesForStockpile);

        //lazily clear child and re-add
        stockpileNode.clearChildren();
        stockpileNode.add(itemTypeNode);

        for (TreeNodeValue itemTypeValue : itemTypeValuesForStockpile) {
            itemTypesTable.add(getItemTypeLabel(itemTypeValue)).minWidth(350).left().row();
        }
    }

    private Label getItemTypeLabel(TreeNodeValue itemTypeValue) {
        I18nWordClass i18nWordClass = itemTypeValue.count > 0 ? PLURAL : NOUN;
        I18nText itemTypeText = i18nTranslator.getTranslatedString(itemTypeValue.itemType.getI18nKey(), i18nWordClass);
        I18nText labelText = i18nTranslator.getTranslatedWordWithReplacements("GUI.RESOURCE_MANAGEMENT.QUANTIFIED_ITEM", Map.of(
                "quantity", new I18nWord(String.valueOf(itemTypeValue.count)),
                "item", itemTypeText
        ));
        Label itemTypeLabel = new Label(labelText.toString(), mainGameSkin);
        itemTypeLabel.setAlignment(Align.left);
        return itemTypeLabel;
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
