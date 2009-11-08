package com.hyperbard.dialify;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;

/**
 * Provides access to contact information.
 */
public class ContactsHelper {

	/** Represents a contact. */
	public static class Contact {
		private long _id;
		public long getId() { return _id; }
		
		private long _personId;
		public long getPersonId() { return _personId; }
		
		private String _displayName;
		public String getDisplayName() { return _displayName; }
		
		private String _number;
		public String getNumber() { return _number; }

		private String _type;
		/** @return the contact type or custom label **/
		public String getType() { return _type; }
		
		public Contact(long id, long personId, String displayName, String number, String type) {
			_id = id;
			_personId = personId;
			_displayName = displayName;
			_number = number;
			_type = type;
		}
		
	};
	
	/** Used to specify the sort order to use when retrieving contacts. */
	public enum Sort {
		/** Appropriate for a cursor used to show all contacts */
		ASC(SORT_PHONES_ASC),
		
		/** Appropriate for a cursor used to create notifications (as the last one created goes at the top) */
		DESC(SORT_PHONES_DESC);
		
		private String _sort;
		public String getSort() { return _sort; }
		
		private Sort(String sort) { _sort = sort; }
	}

	private Context _context;
	
	private String[] _contactTypes;
	
	//contact query columns
	private static final String[] PROJECTION_PHONES = new String[] {
		Phones._ID,          //0
		Phones.DISPLAY_NAME, //1
		Phones.TYPE,         //2
		Phones.LABEL,        //3
		Phones.NUMBER,       //4
		Phones.PERSON_ID     //5
	};

	private static final String SORT_PHONES_DESC =
		"upper(" + Phones.DISPLAY_NAME + ") DESC,"
		+ Phones.TYPE + " DESC,"
		+ "upper(" + Phones.LABEL + ") DESC";
	
	private static final String SORT_PHONES_ASC =
		"upper(" + Phones.DISPLAY_NAME + "),"
		+ Phones.TYPE + ","
		+ "upper(" + Phones.LABEL + ")";
	
	/** A utility for returning a contact's photo **/
	public static Bitmap getPhoto(Context _context, Contact contact) {
		Uri contactUri = Uri.parse("content://contacts/people/" + contact.getPersonId());
		return People.loadContactPhoto(_context, contactUri, R.drawable.icon, null);
	}
	
	public ContactsHelper(Context context) {
		_context = context;

		//get the list of contact types
		final Resources resources = context.getResources();
		_contactTypes = resources.getStringArray(android.R.array.phoneTypes);
	}
	
	/** @return a {@link Contact} representing the row at the current cursor position **/
	public Contact getContactAtCursor(Cursor cursor) {
		//determine type
		int typeId = cursor.getInt(2);
		String type = (typeId == Phones.TYPE_CUSTOM) ? cursor.getString(3) : _contactTypes[typeId - 1];
		
		return new Contact(
				cursor.getLong(0),   //id
				cursor.getLong(5),   //person ID
				cursor.getString(1), //display name
				cursor.getString(4), //number
				type
		);
	}
	
	/** @return a cursor for all contacts */
	public Cursor getContactsCursor(Sort sort) {
		return query(null, sort);
	}
	
	/** @return a cursor for the specified contacts, or all contacts if contactIds is null or an empty list */
	public Cursor getContactsCursor(List<Long> contactIds, Sort sort) {
		return query(contactIds, sort);
	}
	
	/** @param contactIds if null, returns all contacts */
	private Cursor query(List<Long> contactIds, Sort sort) {
		String selection = null;
		String[] selectionArgs = null;
		
		//build up the selection if contact IDs were provided
		if (contactIds != null && contactIds.size() > 0) {
			selectionArgs = new String[contactIds.size()];
			StringBuilder sb = new StringBuilder();
			
			for (int i = 0; i < contactIds.size(); i++) {
				long contactId = contactIds.get(i);
				selectionArgs[i] = String.valueOf(contactId);
				
				if (i > 0) {
					sb.append("or ");
				}
				
				sb.append("Phones._ID=?");
			}
			
			selection = sb.toString();
		}
		
		return _context.getContentResolver().query(
				Phones.CONTENT_URI,
				PROJECTION_PHONES,
				selection,
				selectionArgs,
				sort.getSort()
		);
	}
	
}
