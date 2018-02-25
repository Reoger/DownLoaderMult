package com.example.cm.downloadermult.bean;

/**
 * Date: 2018/2/22 17:05
 * Email: luojie@cmcm.com
 * Author: luojie
 * Description: TODO
 */
public class ThreadInfo  {
    private int id;
    private String url;
    private long start;
    private long end;
    private long finished;

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public ThreadInfo() {
    }

    public ThreadInfo(int id, String url, long start, long end, long finished) {
        this.id = id;
        this.url = url;
        this.start = start;
        this.end = end;
        this.finished = finished;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("ThreadInfo{");
        sb.append("id=").append(id);
        sb.append(", url='").append(url).append('\'');
        sb.append(", start='").append(start).append('\'');
        sb.append(", end='").append(end).append('\'');
        sb.append(", finish=").append(finished);
        sb.append('}');
        return sb.toString();
    }
}
