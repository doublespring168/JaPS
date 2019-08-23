package com.kingq.client.cache;

import java.util.concurrent.ExecutorService;

/**
 * Created by spring on 15.06.2017.
 */
public interface AsyncPubSubCache extends PubSubCache {

    /**
     * Returns the underlying executor service of this async pub sub cache.
     *
     * @return The executor service.
     */
    ExecutorService executorService();
}
