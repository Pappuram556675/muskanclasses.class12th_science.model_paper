package muskanclasses.class12th_science.model_paper;

import android.Manifest;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.View.combineMeasuredStates;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    SwipeRefreshLayout swipeRefreshLayout;
    WebView webView;

    Toolbar toolbar;
    NavigationView navigationView;

    ActionBarDrawerToggle actionBarDrawerToggle;
    boolean doubleBackToExitPressedOnce = false;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private BottomNavigationView bottomNavigationView;

    String url = "false";
    String tab = "false";
    String video_tab = "false";
    String chrome = "false";
    String firebase_event = "false";

    String pdf = "false";

    String nca = "false";
     FirebaseAnalytics mFirebaseAnalytics;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        swipeRefreshLayout = findViewById(R.id.swiperefresh);


        MobileAds.initialize(this, initializationStatus -> {
            Log.d("suman", "Mobile Ads initialized");
        });

        if (!isInternetAvailable()) {

            webView.setVisibility(GONE);
            Intent intent = new Intent(getApplicationContext(), NoInternetActivity.class);
            startActivity(intent);
            finish();
        }

        //Ads Coad Here

        AdsManager.load_int_ads(MainActivity.this);
        AdsManager.loadRewardedAd(MainActivity.this);


        sidemenu();

        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        String name = sharedPreferences.getString("name", "null");
        String email = sharedPreferences.getString("email", "null");
        if (sharedPreferences.getString("email", "null").equals("null")){

            Intent intent = new Intent(getApplicationContext(), LoginnActivity.class);
            startActivity(intent);

        }













        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // एक basic event log करें
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "app_start");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);













// Disable long press (copy/paste block)
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);

// Enable essential settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.setHapticFeedbackEnabled(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

// Cookie set karo
        cookieManager.setCookie("https://muskanclasses.com", "muskanname=" + name + "; max-age=8640000"); // 100 day
        cookieManager.setCookie("https://muskanclasses.com", "muskanemail=" + email + "; max-age=8640000");

        cookieManager.flush();
