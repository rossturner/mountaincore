package technology.rocketjump.saul.entities.model.physical.creature.body;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Streams;
import technology.rocketjump.saul.entities.model.physical.creature.body.organs.OrganDamageLevel;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.*;

public class Body implements ChildPersistable {

	private static final BodyPartDamage NO_DAMAGE = new BodyPartDamage();
	private BodyStructure bodyStructure;
	private Map<BodyPart, BodyPartDamage> damageMap = new HashMap<>();

	public Body() {

	}

	public Body(BodyStructure bodyStructure) {
		this.bodyStructure = bodyStructure;
	}

	public BodyPart randomlySelectPartBasedOnSize(Random random) {

		float totalSize = 0f;
		for (BodyPart bodyPart : getAllWorkingBodyParts()) {
			totalSize += bodyPart.getPartDefinition().getSize();
		}

		float roll = random.nextFloat() * totalSize;
		BodyPart selected = null;
		for (BodyPart bodyPart : getAllWorkingBodyParts()) {
			selected = bodyPart;
			roll -= bodyPart.getPartDefinition().getSize();
			if (roll <= 0) {
				break;
			}
		}
		return selected;
	}

	public Iterable<BodyPart> getAllWorkingBodyParts() {
		return () -> Streams.stream(iterateRecursively(new BodyPart(bodyStructure.getRootPart(), null)))
				.filter(bodyPart -> !(BodyPartDamageLevel.Destroyed == getDamage(bodyPart).getDamageLevel()))
				.iterator();
	}

	public Iterable<BodyPart> iterateRecursively(BodyPart rootPart) {
		BodyPartIterator iterator = new BodyPartIterator(this, rootPart);
		return () -> iterator;
	}

	public List<I18nText> getDamageDescriptions(I18nTranslator i18nTranslator) {
		List<I18nText> result = new ArrayList<>();
		for (Map.Entry<BodyPart, BodyPartDamage> damageMapEntry : damageMap.entrySet()) {

			if (!damageMapEntry.getValue().getDamageLevel().equals(BodyPartDamageLevel.None)) {
				result.add(i18nTranslator.getDamageDescription(damageMapEntry.getKey(), damageMapEntry.getValue().getDamageLevel()));
			}

			for (Map.Entry<BodyPartOrgan, OrganDamageLevel> organDamageEntry : damageMapEntry.getValue().getOrganDamage().entrySet()) {
				if (!organDamageEntry.getValue().equals(OrganDamageLevel.NONE)) {
					result.add(i18nTranslator.getDamageDescription(organDamageEntry.getKey(), organDamageEntry.getValue()));
				}
			}
		}
		return result;
	}

	public Set<Map.Entry<BodyPart, BodyPartDamage>> getAllDamage() {
		return damageMap.entrySet();
	}

	public BodyPartDamage getDamage(BodyPart bodyPart) {
		return damageMap.getOrDefault(bodyPart, NO_DAMAGE);
	}

	public void setDamage(BodyPart bodyPart, BodyPartDamageLevel damageLevel) {
		damageMap.computeIfAbsent(bodyPart, (a) -> new BodyPartDamage()).setDamageLevel(damageLevel);
	}

	public OrganDamageLevel getOrganDamage(BodyPart bodyPart, BodyPartOrgan organ) {
		return damageMap.getOrDefault(bodyPart, NO_DAMAGE).getOrganDamageLevel(organ);
	}

	public void setOrganDamage(BodyPart bodyPart, BodyPartOrgan organ, OrganDamageLevel organDamageLevel) {
		damageMap.computeIfAbsent(bodyPart, (a) -> new BodyPartDamage()).setOrganDamageLevel(organ, organDamageLevel);
	}

	public BodyStructure getBodyStructure() {
		return bodyStructure;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("bodyStructure", bodyStructure.getName());

		if (!damageMap.isEmpty()) {
			JSONArray damageMapJson = new JSONArray();
			for (Map.Entry<BodyPart, BodyPartDamage> entry : damageMap.entrySet()) {
				JSONObject entryJson = new JSONObject(true);
				entryJson.put("definitionName", entry.getKey().getPartDefinition().getName());
				if (entry.getKey().getDiscriminator() != null) {
					entryJson.put("discriminator", entry.getKey().getDiscriminator().name());
				}

				JSONObject damageJson = new JSONObject(true);
				entry.getValue().writeTo(damageJson, savedGameStateHolder);
				entryJson.put("damage", damageJson);
				damageMapJson.add(entryJson);
			}
			asJson.put("damageMap", damageMapJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.bodyStructure = relatedStores.bodyStructureDictionary.getByName(asJson.getString("bodyStructure"));
		if (this.bodyStructure == null) {
			throw new InvalidSaveException("Could not find body structure with name " + asJson.getString("bodyStructure"));
		}

		JSONArray damageMapJson = asJson.getJSONArray("damageMap");
		if (damageMapJson != null) {
			for (int cursor = 0; cursor < damageMapJson.size(); cursor++) {
				JSONObject entryJson = damageMapJson.getJSONObject(cursor);
				String definitionName = entryJson.getString("definitionName");
				BodyPartDefinition partDefinition = bodyStructure.getPartDefinitionByName(definitionName)
						.orElseThrow(() -> new InvalidSaveException("Could not find part definition by name " + definitionName));
				BodyPartDiscriminator discriminator = EnumParser.getEnumValue(entryJson, "discriminator", BodyPartDiscriminator.class, null);


				JSONObject damageJson = entryJson.getJSONObject("damage");
				BodyPartDamage damage = new BodyPartDamage();
				damage.readFrom(damageJson, savedGameStateHolder, relatedStores);

				damageMap.put(new BodyPart(partDefinition, discriminator), damage);
			}
		}
	}

	public void clearDamage(BodyPart bodyPart) {
		damageMap.remove(bodyPart);
	}

}
