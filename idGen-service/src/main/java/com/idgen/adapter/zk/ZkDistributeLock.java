package com.idgen.adapter.zk;

import com.idgen.client.exception.LockException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created by liguoqing on 2017/7/6.
 */
public class ZkDistributeLock implements Lock, Watcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkDistributeLock.class);

    //竞争资源标志
    private String lockName;

    //锁根目录
    private String roots;

    //等待节点
    private String waitNode;

    //临时目录
    private String tempZone;

    private ZooKeeper zooKeeper;

    //超时时间
    private int sessionTimeout = 300000;

    //计数器
    private CountDownLatch latch = new CountDownLatch(1);

    private static final String SPLIT = "_lock_";

    private List<Exception> exceptionList = Lists.newArrayList();

    public ZkDistributeLock(String ZkURL, String root, String lock) {
        this.lockName = lock;
        this.roots = root;
        try {
            //创建zk链接
            zooKeeper = new ZooKeeper(ZkURL, sessionTimeout, this);
            latch.await();
            //根节点是否存在
            Stat stat = zooKeeper.exists(root, false);
            if (stat == null) {
                //创建跟节点
                zooKeeper.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (IOException e) {
            exceptionList.add(e);
        } catch (KeeperException e) {
            exceptionList.add(e);
        } catch (InterruptedException e) {
            exceptionList.add(e);
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
            this.latch.countDown();
        }
    }

    @Override
    public void lock() {
        if (CollectionUtils.isNotEmpty(exceptionList)) {
            throw new LockException(exceptionList.get(0));
        }
        try {
            if (tryLock()) {
                LOGGER.info("ZkDistributeLock.lock.Thread {}", Thread.currentThread().getId());
                return;
            } else {
                //等待锁
                waitForLock(waitNode, sessionTimeout);
            }
        } catch (Exception e) {
            throw new LockException(e);
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.lock();
    }

    @Override
    public boolean tryLock() {
        if (this.lockName.contains(SPLIT)) {
            throw new LockException("lockName can not contains \\\\u000B");
        }
        try {
            tempZone = zooKeeper.create(this.roots + "/" + lockName + SPLIT, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            List<String> allNodes = zooKeeper.getChildren(roots, false);
            //定义所有被锁定节点
            List<String> lockNodes = Lists.newArrayList();
            for (String node : allNodes) {
                String _node = node.split(SPLIT)[0];
                LOGGER.info("ZkDistributeLock.tryLock node {}", node);
                if (_node.equals(lockName)) {
                    lockNodes.add(node);
                }
            }
            //按照节点自增序号从小到大排序
            Collections.sort(lockNodes);
            //判断节点是否在第一个
            if (tempZone.equals(roots.concat("/").concat(lockNodes.get(0)))) {
                //取得锁
                return true;
            }
            //截取路径后部分
            String zone = tempZone.substring(tempZone.lastIndexOf("/") + 1);
            //找到比自己小1的节点
            waitNode = lockNodes.get(Collections.binarySearch(lockNodes, zone) - 1);
            return false;
        } catch (KeeperException e) {
            throw new LockException(e);
        } catch (InterruptedException e) {
            throw new LockException(e);
        }
    }

    private boolean waitForLock(String lockNode, long waitTime) throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(roots + "/" + lockNode, true);
        //比自己小的节点是否存在，如果不存在无需等待
        if (stat != null) {
            LOGGER.info("ZkDistributeLock.waitForLock.Thread {} waiting for {}", Thread.currentThread().getId(), roots.concat("/").concat(lockNode));
            latch = new CountDownLatch(1);
            latch.await(waitTime, TimeUnit.MILLISECONDS);
            latch = null;

            List<String> allNodes = zooKeeper.getChildren(roots, false);
            //定义所有被锁定节点
            List<String> lockNodes = Lists.newArrayList();
            for (String node : allNodes) {
                String _node = node.split(SPLIT)[0];
                if (_node.equals(lockName)) {
                    lockNodes.add(node);
                }
            }
            //按照节点自增序号从小到大排序
            Collections.sort(lockNodes);
            //判断节点是否在第一个
            if (tempZone.equals(roots.concat("/").concat(lockNodes.get(0)))) {
                //取得锁
                return true;
            }
            //截取路径后部分
            String zone = tempZone.substring(tempZone.lastIndexOf("/") + 1);
            //找到比自己小1的节点
            waitNode = lockNodes.get(Collections.binarySearch(lockNodes, zone) - 1);
            waitForLock(waitNode, waitTime);
        }
        return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            if (tryLock()) {
                return true;
            }
            return waitForLock(waitNode, time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void unlock() {
        try {
            zooKeeper.delete(tempZone, -1);
            tempZone = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        } finally {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
