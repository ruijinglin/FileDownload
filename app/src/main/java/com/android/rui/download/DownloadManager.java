package com.android.rui.download;

import android.content.Context;
import android.util.Log;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;
import static com.android.rui.download.SharedPreferencedUtils.getLong;

/**
 * @author: RuiJingLin
 * @date 2017/10/27.
 * 安卓最简单的文件下载,基于OKHttp,包含下载进度、断点续传
 */

public class DownloadManager {

    private final String TAG = DownloadManager.class.getSimpleName();

    private String mFilePath;
    private Context mContext;
    private Long mBreakPoint = 0L;
    private IDownloadProgress mIDownloadProgress;
    private int mTotleLength;
    private boolean mIsFirst = true;
    private Call mDownloadCall;


    public DownloadManager(Context mContext, IDownloadProgress mIDownloadProgress) {
        this.mContext = mContext;
        this.mIDownloadProgress = mIDownloadProgress;
        mBreakPoint = getLong(mContext, "breakPoint", 0L);
    }


    /**
     * 设置断点的位置
     *
     * @param breakPoint
     * @return
     */
    public DownloadManager setBreakPoint(Long breakPoint) {
        this.mBreakPoint = breakPoint;
        return this;
    }


    /**
     * 执行下载操作
     */
    public void download(String url, String path) {
        this.mFilePath = path;
        if (mIDownloadProgress != null) {
            mIDownloadProgress.start();
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("RANGE", "bytes=" + mBreakPoint + "-")
                .build();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();
        mDownloadCall = okHttpClient.newCall(request);
        mDownloadCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (mIDownloadProgress != null) {
                    mIDownloadProgress.error(e.getMessage());
                }
                Log.e(TAG, "onFailure: ------------- " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                ResponseBody body = response.body();
                if (body == null) {
                    return;
                }
                writeFile(body);
            }
        });
    }

    public void onDestrory() {
        mDownloadCall.cancel();
        saveBreakPoint();
    }

    public void cancle() {
        mDownloadCall.cancel();
        saveBreakPoint();
    }

    private void saveBreakPoint() {
        SharedPreferencedUtils.setLong(mContext, "breakPoint", mBreakPoint);
    }


    /**
     * 写入文件
     *
     * @param body
     */
    private void writeFile(ResponseBody body) {
        try {
            InputStream is = body.byteStream();
            if (mIsFirst) {
                //这里的Length为每次请求下来的大小,并不是文件本身的总大小,所以为了计算下载进度,这里只取第一次文件的总大小
                mTotleLength = (int) body.contentLength();
                mIsFirst = false;
            }
            File file = new File(mFilePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rwd");
            long m = randomAccessFile.getFilePointer();
            randomAccessFile.seek(mBreakPoint);

            int len = 0;
            byte[] buffer = new byte[2048];

            while (-1 != (len = is.read(buffer))) {
                randomAccessFile.write(buffer, 0, len);
                mBreakPoint += len;//记录断点
                int pro = (int) ((mBreakPoint * 100) / mTotleLength);
                mIDownloadProgress.progress(mTotleLength, mBreakPoint);
                Log.e(TAG, "progress: ------------ t:" + mTotleLength + " --- c:" + mBreakPoint + " --- p:" + pro);
            }
            randomAccessFile.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
private final String URL = "";
    private void test() {
        try {
            final File file = new File(mFilePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            //定义一个随机读写类
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rwd");
            final long filePointer = randomAccessFile.getFilePointer();
            Request request = new Request.Builder()
                    .url(URL)
                    .addHeader("RANGE", "bytes=" + filePointer + "-")
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
                public void onResponse(Call call, Response response) {
                    ResponseBody body = response.body();
                    InputStream is = null;
                    try {
                        is = body.byteStream();
                        long contentLength = SharedPreferencedUtils.getLong(mContext, "contentLength", 0L);
                        //这里的Length为每次请求下来的大小,并不是文件本身的总大小,所以为了计算下载进度,这里只取第一次文件的总大小
                        if (contentLength <= 0L) {
                            contentLength = (int) body.contentLength();
                            //存一下文件的总大小
                            SharedPreferencedUtils.setLong(mContext, "contentLength", contentLength);
                        }
                        //将文件记录指针移动到上一次写入文件的位置
                        randomAccessFile.seek(filePointer);

                        byte[] buffer = new byte[2048];
                        long currentBytes = 0L;
                        int len = 0;
                        while (-1 != (len = is.read(buffer))) {
                            randomAccessFile.write(buffer, 0, len);
                            currentBytes += len;//每次写入的大小累加
                            int progress = (int) ((currentBytes * 100) / contentLength);//转换得到当前下载的进度%
                            Log.e(TAG, "onResponse: --------- 下载进度:" + progress + "%");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (is != null) {
                                is.close();
                            }
                            if (randomAccessFile != null) {
                                randomAccessFile.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
