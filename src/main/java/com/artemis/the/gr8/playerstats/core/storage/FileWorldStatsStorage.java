package com.artemis.the.gr8.playerstats.core.storage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

/**
 * JSON file based implementation of {@link WorldStatsPersistence}.
 */
public class FileWorldStatsStorage implements WorldStatsPersistence {

    private static final Gson GSON = new Gson();
    private static final Type DB_TYPE = new TypeToken<Map<UUID, PlayerWorldStats>>() { }.getType();

    private final File file;

    public FileWorldStatsStorage(File file) {
        this.file = file;
    }

    @Override
    public void initialize() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory for " + file.getAbsolutePath());
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Could not create file " + file.getAbsolutePath());
            }
            try (Writer writer = new FileWriter(file)) {
                writer.write("{}");
            }
        }
    }

    @Override
    public void load(WorldStatsDatabase database) throws IOException {
        if (!file.exists() || file.length() == 0) {
            database.getAll().clear();
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Map<UUID, PlayerWorldStats> map = GSON.fromJson(reader, DB_TYPE);
            database.replaceAll(map);
        } catch (JsonSyntaxException ex) {
            throw new IOException("Invalid JSON in " + file.getAbsolutePath(), ex);
        }
    }

    @Override
    public void save(WorldStatsDatabase database) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(database.getAll(), DB_TYPE, writer);
        }
    }
}
