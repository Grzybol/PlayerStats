package com.artemis.the.gr8.playerstats.core.storage;

import java.io.File;
import java.io.IOException;

/**
 * Klasa pomocnicza do zapisu i odczytu lokalnej bazy statystyk per world.
 * Sama nie synchronizuje już graczy – to robią listenery.
 */
public class WorldStatsSynchronizer {
    private final WorldStatsDatabase db;
    private final File file;

    public WorldStatsSynchronizer(WorldStatsDatabase db, File file) {
        this.db = db;
        this.file = file;
    }

    /** Zapis bazy do pliku JSON */
    public void save() throws IOException {
        WorldStatsStorage.save(db, file);
    }

    /** Wczytanie bazy z pliku JSON */
    public void load() throws IOException {
        WorldStatsStorage.load(db, file);
    }
}
