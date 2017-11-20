package com.android.rui.download;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadActivity extends AppCompatActivity {

    private final String TAG = DownloadActivity.class.getSimpleName();

    private final String URL = "http://softfile.3g.qq.com:8080/msoft/179/24659/43549/qq_hd_mini_1.4.apk";
    private final String FILE_PATH = Environment.getExternalStorageDirectory() + "/myDownload.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
    }


    public void startDownload(View view) {

        Request request = new Request.Builder()
                .url(URL)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();
        Call mDownloadCall = okHttpClient.newCall(request);
        mDownloadCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //失败的时候回调，打印异常信息
                Log.e(TAG, "onFailure: -------- " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                ResponseBody body = response.body();
                long totleBytes = body.contentLength();//得到文件的总大小
                InputStream is = body.byteStream();//得到一个流文件
                FileOutputStream fos = new FileOutputStream(FILE_PATH);
                byte[] buf = new byte[2048];
                long currentBytes = 0L;
                int len = 0;
                //将文件写入本地（再次友情提示：记得权限问题）
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    currentBytes += len;//每次写入的大小累加
                    int progress = (int) ((currentBytes * 100) / totleBytes);//转换得到当前下载的进度%
                    Log.e(TAG, "onResponse: --------- 下载进度:" + progress + "%");
                }
                fos.flush();
                fos.close();
                is.close();
            }
        });

    }
}
