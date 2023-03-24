package technology.rocketjump.mountaincore.modding;

import technology.rocketjump.mountaincore.modding.model.ModArtifact;

import java.util.List;

public record CompatibilityResult(ModCompatibilityChecker.Compatibility compatibility,
                                  List<ModArtifact> incompatibleArtifacts) {
}
