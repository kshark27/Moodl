package com.herbron.moodl.DataManagers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.herbron.moodl.DataManagers.CurrencyData.Currency;
import com.herbron.moodl.DataManagers.CurrencyData.Transaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Guitoune on 14/01/2018.
 */

public class DatabaseManager extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 7;

    private static final String DATABASE_NAME = "Currencies.db";

    public static final String TABLE_MANUAL_CURRENCIES = "ManualCurrencies";
    public static final String TABLE_EXCHANGE_KEYS = "ExchangeKeys";
    public static final String TABLE_WATCHLIST = "Watchlist";

    private static final String KEY_CURRENCY_ID = "idCurrency";
    private static final String KEY_CURRENCY_SYMBOL = "symbol";
    private static final String KEY_CURRENCY_NAME = "name";
    private static final String KEY_CURRENCY_BALANCE = "balance";
    private static final String KEY_CURRENCY_DATE = "addDate";
    private static final String KEY_CURRENCY_PURCHASED_PRICE = "purchasedPrice";
    private static final String KEY_CURRENCY_IS_MINED = "isMined";
    private static final String KEY_CURRENCY_FEES = "fees";

    private static final String KEY_EXCHANGE_ID = "idExchange";
    private static final String KEY_EXCHANGE_NAME = "name";
    private static final String KEY_EXCHANGE_DESCRIPTION = "description";
    private static final String KEY_EXCHANGE_PUBLIC_KEY = "publicKey";
    private static final String KEY_EXCHANGE_SECRET_KEY = "secretKey";

    private static final String KEY_WATCHLIST_ID = "idWatchlist";
    private static final String KEY_WATCHLIST_SYMBOL = "symbol";
    private static final String KEY_WATCHLIST_NAME = "name";
    private static final String KEY_WATCHLIST_POSITION = "position";

    public DatabaseManager(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MANUAL_CURRENCIES + "("
                + KEY_CURRENCY_ID + " INTEGER PRIMARY KEY,"
                + KEY_CURRENCY_SYMBOL + " VARCHAR(4),"
                + KEY_CURRENCY_NAME + " VARCHAR(45),"
                + KEY_CURRENCY_BALANCE + " TEXT,"
                + KEY_CURRENCY_DATE + " TEXT,"
                + KEY_CURRENCY_PURCHASED_PRICE + " REAL,"
                + KEY_CURRENCY_IS_MINED + " INTEGER,"
                + KEY_CURRENCY_FEES + " REAL"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXCHANGE_KEYS + "("
                + KEY_EXCHANGE_ID + " INTEGER PRIMARY KEY,"
                + KEY_EXCHANGE_NAME + " TEXT,"
                + KEY_EXCHANGE_DESCRIPTION + " TEXT,"
                + KEY_EXCHANGE_PUBLIC_KEY + " TEXT,"
                + KEY_EXCHANGE_SECRET_KEY + " TEXT"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WATCHLIST + "("
                + KEY_WATCHLIST_ID + " INTEGER PRIMARY KEY,"
                + KEY_WATCHLIST_SYMBOL + " VARCHAR(4),"
                + KEY_WATCHLIST_NAME + " TEXT,"
                + KEY_WATCHLIST_POSITION + " INTEGER"
                + ");");

        //loadSample(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        switch (oldVersion)
        {
            case 6:
                db.execSQL("ALTER TABLE " + TABLE_EXCHANGE_KEYS
                        + "  ADD " + KEY_EXCHANGE_DESCRIPTION+ " VARCHAR");
        }
    }

    private boolean isCurrencyInWatchlist(String symbol)
    {
        String searchQuerry = "SELECT * FROM " + TABLE_WATCHLIST + " WHERE " + KEY_WATCHLIST_SYMBOL + "='" + symbol + "'";
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor result = db.rawQuery(searchQuerry, null);

        return result.moveToFirst();
    }

    public boolean addCurrencyToWatchlist(Currency currency)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        if(!isCurrencyInWatchlist(currency.getSymbol()))
        {
            ContentValues values = new ContentValues();

            values.put(KEY_WATCHLIST_SYMBOL, currency.getSymbol());
            values.put(KEY_WATCHLIST_NAME, currency.getName());
            values.put(KEY_WATCHLIST_POSITION, getWatchlistRowCount(db));

            db.insert(TABLE_WATCHLIST, null, values);
            db.close();

            return true;
        }

        return false;
    }

    public void updateWatchlistPosition(String symbol, int position)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(KEY_WATCHLIST_POSITION, position);

        db.update(TABLE_WATCHLIST, cv, KEY_WATCHLIST_SYMBOL + "='" + symbol + "'", null);

    }

    private int getWatchlistRowCount(SQLiteDatabase db)
    {
        String countQuerry = "SELECT COUNT() FROM " + TABLE_WATCHLIST;
        Cursor result = db.rawQuery(countQuerry, null);

        result.moveToFirst();

        return result.getInt(0);
    }

    public void deleteCurrencyFromWatchlist(String symbol)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_WATCHLIST, KEY_WATCHLIST_SYMBOL + " = '" + symbol + "'", null);
        db.close();
    }

    public JSONArray getDatabaseBackup(Context context, String table, boolean encryptData)
    {
        String selectQuerry = "SELECT * FROM " + table;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor result = db.rawQuery(selectQuerry, null);

        JSONArray backupArray = new JSONArray();

        while(result.moveToNext())
        {
            JSONObject backupObject = new JSONObject();

            for(int i = 0; i < result.getColumnCount(); i++)
            {
                try {
                    if(result.getString(i) != null)
                    {
                        if(encryptData)
                        {
                            backupObject.put(result.getColumnName(i), DataCrypter.encrypt(context, result.getString(i)));
                        }
                        else
                        {
                            backupObject.put(result.getColumnName(i), result.getString(i));
                        }
                    }
                    else
                    {
                        backupObject.put(result.getColumnName(i), "");
                    }
                } catch (JSONException e) {
                    Log.d("moodl", "Error while creating a json backup");
                }
            }

            backupArray.put(backupObject);
        }

        return backupArray;
    }

    public void wipeData(String table)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM "+ table);
    }

    public void addRowWatchlist(JSONObject rawValues, Context context, boolean decrypt)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            if(decrypt)
            {
                values.put(KEY_WATCHLIST_SYMBOL, DataCrypter.decrypt(context, rawValues.getString(KEY_WATCHLIST_SYMBOL)));
                values.put(KEY_WATCHLIST_NAME, DataCrypter.decrypt(context, rawValues.getString(KEY_WATCHLIST_NAME)));
                values.put(KEY_WATCHLIST_POSITION, DataCrypter.decrypt(context, rawValues.getString(KEY_WATCHLIST_POSITION)));
            }
            else
            {
                values.put(KEY_WATCHLIST_SYMBOL, rawValues.getString(KEY_WATCHLIST_SYMBOL));
                values.put(KEY_WATCHLIST_NAME, rawValues.getString(KEY_WATCHLIST_NAME));
                values.put(KEY_WATCHLIST_POSITION, rawValues.getString(KEY_WATCHLIST_POSITION));
            }
        } catch (JSONException e) {
            Log.d("moodl", "Error while inserting transaction");
        }

        db.insert(TABLE_MANUAL_CURRENCIES, null, values);
        db.close();
    }

    public void addRowTransaction(JSONObject rawValues, Context context, boolean decrypt)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            if(decrypt)
            {
                values.put(KEY_CURRENCY_SYMBOL, DataCrypter.decrypt(context, rawValues.getString(KEY_CURRENCY_SYMBOL)));
                values.put(KEY_CURRENCY_NAME, DataCrypter.decrypt(context, rawValues.getString(KEY_CURRENCY_NAME)));
                values.put(KEY_CURRENCY_BALANCE, DataCrypter.decrypt(context, rawValues.getString(KEY_CURRENCY_BALANCE)));
                values.put(KEY_CURRENCY_DATE, DataCrypter.decrypt(context, rawValues.getString(KEY_CURRENCY_DATE)));
                values.put(KEY_CURRENCY_PURCHASED_PRICE, DataCrypter.decrypt(context, rawValues.getString(KEY_CURRENCY_PURCHASED_PRICE)));
                values.put(KEY_CURRENCY_IS_MINED, DataCrypter.decrypt(context, rawValues.getString(KEY_CURRENCY_IS_MINED)));
                values.put(KEY_CURRENCY_FEES, DataCrypter.decrypt(context, rawValues.getString(KEY_CURRENCY_FEES)));
            }
            else
            {
                values.put(KEY_CURRENCY_SYMBOL, rawValues.getString(KEY_CURRENCY_SYMBOL));
                values.put(KEY_CURRENCY_NAME, rawValues.getString(KEY_CURRENCY_NAME));
                values.put(KEY_CURRENCY_BALANCE, rawValues.getString(KEY_CURRENCY_BALANCE));
                values.put(KEY_CURRENCY_DATE, rawValues.getString(KEY_CURRENCY_DATE));
                values.put(KEY_CURRENCY_PURCHASED_PRICE, rawValues.getString(KEY_CURRENCY_PURCHASED_PRICE));
                values.put(KEY_CURRENCY_IS_MINED, rawValues.getString(KEY_CURRENCY_IS_MINED));
                values.put(KEY_CURRENCY_FEES, rawValues.getString(KEY_CURRENCY_FEES));
            }
        } catch (JSONException e) {
            Log.d("moodl", "Error while inserting transaction");
        }

        db.insert(TABLE_MANUAL_CURRENCIES, null, values);
        db.close();
    }

    public List<Currency> getAllCurrenciesFromWatchlist()
    {
        String searchQuerry = "SELECT * FROM " + TABLE_WATCHLIST + " ORDER BY " + KEY_WATCHLIST_POSITION + " ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultList = db.rawQuery(searchQuerry, null);

        List<Currency> currencyList = new ArrayList<>();

        while(resultList.moveToNext())
        {
            currencyList.add(new Currency(resultList.getString(2), resultList.getString(1)));
        }

        return currencyList;
    }

    public void addCurrencyToManualCurrency(String symbol, double balance, Date date, double purchasedPrice, double fees)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_CURRENCY_SYMBOL, symbol);
        values.put(KEY_CURRENCY_BALANCE, balance);
        values.put(KEY_CURRENCY_DATE, date.getTime());
        values.put(KEY_CURRENCY_PURCHASED_PRICE, purchasedPrice);
        values.put(KEY_CURRENCY_FEES, fees);

        db.insert(TABLE_MANUAL_CURRENCIES, null, values);
        db.close();
    }

    public List<Currency> getAllCurrenciesFromManualCurrency()
    {
        String searchQuerry = "SELECT * FROM " + TABLE_MANUAL_CURRENCIES;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultatList = db.rawQuery(searchQuerry, null);

        List<Currency> currencyList = new ArrayList<>();

        while(resultatList.moveToNext())
        {
            currencyList.add(new Currency(resultatList.getString(1), resultatList.getDouble(3) - resultatList.getDouble(7)));
        }

        resultatList.close();

        db.close();

        return currencyList;
    }

    public void updateTransactionWithId(int transactionId, double amount, Date time, double purchasedPrice, double fees)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(KEY_CURRENCY_BALANCE, amount);
        cv.put(KEY_CURRENCY_DATE, time.getTime());
        cv.put(KEY_CURRENCY_PURCHASED_PRICE, purchasedPrice);
        cv.put(KEY_CURRENCY_FEES, fees);

        db.update(TABLE_MANUAL_CURRENCIES, cv, KEY_CURRENCY_ID + "=" + transactionId, null);

    }

    public Transaction getCurrencyTransactionById(int id)
    {
        String searchQuerry = "SELECT * FROM " + TABLE_MANUAL_CURRENCIES + " WHERE " + KEY_CURRENCY_ID + "='" + id + "'";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultatList = db.rawQuery(searchQuerry, null);

        Transaction transaction = null;

        if(resultatList.moveToFirst())
        {
            transaction = new Transaction(resultatList.getInt(0), resultatList.getString(1), resultatList.getDouble(3), resultatList.getLong(4), resultatList.getLong(5), resultatList.getDouble(7));
        }

        resultatList.close();

        db.close();

        return transaction;
    }

    public ArrayList<Transaction> getCurrencyTransactionsForSymbol(String symbol)
    {
        String searchQuerry = "SELECT * FROM " + TABLE_MANUAL_CURRENCIES + " WHERE " + KEY_CURRENCY_SYMBOL + "='" + symbol.toUpperCase() + "'";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultatList = db.rawQuery(searchQuerry, null);

        ArrayList<Transaction> transactionList = new ArrayList<>();

        while (resultatList.moveToNext())
        {
            transactionList.add(new Transaction(resultatList.getInt(0), resultatList.getString(1), resultatList.getDouble(3), resultatList.getLong(4), resultatList.getLong(5), resultatList.getDouble(7)));
        }

        resultatList.close();

        db.close();

        return transactionList;
    }

    public void deleteTransactionFromId(int id)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_MANUAL_CURRENCIES, KEY_CURRENCY_ID + "=" + id, null);

        db.close();
    }
}
