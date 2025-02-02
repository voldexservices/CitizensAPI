package net.citizensnpcs.api.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.io.Files;

public class YamlStorage implements FileStorage {
    private final FileConfiguration config;
    private final File file;

    public YamlStorage(File file) {
        this(file, null);
    }

    public YamlStorage(File file, String header) {
        config = new YamlConfiguration();
        tryIncreaseMaxCodepoints(config);
        this.file = file;
        if (!file.exists()) {
            create();
            if (header != null) {
                config.options().header(header);
            }
            save();
        }
    }

    private void create() {
        try {
            Bukkit.getLogger().log(Level.INFO, "Creating file: " + file.getName());
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not create file: " + file.getName());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlStorage other = (YamlStorage) obj;
        if (file == null) {
            if (other.file != null) {
                return false;
            }
        } else if (!file.equals(other.file)) {
            return false;
        }
        return true;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public YamlKey getKey(String root) {
        return new YamlKey(root);
    }

    @Override
    public int hashCode() {
        return 31 + ((file == null) ? 0 : file.hashCode());
    }

    @Override
    public boolean load() {
        try {
            config.load(file);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean pathExists(String key) {
        return config.get(key) != null;
    }

    @Override
    public void save() {
        try {
            Files.createParentDirs(file);
            File temporaryFile = File.createTempFile(file.getName(), null, file.getParentFile());
            temporaryFile.deleteOnExit();
            config.save(temporaryFile);
            file.delete();
            temporaryFile.renameTo(file);
            temporaryFile.delete();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "YamlStorage {file=" + file + "}";
    }

    private void tryIncreaseMaxCodepoints(FileConfiguration config) {
        if (SET_CODEPOINT_LIMIT == null || LOADER_OPTIONS == null)
            return;
        try {
            SET_CODEPOINT_LIMIT.invoke(LOADER_OPTIONS.get(config), 67108864 /* ~64MB, Paper's limit */);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class YamlKey extends DataKey {
        public YamlKey(String root) {
            super(root);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || getClass() != obj.getClass()) {
                return false;
            }
            YamlKey other = (YamlKey) obj;
            return getOuterType().equals(other.getOuterType());
        }

        @Override
        public boolean getBoolean(String key) {
            String path = createRelativeKey(key);
            if (pathExists(path)) {
                if (config.getString(path) == null)
                    return config.getBoolean(path);
                return Boolean.parseBoolean(config.getString(path));
            }
            return false;
        }

        @Override
        public boolean getBoolean(String key, boolean def) {
            return config.getBoolean(createRelativeKey(key), def);
        }

        @Override
        public double getDouble(String key) {
            return getDouble(key, 0);
        }

        @Override
        public double getDouble(String key, double def) {
            String path = createRelativeKey(key);
            if (pathExists(path)) {
                Object value = config.get(path);
                if (value instanceof Number)
                    return ((Number) value).doubleValue();
                String raw = value.toString();
                if (raw.isEmpty())
                    return def;
                return Double.parseDouble(raw);
            }
            return def;
        }

        @Override
        public DataKey getFromRoot(String path) {
            return new YamlKey(path);
        }

        @Override
        public int getInt(String key) {
            return getInt(key, 0);
        }

        @Override
        public int getInt(String key, int def) {
            String path = createRelativeKey(key);
            if (pathExists(path)) {
                Object value = config.get(path);
                if (value instanceof Number)
                    return ((Number) value).intValue();
                String raw = value.toString();
                if (raw.isEmpty())
                    return def;
                return Integer.parseInt(raw);
            }
            return def;
        }

        @Override
        public long getLong(String key) {
            return getLong(key, 0L);
        }

        @Override
        public long getLong(String key, long def) {
            String path = createRelativeKey(key);
            if (pathExists(path)) {
                Object value = config.get(path);
                if (value instanceof Number)
                    return ((Number) value).longValue();
                String raw = value.toString();
                if (raw.isEmpty())
                    return def;
                return Long.parseLong(raw);
            }
            return def;
        }

        private YamlStorage getOuterType() {
            return YamlStorage.this;
        }

        @Override
        public Object getRaw(String key) {
            return config.get(createRelativeKey(key));
        }

        @Override
        public YamlKey getRelative(String relative) {
            if (relative == null || relative.isEmpty())
                return this;
            return new YamlKey(createRelativeKey(relative));
        }

        public YamlStorage getStorage() {
            return YamlStorage.this;
        }

        @Override
        public String getString(String key) {
            String path = createRelativeKey(key);
            if (pathExists(path)) {
                return config.get(path).toString();
            }
            return "";
        }

        @Override
        public Iterable<DataKey> getSubKeys() {
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section == null)
                return Collections.emptyList();
            List<DataKey> res = new ArrayList<DataKey>();
            for (String key : section.getKeys(false)) {
                res.add(getRelative(key));
            }
            return res;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<String, Object> getValuesDeep() {
            ConfigurationSection subSection = config.getConfigurationSection(path);
            return (Map<String, Object>) (subSection == null ? Collections.emptyMap() : subSection.getValues(true));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = prime * super.hashCode() + getOuterType().hashCode();
            return result;
        }

        @Override
        public boolean keyExists(String key) {
            Object value = config.get(createRelativeKey(key));
            if (value == null) {
                return false;
            }
            return true;
        }

        @Override
        public String name() {
            int last = path.lastIndexOf('.');
            return path.substring(last == 0 ? 0 : last + 1);
        }

        @Override
        public void removeKey(String key) {
            config.set(createRelativeKey(key), null);
        }

        @Override
        public void setBoolean(String key, boolean value) {
            config.set(createRelativeKey(key), value);
        }

        @Override
        public void setDouble(String key, double value) {
            config.set(createRelativeKey(key), String.valueOf(value));
        }

        @Override
        public void setInt(String key, int value) {
            config.set(createRelativeKey(key), value);
        }

        @Override
        public void setLong(String key, long value) {
            config.set(createRelativeKey(key), value);
        }

        @Override
        public void setRaw(String key, Object value) {
            config.set(createRelativeKey(key), value);
        }

        @Override
        public void setString(String key, String value) {
            config.set(createRelativeKey(key), value);
        }

        @Override
        public String toString() {
            return "YamlKey [path=" + path + "]";
        }
    }

    private static Field LOADER_OPTIONS;
    private static Method SET_CODEPOINT_LIMIT;
    static {
        try {
            LOADER_OPTIONS = YamlConfiguration.class.getDeclaredField("yamlLoaderOptions");
            LOADER_OPTIONS.setAccessible(true);
            SET_CODEPOINT_LIMIT = Class.forName("org.yaml.snakeyaml.LoaderOptions").getMethod("setCodepointLimit",
                    int.class);
            SET_CODEPOINT_LIMIT.setAccessible(true);
        } catch (Exception e) {
        }
    }
}