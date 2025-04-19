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

Initially, this is a function that only fills the chart with the rate to date, but in order to show changes in the chart in more detail - I decided to slightly change the division price of the rate axis. Now we are looking for the minimum and maximum value of the exchange rate and the beginning of the exchange rate axis is now slightly less than its minimum value for this period, and thanks to this approach we can always clearly see the drops and rises of the exchange rate, even the minimum ones!

Next we have the main logical functions - which just send a request to our server with currency history. They all look almost the same, only the requests and the type of filling in the response differ, because sometimes we get one date and rate value, and sometimes a whole array! Also we have processed the possibility of viewing the rate for Saturday and Sunday, despite the fact that on those days the rate is not updated.

Now comes the main function - the function that returns the response from the server:
```
private JSONObject get_data_from_url(String urlStr) throws IOException
    {
        LocalDate currentDate = LocalDate.now();
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
        {
            LocalDate lastFriday = currentDate.with(DayOfWeek.FRIDAY);

            urlStr = urlStr.replace("today", lastFriday.toString());
        }

        HttpURLConnection connect_to_url = (HttpURLConnection) new URL(urlStr).openConnection();
        connect_to_url.setRequestMethod("GET");

        int result_code = connect_to_url.getResponseCode();
        if (result_code != 200)
        {
            throw new IOException("Message from API: " + result_code + " (check day month and year, mb u r looking into the future)");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connect_to_url.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null)
        {
            response.append(line);
        }

        reader.close();

        return new JSONObject(response.toString());
    }
```
First we throw our request to the server, the server checks the correctness of our request and if it is correct, it returns an error, which we process. If all is well, we write the response into a buffer, from which we write it line by line into our string. After that we close the buffer and return the data to the user in JSON format.

In the Kurs file, we just put it all together, namely logic, markup, and styles. We also run everything through this file.
```
public class Kurs extends Application
{
    @Override
    public void start(Stage stage) throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(Kurs.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(
                "/com/example/kurs/my_style.css")).toExternalForm());
        stage.setTitle("Exchange Rate");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args)
    {
        launch();
    }
}
```







