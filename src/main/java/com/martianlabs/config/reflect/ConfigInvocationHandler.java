package com.martianlabs.config.reflect;

import com.martianlabs.config.BetterConfiguration;
import com.martianlabs.config.ConfigRegistry;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.ClassUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

public class ConfigInvocationHandler implements InvocationHandler {
    private final Class<?> configInterface;
    private final BetterConfiguration configuration;

    public ConfigInvocationHandler(Class<?> configInterface, BetterConfiguration configuration) {
        this.configInterface = configInterface;
        this.configuration = configuration;

        this.validateMethods();
        this.updateConfigFile();
    }

    @Override
    @SneakyThrows
    public Object invoke(Object proxy, Method method, Object[] args) {
        val name = method.getName();

        switch (name) {
            case "toString":
                return this.configInterface.toString();
            case "hashCode":
                return this.hashCode();
            case "equals":
                return proxy.equals(args[0]);
        }

        if (name.startsWith("get"))
            return this.executeGetter(method);

        if (name.startsWith("set"))
            this.executeSetter(method, args[0]);

        return null;
    }

    private Object executeGetter(Method method) {
        val path = getPath(method);
        val type = method.getReturnType();
        var value = this.configuration.get(path);

        // TODO: Optionality

        if (type == String.class && this.configuration.isTranslateColorCodes()) {
            val translated = ChatColor.translateAlternateColorCodes('&', (String) value);

            this.configuration.cache(path, translated);

            return translated;
        }

        if (ClassUtils.isPrimitiveOrWrapper(type)) {
            return value;
        }

        val serializer = ConfigRegistry.getSerializer(type);

        value = serializer.deserialize(path, this.configuration);

        return value;
    }

    private void executeSetter(Method method, Object value) {
        // TODO: Optionality

        this.configuration.set(this.getPath(method), value);

        this.configuration.save();
    }

    @SneakyThrows
    private void validateMethods() {
        for (val method : this.configInterface.getDeclaredMethods()) {
            val name = method.getName();

            if (name.startsWith("get")) {
                checkArgument(method.isDefault(), "Getter " + name + " of class " + this.configInterface.getName() + " don't have default value");

                if (Map.class.isAssignableFrom(method.getReturnType())) {
                    val returnType = (ParameterizedType) method.getGenericReturnType();

                    checkArgument(returnType.getActualTypeArguments()[0] == String.class, "The key type of the returned map by the getter " + name + " of class " + this.configInterface.getName() + " is not a String");
                }

                // TODO: Primitive arrays support
                checkArgument(!isPrimitiveArray(method.getReturnType()), "Primitive arrays are not supported");

                continue;
            }

            if (name.startsWith("set")) {
                checkArgument(!method.isDefault(), "Setter " + name + " in class " + this.configInterface.getName() + " have default value");

                checkArgument(method.getReturnType() == void.class, "Setter " + name + " of class " + this.configInterface.getName() + " returns not void");

                checkArgument(method.getParameterCount() == 1, "Setter " + name + " of class " + this.configInterface.getName() + " has not one parameter");

                // TODO: Optionality

                val getterName = name.replaceFirst("set", "get");

                Optional<Method> result;

                try {
                    val getter = this.configInterface.getDeclaredMethod(getterName);

                    if (getter.getReturnType() != method.getParameterTypes()[0])
                        result = Optional.empty();
                    else result = Optional.of(getter);
                } catch (NoSuchMethodException e) {
                    result = Optional.empty();
                }

                checkArgument(result.isPresent(), "Setter " + name + " of class " + this.configInterface.getName() + " has no getter method");

                continue;
            }

            throw new InvalidConfigurationException("Found non-getter\\non-setter method");
        }
    }

    private void updateConfigFile() {
        var modified = false;

        for (val method : this.configInterface.getMethods()) {
            val path = this.getPath(method);

            if (!method.isDefault() || this.configuration.contains(path)) continue;

            val defaultValue = getDefaultValue(method);

            // TODO: Optionality

            if (defaultValue instanceof Collection)
                checkArgument(!((Collection<?>) defaultValue).isEmpty(), "Default values cannot be empty");

            if (defaultValue instanceof Map)
                checkArgument(!((Map<?, ?>) defaultValue).isEmpty(), "Default values cannot be empty");

            modified = true;
            this.configuration.set(path, defaultValue);
        }

        if (modified)
            this.configuration.save();
    }

    private String getPath(Method method) {
        return this.configuration.getNamingStyle().format(method.getName());
    }

    private static boolean isPrimitiveArray(Class<?> type) {
        return type.isArray() &&
                (type == boolean[].class ||
                        type == int[].class ||
                        type == char[].class ||
                        type == byte[].class ||
                        type == short[].class ||
                        type == double[].class ||
                        type == long[].class ||
                        type == float[].class);
    }

    @SneakyThrows
    public static Object getDefaultValue(Method method) {
        val type = method.getDeclaringClass();

        return getLookup()
                .in(type)
                .unreflectSpecial(method, type)
                .bindTo(generateHelperProxy(type))
                .invoke();
    }

    @SneakyThrows
    private static MethodHandles.Lookup getLookup() {
        val field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        field.setAccessible(true);

        return (MethodHandles.Lookup) field.get(null);
    }

    private static Object generateHelperProxy(Class<?> type) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, (proxy, method, args) -> null);
    }
}
