package ru.orangesoftware.financisto.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import ru.orangesoftware.financisto.BuildConfig;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AboutActivity;
import ru.orangesoftware.financisto.activity.CategoryListActivity;
import ru.orangesoftware.financisto.activity.CsvExportActivity;
import ru.orangesoftware.financisto.activity.CsvImportActivity;
import ru.orangesoftware.financisto.activity.CurrencyListActivity;
import ru.orangesoftware.financisto.activity.ExchangeRatesListActivity;
import ru.orangesoftware.financisto.activity.LocationsListActivity;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.activity.PayeeListActivity;
import ru.orangesoftware.financisto.activity.PreferencesActivity;
import ru.orangesoftware.financisto.activity.ProjectListActivity;
import ru.orangesoftware.financisto.activity.QifExportActivity;
import ru.orangesoftware.financisto.activity.QifImportActivity;
import ru.orangesoftware.financisto.activity.ReceiptListActivity;
import ru.orangesoftware.financisto.activity.SmsDragListActivity;
import ru.orangesoftware.financisto.backup.Backup;
import ru.orangesoftware.financisto.databinding.SummaryEntityListItemBinding;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.BackupExportTask;
import ru.orangesoftware.financisto.export.BackupImportTask;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvExportTask;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxBackupTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxListFilesTask;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifExportTask;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportTask;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import ru.orangesoftware.financisto.utils.EntityEnum;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.ExecutableEntityEnum;
import ru.orangesoftware.financisto.utils.IntegrityFix;
import ru.orangesoftware.financisto.utils.SummaryEntityEnum;

import static android.Manifest.permission.RECEIVE_SMS;
import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.activity.RequestPermission.isRequestingPermission;
import static ru.orangesoftware.financisto.activity.RequestPermission.isRequestingPermissions;
import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static ru.orangesoftware.financisto.utils.EnumUtils.showPickOneDialog;

public class MenuFragment extends AbstractRecycleFragment implements ItemClick {

    private static final int ACTIVITY_CHANGE_PREFERENCES = 1;
    private static final int ACTIVITY_CSV_EXPORT = 2;
    private static final int ACTIVITY_CSV_IMPORT = 3;
    private static final int ACTIVITY_QIF_EXPORT = 4;
    private static final int ACTIVITY_QIF_IMPORT = 5;
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 6;

