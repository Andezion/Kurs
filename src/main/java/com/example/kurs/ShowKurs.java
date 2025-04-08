package com.example.kurs;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ShowKurs
{
    @FXML
    private ComboBox<String> fromCurrencyBox;
    @FXML
    private ComboBox<String> toCurrencyBox;
    @FXML
    private ComboBox<String> periodBox;
    @FXML
    private TextArea resultArea;

    @FXML
    private DatePicker specificDatePicker;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField daysField;

    @FXML
    private LineChart<String, Number> rateChart;

    @FXML
    public void initialize()
    {
        fromCurrencyBox.getItems().addAll("EUR", "USD", "GBP", "CHF", "JPY");
        toCurrencyBox.getItems().addAll("PLN");
        periodBox.getItems().addAll("Now", "Today", "Last N days", "For the date", "Custom period");

        fromCurrencyBox.setValue("EUR");
        toCurrencyBox.setValue("PLN");

        periodBox.setValue("Pick");

        periodBox.setOnAction(e ->
        {
            String selected = periodBox.getValue();
            boolean isDate = selected.equals("For the date");
            boolean isRange = selected.equals("Custom period");
            boolean isNDays = selected.equals("Last N days");

            daysField.setDisable(!isNDays);
            specificDatePicker.setDisable(!isDate);
            startDatePicker.setDisable(!isRange);
            endDatePicker.setDisable(!isRange);
        });


        daysField.setDisable(true);
        specificDatePicker.setDisable(true);
        startDatePicker.setDisable(true);
        endDatePicker.setDisable(true);

        updateResult();
    }

    @FXML
    public void onUpdateButtonClick(ActionEvent actionEvent)
    {
        updateResult();
    }

    private void updateResult()
    {
        String from = fromCurrencyBox.getValue();
        String period = periodBox.getValue();

        try
        {
            String result;

            switch (period)
            {
                case "Now":
                    result = getCurrentRate(from);
                    break;
                case "Today":
                    result = getTodayRate(from);
                    break;
                case "Last N days":
                    String text = daysField.getText();
                    if (text != null && !text.isEmpty())
                    {
                        try
                        {
                            int days = Integer.parseInt(text);

                            if(days <= 0)
                            {
                                result = "Integer should be > 0";
                            }
                            else
                            {
                                result = getLastNDaysRates(from, days);
                            }
                        }
                        catch (NumberFormatException e)
                        {
                            result = "Enter an integer";
                        }
                    }
                    else
                    {
                        result = "Field for days is empty";
                    }
                    break;
                case "For the date":
                    if (specificDatePicker.getValue() != null)
                    {
                        String date = specificDatePicker.getValue().toString();
                        result = getRateForDate(from, date);
                    }
                    else
                    {
                        result = "Выберите дату.";
                    }
                    break;
                case "Custom period":
                    if (startDatePicker.getValue() != null && endDatePicker.getValue() != null)
                    {
                        String start = startDatePicker.getValue().toString();
                        String end = endDatePicker.getValue().toString();
                        result = getRatesForPeriod(from, start, end);
                    }
                    else
                    {
                        result = "Select both dates";
                    }
                    break;

                default:
                    result = "Period is not selected";
            }

            resultArea.setText(result);
        }
        catch (IOException e)
        {
            resultArea.setText("Error: " + e.getMessage());
        }
    }

    private void updateChart(JSONArray rates, String currency)
    {
        rateChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Exchange rate " + currency + " to PLN");

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (int i = 0; i < rates.length(); i++)
        {
            JSONObject rateObj = rates.getJSONObject(i);
            String date = rateObj.getString("effectiveDate");
            double value = rateObj.getDouble("mid");

            series.getData().add(new XYChart.Data<>(date, value));

            if (value < min) min = value;
            if (value > max) max = value;
        }

        NumberAxis yAxis = (NumberAxis) rateChart.getYAxis();
        double padding = (max - min) * 0.1;
        if (padding == 0) padding = 0.01;

        yAxis.setAutoRanging(false);

        yAxis.setLowerBound(min - padding);
        yAxis.setUpperBound(max + padding);
        yAxis.setTickUnit((max - min + 2 * padding) / 5);

        CategoryAxis xAxis = (CategoryAxis) rateChart.getXAxis();
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);

        rateChart.getData().add(series);
    }

    private String getRatesForPeriod(String currency, String startDate, String endDate) throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currency.toLowerCase() + "/" + startDate + "/" + endDate + "/?format=json";
        JSONObject json = getJsonObject(url);
        JSONArray rates = json.getJSONArray("rates");

        updateChart(rates, currency);

        StringBuilder result = new StringBuilder("Exchange rate " + currency + " to PLN for period:\n");
        for (int i = 0; i < rates.length(); i++)
        {
            JSONObject rateObj = rates.getJSONObject(i);
            result.append(rateObj.getString("effectiveDate"))
                    .append(": ")
                    .append(rateObj.getDouble("mid"))
                    .append(" PLN\n");
        }
        return result.toString();
    }

    private String getRateForDate(String currency, String date) throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currency.toLowerCase() + "/" + date + "/?format=json";
        JSONObject json = getJsonObject(url);
        JSONObject rate = json.getJSONArray("rates").getJSONObject(0);
        return "Exchange rate " + currency + " to PLN on " + rate.getString("effectiveDate") + ": " + rate.getDouble("mid") + " PLN";
    }

    private String getCurrentRate(String currency) throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currency.toLowerCase() + "/today/?format=json";
        JSONObject json = getJsonObject(url);
        JSONObject rate = json.getJSONArray("rates").getJSONObject(0);
        return "Current rate " + currency + " to PLN: " + rate.getDouble("mid") + " PLN";
    }

    private String getTodayRate(String currency) throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currency.toLowerCase() + "/?format=json";
        JSONObject json = getJsonObject(url);
        JSONObject rate = json.getJSONArray("rates").getJSONObject(0);
        return "Exchange rate " + currency + " to PLN on today: " + rate.getDouble("mid") + " PLN";
    }

    private String getLastNDaysRates(String currency, int days) throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currency.toLowerCase() + "/last/" + days + "/?format=json";
        JSONObject json = getJsonObject(url);
        JSONArray rates = json.getJSONArray("rates");

        updateChart(rates, currency);

        StringBuilder result = new StringBuilder("Exchange rate " + currency + " to PLN last " + days + " days:\n");
        for (int i = 0; i < rates.length(); i++)
        {
            JSONObject rateObj = rates.getJSONObject(i);
            result.append(rateObj.getString("effectiveDate"))
                    .append(": ")
                    .append(rateObj.getDouble("mid"))
                    .append(" PLN\n");
        }
        return result.toString();
    }


    private JSONObject getJsonObject(String urlStr) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200)
        {
            throw new IOException("Message from API: " + responseCode + " (check day month and year, mb u r looking into the future)");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null)
        {
            response.append(line);
        }

        reader.close();
        return new JSONObject(response.toString());
    }
}