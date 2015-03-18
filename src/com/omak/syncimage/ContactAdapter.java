package com.omak.syncimage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;


public class ContactAdapter extends ArrayAdapter<WhatsAppImageContact> {

	public ArrayList<WhatsAppImageContact> imageContacts;
	Activity mCont;

	public ContactAdapter(Activity context, int textViewResourceId,
			ArrayList<WhatsAppImageContact> objects) {
		super(context, textViewResourceId, objects);
		this.imageContacts = objects;
		mCont = context;
		
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		final ViewHolderProgram holder;
		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.contact_row, parent, false);
			holder = new ViewHolderProgram();
			holder.name = (TextView) v.findViewById(R.id.txt_name);
			holder.number = (TextView) v.findViewById(R.id.txt_number);
			holder.image = (ImageView) v.findViewById(R.id.image);
			holder.select = (CheckBox) v.findViewById(R.id.chb_select);

			v.setTag(holder);

		} else {
			holder = (ViewHolderProgram) v.getTag();
		}

		WhatsAppImageContact model = imageContacts.get(position);
		holder.name.setText(model.username);
		holder.number.setText(model.number);
		holder.image.setImageBitmap(BitmapFactory.decodeByteArray(model.stream.toByteArray() , 0, model.stream.toByteArray().length));
		if(model.isSelected) {
			holder.select.setChecked(true);
		} else {
			holder.select.setChecked(false);
		}
		return v;
	}

	private CharSequence getDateText(long dateText) {

		DateFormat format1 = new SimpleDateFormat("HH:mm - dd/MM/yyyy");
		Date commentDate = new Date(dateText);
		return format1.format(commentDate);
	}


	static class ViewHolderProgram {
		TextView name, number;
		CheckBox select;
		ImageView image;
	}
	
	

}

