package com.yu13140.masquerade;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.List;
import java.util.Arrays;
import android.content.pm.ApplicationInfo;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private static final char[] ENCRYPTION_KEY = {'X','P','O','S','E','D'};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       
        prefs = getSharedPreferences("sys_conf", Context.MODE_PRIVATE);
        
        setupAppSpinner();
        setupSaveButton();
        loadExistingConfig();
    }

    private void setupAppSpinner() {
        List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);

        for (ApplicationInfo app : apps) {
            adapter.add(app.packageName);
        }

        Spinner spinner = findViewById(R.id.app_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupSaveButton() {
        Button saveButton = findViewById(R.id.save_btn);
        saveButton.setOnClickListener(v -> {
            String packageName = ((Spinner) findViewById(R.id.app_spinner)).getSelectedItem().toString();
            String propName = ((EditText) findViewById(R.id.property_name)).getText().toString();
            String propValue = ((EditText) findViewById(R.id.property_value)).getText().toString();
           
            if (propName.isEmpty() || propValue.isEmpty()) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }
            
            prefs.edit()
                    .putString("a", encrypt(packageName))
                    .putString("b", encrypt(propName))
                    .putString("c", encrypt(propValue))
                    .apply();
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> 
                Toast.makeText(this, "配置将在3秒后生效", Toast.LENGTH_LONG).show(), 100);
        });
    }

    private void loadExistingConfig() {
        new Handler().postDelayed(() -> {
            String decryptedPackage = decrypt(prefs.getString("a", ""));
            String decryptedPropName = decrypt(prefs.getString("b", ""));
            String decryptedPropValue = decrypt(prefs.getString("c", ""));
           
            if (!decryptedPackage.isEmpty()) {
                Spinner spinner = findViewById(R.id.app_spinner);
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
                int position = adapter.getPosition(decryptedPackage);
                if (position >= 0) spinner.setSelection(position);
            }
            ((EditText) findViewById(R.id.property_name)).setText(decryptedPropName);
            ((EditText) findViewById(R.id.property_value)).setText(decryptedPropValue);
        }, 300);
    }

  
    private String encrypt(String input) {
        if (input == null) return "";
        char[] output = new char[input.length()];
        for (int i = 0; i < input.length(); i++) {
            output[i] = (char) (input.charAt(i) ^ ENCRYPTION_KEY[i % ENCRYPTION_KEY.length]);
        }
        return new String(output);
    }

    private String decrypt(String input) {
        return encrypt(input);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Handler().postDelayed(() -> {
            Arrays.fill(ENCRYPTION_KEY, '\0');
            System.gc();
        }, 1000);
    }
}