    public MenuFragment() {
        super(R.layout.activity_menu_list);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_CHANGE_PREFERENCES:
                onChangePreferences();
                break;
            case ACTIVITY_CSV_EXPORT:
                onCsvExportResult(resultCode, data);
                break;
            case ACTIVITY_CSV_IMPORT:
                onCsvImportResult(resultCode, data);
                break;
            case ACTIVITY_QIF_EXPORT:
                onQifExportResult(resultCode, data);
                break;
            case ACTIVITY_QIF_IMPORT:
                onQifImportResult(resultCode, data);
                break;
            case RESOLVE_CONNECTION_REQUEST_CODE:
                onConnectionRequest(resultCode);
                break;
        }
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null) {
            setListAdapter(new EntityRecyclerAdapter(MenuListItem.values()));
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        switch (MenuListItem.values()[position]) {
            case MENU_PREFERENCES:
                startActivityForResult(new Intent(context, PreferencesActivity.class), ACTIVITY_CHANGE_PREFERENCES);
                break;
            case MENU_ENTITIES:
                final MenuListItem.MenuEntities[] entities = MenuListItem.MenuEntities.values();
                ListAdapter adapter = EnumUtils.createEntityEnumAdapter(context, entities);
                final AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setAdapter(adapter, (dialog, which) -> {
                            dialog.dismiss();
                            MenuListItem.MenuEntities e = entities[which];
                            if (e.getPermissions() == null
                                    || !isRequestingPermissions(getActivity(), e.getPermissions())) {
                                context.startActivity(new Intent(context, e.getActivityClass()));
                            }
                        })
                        .create();
                alertDialog.setTitle(R.string.entities);
                alertDialog.show();
                break;
            case MENU_BACKUP:
                if (isRequestingPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    return;
                }
                ProgressDialog progressDialog = ProgressDialog.show(context, null, context.getString(R.string.backup_database_inprogress), true);
                new BackupExportTask(getActivity(), progressDialog, true).execute();
                break;
            case MENU_RESTORE:
                if (isRequestingPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    return;
                }
                final String[] backupFiles = Backup.listBackups(context);
                final String[] selectedBackupFile = new String[1];
                new AlertDialog.Builder(context)
                        .setTitle(R.string.restore_database)
                        .setPositiveButton(R.string.restore, (dialog, which) -> {
                            if (selectedBackupFile[0] != null) {
                                ProgressDialog d = ProgressDialog.show(context, null, context.getString(R.string.restore_database_inprogress), true);
                                new BackupImportTask(getActivity(), d).execute(selectedBackupFile);
                            }
                        })
                        .setSingleChoiceItems(backupFiles, -1, (dialog, which) -> {
                            if (backupFiles != null && which >= 0 && which < backupFiles.length) {
                                selectedBackupFile[0] = backupFiles[which];
                            }
                        })
                        .show();
                break;
            case DROPBOX_BACKUP:
                if (isRequestingPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    return;
                }
                doDropboxBackup();
                break;
            case DROPBOX_RESTORE:
                if (isRequestingPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    return;
                }
                doDropboxRestore();
                break;
            case MENU_BACKUP_TO:
                if (isRequestingPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    return;
                }
                ProgressDialog d = ProgressDialog.show(context, null, context.getString(R.string.backup_database_inprogress), true);
                final BackupExportTask t = new BackupExportTask(getActivity(), d, false);
                t.setShowResultMessage(false);
                t.setListener(result -> {
                    String backupFileName = t.backupFileName;
                    File file = Export.getBackupFile(context, backupFileName);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    Uri backupFileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, file);
                    intent.putExtra(Intent.EXTRA_STREAM, backupFileUri);
                    intent.setType("text/plain");
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.backup_database_to_title)));
                });
                t.execute((String[]) null);
                break;
            case MENU_IMPORT_EXPORT:
                if (isRequestingPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    return;
                }
                showPickOneDialog(context, R.string.import_export, ImportExportEntities.values(), this);
                break;
            case MENU_INTEGRITY_FIX:
                new IntegrityFixTask(getActivity()).execute();
                break;
            case MENU_DONATE:
                try {
                    Intent browserIntent = new Intent("android.intent.action.VIEW",
                            Uri.parse("market://search?q=pname:ru.orangesoftware.financisto.support"));
                    context.startActivity(browserIntent);
                } catch (Exception ex) {
                    //eventually market is not available
                    Toast.makeText(context, R.string.donate_error, Toast.LENGTH_LONG).show();
                }
                break;
            case MENU_ABOUT:
                startActivity(new Intent(context, AboutActivity.class));
                break;
        }
    }

    public void onChangePreferences() {
        scheduleNextAutoBackup(context);
    }

    public void onCsvExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvExportOptions options = CsvExportOptions.fromIntent(data);
            ProgressDialog progressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.csv_export_inprogress), true);
            new CsvExportTask(getActivity(), progressDialog, options).execute();
        }
    }

    public void onCsvImportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvImportOptions options = CsvImportOptions.fromIntent(data);
            ProgressDialog progressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.csv_import_inprogress), true);
            new CsvImportTask(getActivity(), progressDialog, options).execute();
        }
    }

    public void onQifExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            QifExportOptions options = QifExportOptions.fromIntent(data);
            ProgressDialog progressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.qif_export_inprogress), true);
            new QifExportTask(getActivity(), progressDialog, options).execute();
        }
    }

    public void onQifImportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            QifImportOptions options = QifImportOptions.fromIntent(data);
            ProgressDialog progressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.qif_import_inprogress), true);
            new QifImportTask(getActivity(), progressDialog, options).execute();
        }
    }

    public void onConnectionRequest(int resultCode) {
        if (resultCode == RESULT_OK) {
            Toast.makeText(context, R.string.google_drive_connection_resolved, Toast.LENGTH_LONG).show();
        }
    }

    public void doDropboxBackup() {
        ProgressDialog d = ProgressDialog.show(getActivity(), null, getString(R.string.backup_database_dropbox_inprogress), true);
        new DropboxBackupTask(getActivity(), d).execute();
    }

    public void doDropboxRestore() {
        ProgressDialog d = ProgressDialog.show(getActivity(), null, getString(R.string.dropbox_loading_files), true);
        new DropboxListFilesTask(getActivity(), d).execute();
    }

    public class EntityItemHolder extends RecyclerView.ViewHolder {

        private final SummaryEntityListItemBinding mBinding;

        EntityItemHolder(SummaryEntityListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(SummaryEntityEnum r) {
            mBinding.title.setText(r.getTitleId());
            mBinding.label.setText(r.getSummaryId());
            if (r.getIconId() > 0) {
                mBinding.icon.setImageResource(r.getIconId());
                mBinding.icon.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
            }
        }
    }

    public class EntityRecyclerAdapter extends RecyclerView.Adapter<EntityItemHolder> {

        private final SummaryEntityEnum[] entities;

        EntityRecyclerAdapter(SummaryEntityEnum[] entities) {
            this.entities = entities;
        }

        @NonNull
        @Override
        public EntityItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            SummaryEntityListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.summary_entity_list_item, parent, false);
            return new EntityItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull EntityItemHolder holder, int position) {
            holder.bind(entities[position]);
        }

        @Override
        public int getItemCount() {
            return entities.length;
        }

    }

    public enum MenuListItem implements SummaryEntityEnum {

        MENU_PREFERENCES(R.string.preferences, R.string.preferences_summary, R.drawable.drawer_action_preferences),
        MENU_ENTITIES(R.string.entities, R.string.entities_summary, R.drawable.drawer_action_entities),
        MENU_BACKUP(R.string.backup_database, R.string.backup_database_summary, R.drawable.actionbar_db_backup),
        MENU_RESTORE(R.string.restore_database, R.string.restore_database_summary, R.drawable.actionbar_db_restore),
        DROPBOX_BACKUP(R.string.backup_database_online_dropbox, R.string.backup_database_online_dropbox_summary, R.drawable.actionbar_dropbox),
        DROPBOX_RESTORE(R.string.restore_database_online_dropbox, R.string.restore_database_online_dropbox_summary, R.drawable.actionbar_dropbox),
        MENU_BACKUP_TO(R.string.backup_database_to, R.string.backup_database_to_summary, R.drawable.actionbar_share),
        MENU_IMPORT_EXPORT(R.string.import_export, R.string.import_export_summary, R.drawable.actionbar_export),
        MENU_INTEGRITY_FIX(R.string.integrity_fix, R.string.integrity_fix_summary, R.drawable.actionbar_flash),
        MENU_DONATE(R.string.donate, R.string.donate_summary, R.drawable.actionbar_donate),
        MENU_ABOUT(R.string.about, R.string.about_summary, R.drawable.ic_action_info);

        public final int titleId;
        public final int summaryId;
        public final int iconId;

        MenuListItem(int titleId, int summaryId, int iconId) {
            this.titleId = titleId;
            this.summaryId = summaryId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getSummaryId() {
            return summaryId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }


        private enum MenuEntities implements EntityEnum {

            CURRENCIES(R.string.currencies, R.drawable.ic_action_money, CurrencyListActivity.class),
            EXCHANGE_RATES(R.string.exchange_rates, R.drawable.ic_action_line_chart, ExchangeRatesListActivity.class),
            CATEGORIES(R.string.categories, R.drawable.ic_action_category, CategoryListActivity.class),
            SMS_TEMPLATES(R.string.sms_templates, R.drawable.ic_action_sms, SmsDragListActivity.class, RECEIVE_SMS),
            ELECTRONIC_RECEIPTS(R.string.electronic_receipts, R.drawable.ic_action_copy, ReceiptListActivity.class),
            PAYEES(R.string.payees, R.drawable.ic_action_users, PayeeListActivity.class),
            PROJECTS(R.string.projects, R.drawable.ic_action_gear, ProjectListActivity.class),
            LOCATIONS(R.string.locations, R.drawable.ic_action_location_2, LocationsListActivity.class);

            private final int titleId;
            private final int iconId;
            private final Class<?> actitivyClass;
            private final String[] permissions;

            MenuEntities(int titleId, int iconId, Class<?> activityClass) {
                this(titleId, iconId, activityClass, (String[]) null);
            }

            MenuEntities(int titleId, int iconId, Class<?> activityClass, String... permissions) {
                this.titleId = titleId;
                this.iconId = iconId;
                this.actitivyClass = activityClass;
                this.permissions = permissions;
            }

            @Override
            public int getTitleId() {
                return titleId;
            }

            @Override
            public int getIconId() {
                return iconId;
            }

            public Class<?> getActivityClass() {
                return actitivyClass;
            }

            public String[] getPermissions() {
                return permissions;
            }
        }

    }

    private enum ImportExportEntities implements ExecutableEntityEnum<Fragment> {

        CSV_EXPORT(R.string.csv_export, R.drawable.backup_csv) {
            @Override
            public void execute(Fragment mainActivity) {
                Intent intent = new Intent(mainActivity.getContext(), CsvExportActivity.class);
                mainActivity.startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
            }
        },
        CSV_IMPORT(R.string.csv_import, R.drawable.backup_csv) {
            @Override
            public void execute(Fragment mainActivity) {
                Intent intent = new Intent(mainActivity.getContext(), CsvImportActivity.class);
                mainActivity.startActivityForResult(intent, ACTIVITY_CSV_IMPORT);
            }
        },
        QIF_EXPORT(R.string.qif_export, R.drawable.backup_qif) {
            @Override
            public void execute(Fragment mainActivity) {
                Intent intent = new Intent(mainActivity.getContext(), QifExportActivity.class);
                mainActivity.startActivityForResult(intent, ACTIVITY_QIF_EXPORT);
            }
        },
        QIF_IMPORT(R.string.qif_import, R.drawable.backup_qif) {
            @Override
            public void execute(Fragment mainActivity) {
                Intent intent = new Intent(mainActivity.getContext(), QifImportActivity.class);
                mainActivity.startActivityForResult(intent, ACTIVITY_QIF_IMPORT);
            }
        };

        private final int titleId;
        private final int iconId;

        ImportExportEntities(int titleId, int iconId) {
            this.titleId = titleId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

    }

    private static class IntegrityFixTask extends AsyncTask<Void, Void, Void> {

        private final Activity activity;
        private ProgressDialog progressDialog;

        IntegrityFixTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.integrity_fix_in_progress), true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void o) {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).refreshCurrentTab();
            }
            progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... objects) {
            DatabaseAdapter db = new DatabaseAdapter(activity);
            new IntegrityFix(db).fix();
            return null;
        }
    }

}
