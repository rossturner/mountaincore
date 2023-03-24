package technology.rocketjump.mountaincore.modding;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.misc.versioning.Version;
import technology.rocketjump.mountaincore.modding.model.ModArtifact;
import technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition;
import technology.rocketjump.mountaincore.modding.model.ModArtifactListing;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ModCompatibilityChecker {

	private final ArtifactCompatibilityChecker artifactCompatibilityChecker;
	private final ModArtifactListing modArtifactListing;

	@Inject
	public ModCompatibilityChecker(ArtifactCompatibilityChecker artifactCompatibilityChecker, ModArtifactListing modArtifactListing) {
		this.artifactCompatibilityChecker = artifactCompatibilityChecker;
		this.modArtifactListing = modArtifactListing;
	}

	public CompatibilityResult checkCompatibility(ParsedMod mod) {
		final Compatibility compatibility;
		List<ModArtifact> incompatibleArtifacts = new ArrayList<>();
		Version modGameVersion = mod.getInfo().getGameVersion();

		if (modGameVersion == null) {
			compatibility = Compatibility.UNKNOWN;
		} else {
			for (ModArtifactDefinition artifactDefinition : modArtifactListing.getAll()) {
				ModArtifact modArtifact = mod.get(artifactDefinition);
				if (modArtifact != null) {
					if (!artifactCompatibilityChecker.checkCompatibility(modArtifact, modGameVersion)) {
						incompatibleArtifacts.add(modArtifact);
					}
				}
			}
			compatibility = incompatibleArtifacts.isEmpty() ? Compatibility.COMPATIBLE : Compatibility.INCOMPATIBLE;
		}

		return new CompatibilityResult(compatibility, incompatibleArtifacts);
	}

	public enum Compatibility {

		COMPATIBLE,
		INCOMPATIBLE,
		UNKNOWN;

		public String getI18nKey() {
			return "MODS.COMPATIBILITY."+this.name();
		}

	}
}