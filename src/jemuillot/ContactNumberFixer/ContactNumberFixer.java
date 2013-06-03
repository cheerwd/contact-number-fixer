package jemuillot.ContactNumberFixer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jemuillot.pkg.Utilities.AfterTaste;
import jemuillot.pkg.Utilities.SelfUpdater;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
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
		cr.update(
				ContentUris.withAppendedId(Contacts.Phones.CONTENT_URI,
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

	private static final String downloadUrl = "https://dl.dropboxusercontent.com/u/1890357/software/cnf/cnf-1.1.0.apk";

	private static final String updateUrl = "https://dl.dropboxusercontent.com/u/1890357/software/cnf/updateinfo.sui";

	private static final String homepageUrl = "http://code.google.com/p/contact-number-fixer";

	private Handler mHandler = new Handler();

	private int idColumn;
	private int numberColumn;
	private boolean finished;
	private boolean finished_shown;

	private boolean isPaused;

	private Activity a;

	private ContactsPhoneNumberResolver contactsPhoneNumberResolver;
	private SelfUpdater updater;

	private AfterTaste afterTaste;

	private boolean bRemoveCountry = false;
	private boolean bRemoveArea = false;

	private String countryCode;
	private String areaCode;

	private List<String> mobilePrefixList;

	private boolean canPause;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	private void donate() {
		afterTaste.donate(null);
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

		isPaused = false;

		finished = true;

		afterTaste = new AfterTaste(this);

		updater = new SelfUpdater(this);

		updater.setUrl(updateUrl);

		updater.check();

		if (Integer.parseInt(Build.VERSION.SDK) >= 7)
			contactsPhoneNumberResolver = new ContactsPhoneNumberResolver7();
		else
			contactsPhoneNumberResolver = new ContactsPhoneNumberResolver3();

		startFromConfig();
	}

	protected String fixEdit(EditText edit) {

		String textInp = edit.getText().toString();
		String text = StringToDigits(textInp);

		if (!text.equals(textInp)) {
			edit.setText(text);
		}

		return text;
	}

	protected String StringToDigits(String string) {

		String item = "";

		for (int n = 0; n < string.length(); n++) {
			char c = string.charAt(n);

			if (c >= '0' && c <= '9') {
				item += c;
			}
		}

		return item;
	}

	protected void saveSettings() {

		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

		editor.putString("countryCode", countryCode);
		editor.putString("areaCode", areaCode);

		editor.putString("mobiePrefix", mobilePrefixToStr(mobilePrefixList));

		editor.commit();

	}

	private void loadSettings() {

		SharedPreferences prefs = getPreferences(MODE_PRIVATE);

		countryCode = prefs.getString("countryCode", null);

		if (countryCode == null) {
			countryCode = "86";
		}

		areaCode = prefs.getString("areaCode", null);

		if (areaCode == null) {
			areaCode = "020";
		}

		String mp = prefs.getString("mobiePrefix", null);

		if (mp == null) {
			mp = "13,15,18";
		}

		mobilePrefixList = stringToMobilePrefix(mp);

	}

	protected String mobilePrefixToStr(List<String> list) {

		String mpStr = "";

		Iterator<String> it = list.iterator();

		while (it.hasNext()) {

			mpStr += it.next() + ",";
		}

		if (mpStr.length() > 0)
			return mpStr.substring(0, mpStr.length() - 1);
		else
			return mpStr;
	}

	protected List<String> stringToMobilePrefix(String mpStr) {

		List<String> ret = new LinkedList<String>();

		String item = "";

		for (int n = 0; n < mpStr.length(); n++) {
			char c = mpStr.charAt(n);

			if (c >= '0' && c <= '9') {
				item += c;
			} else if (!item.equals("")) {
				ret.add(item);
				item = "";
			}

		}

		if (!item.equals("")) {
			ret.add(item);
		}

		return ret;
	}

	@Override
	public void onPause() {
		super.onPause(); // Always call the superclass method first

		if (!finished) {
			canPause = false;

			isPaused = true;

			while (!canPause) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
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

	private void startFromConfig() {

		mobilePrefixList = new LinkedList<String>();

		loadSettings();

		LayoutInflater factory = LayoutInflater.from(a);

		final View textEntryView = factory.inflate(R.layout.dlg_countreacode,
				null);

		final CheckBox ccchk = (CheckBox) textEntryView
				.findViewById(R.id.countryRomove_check);

		final CheckBox acchk = (CheckBox) textEntryView
				.findViewById(R.id.areaRomove_check);

		final EditText ccedit = (EditText) textEntryView
				.findViewById(R.id.countryCode_edit);

		final EditText acedit = (EditText) textEntryView
				.findViewById(R.id.areaCode_edit);

		ccedit.setText(countryCode);
		acedit.setText(areaCode);
		
		ccchk.setChecked(bRemoveCountry);
		acchk.setChecked(bRemoveArea);

		ccchk.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {

				if (isChecked) {

					String text = fixEdit(ccedit);

					if (text.equals("")) {
						Toast.makeText(a, R.string.warnEmptyCode,
								Toast.LENGTH_LONG).show();

						ccchk.setChecked(false);
					}
				}

			}
		});

		acchk.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {

				if (isChecked) {

					String text = fixEdit(acedit);

					if (text.equals("")) {
						Toast.makeText(a, R.string.warnEmptyCode,
								Toast.LENGTH_LONG).show();

						acchk.setChecked(false);
					} else {
						text = fixEdit(ccedit);

						if (text.equals("")) {
							Toast.makeText(a, R.string.warnEmptyCountryButArea,
									Toast.LENGTH_LONG).show();
						} else {
							ccchk.setChecked(true);
						}
					}
				}

			}

		});

		new AlertDialog.Builder(a)
				.setTitle(R.string.whichToRemove)
				.setView(textEntryView)
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
	
						a.finish();
					}
				})
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								countryCode = StringToDigits(ccedit.getText()
										.toString());

								areaCode = StringToDigits(acedit.getText()
										.toString());

								bRemoveArea = acchk.isChecked() && (areaCode.length()>0);

								bRemoveCountry = ccchk.isChecked() && (countryCode.length()>0);

								if (bRemoveCountry) {
									LayoutInflater factory = LayoutInflater
											.from(a);

									final View mpEntryView = factory.inflate(
											R.layout.dlg_mobileprefix, null);

									final EditText mpedit = (EditText) mpEntryView
											.findViewById(R.id.mobilePrefix_edit);

									String mpStr = mobilePrefixToStr(mobilePrefixList);

									mpedit.setText(mpStr);

									saveSettings();

									new AlertDialog.Builder(a)
											.setTitle(
													R.string.enterMobilePrefix)
											.setView(mpEntryView)
											.setOnCancelListener(
													new DialogInterface.OnCancelListener() {

														@Override
														public void onCancel(
																DialogInterface dialog) {
															startFromConfig();

														}
													})
											.setPositiveButton(
													R.string.ok,
													new DialogInterface.OnClickListener() {

														public void onClick(
																DialogInterface dialog,
																int whichButton) {

															String mpStr = mpedit
																	.getText()
																	.toString();

															mobilePrefixList = stringToMobilePrefix(mpStr);

															if (mobilePrefixList
																	.isEmpty()) {

																Toast.makeText(
																		a,
																		R.string.warnEmptyMobilePrefix,
																		Toast.LENGTH_LONG)
																		.show();

																startFromConfig();

															} else {
																saveSettings();

																final Cursor c = getPhoneList();

																FixNumbers(c);
															}
														}

													})
											.setNegativeButton(
													R.string.quit,
													new DialogInterface.OnClickListener() {
														public void onClick(
																DialogInterface dialog,
																int whichButton) {
															a.finish();
														}
													}).create().show();

								}
								else
								{
									final Cursor c = getPhoneList();

									FixNumbers(c);
								}

							}
						})
				.setNegativeButton(R.string.quit,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								a.finish();
							}
						}).create().show();
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

						if (isPaused) {
							canPause = true;

							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							a.finish();

							break;
						}

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
		String logFrom = number;

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

			contactsPhoneNumberResolver
					.update(getContentResolver(), id, values);
		}

		finished = !cur.moveToNext();

		return ret;
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
				
				if (nwc.length() < 9)
				{
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
		// Remove '-' & ' ' from the Phone Number
		for (int i = 0; i < number.length(); i++) {
			char c = number.charAt(i);

			if ((c != '-') && (c != ' ') && (c != '(') && (c != ')'))
				result += c;
		}

		return result;

	}

}