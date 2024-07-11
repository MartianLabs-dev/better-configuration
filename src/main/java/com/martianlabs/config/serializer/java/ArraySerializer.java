package com.martianlabs.config.serializer.java;

import com.martianlabs.config.BetterConfiguration;
import com.martianlabs.config.ConfigRegistry;
import com.martianlabs.config.serializer.Serializer;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;

import java.lang.reflect.Array;

import static com.google.common.base.Preconditions.checkArgument;

public class ArraySerializer extends Serializer<Object[]> {
    @Override
    protected void serializeInternal(String path, Object[] value, BetterConfiguration configuration) {
        checkArgument(value.length > 0, "Cannot serialize an empty array");

        val generic = getArrayGeneric(value);

        val serializer = ConfigRegistry.getSerializer(generic);

        configuration.set(path + ".type", generic.getName());

        var index = 0;

        for (val element : value)
            serializer.serialize(path + '.' + index++, element, configuration);
    }

    @Override
    @SneakyThrows
    public Object[] deserialize(String path, BetterConfiguration configuration) {
        val section = configuration.getConfigurationSection(path);

        val type = Class.forName(section.getString("type"));

        val serializer = ConfigRegistry.getSerializer(type);

        val keys = section.getKeys(false);

        keys.remove("type");

        val array = (Object[]) Array.newInstance(type, keys.size());

        keys.forEach(x -> array[Integer.parseInt(x)] = serializer.deserialize(path + '.' + x, configuration));

        return array;
    }
}
