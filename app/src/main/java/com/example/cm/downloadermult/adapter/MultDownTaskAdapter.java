package com.example.cm.downloadermult.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.cm.downloadermult.R;
import com.example.cm.downloadermult.bean.FileInfo;
import com.example.cm.downloadermult.service.DownLoadServiceMult;

import java.util.List;

/**
 * Date: 2018/2/23 16:59
 * Email: luojie@cmcm.com
 * Author: luojie
 * Description: TODO
 */
public class MultDownTaskAdapter extends BaseAdapter {

    List<FileInfo> mList;
    Context mContext;

    public static final String ACTION = "";




    public MultDownTaskAdapter(List<FileInfo> mList, Context mContext) {
        this.mList = mList;
        this.mContext = mContext;
    }


    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        return mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.mBuStart = view.findViewById(R.id.btu_start);
            viewHolder.mBuStop = view.findViewById(R.id.btu_stop);
            viewHolder.mProgress = view.findViewById(R.id.progress_down);
            viewHolder.mTitle = view.findViewById(R.id.text_file_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        final FileInfo info = mList.get(i);
        viewHolder.mTitle.setText(info.getFileName());
        final Intent intent = new Intent(mContext, DownLoadServiceMult.class);
        viewHolder.mBuStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //开始下载
                intent.setAction(DownLoadServiceMult.ACTION_START);
                intent.putExtra(DownLoadServiceMult.EXTRE_INFO, info);
                mContext.startService(intent);

            }
        });
        viewHolder.mBuStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.setAction(DownLoadServiceMult.ACTION_PAUSE);
                intent.putExtra(DownLoadServiceMult.EXTRE_INFO, info);
                mContext.startService(intent);
            }
        });
        //这种情况 不是更新进度
        Log.d("TAG", "getView: ***************~~");

        return view;
    }




   public  static class ViewHolder {
       public TextView mTitle;
       public SeekBar mProgress;
       public Button mBuStart, mBuStop;
    }



}
