package com.yu13140.masquerade;

import androidx.appcompat.app.AppCompatActivity;
import java.util.List;          
import android.content.pm.ApplicationInfo;
import com.yu13140.masquerade.XposedHook;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.content.pm.ApplicationInfo;

public class MainActivity extends AppCompatActivity {
	private SharedPreferences prefs;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		prefs = getSharedPreferences("config", MODE_PRIVATE);
		setupAppSpinner();
		setupSaveButton();
	}

	private void setupAppSpinner() {
		List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);

		for (ApplicationInfo app : apps) {
			adapter.add(app.packageName);
		}

		Spinner spinner = findViewById(R.id.app_spinner);
		spinner.setAdapter(adapter);
	}

	private void setupSaveButton() {
		findViewById(R.id.save_btn).setOnClickListener(v -> {
			String packageName = ((Spinner) findViewById(R.id.app_spinner)).getSelectedItem().toString();
			String propName = ((EditText) findViewById(R.id.property_name)).getText().toString();
			String propValue = ((EditText) findViewById(R.id.property_value)).getText().toString();

			prefs.edit().putString("target_pkg", packageName).putString("prop_name", propName)
					.putString("prop_value", propValue).apply();

			Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
		});
	}
}