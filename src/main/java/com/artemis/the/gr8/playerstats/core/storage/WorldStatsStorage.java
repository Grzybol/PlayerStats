package com.artemis.the.gr8.playerstats.core.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Klasa narzędziowa do zapisu i odczytu WorldStatsDatabase do pliku JSON.
 */
public class WorldStatsStorage {
    private static final Gson gson = new Gson();
    private static final Type DB_TYPE = new TypeToken<Map<UUID, PlayerWorldStats>>(){}.getType();

    public static void save(WorldStatsDatabase db, File file) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(db.getAll(), DB_TYPE, writer);
        }
    }

    public static void load(WorldStatsDatabase db, File file) throws IOException {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Map<UUID, PlayerWorldStats> map = gson.fromJson(reader, DB_TYPE);
            if (map != null) {
                db.getAll().clear();
                db.getAll().putAll(map);
            }
        }
    }
}
