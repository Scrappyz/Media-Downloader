package com.scrappyz.ytdlp.helper;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ExpiringResource implements Delayed {
    
    private String id;
    private long expiryMillis;

    private long fileSize;

    public ExpiringResource(String id, long expiryMillis, long fileSize) {
        this.id = id;
        this.expiryMillis = System.currentTimeMillis() + expiryMillis;
        this.fileSize = fileSize;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = expiryMillis - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.expiryMillis, ((ExpiringResource) other).expiryMillis);
    }

    @Override
    public String toString() {
        return id + ": " + (expiryMillis - System.currentTimeMillis()) + "ms remaining";
    }
}
