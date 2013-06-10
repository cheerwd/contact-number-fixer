package jemuillot.ContactNumberFixer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Contacts;
import android.provider.ContactsContract;

@SuppressWarnings("deprecation")
interface ContactsPhoneNumberResolver {
	public Uri getUri();

	public String getID();

	public String getNumber();

	public void update(ContentResolver cr, String id, ContentValues values);
}

@SuppressWarnings("deprecation")
class ContactsPhoneNumberResolver3 implements ContactsPhoneNumberResolver {

	@Override
	public String getID() {
		return Contacts.Phones._ID;
	}

	@Override
	public String getNumber() {
		return Contacts.Phones.NUMBER;
	}

	@Override
	public Uri getUri() {
		return Contacts.Phones.CONTENT_URI;
	}

	@Override
	public void update(ContentResolver cr, String id, ContentValues values) {
		cr.update(
				ContentUris.withAppendedId(Contacts.Phones.CONTENT_URI,
						Integer.parseInt(id)), values, null, null);

	}
}

@TargetApi(Build.VERSION_CODES.ECLAIR)
class ContactsPhoneNumberResolver7 implements ContactsPhoneNumberResolver {

	@Override
	public String getID() {
		return ContactsContract.CommonDataKinds.Phone._ID;
	}

	@Override
	public String getNumber() {
		return ContactsContract.CommonDataKinds.Phone.NUMBER;
	}

	@Override
	public Uri getUri() {
		return ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	}

	@Override
	public void update(ContentResolver cr, String id, ContentValues values) {
		String where = ContactsContract.CommonDataKinds.Phone._ID + " = " + id;
		cr.update(ContactsContract.Data.CONTENT_URI, values, where, null);
	}
}

public class FixContactService extends Service {

	protected static final int MSG_OPEN_CONTACTS = 0;

	protected static final int MSG_FIX_CONTACTS = 1;

	private static final int NID_FIX_PROGRESS = 0;

	public static final String FROM_SERVICE = "jemuillot.ContactNumberFixer.fromService";
	public static final String LOGS = "jemuillot.ContactNumberFixer.logs";

	ContactNumberFixer callback;
	private ContactNumberFixer newCallback;

	boolean bRemoveCountry = false;
	boolean bRemoveArea = false;

	String countryCode;
	private String areaCode;

	List<String> mobilePrefixList;

	@SuppressWarnings("deprecation")
	void setCallback(ContactNumberFixer cb) {

		if (callback == null) {
			callback = cb;

			bRemoveCountry = cb.bRemoveCountry;
			bRemoveArea = cb.bRemoveArea;
			countryCode = cb.countryCode;
			areaCode = cb.areaCode;
			mobilePrefixList = cb.mobilePrefixList;

			if (Integer.parseInt(Build.VERSION.SDK) >= 7)
				contactsPhoneNumberResolver = new ContactsPhoneNumberResolver7();
			else
				contactsPhoneNumberResolver = new ContactsPhoneNumberResolver3();

			isVisible = true;

		} else {

			if (finished_shown) {
				callback.finish();

				callback = null;
			} else {
				newCallback = cb;
			}
		}

	}

