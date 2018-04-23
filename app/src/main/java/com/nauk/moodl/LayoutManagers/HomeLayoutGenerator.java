package com.nauk.moodl.LayoutManagers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.nauk.moodl.Activities.CurrencyDetailsActivity;
import com.nauk.moodl.DataManagers.CurrencyData.Currency;
import com.nauk.moodl.DataManagers.CurrencyData.CurrencyDataChart;
import com.nauk.moodl.DataManagers.PreferencesManager;
import com.nauk.moodl.PlaceholderManager;
import com.nauk.moodl.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.abs;

/**
 * Created by Tiji on 05/01/2018.
 */

public class HomeLayoutGenerator {

    private android.content.Context context;

    public HomeLayoutGenerator(Context context)
    {
        this.context = context;
    }

    public View getInfoLayout(final Currency currency, float totalValue, boolean isBalanceHidden)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.cardview_currency, null, true);

        ((LineChart) view.findViewById(R.id.LineChartView)).setNoDataTextColor(currency.getChartColor());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                PreferencesManager preferencesManager = new PreferencesManager(context);

                if (view.findViewById(R.id.collapsableLayout).getVisibility() == View.VISIBLE) {
                    collapseView(view);
                } else {
                    view.findViewById(R.id.linearLayoutSubLayout).setVisibility(View.GONE);
                    view.findViewById(R.id.progressBarLinechartSummary).setVisibility(View.VISIBLE);
                    extendView(view);

                    if (currency.getHistoryMinutes() == null) {
                        currency.updateHistoryMinutes(context, preferencesManager.getDefaultCurrency(), new Currency.CurrencyCallBack() {
                            @Override
                            public void onSuccess(Currency currency) {
                                if(currency.getHistoryMinutes() != null)
                                {
                                    setupLineChart(view, currency);
                                    view.findViewById(R.id.progressBarLinechartSummary).setVisibility(View.GONE);
                                    view.findViewById(R.id.linearLayoutSubLayout).setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    view.findViewById(R.id.progressBarLinechartSummary).setVisibility(View.GONE);
                                    view.findViewById(R.id.linearLayoutSubLayout).setVisibility(View.VISIBLE);
                                    view.findViewById(R.id.linearLayoutSubLayout).findViewById(R.id.detailsArrow).setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                    else
                    {
                        extendView(view);
                        view.findViewById(R.id.progressBarLinechartSummary).setVisibility(View.GONE);
                        view.findViewById(R.id.linearLayoutSubLayout).setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        updateCardViewInfos(view, currency, totalValue, isBalanceHidden);

        view.findViewById(R.id.LineChartView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context.getApplicationContext(), CurrencyDetailsActivity.class);
                intent.putExtra("currency", currency);
                context.getApplicationContext().startActivity(intent);
            }
        });

        updateColor(view, currency);

        return view;
    }

    /*private class ChartLoader extends AsyncTask<Void, Integer, Void>
    {
        private View view;
        private Currency currency;

        ChartLoader(View view, Currency currency)
        {
            this.view = view;
            this.currency = currency;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            extendView(view);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            setupLineChart(view, currency);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            view.findViewById(R.id.progressLineChart).setVisibility(View.GONE);
            view.findViewById(R.id.LineChartView).setVisibility(View.VISIBLE);
            view.findViewById(R.id.LineChartView).invalidate();
        }
    }
*/
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
    }

    private void updateCardViewInfos(View view, Currency currency, float totalValue, boolean isBalanceHidden)
    {
        double value = currency.getValue() * currency.getBalance();
        double percentage = value / totalValue * 100;
        DecimalFormat df = new DecimalFormat(".##");

        ((TextView) view.findViewById(R.id.currencyValueOwnedTextView))
                .setText(PlaceholderManager.getValueParenthesisString(numberConformer(currency.getValue() * currency.getBalance()), context));
        ((TextView) view.findViewById(R.id.currencyValueTextView))
                .setText(PlaceholderManager.getValueString(numberConformer(currency.getValue()), context));
        ((TextView) view.findViewById(R.id.currencyFluctuationTextView))
                .setText(PlaceholderManager.getValueParenthesisString(numberConformer(currency.getDayFluctuation()), context));

        ((ImageView) view.findViewById(R.id.currencyIcon))
                .setImageBitmap(currency.getIcon());
        ((TextView) view.findViewById(R.id.currencyNameTextView))
                .setText(currency.getName());
        ((TextView) view.findViewById(R.id.currencySymbolTextView))
                .setText(PlaceholderManager.getSymbolString(currency.getSymbol(), context));
        ((TextView) view.findViewById(R.id.currencyOwnedTextView))
                .setText(PlaceholderManager.getBalanceString(numberConformer(currency.getBalance()), currency.getSymbol(), context));
        ((TextView) view.findViewById(R.id.currencyFluctuationPercentageTextView))
                .setText(PlaceholderManager.getPercentageString(numberConformer(currency.getDayFluctuationPercentage()), context));

        Drawable arrowDrawable = ((ImageView) view.findViewById(R.id.detailsArrow)).getDrawable();
        arrowDrawable.mutate();
        arrowDrawable.setColorFilter(new PorterDuffColorFilter(currency.getChartColor(), PorterDuff.Mode.SRC_IN));
        arrowDrawable.invalidateSelf();

        Drawable progressDrawable = ((ProgressBar) view.findViewById(R.id.progressBarLinechartSummary)).getIndeterminateDrawable();
        progressDrawable.mutate();
        progressDrawable.setColorFilter(new PorterDuffColorFilter(currency.getChartColor(), PorterDuff.Mode.SRC_IN));
        progressDrawable.invalidateSelf();

        Drawable progressBarDrawable = ((ProgressBar) view.findViewById(R.id.currencyPortfolioDominance)).getProgressDrawable();
        progressBarDrawable.mutate();
        progressBarDrawable.setColorFilter(new PorterDuffColorFilter(currency.getChartColor(), PorterDuff.Mode.SRC_IN));
        progressBarDrawable.invalidateSelf();

        ((ProgressBar) view.findViewById(R.id.currencyPortfolioDominance)).setProgress((int) Math.round(percentage));
        ((TextView) view.findViewById(R.id.percentageOwnedTextView)).setText(PlaceholderManager.getPercentageString(numberConformer(percentage), context));

        if(isBalanceHidden)
        {
            view.findViewById(R.id.currencyPortfolioDominance).setVisibility(View.VISIBLE);
            view.findViewById(R.id.percentageOwnedTextView).setVisibility(View.VISIBLE);
            view.findViewById(R.id.currencyOwnedInfoLayout).setVisibility(View.GONE);
        }
        else
        {
            view.findViewById(R.id.currencyPortfolioDominance).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.percentageOwnedTextView).setVisibility(View.GONE);
            view.findViewById(R.id.currencyOwnedInfoLayout).setVisibility(View.VISIBLE);
        }
    }

    private void collapseView(View view)
    {
        collapse(view.findViewById(R.id.collapsableLayout));
    }

    private void extendView(View view)
    {
        expand(view.findViewById(R.id.collapsableLayout));
        //view.findViewById(R.id.LineChartView).invalidate();
    }

    private void updateColor(View view, Currency currency)
    {
        if(currency.getDayFluctuationPercentage() >= 0)
        {
            ((TextView) view.findViewById(R.id.currencyFluctuationPercentageTextView))
                    .setTextColor(context.getResources().getColor(R.color.increase));
            ((TextView) view.findViewById(R.id.currencyFluctuationTextView))
                    .setTextColor(context.getResources().getColor(R.color.increase));
        }
        else
        {
            ((TextView) view.findViewById(R.id.currencyFluctuationPercentageTextView))
                    .setTextColor(context.getResources().getColor(R.color.decrease));
            ((TextView) view.findViewById(R.id.currencyFluctuationTextView))
                    .setTextColor(context.getResources().getColor(R.color.decrease));
        }
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
        for(int i = str.indexOf(".") - 1; i > 0; i--)
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