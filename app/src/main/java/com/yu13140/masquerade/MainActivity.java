package com.yu13140.masquerade;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText editTextPackageName = findViewById(R.id.editTextPackageName);
        EditText editTextPropName = findViewById(R.id.editTextPropName);
        EditText editTextFakeValue = findViewById(R.id.editTextFakeValue);
        Button saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(v -> {
            String targetApp = editTextPackageName.getText().toString().trim();
            String propName = editTextPropName.getText().toString().trim();
            String fakeValue = editTextFakeValue.getText().toString().trim();

            if (!targetApp.isEmpty() && !propName.isEmpty() && !fakeValue.isEmpty()) {                
                SharedPreferences prefs = getSharedPreferences("xposed_config", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("targetApp", targetApp);
                editor.putString("systemProperty", propName);
                editor.putString("fakeValue", fakeValue);
                editor.apply();

                Toast.makeText(this, "配置已保存", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_LONG).show();
            }
        });
    }
}