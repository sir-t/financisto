package ru.orangesoftware.financisto.utils;

import org.junit.Test;
import ru.orangesoftware.financisto.model.Category;

import static org.junit.Assert.assertEquals;
import static ru.orangesoftware.financisto.utils.TransactionTitleUtils.generateTransactionTitle;

public class TransactionTitleUtilsTest {

    @Test
    public void test_should_generate_title_for_regular_transactions() {
        assertEquals("", generateTransactionTitle(sb(), null, null, null, Category.NO_CATEGORY_ID, null));
        assertEquals("Payee", generateTransactionTitle(sb(), "Payee", null, null, Category.NO_CATEGORY_ID, null));
        assertEquals("Note", generateTransactionTitle(sb(), null, "Note", null, Category.NO_CATEGORY_ID, null));
        assertEquals("Location", generateTransactionTitle(sb(), null, null, "Location", Category.NO_CATEGORY_ID, null));
        assertEquals("Category", generateTransactionTitle(sb(), null, null, null, Category.NO_CATEGORY_ID, "Category"));
        assertEquals("Payee: Location: Note", generateTransactionTitle(sb(), "Payee", "Note", "Location", Category.NO_CATEGORY_ID, null));
        assertEquals("Category (Location)", generateTransactionTitle(sb(), null, null, "Location", Category.NO_CATEGORY_ID, "Category"));
        assertEquals("Category (Payee: Note)", generateTransactionTitle(sb(), "Payee", "Note", null, Category.NO_CATEGORY_ID, "Category"));
    }

    @Test
    public void test_should_generate_title_for_a_split() {
        assertEquals("[Split...]", generateTransactionTitle(sb(), null, null, null, Category.SPLIT_CATEGORY_ID, "[Split...]"));
        assertEquals("[Payee...]", generateTransactionTitle(sb(), "Payee", null, null, Category.SPLIT_CATEGORY_ID, "[Split...]"));
        assertEquals("[...] Note", generateTransactionTitle(sb(), null, "Note", null, Category.SPLIT_CATEGORY_ID, "[Split...]"));
        assertEquals("[...] Location", generateTransactionTitle(sb(), null, null, "Location", Category.SPLIT_CATEGORY_ID, "[Split...]"));
        assertEquals("[Payee...] Location: Note", generateTransactionTitle(sb(), "Payee", "Note", "Location", Category.SPLIT_CATEGORY_ID, "[Split...]"));
        assertEquals("[...] Location", generateTransactionTitle(sb(), null, null, "Location", Category.SPLIT_CATEGORY_ID, "[Split...]"));
        assertEquals("[Payee...] Note", generateTransactionTitle(sb(), "Payee", "Note", null, Category.SPLIT_CATEGORY_ID, "[Split...]"));
    }

    private StringBuilder sb() {
        return new StringBuilder();
    }

}
