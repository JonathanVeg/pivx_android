package altcoin.br.pivx;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import altcoin.br.pivx.adapter.AdapterAlerts;
import altcoin.br.pivx.adapter.AdapterLinks;
import altcoin.br.pivx.data.DBTools;
import altcoin.br.pivx.model.Alert;
import altcoin.br.pivx.model.Link;
import altcoin.br.pivx.services.PriceAlertService;
import altcoin.br.pivx.utils.Bitcoin;
import altcoin.br.pivx.utils.InternetRequests;
import altcoin.br.pivx.utils.Utils;

public class MainActivity extends Activity {

    private Handler handler;

    private TextView tvLastUpdate;

    // parte dos graficos

    private LineChart marketChartBid;
    private LineChart marketChartAsk;

    private ArrayAdapter<String> adapterZoom;
    private ArrayAdapter<String> adapterCandle;

    // footer

    private ImageView bSummary;
    private ImageView bChart;
    private ImageView bCalculator;
    private ImageView bAbout;
    private ImageView bAlerts;
    private ImageView bStats;

    private ScrollView llSummary;
    private ScrollView llChart;
    private ScrollView llCalculator;
    private LinearLayout llAbout;
    private ScrollView llAlerts;
    private LinearLayout llStats;

    // calculator
    private Button bConvertBrlTo;
    private Button bConvertBtcTo;
    private Button bConvertUsdTo;
    private Button bConvertPivxTo;

    private EditText etValueToConvertBrl;
    private EditText etValueToConvertBtc;
    private EditText etValueToConvertUsd;
    private EditText etValueToConvertPivx;

    private TextView tvCalcBrlInPivx;
    private TextView tvCalcBtcInPivx;
    private TextView tvCalcUsdInPivx;
    private TextView tvCalcPivxInBrl;
    private TextView tvCalcPivxInBtc;
    private TextView tvCalcPivxInUsd;

    // about

    private List<Link> links;
    private AdapterLinks adapterLinks;

    private TextView tvAboutDeveloper;
    private TextView tvAboutCode;

    private TextView tvAboutDonateWallet;
    private TextView tvAboutDonate;

    // alerts

