package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Singleton
public class ResourceOverview implements GuiView, GameContextAware {

    private final MainGameSkin mainGameSkin;
    private final SettlementItemTracker settlementItemTracker;
    private final I18nTranslator i18nTranslator;
    private final MessageDispatcher messageDispatcher;
    private Table containerTable;

    @Inject
    public ResourceOverview(GuiSkinRepository guiSkinRepository, SettlementItemTracker settlementItemTracker, I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher) {
        this.mainGameSkin = guiSkinRepository.getMainGameSkin();
        this.settlementItemTracker = settlementItemTracker;
        this.i18nTranslator = i18nTranslator;
        this.messageDispatcher = messageDispatcher;
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
        rebuildTree();
    }

    @Override
    public void clearContextRelatedState() {
        containerTable.clearChildren();
    }


    @Override
    public void populate(Table containerTable) {
        this.containerTable = containerTable;
        containerTable.debugAll();
    }

    @Override
    public void update() {
        //todo: use updatables pattern?

    }

    private void rebuildTree() {
        containerTable.clearChildren();

        //scrollpane
        Tree<TreeNode, TreeNodeValue> tree = new Tree<>(mainGameSkin, "resource_overview_tree");
//        tree.setIndentSpacing();


        //Stockpile groups sorted alphabetically
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


        // ItemType Level
        List<TreeNodeValue> byItemType = individualEntities.stream()
                .collect(ArrayList::new,
                        (values, v) -> foldNodeValue(values, v, existing -> existing.stockpileGroup == v.stockpileGroup && existing.itemType == v.itemType),
                        ArrayList::addAll);


        // StockpileGroup Level
        List<TreeNodeValue> byStockpile = individualEntities.stream()
                .map(node -> new TreeNodeValue(node.stockpileGroup, null, node.count))
                .collect(ArrayList::new,
                        (values, v) -> foldNodeValue(values, v, existing -> existing.stockpileGroup == v.stockpileGroup),
                        ArrayList::addAll);




        //todo: sort me
        //todo tooltip for icon
        for (TreeNodeValue stockpileValue : byStockpile.stream()
                .sorted(Comparator.comparing(s -> i18nTranslator.translate(s.stockpileGroup.getI18nKey())))
                .toList()) {
            Label stockpileLabel = new Label(String.valueOf(stockpileValue.count), mainGameSkin, "resource_overview_stockpile_label");
            stockpileLabel.setAlignment(Align.left);

            Table stockpileTable = new Table();
            stockpileTable.add(stockpileLabel); //TODO: stack this to overlap the icon
//            stockpileTable.addListener(tree.getClickListener());

            TreeNode stockpileNode = new TreeNode();
            stockpileNode.setActor(stockpileTable);
            stockpileNode.setValue(stockpileValue); //nfc if this has any meaning

            for (TreeNodeValue itemTypeValue : byItemType.stream()
                    .filter(itemTypeValue -> itemTypeValue.stockpileGroup == stockpileValue.stockpileGroup)
                    .sorted(Comparator.comparing(v -> i18nTranslator.translate(v.itemType.getI18nKey())))
                    .toList()) {

                Label itemTypeLabel = new Label(i18nTranslator.getItemDescription(itemTypeValue.count, null, itemTypeValue.itemType, null).toString(), mainGameSkin);
                Table itemTypeTable = new Table();
                itemTypeTable.background(mainGameSkin.getDrawable("Inv_Overview_BG"));
                itemTypeTable.add(itemTypeLabel);

                TreeNode itemTypeNode = new TreeNode();
                itemTypeNode.setActor(itemTypeTable);
                itemTypeNode.setValue(itemTypeValue);
                stockpileNode.add(itemTypeNode);

            }


            tree.add(stockpileNode);
        }

        containerTable.add(tree);
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

    static class TreeNode extends Tree.Node<TreeNode, TreeNodeValue, Table> {

    }

    record TreeNodeValue(StockpileGroup stockpileGroup, ItemType itemType, int count) {

    }
}
