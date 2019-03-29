/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.service;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import androidx.test.runner.AndroidJUnit4;
import ru.orangesoftware.financisto.test.DateTime;

import static org.junit.Assert.assertEquals;
import static ru.orangesoftware.financisto.test.DateTime.date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/16/11 1:00 AM
 */
@RunWith(AndroidJUnit4.class)
public class AutoBackupSchedulerTest {

    @Test
    public void test_should_schedule_auto_backup_at_specified_time() {
        assertEquals(date(2011, 12, 16).at(6, 0, 0, 0).asDate(), scheduleAt(date(2011, 12, 16).at(0, 0, 0, 0)));
        assertEquals(date(2011, 12, 17).at(6, 0, 0, 0).asDate(), scheduleAt(date(2011, 12, 16).at(8, 0, 0, 0)));
        assertEquals(date(2012, 1, 1).at(6, 0, 0, 0).asDate(), scheduleAt(date(2011, 12, 31).at(18, 0, 0, 0)));
    }

    private Date scheduleAt(DateTime now) {
        return new DailyAutoBackupScheduler(6, 0, now.asLong()).getScheduledTime();
    }

}
