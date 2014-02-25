package com.example.orientaion_finder;

import com.example.orientaion_finder.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {

	public final static String EXTRA_MESSAGE = "com.example.orientation_finder.MESSAGE";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void RotationSamplesMethod(View view){
		Intent intent = new Intent(this, Rotationvectora.class);
		EditText editText1 = (EditText) findViewById(R.id.edit_path);
		String mes = editText1.getText().toString();
		intent.putExtra(EXTRA_MESSAGE, mes);
		startActivity(intent);
	}

}
