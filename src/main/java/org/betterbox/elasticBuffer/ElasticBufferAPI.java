package org.betterbox.elasticBuffer;

/**
 * Minimal stub of the ElasticBuffer API used for logging. The real
 * implementation is supplied by the ElasticBuffer plugin at runtime.
 */
public class ElasticBufferAPI {

    public ElasticBufferAPI(ElasticBuffer elasticBuffer) {
    }

    public void log(String message, String level, String source, String transactionId) {
        // no-op stub
    }
}

