package com.example.cm.downloadermult.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.cm.downloadermult.R;
import com.example.cm.downloadermult.bean.FileInfo;
import com.example.cm.downloadermult.service.DownLoadService;


public class MainActivity extends AppCompatActivity {

    TextView mTVFileName ;
    SeekBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTVFileName = findViewById(R.id.text_file_name);

        String url = "http://ucan.25pp.com/Wandoujia_web_seo_baidu_homepage.apk";
//        String url = "http://sqdownb.onlinedown.net/down/KSbrowser_rytx1_u201712012.exe";
        String fileName = "homepage.apk";

        mTVFileName.setText(fileName);
        final FileInfo info = new FileInfo(2,url,fileName,0,0);

        progressBar = findViewById(R.id.progress_down);
        progressBar.setMax(100);

        findViewById(R.id.btu_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,DownLoadService.class);
                intent.setAction(DownLoadService.ACTION_START);
                intent.putExtra("fileinfo",info);
                startService(intent);
            }
        });
        findViewById(R.id.btu_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,DownLoadService.class);
                intent.setAction(DownLoadService.ACTION_PAUSE);
                intent.putExtra("fileinfo",info);
                startService(intent);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(DownLoadService.ACTION_UPDATE);
        registerReceiver(mReceiver,filter);
    }

    /**
     * 更新UI的广播接收器
     */
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownLoadService.ACTION_UPDATE.equals(intent.getAction())) {
                long finished = intent.getLongExtra("finished", 100);
                progressBar.setProgress((int) finished);
                mTVFileName.setText(new StringBuffer().append(finished).append("%"));
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null){
            unregisterReceiver(mReceiver);
        }
    }

    public void startMultDpownTask(View view){
        Intent intent = new Intent(MainActivity.this,NextActivity.class);
        startActivity(intent);
    }
}
