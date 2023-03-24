package technology.rocketjump.mountaincore.modding.validation;

import technology.rocketjump.mountaincore.modding.exception.ModLoadingException;
import technology.rocketjump.mountaincore.modding.model.ModArtifact;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;

public interface ModArtifactValidator {

	default void apply(ModArtifact modArtifact, ParsedMod mod) throws ModLoadingException {
	}
}
