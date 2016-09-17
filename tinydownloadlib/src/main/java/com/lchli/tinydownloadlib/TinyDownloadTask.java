package com.lchli.tinydownloadlib;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * @author lchli
 */
@DatabaseTable(tableName = "TinyDownloadTask")
public class TinyDownloadTask implements Parcelable {

    @DatabaseField(generatedId = true)
    public long id;
    @DatabaseField
    public String uid;
    @DatabaseField
    public String url;
    @DatabaseField
    public String saveDir;
    @DatabaseField
    public String name;
    @DatabaseField
    public long totalLength = 0;

    public long currentFinish = 0;//temp.

    @DatabaseField
    public int state = TinyDownloadConfig.TASK_STATE_UNFINISHED;//finish,unfinish.

    public long speed = 0;

    @DatabaseField
    public int threadCount;


    public TinyDownloadTask(String uid, String url, String saveDir, String name) {
        this.uid = uid;
        this.url = url;
        this.saveDir = saveDir;
        this.name = name;
    }

    public TinyDownloadTask() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TinyDownloadTask that = (TinyDownloadTask) o;

        return uid.equals(that.uid);

    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uid);
        dest.writeString(this.url);
        dest.writeString(this.saveDir);
        dest.writeString(this.name);
        dest.writeLong(this.totalLength);
        dest.writeLong(this.currentFinish);
        dest.writeInt(this.state);
        dest.writeLong(this.speed);
    }

    public void readFromParcel(Parcel in) {
        this.uid = in.readString();
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
                "uid='" + uid + '\'' +
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
