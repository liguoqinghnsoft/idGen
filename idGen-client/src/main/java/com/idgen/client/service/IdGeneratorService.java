package com.idgen.client.service;

/**
 * Created by liguoqing on 2017/7/5.
 */
public interface IdGeneratorService {

    /**
     * �ֲ�ʽΨһID����
     * һ�οɷ�������������������
     * @param lock һ��ΪӦ���������ݿ�����
     * @param table ��
     * @param assignNum ��������(<=500)
     * @return like 1,2,3,4....
     */
    public String getNextId(String lock,String table,int assignNum);

}
