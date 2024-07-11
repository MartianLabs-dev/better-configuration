package com.martianlabs.config.serializer;

import com.martianlabs.config.BetterConfiguration;
import lombok.Getter;
import lombok.val;

import java.lang.reflect.ParameterizedType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.ClassUtils.isPrimitiveOrWrapper;

@Getter
public abstract class Serializer<T> {
    private final Class<T> serializerType;

    public Serializer() {
        val type = (ParameterizedType)this.getClass().getGenericSuperclass();

        checkArgument(type.getActualTypeArguments()[0] instanceof Class, "Serializer cannot have wildcard in generic");

        this.serializerType = (Class<T>)type.getActualTypeArguments()[0];
    }

    public void serialize(String path, Object value, BetterConfiguration configuration) {
        configuration.set(path, null);

        if (value == null) return;

        this.serializeInternal(path, (T) value, configuration);
    }

    protected abstract void serializeInternal(String path, T value, BetterConfiguration configuration);

    public abstract T deserialize(String path, BetterConfiguration configuration);

    protected static Class<?> getArrayGeneric(Object[] array) {
        for (val value : array)
            if (value != null) return value.getClass();
        throw new IllegalArgumentException("Cannot get generic of empty or null-filled map");
    }
}
