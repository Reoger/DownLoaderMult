package com.example.cm.downloadermult.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.example.cm.downloadermult.R;
import com.example.cm.downloadermult.adapter.MultDownTaskAdapter;
import com.example.cm.downloadermult.bean.FileInfo;
import com.example.cm.downloadermult.service.DownLoadServiceMult;

import java.util.ArrayList;
import java.util.List;

import static com.example.cm.downloadermult.util.DownTaskMult.BROAD_FILE_INFO;
import static com.example.cm.downloadermult.util.DownTaskMult.BROAD_FINISH;
import static com.example.cm.downloadermult.util.DownTaskMult.BROAD_ID;

public class NextActivity extends AppCompatActivity {


    ListView mListView;

    List<FileInfo> mFileInfos;
    private MultDownTaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);

        mListView = findViewById(R.id.lv_down_task);
        mFileInfos = initData();
        adapter = new MultDownTaskAdapter(mFileInfos,this);
        mListView.setAdapter(adapter);

        IntentFilter intentFilter = new IntentFilter(DownLoadServiceMult.ACTION_FINISHED);
        intentFilter.addAction(DownLoadServiceMult.ACTION_UPDATE);
        registerReceiver(mBroadcastReceiver,intentFilter);
    }

    private List<FileInfo> initData() {
        List<FileInfo> list = new ArrayList<>();
        FileInfo fileInfo0 = new FileInfo(0,"http://gyxz.exmmw.cn/yq/rj_xqf1/kugouchangchang.apk","kugouchangchang.apk",0,0);
        FileInfo fileInfo1 = new FileInfo(1,"http://p3.exmmw.cn/p1/wn/huajinbaodaikuan.apk","huajinbaodaikuan.apk",0,0);
        FileInfo fileInfo2 = new FileInfo(2,"http://gyxz.exmmw.cn/vp/yx_sw1/warsong.apk","warsong.apk",0,0);
        FileInfo fileInfo3 = new FileInfo(3,"http://gyxz.exmmw.cn/a3/rj_wn1/rishanghuiyuan.apk","rishanghuiyuan.apk",0,0);
        list.add(fileInfo0);
        list.add(fileInfo1);
        list.add(fileInfo2);
        list.add(fileInfo3);
        return list;
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownLoadServiceMult.ACTION_FINISHED.equals(intent.getAction())){
                //这里下载完成
                FileInfo fileInfo = (FileInfo) intent.getSerializableExtra(BROAD_FILE_INFO);
                Toast.makeText(
                        NextActivity.this,
                        fileInfo.getFileName() + "下载完成",
                        Toast.LENGTH_SHORT).show();
            }else if(DownLoadServiceMult.ACTION_UPDATE.equals(intent.getAction())){
                //这里是更新下载进度
                int file_id = intent.getIntExtra(BROAD_ID,0);
                long progress = intent.getLongExtra(BROAD_FINISH,0);
                Log.e("TAG", "onReceive: ---> 正在更新："+file_id+"  <---> "+progress);

                updateSingleRow(mListView,file_id, (int) progress);
            }
        }
    };



    /**
     * 局部刷新 设置可以更新进度
     * @param mListView 指定的控件
     * @param posi 第几个item
     * @param progress  进度
     *                  方法来源：https://www.jianshu.com/p/45a43a117365
     */
    public void updateSingleRow(ListView mListView, int posi ,int progress) {
        if (mListView != null) {
            //获取第一个显示的item
            int visiblePos = mListView.getFirstVisiblePosition();
            //计算出当前选中的position和第一个的差，也就是当前在屏幕中的item位置
            int offset = posi - visiblePos;
            int lenth = mListView.getChildCount();
            // 只有在可见区域才更新,因为不在可见区域得不到Tag,会出现空指针,所以这是必须有的一个步骤
            if ((offset < 0) || (offset >= lenth)) return;
            View convertView = mListView.getChildAt(offset);
           MultDownTaskAdapter.ViewHolder viewHolder = (MultDownTaskAdapter.ViewHolder) convertView.getTag();
            //以下是处理需要处理的控件方法。。。。。
            viewHolder.mProgress.setProgress(progress);
        }
    }


    @Override
    protected void onDestroy() {
        if(mBroadcastReceiver != null){
            unregisterReceiver(mBroadcastReceiver);
        }
        super.onDestroy();
    }
}
