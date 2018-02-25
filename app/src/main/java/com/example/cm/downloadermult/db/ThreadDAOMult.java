package com.example.cm.downloadermult.db;

import com.example.cm.downloadermult.bean.ThreadInfo;

import java.util.List;

/**
 * Date: 2018/2/23 17:38
 * Email: luojie@cmcm.com
 * Author: luojie
 * Description: TODO
 */
public interface ThreadDAOMult {
    /**
     * 插入线程信息
     *
     * @param threadInfo 线程信息
     */
    void insertThread(ThreadInfo threadInfo);

    /**
     * 删除线程信息
     *
     * @param url       地址
     * @param thread_id id
     */
    void deleteThread(String url);


    /**
     * /**
     * 更新线程信息
     *
     * @param url       地址
     * @param thread_id id
     * @param finished  完成进度
     */
    void updateThread(String url, int thread_id, long finished);


    /**
     * 查询文件的线程信息
     *
     * @param url 地址
     * @return 信息
     */
    List<ThreadInfo> getThread(String url);


    /**
     * 判断是否存在
     *
     * @param url       地址
     * @param thread_id id
     * @return 是否存在
     */
    boolean isExists(String url, int thread_id);

}
