package com.martianlabs.config;

import com.martianlabs.config.style.NamingStyle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.ClassUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class BetterConfiguration extends YamlConfiguration {
    private final File file;
    @Getter private final NamingStyle namingStyle;
    @Getter private final boolean translateColorCodes;

    private final Map<String, Object> cache;

    @SneakyThrows
    public BetterConfiguration(File file, NamingStyle namingStyle, boolean translateColorCodes) {
        this.file = file;
        this.namingStyle = namingStyle;
        this.translateColorCodes = translateColorCodes;

        this.cache = new Object2ObjectOpenHashMap<>();

        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            this.file.createNewFile();
        }

        this.load();
    }

    @Override
    public void set(String path, Object value) {
        if (value instanceof Collection || value instanceof Map || (value != null && !(value instanceof String) && !ClassUtils.isPrimitiveOrWrapper(value.getClass()))) {

            val serializer = ConfigRegistry.getSerializer(value.getClass());

            this.cache.put(path, value);
            serializer.serialize(path, value, this);

            return;
        }

        if (value == null) {
            if (this.cache.containsKey(path)) {
                this.cache.put(path, null);
            }
        } else if (value instanceof String && this.translateColorCodes) {
            this.cache.put(path, ChatColor.translateAlternateColorCodes('&', (String) value));
        }

        super.set(path, value);
    }

    @Override
    public Object get(String path) {
        return this.cache.containsKey(path) ? this.cache.get(path) : super.get(path);
    }

    public void load() {
        this.load(this.file);
    }

    @Override
    @SneakyThrows
    public void load(File file) {
        this.cache.clear();
        super.load(file);
    }

    @SneakyThrows
    public void save() {
        this.save(this.file);
    }

    public void cache(String path, Object value) {
        this.cache.put(path, value);
    }
}
