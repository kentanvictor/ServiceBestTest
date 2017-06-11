package com.example.dell.servicebesttest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    private DownloadTask downloadTask;
    private String downloadUrl;
    private DownloadListener listener = new DownloadListener() {
        //匿名類中實現方法
        @Override
        public void onProgress(int progress) {
            getNotifictionManager().notify(1, getNotification("Downloading……", progress));
            //顯示下載進度的通知，NotificationManager的notify()方法觸發通知
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            //下載成功時將前台服務通知關閉，并創建一個下載成功的通知
            stopForeground(true);
            getNotifictionManager().notify(1, getNotification("Download Success", -1));
            //用來關掉正在下載的前台通知
            Toast.makeText(DownloadService.this, "Download success", Toast.LENGTH_SHORT).show();
            //新建一個Toast告訴用戶已經下載完成了
        }

        @Override
        public void onFailed() {
            downloadTask = null;
            //下載失敗時將前台服務通知關閉,並創建一個下載失敗的通知
            stopForeground(true);
            getNotifictionManager().notify(1, getNotification("Download Failed", -1));
            Toast.makeText(DownloadService.this, "Download Failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downloadTask = null;
            Toast.makeText(DownloadService.this, "Paused", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
        }
    };
    private DownloadBinder mBinder = new DownloadBinder();


    class DownloadBinder extends Binder {
        public void startDownload(String url) {
            if (downloadTask == null) {
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                //创建了一个DownloadTask的实例，把刚才的DownloadListener作为参数传进去
                downloadTask.execute(downloadUrl);
                //调用execute方法，开启下载，并将下载文件的URL传入到execute里面
                startForeground(1, getNotification("Downloading……", 0));
                //调用startForeground方法使得下载服务成为一个前台服务
                Toast.makeText(DownloadService.this, "Downloading……", Toast.LENGTH_SHORT).show();
            }
        }

        public void pauseDownload() {
            if (downloadTask != null) {
                downloadTask.pauseDownload();
                //调用一下downloadTask中的pauseDownload方法
            }
        }

        public void cancelDownload() {
            //取消下載
            if (downloadTask != null) {
                downloadTask.cancelDownload();
            } else {
                if (downloadUrl != null) {
                    //取消下載時需將文件刪除，並將通知關閉
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    getNotifictionManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private NotificationManager getNotifictionManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress > 0) {
            //當progress大於或等於0時才需要顯示下載速度
            builder.setContentText(progress + "%")
                    .setProgress(100, progress, false);
            /**setProgress参数中第一个参数指的是传入通知的最大进度
             * 第二个参数是传入通知的当前进度
             * 第三个参数表示是否使用模糊进度条
             */
        }
        return builder.build();
    }
}
