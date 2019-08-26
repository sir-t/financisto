package ru.orangesoftware.financisto.activity;

import androidx.fragment.app.Fragment;
import ru.orangesoftware.financisto.fragment.BlotterFragment;

public class GenericBlotterActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new BlotterFragment();
    }

}
