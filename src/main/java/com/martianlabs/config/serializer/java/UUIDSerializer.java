package com.martianlabs.config.serializer.java;

import com.martianlabs.config.BetterConfiguration;
import com.martianlabs.config.serializer.Serializer;

import java.util.UUID;

public class UUIDSerializer extends Serializer<UUID> {
    @Override
    protected void serializeInternal(String path, UUID value, BetterConfiguration configuration) {
        configuration.set(path, value.toString());
    }

    @Override
    public UUID deserialize(String path, BetterConfiguration configuration) {
        return UUID.fromString(configuration.getString(path));
    }
}
