package com.nauk.coinfolio.Activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nauk.coinfolio.DataManagers.BalanceManager;
import com.nauk.coinfolio.DataManagers.CurrencyData.Currency;
import com.nauk.coinfolio.LayoutManagers.HomeLayoutGenerator;
import com.nauk.coinfolio.DataManagers.PreferencesManager;
import com.nauk.coinfolio.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

//Use WilliamChart for charts https://github.com/diogobernardino/WilliamChart

//Auto refresh with predefined intervals
//Adding manually currencies (date, purchased price)
//Multiple portfolio (exchanges & custom)
//Add currency details (market cap, 1h, 3h, 1d, 3d, 1w, 1m, 3m, 1y)
//Add roadmap to buy a coin
//Add reddit link ?
//

public class HomeActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;
    private HomeLayoutGenerator layoutGenerator;
    private BalanceManager balanceManager;

    private int coinCounter;
    private int iconCounter;
    private long lastTimestamp;
    private boolean detailsChecker;
    private boolean isDetailed;

    private CollapsingToolbarLayout toolbarLayout;
    private SwipeRefreshLayout refreshLayout;
    private LinearLayout currencyLayout;
    private TextView toolbarSubtitle;
    private Dialog loadingDialog;
    private Handler handler;
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**Interface setup**/

        //Setup main interface
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_currency_summary);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        generateSplash();

        //Objects initializatoin
        preferencesManager = new PreferencesManager(this);
        layoutGenerator = new HomeLayoutGenerator(this);
        balanceManager = new BalanceManager(this);
        handler = new Handler();
        updateRunnable = new  Runnable() {
            @Override
            public void run() {
                if (refreshLayout.isRefreshing())
                {
                    refreshLayout.setRefreshing(false);

                    showErrorSnackbar();
                }

                if (loadingDialog.isShowing())
                {
                    loadingDialog.dismiss();

                    showErrorSnackbar();
                }
            }
        };

        isDetailed = preferencesManager.getDetailOption();

        //Layouts setup
        refreshLayout = findViewById(R.id.swiperefresh);
        toolbarLayout = findViewById(R.id.toolbar_layout);
        toolbarSubtitle = findViewById(R.id.toolbarSubtitle);
        currencyLayout = findViewById(R.id.currencyListLayout);

        ImageButton addCurrencyButton = findViewById(R.id.addCurrencyButton);
        ImageButton detailsButton = findViewById(R.id.switch_button);
        ImageButton settingsButton = findViewById(R.id.settings_button);

        toolbarLayout.setExpandedTitleGravity(Gravity.CENTER);
        toolbarLayout.setCollapsedTitleGravity(Gravity.CENTER);
        toolbarLayout.setForegroundGravity(Gravity.CENTER);
        toolbarLayout.setTitle("US$0.00");

        toolbarSubtitle.setText("US$0.00");

        //Events setup
        detailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchView();
            }
        });

        settingsButton.setBackground(this.getResources().getDrawable(R.drawable.ic_settings_black_24dp));
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingIntent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(settingIntent);
            }
        });

        refreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        updateAll(false);
                    }
                }
        );

        addCurrencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addIntent = new Intent(HomeActivity.this, CurrencySelectionActivity.class);

                String[] symbolList = new String[balanceManager.getCurrenciesSymbol().size()];
                symbolList = balanceManager.getCurrenciesSymbol().toArray(symbolList);
                String[] nameList = new String[balanceManager.getCurrenciesName().size()];
                nameList = balanceManager.getCurrenciesName().toArray(nameList);

                addIntent.putExtra("currencyListSymbols", symbolList);
                addIntent.putExtra("currencyListNames", nameList);

                startActivity(addIntent);
            }
        });

        updateViewButtonIcon();

        lastTimestamp = 0;
    }

    private void showErrorSnackbar()
    {
        Snackbar.make(findViewById(R.id.currencyListLayout), "Error while updating data", Snackbar.LENGTH_LONG)
                .setAction("Update", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        updateAll(true);
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();

        updateAll(intent.getBooleanExtra("update", false));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_currency_summary, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            /*case R.id.action_settings:
                Log.d(this.getResources().getString(R.string.debug), "Setting button toggled");
                break;*/
        }

        return super.onOptionsItemSelected(item);
    }

    private void switchView()
    {
        if(isDetailed)
        {
            isDetailed = false;

            adaptView();
        }
        else
        {
            isDetailed = true;

            adaptView();
        }
    }

    private void adaptView()
    {
        if(isDetailed)
        {
            currencyLayout.removeAllViews();

            for(int i = 0; i < balanceManager.getTotalBalance().size(); i++)
            {
                final Currency currency = balanceManager.getTotalBalance().get(i);

                if(!currency.getSymbol().equals("USD") && ((currency.getBalance() * currency.getValue()) > 0.001 || currency.getHistoryMinutes() == null))
                {
                    //currencyLayout.addView(layoutGenerator.getInfoLayout(currency));
                    currencyLayout.addView(layoutGenerator.getInfoLayout(currency, true));
                }
            }

            //currencyLayout.addView(layoutGenerator.getInfoLayout(balanceManager.getTotalBalance().get(0), true));
        }
        else
        {
            /*for(int i = 0; i < currencyLayout.getChildCount(); i++)
            {
                currencyLayout.getChildAt(i).findViewWithTag("chart_layout").setVisibility(View.GONE);
                currencyLayout.getChildAt(i).findViewWithTag("separator_layout").setVisibility(View.GONE);
            }*/
        }

        updateViewButtonIcon();
    }



    private void updateAll(boolean mustUpdate)
    {
        if(System.currentTimeMillis()/1000 - lastTimestamp > 60 || mustUpdate)
        {
            lastTimestamp = System.currentTimeMillis() / 1000;
            balanceManager.updateExchangeKeys();
            refreshLayout.setRefreshing(true);

            resetCounters();
            DataUpdater updater = new DataUpdater();
            updater.execute();

            handler.postDelayed(updateRunnable, 10000);
        }
        else
        {
            if(refreshLayout.isRefreshing())
            {
                refreshLayout.setRefreshing(false);
            }
        }
    }

    private void resetCounters()
    {
        coinCounter = 0;
        iconCounter = 0;
        detailsChecker = false;
    }

    private void getBitmapFromURL(String src, IconCallBack callBack) {
        Bitmap result;

        try {
            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            result = BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            result = null;
        }

        callBack.onSuccess(result);
    }

    private void countIcons()
    {
        iconCounter++;

        if(balanceManager.getTotalBalance() != null)
        {
            if(iconCounter == balanceManager.getTotalBalance().size() - 1)
            {
                if(coinCounter == balanceManager.getTotalBalance().size() - 1 && detailsChecker)
                {
                    UiHeavyLoadCalculator uiHeavyLoadCalculator = new UiHeavyLoadCalculator();
                    uiHeavyLoadCalculator.execute();
                }

                if(balanceManager.getTotalBalance().size() == 0)
                {
                    updateNoBalance();
                }
            }
            /*else
            {
                if(balanceManager.getTotalBalance().size() == 0)
                {
                    currencyLayout.removeAllViews();

                    refreshLayout.setRefreshing(false);

                    if(loadingDialog.isShowing())
                    {
                        loadingDialog.dismiss();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toolbarLayout.setTitle("US$0.00");

                            toolbarSubtitle.setText("US$0.00");

                            toolbarSubtitle.setTextColor(-1275068417);
                        }
                    });
                }
            }*/
        }
    }

    private void updateNoBalance()
    {
        refreshLayout.setRefreshing(false);

        currencyLayout.removeAllViews();

        if(loadingDialog.isShowing())
        {
            loadingDialog.dismiss();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toolbarLayout.setTitle("US$0.00");

                toolbarSubtitle.setText("US$0.00");

                toolbarSubtitle.setTextColor(-1275068417);
            }
        });
    }

    private void countCoins(boolean isCoin, boolean isDetails)
    {
        if(isCoin)
        {
            coinCounter++;
        }

        if(isDetails)
        {
            detailsChecker = true;
        }

        if(balanceManager.getTotalBalance() != null)
        {
            if(coinCounter == balanceManager.getTotalBalance().size()-1)
            {
                for (int i = 0; i < balanceManager.getTotalBalance().size(); i++)
                {
                    final Currency localCurrency = balanceManager.getTotalBalance().get(i);

                    if(balanceManager.getIconUrl(localCurrency.getSymbol()) != null)
                    {
                        getBitmapFromURL(balanceManager.getIconUrl(localCurrency.getSymbol()), new IconCallBack() {
                            @Override
                            public void onSuccess(Bitmap bitmapIcon) {
                                localCurrency.setIcon(bitmapIcon);
                                countIcons();
                            }
                        });
                    }
                }
            }
            else
            {
                if(balanceManager.getTotalBalance().size() == 0)
                {
                    countIcons();
                }
            }
        }
    }

    private void updateViewButtonIcon()
    {
        ImageButton imgButton = findViewById(R.id.switch_button);

        if(isDetailed)
        {
            imgButton.setBackground(this.getResources().getDrawable(R.drawable.ic_unfold_less_black_24dp));
            preferencesManager.setDetailOption(true);
        }
        else
        {
            imgButton.setBackground(this.getResources().getDrawable(R.drawable.ic_details_black_24dp));
            preferencesManager.setDetailOption(false);
        }
    }

    private void generateSplash()
    {
        LinearLayout loadingLayout = new LinearLayout(this);
        loadingLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        loadingLayout.setGravity(Gravity.CENTER);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);

        loadingDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        TextView txtView = new TextView(this);
        txtView.setText("Loading data...");
        txtView.setTextSize(20);
        txtView.setGravity(Gravity.CENTER);
        txtView.setTextColor(this.getResources().getColor(R.color.cardview_light_background));

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);

        loadingLayout.setBackgroundColor(this.getResources().getColor(R.color.colorPrimaryDark));
        loadingLayout.addView(txtView);
        loadingLayout.addView(progressBar);

        loadingDialog.setContentView(loadingLayout);
        loadingDialog.show();
    }

    private class UiHeavyLoadCalculator extends AsyncTask<Void, Integer, Void>
    {

        private float totalValue;
        private float totalFluctuation;
        private float totalFluctuationPercentage;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            totalValue = 0;
            totalFluctuation = 0;
            totalFluctuationPercentage = 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            final List<View> cardList = new ArrayList<>();

            Looper.prepare();

            balanceManager.sortCoins();

            for(int i = 0; i < balanceManager.getTotalBalance().size(); i++)
            {
                final Currency localCurrency = balanceManager.getTotalBalance().get(i);

                if(localCurrency.getIcon() != null)
                {
                    Palette.Builder builder = Palette.from(localCurrency.getIcon());

                    localCurrency.setChartColor(builder.generate().getDominantColor(0));
                }
                else
                {
                    localCurrency.setChartColor(12369084);
                }

                if(!localCurrency.getSymbol().equals("USD") && (localCurrency.getBalance() * localCurrency.getValue()) > 0.001)
                {
                    localCurrency.setName(balanceManager.getCurrencyName(localCurrency.getSymbol()));
                    localCurrency.setId(balanceManager.getCurrencyId(localCurrency.getSymbol()));
                    totalValue += localCurrency.getValue() * localCurrency.getBalance();
                    totalFluctuation += (localCurrency.getValue() * localCurrency.getBalance()) * (localCurrency.getDayFluctuationPercentage() / 100);

                    cardList.add(layoutGenerator.getInfoLayout(localCurrency, true));
                }

                if(!localCurrency.getSymbol().equals("USD") && localCurrency.getHistoryMinutes() == null)
                {
                    cardList.add(layoutGenerator.getInfoLayout(localCurrency, true));
                }

                balanceManager.getTotalBalance().set(i, localCurrency);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(false);
                    currencyLayout.removeAllViews();

                    for(int i = 0; i < cardList.size(); i++)
                    {
                        currencyLayout.addView(cardList.get(i));
                    }

                    adaptView();
                }
            });

            toolbarLayout.setTitle("US$" + String.format("%.2f", totalValue));

            if(totalFluctuation > 0)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toolbarSubtitle.setTextColor(getResources().getColor(R.color.increase));
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toolbarSubtitle.setTextColor(getResources().getColor(R.color.decrease));
                    }
                });
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    totalFluctuationPercentage = totalFluctuation / (totalValue - totalFluctuation) *100;

                    toolbarSubtitle.setText("US$" + String.format("%.2f", totalFluctuation) + " (" + String.format("%.2f", totalFluctuationPercentage) + "%)");

                    if(loadingDialog.isShowing())
                    {
                        loadingDialog.dismiss();
                    }
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private class DataUpdater extends AsyncTask<Void, Integer, Void>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            balanceManager.updateTotalBalance(new BalanceManager.VolleyCallBack() {
                @Override
                public void onSuccess() {

                    final List<Currency> balance = balanceManager.getTotalBalance();

                    if(balanceManager.getTotalBalance().size() > 0)
                    {
                        for(int i = 0; i < balanceManager.getTotalBalance().size(); i++)
                        {
                            balance.get(i).updateHistoryMinutes(getApplicationContext(), new Currency.CurrencyCallBack() {
                                @Override
                                public void onSuccess(Currency currency) {
                                    countCoins(true, false);
                                    /*currency.updateName(getApplicationContext(), new Currency.CurrencyCallBack() {
                                        @Override
                                        public void onSuccess(Currency currency) {
                                            countCoins(true, false);
                                        }
                                    });*/
                                }
                            });
                        }
                    }
                    else
                    {
                        countCoins(false, false);
                    }
                }

                public void onError(String error)
                {
                    switch (error)
                    {
                        case "com.android.volley.AuthFailureError":
                            preferencesManager.disableHitBTC();
                            Snackbar.make(findViewById(R.id.currencyListLayout), "HitBTC synchronization error : Invalid keys", Snackbar.LENGTH_LONG)
                                    .show();
                            refreshLayout.setRefreshing(false);
                            updateAll(true);
                            break;
                        default:
                            updateAll(true);
                    }
                    //updateAll();
                }
            });

            balanceManager.updateDetails(new BalanceManager.IconCallBack() {
                @Override
                public void onSuccess()
                {
                    countCoins(false, true);
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {

        }
    }

    public interface IconCallBack
    {
        void onSuccess(Bitmap bitmap);
    }
}
