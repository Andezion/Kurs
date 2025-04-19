# Kurs
Currency exchange rate tracker

## Content
* [General info](#general-info)
* [Demonstration](#demonstration)
* [Technologies](#technologies)
* [Features](#features)
* [Inspiration](#inspiration)
* [Setup](#setup)
---

## General info
I decided to implement a small desktop application to visualise the exchange rate changes of different currencies against the Polish zloty

---
## Demonstration
Скоро будет!

## Technologies
Made a project using:
* JavaFX
* Rest API
* Apache Maven

---
## Features

I'll start with the file that contains all the logic of my application - ShowKurs:
After declaring all the markup we start to initialise it in the function 
```
public void initialize()
```
That is, we fill in the text, declare the borders and other visual events

Then we have the function (updateResult()) which outputs the information we need depending on what the user has entered, there we have the error service.

If the user has chosen to show the rate for one day - then we just output the result in the text field, if we have some period - then we use the graph function - show_cool()
```
private void show_cool(JSONArray rates, String currency)
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
```

Initially, this is a function that only fills the chart with the rate to date, but in order to show changes in the chart in more detail - I decided 
to slightly change the division price of the rate axis. Now we are looking for the minimum and maximum value of the exchange rate and the beginning of 
the exchange rate axis is now slightly less than its minimum value for this period, and thanks to this approach we can always clearly see the drops and rises of the exchange rate, even the minimum ones





