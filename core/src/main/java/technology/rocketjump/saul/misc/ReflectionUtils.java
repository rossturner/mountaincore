package technology.rocketjump.saul.misc;

import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils {
    public static Object getProperty(Object instance, String propertyName) {
        try {
            return PropertyUtils.getProperty(instance, propertyName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new PropertyReflectionException("Error reading field " + propertyName + " on object type " + instance.getClass(), ex);
        }
    }

    public static void setProperty(Object instance, String propertyName, Object value) {
        try {
            PropertyUtils.setProperty(instance, propertyName, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new PropertyReflectionException("Error writing field " + propertyName + " on object type " + instance.getClass() + " with value " + value, ex);
        }
    }

    public static class PropertyReflectionException extends RuntimeException {
        public PropertyReflectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
