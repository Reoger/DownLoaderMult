# android 中断点续传

## 单线程断点续传
> 所谓的断点续传就是在下载一个文件时，文件没有完全下载，中途暂停，那么再次下载时就会继续从上次暂停的地方继续下载，而不需要重新开始下载。

## 原理
实现断点续传的原理其实也比较简单，就是知识点比较多，在这里一并进行记录。要实现断点续传，就需要在文件下载暂停时，将当前文件的下载进度保存到数据库中，再次下载时，先从数据中取出上次保存的下载进度，沿用上次的下载进度进行下载。（通过seek（）方法在文件任意位置实现写入，通过setRequestProperty（）方法设置数据从哪里开始下载）。
 

## 实现
下面就以一个具体的实例来实现单线程断点续传。代码的来源来自于视屏：<https://www.imooc.com/video/7318>

### bean对象的实现
在实现断点续传的时候，一般需要两个bean对象，分别取名为：``FileInfo``和``ThreadInfo``，其中``FileInfo``用来表示要文件的相关状态，主要包括下载文件的url、下载的文件名、下载文件的id、文件总长度和已经完成的部分。参考代码如下：
```
public class FileInfo implements Serializable{

    private int id;
    private String url;
    private String fileName;
    private long length;
    private long finish;
    //省略了set和get方法
}
```
其中的``ThreadInfo``对象用来表示下载相关的相关状态，主要包括线程Id、与当前线程相关的url、线程的开始下载的位置、线程下载结束位置和线程已经完成的下载情况，参考代码如下：
```
public class ThreadInfo  {
    private int id;
    private String url;
    private long start;
    private long end;
    private long finished;
    //省略了set和get方法
}
```
上面两个bean对象就是实现断点续传要用到的对象。下面接续来实现断点续传。

### 数据库操作类的实现
在进行正式的下载之前，有必要先实现数据库的增删改查操作。因为我们在开始下载时，需要先查询数据库本次下载是否之前有过现在，如果有，就需要继续上一次下载的地方继续下载。这里数据库的操作也就最基本的数据操作，直接上代码了。不多解释了。
数据库帮助类的``MyDBHelper``的实现:
```
public class MyDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "download.db";
    private static final String SQL_CREATE = "create table thread_info(_id integer primary key autoincrement," +
            "thread_id integer,url text,start long,end long,finished long)";
    private static final String SQL_DROP = "drop table if exists thread_info";
    private static final int VERSION = 1;



    private static MyDBHelper myDBHelper;



    private MyDBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    public static MyDBHelper getInstance(Context context){
        if(myDBHelper == null){
            myDBHelper = new MyDBHelper(context);
        }
        return myDBHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP);
        db.execSQL(SQL_CREATE);
    }

}
```
我们要实现的数据操作,最好封装在一个类中：
```
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

```
至于``ThreadDAO``就是自定义的接口，里面定义了在``ThreadDAOImpl``的实现的方法，代码就不贴出来了。
以上，就是数据库相关的代码，算的上是比较简单了。

### 下载任务类的实现
我们来进行最重要，最核心的下载类的实现。直接来看代码吧：
```
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
```
上面的代码不多，但是都是一些比较关键的代码。先来梳理一下我们的整体逻辑：可以看出来，我们实现下载的逻辑都封装在``startDownTask()``方法中，而``startDownTask()``中真正下载的实现都在``DownloadThread``线程中。在``startDownTask()``，先尝试从数据库中去取下载的``ThreadInfo``对象，如果有相关线程就沿用上次的``ThreadInfo``对象。如果没有下载线程，就创建新的``ThreadInfo``对象来进行下载。
当然，最重要的还是``DownloadThread``线程的实现：首先判断数据库中是否有当前使用的``ThreadInfo``对象的数据，如果没有就添加``ThreadInfo``对象数据到数据库中。然后开始正式的下载；
下载的关键代码就是利用``setRequestProperty``设置请求数据的位置，利用``seek``设置写入文件的位置。为了实现暂停的效果，添加了``isPause``，当其值为ture时，表示下载暂停，保存当前下载进度到数据中，然后直接返回。
在下载的过程中，每隔500毫秒



