package ru.orangesoftware.financisto.test;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.TransactionAttribute;

public class AttributeBuilder {

    private final DatabaseAdapter db;

    private AttributeBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public static AttributeBuilder withDb(DatabaseAdapter db) {
        return new AttributeBuilder(db);
    }

    public Attribute createTextAttribute(String name) {
        return createAttribute(name, Attribute.TYPE_TEXT);
    }

    public Attribute createNumberAttribute(String name) {
        return createAttribute(name, Attribute.TYPE_NUMBER);
    }

    private Attribute createAttribute(String name, int type) {
        Attribute a = new Attribute();
        a.title = name;
        a.type = type;
        a.id = db.insertOrUpdate(a);
        return a;
    }

    public static TransactionAttribute attributeValue(Attribute a, String value) {
        TransactionAttribute ta = new TransactionAttribute();
        ta.attributeId = a.id;
        ta.value = value;
        return ta;
    }

}