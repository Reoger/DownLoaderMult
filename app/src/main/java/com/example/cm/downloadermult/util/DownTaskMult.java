package com.example.cm.downloadermult.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.cm.downloadermult.bean.FileInfo;
import com.example.cm.downloadermult.bean.ThreadInfo;
import com.example.cm.downloadermult.db.ThreadDAOMultImpl;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.cm.downloadermult.service.DownLoadServiceMult.ACTION_FINISHED;
import static com.example.cm.downloadermult.service.DownLoadServiceMult.ACTION_UPDATE;
import static com.example.cm.downloadermult.service.DownLoadServiceMult.DOWNLOAD_PATH;

/**
 * Date: 2018/2/23 18:02
 * Email: luojie@cmcm.com
 * Author: luojie
 * Description: TODO
 */
public class DownTaskMult {
    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private ThreadDAOMultImpl mThreadDAOMultImpe = null;

    private long mFinished = 0;

    private int mThreadCount = 1;

    public boolean isPause = false;

    public static ExecutorService mExecutorService = Executors.newCachedThreadPool();


    private static final String TAG = "DownTaskMult";

    private List<DownTaskThread> mThreadList = null;


    public static final String BROAD_ID = "broadcast_id";
    public static final String BROAD_FINISH = "broadcast_finish";
    public static final String BROAD_FILE_INFO = "broadcast_file_info";


    public DownTaskMult(Context context, FileInfo mFileInfo, int threadCount) {
        this.mContext = context;
        this.mFileInfo = mFileInfo;
        this.mThreadCount = threadCount;
        mThreadDAOMultImpe = ThreadDAOMultImpl.getInstance(context);
    }


    /**
     * 开始下载
     */
    public void startDownTask() {
        //开始下载
        List<ThreadInfo> threadInfos = mThreadDAOMultImpe.getThread(mFileInfo.getUrl());
        if (threadInfos.size() == 0) {
            long length = mFileInfo.getLength() / mThreadCount;

            for (int i = 0; i < mThreadCount; i++) {
                ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), length * i, (i + 1) * length - 1, 0);
                if (i + 1 == mThreadCount)
                    threadInfo.setEnd(mFileInfo.getLength());
                //添加到线程信息集合中
                threadInfos.add(threadInfo);

                mThreadDAOMultImpe.insertThread(threadInfo);
            }
        }

        mThreadList = new ArrayList<>();

        //启动多线程进行下载
        for (ThreadInfo threadInfo : threadInfos) {
            DownTaskThread downloadThread = new DownTaskThread(threadInfo);
            mExecutorService.execute(downloadThread);
            mThreadList.add(downloadThread);
        }
    }


    //下载线程
    class DownTaskThread extends Thread {
        private ThreadInfo threadInfo;
        public boolean isFinished = false;//表示线程是否结束

        public DownTaskThread(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }

        @Override
        public void run() {
            //向数据库插入线程信息
//            Log.e("isExists==", mThreadDAO2.isExists(threadInfo.getUrl(), threadInfo.getId()) + "");
//            if (!mThreadDAO2.isExists(threadInfo.getUrl(), threadInfo.getId())) {
//                mThreadDAO2.insertThread(threadInfo);
//            }
            HttpURLConnection connection;
            RandomAccessFile raf;
            InputStream is;
            try {
                URL url = new URL(threadInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("GET");
                //设置下载位置
                long start = threadInfo.getStart() + threadInfo.getFinished();
                connection.setRequestProperty("Range", "bytes=" + start + "-" + threadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DOWNLOAD_PATH, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);

                Intent intent = new Intent(ACTION_UPDATE);
                mFinished += threadInfo.getFinished();
                Log.e("threadInfo.getFinish==", threadInfo.getFinished() + "");

//                Log.e("getResponseCode ===", connection.getResponseCode() + "");
                //开始下载
                if (connection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
                    Log.e("getContentLength==", connection.getContentLength() + "");

                    //读取数据
                    is = connection.getInputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int len = -1;
                    long time = System.currentTimeMillis();
                    while ((len = is.read(buffer)) != -1) {

                        if (isPause) {
                            Log.e("mfinished==pause===", mFinished + " 暂停了，已经运行到这里了，");
                            //下载暂停时，保存进度到数据库
                            mThreadDAOMultImpe.updateThread(threadInfo.getUrl(), threadInfo.getId(),
                                    threadInfo.getFinished());
                            return;
                        }

                        //写入文件
                        raf.write(buffer, 0, len);
                        //累加整个文件下载进度
                        mFinished += len;
                        //累加每个线程完成的进度
                        threadInfo.setFinished(threadInfo.getFinished() + len);
                        //每隔1秒刷新UI
                        if (System.currentTimeMillis() - time > 1000) {//减少UI负载
                            time = System.currentTimeMillis();
                            //把下载进度发送广播给Activity
                            intent.putExtra(BROAD_ID, mFileInfo.getId());
                            intent.putExtra(BROAD_FINISH, mFinished * 100 / mFileInfo.getLength());
                            mContext.sendBroadcast(intent);
                            Log.e(" mFinished==update==", mFinished * 100 / mFileInfo.getLength() + "跟新进度提示");
                        }

                    }
                    //标识线程执行完毕
                    isFinished = true;
                    //检查下载任务是否完成
                    checkAllThreadFinished();
                    is.close();
                }
                raf.close();
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 判断所有线程是否都执行完毕
     */
    private synchronized void checkAllThreadFinished() {
        boolean allFinished = true;
        //编辑线程集合 判断是否执行完毕
        for (DownTaskThread thread : mThreadList) {
            if (!thread.isFinished) {
                allFinished = false;
                break;
            }
        }
        if (allFinished) {
            //删除线程信息
            mThreadDAOMultImpe.deleteThread(mFileInfo.getUrl());
            //发送广播给Activity下载结束
            Intent intent = new Intent(ACTION_FINISHED);
            intent.putExtra(BROAD_FILE_INFO, mFileInfo);
            mContext.sendBroadcast(intent);
        }
    }

}
