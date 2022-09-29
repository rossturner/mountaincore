package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.production.StockpileSettings;
import technology.rocketjump.saul.rooms.HaulingAllocation;

public record StockpileSettingsUpdatedMessage(StockpileSettings stockpileSettings, long haulingAllocationTargetId, HaulingAllocation.AllocationPositionType haulingAllocationTargetPositionType) {
}
