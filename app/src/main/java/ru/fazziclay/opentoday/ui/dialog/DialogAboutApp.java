package ru.fazziclay.opentoday.ui.dialog;

import static ru.fazziclay.opentoday.util.InlineUtil.fcu_viewOnClick;

import android.app.Activity;
import android.app.Dialog;

import ru.fazziclay.opentoday.app.App;
import ru.fazziclay.opentoday.databinding.DialogAboutAppBinding;
import ru.fazziclay.opentoday.ui.activity.OpenSourceLicensesActivity;

public class DialogAboutApp {
    private final Dialog dialog;

    public DialogAboutApp(Activity activity) {
        // View
        DialogAboutAppBinding binding = DialogAboutAppBinding.inflate(activity.getLayoutInflater());
        binding.textVersion.setText(App.VERSION_NAME);
        binding.textPackage.setText(App.APPLICATION_ID);

        // Dialog
        dialog = new Dialog(activity);
        dialog.setContentView(binding.getRoot());

        // Buttons
        fcu_viewOnClick(binding.licence, () -> activity.startActivity(OpenSourceLicensesActivity.createLaunchIntent(activity)));
        fcu_viewOnClick(binding.ok, dialog::cancel);
    }

    public void show() {
        dialog.show();
    }
}
