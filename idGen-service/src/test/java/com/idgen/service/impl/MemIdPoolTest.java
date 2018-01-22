package com.idgen.service.impl;

import com.google.common.collect.Lists;
import com.idgen.client.common.IdPool;
import com.idgen.client.common.MemIdPool;
import com.idgen.client.service.IdGeneratorService;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

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

    @Test
    public void localTimeTest() {
        String h1 = "02:20";
        LocalTime t1 = LocalTime.parse(h1);
        System.out.println(t1.isAfter(LocalTime.now()));

        String url = "./guahao/#/vip_process?steeringNo=${steeringNo}&doctorIdentityCode=${identityCode}";
        if (url.contains("${steeringNo}")) {
            System.out.println(true);
        }
        url = url.replace("${steeringNo}", "12110").replace("${identityCode}", "2009");
        System.out.println(url);
    }

    @Test
    public void timeBetween() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar_ = Calendar.getInstance();
        calendar_.setTime(sdf.parse("1987-10-01"));
        Calendar calendar = Calendar.getInstance();
        System.out.println(calendar.get(Calendar.YEAR) - calendar_.get(Calendar.YEAR));
    }

    @Test
    public void testGetDate() {
        String[] weekOfDays = new String[]{"MON", "TUE", "FRI"};
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        System.out.println(dayOfWeek);
        for (String weekOfDay : weekOfDays) {
            int plusDay = 0;
            if ("MON".equals(weekOfDay)) {  //2
                plusDay = 2;
            } else if ("TUE".equals(weekOfDay)) {  //3
                plusDay = 3;
            } else if ("WED".equals(weekOfDay)) { //4
                plusDay = 4;
            } else if ("THURS".equals(weekOfDay)) { //5
                plusDay = 5;
            } else if ("FRI".equals(weekOfDay)) { //6
                plusDay = 6;
            } else if ("SAT".equals(weekOfDay)) {  //7
                plusDay = 7;
            } else if ("SUN".equals(weekOfDay)) { //1
                plusDay = 1;
            }
            if (dayOfWeek <= plusDay) {
                Calendar calendar_ = Calendar.getInstance();
                calendar_.add(Calendar.DATE, plusDay - dayOfWeek);
                System.out.println(new SimpleDateFormat("yyyy-MM-dd").format(calendar_.getTime()));
            } else {
                Calendar calendar_ = Calendar.getInstance();
                calendar_.add(Calendar.DATE, 7 + plusDay - dayOfWeek);
                System.out.println(new SimpleDateFormat("yyyy-MM-dd").format(calendar_.getTime()));
            }
        }
    }

    @Test
    public void testSort() {
        List<TestDTO> testDTOList = Lists.newArrayList();
        TestDTO testDTO = new TestDTO();
        testDTO.value = 10;
        testDTOList.add(testDTO);
        TestDTO testDTO_ = new TestDTO();
        testDTO_.value = 2;
        testDTOList.add(testDTO_);
        List<TestDTO> result = testDTOList.stream().sorted((dto, dto_) -> (dto.value - dto_.value > 0 ? -1 : 1)).collect(Collectors.toList());
        System.out.println(result);
    }

    class TestDTO {
        private int value;

        @Override
        public String toString() {
            return "TestDTO{" +
                    "value=" + value +
                    '}';
        }
    }

}
