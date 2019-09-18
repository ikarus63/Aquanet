package cz.pallasoftware.aquanet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebViewActivity extends Activity {

    //WebView example variables

    private static final String TAG = WebViewActivity.class.getSimpleName();
    private final static int FCR = 1;
    WebView webView;
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    //select whether you want to upload multiple files (set 'true' for yes)
    private boolean multiple_files = false;




    //My variables
    String token;
    String lastUrl;

    static boolean noNet = false;

    static Activity activity;

    //pomocná proměnná
    /*
    -1: standard
    0: notifikace => pustit URL
    1: již puštěno
     */
    int notifURL = 0;

    int index = 0;
    /*
    0: hlavní stránka
    1: další stránka
     */

    String defaultURL = "https://m.aquanet.cz/";

    public static void cancelWebView() {
        noNet();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web);

        activity = this;

        Intent intent = getIntent();

        String url = intent.getStringExtra("url");

        if(!url.equals("empty")){
            lastUrl = url;
            notifURL = 1;
            Log.v("echolofhdfjdsjkf", "WA: " + url);
        }else{
            Log.v("echolofhdfjdsjkf", "WA: EMPTY");
        }

        checkToken();
    }

    public void onResume() {
        super.onResume();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    ProgressDialog dialog;

    private void checkToken() {
        token = PreferenceManager.getDefaultSharedPreferences(this).getString("token", null);
        if (token == null) {
            if (dialog == null)
                dialog = ProgressDialog.show(this, "AQUANET", "Initializing unique app ID...", true);
            initializeToken();
            //opakovat pokus po sekundě
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkToken();
                }
            }, 1000);
        } else {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            Log.v("TOKEN", token);
            loadWeb();
        }
    }

    private void initializeToken() {
        try {
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                            }
                        }
                    });
        } catch (Exception e) {
        }
    }

    private boolean isAttachment(String url) {
        if (url.toLowerCase().trim().endsWith(".pdf") || url.toLowerCase().trim().endsWith(".doc") || url.toLowerCase().trim().endsWith(".docx")
                || url.toLowerCase().trim().endsWith(".xls") || url.toLowerCase().trim().endsWith(".xlsx") || url.toLowerCase().trim().endsWith(".ppt") ||
                url.toLowerCase().trim().endsWith(".pptx") || url.toLowerCase().trim().endsWith(".pps") || url.toLowerCase().trim().endsWith(".ppsx") ||
                url.toLowerCase().trim().endsWith(".txt")) {
            //je to příloha
            return true;
        } else {
            //není to příloha
            return false;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWeb() {
        noNet = false;
        setContentView(R.layout.web);

        webView = (WebView) findViewById(R.id.web);
        assert webView != null;
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);

        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(0);
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }


        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //Toast.makeText(WebViewActivity.this, "SHOULD OVERRIDE", Toast.LENGTH_LONG).show();
                if ((url.startsWith("https://m.aquanet.cz/") || url.startsWith("https://www.aquanet.cz/")) && !isAttachment(url)) {
                    lastUrl = url;
                    if (url.toLowerCase().trim().equals(defaultURL.toLowerCase().trim())) {
                        //je to výchozí stránka
                        index = 0;
                    } else {
                        //je to jiná stránka než výchozí
                        index = 1;
                    }
                    return false;
                } else {
                    webView.stopLoading();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    if (notifURL == 1) {
                        notifURL = 0;
                        //WebViewActivity.this.finish();
                    }
                    return true;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //Toast.makeText(WebViewActivity.this, "PAGE STARTED", Toast.LENGTH_LONG).show();
                if (url.toLowerCase().trim().equals(defaultURL.toLowerCase().trim())) {
                    //je to výchozí stránka
                    index = 0;
                } else {
                    //je to jiná stránka než výchozí
                    index = 1;
                }
                checkNetwork();
                if (notifURL == 1) {
                    notifURL = 0;
                    if ((url.startsWith("https://m.aquanet.cz") || url.startsWith("https://www.aquanet.cz")) && !isAttachment(url)) {
                        lastUrl = url;
                    } else {
                        webView.loadUrl(defaultURL);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                noNet();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //nic
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            /*
             * openFileChooser is not a public Android API and has never been part of the SDK.
             */
            //handling input[type="file"] requests for android API 16+
            @SuppressLint("ObsoleteSdkInt")
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                if (multiple_files && Build.VERSION.SDK_INT >= 18) {
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                startActivityForResult(Intent.createChooser(i, "Upload a file from..."), FCR);
            }

            //handling input[type="file"] requests for android API 21+
            @SuppressLint("InlinedApi")
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (file_permission()) {
                    String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

                    //checking for storage permission to write images for upload
                    if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(WebViewActivity.this, perms, FCR);

                        //checking for WRITE_EXTERNAL_STORAGE permission
                    } else if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, FCR);

                        //checking for CAMERA permissions
                    } else if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{Manifest.permission.CAMERA}, FCR);
                    }
                    if (mUMA != null) {
                        mUMA.onReceiveValue(null);
                    }
                    mUMA = filePathCallback;
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(WebViewActivity.this.getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                            takePictureIntent.putExtra("PhotoPath", mCM);
                        } catch (IOException ex) {
                            Log.e(TAG, "Image file creation failed", ex);
                        }
                        if (photoFile != null) {
                            mCM = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }
                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType("*/*");
                    if (multiple_files) {
                        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                    Intent[] intentArray;
                    if (takePictureIntent != null) {
                        intentArray = new Intent[]{takePictureIntent};
                    } else {
                        intentArray = new Intent[0];
                    }

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, FCR);
                    return true;
                } else {
                    return false;
                }
            }
        });

        webView.getSettings().setUserAgentString("Mozilla/5.0 (" + token + ":android) like Gecko");

        if (lastUrl == null) {
            lastUrl = defaultURL;
        }

        webView.loadUrl(lastUrl);
    }

    @Override
    public void onBackPressed() {
        if (noNet) {
            super.onBackPressed();
        } else {
            if (webView != null) {
                switch (index) {
                    case 0:
                        super.onBackPressed();
                        break;
                    case 1:
                        webView.loadUrl(defaultURL);
                        index = 0;
                        break;
                    default:
                        if (webView.canGoBack())
                            webView.goBack();
                        else
                            super.onBackPressed();
                        break;
                }
            } else {
                super.onBackPressed();
            }
        }
    }

    public static void noNet() {
        noNet = true;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(R.layout.nonet);
            }
        });
    }

    public void Refresh(View v) {
        if (DetectConnection.checkInternetConnection(WebViewActivity.this)) {
            loadWeb();
        }
    }

    private void checkNetwork() {
        boolean isNetwork = DetectConnection.checkInternetConnection(WebViewActivity.this);
        if (!isNetwork) {
            lastUrl = webView.getUrl();
            webView.stopLoading();
            noNet();
        }
    }









    //WebView example methods

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= 21) {
            Uri[] results = null;
            //checking if response is positive
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) {
                        return;
                    }
                    if (intent == null || intent.getData() == null) {
                        if (mCM != null) {
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        } else {
                            if (multiple_files) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else {
            if (requestCode == FCR) {
                if (null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    public boolean file_permission() {
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
            return false;
        } else {
            return true;
        }
    }

    //creating new image file here
    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
