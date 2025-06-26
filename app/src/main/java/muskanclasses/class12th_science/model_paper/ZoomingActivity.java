package muskanclasses.class12th_science.model_paper;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.firebase.analytics.FirebaseAnalytics;

public class ZoomingActivity extends AppCompatActivity {

    WebView webView;
    SwipeRefreshLayout swipeRefreshLayout;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private ValueCallback<Uri[]> filePathCallback;


    String url = "false";
    String tab = "false";
    String video_tab = "false";
    String chrome = "false";
    String firebase_event = "false";

    String pdf = "false";

    String nca = "false";
    FirebaseAnalytics mFirebaseAnalytics;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.app_color));

        setContentView(R.layout.activity_zooming);





        toolbar = findViewById(R.id.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        LinearLayout mainLayout = findViewById(R.id.main); // R.id.main को XML में set करें

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, v.getPaddingTop(), systemBars.right, systemBars.bottom);
            return insets;
        });





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


        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // एक basic event log करें
        Bundle bundle = new Bundle();
        bundle.putString("package", getPackageName());

        mFirebaseAnalytics.logEvent("Zooming_activity_open", bundle);



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

        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        String name = sharedPreferences.getString("name", "null");
        String email = sharedPreferences.getString("email", "null");

        if (sharedPreferences.getString("email", "null").equals("null")){

            Intent intent = new Intent(getApplicationContext(), LoginnActivity.class);
            startActivity(intent);

        }


        if (!isInternetAvailable()) {

            webView.setVisibility(GONE);
            Intent intent = new Intent(getApplicationContext(), NoInternetActivity.class);
            startActivity(intent);
            finish();
        }
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

                 Log.d("suman", consoleMessage.message());
                 handel_consolemessage(consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        });






    }

    private void handel_consolemessage(String message) {

        if (url.equals("true")){




            Intent intent = new Intent(getApplicationContext(), ListActivity.class);
            intent.putExtra("url", message);
            url = "false";
            startActivity(intent);
        }

        if (tab.equals("true")){

            Bundle bundle = new Bundle();
            bundle.putString("package_name", getPackageName());
            mFirebaseAnalytics.logEvent("open_tab_ads", bundle);
            open_tab(message);

        }

        if (pdf.equals("true")){



            Intent intent = new Intent(getApplicationContext(), ZoomingActivity.class);
            intent.putExtra("url", message);
            pdf = "false";
            startActivity(intent);
        }

        if (nca.equals("true")){



            Intent intent = new Intent(getApplicationContext(), PageLoadWithUserDataActivity.class);
            intent.putExtra("url", message);
            nca = "false";
            startActivity(intent);
        }

        if (chrome.equals("true")){

            Bundle bundle = new Bundle();
            bundle.putString("package_name", getPackageName());
            mFirebaseAnalytics.logEvent("open_chrome", bundle);
            Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse(message));
            startActivity(intent1);

        }

        if (firebase_event.equals("true")){



            Bundle bundle = new Bundle();
            bundle.putString("package", getPackageName());
            firebase_event = "false";

            mFirebaseAnalytics.logEvent(message, bundle);

        }

        if (video_tab.equals("true")){

            Bundle bundle = new Bundle();
            bundle.putString("package_name", getPackageName());
            mFirebaseAnalytics.logEvent("open_tab_video_ads", bundle);
            open_video_tab(message);
        }

        if (message.equals("url")){
            url = "true";
        }

        if (message.equals("tab")){

            tab = "true";

        }

        if (message.equals("video_tab")){

            video_tab = "true";

        }

        if (message.equals("chrome")){

            chrome = "true";
        }



        if (message.equals("firebase_event")){

            firebase_event = "true";
        }

        if (message.equals("pdf")){

            pdf = "true";
        }

        if (message.equals("NCA")){

            nca = "true";
        }

        if (message.equals("share")){



            Bundle bundle = new Bundle();
            bundle.putString("package", getPackageName());

            mFirebaseAnalytics.logEvent("page_share", bundle);
            String appPackageName = getPackageName(); // gets your app's package name
            String shareBody = "Check out this amazing app:\n\n" +
                    "https://play.google.com/store/apps/details?id=" + appPackageName;

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Muskan Classes App");
            intent.putExtra(Intent.EXTRA_TEXT, shareBody);

            startActivity(Intent.createChooser(intent, "Share via"));

        }

        if (message.equals("rate")){

            Bundle bundle = new Bundle();
            bundle.putString("package", getPackageName());

            mFirebaseAnalytics.logEvent("page_rate", bundle);
            Uri uri = Uri.parse("market://details?id=" + getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                // Fallback if Play Store is not installed
                Uri webUri = Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName());
                startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            }


        }

        if (message.equals("email")){

            Bundle bundle = new Bundle();
            bundle.putString("package", getPackageName());

            mFirebaseAnalytics.logEvent("page_email", bundle);
            String recipient = "muskanclassesteam@gmail.com";
            String subject = "Report Application Error";
            String body = "Hello,\n\nThis is a Report Application Error email .";

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + recipient));
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);

            try {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } catch (android.content.ActivityNotFoundException ex) {
                // No email app installed
                ex.printStackTrace();
            }

        }

        if (message.equals("whatsapp")){

            Bundle bundle = new Bundle();
            bundle.putString("package", getPackageName());

            mFirebaseAnalytics.logEvent("page_whatsapp", bundle);
            Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://muskanclasses.com/whatsapp"));
            startActivity(intent1);
        }

        if (message.equals("telegram")){

            Bundle bundle = new Bundle();
            bundle.putString("package", getPackageName());

            mFirebaseAnalytics.logEvent("page_telegram", bundle);
            Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://muskanclasses.com/telegram"));
            startActivity(intent1);
        }


        if (message.equals("site")){

            Bundle bundle = new Bundle();
            bundle.putString("package", getPackageName());

            mFirebaseAnalytics.logEvent("page_site", bundle);
            Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://muskanclasses.com/"));
            startActivity(intent1);
        }

        if (message.equals("ads")){

            AdsManager.show_int_ads_with_callback(ZoomingActivity.this);
        }

        if (message.equals("video_ads")){

            AdsManager.showRewardedAd(ZoomingActivity.this);


        }

        if (message.equals("chat")){

            Intent intent = new Intent(getApplicationContext(), RealTimeChatActivity.class);
            startActivity(intent);

        }
    }

    private void open_video_tab(String message) {

        video_tab = "false";
        RewardedAd rewardedAd = AdsManager.rewardedAd;



        Drawable drawable = ContextCompat.getDrawable(ZoomingActivity.this, R.drawable.baseline_arrow_back_24);


        drawable = DrawableCompat.wrap(drawable).mutate();

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        // Now set the close button icon
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(ZoomingActivity.this, R.color.app_color));
        builder.setCloseButtonIcon(bitmap); // your vector converted to bitmap
        builder.enableUrlBarHiding();
        //builder.setShowTitle(true);

        CustomTabsIntent customTabsIntent = builder.build();





        if (rewardedAd!=null){

            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    AdsManager.mInterstitialAd  = null;
                    AdsManager.load_int_ads(ZoomingActivity.this);
                    customTabsIntent.launchUrl(ZoomingActivity.this, Uri.parse(url));
                    super.onAdDismissedFullScreenContent();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    customTabsIntent.launchUrl(ZoomingActivity.this, Uri.parse(url));
                    super.onAdFailedToShowFullScreenContent(adError);
                }
            });

            rewardedAd.show(ZoomingActivity.this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {


                }
            });


        } else {



            AdsManager.load_int_ads(ZoomingActivity.this);
            customTabsIntent.launchUrl(ZoomingActivity.this, Uri.parse(url));
        }
    }

    private void open_tab(String url) {

        tab = "false";
        InterstitialAd interstitialAd = AdsManager.mInterstitialAd;



        Drawable drawable = ContextCompat.getDrawable(ZoomingActivity.this, R.drawable.baseline_arrow_back_24);


        drawable = DrawableCompat.wrap(drawable).mutate();

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        // Now set the close button icon
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(ZoomingActivity.this, R.color.app_color));
        builder.setCloseButtonIcon(bitmap); // your vector converted to bitmap
        builder.enableUrlBarHiding();
        //builder.setShowTitle(true);

        CustomTabsIntent customTabsIntent = builder.build();





        if (interstitialAd!=null){


            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {

                    customTabsIntent.launchUrl(ZoomingActivity.this, Uri.parse(url));
                    AdsManager.mInterstitialAd  = null;
                    AdsManager.load_int_ads(ZoomingActivity.this);
                    super.onAdDismissedFullScreenContent();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    customTabsIntent.launchUrl(ZoomingActivity.this, Uri.parse(url));
                    super.onAdFailedToShowFullScreenContent(adError);
                }
            });

            interstitialAd.show(ZoomingActivity.this);
        } else {

            AdsManager.mInterstitialAd  = null;
            AdsManager.load_int_ads(ZoomingActivity.this);
            customTabsIntent.launchUrl(ZoomingActivity.this, Uri.parse(url));
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


    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        }
        return false;
    }


}


