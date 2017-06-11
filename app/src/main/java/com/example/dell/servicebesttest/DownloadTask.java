package com.example.dell.servicebesttest;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by DELL on 2017/6/9.
 */

public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;
    //定義4個整型常量用於表示下載的狀態

    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
        //通過listener這個參數進行回調
    }

    @Override
    protected Integer doInBackground(String... params)
    //用於在後台執行具體的下載邏輯
    {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadedLength = 0;//记录已经下载的文件长度
            String downloadUrl = params[0];//從參數中獲取到下載的URL地址
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));//根據URL地址解析出了下載的文件名
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();//指定將文件下載到Environment.DIRECTORY_DOWNLOADS目錄下，也就是SD卡的Download目錄
            file = new File(directory + fileName);
            if (file.exists())//判斷是否已經存在下載了的文件
            {
                downloadedLength = file.length();
                //如果發現已經存在的話則讀取已經下載了的字節數
            }
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
                //已经下载字节和文件总字节相等，说明下载已经完成了
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            //發送一條網絡請求
            Request request = new Request.Builder()
                    //断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")//Header表示的是告訴服務器，需要從哪裡開始已下載
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadedLength);//跳过已经下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELED;//取消操作
                    } else if (isPaused) {
                        return TYPE_PAUSED;//中斷操作
                    } else {
                        total += len;
                        savedFile.write(b, 0, len);
                        //计算已经下载的百分比
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);//實時計算當前的下載速度
                        publishProgress(progress);//進行通知
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (savedFile != null) {
                    savedFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values)
    //用於在界面上更新當前的下載進度
    {
        int progress = values[0];
        if (progress > lastProgress) {
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status)
    //用於通知最終的下載結果
    {
        switch (status) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    public void pauseDownload() {
        isPaused = true;//暫停工作
    }

    public void cancelDownload() {
        isCanceled = true;//取消工作
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
