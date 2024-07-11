package com.martianlabs.config.serializer.java;

import com.martianlabs.config.BetterConfiguration;
import com.martianlabs.config.serializer.Serializer;
import lombok.SneakyThrows;
import lombok.val;

public class EnumSerializer extends Serializer<Enum> {
    @Override
    protected void serializeInternal(String path, Enum value, BetterConfiguration configuration) {
        configuration.set(path + ".type", value.getClass().getName());
        configuration.set(path + ".value", value.name());
    }

    @Override
    @SneakyThrows
    public Enum deserialize(String path, BetterConfiguration configuration) {
        val section = configuration.getConfigurationSection(path);

        val type = Class.forName(section.getString("type"));

        return (Enum) type.getMethod("valueOf", String.class).invoke(null, section.getString("value"));
    }
}
