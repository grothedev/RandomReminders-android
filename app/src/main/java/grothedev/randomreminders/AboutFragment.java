package grothedev.randomreminders;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

/**
 * Created by thomas on 7/18/17.
 */

public class AboutFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        TextView message = new TextView(getActivity());
        SpannableString str = new SpannableString(getActivity().getText(R.string.dialog_about));
        Linkify.addLinks(str, Linkify.ALL);
        message.setText(str);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        message.setPadding(10, 10, 10, 10);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onDestroy();
                    }
                })
                .setView(message);
        return builder.create();
    }
}
