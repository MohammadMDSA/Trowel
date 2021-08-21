package ir.sami.trowel.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import ir.sami.trowel.R;

public class ProjectNameDialogFragment extends DialogFragment {

    private EditText input;
    NoticeDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_project_name_dialog, null);
        this.input = view.findViewById(R.id.inp_new_project_name);
        builder.setMessage(R.string.project_list_create_name)
                .setView(view)
                .setPositiveButton(R.string.fire, (dialogInterface, i) -> {
                    File trowelRoot = new File(Environment.getExternalStorageDirectory(), "Trowel");
                    File newProject = new File(trowelRoot, input.getText().toString());
                    if(newProject.exists()) {
                        Toast.makeText(getActivity(), R.string.project_list_create_name_already_exists, Toast.LENGTH_SHORT).show();
                    }
                    else{
                        newProject.mkdirs();
                        listener.onDialogPositiveClick(this);
                    }

                })
                .setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (NoticeDialogListener) context;
        } catch (ClassCastException ignored) {
        }
    }

    public interface NoticeDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }
}