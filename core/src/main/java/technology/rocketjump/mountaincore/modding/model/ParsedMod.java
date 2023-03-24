package technology.rocketjump.mountaincore.modding.model;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class ParsedMod {

	private final Map<ModArtifactDefinition, ModArtifact> artifacts = new TreeMap<>();
	private final Path basePath;
	private final ModInfo info;
	private final Optional<ModioMetadata> modioMeta;

	public ParsedMod(Path basePath, ModInfo info, Optional<ModioMetadata> modioMeta) {
		this.basePath = basePath;
		this.info = info;
		this.modioMeta = modioMeta;
	}

	public void add(ModArtifact artifact) {
		artifacts.put(artifact.artifactDefinition, artifact);
	}

	public ModArtifact get(ModArtifactDefinition artifactDefinition) {
		return artifacts.get(artifactDefinition);
	}

	public Path getBasePath() {
		return basePath;
	}

	public ModInfo getInfo() {
		return info;
	}

	public Optional<ModioMetadata> getModioMeta() {
		return modioMeta;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ParsedMod parsedMod = (ParsedMod) o;
		return info.equals(parsedMod.info);
	}

	@Override
	public int hashCode() {
		return info.hashCode();
	}

	@Override
	public String toString() {
		if (info != null) {
			return info.toString();
		} else {
			return basePath.toString();
		}
	}
}
