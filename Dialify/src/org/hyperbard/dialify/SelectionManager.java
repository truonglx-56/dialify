package org.hyperbard.dialify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperbard.dialify.DatabaseHelper.Selections;
import org.hyperbard.dialify.NotificationHelper.NotificationType;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 * Provides control of and information on notification selections.
 */
public class SelectionManager {

	/** Represents a notification selection for a contact. */
	public static class Selection {
		private int _notificationId;
		public int getNotificationId() { return _notificationId; }
		
		private long _contactId;
		public long getContactId() { return _contactId; }
		
		private NotificationType _type;
		public NotificationType getType() { return _type; }
		
		public Selection(int notificationId, long contactId, NotificationType type) {
			_notificationId = notificationId;
			_contactId = contactId;
			_type = type;
		}
	};
	
	public static final int MAX_SELECTIONS = 10;
	
	private int _numSelections;
	
	/** created with size MAX_SELECTIONS, each position indicates whether that ID is in use */
	private boolean[] _notificationIds;
	
	//maps contact IDs to their selections
	private Map<Long, List<Selection>> _selections;
	
	private DatabaseHelper _database;
	
	private static final String[] PROJECTION_SELECTIONS = {
		Selections.COLUMN_NOTIFICATION_ID,   //0
		Selections.COLUMN_CONTACT_ID,        //1
		Selections.COLUMN_NOTIFICATION_TYPE  //2
	};
	
	private static final String SORT_SELECTIONS =
		Selections.COLUMN_CONTACT_ID + ","
		+ Selections.COLUMN_NOTIFICATION_TYPE + " DESC";
	
	public SelectionManager(Context context) {
		_database = new DatabaseHelper(context);
		loadSelections();
	}

	/** @return the next available notification ID */
	private int getNextNotificationId() {
		for (int i = 0; i < MAX_SELECTIONS; i++) if (!_notificationIds[i]) return i;
		return -1;
	}
	
	private void addSelection(Selection selection) {
		if (!_selections.containsKey(selection.getContactId())) {
			_selections.put(selection.getContactId(), new ArrayList<Selection>());
		}
		
		_selections.get(selection.getContactId()).add(selection);
		_numSelections++;
		_notificationIds[selection.getNotificationId()] = true;
	}
	
	private void loadSelections() {
		_selections = new HashMap<Long, List<Selection>>();
		_numSelections = 0;
		_notificationIds = new boolean[MAX_SELECTIONS];
		
		Cursor cursor = _database.query(Selections.TABLE_NAME, PROJECTION_SELECTIONS, null, null, SORT_SELECTIONS);
		
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			
			do {
				int notificationId = cursor.getInt(0);
				long contactId = cursor.getLong(1);
				
				String notificationType = cursor.getString(2);
				NotificationType type = NotificationType.valueOf(notificationType);
				
				Selection selection = new Selection(notificationId, contactId, type);
				addSelection(selection);
				
				cursor.moveToNext();
			} while (!cursor.isAfterLast());
		}
		
		cursor.close();
	}
	
	/**
	 * Assumes that you have made sure you are not attempting to exceed {@link SelectionManager#MAX_SELECTIONS}.
	 * {@link SelectionManager#getSelectionsForContactId(long)} will return selections in the order you set them.
	 * @return the notification ID for the selection
	 */
	public int setSelection(long contactId, NotificationType type) {
		//if the selection has already been made just return the existing notification ID
		if (_selections.containsKey(contactId)) {
			for (Selection selection : _selections.get(contactId)) {
				if (selection.getType() == type) return selection.getNotificationId();
			}
		}
		
		int notificationId = getNextNotificationId();
		
		ContentValues values = new ContentValues();
		values.put(Selections.COLUMN_CONTACT_ID, contactId);
		values.put(Selections.COLUMN_NOTIFICATION_TYPE, type.toString());
		values.put(Selections.COLUMN_NOTIFICATION_ID, notificationId);
		
		_database.insert(Selections.TABLE_NAME, values);
		
		Selection selection = new Selection(notificationId, contactId, type);
		addSelection(selection);
		
		return notificationId;
	}
	
	/** @return the number of selections deleted */
	public long deleteSelectionsForContact(long contactId) {
		if (!_selections.containsKey(contactId)) return 0;
		
		//reclaim the notification IDs
		for (Selection selection : _selections.get(contactId)) _notificationIds[selection.getNotificationId()] = false;
		
		int removed = _selections.get(contactId).size();
		_selections.remove(contactId);
		_numSelections -= removed;
		
		if (removed > 0) {
			_database.delete(
					Selections.TABLE_NAME,
					Selections.COLUMN_CONTACT_ID + "=?",
					new String[] { String.valueOf(contactId) }
			);
		}
		
		return removed;
	}
	
	/** @return an unmodifiable list of selections for the given contact, or null if none exist */
	public List<Selection> getSelectionsForContactId(long contactId) {
		if (!_selections.containsKey(contactId)) return null;
		return Collections.unmodifiableList(_selections.get(contactId));
	}
	
	/** @return a list of contact IDs that have selections */
	public List<Long> getContactIdsInUse() {
		List<Long> contactIds = new ArrayList<Long>();
		
		for (long contactId : _selections.keySet()) {
			if (_selections.get(contactId).size() > 0) contactIds.add(contactId);
		}
		
		return contactIds;
	}
	
	public int getNumSelections() {
		return _numSelections;
	}
	
	public int getNumSelectionsForContact(long contactId) {
		if (!_selections.containsKey(contactId)) return 0;
		return _selections.get(contactId).size();
	}
	
	public List<Integer> getNotificationIdsForContact(long contactId) {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		if (_selections.containsKey(contactId)) {
			for (Selection selection : _selections.get(contactId)) {
				ids.add(selection.getNotificationId());
			}
		}
		
		return ids;
	}
	
	/** @return true if the current number of selections plus the provided delta would exceed the maximum allowed */
	public boolean wouldExceedMaxSelections(int delta) {
		return _numSelections + delta > MAX_SELECTIONS;
	}
	
	/** @return true if any selections are set for the given contact ID */
	public boolean isSelected(long contactId) {
		return _selections.containsKey(contactId) && _selections.get(contactId).size() > 0;
	}
	
}
