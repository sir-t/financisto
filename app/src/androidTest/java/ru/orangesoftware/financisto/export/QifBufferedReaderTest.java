/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;


import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.test.runner.AndroidJUnit4;
import ru.orangesoftware.financisto.export.qif.QifBufferedReader;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/11/11 10:13 PM
 */
@RunWith(AndroidJUnit4.class)
public class QifBufferedReaderTest {

    @Test
    public void test_should_read_qif_file_line_by_line_trimming_each_line_and_skipping_empty_lines() throws IOException {
        String content =
                "!Account \n"+
                "\n"+
                "NMy Cash Account\n"+
                "\tTCash\n"+
                "\n"+
                "^\n"+
                "\n"+
                " \t!Type:Cash\n"+
                "MSome note here... \n"+
                "^\n";
        QifBufferedReader r = new QifBufferedReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content.getBytes()), "UTF-8")));

        assertEquals("!Account", r.peekLine());
        assertEquals("!Account", r.readLine());

        assertEquals("NMy Cash Account", r.peekLine());
        assertEquals("NMy Cash Account", r.readLine());

        assertEquals("TCash", r.peekLine());
        assertEquals("TCash", r.readLine());

        assertEquals("^", r.peekLine());
        assertEquals("^", r.readLine());

        assertEquals("!Type:Cash", r.peekLine());
        assertEquals("!Type:Cash", r.readLine());

        assertEquals("MSome note here...", r.peekLine());
        assertEquals("MSome note here...", r.readLine());

        assertEquals("^", r.peekLine());
        assertEquals("^", r.readLine());
    }

}
