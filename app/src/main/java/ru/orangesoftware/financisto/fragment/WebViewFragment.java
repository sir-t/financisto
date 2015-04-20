package ru.orangesoftware.financisto.fragment;

import android.app.Fragment;
import android.os.Bundle;

public class WebViewFragment extends android.webkit.WebViewFragment {
    private static final String EXTRA_URL = "url";

    public static Fragment newInstance(String url) {
        Fragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String url = getArguments().getString(EXTRA_URL);
        getWebView().loadUrl(url);
    }
}
