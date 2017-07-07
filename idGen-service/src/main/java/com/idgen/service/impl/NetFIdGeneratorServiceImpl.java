package com.idgen.service.impl;

import com.idgen.client.exception.LockException;
import com.idgen.client.service.IdGeneratorService;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.recipes.locks.InterProcessMutex;
import com.netflix.curator.framework.state.ConnectionState;
import com.netflix.curator.framework.state.ConnectionStateListener;
import com.netflix.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created by liguoqing on 2017/7/7.
 */
@Service("netFIdGeneratorService")
public class NetFIdGeneratorServiceImpl implements IdGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetFIdGeneratorServiceImpl.class);

    private static CuratorFramework client;

    private static String ZkURL = "zk.dev.pajkdc.com:2181,zk2.dev.pajkdc.com:2181,zk3.dev.pajkdc.com:2181";

    private static String root = "/locks";

    private static String seq = "/seq";

    static {
        RetryNTimes retryNTimes = new RetryNTimes(10, 5000);
        client = CuratorFrameworkFactory.newClient(ZkURL, retryNTimes);
        client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                if (connectionState == ConnectionState.RECONNECTED) {
                    LOGGER.info("NetFIdGeneratorServiceImpl.connectionState {}", connectionState);
                } else if (connectionState == ConnectionState.LOST) {
                    LOGGER.info("NetFIdGeneratorServiceImpl.connectionState {}", connectionState);
                } else if (connectionState == ConnectionState.CONNECTED) {
                    LOGGER.info("NetFIdGeneratorServiceImpl.connectionState {}", connectionState);
                } else {
                    LOGGER.info("NetFIdGeneratorServiceImpl.connectionState error {}", connectionState);
                }
            }
        });
        client.start();
        try {
            Stat stat = client.checkExists().forPath(seq);
            if (stat == null) {
                client.create().withMode(CreateMode.PERSISTENT).forPath(seq, new byte[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNextId(String lock, String table, int assignNum) {
        InterProcessMutex mutex = new InterProcessMutex(client, root + "/" + lock + "/" + table);
        try {
            LOGGER.info("NetFIdGeneratorServiceImpl.getNextId lockObjects size {}", getLockNode(mutex));
            if (!mutex.acquire(30L, TimeUnit.SECONDS)) {
                throw new LockException("can't get lock");
            }
            return generatorId(lock, table, assignNum);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mutex.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    private int getLockNode(InterProcessMutex mutex) {
        try {
            return mutex.getParticipantNodes().size();
        } catch (Exception e) {
            if (null != e.getMessage() && !e.getMessage().contains("NoNode")) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private String generatorId(String lock, String table, int num) {
        StringBuilder result = new StringBuilder();
        try {
            Stat stat = client.checkExists().forPath(seq + "/" + lock);
            if (null == stat) {
                client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(seq + "/" + lock);
            }
            Stat _stat = client.checkExists().forPath(seq + "/" + lock + "/" + table);
            String res = "1";
            if (null == _stat) {
                //创建并设置初始数据
                client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(seq + "/" + lock + "/" + table, res.getBytes());
            } else {
                //取出上一次的值
                res = new String(client.getData().forPath(seq + "/" + lock + "/" + table));
            }
            for (int i = 0; i < num; i++) {
                res = getId(res);
                result.append(res).append(",");
            }
            String data = result.substring(0, result.length() - 1);
            client.setData().forPath(seq + "/" + lock + "/" + table, res.getBytes());
            LOGGER.info("NetFIdGeneratorServiceImpl.generatorId seq {} data {}", res, data);
        } catch (Exception e) {
            e.printStackTrace();
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