	public class FixContactBinder extends Binder {
		FixContactService getService() {
			return FixContactService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {

		if (intent.getBooleanExtra(ContactNumberFixer.KILLING_OLD, false)) {
			callback.finish();

			return null;

		}
		return mBinder;
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final FixContactBinder mBinder = new FixContactBinder();

	private NotificationManager mNM;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public void onCreate() {

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mHandler.sendEmptyMessage(MSG_OPEN_CONTACTS);
	}

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MSG_OPEN_CONTACTS: {

				if (callback == null)
					sendMessageDelayed(obtainMessage(MSG_OPEN_CONTACTS), 1000);
				else {

					if (openContacts())
						sendMessage(obtainMessage(MSG_FIX_CONTACTS));
					else
						FixContactService.this.stopSelf();

				}
			}

				break;
			case MSG_FIX_CONTACTS: {

				if (fixContacts())
					sendMessage(obtainMessage(MSG_FIX_CONTACTS));
				else
					FixContactService.this.stopSelf();
			}

				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

	private Cursor contactCursor;

	private ContactsPhoneNumberResolver contactsPhoneNumberResolver;

	private boolean finished;

	boolean finished_shown;

	private int idColumn;

	private int numberColumn;

	private String removeLast;

	private boolean isVisible;

	private ArrayList<String> logs;

	/**
	 * Get the list of all phone numbers
	 */
	private Cursor getPhoneList() {

		Uri uri = contactsPhoneNumberResolver.getUri();

		// ID & Number should be enough for the subsequence operations
		String[] projection = new String[] {
				contactsPhoneNumberResolver.getID(),
				contactsPhoneNumberResolver.getNumber() };

		return getContentResolver().query(uri, projection, null, null, null);

	}

	public boolean openContacts() {

		logs = new ArrayList<String>();

		contactCursor = getPhoneList();

		if (contactCursor.moveToFirst()) {

			idColumn = contactCursor.getColumnIndex(contactsPhoneNumberResolver
					.getID());

			numberColumn = contactCursor
					.getColumnIndex(contactsPhoneNumberResolver.getNumber());

			finished = false;

			return true;
		} else {
			finished = true;
			finished_shown = true;

			pushText(getString(R.string.noContact));

			return false;
		}
	}

	public boolean fixContacts() {

		if (newCallback != null) {

			mNM.cancelAll();

			callback.finish();

			callback = newCallback;

			newCallback = null;

			isVisible = true;

			callback.pushLogs(logs);

		}

		if (finished)
			return false;

		doWork(contactCursor);

		if (finished) {
			finished_shown = true;

			if (logs.isEmpty())
				pushText(getString(R.string.unchanged));
			else
				pushText(getString(R.string.finished));

			pushText(getString(R.string.afterTasteDonate));
			pushText(getString(R.string.afterTasteFeedback));
			pushText(getString(R.string.afterTasteShare));

			if (isVisible) {
				callback.afterTaste.showDonateClickHint();
			} else {
				showNotification();
			}

		}

		if (isVisible) {
			callback.logger.setSelection(callback.ad.getCount() - 1);
		}

		return true;
	}

	private void pushText(String text) {

		callback.addText(text);

		if (finished_shown) {
			logs.add(text);
		}
	}

	private void doWork(Cursor cur) {
		String ret = null;

		String id = cur.getString(idColumn);
		String number = cur.getString(numberColumn);
		String logFrom = number;

		if (removeLast != null) {
			removeTail();
		}

		boolean updateText = isVisible;

		if (updateText) {
			removeLast = number;

			pushText(number);
		} else {
			removeLast = null;
		}

		number = removeMinus(number);

		if (bRemoveCountry) {
			number = removeCountry(number);

			if (bRemoveArea) {
				number = removeArea(number);
			}
		}

		if (!logFrom.equals(number)) {

			ContentValues values = new ContentValues();

			values.put(contactsPhoneNumberResolver.getNumber(), number);

			ret = logFrom + "\n==>" + number;

			if (updateText) {
				removeTail();

				removeLast = null;

				pushText(ret);
			}

			logs.add(ret);

			contactsPhoneNumberResolver
					.update(getContentResolver(), id, values);

		}

		finished = !cur.moveToNext();

		if (finished) {
			removeTail();

			removeLast = null;
		}
	}

	private void removeTail() {
		callback.removeLast(removeLast);
	}

	private String removeArea(String number) {

		String prefix = areaCode;

		if (number.startsWith(prefix)) {
			String ret = number.substring(prefix.length());

			if (ret.length() > 6)
				return ret;
		}

		return number;
	}

	private String removeCountry(String number) {

		final String[] countryPrefix = { "", "+", "00", "001", "0011", "002",
				"005", "009", "01", "010", "011", "07", "09", "097", "16",
				"19", "95", "990" };

		for (int i = 0; i < countryPrefix.length; i++) {
			String prefix = countryPrefix[i] + countryCode;

			if (number.startsWith(prefix)) {
				String nwc = number.substring(prefix.length());

				if (nwc.length() < 9) {
					return number;
				}

				Iterator<String> it = mobilePrefixList.iterator();

				while (it.hasNext()) {
					if (nwc.startsWith(it.next())) {
						return nwc;
					}
				}

				// Prefix 0
				return "0" + nwc;
			}
		}

		return number;

	}

	private String removeMinus(final String number) {
		String result = "";
		// Remove '-()' & ' ' from the Phone Number
		for (int i = 0; i < number.length(); i++) {
			char c = number.charAt(i);

			if ((c != '-') && (c != ' ') && (c != '(') && (c != ')'))
				result += c;
		}

		return result;

	}

	@SuppressWarnings("deprecation")
	private void showNotification() {

		mNM.cancelAll();

		CharSequence text = getString(finished_shown ? R.string.bgDone
				: R.string.bg);

		Notification notification = new Notification(
				finished_shown?R.drawable.done:
				R.drawable.bg, text,
				System.currentTimeMillis());

		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		if (finished_shown)
		{
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			
			notification.ledARGB = Color.CYAN; 
			
		}

		Intent intentForExtra = new Intent(this, ContactNumberFixer.class);

		intentForExtra.putExtra(FROM_SERVICE, true);

		if (finished_shown) {
			intentForExtra.putStringArrayListExtra(LOGS, logs);
		}

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intentForExtra, PendingIntent.FLAG_UPDATE_CURRENT);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.app_name), text,
				contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		mNM.notify(NID_FIX_PROGRESS, notification);
	}

	public void onVisibilityChange(boolean visible) {

		if (finished_shown) {
			if (visible) {
				mNM.cancelAll();

				callback.pushLogs(logs);

				callback.logger.setSelection(callback.ad.getCount() - 1);
			}
			return;
		}

		if (isVisible && !visible) {
			showNotification();
		} else if (visible && !isVisible) {
			mNM.cancelAll();
		}

		isVisible = visible;
	}

}
