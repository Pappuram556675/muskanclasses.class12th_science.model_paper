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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
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
    String loginWithGoogle = "false";
     FirebaseAnalytics mFirebaseAnalytics;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);




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











        webView = findViewById(R.id.webview);
        swipeRefreshLayout = findViewById(R.id.swiperefresh);

// Disable long press (copy/paste block)
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);

// Enable essential settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.setHapticFeedbackEnabled(false);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

// Cookie set karo
        cookieManager.setCookie("https://muskanclasses.com", "muskanname=" + name + "; max-age=8640000"); // 100 day
        cookieManager.setCookie("https://muskanclasses.com", "muskanemail=" + email + "; max-age=8640000");

        cookieManager.flush();
// Add JS Interface
        webView.addJavascriptInterface(new web_function(MainActivity.this), "android");

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

                Toast.makeText(MainActivity.this, consoleMessage.message(), Toast.LENGTH_SHORT).show();



                if (consoleMessage.message().equals("loginWithGoogle")){

                    Intent intent = new Intent(getApplicationContext(), LoginnActivity.class);


                    loginWithGoogle = "false";
                    startActivity(intent);
                    finish();


                }




                return super.onConsoleMessage(consoleMessage);
            }

        });













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

                } else if (id == R.id.nav_chat) {

                    Intent chatIntent = new Intent(MainActivity.this, RealTimeChatActivity.class);
                    startActivity(chatIntent);

                } else if (id == R.id.nav_ask_question) {


                }

                if (id==R.id.nav_policy){



                    open_tab("https://muskanclasses.com/privacy-policy.html");


                }
                if (id==R.id.nav_term){

                    
                    open_tab("https://muskanclasses.com/terms-and-conditions.html");
                }

                if (id==R.id.nav_whatsapp){

                    Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://muskanclasses.com/whatsapp"));
                    startActivity(intent1);
                }


                if (id==R.id.nav_telegram){

                    Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://muskanclasses.com/telegram"));
                    startActivity(intent1);
                }

                if (id==R.id.nav_share){


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

    private void open_tab(String url) {

        Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.baseline_arrow_back_24);

        if (drawable != null) {
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

            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));

        }
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
}