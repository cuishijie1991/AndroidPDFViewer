# PDFViewer

## 一个简单的Android Pdf viewer,借助本地的js解析pdf，通过WebView展示.
 android 原生不支持加载pdf。有时应用需要支持一下pdf功能，大概有以下几种方案：
 
 
 |方案|优点|缺点|
 |:-|:-|:-|
 |Server将pdf直接转换成html返回给Client|Client不需要额外支持|不支持本地pdf，Server压力|
 |使用第三方App打开|Client简单，第三方App对pdf支持好|体验差，如果没有安装pdf app，尴尬|
 |利用在线pdf解析API在线解析|Client简单|体验极差，API响应速度不可控|
 |第三方pdf解析库或者SDK，如barteksc/AndroidPdfViewer|Client体验好|体积大，App增大3M左右|
 |将pdf转为图片再打开|Android有API支持|体验不好，容易OOM|
 |用本地js解析，通过webview打开|体验OK,体积增量小0.5M左右，支持本地和网络|体验并不如原生，有跨域问题|
 
不同的需求，选择方案有所不同。
- 如果是专业pdf阅读app，或者pdf阅读场景多，建议选择pdf原生解析库或者sdk，支持更好，功能更全。
- 如果pdf场景比较少，引入一个3M多的库很不合适。建议Server端进行处理或者用本地js解析的方案。

这个库，就是一个js解析的方案。参考了[PDFWebSite](https://github.com/qiujayen/PDFWebSite)这个库，在其基础上进行了修复和封装。支持手机本地存储，assert和server端pdf的解析读取。
读取server端pdf时，如果pdf server端设置不支持跨域，无法直接解析，会启动线程下载到本地，通过打开本地pdf方式加载。


## 使用
onCreate
```
 pdfViewer = new PdfViewer(webView);
 pdfViewer.loadPdf("pdf/packt-gradle-for-android.pdf");
```

onDestroy
```
  @Override
    protected void onDestroy() {
        super.onDestroy();
        pdfViewer.destroy();
    }
```    