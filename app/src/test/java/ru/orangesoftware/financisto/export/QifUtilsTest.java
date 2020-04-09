package ru.orangesoftware.financisto.export;

import org.junit.Test;
import org.junit.runner.RunWith;

import ru.orangesoftware.financisto.test.DateTime;

import static org.junit.Assert.assertEquals;
import static ru.orangesoftware.financisto.export.qif.QifDateFormat.EU_FORMAT;
import static ru.orangesoftware.financisto.export.qif.QifDateFormat.US_FORMAT;
import static ru.orangesoftware.financisto.export.qif.QifUtils.parseDate;
import static ru.orangesoftware.financisto.export.qif.QifUtils.parseMoney;
import static ru.orangesoftware.financisto.export.qif.QifUtils.trimFirstChar;

public class QifUtilsTest {

    @Test
    public void should_trim_first_char() {
        assertEquals("My Cash Account", trimFirstChar("NMy Cash Account"));
        assertEquals("-10.5", trimFirstChar("X-10.5"));
    }

    @Test
    public void should_parse_dates() {
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("07/02/2011", EU_FORMAT));
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("07/02/2011", EU_FORMAT));
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("02/07/2011", US_FORMAT));
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("07.02.11", EU_FORMAT));
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("07.02'11", EU_FORMAT));
        assertEquals(DateTime.date(2011, 1, 23).atMidnight().asDate(), parseDate("1.23'11", US_FORMAT));
    }

    @Test
    public void should_parse_money() {
        assertEquals(100, parseMoney("1.0"));
        assertEquals(-100, parseMoney("-1."));
//        assertEquals(10100, parseMoney("1,01")); // todo.mb: if this test worked ever? 
        assertEquals(100250, parseMoney("1,002.5"));
        assertEquals(100250, parseMoney("1.002,5"));
    }

}