package com.nauk.moodl.DataManagers;

import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.nauk.moodl.DataManagers.CurrencyData.Currency;
import com.nauk.moodl.DataManagers.CurrencyData.CurrencyDetailsList;
import com.nauk.moodl.DataManagers.ExchangeManager.BinanceManager;
import com.nauk.moodl.DataManagers.ExchangeManager.HitBtcManager;
import com.nauk.moodl.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Tiji on 25/12/2017.
 */

public class BalanceManager {

    private String publicHitKey;
    private String publicBinanceKey;
    private String publicPoloniex;
    private String privateHitKey;
    private String privateBinanceKey;
    private String privatePoloniex;
    final private String hitBalanceUrl = "https://api.hitbtc.com/api/2/trading/balance";
    final private String detailUrl = "https://www.cryptocompare.com/api/data/coinlist/";
    private RequestQueue requestQueue;
    private List<Currency> binanceBalance;
    private List<Currency> hitBalance;
    private List<Currency> manualBalances;
    private List<Currency> totalBalance;
    private android.content.Context context;
    private LinkedHashMap<String, String> coinInfosHashmap;
    private PreferencesManager preferenceManager;
    private DatabaseManager databaseManager;
    private CurrencyDetailsList currencyDetailsList;

    private int balanceCounter;

    //NEW IMPLEMENTATION
    private List<HitBtcManager> hitBtcManagers;
    private List<BinanceManager> binanceManagers;

    public BalanceManager(android.content.Context context)
    {
        this.context = context;

        preferenceManager = new PreferencesManager(context);
        requestQueue = Volley.newRequestQueue(context);
        binanceBalance = new ArrayList<Currency>();
        hitBalance = new ArrayList<Currency>();
        manualBalances = new ArrayList<Currency>();
        databaseManager = new DatabaseManager(context);
        hitBtcManagers = new ArrayList<>();
        binanceManagers = new ArrayList<>();
        currencyDetailsList = new CurrencyDetailsList(context);

        balanceCounter = 0;
    }

    public List<String> getBiggestCurrencies()
    {
        List<String> currenciesDetails = new ArrayList<>();

        int index = 0;
        Iterator<String> coinIterator = currencyDetailsList.getCoinInfosHashmap().keySet().iterator();

        while(index < 11)
        {
            index++;

            Log.d("moodl", "For " + index + " : " + coinIterator.next());
        }

        return currenciesDetails;
    }

    public void updateExchangeKeys()
    {
        String publicKey = preferenceManager.getHitBTCPublicKey();
        String privateKey = preferenceManager.getHitBTCPrivateKey();

        hitBtcManagers.clear();

        if(preferenceManager.isHitBTCActivated() && publicKey != null && privateKey != null)
        {
            hitBtcManagers.add(new HitBtcManager(context, publicKey, privateKey));
        }

        publicKey = preferenceManager.getBinancePublicKey();
        privateKey = preferenceManager.getBinancePrivateKey();

        binanceManagers.clear();

        if(preferenceManager.isBinanceActivated() && publicKey != null && privateKey != null)
        {
            binanceManagers.add(new BinanceManager(publicKey, privateKey));
        }
    }

    public List<Currency> getTotalBalance()
    {
        return totalBalance;
    }

    public void updateTotalBalance(final VolleyCallBack callBack)
    {
        boolean isUpdated = false;
        
        balanceCounter = 0;

        manualBalances = databaseManager.getAllCurrenciesFromManualCurrency();

        if(binanceManagers.size() > 0)
        {
            isUpdated = true;

            for(int i = 0; i < binanceManagers.size(); i++)
            {
                binanceManagers.get(i).updateBalance(new BinanceManager.BinanceCallBack() {
                    @Override
                    public void onSuccess() {
                        countBalances(callBack);
                    }

                    @Override
                    public void onError(String error) {
                        callBack.onError(error);
                    }
                });
            }
        }

        if(hitBtcManagers.size() > 0)
        {
            isUpdated = true;

            for(int i = 0; i < hitBtcManagers.size(); i++)
            {
                hitBtcManagers.get(i).updateGlobalBalance(new HitBtcManager.HitBtcCallBack() {
                    @Override
                    public void onSuccess() {
                        countBalances(callBack);
                    }

                    @Override
                    public void onError(String error) {
                        callBack.onError(error);
                    }
                });
            }
        }

        if(!isUpdated)
        {
            refreshAllBalances(callBack);
        }
    }

