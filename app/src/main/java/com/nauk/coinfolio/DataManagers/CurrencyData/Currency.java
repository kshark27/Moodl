package com.nauk.coinfolio.DataManagers.CurrencyData;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Created by Tiji on 25/12/2017.
 */

public class Currency {

    private String name;
    private String symbol;
    private double value;
    private double balance;
    private float dayFluctuationPercentage;
    private double dayFluctuation;
    private List<CurrencyDataChart> dayPriceHistory;
    private CurrencyDataRetriver dataRetriver;
    private Bitmap icon;

    public Currency(String symbol, double balance)
    {
        this.symbol = symbol;
        this.balance = balance;
    }

    public Currency(String symbol, String name, double balance)
    {
        this.symbol = symbol;
        this.name = name;
        this.balance = balance;
    }

    public Currency(String name, String symbol)
    {
        this.name = name;
        this.symbol = symbol;
    }

    public void updateDayPriceHistory(android.content.Context context, final CurrencyCallBack callBack)
    {
        dataRetriver = new CurrencyDataRetriver(context);
        dataRetriver.updateLastDayHistory(symbol, new CurrencyDataRetriver.DataChartCallBack() {
            @Override
            public void onSuccess(List<CurrencyDataChart> dataChart) {
                setDayPriceHistory(dataChart);
                updateDayFluctuation();

                setValue(dataChart.get(dataChart.size() - 1).getClose());

                callBack.onSuccess(Currency.this);
            }
        });
    }

    public void updateName(android.content.Context context, final CurrencyCallBack callBack)
    {
        dataRetriver = new CurrencyDataRetriver(context);
        dataRetriver.updateCurrencyName(symbol, new CurrencyDataRetriver.NameCallBack() {
            @Override
            public void onSuccess(String name) {
                if(name != null)
                {
                    setName(name);
                }
                else
                {
                    setName("NameNotFound");
                }

                callBack.onSuccess(Currency.this);
            }
        });
    }

    public List<CurrencyDataChart> getDayPriceHistory()
    {
        return dayPriceHistory;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String newName)
    {
        name = newName;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public double getValue()
    {
        return value;
    }

    public void setValue(double newValue)
    {
        value = newValue;
    }

    public double getBalance()
    {
        return balance;
    }

    public float getDayFluctuationPercentage()
    {
        return  dayFluctuationPercentage;
    }

    public double getDayFluctuation()
    {
        return dayFluctuation;
    }

    public void setBalance(double newBalance)
    {
        balance = newBalance;
    }

    private void setDayPriceHistory(List<CurrencyDataChart> newDataChart)
    {
        dayPriceHistory = newDataChart;
    }

    public void setIcon(Bitmap newIcon)
    {
        icon = newIcon;
    }

    public Bitmap getIcon()
    {
        return icon;
    }

    private void updateDayFluctuation()
    {
        dayFluctuation = dayPriceHistory.get(dayPriceHistory.size() - 1).getOpen() - dayPriceHistory.get(0).getOpen();

        dayFluctuationPercentage = (float) (dayFluctuation / dayPriceHistory.get(0).getOpen() * 100);
    }

    public interface CurrencyCallBack {
        void onSuccess(Currency currency);
    }

}