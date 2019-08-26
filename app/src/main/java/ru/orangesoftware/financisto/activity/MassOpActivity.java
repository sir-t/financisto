package ru.orangesoftware.financisto.activity;

import androidx.fragment.app.Fragment;
import ru.orangesoftware.financisto.fragment.MassOpFragment;

public class MassOpActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new MassOpFragment();
	}

}
