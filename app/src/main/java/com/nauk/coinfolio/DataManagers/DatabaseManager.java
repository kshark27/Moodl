package com.nauk.coinfolio.DataManagers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.nauk.coinfolio.DataManagers.CurrencyData.Currency;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Guitoune on 14/01/2018.
 */

public class DatabaseManager extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 4;

    private static final String DATABASE_NAME = "Currencies.db";

    private static final String TABLE_MANUAL_CURRENCIES = "ManualCurrencies";
    private static final String TABLE_EXCHANGE_KEYS = "ExchangeKeys";

    private static final String KEY_CURRENCY_ID = "idCurrency";
    private static final String KEY_CURRENCY_SYMBOL = "symbol";
    private static final String KEY_CURRENCY_NAME = "name";
    private static final String KEY_CURRENCY_BALANCE = "balance";
    private static final String KEY_CURRENCY_DATE = "addDate";
    private static final String KEY_CURRENCY_PURCHASED_PRICE = "purchasedPrice";
    private static final String KEY_CURRENCY_IS_MINED = "isMined";

    private static final String KEY_EXCHANGE_ID = "idExchange";
    private static final String KEY_EXCHANGE_NAME = "name";
    private static final String KEY_EXCHANGE_PUBLIC_KEY = "publicKey";
    private static final String KEY_EXCHANGE_SECRET_KEY = "secretKey";

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
                + KEY_CURRENCY_DATE + " DATE,"
                + KEY_CURRENCY_PURCHASED_PRICE + " TEXT,"
                + KEY_CURRENCY_IS_MINED + " INTEGER"
                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXCHANGE_KEYS + "("
                + KEY_EXCHANGE_ID + " INTEGER PRIMARY KEY,"
                + KEY_EXCHANGE_NAME + " TEXT,"
                + KEY_EXCHANGE_PUBLIC_KEY + " TEXT,"
                + KEY_EXCHANGE_SECRET_KEY + " TEXT"
                + ");");

        //loadSample(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MANUAL_CURRENCIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXCHANGE_KEYS);

        onCreate(db);
    }

    public void addCurrencyToManualCurrency(String symbol, double balance)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_CURRENCY_SYMBOL, symbol);
        values.put(KEY_CURRENCY_BALANCE, balance);
        //values.put(KEY_CURRENCY_DATE, getDate());
        //values.put(KEY_CURRENCY_PURCHASED_PRICE, something);

        db.insert(TABLE_MANUAL_CURRENCIES, null, values);
        db.close();
    }

    public List<Currency> getAllCurrencyFromManualCurrency()
    {
        String searchQuerry = "SELECT * FROM " + TABLE_MANUAL_CURRENCIES;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resultatList = db.rawQuery(searchQuerry, null);

        List<Currency> currencyList = new ArrayList<>();

        while(resultatList.moveToNext())
        {
            //Currency currency = new Currency(resultatList.getString(1), resultatList.getString(2));
            Log.d("CrystalVault", "Database result : " + resultatList.getString(1) + " " + resultatList.getString(2) + " " + resultatList.getCount());
            //currencyList.add(new Currency(resultatList.getString(1), resultatList.getDouble(2)));
        }

        resultatList.close();

        db.close();

        return currencyList;
    }
}