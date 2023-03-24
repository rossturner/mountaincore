package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.production.StockpileSettings;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

public record StockpileSettingsUpdatedMessage(StockpileSettings stockpileSettings, long haulingAllocationTargetId, HaulingAllocation.AllocationPositionType haulingAllocationTargetPositionType) {
}
