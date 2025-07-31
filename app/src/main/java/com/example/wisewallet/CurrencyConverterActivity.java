package com.example.wisewallet;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wallet_wise.R;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class CurrencyConverterActivity extends AppCompatActivity {

    EditText editAmount;
    Spinner spinnerFrom, spinnerTo;
    Button convertButton;
    TextView resultText;
    private EditText calculatorInput;
    private StringBuilder currentExpression = new StringBuilder();

    String[] currencies = {"USD", "KES", "EUR", "GBP", "NGN", "TZS", "UGX"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency_converter);

        editAmount = findViewById(R.id.editAmount);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        convertButton = findViewById(R.id.convertButton);
        resultText = findViewById(R.id.resultText);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        // Inside onCreate()
        calculatorInput = findViewById(R.id.calculatorInput);
        // Digits
        setCalculatorButton(R.id.btn0, "0");
        setCalculatorButton(R.id.btn1, "1");
        setCalculatorButton(R.id.btn2, "2");
        setCalculatorButton(R.id.btn3, "3");
        setCalculatorButton(R.id.btn4, "4");
        setCalculatorButton(R.id.btn5, "5");
        setCalculatorButton(R.id.btn6, "6");
        setCalculatorButton(R.id.btn7, "7");
        setCalculatorButton(R.id.btn8, "8");
        setCalculatorButton(R.id.btn9, "9");
        setCalculatorButton(R.id.btnDot, ".");

        // Operators
        setCalculatorButton(R.id.btnPlus, "+");
        setCalculatorButton(R.id.btnMinus, "-");
        setCalculatorButton(R.id.btnMultiply, "*"); // × mapped to *
        setCalculatorButton(R.id.btnDivide, "/"); // ÷ mapped to /

        // Clear button
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            currentExpression.setLength(0);
            calculatorInput.setText("");
        });

        // Equals button
        findViewById(R.id.btnEqual).setOnClickListener(v -> {
            try {
                double result = evaluateExpression(currentExpression.toString());
                calculatorInput.setText(String.valueOf(result));
                currentExpression.setLength(0); // Reset expression
                currentExpression.append(result); // Allow chaining
            } catch (Exception e) {
                calculatorInput.setText("Error");
                currentExpression.setLength(0);
            }
        });

        convertButton.setOnClickListener(v -> convertCurrency());
    }

    private double evaluateExpression(String expression) throws Exception {
        Expression exp = new ExpressionBuilder(expression).build();
        return exp.evaluate();
    }

    private void setCalculatorButton(int buttonId, String value) {
        findViewById(buttonId).setOnClickListener(v -> {
            currentExpression.append(value);
            calculatorInput.setText(currentExpression.toString());
        });
    }

    private void convertCurrency() {
        String amountStr = editAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        String from = spinnerFrom.getSelectedItem().toString();
        String to = spinnerTo.getSelectedItem().toString();

        new Thread(() -> {
            try {
                String apiKey = "816879d865c66c7e39b4a307"; // Replace with your key
                URL url = new URL("https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + from);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(json.toString());
                double rate = jsonObject.getJSONObject("conversion_rates").getDouble(to);
                double amount = Double.parseDouble(amountStr);
                double converted = amount * rate;

                runOnUiThread(() ->
                        resultText.setText("Converted Amount: " + String.format("%.2f", converted) + " " + to)
                );

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Conversion failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
