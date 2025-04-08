package com.example.kurs;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class showKurs
{
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() throws IOException
    {
        welcomeText.setText(printLast10DaysRates());
    }

    private static String printCurrentRate() throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/eur/?format=json";
        JSONObject json = getJsonObject(url);
        double rate = json.getJSONArray("rates").getJSONObject(0).getDouble("mid");
        String date = json.getJSONArray("rates").getJSONObject(0).getString("effectiveDate");

        return "Курс euro к злотому на " + date + ": " + rate + " PLN";
    }

    private static String printLast10DaysRates() throws IOException
    {
        String url = "https://api.nbp.pl/api/exchangerates/rates/a/eur/last/10/?format=json";
        JSONObject json = getJsonObject(url);
        JSONArray rates = json.getJSONArray("rates");

        StringBuilder result = new StringBuilder("\nКурс евро к злотому за последние 10 дней:");
        for (int i = 0; i < rates.length(); i++)
        {
            JSONObject rateObj = rates.getJSONObject(i);
            String date = rateObj.getString("effectiveDate");
            double rate = rateObj.getDouble("mid");
            result.append(date).append(": ").append(rate).append(" PLN\n");
        }
        return result.toString();
    }

    private static JSONObject getJsonObject(String url) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
        );

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            response.append(line);
        }
        reader.close();

        JSONObject json = new JSONObject(response.toString());
        return json;
    }
}