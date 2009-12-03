package org.hyperbard.dialify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Recreates selected notifications at startup.
 */
public class StartupIntentReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//recreate notifications
		NotificationCleaner cleaner = new NotificationCleaner(
				new ContactsHelper(context),
				new SelectionManager(context),
				new NotificationHelper(context)
		);
		
		cleaner.clean();
	}
	
}
