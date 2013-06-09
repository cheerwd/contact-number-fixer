package jemuillot.ContactNumberFixer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jemuillot.pkg.Utilities.AfterTaste;
import jemuillot.pkg.Utilities.SelfUpdater;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
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

public class ContactNumberFixer extends Activity {

	private static final String downloadUrl = "https://dl.dropboxusercontent.com/u/1890357/software/cnf/cnf-1.1.0.apk";

	private static final String updateUrl = "https://dl.dropboxusercontent.com/u/1890357/software/cnf/updateinfo.sui";

	private static final String homepageUrl = "http://code.google.com/p/contact-number-fixer";

	protected static final int BUMP_MSG_FIX_PROC = 0;

	static final String KILLING_OLD = "jemuillot.ContactNumberFixer.killingOld";

	private Activity a;

	private SelfUpdater updater;

	boolean bRemoveCountry = false;
	boolean bRemoveArea = false;

	String countryCode;
	String areaCode;

	List<String> mobilePrefixList;

	protected FixContactService mBoundService;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			mBoundService = ((FixContactService.FixContactBinder) service)
					.getService();

			mBoundService.setCallback(ContactNumberFixer.this);

		}

		public void onServiceDisconnected(ComponentName className) {

			mBoundService = null;

		}
	};

	AfterTaste afterTaste;

	ListView logger;

	ArrayAdapter<String> ad;

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

		afterTaste = new AfterTaste(this);

		updater = new SelfUpdater(this);

		updater.setUrl(updateUrl);

		logger = (ListView) findViewById(R.id.logger);

		ad = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

		logger.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				int total = parent.getCount();

				if (total < 2) {
					return;
				}

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

		Intent intent = getIntent();

		if (intent.getBooleanExtra(FixContactService.FROM_SERVICE, false)) {

			ArrayList<String> logs = intent
					.getStringArrayListExtra(FixContactService.LOGS);

			if (logs == null) {
				FixNumbers();
			} else {

				pushLogs(logs);

				logger.setSelection(ad.getCount() - 1);

				FixNumbers();

			}

		} else {
			updater.check();

			startFromConfig();
		}

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
			countryCode = getString(R.string.defaultCountryCode);
		}

		areaCode = prefs.getString("areaCode", null);

		if (areaCode == null) {
			areaCode = getString(R.string.defaultAreaCode);
		}

		String mp = prefs.getString("mobiePrefix", null);

		if (mp == null) {
			mp = getString(R.string.defaultMobilePrefix);
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

	}

	@Override
	protected void onStop() {
		super.onStop(); // Always call the superclass method first

		if (mBoundService != null) {
			if (mBoundService.finished_shown && ad.getCount() < 2) {
				finish();
			} else {
				mBoundService.onVisibilityChange(false);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart(); // Always call the superclass method first

		if (mBoundService != null) {
			mBoundService.onVisibilityChange(true);
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart(); // Always call the superclass method first

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
				} else if (acchk.isChecked()) {
					Toast.makeText(a, R.string.warnEmptyCountryButArea,
							Toast.LENGTH_LONG).show();
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
				.setPositiveButton(R.string.utilPopStrOK,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								countryCode = StringToDigits(ccedit.getText()
										.toString());

								areaCode = StringToDigits(acedit.getText()
										.toString());

								bRemoveArea = acchk.isChecked()
										&& (areaCode.length() > 0);

								bRemoveCountry = ccchk.isChecked()
										&& (countryCode.length() > 0);

								if (bRemoveCountry) {

									saveSettings();

									new AlertDialog.Builder(a)
											.setTitle(R.string.aboutPrefix)
											.setMessage(R.string.introPrefix)
											.setOnCancelListener(
													new DialogInterface.OnCancelListener() {

														@Override
														public void onCancel(
																DialogInterface dialog) {
															startFromConfig();

														}
													})
											.setPositiveButton(
													R.string.utilPopStrOK,
													new DialogInterface.OnClickListener() {

														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {

															LayoutInflater factory = LayoutInflater
																	.from(a);

															final View mpEntryView = factory
																	.inflate(
																			R.layout.dlg_mobileprefix,
																			null);

															final EditText mpedit = (EditText) mpEntryView
																	.findViewById(R.id.mobilePrefix_edit);

															String mpStr = mobilePrefixToStr(mobilePrefixList);

															mpedit.setText(mpStr);

															new AlertDialog.Builder(
																	a)
																	.setTitle(
																			R.string.enterMobilePrefix)
																	.setView(
																			mpEntryView)
																	.setOnCancelListener(
																			new DialogInterface.OnCancelListener() {

																				@Override
																				public void onCancel(
																						DialogInterface dialog) {
																					startFromConfig();

																				}
																			})
																	.setPositiveButton(
																			R.string.utilPopStrOK,
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

																						FixNumbers();
																					}
																				}

																			})
																	.setNegativeButton(
																			R.string.utilPopStrQuit,
																			new DialogInterface.OnClickListener() {
																				public void onClick(
																						DialogInterface dialog,
																						int whichButton) {
																					a.finish();
																				}
																			})
																	.create()
																	.show();
														}

													}).create().show();
									;

								} else {
									FixNumbers();
								}

							}
						})
				.setNegativeButton(R.string.utilPopStrQuit,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								a.finish();
							}
						}).create().show();
	}

	private void FixNumbers() {

		bindService(
				new Intent(ContactNumberFixer.this, FixContactService.class),
				mConnection, Context.BIND_AUTO_CREATE);

	}

	public void addText(String text) {
		ad.add(text);
	}

	protected void onDestroy() {
		super.onDestroy();

		if (mBoundService != null) {
			unbindService(mConnection);
		}

	}

	public void removeLast(String last) {
		ad.remove(last);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (mBoundService != null && !mBoundService.finished_shown
				&& Integer.valueOf(android.os.Build.VERSION.SDK) < 7
				&& keyCode == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0) {
			onBackPressed();
		}

		return super.onKeyDown(keyCode, event);
	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	public void onBackPressed() {
		if (mBoundService != null) {

			if (mBoundService.finished_shown) {
				finish();
				return;
			} else {

				Intent i = new Intent(Intent.ACTION_MAIN);

				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addCategory(Intent.CATEGORY_HOME);

				startActivity(i);
				return;
			}
		}
		super.onBackPressed();
	}

	public void pushLogs(ArrayList<String> logs) {

		ad.clear();

		for (int i = 0; i < logs.size(); i++) {
			ad.add(logs.get(i));
		}

	}

}