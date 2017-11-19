package com.csj.pdfviewer.viewer;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by shijiecui on 2017/10/30.
 * 使用pdf.js进行local和server pdf展示
 */

public class PdfViewer {
    public static final String TAG = "PDFView";
    private static final String PdfReaderPrefix = "file:///android_asset/pdf-viewer/index.html?pdf=";
    private static final int MSG_DOWNLOAD_START = 0;
    private static final int MSG_DOWNLOAD_SUCCEED = MSG_DOWNLOAD_START + 1;
    private static final int MSG_DOWNLOAD_FAILED = MSG_DOWNLOAD_SUCCEED + 1;
    //缓存下载的pdf文件路径，退出时清除
    private HashMap<String, String> cachedPdfFiles = new HashMap<>();
    private WebView mWebView;
    private IPdfViewerDownloadCallback mCallback;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DOWNLOAD_START:
                    if (mCallback != null) {
                        mCallback.onDownloadStart();
                    }
                    break;
                case MSG_DOWNLOAD_SUCCEED:
                    if (msg.obj instanceof String) {
                        String filePath = (String) msg.obj;
                        String url = PdfReaderPrefix + filePath;
                        mWebView.loadUrl(url);
                        if (mCallback != null) {
                            mCallback.onDownloadSucceed();
                        }
                    }
                    break;
                case MSG_DOWNLOAD_FAILED:
                    if (mCallback != null && msg.obj instanceof String) {
                        mCallback.onDownloadPageFailed((String) msg.obj);
                    }
                    break;
            }
        }
    };

    public PdfViewer(WebView webView) {
        mWebView = webView;
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
    }


    /**
     * @param file pdfFile
     */
    public void loadPdf(File file) {
        if (file != null && file.exists() && !file.isDirectory() && file.getName().toLowerCase().endsWith(".pdf")) {
            String url = PdfReaderPrefix + file.getAbsolutePath();
            mWebView.loadUrl(url);
        }
    }

    /**
     * @param assertFilePath em: "pdf/xxx.pdf"
     */
    public void loadPdf(String assertFilePath) {
        if (assertFilePath != null && assertFilePath.toLowerCase().endsWith(".pdf")) {
            String url = PdfReaderPrefix + "../" + assertFilePath;
            mWebView.loadUrl(url);
        }
    }

    /**
     * @param externalUrl               远程文件url em:"http://xxx/xxx.pdf"
     * @param isServerAllowsCrossOrigin 远程文件服务端是否允许跨域
     *                                  False 先将pdf文件下载到本地，当作本地文件加载
     * @param callback                  pdf文件下载回调
     */
    public void loadPdf(String externalUrl, boolean isServerAllowsCrossOrigin, @Nullable IPdfViewerDownloadCallback callback) {
        mCallback = callback;
        if (externalUrl == null || !(externalUrl.toLowerCase().startsWith("http") || externalUrl.toLowerCase().startsWith("ftp")) ||
                !externalUrl.toLowerCase().contains(".pdf")) {
            String error = "pdf file url must starts with \"http\" or \"ftp\" and  end with \"pdf\"";
            mHandler.obtainMessage(MSG_DOWNLOAD_FAILED, error).sendToTarget();
            Log.d(TAG, error);
            return;
        }
        if (!isServerAllowsCrossOrigin) {
            mHandler.obtainMessage(MSG_DOWNLOAD_START).sendToTarget();
            _downloadPdfAsLocal(externalUrl);
        } else {
            String url = PdfReaderPrefix + externalUrl;
            mWebView.loadUrl(url);
        }
    }

    public void destroy() {
        mCallback = null;
        mHandler.removeCallbacksAndMessages(null);
        if (!cachedPdfFiles.isEmpty()) {
            for (String path : cachedPdfFiles.values()) {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        cachedPdfFiles.clear();
    }

    private void _downloadPdfAsLocal(final String urlStr) {
        String cachedFilePath = cachedPdfFiles.get(urlStr);
        if (cachedFilePath != null) {
            File file = new File(cachedFilePath);
            if (file.exists()) {
                mHandler.obtainMessage(MSG_DOWNLOAD_SUCCEED, cachedFilePath).sendToTarget();
                return;
            }
        }
        final String path = Environment.getExternalStorageDirectory() + "/Download/";
        final String filePath = String.format("%s%d.pdf", path, System.currentTimeMillis());
        final File file = new File(filePath);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);
                    connection.setConnectTimeout(5 * 1000);
                    connection.setReadTimeout(5 * 1000);
                    //实现连接
                    connection.connect();

                    if (connection.getResponseCode() == 200) {
                        InputStream is = connection.getInputStream();
                        //以下为下载操作
                        byte[] arr = new byte[1];
                        ByteArrayOutputStream aos = new ByteArrayOutputStream();
                        BufferedOutputStream bos = new BufferedOutputStream(aos);
                        int n = is.read(arr);
                        while (n > 0) {
                            bos.write(arr);
                            n = is.read(arr);
                        }
                        bos.close();
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(aos.toByteArray());
                        fos.close();

                        if (file.exists()) {
                            mHandler.obtainMessage(MSG_DOWNLOAD_SUCCEED, filePath).sendToTarget();
                            cachedPdfFiles.put(urlStr, filePath);
                        } else {
                            mHandler.obtainMessage(MSG_DOWNLOAD_FAILED, "write pdf file to sd card failed!").sendToTarget();
                        }
                    } else {
                        mHandler.obtainMessage(MSG_DOWNLOAD_FAILED, "response " + connection.getResponseCode()).sendToTarget();
                    }
                } catch (Exception e) {
                    mHandler.obtainMessage(MSG_DOWNLOAD_FAILED, e.getMessage()).sendToTarget();
                    e.printStackTrace();
                } finally {
                    //关闭网络连接
                    connection.disconnect();
                }
            }
        }).start();
    }

    public interface IPdfViewerDownloadCallback {
        void onDownloadStart();

        void onDownloadSucceed();

        void onDownloadPageFailed(String error);
    }

}
