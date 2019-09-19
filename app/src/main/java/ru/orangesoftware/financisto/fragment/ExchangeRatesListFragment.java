package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.ExchangeRateActivity;
import ru.orangesoftware.financisto.databinding.ExchangeRateListBinding;
import ru.orangesoftware.financisto.databinding.GenericRecyclerItemBinding;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSwipeable;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;

import static ru.orangesoftware.financisto.utils.Utils.formatRateDate;

public class ExchangeRatesListFragment extends AbstractRecycleFragment implements ItemClick, ItemSwipeable {

    private static final int ADD_RATE = 1;
    private static final int EDIT_RATE = 2;

    private final DecimalFormat nf = new DecimalFormat("0.00000");

    private Spinner fromCurrencySpinner;
    private Spinner toCurrencySpinner;
    private List<Currency> currencies;

    private long lastSelectedCurrencyId;

    public ExchangeRatesListFragment() {
        super(R.layout.exchange_rate_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currencies = db.getAllCurrenciesList("name");

        ExchangeRateListBinding binding = (ExchangeRateListBinding) getBinding();
        binding.bAdd.setOnClickListener(v -> {
            long fromCurrencyId = fromCurrencySpinner.getSelectedItemId();
            long toCurrencyId = toCurrencySpinner.getSelectedItemId();
            if (fromCurrencyId > 0 && toCurrencyId > 0) {
                Intent intent = new Intent(context, ExchangeRateActivity.class);
                intent.putExtra(ExchangeRateActivity.FROM_CURRENCY_ID, fromCurrencyId);
                intent.putExtra(ExchangeRateActivity.TO_CURRENCY_ID, toCurrencyId);
                startActivityForResult(intent, ADD_RATE);
            }
        });
        fromCurrencySpinner = binding.spinnerFromCurrency;
        fromCurrencySpinner.setPromptId(R.string.rate_from_currency);
        toCurrencySpinner = binding.spinnerToCurrency;
        toCurrencySpinner.setPromptId(R.string.rate_to_currency);

        if (currencies.size() > 0) {
            toCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    updateAdapter();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            fromCurrencySpinner.setAdapter(createCurrencyAdapter(currencies));
            fromCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                    List<Currency> currencies = getCurrenciesButSelected(id);
                    if (currencies.size() > 0) {
                        int position = findSelectedCurrency(currencies, lastSelectedCurrencyId);
                        toCurrencySpinner.setAdapter(createCurrencyAdapter(currencies));
                        toCurrencySpinner.setSelection(position);
                    }
                    lastSelectedCurrencyId = id;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            fromCurrencySpinner.setSelection(findDefaultCurrency());

            binding.bFlip.setOnClickListener(arg0 -> flipCurrencies());
            binding.bRefresh.setOnClickListener(arg0 -> refreshAllRates());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            updateAdapter();
        }
    }

    @Override
    protected void updateAdapter() {
        Currency fromCurrency = (Currency) fromCurrencySpinner.getSelectedItem();
        Currency toCurrency = (Currency) toCurrencySpinner.getSelectedItem();
        if (fromCurrency != null && toCurrency != null) {
            List<ExchangeRate> rates = db.findRates(fromCurrency, toCurrency);
            setListAdapter(new ExchangeRateRecyclerAdapter(rates));
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        editItem(position);
    }

    @Override
    public Integer[] getSwipeOptions() {
        return new Integer[]{R.id.delete_task, R.id.edit_task};
    }

    @Override
    public void onSwipeClick(int viewID, int position) {
        switch (viewID) {
            case R.id.delete_task:
                deleteItem(position);
                break;
            case R.id.edit_task:
                editItem(position);
                break;
        }
    }

    protected void deleteItem(int position) {
        ExchangeRate rate = ((ExchangeRateRecyclerAdapter) getListAdapter()).getItem(position);
        db.deleteRate(rate);
        updateAdapter();
    }

    protected void editItem(int position) {
        ExchangeRate rate = ((ExchangeRateRecyclerAdapter) getListAdapter()).getItem(position);

        Intent intent = new Intent(context, ExchangeRateActivity.class);
        intent.putExtra(ExchangeRateActivity.FROM_CURRENCY_ID, rate.fromCurrencyId);
        intent.putExtra(ExchangeRateActivity.TO_CURRENCY_ID, rate.toCurrencyId);
        intent.putExtra(ExchangeRateActivity.RATE_DATE, rate.date);
        startActivityForResult(intent, EDIT_RATE);
    }

    private SpinnerAdapter createCurrencyAdapter(List<Currency> currencies) {
        ArrayAdapter<Currency> a = new ArrayAdapter<Currency>(context, android.R.layout.simple_spinner_item, currencies) {
            @Override
            public long getItemId(int position) {
                return getItem(position).id;
            }
        };
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private List<Currency> getCurrenciesButSelected(long id) {
        List<Currency> list = new ArrayList<>();
        for (Currency currency : currencies) {
            if (currency.id != id) {
                list.add(currency);
            }
        }
        return list;
    }

    private int findSelectedCurrency(List<Currency> currencies, long id) {
        int i = 0;
        for (Currency currency : currencies) {
            if (currency.id == id) {
                return i;
            }
            ++i;
        }
        return 0;
    }

    private int findDefaultCurrency() {
        int i = 0;
        for (Currency currency : currencies) {
            if (currency.isDefault) {
                return i;
            }
            ++i;
        }
        return 0;
    }

    private void flipCurrencies() {
        Currency toCurrency = (Currency) toCurrencySpinner.getSelectedItem();
        if (toCurrency != null) {
            fromCurrencySpinner.setSelection(findSelectedCurrency(currencies, toCurrency.id));
        }
    }

    private void refreshAllRates() {
        new RatesDownloadTask(context).execute();
    }

    private class RatesDownloadTask extends AsyncTask<Void, Void, List<ExchangeRate>> {

        private final Context context;
        private ProgressDialog progressDialog;

        private RatesDownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected List<ExchangeRate> doInBackground(Void... args) {
            List<ExchangeRate> rates = getProvider().getRates(currencies);
            if (isCancelled()) {
                return null;
            } else {
                db.saveDownloadedRates(rates);
                return rates;
            }
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        private void showProgressDialog() {
            String message = context.getString(R.string.downloading_rates, asString(currencies));
            progressDialog = ProgressDialog.show(context, null, message, true, true, dialogInterface -> cancel(true));
        }

        private String asString(List<Currency> currencies) {
            StringBuilder sb = new StringBuilder();
            for (Currency currency : currencies) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(currency.name);
            }
            return sb.toString();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(List<ExchangeRate> result) {
            progressDialog.dismiss();
            if (result != null) {
                showResult(result);
                updateAdapter();
            }
        }

        private void showResult(List<ExchangeRate> result) {
            StringBuilder sb = new StringBuilder();
            for (ExchangeRate rate : result) {
                Currency fromCurrency = CurrencyCache.getCurrency(db, rate.fromCurrencyId);
                Currency toCurrency = CurrencyCache.getCurrency(db, rate.toCurrencyId);
                sb.append(fromCurrency.name).append(" -> ").append(toCurrency.name);
                if (rate.isOk()) {
                    sb.append(" = ").append(nf.format(rate.rate));
                } else {
                    sb.append(" ! ").append(rate.getErrorMessage());
                }
                sb.append(String.format("%n%n"));
            }
            new AlertDialog.Builder(context)
                    .setTitle(R.string.downloading_rates_result)
                    .setMessage(sb.toString())
                    .setNeutralButton(R.string.ok, null)
                    .create().show();
        }

        private ExchangeRateProvider getProvider() {
            return MyPreferences.createExchangeRatesProvider(context);
        }

    }

    private class ExchangeRateItemHolder extends RecyclerView.ViewHolder {

        private final GenericRecyclerItemBinding mBinding;

        ExchangeRateItemHolder(GenericRecyclerItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(ExchangeRate rate) {
            mBinding.line.setText(formatRateDate(context, rate.date));
            mBinding.amount.setText(nf.format(rate.rate));
        }

    }

    private class ExchangeRateRecyclerAdapter extends RecyclerView.Adapter<ExchangeRateItemHolder> {

        private final List<ExchangeRate> rates;

        private ExchangeRateRecyclerAdapter(List<ExchangeRate> rates) {
            this.rates = rates;
        }

        @NonNull
        @Override
        public ExchangeRateItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            GenericRecyclerItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.generic_recycler_item, parent, false);
            return new ExchangeRateItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ExchangeRateItemHolder holder, int position) {
            ExchangeRate rate = rates.get(position);
            holder.bind(rate);
        }

        @Override
        public int getItemCount() {
            return rates.size();
        }

        public ExchangeRate getItem(int position) {
            return rates.get(position);
        }

    }

}
