package com.kingq.server.cache;

/**
 * Created by spring on 13.06.2017.
 */
public class CacheEntry {

    private long expireBy;

    private Object value;

    public CacheEntry(long expireBy, Object value) {

        this.expireBy = expireBy;
        this.value = value;
    }

    public long expireBy() {

        return expireBy;
    }

    public void expireBy(long expireBy) {

        this.expireBy = expireBy;
    }

    public Object value() {

        return value;
    }
}
