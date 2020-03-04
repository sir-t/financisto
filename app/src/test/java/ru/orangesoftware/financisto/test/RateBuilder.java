package ru.orangesoftware.financisto.test;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.ExchangeRate;

public class RateBuilder {

    private final DatabaseAdapter db;
    private final ExchangeRate r = new ExchangeRate();

    public static RateBuilder inMemory() {
        return new RateBuilder(null);
    }

    public static RateBuilder withDb(DatabaseAdapter db) {
        return new RateBuilder(db);
    }

    private RateBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public RateBuilder from(Currency c) {
        r.fromCurrencyId = c.id;
        return this;
    }

    public RateBuilder to(Currency c) {
        r.toCurrencyId = c.id;
        return this;
    }


    public RateBuilder at(DateTime date) {
        r.date = date.asLong();
        return this;
    }

    public RateBuilder rate(float rate) {
        r.rate = rate;
        return this;
    }

    public RateBuilder notOK() {
        r.error = "Exception";
        return this;
    }

    public ExchangeRate create() {
        if (db != null) {
            db.saveRate(r);
        }
        return r;
    }

}
