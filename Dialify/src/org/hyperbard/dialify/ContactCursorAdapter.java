package org.hyperbard.dialify;

import org.hyperbard.dialify.ContactsHelper.Contact;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import org.hyperbard.dialify.R;

/**
 * Binds data from a contacts cursor (returned by methods in {@link ContactsHelper}) to the contact.xml view.
 */
public class ContactCursorAdapter extends ResourceCursorAdapter {

	private SelectionManager _selectionManager;
	private ContactsHelper _contactsHelper;
	
	public ContactCursorAdapter(
			Context context,
			int layout,
			Cursor cursor,
			boolean autoRequery,
			SelectionManager selectionManager
	) {
		super(context, layout, cursor, autoRequery);
		init(context, selectionManager);
	}

	public ContactCursorAdapter(Context context, int layout, Cursor cursor, SelectionManager selectionManager) {
		super(context, layout, cursor);
	 	init(context, selectionManager);
	}

	private void init(Context context, SelectionManager selectionManager) {
		_selectionManager = selectionManager;
		_contactsHelper = new ContactsHelper(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Contact contact = _contactsHelper.getContactAtCursor(cursor);
		
		//set the contact name
		CheckedTextView contactNameView = (CheckedTextView)view.findViewById(R.id.contact_name);
		contactNameView.setText(contact.getDisplayName());
		contactNameView.setChecked(_selectionManager.isSelected(contact.getId()));
		
		//set the contact type
		TextView contactTypeView = (TextView)view.findViewById(R.id.contact_type);
		contactTypeView.setText(contact.getType());
		
		//set the contact number
		TextView contactNumberView = (TextView)view.findViewById(R.id.contact_number);
		contactNumberView.setText(contact.getNumber());
	}
	
}
