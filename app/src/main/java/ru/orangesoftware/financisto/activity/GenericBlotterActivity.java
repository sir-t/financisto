package ru.orangesoftware.financisto.activity;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.fragment.BlotterFragment;

public class GenericBlotterActivity extends FragmentActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout content = new LinearLayout(this);
        content.setId(R.id.main_container);
        setContentView(content);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.main_container, new BlotterFragment())
                .commit();
    }
}
