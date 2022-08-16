package technology.rocketjump.saul.assets.editor.model;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.jobs.SkillDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static technology.rocketjump.saul.assets.editor.model.CreatureNameBuildersTest.CreatureEntityAssetBuilder.asset;
import static technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape.AVERAGE;
import static technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape.STRONG;
import static technology.rocketjump.saul.entities.model.physical.creature.Consciousness.*;
import static technology.rocketjump.saul.entities.model.physical.creature.Gender.*;

class CreatureNameBuildersTest {

    private static Stream<Arguments> uniqueNameForAsset() {
        return Stream.of(
            Arguments.of(race("Dwarf"), asset().gender(FEMALE).build(), "Dwarf-Female"),
            Arguments.of(race("Pig"), asset().gender(ANY).build(), "Pig"),
            Arguments.of(race("Dwarf"), asset().gender(MALE).bodyShape(AVERAGE).build(), "Dwarf-Male"),
            Arguments.of(race("Dwarf", AVERAGE), asset().gender(MALE).bodyShape(STRONG).build(), "Dwarf-Male"),
            Arguments.of(typicalDwarf(), asset().gender(MALE).bodyShape(STRONG).build(), "Dwarf-Male-Strong"),
            Arguments.of(typicalDwarf(), asset().gender(FEMALE).assetType("CREATURE_BODY").build(), "Dwarf-Female-Creature_Body"),
            Arguments.of(typicalDwarf(), asset().gender(FEMALE).assetType("HEAD").consciousness(AWAKE).build(), "Dwarf-Female-Head-Awake"),
            Arguments.of(typicalDwarf(), asset().gender(MALE).assetType("HEAD").consciousness(DEAD).build(), "Dwarf-Male-Head-Dead"),
            Arguments.of(typicalDwarf(), asset().gender(MALE).assetType("CREATURE_BODY").consciousness(DEAD, SLEEPING, KNOCKED_UNCONSCIOUS).build(), "Dwarf-Male-Creature_Body-Not_Awake"),
            Arguments.of(typicalDwarf(), asset().gender(FEMALE).profession("BLACKSMITH").build(), "Dwarf-Female-Blacksmith"),
            Arguments.of(typicalDwarf(), asset().gender(FEMALE).profession(SkillDictionary.NULL_PROFESSION.getName()).build(), "Dwarf-Female")
        );
    }

    private static Race typicalDwarf() {
        return race("Dwarf", AVERAGE, STRONG);
    }


    @ParameterizedTest
    @MethodSource("uniqueNameForAsset")
    void buildUniqueNameForAsset(Race race, CreatureEntityAsset asset, String expectedName) {
        String actualName = CreatureNameBuilders.buildUniqueNameForAsset(race, asset);
        assertThat(actualName).isEqualTo(expectedName);
    }

    private static Race race(String name, CreatureBodyShape... creatureBodyShapes) {
        Race race = new Race();
        race.setName(name);
        race.setBodyShapes(Stream.of(creatureBodyShapes)
        .map(bs -> {
            CreatureBodyShapeDescriptor descriptor = new CreatureBodyShapeDescriptor();
            descriptor.setValue(bs);
            return descriptor;
        }).toList());
        return race;
    }

    static class CreatureEntityAssetBuilder {
        private final CreatureEntityAsset asset = new CreatureEntityAsset();
        public static CreatureEntityAssetBuilder asset() {
            return new CreatureEntityAssetBuilder();
        }

        private CreatureEntityAssetBuilder() {
            asset.setConsciousnessList(new ArrayList<>());
        }

        public CreatureEntityAssetBuilder gender(Gender gender) {
            asset.setGender(gender);
            return this;
        }

        public CreatureEntityAssetBuilder bodyShape(CreatureBodyShape bodyShape) {
            asset.setBodyShape(bodyShape);
            return this;
        }

        public CreatureEntityAssetBuilder assetType(String entityAssetTypeName) {
            asset.setType(new EntityAssetType(entityAssetTypeName));
            return this;
        }

        public CreatureEntityAssetBuilder consciousness(Consciousness... consciousnesses) {
            asset.setConsciousnessList(List.of(consciousnesses));
            return this;
        }

        public CreatureEntityAssetBuilder profession(String professionName) {
            asset.setProfession(professionName);
            return this;
        }

        public CreatureEntityAsset build() {
            return asset;
        }

    }
}