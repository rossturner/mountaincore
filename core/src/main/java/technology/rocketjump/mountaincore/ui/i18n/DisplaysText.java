package technology.rocketjump.mountaincore.ui.i18n;

/**
 * This interface is to be used on any components which display text using Scene2D  - labels, inputs, select boxes, that kind of thing
 *
 * As the font may change when the language changes, it is necessary to *recreate* all UI widgets which display text, as they will cache the previous font otherwise
 */
public interface DisplaysText {

	void rebuildUI();

}
