package com.example.cm.downloadermult.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.cm.downloadermult.bean.ThreadInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 2018/2/22 19:53
 * Email: luojie@cmcm.com
 * Author: luojie
 * Description: 数据访问接口实现
 */
public class ThreadDAOImpl implements ThreadDAO {
    private static final String TAG = "ThreadDAOImpl";

    private MyDBHelper myDBHelper;

    public ThreadDAOImpl(Context context) {
        this.myDBHelper =  MyDBHelper.getInstance(context);
    }

    /**
     * 数据库插入数据
     * @param threadInfo 线程信息
     */
    @Override
    public void insertThread(ThreadInfo threadInfo) {
        Log.e("insertThread: ", "insertThread");
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        db.execSQL("insert into thread_info(thread_id,url,start,end,finished) values(?,?,?,?,?)",
                new Object[]{threadInfo.getId(), threadInfo.getUrl(),
                        threadInfo.getStart(), threadInfo.getEnd(), threadInfo.getFinished()});
        db.close();
    }

    /**
     * 删除下载好的文件下载信息
     * @param url       地址
     * @param thread_id id
     */
    @Override
    public void deleteThread(String url, int thread_id) {
        Log.e("deleteThread: ", "deleteThread");
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        db.execSQL("delete from  thread_info where url = ? and thread_id= ?",
                new Object[]{url, thread_id});
        db.close();
    }

    /**
     * 更新下载进度到数据库中
     * @param url       地址
     * @param thread_id id
     * @param finished  完成进度
     */
    @Override
    public void updateThread(String url, int thread_id, long finished) {
        Log.e("updateThread: ", "updateThread 更新的进度为+"+finished+" 跟新的id为"+thread_id+"-- url为："+url);
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        String sql = "update thread_info set finished = "+finished+" where url = '"+url+"' and thread_id = "+thread_id+";";
        db.execSQL("update thread_info set finished = ?  where url = ? and thread_id = ?",
                new Object[]{finished, url, thread_id});

        Log.e(TAG, "updateThread: ----[[:"+sql );
//        db.execSQL(sql);
        db.close();
    }

    /**
     * 查询数据库中下载某个url的线程列表
     * @param url 地址
     * @return
     */
    @Override
    public List<ThreadInfo> getThread(String url) {
        Log.e("getThread: ", "getThread");
        List<ThreadInfo> list = new ArrayList<>();
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from thread_info where url=?", new String[]{url});
        while (cursor.moveToNext()) {
            ThreadInfo thread = new ThreadInfo();
            thread.setId(cursor.getInt(cursor.getColumnIndex("thread_id")));
            thread.setUrl(cursor.getString(cursor.getColumnIndex("url")));
            thread.setStart(cursor.getLong(cursor.getColumnIndex("start")));
            thread.setEnd(cursor.getLong(cursor.getColumnIndex("end")));
            thread.setFinished(cursor.getLong(cursor.getColumnIndex("finished")));
            list.add(thread);
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * 判断下载指定url的线程是否存在
     * @param url       地址
     * @param thread_id id
     * @return
     */
    @Override
    public boolean isExists(String url, int thread_id) {
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from thread_info where url=? and thread_id = ?",
                new String[]{url, String.valueOf(thread_id)});
        boolean isExist = cursor.moveToNext();
        cursor.close();
        db.close();
        Log.e(TAG, "isExists: " + isExist);
        return isExist;
    }
}
