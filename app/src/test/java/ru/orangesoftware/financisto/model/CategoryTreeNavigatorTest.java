package ru.orangesoftware.financisto.model;

import junit.framework.Assert;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.test.CategoryBuilder;

public class CategoryTreeNavigatorTest extends AbstractDbTest {

    Map<String, Category> categories;
    CategoryTreeNavigator navigator;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        /**
         * A
         * - A1
         * -- AA1
         * - A2
         * B
         */
        categories = CategoryBuilder.createDefaultHierarchy(db);
        navigator = new CategoryTreeNavigator(db);
    }

    @Test
    public void should_add_expense_income_level() {
        navigator.separateIncomeAndExpense();
        assertSelected(Category.NO_CATEGORY_ID, "<NO_CATEGORY>", "<INCOME>", "<EXPENSE>");

        navigator.navigateTo(CategoryTreeNavigator.EXPENSE_CATEGORY_ID);
        assertSelected(CategoryTreeNavigator.EXPENSE_CATEGORY_ID, "<EXPENSE>", "A");

        navigator.navigateTo(categories.get("A").id);
        assertSelected(categories.get("A").id, "A", "A1", "A2");

        navigator.goBack();
        assertSelected(Category.NO_CATEGORY_ID, "<EXPENSE>", "A");

        navigator.goBack();
        assertSelected(Category.NO_CATEGORY_ID, "<NO_CATEGORY>", "<INCOME>", "<EXPENSE>");
    }

    @Test
    public void should_select_startup_category() {
        long selectedCategoryId = categories.get("AA1").id;
        navigator.selectCategory(selectedCategoryId);
        Assert.assertEquals(selectedCategoryId, navigator.selectedCategoryId);
        assertSelected(selectedCategoryId, "A1", "AA1");
    }

    @Test
    public void should_navigate_to_category() {
        long categoryId = categories.get("A").id;
        navigator.navigateTo(categoryId);
        assertSelected(categoryId, "A", "A1", "A2");

        categoryId = categories.get("A1").id;
        navigator.navigateTo(categoryId);
        assertSelected(categoryId, "A1", "AA1");

        categoryId = categories.get("AA1").id;
        navigator.navigateTo(categoryId);
        assertSelected(categoryId, "A1", "AA1");
    }

    @Test
    public void should_select_parent_category_when_navigating_back() {
        long categoryId = categories.get("AA1").id;
        navigator.selectCategory(categoryId);
        assertSelected(categoryId, "A1", "AA1");
        Assert.assertTrue(navigator.canGoBack());

        Assert.assertTrue(navigator.goBack());
        assertSelected(categories.get("A1").id, "A", "A1", "A2");
        Assert.assertTrue(navigator.canGoBack());

        Assert.assertTrue(navigator.goBack());
        assertSelected(categories.get("A").id, "<NO_CATEGORY>", "A", "B");
        Assert.assertFalse(navigator.canGoBack());

        Assert.assertFalse(navigator.goBack());
    }

    private void assertSelected(long selectedCategoryId, String... categories) {
        Assert.assertEquals(selectedCategoryId, navigator.selectedCategoryId);
        List<Category> roots = navigator.getSelectedRoots();
        Assert.assertEquals("Got too many or too few categories", categories.length, roots.size());
        for (int i=0; i<categories.length; i++) {
            Assert.assertEquals("Unexpected category on index "+i, categories[i], roots.get(i).title);
        }
    }


}
