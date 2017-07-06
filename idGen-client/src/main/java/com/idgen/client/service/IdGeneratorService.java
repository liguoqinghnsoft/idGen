package com.idgen.client.service;

/**
 * Created by liguoqing on 2017/7/5.
 */
public interface IdGeneratorService {

    /**
     * 分布式唯一ID生成
     * 一次可分配多个，由依赖方控制
     * @param lock 一般为应用名（数据库名）
     * @param table 表
     * @param assignNum 分配数量(<=500)
     * @return like 1,2,3,4....
     */
    public String getNextId(String lock,String table,int assignNum);

}
