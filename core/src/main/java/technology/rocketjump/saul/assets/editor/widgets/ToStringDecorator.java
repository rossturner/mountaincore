package technology.rocketjump.saul.assets.editor.widgets;

import java.util.function.Function;

public class ToStringDecorator<T> {
    private final T object;
    private final Function<T, String> toString;

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
}
