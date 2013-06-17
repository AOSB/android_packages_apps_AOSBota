package com.beerbong.otaplatform.ui.fragment;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.updater.RomUpdater;
import com.beerbong.otaplatform.updater.Updater;
import com.beerbong.otaplatform.util.Constants;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ChangelogFragment extends Fragment {

    private WebView mWebView;
    private Bundle mWebViewBundle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.changelog_fragment, container, false);

        Context context = getActivity();
        RomUpdater romUpdater = Updater.getRomUpdater(context, null, false);

        String romChangelogUrl = null;
        if (romUpdater != null && romUpdater.canUpdate()) {
            romChangelogUrl = Constants.getProperty(Constants.OVERLAY_CHANGELOG);
        }

        final ProgressBar bar = (ProgressBar) view.findViewById(R.id.progress_bar);

        mWebView = (WebView) view.findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                bar.setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
            }

        });

        TextView textView = (TextView) view.findViewById(R.id.no_changelog);

        if (romChangelogUrl == null || "".equals(romChangelogUrl)) {
            mWebView.setVisibility(View.GONE);
            bar.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        } else {
            bar.setVisibility(View.VISIBLE);
            if (mWebViewBundle == null) {
                mWebView.loadUrl(romChangelogUrl);
            } else {
                bar.setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
                mWebView.restoreState(mWebViewBundle);
            }
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

        mWebViewBundle = new Bundle();
        mWebView.saveState(mWebViewBundle);
    }
}
