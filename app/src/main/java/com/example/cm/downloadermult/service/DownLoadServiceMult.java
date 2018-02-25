package com.example.cm.downloadermult.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.cm.downloadermult.bean.FileInfo;
import com.example.cm.downloadermult.util.DownTask;
import com.example.cm.downloadermult.util.DownTaskMult;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Date: 2018/2/23 17:11
 * Email: luojie@cmcm.com
 * Author: luojie
 * Description: TODO
 */
public class DownLoadServiceMult extends Service {

    public static final String ACTION_START = "ation-start";
    public static final String ACTION_PAUSE = "ation-pause";
    public static final String TAG = "uuuuu";
    public static final String EXTRE_INFO = "extre_info";

    public static final String  DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/downMult/";
    private static final int MSG_INIT = 0;

    public static final String ACTION_FINISHED = "action_finished";

    public static final String ACTION_UPDATE = "action_update";

    //下载任务集合
    private Map<Integer, DownTaskMult> tasks = new LinkedHashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FileInfo info;
        if (ACTION_START.equals(intent.getAction())){
            //开始下载的逻辑
            info= (FileInfo) intent.getSerializableExtra(EXTRE_INFO);
            Log.d(TAG, "onStartCommand: 这里是开始的逻辑"+info.toString());
            new InitThread(info).start();
        }else if(ACTION_PAUSE.equals(intent.getAction())){
            //暂停下载的逻辑
            info= (FileInfo) intent.getSerializableExtra(EXTRE_INFO);
            Log.d(TAG, "onStartCommand:  这里是暂停的逻辑： "+info.toString());
            DownTaskMult downTaskMult = tasks.get(info.getId());
            if(downTaskMult != null){
                downTaskMult.isPause = true;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    FileInfo fileinfo = (FileInfo) msg.obj;
                    Log.e("mHandler--fileinfo:", fileinfo.toString());
                    //启动下载任务
                    DownTaskMult instance = new DownTaskMult(DownLoadServiceMult.this,fileinfo,3);
                    instance.startDownTask();
                    tasks.put(fileinfo.getId(),instance);
                    break;
            }
        }
    };


    /**
     * 初始化 子线程
     */
    class InitThread extends Thread {
        private FileInfo tFileInfo;

        public InitThread(FileInfo tFileInfo) {
            this.tFileInfo = tFileInfo;
        }

        @Override
        public void run() {
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            try {
                //连接网络文件
                URL url = new URL(tFileInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("GET");
                int length = -1;
                Log.e("getResponseCode==", conn.getResponseCode() + "");
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    //获取文件长度
                    length = conn.getContentLength();
                    Log.e("length==", length + "");
                }
                if (length < 0) {
                    return;
                }
                File dir = new File(DOWNLOAD_PATH);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                //在本地创建文件
                File file = new File(dir, tFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                //设置本地文件长度
                raf.setLength(length);
                tFileInfo.setLength(length);
                Log.e("tFileInfo.getLength==", tFileInfo.getLength() + "");
                mHandler.obtainMessage(MSG_INIT, tFileInfo).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (conn != null && raf != null) {
                        raf.close();
                        conn.disconnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }


}
