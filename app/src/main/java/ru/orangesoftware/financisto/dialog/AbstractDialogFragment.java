package ru.orangesoftware.financisto.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.fragment.BlotterFragment;

public abstract class AbstractDialogFragment extends BottomSheetDialogFragment {

    Map<Integer, Integer> resourceResponseMap;

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

        for (int resourceId : resourceResponseMap.keySet()) {
            Button deleteBtn = new Button(context);
            deleteBtn.setText(resourceId);
            deleteBtn.setOnClickListener(arg0 -> dismissWithReply(resourceResponseMap.get(resourceId)));
            ll.addView(deleteBtn);
        }
        return ll;

    }

    private void dismissWithReply(int massOperationDelete) {
        BlotterFragment targetFragment = (BlotterFragment) getTargetFragment();
        if (targetFragment != null) {
            targetFragment.onActivityResult(getTargetRequestCode(), massOperationDelete, null);
        }
        dismiss();
    }
}
