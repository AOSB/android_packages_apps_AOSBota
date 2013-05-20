package com.beerbong.otaplatform.ui.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.updater.CancelPackage;
import com.beerbong.otaplatform.updater.GappsUpdater;
import com.beerbong.otaplatform.updater.RomUpdater;
import com.beerbong.otaplatform.updater.Updater;
import com.beerbong.otaplatform.updater.Updater.PackageInfo;
import com.beerbong.otaplatform.util.Constants;

public class UpdateFragment extends Fragment implements RomUpdater.RomUpdaterListener,
        GappsUpdater.GappsUpdaterListener {

    private static PackageInfo mNewRom = null;

    private ProgressDialog mProgress;
    private ProgressBar mProgressBar;
    private RomUpdater mRomUpdater;
    private GappsUpdater mGappsUpdater;
    private Button mButtonCheckRom;
    private Button mButtonCheckGapps;
    private Button mButtonDownload;
    private Button mButtonDownloadDelta;
    private TextView mNoNewRom;
    private TextView mRemoteVersionHeader;
    private TextView mRemoteVersionBody;
    private boolean mRomCanUpdate = true;

    public UpdateFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.update_fragment, container, false);

        mRomUpdater = Updater.getRomUpdater(getActivity(), this, false);

        mGappsUpdater = new GappsUpdater(getActivity(), this, false);

        mRomCanUpdate = mRomUpdater != null && mRomUpdater.canUpdate();

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mButtonCheckRom = (Button) view.findViewById(R.id.button_checkupdates);
        mButtonCheckGapps = (Button) view.findViewById(R.id.button_checkupdatesgapps);
        mButtonDownload = (Button) view.findViewById(R.id.button_download);
        mButtonDownloadDelta = (Button) view.findViewById(R.id.button_download_delta);
        mNoNewRom = (TextView) view.findViewById(R.id.no_new_version);
        mRemoteVersionHeader = (TextView) view.findViewById(R.id.remoteversion_header);
        mRemoteVersionBody = (TextView) view.findViewById(R.id.remoteversion_body);
        TextView romHeader = (TextView) view.findViewById(R.id.rom_header);
        TextView devHeader = (TextView) view.findViewById(R.id.developer_header);
        TextView versionHeader = (TextView) view.findViewById(R.id.version_header);

        Resources res = getActivity().getResources();

        mButtonCheckRom.setEnabled(mRomCanUpdate);
        mButtonCheckRom.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                checkRom();
            }
        });

        mButtonCheckGapps.setEnabled(mGappsUpdater.canUpdate());
        mButtonCheckGapps.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                checkGapps();
            }
        });

        mButtonDownload.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ManagerFactory.getFileManager(getActivity()).download(getActivity(), mNewRom.getPath(),
                        mNewRom.getFilename(), mNewRom.getMd5(), false,
                        Constants.DOWNLOADROM_NOTIFICATION_ID);
            }
        });

        mButtonDownloadDelta.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ManagerFactory.getFileManager(getActivity()).download(
                        getActivity(), mNewRom.getDeltaPath(), mNewRom.getDeltaFilename(),
                        mNewRom.getDeltaMd5(), true,
                        Constants.DOWNLOADROM_NOTIFICATION_ID);
            }
        });

        mRemoteVersionBody.setMovementMethod(new ScrollingMovementMethod());
        
        romHeader.setText(mRomCanUpdate ? mRomUpdater.getRomName() : res
                .getString(R.string.not_available));
        devHeader.setText(mRomCanUpdate ? res.getString(R.string.developer_header,
                new Object[] { mRomUpdater.getDeveloperId() }) : res
                .getString(R.string.not_available));
        versionHeader.setText(mRomCanUpdate ? String.valueOf(mRomUpdater.getRomVersion()) : res
                .getString(R.string.not_available));

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.getExtras() != null
                && intent.getExtras().get("NOTIFICATION_ID") != null) {
            PackageInfo info = ManagerFactory.getFileManager(getActivity())
                    .onNewIntent(getActivity(), intent);
            if (!(info instanceof CancelPackage)) {
                mNewRom = info;
            }
        }

        if (mNewRom != null) {
            checkRomCompleted(mNewRom);
        } else if (mRomCanUpdate) {
            checkRom();
        }

        if (savedInstanceState == null) {
            if (!mRomCanUpdate) {
                Constants.showSimpleDialog(getActivity(), R.string.unsupported_rom_title,
                        R.string.unsupported_rom_message);
            }
        }

        return view;
    }

    @Override
    public void checkGappsCompleted(long newVersion) {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        mButtonCheckGapps.setEnabled(mGappsUpdater.canUpdate());
    }

    @Override
    public void checkRomCompleted(PackageInfo info) {
        mProgressBar.setVisibility(View.GONE);
        if (info == null) {
            mNewRom = null;
            mRemoteVersionHeader.setText("");
            mRemoteVersionBody.setText("");
            mButtonDownload.setVisibility(View.GONE);
            mButtonDownloadDelta.setVisibility(View.GONE);
            mNoNewRom.setVisibility(View.VISIBLE);
        } else {
            mNewRom = info;
            mRemoteVersionHeader.setText(getActivity().getResources().getString(
                    R.string.new_rom_found_title, new Object[] { info.getVersion() }));
            mRemoteVersionBody.setText(info.getMessage(getActivity()));
            mNoNewRom.setVisibility(View.GONE);
            mButtonDownload.setVisibility(View.VISIBLE);
            mButtonDownloadDelta.setVisibility(info.isDelta() ? View.VISIBLE : View.GONE);
        }
        mButtonCheckRom.setEnabled(mRomUpdater != null && mRomUpdater.canUpdate());
    }

    private void checkRom() {
        mNoNewRom.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mRomUpdater.check();
    }

    private void checkGapps() {
        mProgress = ProgressDialog.show(getActivity(), null,
                getActivity().getResources().getString(R.string.checking), true, true);
        mGappsUpdater.check();
    }

}
