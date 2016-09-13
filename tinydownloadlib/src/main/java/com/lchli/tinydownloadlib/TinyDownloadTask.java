package com.lchli.tinydownloadlib;

import android.os.Parcel;
import android.os.Parcelable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;

/**
 * @author lchli
 */
@Entity
public class TinyDownloadTask implements Parcelable {
    @Id
    public String id;
    public String url;
    public String saveDir;
    public String name;
    public long totalLength = 0;
    public long currentFinish = 0;//temp.
    public int state = TinyDownloadConfig.TASK_STATE_UNFINISHED;//finish,unfinish.
    @Keep
    public long speed = 0;

    @Generated(hash = 1918710703)
    public TinyDownloadTask(String id, String url, String saveDir, String name,
                            long totalLength, long currentFinish, int state, long speed) {
        this.id = id;
        this.url = url;
        this.saveDir = saveDir;
        this.name = name;
        this.totalLength = totalLength;
        this.currentFinish = currentFinish;
        this.state = state;
        this.speed = speed;
    }

    @Keep
    @Generated(hash = 1689830199)
    public TinyDownloadTask(String id, String url, String saveDir, String name) {
        this.id = id;
        this.url = url;
        this.saveDir = saveDir;
        this.name = name;
    }

    @Generated(hash = 1689830199)
    public TinyDownloadTask() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TinyDownloadTask that = (TinyDownloadTask) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getCurrentFinish() {
        return this.currentFinish;
    }

    public void setCurrentFinish(long currentFinish) {
        this.currentFinish = currentFinish;
    }

    public long getTotalLength() {
        return this.totalLength;
    }

    public void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSaveDir() {
        return this.saveDir;
    }

    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSpeed() {
        return this.speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.url);
        dest.writeString(this.saveDir);
        dest.writeString(this.name);
        dest.writeLong(this.totalLength);
        dest.writeLong(this.currentFinish);
        dest.writeInt(this.state);
        dest.writeLong(this.speed);
    }

    public void readFromParcel(Parcel in) {
        this.id = in.readString();
        this.url = in.readString();
        this.saveDir = in.readString();
        this.name = in.readString();
        this.totalLength = in.readLong();
        this.currentFinish = in.readLong();
        this.state = in.readInt();
        this.speed = in.readLong();
    }


    protected TinyDownloadTask(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<TinyDownloadTask> CREATOR = new Parcelable.Creator<TinyDownloadTask>() {
        @Override
        public TinyDownloadTask createFromParcel(Parcel source) {
            return new TinyDownloadTask(source);
        }

        @Override
        public TinyDownloadTask[] newArray(int size) {
            return new TinyDownloadTask[size];
        }
    };

    @Override
    public String toString() {
        return "TinyDownloadTask{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", saveDir='" + saveDir + '\'' +
                ", name='" + name + '\'' +
                ", totalLength=" + totalLength +
                ", currentFinish=" + currentFinish +
                ", state=" + state +
                ", speed=" + speed +
                '}';
    }
}
