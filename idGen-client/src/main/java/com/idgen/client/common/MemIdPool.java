package com.idgen.client.common;

import com.idgen.client.service.IdGeneratorService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * Created by liguoqing on 2017/7/7.
 */
public class MemIdPool implements IdPool {

    private static Logger LOGGER = LoggerFactory.getLogger(MemIdPool.class);
    private String lock;
    private String table;
    private int num;
    private IdGeneratorService idGeneratorService;
    private static ConcurrentLinkedQueue<String> q1 = new ConcurrentLinkedQueue<>();

    public MemIdPool(String namespace, String table, int quantity, IdGeneratorService idGeneratorService) {
        this.lock = namespace;
        this.table = table;
        this.num = quantity;
        this.idGeneratorService = idGeneratorService;
    }

    @Override
    public String get() {
        if (q1.size() <= 0) {
            synchronized (q1) {
                LOGGER.info("Thread-{}", Thread.currentThread().getId());
                if (q1.size() <= 0) {
                    LOGGER.info("GetData from server Thread-{}", Thread.currentThread().getId());
                    String ids = idGeneratorService.getNextId(lock, table, num);
                    Stream.of(ids.split(",")).forEach(id -> q1.add(id));
                }
            }
        }
        LOGGER.info("LinkedQueue size {}", q1.size());
        return q1.poll();
    }

    @Override
    public void giveBack(String id) {
        if (StringUtils.isNotEmpty(id)) {
            synchronized (q1) {
                if (!q1.contains(id)) {
                    q1.add(id);
                }
            }
        }
    }
}
