package technology.rocketjump.mountaincore.messaging.types;

public interface CheckCraftingQuoteCallback {

	void result(boolean isLimitReached, int limitQuantity);

}
