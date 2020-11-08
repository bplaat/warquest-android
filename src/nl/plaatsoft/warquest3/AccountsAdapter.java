package nl.plaatsoft.warquest3;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.text.NumberFormat;

// Accounts list adapter
public class AccountsAdapter extends ArrayAdapter<Account> {
    // Account view holder
    private static class AccountViewHolder {
        public ImageView accountAvatarImage;
        public TextView accountNicknameLabel;
        public TextView accountInfoLabel;
        public ImageView accountRemoveButton;
    }

    // Account action button view holder
    private static class AccountButtonViewHolder {
        public ImageView accountButtonImage;
        public TextView accountButtonLabel;
    }

    private long selectedAccountId;

    public AccountsAdapter(Context context, long selectedAccountId) {
       super(context, 0);
       this.selectedAccountId = selectedAccountId;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = getContext();

        // Create account view
        if (position < getCount() - 2 && (convertView == null || convertView.getTag() instanceof AccountButtonViewHolder)) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_account, parent, false);
            AccountViewHolder accountViewHolder = new AccountViewHolder();
            accountViewHolder.accountAvatarImage = (ImageView)convertView.findViewById(R.id.account_avatar_image);
            accountViewHolder.accountNicknameLabel = (TextView)convertView.findViewById(R.id.account_nickname_label);
            accountViewHolder.accountInfoLabel = (TextView)convertView.findViewById(R.id.account_info_label);
            accountViewHolder.accountRemoveButton = (ImageView)convertView.findViewById(R.id.account_remove_button);
            convertView.setTag(accountViewHolder);
        }

        // Create account action button view
        if (position >= getCount() - 2 && (convertView == null || convertView.getTag() instanceof AccountViewHolder)) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_account_button, parent, false);
            AccountButtonViewHolder accountButtonViewHolder = new AccountButtonViewHolder();
            accountButtonViewHolder.accountButtonImage = (ImageView)convertView.findViewById(R.id.account_button_image);
            accountButtonViewHolder.accountButtonLabel = (TextView)convertView.findViewById(R.id.account_button_label);
            convertView.setTag(accountButtonViewHolder);
        }

        // Fill account view
        if (position < getCount() - 2) {
            AccountViewHolder accountViewHolder = (AccountViewHolder)convertView.getTag();
            Account account = getItem(position);

            // Highlight account button if selected
            if (account.getId() == selectedAccountId) {
                convertView.setBackgroundResource(R.color.selected_background_color);
            } else {
                convertView.setBackgroundResource(0);
            }

            // Load avatar image async
            FetchImageTask.fetchImage(context, accountViewHolder.accountAvatarImage, Config.GRAVATAR_URL + Utils.md5(account.getEmail()) + "?s=" + Utils.convertDpToPixel(context, 40) + "&d=mp");

            // Set account labels
            accountViewHolder.accountNicknameLabel.setText(account.getNickname());
            accountViewHolder.accountInfoLabel.setText(context.getResources().getString(R.string.account_info_label, account.getLevel(), NumberFormat.getInstance(Utils.getCurrentLocale(context)).format(account.getExperience())));

            // When remove button is clicked
            accountViewHolder.accountRemoveButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // Show remove warning alert
                    new AlertDialog.Builder(context)
                        .setTitle(R.string.settings_remove_alert_title)
                        .setMessage(R.string.settings_remove_alert_message_label)
                        .setPositiveButton(R.string.settings_remove_alert_remove_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Request account removal at parent activity
                                ((SettingsActivity)context).removeAccount(position);
                            }
                        })
                        .setNegativeButton(R.string.settings_remove_alert_cancel_button, null)
                        .show();
                }
            });
        }

        // Fill add account action button
        if (position == getCount() - 2) {
            AccountButtonViewHolder accountButtonViewHolder = (AccountButtonViewHolder)convertView.getTag();
            accountButtonViewHolder.accountButtonImage.setImageResource(R.drawable.ic_account_plus);
            accountButtonViewHolder.accountButtonLabel.setText(context.getResources().getString(R.string.settings_add_account_button));
        }

        // Fill create account action button
        if (position == getCount() - 1) {
            AccountButtonViewHolder accountButtonViewHolder = (AccountButtonViewHolder)convertView.getTag();
            accountButtonViewHolder.accountButtonImage.setImageResource(R.drawable.ic_account_details);
            accountButtonViewHolder.accountButtonLabel.setText(context.getResources().getString(R.string.settings_create_account_button));
        }

        return convertView;
    }

    public long getSelectedAccountId() {
        return selectedAccountId;
    }

    public void setSelectedAccountId(long selectedAccountId) {
        this.selectedAccountId = selectedAccountId;
        notifyDataSetInvalidated();
    }
}
