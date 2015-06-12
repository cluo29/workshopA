package com.example.ivan.SmartVolume;


/**
 * Created by CLUO on 6/10/2015.
 */
        import java.util.HashMap;
        import android.content.ContentProvider;
        import android.content.ContentUris;
        import android.content.ContentValues;
        import android.content.UriMatcher;
        import android.database.Cursor;
        import android.database.SQLException;
        import android.database.sqlite.SQLiteDatabase;
        import android.database.sqlite.SQLiteQueryBuilder;
        import android.net.Uri;
        import android.os.Environment;
        import android.provider.BaseColumns;
        import android.util.Log;

        import com.aware.Aware;
        import com.aware.utils.DatabaseHelper;

public class Provider extends ContentProvider {
    public static final int DATABASE_VERSION = 1;
    public static String AUTHORITY = "com.example.ivan.SmartVolume.provider.SmartVolume";
    private static final int SMARTVOLUME = 1;
    private static final int SMARTVOLUME_ID = 2;
    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory()+"/AWARE/SmartVolume.db";

    public static final String[] DATABASE_TABLES = {
            "SmartVolume"
    };
    public static final class Smart_Volume_Data implements BaseColumns {
        private Smart_Volume_Data(){};

        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/SmartVolume");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.SmartVolume";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.SmartVolume";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String A_VALUES_0 = "double_A_values_0";   //x axis of accelerometer: m/s^2
        public static final String A_VALUES_1 = "double_A_values_1";    //y
        public static final String A_VALUES_2 = "double_A_values_2";    //z
        public static final String R_VALUES_0 = "double_R_values_0";   //x axis of rotation: sin(?/2)
        public static final String R_VALUES_1 = "double_R_values_1";    //y
        public static final String R_VALUES_2 = "double_R_values_2";    //z
        public static final String LUX = "double_Lux";    //Light sensor
        public static final String PROXIMITY = "double_Proximity";    //Proximity sensor
        public static final String MICROPHONE = "double_Microphone";    //Microphone, for ambient noise
        public static final String VOLUME = "Volume";    //Volume of phone 0-15
        public static final String LABEL = "Label";
    }
    public static final String[] TABLES_FIELDS = {
            Smart_Volume_Data._ID + " integer primary key autoincrement," +
                    Smart_Volume_Data.TIMESTAMP + " real default 0," +
                    Smart_Volume_Data.DEVICE_ID + " text default ''," +
                    Smart_Volume_Data.A_VALUES_0 + " real default 0," +
                    Smart_Volume_Data.A_VALUES_1 + " real default 0," +
                    Smart_Volume_Data.A_VALUES_2 + " real default 0," +
                    Smart_Volume_Data.R_VALUES_0 + " real default 0," +
                    Smart_Volume_Data.R_VALUES_1 + " real default 0," +
                    Smart_Volume_Data.R_VALUES_2 + " real default 0," +
                    Smart_Volume_Data.LUX + " real default 0," +
                    Smart_Volume_Data.PROXIMITY + " real default 0," +
                    Smart_Volume_Data.MICROPHONE + " real default 0," +
                    Smart_Volume_Data.VOLUME + " integer default 0," +
                    Smart_Volume_Data.LABEL + " text default ''," +

                    "UNIQUE("+ Smart_Volume_Data.TIMESTAMP+","+ Smart_Volume_Data.DEVICE_ID+")"
    };
    private static UriMatcher URIMatcher;
    private static HashMap<String, String> databaseMap;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    @Override
    public boolean onCreate() {
        Log.d("DATABASE NAME", "DEFAULT AUTHORITY: "+ AUTHORITY );
        AUTHORITY = getContext().getPackageName() + ".provider.SmartVolume";
        Log.d("DATABASE", "Authority set up after onCreate "+AUTHORITY);
        URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], SMARTVOLUME);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", SMARTVOLUME_ID);
        databaseMap = new HashMap<String, String>();
        databaseMap.put(Smart_Volume_Data._ID, Smart_Volume_Data._ID);
        databaseMap.put(Smart_Volume_Data.TIMESTAMP, Smart_Volume_Data.TIMESTAMP);
        databaseMap.put(Smart_Volume_Data.DEVICE_ID, Smart_Volume_Data.DEVICE_ID);
        databaseMap.put(Smart_Volume_Data.A_VALUES_0, Smart_Volume_Data.A_VALUES_0);
        databaseMap.put(Smart_Volume_Data.A_VALUES_1, Smart_Volume_Data.A_VALUES_1);
        databaseMap.put(Smart_Volume_Data.A_VALUES_2, Smart_Volume_Data.A_VALUES_2);
        databaseMap.put(Smart_Volume_Data.R_VALUES_0, Smart_Volume_Data.R_VALUES_0);
        databaseMap.put(Smart_Volume_Data.R_VALUES_1, Smart_Volume_Data.R_VALUES_1);
        databaseMap.put(Smart_Volume_Data.R_VALUES_2, Smart_Volume_Data.R_VALUES_2);
        databaseMap.put(Smart_Volume_Data.LUX, Smart_Volume_Data.LUX);
        databaseMap.put(Smart_Volume_Data.PROXIMITY, Smart_Volume_Data.PROXIMITY);
        databaseMap.put(Smart_Volume_Data.MICROPHONE, Smart_Volume_Data.MICROPHONE);
        databaseMap.put(Smart_Volume_Data.VOLUME, Smart_Volume_Data.VOLUME);
        databaseMap.put(Smart_Volume_Data.LABEL, Smart_Volume_Data.LABEL);

        return true;
    }

    private boolean initializeDB() {

        if (databaseHelper == null) {
            Log.d("DATABASE","DatabaseHelper is null");
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );


        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();

        }
        return( database != null && databaseHelper != null);
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case SMARTVOLUME:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (URIMatcher.match(uri)) {
            case SMARTVOLUME:
                return Smart_Volume_Data.CONTENT_TYPE;
            case SMARTVOLUME_ID:
                return Smart_Volume_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (URIMatcher.match(uri)) {
            case SMARTVOLUME:
                Log.d("DATABASE",database.getPath());
                long weather_id = database.insert(DATABASE_TABLES[0], Smart_Volume_Data.DEVICE_ID, values);

                if (weather_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            Smart_Volume_Data.CONTENT_URI,
                            weather_id);
                    getContext().getContentResolver().notifyChange(new_uri,
                            null);
                    return new_uri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URIMatcher.match(uri)) {
            case SMARTVOLUME:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case SMARTVOLUME:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
