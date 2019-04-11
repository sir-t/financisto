package ru.orangesoftware.financisto.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.fragment.BlotterFragment;

public class MultiSelectActionsDialog extends BottomSheetDialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = this.getContext();
        LinearLayout ll = new LinearLayout(context);
        ll.setBackgroundResource(R.color.holo_gray_dark);
        ll.setOrientation(LinearLayout.VERTICAL);
        Button deleteBtn = new Button(context);
        deleteBtn.setText(R.string.delete);
        deleteBtn.setOnClickListener(arg0 -> dismissWithReply(BlotterFragment.MASS_OPERATION_DELETE));
        ll.addView(deleteBtn);
        Button clearBtn = new Button(context);
        clearBtn.setText(R.string.clear);
        clearBtn.setOnClickListener(arg0 -> dismissWithReply(BlotterFragment.MASS_OPERATION_CLEAR));
        ll.addView(clearBtn);
        Button pendingBtn = new Button(context);
        pendingBtn.setText(R.string.transaction_status_pending);
        pendingBtn.setOnClickListener(arg0 -> dismissWithReply(BlotterFragment.MASS_OPERATION_PENDING));
        ll.addView(pendingBtn);
        Button reconcileBtn = new Button(context);
        reconcileBtn.setText(R.string.reconcile);
        reconcileBtn.setOnClickListener(arg0 -> dismissWithReply(BlotterFragment.MASS_OPERATION_RECONCILE));
        ll.addView(reconcileBtn);
        Button cancelBtn = new Button(context);
        cancelBtn.setText(R.string.cancel);
        cancelBtn.setOnClickListener(arg0 -> dismiss());
        ll.addView(cancelBtn);
        return ll;

    }

    private void dismissWithReply(int massOperationDelete) {
        BlotterFragment targetFragment = (BlotterFragment) getTargetFragment();
        if(targetFragment !=null){
            targetFragment.onActivityResult(getTargetRequestCode(), massOperationDelete, null);
        }
        dismiss();
    }
}
