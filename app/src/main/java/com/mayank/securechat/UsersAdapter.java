package com.mayank.securechat;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.google.firebase.udacity.friendlychat.R;

import java.util.List;

public class UsersAdapter extends ArrayAdapter<User> {
    public UsersAdapter(Context context, int resource, List<User> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_user, parent, false);
        }

        ColorGenerator generator = ColorGenerator.MATERIAL;


        ImageView imageView=convertView.findViewById(R.id.photoImageView);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);

        User user = getItem(position);
        int color1 = generator.getColor(user.getDisplayName());
        TextDrawable drawable = TextDrawable
                .builder()
                .beginConfig()
                .fontSize(75)
                .endConfig()
                .buildRound(user.getDisplayName().substring(0,1), color1);

        nameTextView.setText(user.getDisplayName());
        imageView.setImageDrawable(drawable);

        return convertView;
    }

    @Nullable
    @Override
    public User getItem(int position) {
        return super.getItem(position);
    }
}
