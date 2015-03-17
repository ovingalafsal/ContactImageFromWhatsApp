package com.omak.syncimage;

import com.omak.syncimage.R;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ProfileImage extends Activity implements OnClickListener {

	EditText edt_add;
	Button add, sync;
	ListView list;
	ArrayList<String> numbers = new ArrayList<String>();
	ArrayAdapter<String> listAdapter;
	static final int PICK_CONTACT_REQUEST = 1;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		edt_add = (EditText) findViewById(R.id.edt_add);
		add = (Button) findViewById(R.id.btn_add);
		sync = (Button) findViewById(R.id.btn_syn);
		list = (ListView) findViewById(R.id.list);

		sync.setOnClickListener(this);
		add.setOnClickListener(this);

		numbers = new ArrayList<String>();

		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, numbers);
		list.setAdapter(listAdapter);


	}

	private ByteArrayOutputStream checkWhatsAppImage(String number) {
		
		number = number.replace(" ", "");
		number = number.substring(number.length() - 10);
		number = number + ".jpg";

		String path = Environment.getExternalStorageDirectory().toString()
				+ "/WhatsApp/Profile Pictures";

		try {
			File f = new File(path);
			if (f.exists()) {
				File file[] = f.listFiles();
				for (int i = 0; i < file.length; i++) {
					if (file[i].getName().contains(number)) {
						File s = file[i];

						FileInputStream is = new FileInputStream(s);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] b = new byte[1024];
						int bytesRead;

						while ((bytesRead = is.read(b)) != -1) {
							bos.write(b, 0, bytesRead);
						}

						byte[] bytes = bos.toByteArray();
						return bos;
					}
				}
				return null;
			} else {
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
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

	private ArrayList<String> getRawContactIdFromNumber(String givenNumber) {
		ArrayList<String> rawIds = new ArrayList<String>();
		Cursor phones = getContentResolver()
				.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						new String[] { ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID },
						ContactsContract.CommonDataKinds.Phone.NUMBER + "='"
								+ givenNumber + "'", null,
						ContactsContract.CommonDataKinds.Phone.NUMBER);

		while (phones.moveToNext()) {
			rawIds.add(phones.getString(phones
					.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)));
			Log.v("contacts", "Given Number: " + givenNumber + "Raw ID: "
					+ rawIds.get(rawIds.size() - 1));
		}
		phones.close();

		return rawIds;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_add:

			pickContact();

			break;

		case R.id.btn_syn:
			
			for (int i = 0; i < numbers.size(); i++) {
				setImage(numbers.get(i));
			}

			break;

		default:
			break;
		}
	}

	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	private void pickContact() {
	    Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
	    pickContactIntent.setType(Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
	    startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == PICK_CONTACT_REQUEST) {
	        if (resultCode == RESULT_OK) {
	            Uri contactUri = data.getData();
	            String[] projection = {Phone.NUMBER};

	            Cursor cursor = getContentResolver()
	                    .query(contactUri, projection, null, null, null);
	            cursor.moveToFirst();

	            int column = cursor.getColumnIndex(Phone.NUMBER);
	            String number = cursor.getString(column);
	            
	            numbers.add(number);
	            listAdapter.notifyDataSetChanged();
	        }
	    }
	}

	private void setImage(String number) {
		ContentResolver cr = this.getContentResolver(); // Activity/Application
		// android.content.Context
		Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
				null, null, null);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream = checkWhatsAppImage(number);
		if (stream != null) {
			Log.e("1", "Stream not null");
			ArrayList<String> persons = getRawContactIdFromNumber(number);
			if (persons.size() > 0) {
				Log.e("1",
						"Stream not null and persons id size " + persons.size());
				for (int i = 0; i < persons.size(); i++) {
					setContactPhoto(cr, stream.toByteArray(),
							Integer.parseInt(persons.get(i)));
				}
			}
		}
	}
}
