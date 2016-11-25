
package com.udacity.nanodegree.mystockhealth.network;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Dmitry Malkovich on 4/10/16.
 * For storing response from Yahoo API.
 */
@SuppressWarnings("unused")
public class StockQuote {

    @SerializedName("Change")
    private String mChange;

    @SerializedName("symbol")
    private String mSymbol;

    @SerializedName("Name")
    private String mName;

    @SerializedName("Bid")
    private String mBid;

    @SerializedName("ChangeinPercent")
    private String mChangeInPercent;


    @SerializedName("Quantity")
    private String mQuantity;

    @SerializedName("Cost")
    private String mCost;

    public String getQuantity() {
        return mQuantity;
    }

    public String getCost() {
        return mCost;
    }

    public String getChange() {
        return mChange;
    }

    public String getBid() {
        return mBid;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public String getChangeInPercent() {
        return mChangeInPercent;
    }

    public String getName() {
        return mName;
    }
}
