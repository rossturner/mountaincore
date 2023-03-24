package technology.rocketjump.mountaincore.assets.editor.model;

import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Gender;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class CreatureNameBuilders {
    public static String buildUniqueNameForAsset(Race race, CreatureEntityAsset asset) {
        Gender gender = asset.getGender();
        List<CreatureBodyShapeDescriptor> raceBodyShapes = race.getBodyShapes();
        CreatureBodyShape bodyShape = asset.getBodyShape();
        EntityAssetType assetType = asset.getType();
        List<Consciousness> consciousnesses = asset.getConsciousnessList();
        String profession = asset.getProfession();

        StringJoiner uniqueNameJoiner = new StringJoiner("-");
        uniqueNameJoiner.add(race.getName());
        if (gender != null && Gender.ANY != gender) {
            uniqueNameJoiner.add(gender.name());
        }
        if (bodyShape != null && CreatureBodyShape.ANY != bodyShape && raceBodyShapes.size() > 1) {
            uniqueNameJoiner.add(bodyShape.name());
        }
        if (profession != null && !SkillDictionary.NULL_PROFESSION.getName().equals(profession)) {
            uniqueNameJoiner.add(profession);
        }
        if (assetType != null) {
            uniqueNameJoiner.add(assetType.getName());
        }
        Set<Consciousness> allConsciousness = new HashSet<>(List.of(Consciousness.values()));
        allConsciousness.removeAll(consciousnesses);
        if (allConsciousness.size() == 1) {
            allConsciousness.stream().findFirst().ifPresent(unusedValue -> {
                uniqueNameJoiner.add("Not_"+unusedValue.name());
            });
        } else if(!consciousnesses.isEmpty() && !allConsciousness.isEmpty()) {
            String consciousTerm = consciousnesses.stream().map(Consciousness::name).collect(Collectors.joining("_"));
            uniqueNameJoiner.add(consciousTerm);
        }

        return WordUtils.capitalizeFully(uniqueNameJoiner.toString(), '_', '-');
    }
}
