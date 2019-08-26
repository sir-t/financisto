package ru.orangesoftware.financisto.activity;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import ru.orangesoftware.financisto.R;

public abstract class SingleFragmentActivity extends FragmentActivity {

    protected abstract Fragment createFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout content = new LinearLayout(this);
        content.setId(R.id.main_container);
        setContentView(content);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.main_container, createFragment())
                .commit();
    }

}