    private void countBalances(VolleyCallBack callBack)
    {
        balanceCounter++;

        if(balanceCounter == hitBtcManagers.size() + binanceManagers.size())
        {
            refreshAllBalances(callBack);

            balanceCounter = 0;
        }
    }

    private void refreshAllBalances(final VolleyCallBack callBack)
    {
        totalBalance = new ArrayList<>();

        for(int i = 0; i < hitBtcManagers.size(); i++)
        {
            mergeBalanceTotal(hitBtcManagers.get(i).getBalance());
        }

        for(int i = 0; i < binanceManagers.size(); i++)
        {
            mergeBalanceTotal(binanceManagers.get(i).getBalance());
        }

        mergeBalanceTotal(manualBalances);

        callBack.onSuccess();
    }

    private void mergeBalanceTotal(List<Currency> balance)
    {
        if(balance != null)
        {
            for(int i = 0; i < balance.size(); i++)
            {
                boolean isIn = false;

                for(int j = 0; j < totalBalance.size(); j++)
                {
                    if(balance.get(i).getSymbol().equals(totalBalance.get(j).getSymbol()))
                    {
                        totalBalance.get(j).setBalance(totalBalance.get(j).getBalance() + balance.get(i).getBalance());

                        isIn = true;
                    }
                }

                if(!isIn)
                {
                    totalBalance.add(balance.get(i));
                }
            }
        }
    }

    public interface VolleyCallBack {
        void onSuccess();
        void onError(String error);
    }

    public interface IconCallBack {
        void onSuccess();
    }

    public void sortCoins()
    {
        for(int i = 0; i < totalBalance.size(); i++)
        {
            for(int j = i; j < totalBalance.size(); j++)
            {
                if(totalBalance.get(j).getBalance() * totalBalance.get(j).getValue() > totalBalance.get(i).getBalance() * totalBalance.get(i).getValue())
                {
                    Currency temp = totalBalance.get(j);
                    totalBalance.set(j, totalBalance.get(i));
                    totalBalance.set(i, temp);
                }
            }
        }
    }

    public void updateDetails(final IconCallBack callBack)
    {
        currencyDetailsList.update(callBack);
        /*StringRequest strRequest = new StringRequest(Request.Method.GET, detailUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.length() > 0) {
                            processDetailResult(response, callBack);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        requestQueue.add(strRequest);*/
    }

    public String getIconUrl(String symbol)
    {
        String url;

        try {
            switch (symbol)
            {
                case "IOTA":
                    url = "https://www.cryptocompare.com/media/1383540/iota_logo.png?width=50";
                    break;
                default:
                    JSONObject jsonObject = new JSONObject(currencyDetailsList.getCoinInfosHashmap().get(symbol));
                    url = "https://www.cryptocompare.com" + jsonObject.getString("ImageUrl") + "?width=50";
                    break;
            }
        } catch (NullPointerException e) {
            Log.d(context.getResources().getString(R.string.debug), symbol + " has no icon URL");
            url = null;
        } catch (JSONException e) {
            Log.d(context.getResources().getString(R.string.debug), "Url parsing error for " + symbol);
            url = null;
        }

        return url;
    }

    public CurrencyDetailsList getCurrencyDetailList()
    {
        return currencyDetailsList;
    }

    public String getCurrencyName(String symbol)
    {
        String currencyName = null;

        try {
            JSONObject jsonObject = new JSONObject(currencyDetailsList.getCoinInfosHashmap().get(symbol));
            currencyName = jsonObject.getString("CoinName");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return currencyName;
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

    private void sortDetails()
    {
        LinkedHashMap<String, String> sortedHashmap = new LinkedHashMap<>();
        List<String> listInfos = new ArrayList<>(coinInfosHashmap.values());
        List<String> listSymbols = new ArrayList<>(coinInfosHashmap.keySet());

        for(int i = 0; i < coinInfosHashmap.keySet().size(); i++)
        {

            try {
                JSONObject jsonObject = new JSONObject(listInfos.get(i));
                int index = jsonObject.getInt("SortOrder");

                listInfos.add(index, listInfos.get(i));
                listSymbols.add(index, listSymbols.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for(int i = 0; i < listInfos.size(); i++)
        {
            sortedHashmap.put(listSymbols.get(i), listInfos.get(i));
        }

        coinInfosHashmap = sortedHashmap;
    }
}
