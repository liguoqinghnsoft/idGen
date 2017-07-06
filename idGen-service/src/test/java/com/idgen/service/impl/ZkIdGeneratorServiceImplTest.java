package com.idgen.service.impl;

import com.idgen.client.service.IdGeneratorService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.util.stream.Stream;

/**
 * Created by liguoqing on 2017/7/6.
 */
@ActiveProfiles("dev")
@EnableAutoConfiguration
public class ZkIdGeneratorServiceImplTest extends BaseTest {

    @Resource
    private IdGeneratorService idGeneratorService;

    @Test
    public void getNextIdTest() {
        String ids = idGeneratorService.getNextId("elise", "center-code", 10);
        if (StringUtils.isNotEmpty(ids)) {
            Stream.of(ids.split(",")).forEach(id -> {
                System.out.println("id->" + id);
            });
        }
    }

    @Test
    public void getNextIdThreadTest() throws Exception{
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(new Target(i));
            t.start();
        }
        Thread.sleep(10000000);
    }

    class Target implements Runnable {
        private int idx;

        public Target() {
        }

        public Target(int i) {
            this.idx = i;
        }

        @Override
        public void run() {
            String ids = idGeneratorService.getNextId("elise", "center-code", 10);
            if (StringUtils.isNotEmpty(ids)) {
                Stream.of(ids.split(",")).forEach(id -> {
                    System.out.println(idx + "<-id->" + id);
                });
            }
        }
    }

}