    private CheckBox cbAlertPoloniex;
    private CheckBox cbAlertBittrex;
    private Spinner sOptions;
    private EditText etValue;
    private Button bSaveAlert;
    private AdapterAlerts adapterAlerts;
    private List<Alert> alerts;
    private RelativeLayout rlNoAlerts;
    private LinearLayout llCurrentAlerts;
    private RecyclerView rvAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getActionBar() != null) {
            getActionBar().setDisplayShowHomeEnabled(true);
            getActionBar().setTitle("Pivx");
        }

        instanceObjects();

        prepareListeners();

        resetFooter();

        bSummary.performClick();

        startService(new Intent(this, PriceAlertService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // creating the handler for updating the altcoin.br.pivx.data constantily
        try {

            handler = new Handler();

            handler.postDelayed(runnableCode, 10000);

        } catch (Exception e) {
            Log.e("Handler", "Error while creating handler");

            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // creating the handler for updating the altcoin.br.pivx.data constantily
        try {

            handler.removeCallbacks(runnableCode);

        } catch (Exception e) {
            Log.e("Handler", "Error while pausing handler");

            e.printStackTrace();
        }
    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            tvLastUpdate.setText(" ... ");

            loadSummary();

            loadBittrexData();

            loadMarketChart();

            loadStatsData();

            // after executing it creates another instance
            // i think there is a way to make it better.
            handler = new Handler();

            handler.postDelayed(runnableCode, 10000);
        }
    };

    private void loadMarketChart() {
        String url = "https://bittrex.com/api/v1.1/public/getorderbook?market=BTC-PIVX&type=both&depth=750";


        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                new atParseMarketChart(response).execute();
            }
        };

        InternetRequests internetRequests = new InternetRequests();
        internetRequests.executePost(url, listener);
    }

    private class atParseMarketChart extends AsyncTask<Void, Void, Void> {
        String response;

        ArrayList<Entry> entriesBid;
        ArrayList<Entry> entriesAsk;

        ArrayList<String> labelsBid;
        ArrayList<String> labelsAsk;

        atParseMarketChart(String response) {
            this.response = response;

            entriesBid = new ArrayList<>();
            entriesAsk = new ArrayList<>();

            labelsBid = new ArrayList<>();
            labelsAsk = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSONObject jObject = new JSONObject(response);

                JSONArray arr;
                JSONObject obj;

                if (jObject.getBoolean("success")) {
                    // buy
                    double totalAsk = 0;

                    arr = jObject.getJSONObject("result").getJSONArray("sell");

                    for (int i = 0; i < arr.length(); i++) {
                        obj = arr.getJSONObject(i);

                        totalAsk += obj.getDouble("Quantity") * obj.getDouble("Rate");

                        entriesAsk.add(new Entry((float) totalAsk, i));
                        labelsAsk.add(Utils.numberComplete(obj.getString("Rate"), 8));
                    }

                    // sell

                    double totalBid = 0;

                    arr = jObject.getJSONObject("result").getJSONArray("buy");

                    for (int i = 0; i < arr.length(); i++) {
                        obj = arr.getJSONObject(i);

                        totalBid += obj.getDouble("Quantity") * obj.getDouble("Rate");

                        entriesBid.add(new Entry((float) totalBid, i));
                        labelsBid.add(Utils.numberComplete(obj.getString("Rate"), 8));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // bid

            // invert the data
            for (int i = 0; i < entriesBid.size(); i++)
                entriesBid.get(i).setXIndex(entriesBid.size() - 1 - i);

            Collections.reverse(labelsBid);

            LineDataSet datasetBid = new LineDataSet(entriesBid, "Bids");

            datasetBid.setColor(0xFF00FF00);

            datasetBid.setDrawValues(false);

            datasetBid.setFillColor(0xFF00FF00);

            datasetBid.setDrawCircles(false);

            datasetBid.setDrawFilled(true);

            LineData lineDataBid = new LineData(labelsBid, datasetBid);

            marketChartBid.setData(lineDataBid);

            marketChartBid.getAxisRight().setDrawLabels(false);

            marketChartBid.setDescription("");

            marketChartBid.notifyDataSetChanged();

            marketChartBid.invalidate();

            // ask

            LineDataSet datasetAsk = new LineDataSet(entriesAsk, "Asks");

            datasetAsk.setColor(0xFFFF0000);

            datasetAsk.setDrawValues(false);

            datasetAsk.setFillColor(0xFFFF0000);

            datasetAsk.setDrawCircles(false);

            datasetAsk.setDrawFilled(true);

            LineData lineDataAsk = new LineData(labelsAsk, datasetAsk);

            marketChartAsk.setData(lineDataAsk);

            marketChartAsk.getAxisRight().setDrawLabels(false);

            marketChartAsk.setDescription("");

            marketChartAsk.notifyDataSetChanged();

            marketChartAsk.invalidate();
        }

    }

    private void instanceObjects() {
        tvLastUpdate = (TextView) findViewById(R.id.tvLastUpdate);

        TextView tvOficialSite = (TextView) findViewById(R.id.tvOficialSite);
        TextView tvBittrexTitle = (TextView) findViewById(R.id.tvBittrexTitle);
        TextView tvCoinMarketCapTitle = (TextView) findViewById(R.id.tvCoinMarketCapTitle);

        Utils.textViewLink(tvOficialSite, "https://pivx.info/");
        Utils.textViewLink(tvBittrexTitle, "https://coinmarketcap.com/exchanges/bittrex/");
        Utils.textViewLink(tvCoinMarketCapTitle, "https://coinmarketcap.com/currencies/pivx/#markets");

        // parte dos graficos

        // market chart

        marketChartBid = (LineChart) findViewById(R.id.marketChartBid);
        marketChartAsk = (LineChart) findViewById(R.id.marketChartAsk);

        // footer

        bSummary = (ImageView) findViewById(R.id.bSummary);
        bChart = (ImageView) findViewById(R.id.bChart);
        bCalculator = (ImageView) findViewById(R.id.bCalculator);
        bAbout = (ImageView) findViewById(R.id.bAbout);
        bStats = (ImageView) findViewById(R.id.bStats);
        bAlerts = (ImageView) findViewById(R.id.bAlerts);

        llSummary = (ScrollView) findViewById(R.id.llSummary);
        llChart = (ScrollView) findViewById(R.id.llChart);
        llCalculator = (ScrollView) findViewById(R.id.llCalculator);
        llAbout = (LinearLayout) findViewById(R.id.llAbout);
        llStats = (LinearLayout) findViewById(R.id.llStats);
        llAlerts = (ScrollView) findViewById(R.id.llAlerts);

        instanceObjectsCalculator();

        instanceObjectsAbout();

        instanceObjectsAlerts();
    }

    private void instanceObjectsAlerts() {
        rvAlerts = (RecyclerView) findViewById(R.id.rvAlerts);
        sOptions = (Spinner) findViewById(R.id.sOptions);
        etValue = (EditText) findViewById(R.id.etValue);
        bSaveAlert = (Button) findViewById(R.id.bSaveAlert);
        rlNoAlerts = (RelativeLayout) findViewById(R.id.rlNoAlerts);
        llCurrentAlerts = (LinearLayout) findViewById(R.id.llCurrentAlerts);
        cbAlertPoloniex = (CheckBox) findViewById(R.id.cbAlertPoloniex);
        cbAlertBittrex = (CheckBox) findViewById(R.id.cbAlertBittrex);

        alerts = new ArrayList<>();

        adapterAlerts = new AdapterAlerts(this, alerts);

        rvAlerts.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        rvAlerts.setLayoutManager(linearLayoutManager);
        rvAlerts.setAdapter(adapterAlerts);
    }

    private void instanceObjectsAbout() {
        ListView lvLinks = (ListView) findViewById(R.id.lvLinks);

        links = new ArrayList<>();

        adapterLinks = new AdapterLinks(this, links);

        lvLinks.setAdapter(adapterLinks);

        tvAboutDeveloper = (TextView) findViewById(R.id.tvAboutDeveloper);
        tvAboutCode = (TextView) findViewById(R.id.tvAboutCode);

        tvAboutDonateWallet = (TextView) findViewById(R.id.tvAboutDonateWallet);
        tvAboutDonate = (TextView) findViewById(R.id.tvAboutDonate);
    }

    private void instanceObjectsCalculator() {
        bConvertBrlTo = (Button) findViewById(R.id.bConvertBrlTo);
        bConvertBtcTo = (Button) findViewById(R.id.bConvertBtcTo);
        bConvertUsdTo = (Button) findViewById(R.id.bConvertUsdTo);
        bConvertPivxTo = (Button) findViewById(R.id.bConvertPivxTo);

        etValueToConvertBrl = (EditText) findViewById(R.id.etValueToConvertBrl);
        etValueToConvertBtc = (EditText) findViewById(R.id.etValueToConvertBtc);
        etValueToConvertUsd = (EditText) findViewById(R.id.etValueToConvertUsd);
        etValueToConvertPivx = (EditText) findViewById(R.id.etValueToConvertPivx);

        tvCalcBrlInPivx = (TextView) findViewById(R.id.tvCalcBrlInPivx);
        tvCalcBtcInPivx = (TextView) findViewById(R.id.tvCalcBtcInPivx);
        tvCalcUsdInPivx = (TextView) findViewById(R.id.tvCalcUsdInPivx);
        tvCalcPivxInBrl = (TextView) findViewById(R.id.tvCalcPivxInBrl);
        tvCalcPivxInBtc = (TextView) findViewById(R.id.tvCalcPivxInBtc);
        tvCalcPivxInUsd = (TextView) findViewById(R.id.tvCalcPivxInUsd);

        // load in the lasts values used

        etValueToConvertBrl.setText(Utils.readPreference(this, "etValueToConvertBrl", "0"));
        etValueToConvertBtc.setText(Utils.readPreference(this, "etValueToConvertBtc", "0"));
        etValueToConvertUsd.setText(Utils.readPreference(this, "etValueToConvertUsd", "0"));
        etValueToConvertPivx.setText(Utils.readPreference(this, "etValueToConvertPivx", "0"));
    }

    private void prepareListeners() {
        prepareListenersAlerts();

        prepareListenersCalculator();

        prepareListenersAbout();

        prepareMenuListeners();
    }

    private void prepareListenersAbout() {
        tvAboutDonateWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String wallet = tvAboutDonateWallet.getText().toString();

                    Utils.copyToClipboard(MainActivity.this, wallet);

                    Toast.makeText(MainActivity.this, "Wallet WALLET copied to clipboard".replaceAll("WALLET", wallet), Toast.LENGTH_LONG).show();

                    Utils.logFabric("donationWalletCopied");
                } catch (Exception e) {
                    e.printStackTrace();

                    Toast.makeText(MainActivity.this, "Error while copying wallet", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    private void prepareListenersAlerts() {
        bSaveAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    hideKeyboard();

                    Alert alert = new Alert(MainActivity.this);

                    alert.setWhen(sOptions.getSelectedItemPosition() == 0 ? Alert.GREATER : Alert.LOWER);

                    alert.setValue(etValue.getText().toString());

                    alert.setBittrex(cbAlertBittrex.isChecked());

                    alert.setPoloniex(cbAlertPoloniex.isChecked());

                    alert.setActive(true);

                    if (alert.save()) {
                        Utils.alert(MainActivity.this, "Alert saved");

                        new atLoadAlerts(MainActivity.this).execute();

                        stopService(new Intent(MainActivity.this, PriceAlertService.class));

                        startService(new Intent(MainActivity.this, PriceAlertService.class));

                        String where = "";

                        if (alert.isPoloniex()) where += "P";

                        if (alert.isBittrex()) where += "B";

                        Utils.logFabric("alertSaved", "where", where);
                    } else
                        Utils.alert(MainActivity.this, "Error while saving alert");
                } catch (Exception e) {
                    e.printStackTrace();

                    Utils.alert(MainActivity.this, "Error while saving alert");
                }
            }
        });
    }

    private void prepareListenersCalculator() {
        bConvertBtcTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyEditTextNull(etValueToConvertBtc)) {
                    Utils.logFabric("calculator", "operation", "btcTo");

                    hideKeyboard();

                    Utils.writePreference(MainActivity.this, "etValueToConvertBtc", etValueToConvertBtc.getText().toString());

                    Response.Listener<String> listener = new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {

                                JSONObject obj = new JSONArray(response).getJSONObject(0);

                                double quantity = Double.parseDouble(etValueToConvertBtc.getText().toString());

                                tvCalcBtcInPivx.setText(Utils.numberComplete(String.format("%s", quantity / Double.parseDouble(obj.getString("price_btc"))), 8));

                            } catch (Exception e) {
                                e.printStackTrace();

                                Toast.makeText(MainActivity.this, "Error while converting", Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    execApiCall(listener);
                }
            }
        });

        bConvertUsdTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyEditTextNull(etValueToConvertUsd)) {
                    Utils.logFabric("calculator", "operation", "usdTo");

                    hideKeyboard();

                    Utils.writePreference(MainActivity.this, "etValueToConvertUsd", etValueToConvertUsd.getText().toString());

                    Response.Listener<String> listener = new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {

                                JSONObject obj = new JSONArray(response).getJSONObject(0);

                                double quantity = Double.parseDouble(etValueToConvertUsd.getText().toString());

                                tvCalcUsdInPivx.setText(Utils.numberComplete(String.format("%s", quantity / Double.parseDouble(obj.getString("price_usd"))), 8));

                            } catch (Exception e) {
                                e.printStackTrace();

                                Toast.makeText(MainActivity.this, "Error while converting", Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    execApiCall(listener);
                }
            }
        });

        bConvertBrlTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyEditTextNull(etValueToConvertBrl)) {
                    Utils.logFabric("calculator", "operation", "brlTo");

                    hideKeyboard();

                    Utils.writePreference(MainActivity.this, "etValueToConvertBrl", etValueToConvertBrl.getText().toString());

                    Response.Listener<String> listener2 = new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject obj = new JSONObject(response);

                                final double quantity = Double.parseDouble(etValueToConvertBrl.getText().toString()) / obj.getDouble("last");

                                Response.Listener<String> listener = new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {

                                            JSONObject obj = new JSONArray(response).getJSONObject(0);

                                            tvCalcBrlInPivx.setText(Utils.numberComplete(String.format("%s", quantity / Double.parseDouble(obj.getString("price_btc"))), 4));

                                        } catch (Exception e) {
                                            e.printStackTrace();

                                            Toast.makeText(MainActivity.this, "Error while converting", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                };

                                execApiCall(listener);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    Bitcoin.convertBtcToBrl(listener2);
                }
            }
        });

        bConvertPivxTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyEditTextNull(etValueToConvertPivx)) {
                    Utils.logFabric("calculator", "operation", "pivxTo");

                    hideKeyboard();

                    Utils.writePreference(MainActivity.this, "etValueToConvertPivx", etValueToConvertPivx.getText().toString());

                    Response.Listener<String> listener = new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {

                                final JSONObject obj = new JSONArray(response).getJSONObject(0);

                                final double quantity = Double.parseDouble(etValueToConvertPivx.getText().toString());

                                tvCalcPivxInBtc.setText(Utils.numberComplete(String.format("%s", quantity * Double.parseDouble(obj.getString("price_btc"))), 8));

                                tvCalcPivxInUsd.setText(Utils.numberComplete(String.format("%s", quantity * Double.parseDouble(obj.getString("price_usd"))), 4));

                                Response.Listener<String> listener = new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            JSONObject obj2 = new JSONObject(response);

                                            tvCalcPivxInBrl.setText(Utils.numberComplete(Double.parseDouble(obj.getString("price_btc")) * obj2.getDouble("last") * quantity, 4));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };

                                Bitcoin.convertBtcToBrl(listener);

                            } catch (Exception e) {
                                e.printStackTrace();

                                Toast.makeText(MainActivity.this, "Error while converting", Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    execApiCall(listener);
                }
            }
        });

    }

    private void loadBittrexData() {
        String url = "https://bittrex.com/api/v1.1/public/getmarketsummary?market=BTC-PIVX";

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                new atParseBittrexData(response).execute();
            }
        };

        InternetRequests internetRequests = new InternetRequests();
        internetRequests.executePost(url, listener);
    }

    private void loadSummary() {
        String url = "https://api.coinmarketcap.com/v1/ticker/pivx/";

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                new atParseSummaryData(response).execute();
            }
        };

        InternetRequests internetRequests = new InternetRequests();
        internetRequests.executeGet(url, listener);
    }

    private class atParseBittrexData extends AsyncTask<Void, Void, Void> {
        String response;

        String last;
        String baseVolume;
        String ask;
        String bid;
        String changes;

        atParseBittrexData(String response) {
            this.response = response;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {

                JSONObject obj = new JSONObject(response);

                if (obj.getBoolean("success")) {
                    obj = obj.getJSONArray("result").getJSONObject(0);

                    last = Utils.numberComplete(obj.getString("Last"), 8);
                    baseVolume = Utils.numberComplete(obj.getString("BaseVolume"), 8);
                    ask = Utils.numberComplete(obj.getString("Ask"), 8);
                    bid = Utils.numberComplete(obj.getString("Bid"), 8);

                    // the api does not give the % changes, but we can calculate it using the prevDay and last values
                    Double prev = obj.getDouble("PrevDay");

                    double c = (prev - Double.parseDouble(last)) / prev * (-100);

                    changes = Utils.numberComplete("" + c, 2);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            TextView tvBittrexLast = (TextView) findViewById(R.id.tvBittrexLast);
            TextView tvBittrexBaseVolume = (TextView) findViewById(R.id.tvBittrexBaseVolume);
            TextView tvBittrexBid = (TextView) findViewById(R.id.tvBittrexBid);
            TextView tvBittrexAsk = (TextView) findViewById(R.id.tvBittrexAsk);
            TextView tvBittrexChanges = (TextView) findViewById(R.id.tvBittrexChanges);

            tvBittrexLast.setText(last);
            tvBittrexBaseVolume.setText(baseVolume);
            tvBittrexBid.setText(bid);
            tvBittrexAsk.setText(ask);
            tvBittrexChanges.setText(String.format("%s%%", changes));

            if (changes == null) changes = "0";

            if (Double.parseDouble(changes) >= 0)
                tvBittrexChanges.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorChangesUp));
            else
                tvBittrexChanges.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorChangesDown));
        }
    }

    private class atParseSummaryData extends AsyncTask<Void, Void, Void> {
        String response;

        String usdPrice;
        String btcPrice;
        String usdVolume24h;
        String p24hChanges;
        String usdMarketCap;

        atParseSummaryData(String response) {
            this.response = response;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {

                JSONObject obj = new JSONArray(response).getJSONObject(0);

                usdPrice = Utils.numberComplete(obj.getString("price_usd"), 4);
                btcPrice = Utils.numberComplete(obj.getString("price_btc"), 8);
                p24hChanges = Utils.numberComplete(obj.getString("percent_change_24h"), 2);
                usdVolume24h = Utils.numberComplete(obj.getString("24h_volume_usd"), 4);
                usdMarketCap = Utils.numberComplete(obj.getString("market_cap_usd"), 4);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            TextView tvSummaryBtcPrice = (TextView) findViewById(R.id.tvSummaryBtcPrice);
            TextView tvSummaryUsdPrice = (TextView) findViewById(R.id.tvSummaryUsdPrice);
            final TextView tvSummaryBrlPrice = (TextView) findViewById(R.id.tvSummaryBrlPrice);
            TextView tvSummaryUsd24hVolume = (TextView) findViewById(R.id.tvSummaryUsd24hVolume);
            TextView tvSummaryUsdMarketCap = (TextView) findViewById(R.id.tvSummaryUsdMarketCap);
            TextView tvSummary24hChanges = (TextView) findViewById(R.id.tvSummary24hChanges);

            tvSummaryBtcPrice.setText(btcPrice);
            tvSummaryUsdPrice.setText(usdPrice);
            tvSummaryUsd24hVolume.setText(usdVolume24h);
            tvSummaryUsdMarketCap.setText(usdMarketCap);
            tvSummary24hChanges.setText(String.format("%s%%", p24hChanges));

            if (Double.parseDouble(p24hChanges) >= 0)
                tvSummary24hChanges.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorChangesUp));
            else
                tvSummary24hChanges.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorChangesDown));

            Response.Listener<String> listener = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject obj = new JSONObject(response);

                        tvSummaryBrlPrice.setText(Utils.numberComplete(Double.parseDouble(btcPrice) * obj.getDouble("last"), 4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            Bitcoin.convertBtcToBrl(listener);

            tvLastUpdate.setText(Utils.now());
        }
    }

    private void resetFooter() {
        bSummary.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_silver));
        bChart.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_silver));
        bCalculator.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_silver));
        bAbout.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_silver));
        bStats.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_silver));
        bAlerts.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_silver));

        llSummary.setVisibility(View.GONE);
        llChart.setVisibility(View.GONE);
        llCalculator.setVisibility(View.GONE);
        llAbout.setVisibility(View.GONE);
        llAlerts.setVisibility(View.GONE);
        llStats.setVisibility(View.GONE);
    }

    private void prepareMenuListeners() {
        bSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadSummary();

                loadBittrexData();

                resetFooter();

                bSummary.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.silver));

                llSummary.setVisibility(View.VISIBLE);

                Utils.logFabric("tabChanged", "tab", "summary");
            }
        });

        bChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadMarketChart();

                resetFooter();

                bChart.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.silver));

                llChart.setVisibility(View.VISIBLE);

                Utils.logFabric("tabChanged", "tab", "chart");
            }
        });

        bCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFooter();

                bCalculator.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.silver));

                llCalculator.setVisibility(View.VISIBLE);

                Utils.logFabric("tabChanged", "tab", "calculator");
            }
        });

        bAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFooter();

                bAbout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.silver));

                llAbout.setVisibility(View.VISIBLE);

                prepareLinks();

                Utils.logFabric("tabChanged", "tab", "about");
            }
        });

        bAlerts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFooter();

                bAlerts.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.silver));

                llAlerts.setVisibility(View.VISIBLE);

                new atLoadAlerts(MainActivity.this).execute();

                Utils.logFabric("tabChanged", "tab", "alerts");
            }
        });

        bStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFooter();

                bStats.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.silver));

                llStats.setVisibility(View.VISIBLE);

                loadStatsData();

                Utils.logFabric("tabChanged", "tab", "stats");
            }
        });
    }

    private long statsTimeStamp = 0;

    private void loadStatsData() {
        statsTimeStamp = Utils.timestampLong();

        String url = "https://pivxstats.com/api/v1/get_stats?" + statsTimeStamp;

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                new atParseStatsData(MainActivity.this, response).execute();

                String url2 = "https://pivxstats.com/api/v1/fees";

                Response.Listener<String> listener2 = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        new atParseStatsBlockData(MainActivity.this, response).execute();
                    }
                };

                InternetRequests internetRequests = new InternetRequests();

                internetRequests.executeGet(url2, listener2);
            }
        };

        InternetRequests internetRequests = new InternetRequests();

        internetRequests.executeGet(url, listener);
    }

    private class atParseStatsBlockData extends AsyncTask<Void, Void, Void> {
        Context context;
        String response;

        String statsLastBlockNumber = "";

        atParseStatsBlockData(Context c, String r) {
            context = c;

            response = r;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSONArray arr = new JSONArray(response);

                statsLastBlockNumber = arr.getJSONObject(0).getString("height");

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            TextView tvStatsLastBlockNumber = (TextView) findViewById(R.id.tvStatsLastBlockNumber);

            tvStatsLastBlockNumber.setText(statsLastBlockNumber);
        }
    }

    private class atParseStatsData extends AsyncTask<Void, Void, Void> {
        String response;

        String statsTicketPrice = ""; // done
        String statsNextTicketPrice = ""; // done
        String statsMinTicketPrice = ""; // done
        String statsMaxTicketPrice = ""; // done
        String statsLastBlockDatetime = ""; // done

        String statsLastAvgBlockTime = ""; // done
        String statsLastAvgHashrate = ""; // done
        String statsStakeReward = "";
        String statsAdjustIn = "";
        String statsAvailableSupply = "";
        String statsPowReward = "";
        String statsTicketPollSize = ""; // done
        String statsLockedPivx = "";
        String statsAvgTicketPrice = "";

        atParseStatsData(Context c, String r) {

            response = r;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSONObject obj = new JSONObject(response);

                statsTicketPrice = Utils.numberComplete(obj.getString("sbits"), 2);
                statsNextTicketPrice = Utils.numberComplete(obj.getString("est_sbits"), 2);
                statsMinTicketPrice = Utils.numberComplete(obj.getString("est_sbits_min"), 2);
                statsMaxTicketPrice = Utils.numberComplete(obj.getString("est_sbits_max"), 2);

                Long timestamp = statsTimeStamp - obj.getLong("last_block_datetime");

                String min = (timestamp / 60) + "";
                String sec = (timestamp % 60) + "";

                if (min.length() == 1)
                    min = "0" + min;

                if (sec.length() == 1)
                    sec = "0" + sec;

                statsLastBlockDatetime = min + ":" + sec;
                statsLastAvgBlockTime = obj.getString("average_minutes") + ":" + obj.getString("average_seconds");

                statsTicketPollSize = obj.getString("poolsize");

                statsLastAvgHashrate = Utils.numberComplete(obj.getLong("networkhashps") / (1000000000000.0), 2);

                statsAvailableSupply = String.valueOf(Utils.numberComplete(obj.getLong("coinsupply") / 1000000.0, 0));

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            TextView tvStatsTicketPrice = (TextView) findViewById(R.id.tvStatsTicketPrice);
            TextView tvStatsNextTicketPrice = (TextView) findViewById(R.id.tvStatsNextTicketPrice);
            TextView tvStatsMinTicketPrice = (TextView) findViewById(R.id.tvStatsMinTicketPrice);
            TextView tvStatsMaxTicketPrice = (TextView) findViewById(R.id.tvStatsMaxTicketPrice);
            TextView tvStatsLastBlockDatetime = (TextView) findViewById(R.id.tvStatsLastBlockDatetime);

            TextView tvStatsLastAvgBlockTime = (TextView) findViewById(R.id.tvStatsLastAvgBlockTime);
            TextView tvStatsLastAvgHashrate = (TextView) findViewById(R.id.tvStatsLastAvgHashrate);
            TextView tvStatsStakeReward = (TextView) findViewById(R.id.tvStatsStakeReward);
            TextView tvStatsAdjustIn = (TextView) findViewById(R.id.tvStatsAdjustIn);
            TextView tvStatsAvailableSupply = (TextView) findViewById(R.id.tvStatsAvailableSupply);
            TextView tvStatsPowReward = (TextView) findViewById(R.id.tvStatsPowReward);
            TextView tvStatsTicketPollSize = (TextView) findViewById(R.id.tvStatsTicketPollSize);
            TextView tvStatsLockedPivx = (TextView) findViewById(R.id.tvStatsLockedPivx);
            TextView tvStatsAvgTicketPrice = (TextView) findViewById(R.id.tvStatsAvgTicketPrice);

            tvStatsTicketPrice.setText(statsTicketPrice);
            tvStatsNextTicketPrice.setText(statsNextTicketPrice);
            tvStatsMinTicketPrice.setText(statsMinTicketPrice);
            tvStatsMaxTicketPrice.setText(statsMaxTicketPrice);
            tvStatsLastBlockDatetime.setText(statsLastBlockDatetime);
            tvStatsLastAvgBlockTime.setText(statsLastAvgBlockTime);
            tvStatsLastAvgHashrate.setText(statsLastAvgHashrate);
            tvStatsStakeReward.setText(statsStakeReward);
            tvStatsAdjustIn.setText(statsAdjustIn);
            tvStatsAvailableSupply.setText(statsAvailableSupply);
            tvStatsPowReward.setText(statsPowReward);
            tvStatsTicketPollSize.setText(statsTicketPollSize);
            tvStatsLockedPivx.setText(statsLockedPivx);
            tvStatsAvgTicketPrice.setText(statsAvgTicketPrice);

            tvStatsLastBlockDatetime.setText(statsLastBlockDatetime);
        }
    }

    private boolean verifyEditTextNull(EditText et) {
        if (et.getText().toString().equals("")) {
            Toast.makeText(this, "You need to fill the box", Toast.LENGTH_SHORT).show();

            return false;
        }

        return true;
    }

    private void hideKeyboard() {
        try {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void execApiCall(Response.Listener<String> listener) {

        String url = "https://api.coinmarketcap.com/v1/ticker/pivx/";

        InternetRequests internetRequests = new InternetRequests();

        internetRequests.executeGet(url, listener);

    }

    private void prepareLinks() {
        Utils.textViewLink(tvAboutDeveloper, "https://twitter.com/jonathanveg2");
        Utils.textViewLink(tvAboutCode, "https://github.com/JonathanVeg/pivx_android");

        try {
            String value = "";
            value += "Official site,https://pivx.org/,";
            value += "Slack,https://pivx.slack.com/messages,";
            value += "Twitter,https://twitter.com/_pivx,";
            value += "-,-,";
            value += "Dev Twitter,https://twitter.com/JonathanVeg2,";

            List<Link> localLinks = new ArrayList<>();

            String[] arrLinks = value.split(",");

            for (int i = 0; i < arrLinks.length; i += 2) {
                localLinks.add(new Link(arrLinks[i], arrLinks[i + 1]));
            }

            links.clear();

            links.addAll(localLinks);

            adapterLinks.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class atLoadAlerts extends io.fabric.sdk.android.services.concurrency.AsyncTask<Void, Void, Void> {
        Context context;

        List<Alert> list;

        atLoadAlerts(Context context) {
            this.context = context;

            list = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            DBTools db = new DBTools(context);

            try {
                int count = db.search("select * from alerts order by created_at desc");

                Alert alert;

                for (int i = 0; i < count; i++) {
                    alert = new Alert(MainActivity.this);

                    alert.setId(db.getData(i, 0));
                    alert.setWhen(db.getData(i, 1));
                    alert.setValue(db.getData(i, 2));
                    alert.setCreatedAt(db.getData(i, 3));
                    alert.setActive(Utils.isTrue(db.getData(i, 4)));
                    alert.setPoloniex(false);
                    alert.setBittrex(Utils.isTrue(db.getData(i, 6)));

                    list.add(alert);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.close();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            alerts.clear();

            alerts.addAll(list);

            adapterAlerts.notifyDataSetChanged();

            correctListVisibility();
        }
    }

    public void correctListVisibility() {
        if (alerts.size() > 0) {
            rlNoAlerts.setVisibility(View.GONE);
            llCurrentAlerts.setVisibility(View.VISIBLE);
            rvAlerts.setVisibility(View.VISIBLE);
        } else {
            llCurrentAlerts.setVisibility(View.GONE);
            rvAlerts.setVisibility(View.GONE);
            rlNoAlerts.setVisibility(View.VISIBLE);
        }
    }
}
