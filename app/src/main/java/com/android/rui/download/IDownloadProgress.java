package com.android.rui.download;

/**
 * Created by RuiJingLin
 * on 2017/10/27.
 * 下载进度
 */

public interface IDownloadProgress {

    void start();

    void progress(long totle, long currentBytesRead);

    void done();

    void error(String errorMsg);

    void cancle();

}
