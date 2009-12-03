package org.hyperbard.dialify;

import java.util.ArrayList;
import java.util.List;

import org.hyperbard.dialify.ContactsHelper.Contact;
import org.hyperbard.dialify.ContactsHelper.Sort;
import org.hyperbard.dialify.NotificationHelper.NotificationType;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import org.hyperbard.dialify.R;

/**
 * Main application activity.
 */
public class Dialify extends ListActivity {

	//menus
	private static final int MENU_HELP = 0;
	
	//dialogs
	private static final int DIALOG_HELP = 0;
	private static final int DIALOG_NO_CONTACTS = 1;
	private static final int DIALOG_TOO_MANY = 2;
	private static final int DIALOG_AT_MAX = 3;
	private static final int DIALOG_SELECT_NOTIFICATION_TYPE = 4;

	private SelectionManager _selectionManager;
	private NotificationHelper _notificationHelper;
	private ContactCursorAdapter _contactAdapter;
	
	//we need to keep track of these so they're available to the notification type selection dialog's onClick listener
	private CheckedTextView _selectedContactNameView;
	private Contact _selectedContact;
	
	private Cursor _contactsCursor;
	
	private ContactsHelper _contactsHelper;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		_selectionManager = new SelectionManager(this);
		_notificationHelper = new NotificationHelper(this);
		_contactsHelper = new ContactsHelper(this);
		
		cleanNotifications();
		
		_contactsCursor = _contactsHelper.getContactsCursor(Sort.ASC);
		startManagingCursor(_contactsCursor);

		//see if they have contacts
		if (_contactsCursor.getCount() == 0) {
			showDialog(DIALOG_NO_CONTACTS);
			return;
		}

		//create adapter to display contacts
		_contactAdapter = new ContactCursorAdapter(this, R.layout.contact, _contactsCursor, _selectionManager);
		setListAdapter(_contactAdapter);
		
		// create the listener that is invoked when the user clicks a contact
		OnItemClickListener listener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				_selectedContact = _contactsHelper.getContactAtCursor(_contactsCursor);
				
				//warn the user if they have already selected the maximum allowed and are clicking an unselected contact
				boolean notSelected = _selectionManager.getNumSelectionsForContact(_selectedContact.getId()) == 0;
				boolean atMax = _selectionManager.getNumSelections() == SelectionManager.MAX_SELECTIONS;
				
				if (notSelected && atMax) {
					showDialog(DIALOG_AT_MAX);
					return;
				}
				
				_selectedContactNameView = (CheckedTextView)view.findViewById(R.id.contact_name);
				showDialog(DIALOG_SELECT_NOTIFICATION_TYPE);
			}
		};

		ListView listView = getListView();
		listView.setOnItemClickListener(listener);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		cleanNotifications();
	}
	
	private void cleanNotifications() {
		NotificationCleaner cleaner = new NotificationCleaner(_contactsHelper, _selectionManager, _notificationHelper);
		cleaner.clean();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_HELP, 0, R.string.help).setIcon(android.R.drawable.ic_menu_help);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_HELP:
			showDialog(DIALOG_HELP);
			return true;
		}
		
		return false;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_HELP: return AlertForString(R.string.help_content);
		case DIALOG_NO_CONTACTS: return AlertForString(R.string.no_contacts);
		case DIALOG_TOO_MANY: return AlertForString(R.string.too_many);
		case DIALOG_AT_MAX: return AlertForString(R.string.at_max);
		case DIALOG_SELECT_NOTIFICATION_TYPE:
			return new AlertDialog.Builder(Dialify.this)
				.setTitle(R.string.select_notification_type_title)
				.setItems(R.array.notification_selections, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						boolean create = false;
						List<NotificationType> types = new ArrayList<NotificationType>();
						
						switch (which) {
						case 0: //call
							create = true;
							types.add(NotificationType.CALL);
							break;
						case 1: //text
							create = true;
							types.add(NotificationType.TEXT);
							break;
						case 2: //both
							create = true;
							
							//notifications are created in the order specified from the bottom up
							//this order makes sure that CALL is on top in the notification list
							types.add(NotificationType.TEXT);
							types.add(NotificationType.CALL);
							break;
						case 3: //none
							//remove existing selections and notifications
							removeNotificationsAndDeleteSelectionsForContact(_selectedContact.getId());
							
							_selectedContactNameView.setChecked(false);
							break;
						}
						
						if (create) {
							//ensure we would not max out
							int delta =
								types.size() - _selectionManager.getNumSelectionsForContact(_selectedContact.getId());
							
							//warn the user if their selections would exceed the max allowed
							if (_selectionManager.wouldExceedMaxSelections(delta)) {
								showDialog(DIALOG_TOO_MANY);
								return;
							}
							
							//remove existing selections and notifications
							removeNotificationsAndDeleteSelectionsForContact(_selectedContact.getId());
							
							//note the settings in prefs and create the notifications
							for (NotificationType type : types) {
								_selectionManager.setSelection(_selectedContact.getId(), type);
							}
							
							cleanNotifications();
							
							_selectedContactNameView.setChecked(true);
						}
						
					}
			}).create();
		default: throw new IllegalArgumentException("No such dialog");
		}
	}
	
	private void removeNotificationsAndDeleteSelectionsForContact(long contactId) {
		for (int notificationId : _selectionManager.getNotificationIdsForContact(contactId)) {
			_notificationHelper.removeNotification(notificationId);
		}
		
		_selectionManager.deleteSelectionsForContact(contactId);
	}
	
	/** @return A simple alert w/OK button for the given string ID */
	private Dialog AlertForString(int stringId) {
		return new AlertDialog.Builder(this)
			.setMessage(stringId)
			.setPositiveButton(R.string.OK, null)
			.create();
	}
}