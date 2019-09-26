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
package ru.orangesoftware.financisto.model;

import android.content.ContentValues;
import android.database.Cursor;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import ru.orangesoftware.financisto.db.DatabaseHelper;

import static ru.orangesoftware.financisto.db.DatabaseHelper.ElectronicReceiptColumns;
import static ru.orangesoftware.financisto.db.DatabaseHelper.ELECTRONIC_RECEIPTS_TABLE;
import static ru.orangesoftware.orb.EntityManager.DEF_ID_COL;

@Entity
@Table(name = ELECTRONIC_RECEIPTS_TABLE)
public class ElectronicReceipt implements Serializable {

    @Id
    @Column(name = DEF_ID_COL)
    public long id = -1;

    @Column(name = "transaction_id")
    public long transaction_id;

    @Column(name = "qr_code")
    public String qr_code;

    @Column(name = "check_status")
    public long check_status;

    @Column(name = "request_status")
    public long request_status;

    @Column(name = "response_data")
    public String response_data;

    public static ElectronicReceipt fromCursor(Cursor c) {
        ElectronicReceipt er = new ElectronicReceipt();
        er.id = c.getLong(ElectronicReceiptColumns.Indicies.RECEIPT_ID);
        er.transaction_id = c.getLong(ElectronicReceiptColumns.Indicies.TRANSACTION_ID);
        er.qr_code = c.getString(ElectronicReceiptColumns.Indicies.QR_CODE);
        er.check_status = c.getLong(ElectronicReceiptColumns.Indicies.CHECK_STATUS);
        er.request_status = c.getLong(ElectronicReceiptColumns.Indicies.REQUEST_STATUS);
        er.response_data = c.getString(ElectronicReceiptColumns.Indicies.RESPONSE_DATA);
        return er;
    }

    public static ElectronicReceipt fromBlotterCursor(Cursor c) {
        ElectronicReceipt er = new ElectronicReceipt();
        er.id = c.getLong(DatabaseHelper.BlotterColumns.e_receipt_id.ordinal());
        er.transaction_id = c.getLong(DatabaseHelper.BlotterColumns._id.ordinal());
        er.qr_code = c.getString(DatabaseHelper.BlotterColumns.e_receipt_qr_code.ordinal());
        er.check_status = c.getLong(DatabaseHelper.BlotterColumns.e_receipt_check_status.ordinal());
        er.request_status = c.getLong(DatabaseHelper.BlotterColumns.e_receipt_request_status.ordinal());
        er.response_data = c.getString(DatabaseHelper.BlotterColumns.e_receipt_data.ordinal());
        return er;
    }

    public ContentValues toValues() {
        ContentValues values = new ContentValues();
        values.put(ElectronicReceiptColumns.TRANSACTION_ID, transaction_id);
        values.put(ElectronicReceiptColumns.QR_CODE, qr_code);
        values.put(ElectronicReceiptColumns.CHECK_STATUS, check_status);
        values.put(ElectronicReceiptColumns.REQUEST_STATUS, request_status);
        values.put(ElectronicReceiptColumns.RESPONSE_DATA, response_data);
        return values;
    }

}
