package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {
    private Retrofit retrofit;
    private ApiService apiService;
    private EditText fullName, emailAddress, mobileNumber;
    private DatePicker datePicker;
    private TextView ageLabel;
    private TextView errorPrompt;
    private Spinner genderSpinner;
    private Button submitButton;
    private Button showDatePickerButton;
    private Calendar calendar;
    private int currentYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        fullName = findViewById(R.id.fullName);
        emailAddress = findViewById(R.id.emailAddress);
        mobileNumber = findViewById(R.id.mobileNumber);
        datePicker = findViewById(R.id.datePicker);
        ageLabel = findViewById(R.id.ageLabel);
        errorPrompt = findViewById(R.id.errorPrompt);
        genderSpinner = findViewById(R.id.genderSpinner);
        submitButton = findViewById(R.id.submitButton);
        showDatePickerButton = findViewById(R.id.showDatePickerButton);

        calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);

        // Set up gender spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        initRetrofit();

        // Show DatePicker when button is clicked
        showDatePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (datePicker.getVisibility() == View.GONE) {
                    datePicker.setVisibility(View.VISIBLE);
                } else {
                    datePicker.setVisibility(View.GONE);
                }
            }
        });

        // Calculate and display age when the date is changed
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                int age = calculateAge(year, monthOfYear, dayOfMonth);
                ageLabel.setText("Age: " + age);
            }
        });

        // Submit button click listener
        submitButton.setOnClickListener(v -> {
            if (validateInputs()) {
                FormData formData = new FormData(
                        fullName.getText().toString(),
                        emailAddress.getText().toString(),
                        mobileNumber.getText().toString(),
                        Integer.parseInt(ageLabel.getText().toString().replace("Age: ", "").trim()),
                        genderSpinner.getSelectedItem().toString()
                );

                Call<ResponseBody> call = apiService.submitForm(formData);
                call.enqueue(new retrofit2.Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            String responseBody = "";
                            try {
                                responseBody = response.body() != null ? response.body().string() : "No response body";
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(MainActivity.this, "Form submitted successfully! Response: " + responseBody, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Submission failed: " + response.message(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Submission error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "Please fill out the forms!", Toast.LENGTH_SHORT).show();
            }
        });



    }

    private boolean validateInputs() {
        // Validate Full Name
        if (!validateName(fullName.getText().toString())) {
            fullName.setError("Enter a valid name (only letters, commas, and periods allowed)");
            return false;
        }

        // Validate Email Address
        if (!validateEmail(emailAddress.getText().toString())) {
            emailAddress.setError("Enter a valid email address");
            return false;
        }

        // Validate Mobile Number
        if (!validateMobileNumber(mobileNumber.getText().toString())) {
            mobileNumber.setError("Enter a valid mobile number (e.g., 09171234567)");
            return false;
        }

        // Validate Age
        String ageText = ageLabel.getText().toString();
        int age = 0;
        if (ageText.startsWith("Age: ")) {
            try {
                age = Integer.parseInt(ageText.substring(5).trim());
            } catch (NumberFormatException e) {
                // Handle parsing error
            }
        }

        if (age < 18) {
            errorPrompt.setVisibility(View.VISIBLE); // Show error prompt
            return false;
        } else {
            errorPrompt.setVisibility(View.GONE); // Hide error prompt
        }

        return true;
    }

    private int calculateAge(int year, int month, int day) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - year;

        if (today.get(Calendar.MONTH) < month || (today.get(Calendar.MONTH) == month && today.get(Calendar.DAY_OF_MONTH) < day)) {
            age--;
        }
        return age;
    }

    private boolean validateName(String name) {
        return name.matches("^[a-zA-Z\\s,\\.]+$");
    }

    private boolean validateEmail(String email) {
        Pattern pattern = Pattern.compile("^[\\w-\\.]+@[\\w-]+\\.[a-z]{2,4}$");
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private boolean validateMobileNumber(String mobile) {
        return mobile.matches("^09\\d{9}$");
    }

    private void initRetrofit() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://run.mocky.io/v3/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // Log or check if retrofit and apiService are not null
        if (retrofit == null || apiService == null) {
            Log.e("RetrofitError", "Retrofit or ApiService is null");
        }
    }

}
