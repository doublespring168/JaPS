package com.kingq.client.pub;

import java.util.concurrent.ExecutorService;

/**
 * Created by spring on 07.05.2017.
 */
public interface AsyncPublisher extends Publisher {

    /**
     * Returns the underlying executor service of this async publisher.
     *
     * @return The executor service.
     */
    ExecutorService executorService();
}
