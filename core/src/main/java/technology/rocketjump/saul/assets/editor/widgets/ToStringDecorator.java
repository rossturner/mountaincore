package technology.rocketjump.saul.assets.editor.widgets;

import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.util.Objects;
import java.util.function.Function;

public class ToStringDecorator<T> {
    private final T object;
    private final Function<T, String> toString;

    public static <T> ToStringDecorator<T> none() {
        return new ToStringDecorator<>(null, x -> "-none-");
    }

    public static ToStringDecorator<GameMaterial> material(GameMaterial input) {
        if (input == null) {
            return none();
        }
        Function<GameMaterial, String> materialToString = material -> String.format("%s - %s", material.getMaterialType().name(), material.getMaterialName());
        return new ToStringDecorator<>(input, materialToString);
    }

    public static ToStringDecorator<GameMaterialType> materialType(GameMaterialType input) {
        if (input == null) {
            return none();
        }
        return new ToStringDecorator<>(input, Enum::name);
    }

    public ToStringDecorator(T object, Function<T, String> toString) {
        this.object = object;
        this.toString = toString;
    }

    public T getObject() {
        return object;
    }

    @Override
    public String toString() {
        return toString.apply(object);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof ToStringDecorator<?> tsd) {
            return Objects.equals(object, tsd.object);
        }
        return false;
    }
}
