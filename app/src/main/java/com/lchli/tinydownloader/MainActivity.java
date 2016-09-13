package com.lchli.tinydownloader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.lchli.tinydownloadlib.IDownloadListener;
import com.lchli.tinydownloadlib.IDownloadManager;
import com.lchli.tinydownloadlib.TinyDownloadConfig;
import com.lchli.tinydownloadlib.TinyDownloadManager;
import com.lchli.tinydownloadlib.TinyDownloadService;
import com.lchli.tinydownloadlib.TinyDownloadTask;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String SAVE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/dowloadlib";

    private ListView listView;
    private IDownloadManager mIDownloadManager;
    private TestAdapter mTestAdapter;
    private Button addTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        addTask = (Button) findViewById(R.id.addTask);
        final String url = "http://" + "192.168.1.7" + ":9090" + "/UserPortraitDir/1.mp4";
        addTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIDownloadManager != null) {
                    try {
                        mIDownloadManager.addTask(new TinyDownloadTask(System.currentTimeMillis() + "", url, SAVE_DIR, System.currentTimeMillis() + ""));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mTestAdapter = new TestAdapter(null);
        listView.setAdapter(mTestAdapter);

        Intent it = new Intent(this, TinyDownloadService.class);
        bindService(it, mServiceConnection, Context.BIND_AUTO_CREATE);

        loadTasks();

    }

    static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else
            return String.format("%d B", size);
    }


    private void loadTasks() {
        new AsyncTask<Void, Void, List<TinyDownloadTask>>() {
            @Override
            protected List<TinyDownloadTask> doInBackground(Void... params) {
                return TinyDownloadManager.queryAllTasks();
            }

            @Override
            protected void onPostExecute(List<TinyDownloadTask> tinyDownloadTasks) {
                mTestAdapter.setDatas(tinyDownloadTasks);
            }

        }.execute();

    }

    private IDownloadListener listener = new IDownloadListener.Stub() {
        @Override
        public void onProgressChanged(final TinyDownloadTask task) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int index = mTestAdapter.datas.indexOf(task);
                    if (index != -1) {
                        mTestAdapter.datas.get(index).currentFinish = task.currentFinish;
                        mTestAdapter.datas.get(index).totalLength = task.totalLength;
                        mTestAdapter.datas.get(index).speed = task.speed;
                        mTestAdapter.notifyDataSetChanged();
                    }
                }
            });

        }

        @Override
        public void onTaskAdded(TinyDownloadTask task) throws RemoteException {
            loadTasks();
        }

        @Override
        public void onTaskDeleted(TinyDownloadTask task) throws RemoteException {
            loadTasks();
        }

        @Override
        public void onDownloadSuccess(final TinyDownloadTask task) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "download success!", Toast.LENGTH_LONG).show();
                    int index = mTestAdapter.datas.indexOf(task);
                    if (index != -1) {
                        mTestAdapter.datas.get(index).state = task.state;
                        mTestAdapter.notifyDataSetChanged();
                    }
                }
            });

        }

        @Override
        public void onDownloadError(TinyDownloadTask task, int errorCode) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "download error!", Toast.LENGTH_LONG).show();
                    mTestAdapter.notifyDataSetChanged();
                }
            });

        }

    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtils.e("onServiceConnected");
            mIDownloadManager = IDownloadManager.Stub.asInterface(service);
            try {
                mIDownloadManager.registerDownloadListener(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtils.e("onServiceDisconnected>>>>>>");
            mIDownloadManager = null;
            Intent it = new Intent(getApplicationContext(), TinyDownloadService.class);
            bindService(it, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    };

    @Override
    protected void onDestroy() {
        if (mIDownloadManager != null) {
            try {
                mIDownloadManager.unregisterDownloadListener(listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    private class TestAdapter extends BaseAdapter {

        List<TinyDownloadTask> datas;


        public TestAdapter(List<TinyDownloadTask> datas) {
            this.datas = datas;
        }

        public void setDatas(List<TinyDownloadTask> datas) {
            this.datas = datas;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return datas != null ? datas.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return datas != null ? datas.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VH item = (VH) convertView;
            if (item == null) {
                item = new VH(getApplicationContext());
            }
            final TinyDownloadTask data = (TinyDownloadTask) getItem(position);
            item.tvTaskName.setText(data.name);
            item.tvTaskUrl.setText(data.url);
            item.tvCurrentFinish.setText(data.currentFinish + "/" + data.totalLength + "(" + convertFileSize(data.speed) + "/s)");
            float progress = (float) data.currentFinish / data.totalLength * 100;
            item.progressBar.setMax(100);
            item.progressBar.setProgress((int) progress);

            if (data.state == TinyDownloadConfig.TASK_STATE_FINISHED) {
                item.button.setText("finished");
                item.button.setEnabled(false);

            } else {
                item.button.setEnabled(true);
                if (mIDownloadManager != null && isTaskRunning(data.id)) {
                    item.button.setText("pause");
                } else {
                    item.button.setText("start");
                }

            }
            final VH finalItem = item;
            item.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mIDownloadManager != null) {
                        try {
                            mIDownloadManager.continueTask(data);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (isTaskRunning(data.id)) {
                            finalItem.button.setText("start");
                            if (mIDownloadManager != null) {
                                try {
                                    mIDownloadManager.pauseTask(data);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            finalItem.button.setText("pause");
                            if (mIDownloadManager != null) {
                                try {
                                    mIDownloadManager.continueTask(data);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            });
            return item;
        }

        private boolean isTaskRunning(String id) {
            try {
                return mIDownloadManager.isTaskDownloading(id);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return false;
        }

        class VH extends FrameLayout {

            private final TextView tvTaskName;
            private final TextView tvTaskUrl;
            private final ProgressBar progressBar;
            private final Button button;
            private final TextView tvCurrentFinish;

            public VH(Context context) {
                super(context);
                View.inflate(context, R.layout.list_item, this);
                tvTaskName = (TextView) findViewById(R.id.tvTaskName);
                tvTaskUrl = (TextView) findViewById(R.id.tvTaskUrl);
                tvCurrentFinish = (TextView) findViewById(R.id.tvCurrentFinish);
                progressBar = (ProgressBar) findViewById(R.id.progressBar);
                button = (Button) findViewById(R.id.button);
            }
        }
    }
}
