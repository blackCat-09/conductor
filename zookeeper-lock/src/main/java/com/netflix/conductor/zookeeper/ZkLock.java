package com.netflix.conductor.zookeeper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.conductor.core.utils.Lock;
import com.netflix.conductor.zookeeper.config.ZookeeperConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class ZkLock implements Lock {
    public static final int CACHE_MAXSIZE = 20000;
    public static final int CACHE_EXPIRY_TIME = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkLock.class);
    private CuratorFramework client;
    private LoadingCache<String, InterProcessMutex> zkLocks;
    private String zkPath;

    @Inject
    public ZkLock(ZookeeperConfiguration config, String namespace) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.newClient(config.getZkConnection(), retryPolicy);
        client.start();
        zkLocks = CacheBuilder.newBuilder()
                .maximumSize(CACHE_MAXSIZE)
                .expireAfterAccess(CACHE_EXPIRY_TIME, TimeUnit.MINUTES)
                .build(new CacheLoader<String, InterProcessMutex>() {
                           @Override
                           public InterProcessMutex load(String key) throws Exception {
                               return new InterProcessMutex(client, zkPath.concat(key));
                           }
                       }
                );

        zkPath = StringUtils.isEmpty(namespace)
                ? ("/conductor/")
                : ("/conductor/" + namespace + "/");
    }

    public void acquireLock(String lockId) {
        if (StringUtils.isEmpty(lockId)) {
            throw new IllegalArgumentException("lockId cannot be NULL or empty: lockId=" + lockId);
        }
        try {
            InterProcessMutex mutex = zkLocks.get(lockId);
            mutex.acquire();
        } catch (Exception e) {
            LOGGER.debug("Failed in acquireLock: ", e);
        }
    }

    public boolean acquireLock(String lockId, long timeToTry, TimeUnit unit) {
        if (StringUtils.isEmpty(lockId)) {
            throw new IllegalArgumentException("lockId cannot be NULL or empty: lockId=" + lockId);
        }
        try {
            InterProcessMutex mutex = zkLocks.get(lockId);
            return mutex.acquire(timeToTry, unit);
        } catch (Exception e) {
            LOGGER.debug("Failed in acquireLock: ", e);
        }
        return false;
    }

    public void releaseLock(String lockId) {
        if (StringUtils.isEmpty(lockId)) {
            throw new IllegalArgumentException("lockId cannot be NULL or empty: lockId=" + lockId);
        }
        try {
            zkLocks.get(lockId).release();
        } catch (Exception e) {
            LOGGER.debug("Failed in releaseLock: ", e);
        }
    }
}