### 服务类的实现
一般来说，下载任务需要放在服务中，因为服务在进入后台时不会被轻易杀死。下面我们来实现下载的服务类：
```
public class DownLoadService extends Service {

    private static final String TAG = "DownloadService";
    //初始化
    private static final int MSG_INIT = 0;
    //开始下载
    public static final String ACTION_START = "ACTION_START";
    //暂停下载
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    //结束下载
    public static final String ACTION_FINISHED = "ACTION_FINISHED";
    //更新UI
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    //下载路径
    public static final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/downloads/";


    private DownTask mDownloadTask;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获得Activity传来的参数
        if (ACTION_START.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileinfo");
            Log.e(TAG, "onStartCommand: ACTION_START-" + fileInfo.toString());
            new InitThread(fileInfo).start();
        } else if (ACTION_PAUSE.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileinfo");
            Log.e(TAG, "onStartCommand:ACTION_PAUSE- " + fileInfo.toString());
            if (mDownloadTask != null) {
                mDownloadTask.isPause = true;
            }

        }
        return super.onStartCommand(intent, flags, startId);
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
                    mDownloadTask = new DownTask(DownLoadService.this, fileinfo);
                    mDownloadTask.startDownTask();
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
```
可以看到，在service中实现了一个``InitThread``线程，它用于获取要下载文件的大小，并创建文件到指定的文件夹。最后，通过handler实现调用之前写好的下载类，完成下载。
总的来说，service的实现是比较简单的。他主要实现了文件的下载和暂停两个主要的功能，文件的下载主要通过``InitThread``线程初始化下载环境，并通过handler调用``startDownTask()``下载类实现下载；暂停的实现就更加简单了，只需要将``downTask``的中``isPause``设置为``ture``即可。

### 界面展示
下载的实现已经完成了，我们将其实现到界面上。
```
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

```
在activity中只是绑定之前实现的功能，应该不难理解。重要是在activity中注册了一个广播，用于接收``downtask``发送的下载进度广播。并进度显示出来。

## 多线程断点续传下载
>有了前面单线程断点续传的实现，相信实现多线程断线续传也不是很难。多线程下载的原理：
例如，我要下载一个大小为3096k的文件，我用三个线程a,b,c来进行下载，那么可以用a线程下载这个文件0-1024k的数据，用b线程下载1025-2048k的数据，用c线程下载2049-3096k的数据。这样就实现了多个线程对同一个文件的下载，当然，这个只是多线程下载的原理，事实上，有更好划分下载任务的方法，这里就不过多的研究。

### 下载核心
有了前面单线程的基础，只需要在单线程的基础上做一定的修改就可以了,修改后的下载关键代码如下：
```
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
```

在开始下载时，还是先从数据库中取出相关的``ThreadInfo``对象，然后判断对象是否存在，如果存在就取出来复用，否则创建新的对象，这里需要创建多个线程来进行下载。为了统一管理，将这些线程都添加到threadinfos集合中，然后将数据插入到数据库中。然后启动这些线程来进行下载，在下载的时候使用``newCachedThreadPool()``线程池来运行任务，以免线程消耗过大。
然后，下载过程中的进度计算与单线程的计算方式也稍微有点不同，因为这里有多个线程进行下载，所有需要将每个线程下载的进度相加进而计算最终的结果。还有每个线程完成有``isFinished``用于表示当前线程下载任务是否完成。最终判断任务是否下载完成时，需要判断所有的线程都是否执行完毕，只有所有的线程都执行完毕，当前的下载任务才是执行完毕了。

###
其他，因为涉及到多线程，当然要避免一些因为多线程产生的问题，特别是在进行数据库的写、删、改操作时，特别要主要线程的安全性。其他的实现其实和单线程的差不多，具体细节可以参考给出的代码：




