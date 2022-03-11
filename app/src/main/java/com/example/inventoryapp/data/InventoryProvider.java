package com.example.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InventoryProvider extends ContentProvider {

    private static final int ITEMS = 100;

    private static final int ITEM_ID = 101;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    static {

        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, ITEMS);

        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", ITEM_ID);

    }

    private InventoryDbHelper dbHelper;


    @Override
    public boolean onCreate() {
        dbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        SQLiteDatabase database = dbHelper.getReadableDatabase();

        Cursor cursor;

        int match = uriMatcher.match(uri);
        switch (match) {
            case ITEMS: {
                cursor = database.query(InventoryContract.InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
            case ITEM_ID: {
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(InventoryContract.InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return InventoryContract.InventoryEntry.CONTENT_LIST_TYPE;
            case ITEM_ID:
                return InventoryContract.InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match =uriMatcher.match(uri);
        if (match == ITEMS) {
            return insertItem(uri, contentValues);
        }
        throw new IllegalArgumentException("Insertion is not supported for " + uri);
    }

    private Uri insertItem(Uri uri, ContentValues values) {

        if(values.getAsString(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME) == null) {
            throw new IllegalArgumentException("Item requires a name");
        }

        Integer quantity = values.getAsInteger(InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY);
        if(quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Item requires valid quantity");
        }

        Double price = values.getAsDouble(InventoryContract.InventoryEntry.COLUMN_ITEM_PRICE);
        if(price == null || price < 0) {
            throw new IllegalArgumentException("Item requires valid price");
        }

        if(values.getAsString(InventoryContract.InventoryEntry.COLUMN_ITEM_SUPPLIER) == null) {
            throw new IllegalArgumentException("Item requires a supplier name");
        }

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        long id = database.insert(InventoryContract.InventoryEntry.TABLE_NAME, null, values);

        if(id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        int rowsDeleted;

        final int match = uriMatcher.match(uri);
        switch (match) {
            case ITEMS: {
                rowsDeleted = database.delete(InventoryContract.InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case ITEM_ID: {
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(InventoryContract.InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if(rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case ITEMS: {
                return updateItem(uri, contentValues, selection, selectionArgs);
            }
            case ITEM_ID: {
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateItem(uri, contentValues, selection, selectionArgs);
            }
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if(values.containsKey(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME)) {
            if(values.getAsString(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME) == null) {
                throw new IllegalArgumentException("Item requires a name");
            }
        }

        if(values.containsKey(InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY)){
            Integer quantity = values.getAsInteger(InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY);
            if(quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Item requires valid quantity");
            }
        }

        if(values.containsKey(InventoryContract.InventoryEntry.COLUMN_ITEM_PRICE)) {
            Double price = values.getAsDouble(InventoryContract.InventoryEntry.COLUMN_ITEM_PRICE);
            if (price == null || price < 0) {
                throw new IllegalArgumentException("Item requires valid price");
            }
        }

        if(values.containsKey(InventoryContract.InventoryEntry.COLUMN_ITEM_SUPPLIER)) {
            if (values.getAsString(InventoryContract.InventoryEntry.COLUMN_ITEM_SUPPLIER) == null) {
                throw new IllegalArgumentException("Item requires a supplier name");
            }
        }

        if(values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        int rowsUpdated = database.update(InventoryContract.InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

        if(rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

}
