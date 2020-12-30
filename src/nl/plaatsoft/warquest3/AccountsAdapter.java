package nl.plaatsoft.warquest3;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.text.NumberFormat;

// Accounts list adapter
public class AccountsAdapter extends ArrayAdapter<Account> {
    private static class ViewHolder {
        public ImageView accountAvatarImage;
        public TextView accountNicknameLabel;
        public TextView accountInfoLabel;
        public ImageButton accountRemoveButton;
    }

    private long selectedAccountId = -1;

    public AccountsAdapter(Context context) {
       super(context, 0);
    }

    public long getSelectedAccountId() {
        return selectedAccountId;
    }

    public void setSelectedAccountId(long selectedAccountId) {
        this.selectedAccountId = selectedAccountId;
        notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_account, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.accountAvatarImage = (ImageView)convertView.findViewById(R.id.account_avatar_image);
            viewHolder.accountNicknameLabel = (TextView)convertView.findViewById(R.id.account_nickname_label);
            viewHolder.accountInfoLabel = (TextView)convertView.findViewById(R.id.account_info_label);
            viewHolder.accountRemoveButton = (ImageButton)convertView.findViewById(R.id.account_remove_button);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        Account account = getItem(position);

        if (account.getId() == selectedAccountId) {
            convertView.setBackgroundResource(R.color.selected_background_color);
        } else {
            convertView.setBackgroundColor(0);
        }

        FetchImageTask.with(getContext())
            .load(Config.APP_GRAVATAR_URL + "/avatar/" + Utils.md5(account.getEmail()) + "?s=" + Utils.convertDpToPixel(getContext(), 40) + "&d=mp")
            .fadeIn()
            .into(viewHolder.accountAvatarImage);

        viewHolder.accountNicknameLabel.setText(account.getNickname());
        viewHolder.accountInfoLabel.setText(getContext().getResources().getString(R.string.account_info_label, account.getLevel(), NumberFormat.getInstance(Utils.getCurrentLocale(getContext())).format(account.getExperience())));

        viewHolder.accountRemoveButton.setOnClickListener((View view) -> {
            new AlertDialog.Builder(getContext())
                .setTitle(R.string.account_remove_alert_title_label)
                .setMessage(R.string.account_remove_alert_message_label)
                .setPositiveButton(R.string.account_remove_alert_remove_button, (DialogInterface dialog, int whichButton) -> {
                    ((SettingsActivity)getContext()).removeAccount(account);
                })
                .setNegativeButton(R.string.account_remove_alert_cancel_button, null)
                .show();
        });

        return convertView;
    }
}