// Add JS Interface


        webView.loadUrl("https://muskanclasses.com/application/12th-science/layout/model-paper/home.php?v=2");






        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                webView.reload();
            }
        });

        swipeRefreshLayout.setRefreshing(true);


        webView.setWebViewClient(new WebViewClient() {

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


        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {


                Log.d("suman", consoleMessage.message());
                handel_consolemessage(consoleMessage.message());

                return super.onConsoleMessage(consoleMessage);
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

            open_tab(message);

        }

        if (chrome.equals("true")){

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

            AdsManager.show_int_ads_with_callback(MainActivity.this);
        }

        if (message.equals("video_ads")){

            AdsManager.showRewardedAd(MainActivity.this);


        }

        if (message.equals("chat")){

            Intent intent = new Intent(getApplicationContext(), RealTimeChatActivity.class);
            startActivity(intent);

        }
    }

    private void open_video_tab(String message) {

        video_tab = "false";
        RewardedAd rewardedAd = AdsManager.rewardedAd;



        Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.baseline_arrow_back_24);


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
        builder.setToolbarColor(ContextCompat.getColor(MainActivity.this, R.color.app_color));
        builder.setCloseButtonIcon(bitmap); // your vector converted to bitmap
        builder.enableUrlBarHiding();
        //builder.setShowTitle(true);

        CustomTabsIntent customTabsIntent = builder.build();





        if (rewardedAd!=null){

            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    AdsManager.mInterstitialAd  = null;
                    AdsManager.load_int_ads(MainActivity.this);
                    customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
                    super.onAdDismissedFullScreenContent();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
                    super.onAdFailedToShowFullScreenContent(adError);
                }
            });


        } else {

            AdsManager.mInterstitialAd  = null;
            AdsManager.load_int_ads(MainActivity.this);
            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
        }
    }

    private void open_tab(String url) {

        Log.d("suman", "tab_func_call");

        tab = "false";
        InterstitialAd interstitialAd = AdsManager.mInterstitialAd;



        Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.baseline_arrow_back_24);


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
        builder.setToolbarColor(ContextCompat.getColor(MainActivity.this, R.color.app_color));
        builder.setCloseButtonIcon(bitmap); // your vector converted to bitmap
        builder.enableUrlBarHiding();
        //builder.setShowTitle(true);

        CustomTabsIntent customTabsIntent = builder.build();





        if (interstitialAd!=null){


            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {

                    AdsManager.mInterstitialAd  = null;
                    AdsManager.load_int_ads(MainActivity.this);
                    customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
                    super.onAdDismissedFullScreenContent();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
                    super.onAdFailedToShowFullScreenContent(adError);
                }
            });

            interstitialAd.show(MainActivity.this);
        } else {


            AdsManager.mInterstitialAd  = null;
            AdsManager.load_int_ads(MainActivity.this);
            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));

        }
    }


    private void sidemenu() {
        toolbar = findViewById(R.id.webtoolbar);
        setSupportActionBar(toolbar);
        navigationView = findViewById(R.id.nav_menu);
        navigationView.setItemIconTintList(null); // Keep this if you want to use custom colors for icons
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        //Toast.makeText(this, "Side menu initialized", Toast.LENGTH_SHORT).show(); // More descriptive toast

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Handle navigation view item clicks here.

                int id = item.getItemId(); // Get the ID of the clicked menu item

                // Using if-else if statements instead of switch
                if (id == R.id.nav_home) {

                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_home", bundle);
                    webView.loadUrl("https://muskanclasses.com/application/12th-science/layout/home.php");

                } else if (id == R.id.nav_chat) {

                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_chat", bundle);
                    Intent chatIntent = new Intent(MainActivity.this, RealTimeChatActivity.class);
                    startActivity(chatIntent);

                } else if (id == R.id.nav_ask_question) {

                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_ask_question", bundle);

                }

                if (id==R.id.nav_policy){



                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_privacy_policy", bundle);
                    open_tab("https://muskanclasses.com/privacy-policy.html");


                }
                if (id==R.id.nav_term){

                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_term_condition", bundle);
                    open_tab("https://muskanclasses.com/terms-and-conditions.html");
                }

                if (id==R.id.nav_whatsapp){

                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_whatsapp", bundle);
                    Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://muskanclasses.com/whatsapp"));
                    startActivity(intent1);
                }


                if (id==R.id.nav_telegram){

                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_telegram", bundle);
                    Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://muskanclasses.com/telegram"));
                    startActivity(intent1);
                }

                if (id==R.id.nav_share){


                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_share", bundle);
                    String appPackageName = getPackageName(); // gets your app's package name
                    String shareBody = "Check out this amazing app:\n\n" +
                            "https://play.google.com/store/apps/details?id=" + appPackageName;

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Muskan Classes App");
                    intent.putExtra(Intent.EXTRA_TEXT, shareBody);

                    startActivity(Intent.createChooser(intent, "Share via"));

                }

                if (id==R.id.nav_help){



                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_email", bundle);
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

                if (id==R.id.nav_rate){

                    Bundle bundle = new Bundle();
                    bundle.putString("package", getPackageName());

                    mFirebaseAnalytics.logEvent("side_rate", bundle);
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


                drawerLayout.closeDrawer(GravityCompat.START); // Close the navigation drawer after an item is selected
                return true; // Important: Return true to indicate the event has been handled
            }
        });
    }


    private void layouthide() {

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {

            drawerLayout.closeDrawer(GravityCompat.START);

        } else {




            if (webView.canGoBack()) {

                webView.goBack();

            } else {

                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed();
                    return;
                }

                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce = false;
                    }
                }, 1000);
            }
        }

    }

    @Override
    public void onBackPressed() {

        layouthide();

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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Update cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }






}