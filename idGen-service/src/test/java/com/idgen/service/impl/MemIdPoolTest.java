package com.idgen.service.impl;

import com.idgen.client.common.IdPool;
import com.idgen.client.common.MemIdPool;
import com.idgen.client.service.IdGeneratorService;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;

/**
 * Created by liguoqing on 2017/7/7.
 */
@ActiveProfiles("dev")
@EnableAutoConfiguration
public class MemIdPoolTest extends BaseTest {

    @Resource(name = "netFIdGeneratorService")
    private IdGeneratorService idGeneratorService;

    public IdPool getIdPoolBean() {
        return new MemIdPool("elise", "center-code", 10, idGeneratorService);
    }

    @Test
    public void testGet() {
        for (int i = 0; i < 10; i++) {
            System.out.println("get id generator {}" + getIdPoolBean().get());
        }
    }

    @Test
    public void testThreadGet() throws Exception {
        for (int i = 0; i < 12; i++) {
            Thread t = new Thread(new ThreadTest());
            t.start();
        }
        Thread.sleep(100000);
    }

    class ThreadTest implements Runnable {
        @Override
        public void run() {
            System.out.println("get id generator {}" + getIdPoolBean().get());
        }
    }

}
