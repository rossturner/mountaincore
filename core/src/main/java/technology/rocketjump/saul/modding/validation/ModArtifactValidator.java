package technology.rocketjump.saul.modding.validation;

import technology.rocketjump.saul.modding.exception.ModLoadingException;
import technology.rocketjump.saul.modding.model.ModArtifact;
import technology.rocketjump.saul.modding.model.ParsedMod;

public interface ModArtifactValidator {

	default void apply(ModArtifact modArtifact, ParsedMod mod) throws ModLoadingException {
	}
}
