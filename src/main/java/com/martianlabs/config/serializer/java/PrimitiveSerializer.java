package com.martianlabs.config.serializer.java;

import com.martianlabs.config.BetterConfiguration;
import com.martianlabs.config.serializer.Serializer;
import lombok.val;

public class PrimitiveSerializer extends Serializer<Object> {
    @Override
    protected void serializeInternal(String path, Object value, BetterConfiguration configuration) {
        configuration.set(path, value);
    }

    @Override
    public Object deserialize(String path, BetterConfiguration configuration) {
        return configuration.get(path);
    }
}
