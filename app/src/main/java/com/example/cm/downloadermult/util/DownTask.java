package com.example.cm.downloadermult.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.cm.downloadermult.bean.FileInfo;
import com.example.cm.downloadermult.bean.ThreadInfo;
import com.example.cm.downloadermult.db.ThreadDAOImpl;
import com.example.cm.downloadermult.service.DownLoadService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Date: 2018/2/22 20:01
 * Email: luojie@cmcm.com
 * Author: luojie
 * Description: 单线程下载任务
 */
public class DownTask {
    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private ThreadDAOImpl mThreadDAOImpe = null;

    private long mFinished =0;

    public boolean isPause = false;

    private static final String TAG = "DownTask";


    public DownTask(Context mContext, FileInfo mFileInfo) {
        this.mContext = mContext;
        this.mFileInfo = mFileInfo;
        mThreadDAOImpe = new ThreadDAOImpl(mContext);
    }

    public void startDownTask(){
        List<ThreadInfo> threadInfos = mThreadDAOImpe.getThread(mFileInfo.getUrl());
        ThreadInfo info;
        if(threadInfos.size() == 0){
            info = new ThreadInfo(0,mFileInfo.getUrl(),0,mFileInfo.getLength(),0);
            Log.e(TAG, "startDownTask: size ==0 "+info.toString());
        }else{
            info = threadInfos.get(0);
            Log.e(TAG, "startDownTask: size !=0 "+info.toString());
        }

        Thread a = new DownloadThread(info);
        a.start();
    }

    class DownloadThread extends Thread{
        private ThreadInfo threadInfo = null;

        public DownloadThread(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }

        @Override
        public void run() {
            //如果数据库中，不存在记录就要插入数据
            if(!mThreadDAOImpe.isExists(threadInfo.getUrl(),threadInfo.getId())){
                mThreadDAOImpe.insertThread(threadInfo);
            }

            HttpURLConnection connection = null;
            RandomAccessFile raf = null;
            InputStream is = null;


            try {
                URL url = new URL(threadInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("GET");

                //设置下载位置
                long start = threadInfo.getStart() + threadInfo.getFinished();
                Log.e(TAG, "run: 继续下载进度为"+start );
                connection.setRequestProperty("Range","bytes="+start+"-"+threadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DownLoadService.DOWNLOAD_PATH,mFileInfo.getFileName());
                raf = new RandomAccessFile(file,"rwd");
                raf.seek(start);

                //设置广播
                Intent intent = new Intent(DownLoadService.ACTION_UPDATE);
                //从上次停止的地方继续下载
                mFinished += threadInfo.getFinished();
                Log.e(TAG, "上次下载的进度："+mFinished);

                if(connection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
                    is = connection.getInputStream();
                    byte[] buffer = new byte[4096];
                    int len= -1;
                    long time = System.currentTimeMillis();
                    while((len = is.read(buffer)) != -1){
                        //下载暂停时，保存进度
                        if(isPause){
                            Log.e(TAG, "run: 进度为："+mFinished );
                            mThreadDAOImpe.updateThread(mFileInfo.getUrl(),threadInfo.getId(),mFinished);
                            return;
                        }

                        raf.write(buffer,0,len);
                        mFinished += len;

                        if (System.currentTimeMillis() - time > 500){
                            time = System.currentTimeMillis();
                            intent.putExtra("finished",mFinished * 100 /mFileInfo.getLength());
                            Log.e(TAG, "run: 这里发送广播了"+ mFinished +" -- "+mFileInfo.getLength());
                            mContext.sendBroadcast(intent);
                        }
                    }

                    intent.putExtra("finished",(long) 100);
                    mContext.sendBroadcast(intent);
                    mThreadDAOImpe.deleteThread(mFileInfo.getUrl(),mFileInfo.getId());

                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "run: ");
            } finally {
                try {
                    if (is != null)
                        is.close();
                    if(raf != null)
                        raf.close();
                    if (connection != null)
                        connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
