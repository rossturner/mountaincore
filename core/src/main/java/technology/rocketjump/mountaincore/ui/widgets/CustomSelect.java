package technology.rocketjump.mountaincore.ui.widgets;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ArraySelection;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.*;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

public class CustomSelect<T> extends Widget implements Disableable {
    static final Vector2 temp = new Vector2();

    com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle style;
    final Array<T> items = new Array();
    final ArraySelection<T> selection = new ArraySelection(items);
    CustomSelectBoxList<T> customSelectBoxList;
    private float prefWidth, prefHeight;
    private ClickListener clickListener;
    boolean disabled;
    private int alignment = Align.left;
    private final DrawItemProcedure<T> drawItemProcedure;

    interface DrawItemProcedure<T> {
        GlyphLayout drawItem(Batch batch, BitmapFont font, T item, float x, float y, float width);
    }

    public CustomSelect(com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle style, List<T> customList, DrawItemProcedure<T> drawItemProcedure) {
        this.drawItemProcedure = drawItemProcedure;
        setStyle(style);
        setSize(getPrefWidth(), getPrefHeight());

        selection.setActor(this);
        selection.setRequired(true);

        customSelectBoxList = new CustomSelectBoxList<>(this, customList);
        setItems(customList.getItems());


        addListener(clickListener = new ClickListener() {
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                if (pointer == 0 && button != 0) return false;
                if (disabled) return false;
                if (customSelectBoxList.hasParent())
                    hideList();
                else
                    showList();
                return true;
            }
        });
    }

    /** Set the max number of items to display when the select box is opened. Set to 0 (the default) to display as many as fit in
     * the stage height. */
    public void setMaxListCount (int maxListCount) {
        customSelectBoxList.maxListCount = maxListCount;
    }

    /** @return Max number of items to display when the box is opened, or <= 0 to display them all. */
    public int getMaxListCount () {
        return customSelectBoxList.maxListCount;
    }

    protected void setStage (Stage stage) {
        if (stage == null) customSelectBoxList.hide();
        super.setStage(stage);
    }

    public void setStyle (com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle style) {
        if (style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        if (customSelectBoxList != null) {
            customSelectBoxList.setStyle(style.scrollStyle);
            customSelectBoxList.list.setStyle(style.listStyle);
        }
        invalidateHierarchy();
    }

    /** Returns the select box's style. Modifying the returned style may not have an effect until {@link #setStyle(com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle)}
     * is called. */
    public com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle getStyle () {
        return style;
    }

    /** Set the backing Array that makes up the choices available in the SelectBox */
    public void setItems (T... newItems) {
        if (newItems == null) throw new IllegalArgumentException("newItems cannot be null.");
        float oldPrefWidth = getPrefWidth();

        items.clear();
        items.addAll(newItems);
        selection.validate();
        customSelectBoxList.list.setItems(items);

        invalidate();
        if (oldPrefWidth != getPrefWidth()) invalidateHierarchy();
    }

    /** Sets the items visible in the select box. */
    public void setItems (Array<T> newItems) {
        if (newItems == null) throw new IllegalArgumentException("newItems cannot be null.");
        float oldPrefWidth = getPrefWidth();

        if (newItems != items) {
            items.clear();
            items.addAll(newItems);
        }
        selection.validate();
        customSelectBoxList.list.setItems(items);

        invalidate();
        if (oldPrefWidth != getPrefWidth()) invalidateHierarchy();
    }

    public void clearItems () {
        if (items.size == 0) return;
        items.clear();
        selection.clear();
        invalidateHierarchy();
    }

    /** Returns the internal items array. If modified, {@link #setItems(Array)} must be called to reflect the changes. */
    public Array<T> getItems () {
        return items;
    }

    @Override
    public void layout () {
        Drawable bg = style.background;
        BitmapFont font = style.font;

        if (bg != null) {
            prefHeight = Math.max(bg.getTopHeight() + bg.getBottomHeight() + font.getCapHeight() - font.getDescent() * 2,
                    bg.getMinHeight());
        } else
            prefHeight = font.getCapHeight() - font.getDescent() * 2;

        float maxItemWidth = 0;
        Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class);
        GlyphLayout layout = layoutPool.obtain();
        for (int i = 0; i < items.size; i++) {
            layout.setText(font, toString(items.get(i)));
            maxItemWidth = Math.max(layout.width, maxItemWidth);
        }
        layoutPool.free(layout);

        prefWidth = maxItemWidth;
        if (bg != null) prefWidth += bg.getLeftWidth() + bg.getRightWidth();

        List.ListStyle listStyle = style.listStyle;
        ScrollPane.ScrollPaneStyle scrollStyle = style.scrollStyle;
        float listWidth = maxItemWidth + listStyle.selection.getLeftWidth() + listStyle.selection.getRightWidth();
        if (scrollStyle.background != null)
            listWidth += scrollStyle.background.getLeftWidth() + scrollStyle.background.getRightWidth();
        if (customSelectBoxList == null || !customSelectBoxList.isScrollingDisabledY())
            listWidth += Math.max(style.scrollStyle.vScroll != null ? style.scrollStyle.vScroll.getMinWidth() : 0,
                    style.scrollStyle.vScrollKnob != null ? style.scrollStyle.vScrollKnob.getMinWidth() : 0);
        prefWidth = Math.max(prefWidth, listWidth);
    }

    @Override
    public void draw (Batch batch, float parentAlpha) {
        validate();

        Drawable background;
        if (disabled && style.backgroundDisabled != null)
            background = style.backgroundDisabled;
        else if (customSelectBoxList.hasParent() && style.backgroundOpen != null)
            background = style.backgroundOpen;
        else if (clickListener.isOver() && style.backgroundOver != null)
            background = style.backgroundOver;
        else if (style.background != null)
            background = style.background;
        else
            background = null;
        BitmapFont font = style.font;
        Color fontColor = (disabled && style.disabledFontColor != null) ? style.disabledFontColor : style.fontColor;

        Color color = getColor();
        float x = getX(), y = getY();
        float width = getWidth(), height = getHeight();

        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        if (background != null) background.draw(batch, x, y, width, height);

        T selected = selection.first();
        if (selected != null) {
            if (background != null) {
                width -= background.getLeftWidth() + background.getRightWidth();
                height -= background.getBottomHeight() + background.getTopHeight();
                x += background.getLeftWidth();
                y += (int)(height / 2 + background.getBottomHeight() + font.getData().capHeight / 2);
            } else {
                y += (int)(height / 2 + font.getData().capHeight / 2);
            }
            font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha);
            drawItem(batch, font, selected, x, y, width);
        }
    }

    protected GlyphLayout drawItem (Batch batch, BitmapFont font, T item, float x, float y, float width) {
        return drawItemProcedure.drawItem(batch, font, item, x, y, width);
    }

    /** Sets the alignment of the selected item in the select box. See {@link #getList()} and {@link List#setAlignment(int)} to set
     * the alignment in the list shown when the select box is open.
     * @param alignment See {@link Align}. */
    public void setAlignment (int alignment) {
        this.alignment = alignment;
    }

    /** Get the set of selected items, useful when multiple items are selected
     * @return a Selection object containing the selected elements */
    public ArraySelection<T> getSelection () {
        return selection;
    }

    /** Returns the first selected item, or null. For multiple selections use {@link com.badlogic.gdx.scenes.scene2d.ui.SelectBox#getSelection()}. */
    public T getSelected () {
        return selection.first();
    }

    /** Sets the selection to only the passed item, if it is a possible choice, else selects the first item. */
    public void setSelected (T item) {
        if (items.contains(item, false))
            selection.set(item);
        else if (items.size > 0)
            selection.set(items.first());
        else
            selection.clear();
    }

    /** @return The index of the first selected item. The top item has an index of 0. Nothing selected has an index of -1. */
    public int getSelectedIndex () {
        ObjectSet<T> selected = selection.items();
        return selected.size == 0 ? -1 : items.indexOf(selected.first(), false);
    }

    /** Sets the selection to only the selected index. */
    public void setSelectedIndex (int index) {
        selection.set(items.get(index));
    }

    public void setDisabled (boolean disabled) {
        if (disabled && !this.disabled) hideList();
        this.disabled = disabled;
    }

    public boolean isDisabled () {
        return disabled;
    }

    public float getPrefWidth () {
        validate();
        return prefWidth;
    }

    public float getPrefHeight () {
        validate();
        return prefHeight;
    }

    protected String toString (T item) {
        return item.toString();
    }

    public void showList () {
        if (items.size == 0) return;
        if (getStage() != null) customSelectBoxList.show(getStage());
    }

    public void hideList () {
        customSelectBoxList.hide();
    }

    /** Returns the list shown when the select box is open. */
    public List<T> getList () {
        return customSelectBoxList.list;
    }

    /** Disables scrolling of the list shown when the select box is open. */
    public void setScrollingDisabled (boolean y) {
        customSelectBoxList.setScrollingDisabled(true, y);
        invalidateHierarchy();
    }

    /** Returns the scroll pane containing the list that is shown when the select box is open. */
    public ScrollPane getScrollPane () {
        return customSelectBoxList;
    }

    protected void onShow (Actor selectBoxList, boolean below) {
        selectBoxList.getColor().a = 0;
        selectBoxList.addAction(fadeIn(0.3f, Interpolation.fade));
    }

    protected void onHide (Actor selectBoxList) {
        selectBoxList.getColor().a = 1;
        selectBoxList.addAction(sequence(fadeOut(0.15f, Interpolation.fade), removeActor()));
    }

    /** @author Nathan Sweet */
    static class CustomSelectBoxList<T> extends ScrollPane {
        private final CustomSelect<T> selectBox;
        int maxListCount;
        private final Vector2 screenPosition = new Vector2();
        final List<T> list;
        private InputListener hideListener;
        private Actor previousScrollFocus;

        public CustomSelectBoxList(final CustomSelect<T> selectBox, List<T> list) {
            super(null, selectBox.style.scrollStyle);
            this.selectBox = selectBox;

            setOverscroll(false, false);
            setFadeScrollBars(false);
            setScrollingDisabled(true, false);

            this.list = list;
            list.setTouchable(Touchable.disabled);
            list.setTypeToSelect(true);
            setActor(list);

            list.addListener(new ClickListener() {
                public void clicked (InputEvent event, float x, float y) {
                    selectBox.selection.choose(list.getSelected());
                    hide();
                }

                public boolean mouseMoved (InputEvent event, float x, float y) {
                    int index = list.getItemIndexAt(y);
                    if (index != -1) list.setSelectedIndex(index);
                    return true;
                }
            });

            addListener(new InputListener() {
                public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (toActor == null || !isAscendantOf(toActor)) list.getSelection().set(selectBox.getSelected());
                }
            });

            hideListener = new InputListener() {
                public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                    Actor target = event.getTarget();
                    if (isAscendantOf(target)) return false;
                    list.getSelection().set(selectBox.getSelected());
                    hide();
                    return false;
                }

                public boolean keyDown (InputEvent event, int keycode) {
                    switch (keycode) {
                        case Input.Keys.ENTER:
                            selectBox.selection.choose(list.getSelected());
                            // Fall thru.
                        case Input.Keys.ESCAPE:
                            hide();
                            event.stop();
                            return true;
                    }
                    return false;
                }
            };
        }

        public void show (Stage stage) {
            if (list.isTouchable()) return;

            stage.addActor(this);
            stage.addCaptureListener(hideListener);
            stage.addListener(list.getKeyListener());

            selectBox.localToStageCoordinates(screenPosition.set(0, 0));

            // Show the list above or below the select box, limited to a number of items and the available height in the stage.
            float itemHeight = list.getItemHeight();
            float height = itemHeight * (maxListCount <= 0 ? selectBox.items.size : Math.min(maxListCount, selectBox.items.size));
            Drawable scrollPaneBackground = getStyle().background;
            if (scrollPaneBackground != null) height += scrollPaneBackground.getTopHeight() + scrollPaneBackground.getBottomHeight();
            Drawable listBackground = list.getStyle().background;
            if (listBackground != null) height += listBackground.getTopHeight() + listBackground.getBottomHeight();

            float heightBelow = screenPosition.y;
            float heightAbove = stage.getCamera().viewportHeight - screenPosition.y - selectBox.getHeight();
            boolean below = true;
            if (height > heightBelow) {
                if (heightAbove > heightBelow) {
                    below = false;
                    height = Math.min(height, heightAbove);
                } else
                    height = heightBelow;
            }

            if (below)
                setY(screenPosition.y - height);
            else
                setY(screenPosition.y + selectBox.getHeight());
            setX(screenPosition.x);
            setHeight(height);
            validate();
            float width = Math.max(getPrefWidth(), selectBox.getWidth());
            if (getPrefHeight() > height && !isScrollingDisabledY()) width += getScrollBarWidth();
            setWidth(width);

            validate();
            scrollTo(0, list.getHeight() - selectBox.getSelectedIndex() * itemHeight - itemHeight / 2, 0, 0, true, true);
            updateVisualScroll();

            previousScrollFocus = null;
            Actor actor = stage.getScrollFocus();
            if (actor != null && !actor.isDescendantOf(this)) previousScrollFocus = actor;
            stage.setScrollFocus(this);

            list.getSelection().set(selectBox.getSelected());
            list.setTouchable(Touchable.enabled);
            clearActions();
            selectBox.onShow(this, below);
        }

        public void hide () {
            if (!list.isTouchable() || !hasParent()) return;
            list.setTouchable(Touchable.disabled);

            Stage stage = getStage();
            if (stage != null) {
                stage.removeCaptureListener(hideListener);
                stage.removeListener(list.getKeyListener());
                if (previousScrollFocus != null && previousScrollFocus.getStage() == null) previousScrollFocus = null;
                Actor actor = stage.getScrollFocus();
                if (actor == null || isAscendantOf(actor)) stage.setScrollFocus(previousScrollFocus);
            }

            clearActions();
            selectBox.onHide(this);
        }

        public void draw (Batch batch, float parentAlpha) {
            selectBox.localToStageCoordinates(temp.set(0, 0));
            if (!temp.equals(screenPosition)) hide();
            super.draw(batch, parentAlpha);
        }

        public void act (float delta) {
            super.act(delta);
            toFront();
        }

        protected void setStage (Stage stage) {
            Stage oldStage = getStage();
            if (oldStage != null) {
                oldStage.removeCaptureListener(hideListener);
                oldStage.removeListener(list.getKeyListener());
            }
            super.setStage(stage);
        }
    }

}