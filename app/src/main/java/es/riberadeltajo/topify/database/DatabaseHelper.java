package es.riberadeltajo.topify.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "usuarios";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_LAST_LOGIN = "last_login";
    public static final String COLUMN_LAST_LOGOUT = "last_logout";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_IMAGE = "image";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_EMAIL + " TEXT NOT NULL UNIQUE, ";
        db.execSQL(CREATE_USERS_TABLE);

    }

    public synchronized void insertOrUpdateUser(String userId, String name, String email) {
        SQLiteDatabase db = getWritableDatabase();


        Cursor cursor = db.rawQuery("SELECT " + COLUMN_USER_ID + " FROM " + TABLE_USERS +
                " WHERE " + COLUMN_USER_ID + " = ?", new String[]{userId});

        boolean existe = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_EMAIL, email);

        if (!existe) {
            db.insert(TABLE_USERS, null, values);
        } else {
            db.update(TABLE_USERS, values, COLUMN_USER_ID + "=?", new String[]{userId});
        }

        db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
