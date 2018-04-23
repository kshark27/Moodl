package com.nauk.moodl.Activities.HomeActivityFragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.nauk.moodl.Activities.CurrencyDetailsActivity;
import com.nauk.moodl.Activities.CurrencySelectionActivity;
import com.nauk.moodl.Activities.HomeActivity;
import com.nauk.moodl.Activities.SettingsActivity;
import com.nauk.moodl.DataManagers.BalanceManager;
import com.nauk.moodl.DataManagers.CurrencyData.Currency;
import com.nauk.moodl.DataManagers.CurrencyData.CurrencyDataChart;
import com.nauk.moodl.DataManagers.CurrencyData.CurrencyDetailsList;
import com.nauk.moodl.DataManagers.CurrencyData.CurrencyTickerList;
import com.nauk.moodl.DataManagers.DatabaseManager;
import com.nauk.moodl.DataManagers.PreferencesManager;
import com.nauk.moodl.DataManagers.WatchlistManager;
import com.nauk.moodl.PlaceholderManager;
import com.nauk.moodl.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.abs;

/**
 * Created by Tiji on 13/04/2018.
 */

public class Watchlist extends Fragment {

    private WatchlistManager watchlistManager;
    private View view;
    private int watchlistCounter;
    private CurrencyDetailsList currencyDetailsList;
    private SwipeRefreshLayout refreshLayout;
    private long lastTimestamp;
    private PreferencesManager preferencesManager;
    private String defaultCurrency;
    private CurrencyTickerList currencyTickerList;
    private boolean tickerUpdated;
    private boolean detailsUpdated;
    private boolean editModeEnabled;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_watchlist_homeactivity, container, false);

        refreshLayout = view.findViewById(R.id.swiperefreshwatchlist);
        currencyDetailsList = new CurrencyDetailsList(getContext());
        preferencesManager = new PreferencesManager(getContext());

        lastTimestamp = 0;
        defaultCurrency = preferencesManager.getDefaultCurrency();
        currencyTickerList = new CurrencyTickerList(getActivity());
        tickerUpdated = false;
        currencyTickerList.update(new BalanceManager.IconCallBack() {
            @Override
            public void onSuccess() {
                tickerUpdated = true;
                checkUpdatedData();
            }
        });

        editModeEnabled = false;

        watchlistManager = new WatchlistManager(getContext());

        updateWatchlist(true);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateWatchlist(false);
            }
        });

        Button addWatchlistButton = view.findViewById(R.id.buttonAddWatchlist);
        addWatchlistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent selectionIntent = new Intent(getActivity(), CurrencySelectionActivity.class);
                selectionIntent.putExtra("isWatchList", true);
                startActivity(selectionIntent);
            }
        });

        ImageButton settingsButton = view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingIntent);
            }
        });

        ImageButton editButton = view.findViewById(R.id.edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(editModeEnabled)
                {
                    editModeEnabled = false;

                    for(int i = 0; i < ((LinearLayout) Watchlist.this.view.findViewById(R.id.linearLayoutWatchlist)).getChildCount(); i++)
                    {
                        ((LinearLayout) Watchlist.this.view.findViewById(R.id.linearLayoutWatchlist)).getChildAt(i).setClickable(true);
                        collapseW(((LinearLayout) Watchlist.this.view.findViewById(R.id.linearLayoutWatchlist)).getChildAt(i).findViewById(R.id.deleteCardWatchlist));
                    }
                }
                else
                {
                    editModeEnabled = true;

                    for(int i = 0; i < ((LinearLayout) Watchlist.this.view.findViewById(R.id.linearLayoutWatchlist)).getChildCount(); i++)
                    {
                        ((LinearLayout) Watchlist.this.view.findViewById(R.id.linearLayoutWatchlist)).getChildAt(i).setClickable(false);
                        expandW(((LinearLayout) Watchlist.this.view.findViewById(R.id.linearLayoutWatchlist)).getChildAt(i).findViewById(R.id.deleteCardWatchlist));
                    }
                }
            }
        });

        return view;
    }

    private void collapseView(View view)
    {
        collapse(view.findViewById(R.id.collapsableLayout));
    }

    private void extendView(View view)
    {
        expand(view.findViewById(R.id.collapsableLayout));
        view.findViewById(R.id.LineChartView).invalidate();
    }

    private static void expand(final View v) {
        v.measure(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? CardView.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private static void expandW(final View v) {
        v.measure(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
        final int targetWidth = v.getMeasuredWidth();

        v.getLayoutParams().width = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().width = interpolatedTime == 1
                        ? CardView.LayoutParams.WRAP_CONTENT
                        : (int)(targetWidth * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int)(targetWidth / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private static void collapseW(final View v) {
        final int initialWidth = v.getMeasuredWidth();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().width = initialWidth - (int)(initialWidth * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int)(initialWidth / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }


    @Override
    public void onResume()
    {
        super.onResume();

        if(!defaultCurrency.equals(preferencesManager.getDefaultCurrency()))
        {
            defaultCurrency = preferencesManager.getDefaultCurrency();
            updateWatchlist(true);
        }
        else
        {
            updateWatchlist(preferencesManager.mustUpdateWatchlist());
        }
    }

    private void updateWatchlist(boolean mustUpdate)
    {
        if(System.currentTimeMillis()/1000 - lastTimestamp > 60 || mustUpdate)
        {
            if(!refreshLayout.isRefreshing())
            {
                refreshLayout.setRefreshing(true);
            }

            lastTimestamp = System.currentTimeMillis()/1000;
            detailsUpdated = false;

            watchlistManager.updateWatchlist();

            currencyDetailsList.update(new BalanceManager.IconCallBack() {
                @Override
                public void onSuccess() {
                    detailsUpdated = true;
                    checkUpdatedData();
                }
            });
        }
        else
        {
            if(refreshLayout.isRefreshing())
            {
                refreshLayout.setRefreshing(false);
            }
        }
    }

    private void checkUpdatedData()
    {
        if(tickerUpdated && detailsUpdated)
        {
            WatchlistUpdater watchlistUpdater = new WatchlistUpdater();
            watchlistUpdater.execute();
        }
    }

    private void generateCards()
    {
        final List<View> watchlistViews = new ArrayList<View>();

        ((LinearLayout) view.findViewById(R.id.linearLayoutWatchlist)).removeAllViews();

        Runnable newRunnable = new Runnable() {
            @Override
            public void run() {
                for(final Currency currency : watchlistManager.getWatchlist())
                {
                    watchlistViews.add(getCurrencyCardFor(currency));
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0; i < watchlistViews.size(); i++)
                        {
                            ((LinearLayout) view.findViewById(R.id.linearLayoutWatchlist)).addView(watchlistViews.get(i), 0);
                        }
                    }
                });
            }
        };

        newRunnable.run();

        if(refreshLayout.isRefreshing())
        {
            refreshLayout.setRefreshing(false);
        }
    }

    private void countWatchlist()
    {
        watchlistCounter++;

        if(watchlistCounter >= watchlistManager.getWatchlist().size())
        {
            generateCards();
        }
    }

    private View getCurrencyCardFor(final Currency currency)
    {
        final View card = LayoutInflater.from(getContext()).inflate(R.layout.cardview_watchlist, null);

        card.setTag(currency.getSymbol());

        ((TextView) card.findViewById(R.id.currencyFluctuationTextView))
                .setText(PlaceholderManager.getValueParenthesisString(numberConformer(currency.getDayFluctuation()), getActivity()));
        ((TextView) card.findViewById(R.id.currencyValueTextView))
                .setText(PlaceholderManager.getValueString(numberConformer(currency.getValue()), getActivity()));

        ((TextView) card.findViewById(R.id.currencyFluctuationPercentageTextView))
                .setText(PlaceholderManager.getPercentageString(numberConformer(currency.getDayFluctuationPercentage()), getActivity()));
        ((TextView) card.findViewById(R.id.currencyNameTextView))
                .setText(currency.getName());
        ((TextView) card.findViewById(R.id.currencySymbolTextView))
                .setText(PlaceholderManager.getSymbolString(currency.getSymbol(), getActivity()));
        ((ImageView) card.findViewById(R.id.currencyIcon)).setImageBitmap(currency.getIcon());

        ((LineChart) card.findViewById(R.id.LineChartView)).setNoDataTextColor(currency.getChartColor());

        Drawable arrowDrawable = ((ImageView) card.findViewById(R.id.detailsArrow)).getDrawable();
        arrowDrawable.mutate();
        arrowDrawable.setColorFilter(new PorterDuffColorFilter(currency.getChartColor(), PorterDuff.Mode.SRC_IN));
        arrowDrawable.invalidateSelf();

        Drawable progressDrawable = ((ProgressBar) card.findViewById(R.id.progressBarLinechartWatchlist)).getIndeterminateDrawable();
        progressDrawable.mutate();
        progressDrawable.setColorFilter(new PorterDuffColorFilter(currency.getChartColor(), PorterDuff.Mode.SRC_IN));
        progressDrawable.invalidateSelf();

        card.findViewById(R.id.deleteCardWatchlist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collapse(card);
                DatabaseManager databaseManager = new DatabaseManager(getActivity());
                databaseManager.deleteCurrencyFromWatchlist(currency.getSymbol());
            }
        });

        updateColor(card, currency);

        card.findViewById(R.id.LineChartView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CurrencyDetailsActivity.class);
                intent.putExtra("currency", currency);
                getActivity().getApplicationContext().startActivity(intent);
            }
        });

        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if(view.findViewById(R.id.collapsableLayout).getVisibility() == View.VISIBLE)
                {
                    collapseView(view);
                }
                else
                {
                    view.findViewById(R.id.linearLayoutSubLayout).setVisibility(View.GONE);
                    view.findViewById(R.id.progressBarLinechartWatchlist).setVisibility(View.VISIBLE);
                    extendView(view);

                    if (currency.getHistoryMinutes() == null) {
                        currency.updateHistoryMinutes(getActivity(), preferencesManager.getDefaultCurrency(), new Currency.CurrencyCallBack() {
                            @Override
                            public void onSuccess(Currency currency) {
                                if(currency.getHistoryMinutes() != null)
                                {
                                    setupLineChart(view, currency);
                                    view.findViewById(R.id.progressBarLinechartWatchlist).setVisibility(View.GONE);
                                    view.findViewById(R.id.linearLayoutSubLayout).setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    view.findViewById(R.id.progressBarLinechartWatchlist).setVisibility(View.GONE);
                                    view.findViewById(R.id.linearLayoutSubLayout).setVisibility(View.VISIBLE);
                                    view.findViewById(R.id.linearLayoutSubLayout).findViewById(R.id.detailsArrow).setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                    else
                    {
                        extendView(view);
                        view.findViewById(R.id.progressBarLinechartWatchlist).setVisibility(View.GONE);
                        view.findViewById(R.id.linearLayoutSubLayout).setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        return card;
    }

    private LineData generateData(Currency currency)
    {
        LineDataSet dataSet;
        List<CurrencyDataChart> dataChartList = currency.getHistoryMinutes();
        ArrayList<Entry> values = new ArrayList<>();

        for(int i = 0; i < dataChartList.size(); i+=10)
        {
            values.add(new Entry(i, (float) dataChartList.get(i).getOpen()));
        }

        dataSet = new LineDataSet(values, "History");
        dataSet.setDrawIcons(false);
        dataSet.setColor(currency.getChartColor());
        dataSet.setLineWidth(1);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getColorWithAplha(currency.getChartColor(), 0.5f));
        dataSet.setFormLineWidth(1);
        dataSet.setFormSize(15);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setHighlightEnabled(false);

        return new LineData(dataSet);
    }

    private int getColorWithAplha(int color, float ratio)
    {
        int transColor;
        int alpha = Math.round(Color.alpha(color) * ratio);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        transColor = Color.argb(alpha, r, g, b);

        return transColor ;
    }

    private void setupLineChart(View view, final Currency currency)
    {
        LineChart lineChart = view.findViewById(R.id.LineChartView);

        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);
        lineChart.setDrawMarkers(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setScaleEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getXAxis().setEnabled(false);
        lineChart.setViewPortOffsets(0, 0, 0, 0);
        lineChart.setData(generateData(currency));

        lineChart.invalidate();
    }

    private void updateColor(View card, Currency currency)
    {
        if(currency.getDayFluctuation() >= 0)
        {
            ((TextView) card.findViewById(R.id.currencyFluctuationPercentageTextView)).setTextColor(getResources().getColor(R.color.increase));
            ((TextView) card.findViewById(R.id.currencyFluctuationTextView)).setTextColor(getResources().getColor(R.color.increase));
        }
        else
        {
            ((TextView) card.findViewById(R.id.currencyFluctuationPercentageTextView)).setTextColor(getResources().getColor(R.color.decrease));
            ((TextView) card.findViewById(R.id.currencyFluctuationTextView)).setTextColor(getResources().getColor(R.color.decrease));
        }
    }

    private String getIconUrl(String symbol)
    {
        String url;

        try {
            JSONObject jsonObject = new JSONObject(currencyDetailsList.getCoinInfosHashmap().get(symbol));
            url = "https://www.cryptocompare.com" + jsonObject.getString("ImageUrl") + "?width=50";
        } catch (NullPointerException e) {
            Log.d(getContext().getResources().getString(R.string.debug), symbol + " has no icon URL");
            url = null;
        } catch (JSONException e) {
            Log.d(getContext().getResources().getString(R.string.debug), "Url parsing error for " + symbol);
            url = null;
        }

        return url;
    }

    private void getBitmapFromURL(String src, HomeActivity.IconCallBack callBack) {
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
            result = BitmapFactory.decodeResource(this.getResources(),
                    R.mipmap.ic_launcher_moodl);
            result = Bitmap.createScaledBitmap(result, 50, 50, false);
        }

        callBack.onSuccess(result);
    }

    private void updateChartColor(Currency currency)
    {
        if(currency.getIcon() != null)
        {
            Palette.Builder builder = Palette.from(currency.getIcon());

            currency.setChartColor(builder.generate().getDominantColor(0));
        }
        else
        {
            currency.setChartColor(12369084);
        }
    }

    public int getCurrencyId(String symbol)
    {
        int id = 0;

        try {
            JSONObject jsonObject = new JSONObject(currencyDetailsList.getCoinInfosHashmap().get(symbol));
            id = jsonObject.getInt("Id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return id;
    }

    private class WatchlistUpdater extends AsyncTask<Void, Integer, Void>
    {
        @Override
        protected void onPreExecute()
        {
            watchlistCounter = 0;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for(final Currency currency : watchlistManager.getWatchlist())
            {
                currency.setTickerId(currencyTickerList.getTickerIdForSymbol(currency.getSymbol()));
                currency.setId(getCurrencyId(currency.getSymbol()));
                currency.updatePrice(getActivity(), preferencesManager.getDefaultCurrency(), new Currency.CurrencyCallBack() {
                    @Override
                    public void onSuccess(final Currency sucessCurrency) {
                        if(getIconUrl(sucessCurrency.getSymbol()) != null)
                        {
                            getBitmapFromURL(getIconUrl(sucessCurrency.getSymbol()), new HomeActivity.IconCallBack() {
                                @Override
                                public void onSuccess(Bitmap bitmapIcon) {
                                    sucessCurrency.setIcon(bitmapIcon);
                                    updateChartColor(currency);
                                    countWatchlist();
                                }
                            });
                        }
                    }
                });
            }
            return null;
        }
    }

    private String numberConformer(double number)
    {
        String str;

        if(abs(number) > 1)
        {
            str = String.format( Locale.UK, "%.2f", number).replaceAll("\\.?0*$", "");
        }
        else
        {
            str = String.format( Locale.UK, "%.4f", number).replaceAll("\\.?0*$", "");
        }

        int counter = 0;
        int i = str.indexOf(".");
        if(i <= 0)
        {
            i = str.length();
        }
        for(i -= 1; i > 0; i--)
        {
            counter++;
            if(counter == 3)
            {
                str = str.substring(0, i) + " " + str.substring(i, str.length());
                counter = 0;
            }
        }

        return str;
    }
}