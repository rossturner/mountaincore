package technology.rocketjump.saul.modding;

import technology.rocketjump.saul.modding.model.ModArtifact;

import java.util.List;

public record CompatibilityResult(ModCompatibilityChecker.Compatibility compatibility,
                                  List<ModArtifact> incompatibleArtifacts) {
}
