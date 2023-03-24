package technology.rocketjump.mountaincore.assets.entities.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class AnimationDictionary {

    private final Map<String, AnimationScript> templatesByName;

    @Inject
    public AnimationDictionary() throws IOException {
        FileHandle file = Gdx.files.internal("assets/definitions/animation/animations.json");
        ObjectMapper objectMapper = new ObjectMapper();

        List<TemplateAnimationScript> templates = objectMapper.readValue(file.readString(),
                objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, TemplateAnimationScript.class));

        this.templatesByName = templates.stream().collect(Collectors.toMap(TemplateAnimationScript::getName, TemplateAnimationScript::getTemplate));
    }

    public AnimationScript newInstance(TemplateAnimationScript.Variables templateVariables) {
        AnimationScript templateScript = templatesByName.get(templateVariables.getUse());
        return deepCopy(templateScript, templateVariables);
    }

    private AnimationScript deepCopy(AnimationScript templateScript, TemplateAnimationScript.Variables templateVariables) {
        AnimationScript instance = new AnimationScript();
        instance.setDuration(templateScript.getDuration());
        instance.setParticleEffectCues(cloneList(templateScript.getParticleEffectCues(), s -> clone(s, templateVariables)));
        instance.setSoundCues(cloneList(templateScript.getSoundCues(), s -> clone(s, templateVariables)));
        instance.setRotations(cloneList(templateScript.getRotations(), s -> clone(s, templateVariables)));
        instance.setTranslations(cloneList(templateScript.getTranslations(), s -> clone(s, templateVariables)));
        instance.setScalings(cloneList(templateScript.getScalings(), s -> clone(s, templateVariables)));
        return instance;
    }

    private <T> List<T> cloneList(List<T> source, Function<T, T> f) {
        if (source == null) {
            return null;
        }
        return new ArrayList<>(source.stream().map(f).toList());
    }

    private AnimationScript.ScalingFrame clone(AnimationScript.ScalingFrame source, TemplateAnimationScript.Variables variables) {
        AnimationScript.ScalingFrame destination = new AnimationScript.ScalingFrame();
        destination.setAtTime(source.getAtTime());
        StorableVector2 storableVector2 = new StorableVector2();
        storableVector2.setX(source.getVector2().getX());
        storableVector2.setY(source.getVector2().getY());
        destination.setVector2(storableVector2);
        return destination;
    }

    private AnimationScript.TranslationFrame clone(AnimationScript.TranslationFrame source, TemplateAnimationScript.Variables variables) {
        AnimationScript.TranslationFrame destination = new AnimationScript.TranslationFrame();
        destination.setAtTime(source.getAtTime());
        StorableVector2 storableVector2 = new StorableVector2();
        storableVector2.setX(source.getVector2().getX());
        storableVector2.setY(source.getVector2().getY());
        destination.setVector2(storableVector2);
        return destination;
    }

    private AnimationScript.RotationFrame clone(AnimationScript.RotationFrame source, TemplateAnimationScript.Variables variables) {
        AnimationScript.RotationFrame destination = new AnimationScript.RotationFrame();
        destination.setAtTime(source.getAtTime());
        destination.setRoll(source.getRoll());
        return destination;
    }

    private AnimationScript.SoundCueFrame clone(AnimationScript.SoundCueFrame source, TemplateAnimationScript.Variables variables) {
        AnimationScript.SoundCueFrame destination = new AnimationScript.SoundCueFrame();
        destination.setAtTime(source.getAtTime());
        destination.setSoundAssetName(getValue(variables, source.getSoundAssetName()));
        return destination;
    }

    private AnimationScript.ParticleEffectCueFrame clone(AnimationScript.ParticleEffectCueFrame source, TemplateAnimationScript.Variables variables) {
        AnimationScript.ParticleEffectCueFrame destination = new AnimationScript.ParticleEffectCueFrame();
        destination.setAtTime(source.getAtTime());
        destination.setParticleEffectName(getValue(variables, source.getParticleEffectName()));
        return destination;
    }

    private String getValue(TemplateAnimationScript.Variables variables, String value) {
        return variables.getVariables().getOrDefault(value, value);
    }
}
