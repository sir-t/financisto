package ru.orangesoftware.financisto.service;

import android.util.Pair;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.service.SmsTransactionProcessor.Placeholder;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.SmsTemplateBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.orangesoftware.financisto.model.Category.NO_CATEGORY_ID;
import static ru.orangesoftware.financisto.service.SmsTransactionProcessor.toBigDecimal;

public class SmsTransactionProcessorTest extends AbstractDbTest {

    SmsTransactionProcessor smsProcessor;
    TransactionStatus status =TransactionStatus.PN;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        smsProcessor = new SmsTransactionProcessor(db);
    }

    @Test
    public void TransactionCorrection(){
        Currency usdCurr = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").makeDefault().create();
        Account a1 = AccountBuilder.withDb(db).currency(usdCurr).title("cash").total(7700).create();
        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);
        Category aa1Cat = categories.get("AA1");

        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(-1000).category(aa1Cat).create();
        a1 = db.getAccount(a1.id);
        assertEquals(6700, a1.totalAmount);

        String template = "*{{a}}. Summa {{P}} RUB. NOVYY PROEKT, MOSCOW. {{D}}. Dostupno {{b}}";
        String sms = "Pokupka. Karta *5631. Summa 6.99 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 50.71 RUB. Tinkoff.ru";

        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(a1.id).categoryId(aa1Cat.id).template(template).create();
        final Pair<Transaction, Transaction> results = smsProcessor.createTransactionBySmsAndCorrect("Tinkoff", sms, status, true, 1);

        Transaction correction = results.second;
        assertEquals(a1.id, correction.fromAccountId);
        assertEquals(NO_CATEGORY_ID, correction.categoryId);
        assertEquals(-930L, correction.fromAmount);
        assertEquals("sms correction", correction.note);
        assertEquals(status, correction.status);

        Transaction transaction = results.first;
        assertEquals(a1.id, transaction.fromAccountId);
        assertEquals(aa1Cat.id, transaction.categoryId);
        assertEquals(-699L, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(status, transaction.status);
    }

    @Test
    public void CorrectionWithoutTransaction(){
        Currency usdCurr = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").makeDefault().create();
        Account a1 = AccountBuilder.withDb(db).currency(usdCurr).title("cash").total(7700).create();
        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);
        Category aa1Cat = categories.get("AA1");

        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(-1000).category(aa1Cat).create();
        a1 = db.getAccount(a1.id);
        assertEquals(6700, a1.totalAmount);

        String template = "*{{a}}. Summa {{*}} RUB. NOVYY PROEKT, MOSCOW. {{D}}. Dostupno {{b}}";
        String sms = "Pokupka. Karta *5631. Summa 6.99 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 50.71 RUB. Tinkoff.ru";

        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(a1.id).categoryId(aa1Cat.id).template(template).create();
        final Pair<Transaction, Transaction> results = smsProcessor.createTransactionBySmsAndCorrect("Tinkoff", sms, status, true, 1);

        assertNull(results.first);
        Transaction correction = results.second;
        assertEquals(a1.id, correction.fromAccountId);
        assertEquals(NO_CATEGORY_ID, correction.categoryId);
        assertEquals(-1629L, correction.fromAmount);
        assertEquals("sms correction", correction.note);
        assertEquals(status, correction.status);

    }

    @Test
    public void TemplateWithTextPlaceholder() {
        String template = "Покупка. Карта *{{a}}. {{p}} RUB.{{t}}. Доступно {{b}}";
        String sms = "Покупка. Карта *5631. 1477.14 RUB. RNAZK ROSNEF. Доступно 30321.9 RUB";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(template, sms);

        Assert.assertArrayEquals(new String[]{null, "5631", "30321.9 ", null, "1477.14", " RNAZK ROSNEF"}, matches);

        SmsTemplateBuilder.withDb(db).title("777").accountId(7).categoryId(8).template(template).create();
        Transaction transaction = smsProcessor.createTransactionBySmsAndCorrect("777", sms, status, true, 0).first;

        assertEquals(7, transaction.fromAccountId);
        assertEquals(8, transaction.categoryId);
        assertEquals(-147714, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(status, transaction.status);
    }

    @Test
    public void TransactionByTinkoffSms() {
        String template = "*{{a}}. Summa {{P}} RUB. NOVYY PROEKT, MOSCOW. {{D}}. Dostupno {{b}}";
        String sms = "Pokupka. Karta *5631. Summa 1234567.20 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34202.70 RUB. Tinkoff.ru";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(template, sms);

        Assert.assertArrayEquals(new String[]{null, "5631", "34202.70 ", "02.10.2017 14:19", "1234567.20", null}, matches);

        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(7).categoryId(8).template(template).create();
        Transaction transaction = smsProcessor.createTransactionBySmsAndCorrect("Tinkoff", sms, status, true, 0).first;

        assertEquals(7, transaction.fromAccountId);
        assertEquals(8, transaction.categoryId);
        assertEquals(-123456720L, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(status, transaction.status);
    }

    @Test
    public void TemplatesWithDifferentLength() {
        String template1 = "*{{a}}. Summa {{p}} RUB. {{*}}, MOSCOW. {{d}}. Dostupno {{b}}";
        String template2 = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}}";
        String template3 = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}}";
        String sms = "Pokupka. Karta *5631. Summa 250.77 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34202.82 RUB. Tinkoff.ru";

        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(7).categoryId(8).template(template1).sortOrder(2).create();
        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(7).categoryId(88).template(template2).sortOrder(1).create();
        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(7).categoryId(89).template(template3).create();
        Transaction transaction = smsProcessor.createTransactionBySmsAndCorrect("Tinkoff", sms, status, true, 0).first;

        assertEquals(7, transaction.fromAccountId);
        assertEquals(88, transaction.categoryId);
        assertEquals(-25077, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(TransactionStatus.PN, transaction.status);
    }

    @Test
    public void MultilineSms() {
        String template = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}}{{*}}";
        String sms = "Pokupka. Karta *5631. Summa 1250,77 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34 202.82 RUB.\nTinkoff\n.ru";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(template, sms);

        Assert.assertArrayEquals(new String[]{null, "5631", "34 202.82 ", "02.10.2017 14:19", "1250,77", null}, matches);
    }

    @Test
    public void UniversalPrices() {
        String template = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}} RUB. Tinkoff.ru";
        String sms = "Pokupka. Karta *5631. Summa 1 250,77 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34,202.82 RUB. Tinkoff.ru";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(template, sms);

        Assert.assertArrayEquals(new String[]{null, "5631", "34,202.82", "02.10.2017 14:19", "1 250,77", null}, matches);

        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(7).categoryId(8).template(template).create();
        Transaction transaction = smsProcessor.createTransactionBySmsAndCorrect("Tinkoff", sms, status, true, 0).first;

        assertEquals(7, transaction.fromAccountId);
        assertEquals(8, transaction.categoryId);
        assertEquals(-125077, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(status, transaction.status);
    }

    @Test
    public void UniversalPrices2() {
        String template = "*{{a}}. Summa {{p}} RUB. NOVYY PROEKT, MOSCOW. {{d}}. Dostupno {{b}} RUB. Tinkoff.ru";
        String sms = "Pokupka. Karta *5631. Summa 1'250.77 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34'202,82 RUB. Tinkoff.ru";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(template, sms);

        Assert.assertArrayEquals(new String[]{null, "5631", "34'202,82", "02.10.2017 14:19", "1'250.77", null}, matches);

        SmsTemplateBuilder.withDb(db).title("Tinkoff").accountId(7).categoryId(8).template(template).create();
        Transaction transaction = smsProcessor.createTransactionBySmsAndCorrect("Tinkoff", sms, status, true, 0).first;

        assertEquals(7, transaction.fromAccountId);
        assertEquals(8, transaction.categoryId);
        assertEquals(-125077, transaction.fromAmount);
        assertEquals(sms, transaction.note);
        assertEquals(status, transaction.status);
    }

    @Test
    public void NotFound() {
        String smsTpl = "ECMC{{a}} {{d}} покупка {{p}}р TEREMOK METROPOLIS Баланс: {{b}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK XXX Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        assertNull(matches);

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).create();
        Pair<Transaction, Transaction> transaction = smsProcessor.createTransactionBySmsAndCorrect("900", sms, status, true, 0);

        assertNull(transaction);
    }

    @Test
    public void TransactionBySberSmsWithAccountLookup() {
        String smsTpl = "ECMC{{a}} {{d}} покупка {{p}}р TEREMOK METROPOLIS Баланс: {{b}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).create();
        Account account = AccountBuilder.withDb(db)
            .currency(CurrencyBuilder.createDefault(db))
            .title("SB")
            .number("1111-2222-3333-5431")
            .create();
        Transaction transaction = smsProcessor.createTransactionBySmsAndCorrect("900", sms, status, false, 0).first;

        assertEquals(account.id, transaction.fromAccountId);
        assertEquals(18, transaction.categoryId);
        assertEquals(-55000, transaction.fromAmount);
        assertEquals("", transaction.note);
    }

    @Test
    public void NotFoundAccount() {
        String smsTpl = "ECMC{{A}} {{D}} покупка {{P}}р TEREMOK METROPOLIS Баланс: {{B}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);

        SmsTemplateBuilder.withDb(db).title("900").categoryId(18).template(smsTpl).create();
        Pair<Transaction, Transaction> transaction = smsProcessor.createTransactionBySmsAndCorrect("900", sms, status, true, 0);

        assertNull(transaction);
    }

    @Test
    public void WrongPrice() {
        String smsTpl = "ECMC{{*}} {{*}} покупка {{p}}р TEREMOK METROPOLIS Баланс: {{*}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 0р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, null, null, null, "0", null}, matches);

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).create();
        Pair<Transaction, Transaction> transaction = smsProcessor.createTransactionBySmsAndCorrect("900", sms, status, true, 0);

        assertNull(transaction);
    }

    @Test
    public void DebitTransactionBySberSms() {
        String smsTpl = "ECMC<:A:> <:D:> покупка <:P:>р TEREMOK METROPOLIS Баланс: <:B:>р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).income(true).create();
        Transaction transaction = smsProcessor.createTransactionBySmsAndCorrect("900", sms, status, true, 0).first;

        assertEquals(17, transaction.fromAccountId);
        assertEquals(18, transaction.categoryId);
        assertEquals(55000, transaction.fromAmount);
        assertEquals(sms, transaction.note);
    }

    @Test
    public void TemplateWithWrongSpaces() {
        String smsTpl = "ECMC{{a}}<:D:>покупка{{P}}р TEREMOK METROPOLIS Баланс:{{b}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);
    }

    @Test
    public void TemplateWithAnyMatch() {
        String smsTpl = "ECMC{{A}}{{d}}покупка<:P:>р TEREMOK <::>Баланс:<:B:>р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);
    }

    @Test
    public void TemplateWithMultipleAnyMatch() {
        String smsTpl = "ECMC<:A:> <:D:> {{*}} <:P:>р TEREMOK<::><:B:>р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "5431", "49820.45", "01.10.17 19:50", "550", null}, matches);
    }

    @Test
    public void TemplateWithMultipleAnyMatchWithoutAccount() {
        String smsTpl = "<::> <:D:> {{*}} <:P:>р TEREMOK<::><:B:>р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 550р TEREMOK METROPOLIS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, null, "49820.45", "01.10.17 19:50", "550", null}, matches);
    }

    @Test
    public void TemplateWithSpecialChars() {
        String smsTpl = "{{*}} {{d}} {{*}} {{p}}р TE{{R}}E{{MOK ME}TROP<:P:OL?$()[]/\\.*IS{{*}}{{b}}р";
        String sms = "ECMC5431 01.10.17 19:50 покупка 555р TE{{R}}E{{MOK ME}TROP<:P:OL?$()[]/\\.*IS Баланс: 49820.45р";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, null, "49820.45", "01.10.17 19:50", "555", null}, matches);
    }

    @Test
    public void MultipleAnyMatchWithoutAccountAndDate() {
        String smsTpl = "Pokupka{{*}}Summa {{p}} RUB. NOVYY PROEKT, MOSCOW{{*}}Dostupno {{b}} RUB.{{*}}";
        String sms = "Pokupka. Karta *5631. Summa 250.00 RUB. NOVYY PROEKT, MOSCOW. 02.10.2017 14:19. Dostupno 34202.82 RUB. Tinkoff.ru";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, null, "34202.82", null, "250.00", null}, matches);
    }

    @Test
    public void NegativeSums() {
        String smsTpl = "Karta {{*}}.{{a}} {{*}} Cash {{p}} BYN {{*}}";
        String sms = "Karta 5.3471 08.07.2019 18:46:28 Cash -497.00 BYN EO 27-1 MINSK BLR OK. Dostupno 1169.42 BYN";

        String[] matches = SmsTransactionProcessor.findTemplateMatches(smsTpl, sms);
        Assert.assertArrayEquals(new String[]{null, "3471", null, null, "-497.00", null}, matches);

        AccountBuilder.withDb(db).currency(CurrencyBuilder.createDefault(db)).title("BLR")
            .number("1111-2222-3333-3471")
            .create();

        SmsTemplateBuilder.withDb(db).title("900").accountId(17).categoryId(18).template(smsTpl).income(false).create();
        Transaction transaction = smsProcessor.createTransactionBySmsAndCorrect("900", sms, status, true, 0).first;

        assertEquals(1, transaction.fromAccountId);
        assertEquals(18, transaction.categoryId);
        assertEquals(-49700, transaction.fromAmount);
        assertEquals(sms, transaction.note);
    }

    @Test
    public void FindingPlaceholderIndexes() {
        int[] indexes = SmsTransactionProcessor.findPlaceholderIndexes("Pokupka. Karta *<:A:>. Summa <:P:> RUB. NOVYY PROEKT, MOSCOW. <:D:>. Dostupno <:B:> RUB. Tinkoff.ru");
        assertTrue(indexes[Placeholder.ACCOUNT.ordinal()] == 0);
        assertTrue(indexes[Placeholder.PRICE.ordinal()] == 1);
        assertTrue(indexes[Placeholder.DATE.ordinal()] == 2);
        assertTrue(indexes[Placeholder.BALANCE.ordinal()] == 3);

        indexes = SmsTransactionProcessor.findPlaceholderIndexes("ECMC<:A:> <:D:> покупка <:P:> TEREMOK METROPOLIS Баланс: <:B:>р");
        assertTrue(indexes[Placeholder.ACCOUNT.ordinal()] == 0);
        assertTrue(indexes[Placeholder.DATE.ordinal()] == 1);
        assertTrue(indexes[Placeholder.PRICE.ordinal()] == 2);
        assertTrue(indexes[Placeholder.BALANCE.ordinal()] == 3);

        indexes = SmsTransactionProcessor.findPlaceholderIndexes("ECMC<:A:> <:D:> покупка <:P:> TEREMOK METROPOLIS Баланс:");
        assertTrue(indexes[Placeholder.ACCOUNT.ordinal()] == 0);
        assertTrue(indexes[Placeholder.DATE.ordinal()] == 1);
        assertTrue(indexes[Placeholder.PRICE.ordinal()] == 2);
        assertTrue(indexes[Placeholder.BALANCE.ordinal()] == -1);

        indexes = SmsTransactionProcessor.findPlaceholderIndexes("ECMC<:A:> <:D:><::> <:P:> TEREMOK METROPOLIS Баланс:");
        assertTrue(indexes[Placeholder.ANY.ordinal()] == -1);
        assertTrue(indexes[Placeholder.ACCOUNT.ordinal()] == 0);
        assertTrue(indexes[Placeholder.DATE.ordinal()] == 1);
        assertTrue(indexes[Placeholder.PRICE.ordinal()] == 2);
        assertTrue(indexes[Placeholder.BALANCE.ordinal()] == -1);

        indexes = SmsTransactionProcessor.findPlaceholderIndexes("ECMC<:A:> <:D:><::> TEREMOK METROPOLIS Баланс:");
        assertNull(indexes);
    }

    @Test
    public void DifferentPrices() {
        assertEquals(new BigDecimal(5), toBigDecimal("5"));
        assertEquals(new BigDecimal("5.3"), toBigDecimal(" 5,3 "));
        assertEquals(new BigDecimal("5.3"), toBigDecimal("5.3"));
        assertEquals(new BigDecimal("5000.3"), toBigDecimal("5.000,3"));
        assertEquals(new BigDecimal("5000000.3"), toBigDecimal("5.000.000,3"));
        assertEquals(new BigDecimal("5000000"), toBigDecimal("5.000.000"));
        assertEquals(new BigDecimal("5000.3"), toBigDecimal("5,000.3"));
        assertEquals(new BigDecimal("5000000.3"), toBigDecimal("5,000,000.3"));
        assertEquals(new BigDecimal("5000000"), toBigDecimal("5,000,000"));
        assertEquals(new BigDecimal("5"), toBigDecimal("+5"));
        assertEquals(new BigDecimal("5.3"), toBigDecimal("+5,3"));
        assertEquals(new BigDecimal("5.3"), toBigDecimal("+5.3"));
        assertEquals(new BigDecimal("5000.3"), toBigDecimal("+5.000,3"));
        assertEquals(new BigDecimal("5000000.3"), toBigDecimal("+5.000.000,3"));
        assertEquals(new BigDecimal("5000000"), toBigDecimal("+5.000.000"));
        assertEquals(new BigDecimal("5000.3"), toBigDecimal("+5,000.3"));
        assertEquals(new BigDecimal("5000000.3"), toBigDecimal("+5,000,000.3"));
        assertEquals(new BigDecimal("5000000"), toBigDecimal("+5,000,000"));
        assertEquals(new BigDecimal("-5"), toBigDecimal(" -5 "));
        assertEquals(new BigDecimal("-5.3"), toBigDecimal("-5,3"));
        assertEquals(new BigDecimal("-5.3"), toBigDecimal("-5.3"));
        assertEquals(new BigDecimal("-5000.3"), toBigDecimal("-5.000,3"));
        assertEquals(new BigDecimal("-5000000.3"), toBigDecimal("-5.000.000,3"));
        assertEquals(new BigDecimal("-5000000"), toBigDecimal("-5.000.000"));
        assertEquals(new BigDecimal("-5000.3"), toBigDecimal("-5,000.3"));
        assertEquals(new BigDecimal("-5000000.3"), toBigDecimal("-5,000,000.3"));
        assertEquals(new BigDecimal("-5000000"), toBigDecimal("-5,000,000"));
        assertEquals(new BigDecimal("1234.56"), toBigDecimal("1 234.56"));
        assertEquals(new BigDecimal("1234.56"), toBigDecimal("1234.56"));
        assertEquals(new BigDecimal("1234.56"), toBigDecimal("1 234,56"));
        assertEquals(new BigDecimal("1234.56"), toBigDecimal("1 234,56"));
        assertEquals(new BigDecimal("1234.56"), toBigDecimal("1,234.56"));
        assertEquals(new BigDecimal("123456"), toBigDecimal("123456"));
        assertEquals(new BigDecimal("1234.5678"), toBigDecimal("1234,5678"));
        assertEquals(new BigDecimal("123456789"), toBigDecimal("123456789"));
        assertEquals(new BigDecimal("123456789"), toBigDecimal("123 456 789"));

        assertEquals(new BigDecimal("1234.56"), toBigDecimal("1'234.56"));
    }
}