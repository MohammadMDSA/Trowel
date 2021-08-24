package ir.sami.trowel.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;

import ir.sami.trowel.R;
import ir.sami.trowel.utils.Utils;

public class SubmissionDialogFragment extends DialogFragment {

    private final String projectName;
    ProjectNameDialogFragment.NoticeDialogListener listener;

    public SubmissionDialogFragment(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.project_list_delete_ensure)
                .setPositiveButton(R.string.sure, (dialogInterface, i) -> {
                    File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
                    File project = new File(trowelRoot, projectName);
                    Utils.deleteDirectory(project);
                    listener.onDialogPositiveClick(this);
                })
                .setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (ProjectNameDialogFragment.NoticeDialogListener) context;
        } catch (ClassCastException ignored) {
        }
    }

    public interface NoticeDialogListener {
        void onDialogPositiveClick(androidx.fragment.app.DialogFragment dialog);

        void onDialogNegativeClick(androidx.fragment.app.DialogFragment dialog);
    }
}