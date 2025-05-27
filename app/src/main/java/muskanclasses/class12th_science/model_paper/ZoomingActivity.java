package muskanclasses.class12th_science.model_paper;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ZoomingActivity extends AppCompatActivity {

    WebView webView;
    SwipeRefreshLayout swipeRefreshLayout;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private ValueCallback<Uri[]> filePathCallback;


    String url = "false";
    String loginWithGoogle = "false";
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_zooming);




        // Back button enable करें
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            // यह back arrow को दिखाने के लिए जरूरी है
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        });





        swipeRefreshLayout = findViewById(R.id.swiperefresh);

        swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });


        webView = findViewById(R.id.webview);
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);

// Enable essential settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.setHapticFeedbackEnabled(false);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

// Enable cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true); // For API < 21
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        webView.addJavascriptInterface(new web_function(ZoomingActivity.this), "android");
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        String name = sharedPreferences.getString("name", "null");
        String email = sharedPreferences.getString("email", "null");

        if (sharedPreferences.getString("email", "null").equals("null")){

            Intent intent = new Intent(getApplicationContext(), LoginnActivity.class);
            startActivity(intent);

        }


        //String muskan = getIntent().getStringExtra("url"+"?name="+name+"&email="+email);
        //Toast.makeText(this, muskan, Toast.LENGTH_LONG).show();
        webView.loadUrl(getIntent().getStringExtra("url"));
        cookieManager.flush();


        webView.setWebViewClient(new WebViewClient(){

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
                //Toast.makeText(MainActivity.this, url, Toast.LENGTH_LONG).show();
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                webView.setVisibility(VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                webView.setVisibility(GONE);
                Intent intent = new Intent(getApplicationContext(), NoInternetActivity.class);
                startActivity(intent);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });


        webView.setWebChromeClient(new WebChromeClient(){


            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {




                if (consoleMessage.message().equals("loginWithGoogle")){

                    Intent intent = new Intent(getApplicationContext(), LoginnActivity.class);


                    loginWithGoogle = "false";
                    startActivity(intent);
                    finish();


                }







                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        });






    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void receiveMessage(String msg) {
            // Show message from web in Toast
            Toast.makeText(ZoomingActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()){

            webView.goBack();
        } else {

            super.onBackPressed();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }


}


