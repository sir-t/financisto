package ru.orangesoftware.financisto.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Status;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.api.builder.ActivityIntentBuilder;
import org.androidannotations.api.builder.PostActivityStarter;
import org.androidannotations.api.view.HasViews;
import org.androidannotations.api.view.OnViewChangedListener;
import org.androidannotations.api.view.OnViewChangedNotifier;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MenuListActivity;
import ru.orangesoftware.financisto.activity.MenuListActivity_;
import ru.orangesoftware.financisto.activity.MenuListItem;
import ru.orangesoftware.financisto.adapter.SummaryEntityListAdapter;
import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.bus.GreenRobotBus_;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.drive.DoDriveBackup;
import ru.orangesoftware.financisto.export.drive.DoDriveListFiles;
import ru.orangesoftware.financisto.export.drive.DoDriveRestore;
import ru.orangesoftware.financisto.export.drive.DriveBackupError;
import ru.orangesoftware.financisto.export.drive.DriveBackupFailure;
import ru.orangesoftware.financisto.export.drive.DriveBackupSuccess;
import ru.orangesoftware.financisto.export.drive.DriveConnectionFailed;
import ru.orangesoftware.financisto.export.drive.DriveFileInfo;
import ru.orangesoftware.financisto.export.drive.DriveFileList;
import ru.orangesoftware.financisto.export.drive.DriveRestoreSuccess;
import ru.orangesoftware.financisto.export.drive.DropboxFileList;
import ru.orangesoftware.financisto.export.dropbox.DropboxBackupTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxListFilesTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxRestoreTask;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.utils.PinProtection;

import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;

@EFragment
public class MenuFragment extends ListFragment implements HasViews, OnViewChangedListener {

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;

    private Activity context;
    private View view;

