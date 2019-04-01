package ru.orangesoftware.financisto.activity;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.fragment.MassOpFragment;

public class MassOpActivity extends FragmentActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LinearLayout content = new LinearLayout(this);
		content.setId(R.id.main_container);
		setContentView(content);

		getSupportFragmentManager().beginTransaction()
				.add(R.id.main_container, new MassOpFragment())
				.commit();
	}
}
