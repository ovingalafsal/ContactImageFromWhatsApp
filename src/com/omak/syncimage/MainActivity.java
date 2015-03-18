package com.omak.syncimage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends Activity implements OnClickListener {

	Button sync;
	ListView list;
	ArrayList<WhatsAppImageContact> contacts = new ArrayList<WhatsAppImageContact>();
	ContactAdapter listAdapter;
	SyncImage syncImage;
	FetchContacts fetchContacts;
	ProgressDialog dialog;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		sync = (Button) findViewById(R.id.btn_syn);
		list = (ListView) findViewById(R.id.list);

		sync.setOnClickListener(this);

		listAdapter = new ContactAdapter(this, R.id.list, contacts);
		list.setAdapter(listAdapter);

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {

				contacts.get(pos).isSelected = !contacts.get(pos).isSelected;
				listAdapter.notifyDataSetChanged();

			}
		});
		
		fetchContacts = new FetchContacts();
		fetchContacts.execute();

	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(fetchContacts != null) {
			fetchContacts.cancel(true);
		}
		
		if(syncImage != null) {
			syncImage.cancel(true);
		}
		
		if(dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
	}

	private void getAllWhatsAppsProfileImages() {

		String path = Environment.getExternalStorageDirectory().toString()
				+ "/WhatsApp/Profile Pictures";

		try {
			File f = new File(path);
			if (f.exists()) {
				File file[] = f.listFiles();
				for (int i = 0; i < file.length; i++) {
					if (file[i].getName().endsWith(".jpg")) {
						File s = file[i];
						String number = s.getName().replace(".jpg", "");
						number = number.substring(number.length() - 10);

						FileInputStream is = new FileInputStream(s);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] b = new byte[1024];
						int bytesRead;

						while ((bytesRead = is.read(b)) != -1) {
							bos.write(b, 0, bytesRead);
						}

						ArrayList<WhatsAppImageContact> data = getRawContactIdFromNumber(
								number, bos);
						if (data.size() > 0) {
							contacts.addAll(data);
						} else {
							String newNum = number.substring(0, 2) + " " + number.substring(2,4) + " " + number.substring(4);
//							Log.e("", newNum + " newNum ");
							data = getRawContactIdFromNumber(newNum, bos);

							if (data.size() > 0) {
								contacts.addAll(data);
							} else {
								String newNum2 = number.substring(0, 5) + " " + number.substring(5);
								data = getRawContactIdFromNumber(
										newNum2, bos);
								if (data.size() > 0) {
									contacts.addAll(data);
								} else {
									String newNum3 = number.substring(0, 1) + " " + number.substring(1,3) + " " + number.substring(3,6) + " " + number.substring(6);
									data = getRawContactIdFromNumber(
											newNum3, bos);
									if (data.size() > 0) {
										contacts.addAll(data);
									}
								}
							}

						}

					}
				}

			} else {

			}
		} catch (IOException e) {
			e.printStackTrace();

		}

	}

	public static void setContactPhoto(ContentResolver c, byte[] bytes,
			long personId) {
		ContentValues values = new ContentValues();
		int photoRow = -1;
		String where = ContactsContract.Data.RAW_CONTACT_ID + " = " + personId
				+ " AND " + ContactsContract.Data.MIMETYPE + "=='"
				+ ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
				+ "'";
		Cursor cursor = c.query(ContactsContract.Data.CONTENT_URI, null, where,
				null, null);
		int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID);
		if (cursor.moveToFirst()) {
			photoRow = cursor.getInt(idIdx);
		}
		cursor.close();

		values.put(ContactsContract.Data.RAW_CONTACT_ID, personId);
		values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
		values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes);
		values.put(ContactsContract.Data.MIMETYPE,
				ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

		if (photoRow >= 0) {
			c.update(ContactsContract.Data.CONTENT_URI, values,
					ContactsContract.Data._ID + " = " + photoRow, null);
		} else {
			c.insert(ContactsContract.Data.CONTENT_URI, values);
		}
	}

	private ArrayList<WhatsAppImageContact> getRawContactIdFromNumber(
			String givenNumber, ByteArrayOutputStream stream) {
		ArrayList<WhatsAppImageContact> rawIds = new ArrayList<WhatsAppImageContact>();
		Cursor phones = getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				new String[] {
						ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
						ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME },
				ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE '%"
						+ givenNumber + "%'", null,
				ContactsContract.CommonDataKinds.Phone.NUMBER);

		while (phones.moveToNext()) {
			WhatsAppImageContact model = new WhatsAppImageContact();
			model.contactId = phones
					.getString(phones
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID));
			model.username = phones
					.getString(phones
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
			model.stream = stream;
			model.number = givenNumber;

			rawIds.add(model);
		}
		phones.close();

		return rawIds;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.btn_syn:

			syncImage = new SyncImage();
			syncImage.execute();

			break;

		default:
			break;
		}
	}

	private class SyncImage extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new ProgressDialog(MainActivity.this);
			dialog.setMessage("...");
			dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			ContentResolver cr = MainActivity.this.getContentResolver();

			for (int i = 0; i < contacts.size(); i++) {
				if (contacts.get(i).isSelected) {
					setContactPhoto(cr, contacts.get(i).stream.toByteArray(),
							Integer.parseInt(contacts.get(i).contactId));
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			dialog.dismiss();
		}

	}

	private class FetchContacts extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new ProgressDialog(MainActivity.this);
			dialog.setMessage("...");
			dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			getAllWhatsAppsProfileImages();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			dialog.dismiss();
			listAdapter.notifyDataSetChanged();
		}

	}

}
