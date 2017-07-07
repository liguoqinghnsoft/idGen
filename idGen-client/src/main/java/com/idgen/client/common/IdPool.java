package com.idgen.client.common;

/**
 * Created by liguoqing on 2017/7/7.
 */
public interface IdPool {

    public String get();

    public void giveBack(String id);

}
