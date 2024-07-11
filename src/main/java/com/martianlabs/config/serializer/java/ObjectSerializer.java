package com.martianlabs.config.serializer.java;

import com.martianlabs.config.BetterConfiguration;
import com.martianlabs.config.ConfigRegistry;
import com.martianlabs.config.serializer.Serializer;
import lombok.SneakyThrows;
import lombok.val;

import java.io.Serializable;
import java.lang.reflect.Modifier;

public class ObjectSerializer extends Serializer<Serializable> {
    @Override
    @SneakyThrows
    protected void serializeInternal(String path, Serializable object, BetterConfiguration configuration) {
        val type = object.getClass();

        type.getConstructor();

        configuration.set(path + ".type", type.getName());

        for (val field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))
                continue;

            field.setAccessible(true);

            val value = field.get(object);

            val serializer = ConfigRegistry.getSerializer(field.getType());

            serializer.serialize(path + "." + configuration.getNamingStyle().format(field.getName()), value, configuration);
        }
    }

    @Override
    @SneakyThrows
    public Serializable deserialize(String path, BetterConfiguration configuration) {
        val section = configuration.getConfigurationSection(path);

        val type = Class.forName(section.getString("type"));

        type.getConstructor();

        val instance = (Serializable) type.newInstance();

        for (val field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))
                continue;

            field.setAccessible(true);

            val serializer = ConfigRegistry.getSerializer(field.getType());

            field.set(instance, serializer.deserialize(path + '.' + configuration.getNamingStyle().format(field.getName()), configuration));
        }

        return instance;
    }
}
