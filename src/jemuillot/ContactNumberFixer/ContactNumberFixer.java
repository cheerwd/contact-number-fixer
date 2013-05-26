package jemuillot.ContactNumberFixer;

import jemuillot.pkg.Utilities.AfterTaste;
import jemuillot.pkg.Utilities.SelfUpdater;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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
		cr.update(ContentUris.withAppendedId(Contacts.Phones.CONTENT_URI,
				Integer.parseInt(id)), values, null, null);

	}
}

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

public class ContactNumberFixer extends Activity {

	private static final String downloadUrl = 
		"http://contact-number-fixer.googlecode.com/files/ContactNumberFixer-1.06.apk";
	
	private static final String updateUrl = "http://contact-number-fixer.googlecode.com/files/updateinfox.sui";
	private static final String donateUrl = "https://me.alipay.com/jemuillot";
	private static final String homepageUrl = "http://code.google.com/p/contact-number-fixer";

	private Handler mHandler = new Handler();

	private int idColumn;
	private int numberColumn;
	private boolean finished;
	private boolean finished_shown;

	private Activity a;

	private ContactsPhoneNumberResolver contactsPhoneNumberResolver;
	private SelfUpdater updater;

	private AfterTaste afterTaste;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	private void donate() {
		afterTaste.donate(donateUrl);
	}
	private void feedback() {
		afterTaste.feedback(null, homepageUrl);
	}

	private void share() {
		afterTaste.share(downloadUrl);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.donate:
			donate();
			return true;
		case R.id.feedback:
			feedback();
			return true;
		case R.id.share:
			share();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		a = this;

		afterTaste = new AfterTaste(this);

		updater = new SelfUpdater(this);

		updater.setUrl(updateUrl);

		updater.check();

		if (Integer.parseInt(Build.VERSION.SDK) >= 7)
			contactsPhoneNumberResolver = new ContactsPhoneNumberResolver7();
		else
			contactsPhoneNumberResolver = new ContactsPhoneNumberResolver3();

		new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(
				R.string.warning).setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						// We don't care who owns the number, we just need the
						// phone list
						final Cursor c = getPhoneList();

						// Fix them
						FixNumbers(c);
					}
				}).setNegativeButton(R.string.quit,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						a.finish();

					}
				}).show();
	}

	/**
	 * Get the list of all phone numbers
	 */
	private Cursor getPhoneList() {

		Uri uri = contactsPhoneNumberResolver.getUri();

		// ID & Number should be enough for the subsequence operations
		String[] projection = new String[] {
				contactsPhoneNumberResolver.getID(),
				contactsPhoneNumberResolver.getNumber() };

		return managedQuery(uri, projection, null, null, null);

	}

	private void FixNumbers(final Cursor cur) {

		if (cur.moveToFirst()) {

			idColumn = cur.getColumnIndex(contactsPhoneNumberResolver.getID());

			numberColumn = cur.getColumnIndex(contactsPhoneNumberResolver
					.getNumber());

			final ListView logger = (ListView) findViewById(R.id.logger);

			final ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1);

			logger.setOnItemClickListener(new ListView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {

					int total = parent.getCount();

					if (position + 1 == total) {
						share();
					} else if (position + 2 == total) {
						feedback();
					} else if (position + 3 == total) {
						donate();
					}

				}
			});

			logger.setAdapter(ad);

			finished = false;

			// Start lengthy operation in a background thread
			new Thread(new Runnable() {
				public void run() {
					while (!finished) {
						final String text = doWork(cur);

						// Update the progress bar
						mHandler.post(new Runnable() {

							public void run() {

								if (finished_shown)
									return;

								if (text != null) {
									ad.add(text);
								}
								// else {
								// ad.add("no conv");
								// }
								if (finished) {
									finished_shown = true;

									afterTaste.showDonateClickHint();

									if (ad.isEmpty())
										ad.add(getString(R.string.unchanged));
									else
										ad.add(getString(R.string.finished));

									ad.add(getString(R.string.afterTasteDonate));
									ad.add(getString(R.string.afterTasteFeedback));
									ad.add(getString(R.string.afterTasteShare));
								}

								logger.setSelection(ad.getCount() - 1);
							}
						});
					}

				}
			}).start();

		} else {
			Toast.makeText(a, R.string.noContact, Toast.LENGTH_LONG).show();
		}

	}

	private String doWork(Cursor cur) {
		String ret = null;

		String id = cur.getString(idColumn);
		String number = cur.getString(numberColumn);
		String result = "";

		boolean changed = false;

		// Remove '-' & ' ' from the Phone Number
		for (int i = 0; i < number.length(); i++) {
			char c = number.charAt(i);

			if ((c != '-') && (c != ' '))
				result = result + c;
			else
				changed = true;
		}

		if (changed) {
			ContentValues values = new ContentValues();

			values.put(contactsPhoneNumberResolver.getNumber(), result);

			ret = number + "\n==>" + result;

			contactsPhoneNumberResolver
					.update(getContentResolver(), id, values);
		}

		finished = !cur.moveToNext();

		return ret;
	}

}