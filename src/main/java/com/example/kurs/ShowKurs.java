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
        periodBox.getItems().addAll("Сейчас", "Сегодня", "Последние N дней", "За дату", "Произвольный период");

        fromCurrencyBox.setValue("EUR");
        toCurrencyBox.setValue("PLN");

        periodBox.setValue("Pick");

        periodBox.setOnAction(e -> {
            String selected = periodBox.getValue();
            boolean isDate = selected.equals("За дату");
            boolean isRange = selected.equals("Произвольный период");
            boolean isNDays = selected.equals("Последние N дней");

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
                case "Сейчас":
                    result = getCurrentRate(from);
                    break;
                case "Сегодня":
                    result = getTodayRate(from);
                    break;
                case "Последние N дней":
                    String text = daysField.getText();
                    if (text != null && !text.isEmpty())
                    {
                        try
                        {
                            int days = Integer.parseInt(text);

                            if(days <= 0)
                            {
                                result = "u dumb fuck";
                            }
                            else
                            {
                                result = getLastNDaysRates(from, days);
                            }
                        }
                        catch (NumberFormatException e)
                        {
                            result = "Введите корректное число дней.";
                        }
                    }
                    else
                    {
                        result = "Поле для дней пустое.";
                    }
                    break;
                case "За дату":
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
                case "Произвольный период":
                    if (startDatePicker.getValue() != null && endDatePicker.getValue() != null)
                    {
                        String start = startDatePicker.getValue().toString();
                        String end = endDatePicker.getValue().toString();
                        result = getRatesForPeriod(from, start, end);
                    }
                    else
                    {
                        result = "Выберите обе даты периода.";
                    }
                    break;

                default:
                    result = "Период не выбран.";
            }

            resultArea.setText(result);
        }
        catch (IOException e)
        {
            resultArea.setText("Ошибка при получении данных: " + e.getMessage());
        }
    }

    private void updateChart(JSONArray rates, String currency)
    {
        rateChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Курс " + currency + " к PLN");

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

        // Настройка Y оси
        NumberAxis yAxis = (NumberAxis) rateChart.getYAxis();
        double padding = (max - min) * 0.1; // 10% от разницы
        if (padding == 0) padding = 0.01;   // Минимальный паддинг, если курс не менялся вообще
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(min - padding);
        yAxis.setUpperBound(max + padding);
        yAxis.setTickUnit((max - min + 2 * padding) / 5); // 5 делений, можно настроить

        // X ось с поворотом дат
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

        StringBuilder result = new StringBuilder("Курс " + currency + " к PLN за период:\n");
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
        return "Курс " + currency + " к PLN на " + rate.getString("effectiveDate") + ": " + rate.getDouble("mid") + " PLN";
    }

    private String getCurrentRate(String currency) throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currency.toLowerCase() + "/today/?format=json";
        JSONObject json = getJsonObject(url);
        JSONObject rate = json.getJSONArray("rates").getJSONObject(0);
        return "Курс " + currency + " к PLN на сейчас: " + rate.getDouble("mid") + " PLN";
    }

    private String getTodayRate(String currency) throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currency.toLowerCase() + "/?format=json";
        JSONObject json = getJsonObject(url);
        JSONObject rate = json.getJSONArray("rates").getJSONObject(0);
        return "Курс " + currency + " к PLN на " + rate.getString("effectiveDate") + ": " + rate.getDouble("mid") + " PLN";
    }

    private String getLastNDaysRates(String currency, int days) throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currency.toLowerCase() + "/last/" + days + "/?format=json";
        JSONObject json = getJsonObject(url);
        JSONArray rates = json.getJSONArray("rates");

        updateChart(rates, currency);

        StringBuilder result = new StringBuilder("Курс " + currency + " к PLN за последние " + days + " дней:\n");
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
            throw new IOException("Ответ от API: " + responseCode + " (возможно, на выбранную дату нет данных)");
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