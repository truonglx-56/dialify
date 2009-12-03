package org.hyperbard.dialify;

import java.util.List;

import org.hyperbard.dialify.ContactsHelper.Contact;
import org.hyperbard.dialify.ContactsHelper.Sort;
import org.hyperbard.dialify.SelectionManager.Selection;

import android.database.Cursor;


/**
 * Cleans up notifications by removing and recreating them.
 */
public class NotificationCleaner implements Runnable {
	
	ContactsHelper _contactsHelper;
	SelectionManager _selectionManager;
	NotificationHelper _notificationHelper;
	
	public NotificationCleaner(
			ContactsHelper contactsHelper,
			SelectionManager selectionManager,
			NotificationHelper notificationHelper
	) {
		_contactsHelper = contactsHelper;
		_selectionManager = selectionManager;
		_notificationHelper = notificationHelper;
	}
	
	/** Synchronous */
	public void run() {
		//remove all existing notifications
		_notificationHelper.removeAllNotifications();

		List<Long> contacts = _selectionManager.getContactIdsInUse();
		
		//don't bother continuing if the user had no notifications
		if (contacts.size() == 0) return;
		
		Cursor cursor = _contactsHelper.getContactsCursor(contacts, Sort.DESC);
		
		//iterate over contacts in use, creating notifications
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			
			do {
				Contact contact = _contactsHelper.getContactAtCursor(cursor);
				
				for (Selection selection : _selectionManager.getSelectionsForContactId(contact.getId())) {
					_notificationHelper.createNotification(selection.getType(), selection.getNotificationId(), contact);
				}
				
				contacts.remove(contact.getId()); //note which contacts are used
				cursor.moveToNext();
			} while (!cursor.isAfterLast());
		}
		
		//remaining contacts were deleted (we had selections for them but they weren't returned by the query)
		for (long contactId : contacts) {
			_selectionManager.deleteSelectionsForContact(contactId);
		}
		
		cursor.close();
	}
	
	/** Asynchronous: Spawns a thread to do the cleanup */
	public void clean() {
		Thread thread = new Thread(this);
		thread.start();
	}
	
}
