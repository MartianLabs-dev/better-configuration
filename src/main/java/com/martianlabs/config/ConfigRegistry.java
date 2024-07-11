package com.martianlabs.config;

import com.martianlabs.config.annotation.ConfigName;
import com.martianlabs.config.reflect.ConfigInvocationHandler;
import com.martianlabs.config.serializer.Serializer;
import com.martianlabs.config.serializer.java.*;
import com.martianlabs.config.style.NamingStyle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.val;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

public class ConfigRegistry {
    private final static Map<Class, Serializer> SERIALIZERS;
    private final static Serializer PRIMITIVE_SERIALIZER;
    private final static Serializer ARRAY_SERIALIZER;
    private final static Serializer OBJECT_SERIALIZER;

    private final Map<String, Object> proxies;

    static {
        SERIALIZERS = new Object2ObjectOpenHashMap<>();

        PRIMITIVE_SERIALIZER = new PrimitiveSerializer();
        ARRAY_SERIALIZER = new ArraySerializer();
        OBJECT_SERIALIZER = new ObjectSerializer();

        registerSerializer(Enum.class, new EnumSerializer());
        registerSerializer(UUID.class, new UUIDSerializer());
    }

    public ConfigRegistry() {
        this.proxies = new Object2ObjectOpenHashMap<>();
    }

    public <T> T register(Class<T> type, File directory) {
        return this.register(type, directory, NamingStyle.CAMEL_CASE, true);
    }

    public <T> T register(Class<T> type, File directory, NamingStyle namingStyle, boolean translateColorCodes) {
        checkArgument(type.isInterface(), "Only interfaces can be registered as configs");
        checkArgument(type.isAnnotationPresent(ConfigName.class), "Config's interface must be annotated with @ConfigName");

        val name = type.getAnnotation(ConfigName.class).value() + ".yml";

        val file = new File(directory, name);

        val configuration = new BetterConfiguration(file, namingStyle, translateColorCodes);

        val proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new ConfigInvocationHandler(type, configuration));

        this.proxies.put(name, configuration);

        return (T) proxy;
    }

    public <T> T getConfiguration(Class<T> type) {
        checkArgument(type.isInterface(), "Only interfaces can be registered as configs");
        checkArgument(type.isAnnotationPresent(ConfigName.class), "Config's interface must be annotated with @ConfigName");

        val name = type.getAnnotation(ConfigName.class).value() + ".yml";

        checkArgument(this.proxies.containsKey(name), "Config is not registered");

        return (T) this.proxies.get(name);
    }

    public static <T> void registerSerializer(Class<T> type, Serializer<T> serializer) {
        SERIALIZERS.put(type, serializer);
    }

    public static <T> Serializer<T> getSerializer(Class<T> type) {
        if (SERIALIZERS.containsKey(type))
            return SERIALIZERS.get(type);

        for (val entry : SERIALIZERS.entrySet())
            if (entry.getKey().isAssignableFrom(type))
                return entry.getValue();

        if (type == String.class || ClassUtils.isPrimitiveOrWrapper(type))
            return PRIMITIVE_SERIALIZER;

        if (type.isArray())
            return ARRAY_SERIALIZER;

        if (Serializable.class.isAssignableFrom(type))
            return OBJECT_SERIALIZER;

        throw new NotImplementedException("Serializer for type \"" + type.getName() + "\" is not registered!");
    }
}
