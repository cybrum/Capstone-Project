
package com.udacity.nanodegree.mystockhealth.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Reference: https://github.com/SimonVT/schematic
 * Schematic: Automatically generate a ContentProvider backed by an SQLite database.
 * Usage: First create a class that contains the columns of a database table.
 * Credit: Dmitry Malkovich , sam_chordas
 */
public class QuoteColumns {
    @DataType(DataType.Type.INTEGER)
    @PrimaryKey
    @AutoIncrement
    public static final String _ID = "_id";
    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String SYMBOL = "symbol";
    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String PERCENT_CHANGE = "percent_change";
    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String CHANGE = "change";
    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String BIDPRICE = "bid_price";
    @DataType(DataType.Type.TEXT)
    public static final String CREATED = "created";
    @DataType(DataType.Type.INTEGER)
    @NotNull
    public static final String ISUP = "is_up";
    @DataType(DataType.Type.INTEGER)
    @NotNull
    public static final String ISCURRENT = "is_current";
    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String QUANTITY = "quantity";
    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String PURCHASE_COST = "cost";
    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String NAME = "name";
}
