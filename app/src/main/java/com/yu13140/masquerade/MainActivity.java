package com.yu13140.masquerade;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;

public class MainActivity extends Activity {
    private static final String CONFIG_PATH = "/data/data/com.yu13140.masquerade/xposed_config.json";

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
                saveConfig(targetApp, propName, fakeValue);
            } else {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveConfig(String packageName, String propName, String fakeValue) {
        try {
            JSONObject config = new JSONObject();
            config.put("targetApp", packageName);
            config.put("systemProperty", propName);
            config.put("fakeValue", fakeValue);

            File file = new File(CONFIG_PATH);
            FileWriter writer = new FileWriter(file);
            writer.write(config.toString(4));
            writer.close();

            Toast.makeText(this, "配置已保存：" + CONFIG_PATH, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存配置失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}