    @Bean
    GreenRobotBus bus;
    private final OnViewChangedNotifier onViewChangedNotifier_ = new OnViewChangedNotifier();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        OnViewChangedNotifier previousNotifier = OnViewChangedNotifier.replaceNotifier(onViewChangedNotifier_);
        init_(savedInstanceState);
        super.onCreate(savedInstanceState);
        OnViewChangedNotifier.replaceNotifier(previousNotifier);
    }

    public <T extends View> T internalFindViewById(int id) {
        return ((T) view.findViewById(id));
    }

    private void init_(Bundle savedInstanceState) {
        this.bus = GreenRobotBus_.getInstance_(context);
        OnViewChangedNotifier.registerOnViewChangedListener(this);
    }

    public static MenuListActivity_.IntentBuilder_ intent(Context context) {
        return new MenuListActivity_.IntentBuilder_(context);
    }

    public static MenuListActivity_.IntentBuilder_ intent(Fragment supportFragment) {
        return new MenuListActivity_.IntentBuilder_(supportFragment);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 2: {
                onCsvExportResult(resultCode, data);
                break;
            }
            case 3: {
                onQifExportResult(resultCode, data);
                break;
            }
            case 4: {
                onCsvImportResult(resultCode, data);
                break;
            }
            case 5: {
                onQifImportResult(resultCode, data);
                break;
            }
            case 6: {
                onChangePreferences();
                break;
            }
            case 1: {
                onConnectionRequest(resultCode);
                break;
            }
        }
    }

    @Override
    public void onViewChanged(HasViews hasViews) {
        init();
    }

    public static class IntentBuilder_
            extends ActivityIntentBuilder<MenuListActivity_.IntentBuilder_> {
        private Fragment fragmentSupport_;

        public IntentBuilder_(Context context) {
            super(context, MenuListActivity_.class);
        }

        public IntentBuilder_(Fragment fragment) {
            super(fragment.getActivity(), MenuListActivity_.class);
            fragmentSupport_ = fragment;
        }

        @Override
        public PostActivityStarter startForResult(int requestCode) {
            if (fragmentSupport_ != null) {
                fragmentSupport_.startActivityForResult(intent, requestCode);
            } else {
                if (context instanceof Activity) {
                    Activity activity = ((Activity) context);
                    ActivityCompat.startActivityForResult(activity, intent, requestCode, lastOptions);
                } else {
                    context.startActivity(intent);
                }
            }
            return new PostActivityStarter(context);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity();
        view = inflater.inflate(R.layout.activity_menu_list, container, false);
        init();
        return view;
    }

    @AfterViews
    protected void init() {
        setListAdapter(new SummaryEntityListAdapter(context, MenuListItem.values()));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MenuListItem.values()[position].call(context);
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CSV_EXPORT)
    public void onCsvExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvExportOptions options = CsvExportOptions.fromIntent(data);
            MenuListItem.doCsvExport(context, options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_QIF_EXPORT)
    public void onQifExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            QifExportOptions options = QifExportOptions.fromIntent(data);
            MenuListItem.doQifExport(context, options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CSV_IMPORT)
    public void onCsvImportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvImportOptions options = CsvImportOptions.fromIntent(data);
            MenuListItem.doCsvImport(context, options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_QIF_IMPORT)
    public void onQifImportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            QifImportOptions options = QifImportOptions.fromIntent(data);
            MenuListItem.doQifImport(context, options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CHANGE_PREFERENCES)
    public void onChangePreferences() {
        scheduleNextAutoBackup(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        PinProtection.lock(context);
        bus.unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        PinProtection.unlock(context);
        bus.register(this);
    }

    ProgressDialog progressDialog;

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    // google drive

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doGoogleDriveBackup(MenuListActivity.StartDriveBackup e) {
        progressDialog = ProgressDialog.show(context, null, getString(R.string.backup_database_gdocs_inprogress), true);
        bus.post(new DoDriveBackup());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doGoogleDriveRestore(MenuListActivity.StartDriveRestore e) {
        progressDialog = ProgressDialog.show(context, null, getString(R.string.google_drive_loading_files), true);
        bus.post(new DoDriveListFiles());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveList(DriveFileList event) {
        dismissProgressDialog();
        final List<DriveFileInfo> files = event.files;
        final String[] fileNames = getFileNames(files);
        final DriveFileInfo[] selectedDriveFile = new DriveFileInfo[1];
        new AlertDialog.Builder(context)
                .setTitle(R.string.restore_database_online_google_drive)
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    if (selectedDriveFile[0] != null) {
                        progressDialog = ProgressDialog.show(context, null, getString(R.string.google_drive_restore_in_progress), true);
                        bus.post(new DoDriveRestore(selectedDriveFile[0]));
                    }
                })
                .setSingleChoiceItems(fileNames, -1, (dialog, which) -> {
                    if (which >= 0 && which < files.size()) {
                        selectedDriveFile[0] = files.get(which);
                    }
                })
                .show();
    }

    private String[] getFileNames(List<DriveFileInfo> files) {
        String[] names = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            names[i] = files.get(i).title;
        }
        return names;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveConnectionFailed(DriveConnectionFailed event) {
        dismissProgressDialog();
        ConnectionResult connectionResult = event.connectionResult;
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(context, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                onDriveBackupError(new DriveBackupError(e.getMessage()));
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), context, 0).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveBackupFailed(DriveBackupFailure event) {
        dismissProgressDialog();
        Status status = event.status;
        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(context, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                onDriveBackupError(new DriveBackupError(e.getMessage()));
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(status.getStatusCode(), context, 0).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveBackupSuccess(DriveBackupSuccess event) {
        dismissProgressDialog();
        Toast.makeText(context, getString(R.string.google_drive_backup_success, event.fileName), Toast.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveRestoreSuccess(DriveRestoreSuccess event) {
        dismissProgressDialog();
        Toast.makeText(context, R.string.restore_database_success, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("StringFormatInvalid")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDriveBackupError(DriveBackupError event) {
        dismissProgressDialog();
        Toast.makeText(context, getString(R.string.google_drive_connection_failed, event.message), Toast.LENGTH_LONG).show();
    }

    @OnActivityResult(RESOLVE_CONNECTION_REQUEST_CODE)
    public void onConnectionRequest(int resultCode) {
        if (resultCode == RESULT_OK) {
            Toast.makeText(context, R.string.google_drive_connection_resolved, Toast.LENGTH_LONG).show();
        }
    }

    // dropbox
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doImportFromDropbox(DropboxFileList event) {
        final String[] backupFiles = event.files;
        if (backupFiles != null) {
            final String[] selectedDropboxFile = new String[1];
            new AlertDialog.Builder(context)
                    .setTitle(R.string.restore_database_online_dropbox)
                    .setPositiveButton(R.string.restore, (dialog, which) -> {
                        if (selectedDropboxFile[0] != null) {
                            ProgressDialog d = ProgressDialog.show(context, null, getString(R.string.restore_database_inprogress_dropbox), true);
                            new DropboxRestoreTask(context, d, selectedDropboxFile[0]).execute();
                        }
                    })
                    .setSingleChoiceItems(backupFiles, -1, (dialog, which) -> {
                        if (which >= 0 && which < backupFiles.length) {
                            selectedDropboxFile[0] = backupFiles[which];
                        }
                    })
                    .show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doDropboxBackup(MenuListActivity.StartDropboxBackup e) {
        ProgressDialog d = ProgressDialog.show(context, null, this.getString(R.string.backup_database_dropbox_inprogress), true);
        new DropboxBackupTask(context, d).execute();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doDropboxRestore(MenuListActivity.StartDropboxRestore e) {
        ProgressDialog d = ProgressDialog.show(context, null, this.getString(R.string.dropbox_loading_files), true);
        new DropboxListFilesTask(context, d).execute();
    }

    public static class StartDropboxBackup {
    }

    public static class StartDropboxRestore {
    }

    public static class StartDriveBackup {
    }

    public static class StartDriveRestore {
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

    }
}
