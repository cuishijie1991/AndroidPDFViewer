package com.csj.pdfviewer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

import com.csj.pdfviewer.viewer.PdfViewer;

public class MainActivity extends AppCompatActivity {

    PdfViewer pdfViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView = (WebView) findViewById(R.id.webView);
        pdfViewer = new PdfViewer(webView);
        pdfViewer.loadPdf("pdf/packt-gradle-for-android.pdf");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pdfViewer.destroy();
    }
}
