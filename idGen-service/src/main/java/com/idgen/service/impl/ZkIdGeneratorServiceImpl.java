package com.idgen.service.impl;

import com.idgen.adapter.zk.ZkDistributeLock;
import com.idgen.client.service.IdGeneratorService;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


/**
 * Created by liguoqing on 2017/7/5.
 */
@Service("zkIdGeneratorService")
public class ZkIdGeneratorServiceImpl implements IdGeneratorService, Watcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkIdGeneratorServiceImpl.class);

    private ZooKeeper zooKeeper;

    @Value("${zk.url}")
    private String ZkURL;

    @Value("${root.path}")
    private String root;

    @Value("${seq.path}")
    private String seq;

    //超时时间
    private int sessionTimeout = 3000;

    private CountDownLatch latch = new CountDownLatch(1);

    public void process(WatchedEvent watchedEvent) {
        if (Event.KeeperState.SyncConnected == watchedEvent.getState()) {
            latch.countDown();
        }
    }

    private void getZk() {
        try {
            zooKeeper = new ZooKeeper(ZkURL, sessionTimeout, this);
            latch.await();
            Stat stat = zooKeeper.exists(seq, false);
            if (null == stat) {
                zooKeeper.create(seq, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getNextId(String lock, String table, int assignNum) {
        ZkDistributeLock zkDistributeLock = null;
        String result = null;
        try {
            zkDistributeLock = new ZkDistributeLock(ZkURL, root, lock);
            zkDistributeLock.lock();
            getZk();
            return generatorId(lock, table, assignNum);
        } catch (Exception e) {
            LOGGER.error("ZkIdGeneratorServiceImpl.getNextId error {}", e);
        } finally {
            if (null != zkDistributeLock) {
                zkDistributeLock.unlock();
            }
        }
        return result;
    }

    private String generatorId(String lock, String table, int num) throws KeeperException, InterruptedException {
        StringBuilder result = new StringBuilder();
        if (num >= 1 && num <= 50) {
            Stat stat = zooKeeper.exists(seq + "/" + lock, false);
            if (null == stat) {
                zooKeeper.create(seq + "/" + lock, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            Stat _stat = zooKeeper.exists(seq + "/" + lock + "/" + table, this);
            String res = "1";
            if (null == _stat) {
                //创建并设置初始数据
                zooKeeper.create(seq + "/" + lock + "/" + table, (new String(res)).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                //取出上一次的值
                res = new String(zooKeeper.getData(seq + "/" + lock + "/" + table, this, _stat));
            }
            for (int i = 0; i < num; i++) {
                res = getId(res);
                result.append(res).append(",");
            }
            String data = result.substring(0, result.length() - 1);
            zooKeeper.setData(seq + "/" + lock + "/" + table, res.getBytes(), -1);
            LOGGER.info("ZkIdGeneratorServiceImpl.generatorId seq {} data {}", res, data);
            return data;
        }
        return result.toString();
    }

    private String getId(String start) {
        Long id = Long.valueOf(start).longValue() + 1L;
//        if (start.endsWith("909")) {
//            id += 9090L;
//        }
//        if (start.endsWith("9")) {
//            id += 90L;
//        }
        return String.valueOf(id);
    }
}
