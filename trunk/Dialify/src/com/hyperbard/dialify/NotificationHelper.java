package com.hyperbard.dialify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.hyperbard.dialify.ContactsHelper.Contact;

/**
 * Utility for creating and removing notifications.
 */
public class NotificationHelper {

	/** Notification types and their associated scheme (for URI generation) and action (for Intent creation). */
	public enum NotificationType {
		CALL("tel:", Intent.ACTION_CALL, R.string.call),
		TEXT("smsto:", Intent.ACTION_SENDTO, R.string.text);
		
		private String _scheme;
		private String _action;
		private int _verbStringId;
		
		private NotificationType(String scheme, String action, int verbStringId) {
			_scheme = scheme;
			_action = action;
			_verbStringId = verbStringId;
		}
		
		/** @return the scheme used to construct intent URIs */
		public String getScheme() { return _scheme; }
		
		/** @return the action used to create intents */
		public String getAction() { return _action; }
		
		/** @return the ID of the string to use in the notification layout to indicate its type */
		public int getVerbStringId() { return _verbStringId; }
	}
	
	private Context _context;

	NotificationManager _manager;
	
	public NotificationHelper(Context context) {
		_context = context;
		_manager = (NotificationManager)_context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	/** Creates a notification of the specified type for a contact. */
	public void createNotification(NotificationType type, int notificationId, Contact contact) {
		Notification notification = new Notification(-1, null, System.currentTimeMillis());

		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		RemoteViews contentView = new RemoteViews(_context.getPackageName(), R.layout.notification);

		//set the photo
		contentView.setImageViewBitmap(R.id.notification_contact_photo, ContactsHelper.getPhoto(_context, contact));
		
		//set the verb, i.e. "call" or "text"
		contentView.setTextViewText(R.id.notification_notification_type, _context.getString(type.getVerbStringId()));

		//set the name and type
		contentView.setTextViewText(R.id.notification_contact_name, contact.getDisplayName());
		contentView.setTextViewText(R.id.notification_contact_type, contact.getType());
		
		//set the number
		contentView.setTextViewText(R.id.notification_contact_number, contact.getNumber());
		
		notification.contentView = contentView;

		//create the intent that will fire when the contact is selected
		Intent intent = new Intent(type.getAction(), Uri.parse(type.getScheme() + contact.getNumber()));
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, intent, 0);
		notification.contentIntent = contentIntent;

		_manager.notify(notificationId, notification);
	}
	
	public void removeNotification(int notificationId) {
		_manager.cancel(notificationId);
	}
	
	public void removeAllNotifications() {
		_manager.cancelAll();
	}
	
}
