/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.rates.ExchangeRateProviderFactory;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static ru.orangesoftware.financisto.backup.Backup.*;
import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.financisto.utils.MyPreferences.DROPBOX_AUTHORIZE;
import static ru.orangesoftware.financisto.utils.MyPreferences.DROPBOX_AUTH_TOKEN;
import static ru.orangesoftware.financisto.utils.MyPreferences.ENTITY_SELECTOR_FILTER;
import static ru.orangesoftware.financisto.utils.MyPreferences.NALOG_LOGIN;
import static ru.orangesoftware.financisto.utils.MyPreferences.NALOG_PASSWORD;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

public class DatabaseExport extends Export {

    private final Context context;
    private final SQLiteDatabase db;

    public DatabaseExport(Context context, SQLiteDatabase db, boolean useGZip) {
        super(context, useGZip);
        this.context = context;
        this.db = db;
    }

    @Override
    protected String getExtension() {
        return ".backup";
    }

    @Override
    protected void writeHeader(BufferedWriter bw) throws IOException, NameNotFoundException {
        PackageInfo pi = Utils.getPackageInfo(context);
        bw.write("PACKAGE:");
        bw.write(pi.packageName);
        bw.write("\n");
        bw.write("VERSION_CODE:");
        bw.write(String.valueOf(pi.versionCode));
        bw.write("\n");
        bw.write("VERSION_NAME:");
        bw.write(pi.versionName);
        bw.write("\n");
        bw.write("DATABASE_VERSION:");
        bw.write(String.valueOf(db.getVersion()));
        bw.write("\n");
        exportPreferences(bw);
        bw.write("#START\n");
    }

    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();

            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);

            out.write(buf);

        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    @Override
    protected void writeBody(BufferedWriter bw) throws IOException {
        for (String tableName : BACKUP_TABLES) {
            exportTable(bw, tableName);
        }
    }

    @Override
    protected void writeFooter(BufferedWriter bw) throws IOException {
        bw.write("#END");
    }

    private void exportPreferences(BufferedWriter bw) throws IOException {
        exportPreference(bw, "string", "startup_screen", MyPreferences.StartupScreen.ACCOUNTS.name());
        // Accounts List
        exportPreference(bw, "boolean", "quick_menu_account_enabled", true);
        exportPreference(bw, "boolean", "show_account_last_transaction_date", true);
        exportPreference(bw, "string", "sort_accounts", MyPreferences.AccountSortOrder.SORT_ORDER_DESC.name());
        exportPreference(bw, "boolean", "hide_closed_accounts", false);
        exportPreference(bw, "boolean", "show_menu_button_on_accounts_screen", true);
        // Blotter
        exportPreference(bw, "boolean", "quick_menu_transaction_enabled", true);
        exportPreference(bw, "boolean", "collapse_blotter_buttons", false);
        exportPreference(bw, "boolean", "show_running_balance", true);
        // New transaction screen
        exportPreference(bw, "boolean", "use_hierarchical_category_selector", true);
        exportPreference(bw, "boolean", "hierarchical_category_selector_select_child_immediately", true);
        exportPreference(bw, "boolean", "hierarchical_category_selector_income_expense", false);
        exportPreference(bw, "boolean", "remember_last_account", true);
        exportPreference(bw, "boolean", "remember_last_category", false);
        exportPreference(bw, "boolean", "remember_last_location", false);
        exportPreference(bw, "boolean", "remember_last_project", false);
        exportPreference(bw, "boolean", "ntsl_use_fixed_layout", true);
        exportPreference(bw, "boolean", "ntsl_show_currency", true);
        exportPreference(bw, "boolean", "ntsl_enter_currency_decimal_places", true);
        exportPreference(bw, "boolean", "ntsl_show_payee", true);
        exportPreference(bw, "boolean", "ntsl_show_location", true);
        exportPreference(bw, "string", "ntsl_show_location_order", "1");
        exportPreference(bw, "boolean", "ntsl_show_note", true);
        exportPreference(bw, "string", "ntsl_show_note_order", "2");
        exportPreference(bw, "boolean", "ntsl_show_project", true);
        exportPreference(bw, "string", "ntsl_show_project_order", "3");
        exportPreference(bw, "boolean", "ntsl_show_e_receipt", true);
        exportPreference(bw, "boolean", "ntsl_show_picture", true);
        exportPreference(bw, "boolean", "ntsl_show_is_ccard_payment", true);
        exportPreference(bw, "boolean", "ntsl_show_category_in_transfer", true);
        exportPreference(bw, "boolean", "ntsl_show_payee_in_transfers", false);
        exportPreference(bw, "boolean", "ntsl_open_calculator_for_template_transactions", true);
        exportPreference(bw, "boolean", "ntsl_set_focus_on_amount_field", false);
        // SMS
        exportPreference(bw, "string", "sms_transaction_status", "PN");
        exportPreference(bw, "boolean", "sms_transaction_note", true);
        // Protection
        exportPreference(bw, "boolean", "enable_widget", true);
        exportPreference(bw, "boolean", "pin_protection", false);
        exportPreference(bw, "string", "pin", "");
        exportPreference(bw, "boolean", "pin_protection_use_fingerprint", false);
        exportPreference(bw, "boolean", "pin_protection_use_fingerprint_fallback_to_pin", true);
        exportPreference(bw, "boolean", "pin_protection_lock_transaction", true);
        exportPreference(bw, "string", "pin_protection_lock_time", "5");
        // Database Backup
        exportPreference(bw, "string", "database_backup_folder", Export.DEFAULT_EXPORT_PATH.getAbsolutePath());
        exportPreference(bw, "boolean", "auto_backup_reminder_enabled", true);
        exportPreference(bw, "boolean", "auto_backup_enabled", false);
        exportPreference(bw, "int", "auto_backup_time", 600);
        exportPreference(bw, "boolean", "auto_backup_warning_enabled", true);
        // Dropbox
        exportPreference(bw, "string", DROPBOX_AUTH_TOKEN, null);
        exportPreference(bw, "boolean", DROPBOX_AUTHORIZE, false);
        exportPreference(bw, "boolean", "dropbox_upload_backup", false);
        exportPreference(bw, "boolean", "dropbox_upload_autobackup", false);
        // Google Drive Backup
        exportPreference(bw, "string", "google_drive_backup_account", null);
        exportPreference(bw, "boolean", "google_drive_backup_full_readonly", false);
        exportPreference(bw, "string", "backup_folder", "financisto");
        exportPreference(bw, "boolean", "google_drive_upload_backup", false);
        exportPreference(bw, "boolean", "google_drive_upload_autobackup", false);
        // Electronic receipt
        exportPreference(bw, "string", NALOG_LOGIN, null);
        exportPreference(bw, "string", NALOG_PASSWORD, null);
        // Exchange rates
        exportPreference(bw, "string", "exchange_rate_provider", ExchangeRateProviderFactory.freeCurrency.name());
        exportPreference(bw, "boolean", "openexchangerates_app_id", false);
        // Entity Selector
        exportPreference(bw, "string", "payee_selector", ENTITY_SELECTOR_FILTER);
        exportPreference(bw, "string", "location_selector", "filter");
        exportPreference(bw, "string", "project_selector", "filter");
        // Sort Order
        exportPreference(bw, "string", "sort_locations", MyPreferences.LocationsSortOrder.NAME.name());
        exportPreference(bw, "string", "sort_templates", MyPreferences.TemplatesSortOrder.DATE.name());
        exportPreference(bw, "string", "sort_budgets", MyPreferences.BudgetsSortOrder.DATE.name());
        // Other
        exportPreference(bw, "boolean", "pin_protection_haptic_feedback", true);
        exportPreference(bw, "boolean", "include_transfers_into_reports", false);
        exportPreference(bw, "boolean", "restore_missed_scheduled_transactions", true);
    }

    private void exportPreference(BufferedWriter bw, String type, String key, Object defValue) throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDefault = false;
        String val = "";
        switch (type) {
            case "boolean":
                boolean b = sharedPreferences.getBoolean(key, (boolean)defValue);
                val = b ? "true" : "false";
                isDefault = b == (boolean)defValue;
                break;
            case "int":
                int i = sharedPreferences.getInt(key, (int)defValue);
                val = String.valueOf(i);
                isDefault = i == (int)defValue;
                break;
            case "string":
                val = sharedPreferences.getString(key, (String)defValue);
                try {
                    isDefault = val.equals(defValue);
                } catch (NullPointerException ex) {
                    isDefault = val == null && defValue == null;
                }
                break;
        }
        if (!isDefault) {
            bw.write("#PREFERENCE:");
            bw.write(type);
            bw.write(":");
            bw.write(key);
            bw.write(":");
            bw.write(val);
            bw.write("\n");
        }
    }

    private void exportTable(BufferedWriter bw, String tableName) throws IOException {
        final boolean orderedTable = tableHasOrder(tableName);
        final boolean customOrdered = ACCOUNT_TABLE.equals(tableName);
        String sql = "select * from " + tableName 
                + (tableHasSystemIds(tableName) ? " WHERE _id > 0 " : " ") 
                + (orderedTable ? " order by " + DEF_SORT_COL + " asc" : "");
        long row = 0;
        try (Cursor c = db.rawQuery(sql, null)) {
            final String[] columnNames = c.getColumnNames();
            int cols = columnNames.length;
            while (c.moveToNext()) {
                bw.write("$ENTITY:");
                bw.write(tableName);
                bw.write("\n");
                for (int i = 0; i < cols; i++) {
                    final String colName = columnNames[i];
                    if (!DEF_SORT_COL.equals(colName) || customOrdered) {
                        final String value = c.getString(i);
                        if (value != null) {
                            bw.write(colName);
                            bw.write(":");
                            bw.write(removeNewLine(value));
                            bw.write("\n");
                        }
                    }
                }
                bw.write("$$\n");
            }
        }
    }

    private static String removeNewLine(String value) {
        return value.replace('\n', ' ');
    }

}
