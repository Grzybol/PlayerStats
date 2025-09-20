package com.artemis.the.gr8.playerstats.core.storage;

import com.artemis.the.gr8.playerstats.core.utils.Closable;

/**
 * Abstraction for loading and saving world statistics using different backends.
 */
public interface WorldStatsPersistence extends Closable {

    /**
     * Prepare the persistence backend (e.g. create files or database tables).
     */
    void initialize() throws Exception;

    /**
     * Populate the provided database instance with data from the backend.
     */
    void load(WorldStatsDatabase database) throws Exception;

    /**
     * Persist the contents of the provided database instance.
     */
    void save(WorldStatsDatabase database) throws Exception;

    @Override
    default void close() {
        // no-op by default
    }
}
