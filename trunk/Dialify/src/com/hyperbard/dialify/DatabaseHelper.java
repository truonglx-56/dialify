package com.hyperbard.dialify;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

/**
 * Provides access to the application's database and information on its tables.
 */
public class DatabaseHelper {

	/** Contains constants related to the selections table. */
	public static class Selections {
		public static final String TABLE_NAME = "selections";
		public static final String COLUMN_ID = "id";
		public static final String COLUMN_NOTIFICATION_ID = "notification_id";
		public static final String COLUMN_CONTACT_ID = "contact_id";
		public static final String COLUMN_NOTIFICATION_TYPE = "type";
	}
	
	//database identification
	private static final String DATABASE_NAME = "dialify.db";
	private static final int DATABASE_VERSION = 1;

	//create projection maps
	private static HashMap<String, HashMap<String, String>> PROJECTION_MAPS;
	
	static {
		PROJECTION_MAPS = new HashMap<String, HashMap<String,String>>();
		
		HashMap<String, String> selectionsMap = new HashMap<String, String>();
		selectionsMap.put(Selections.COLUMN_ID, Selections.COLUMN_ID);
		selectionsMap.put(Selections.COLUMN_NOTIFICATION_ID, Selections.COLUMN_NOTIFICATION_ID);
		selectionsMap.put(Selections.COLUMN_CONTACT_ID, Selections.COLUMN_CONTACT_ID);
		selectionsMap.put(Selections.COLUMN_NOTIFICATION_TYPE, Selections.COLUMN_NOTIFICATION_TYPE);
		
		PROJECTION_MAPS.put(Selections.TABLE_NAME, selectionsMap);
	}
	
	private OpenHelper _openHelper;
	
	private static class OpenHelper extends SQLiteOpenHelper {

		public OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(
				"CREATE TABLE " + Selections.TABLE_NAME + " ("
					+ Selections.COLUMN_ID + " INTEGER PRIMARY KEY,"
					+ Selections.COLUMN_NOTIFICATION_ID + " INTEGER,"
					+ Selections.COLUMN_CONTACT_ID + " INTEGER,"
					+ Selections.COLUMN_NOTIFICATION_TYPE + " TEXT"
				+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			//nothing to upgrade at the moment
		}
	}
	
	public DatabaseHelper(Context context) {
		_openHelper = new OpenHelper(context);
	}
	
	/**
	 * Performs a query against the specified table.
	 * @param projection The columns to select
	 * @param selection The where clause sans "where" and with "?" in place of values
	 * @param selectionArgs The values that will replace "?", in order, in the selection
	 * @param sortOrder The order by clause sans "order by"
	 */
	public Cursor query(
			String tableName,
			String[] projection,
			String selection,
			String[] selectionArgs,
			String sortOrder
	) {
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(tableName);
		query.setProjectionMap(PROJECTION_MAPS.get(tableName));
		
		SQLiteDatabase db = _openHelper.getReadableDatabase();
		return query.query(db, projection, selection, selectionArgs, null, null, sortOrder);
	}

	/**
	 * Inserts a new row into the specified table.
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long insert(String tableName, ContentValues values) {
		SQLiteDatabase db = _openHelper.getWritableDatabase();
		InsertHelper insert = new InsertHelper(db, tableName);
		return insert.insert(values);
	}
	
	/**
	 * Inserts a new row or replaces a conflicting row (i.e. if an existing primary key is provided).
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long replace(String tableName, ContentValues values) {
		SQLiteDatabase db = _openHelper.getWritableDatabase();
		InsertHelper insert = new InsertHelper(db, tableName);
		return insert.replace(values);		
	}
	
	/**
	 * Deletes rows from the specified table.
	 * @param whereClause The where clause sans "where" and with "?" in place of values. Omit to delete all rows.
	 * @param whereArgs The values that will replece "?", in order, in the whereClause
	 * @return the number of rows affected if a whereClause is passed in, 0 otherwise. To remove all rows and get a
	 *         count pass "1" as the whereClause.
	 */
	public long delete(String tableName, String whereClause, String[] whereArgs) {
		SQLiteDatabase db = _openHelper.getWritableDatabase();
		return db.delete(tableName, whereClause, whereArgs);
	}
	
}
