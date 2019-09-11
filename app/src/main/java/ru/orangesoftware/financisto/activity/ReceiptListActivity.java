package ru.orangesoftware.financisto.activity;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.fragment.ReceiptListFragment;

public class ReceiptListActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new ReceiptListFragment();
	}

}
