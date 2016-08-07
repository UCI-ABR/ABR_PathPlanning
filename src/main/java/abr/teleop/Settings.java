package abr.teleop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class Settings extends Activity {
	EditText etxtStartX,etxtStartY, etxtEndX, etxtEndY, etxtFile;
	Button buttonConnect;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); 
		setContentView(R.layout.controller_connection);
		SharedPreferences settings = getSharedPreferences("Settings", 0);
	       
		etxtStartX = (EditText)findViewById(R.id.etxtStartX);
		etxtStartX.setText(settings.getString("StartX", "1"));
		etxtStartY = (EditText)findViewById(R.id.etxtStartY);
		etxtStartY.setText(settings.getString("StartY", "9"));
		etxtEndX = (EditText)findViewById(R.id.etxtEndX);
		etxtEndX.setText(settings.getString("EndX", "18"));
		etxtEndY = (EditText)findViewById(R.id.etxtEndY);
		etxtEndY.setText(settings.getString("EndY", "9"));
		etxtFile = (EditText)findViewById(R.id.etxtFile);
		etxtFile.setText(settings.getString("File", "map_open_nopath.txt"));

		buttonConnect = (Button)findViewById(R.id.buttonConnect);
		buttonConnect.setEnabled(false);
		buttonConnect.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Settings.this, IOIO.class);
				intent.putExtra("StartX", Integer.parseInt(etxtStartX.getText().toString()));
				intent.putExtra("StartY", Integer.parseInt(etxtStartY.getText().toString()));
				intent.putExtra("EndX", Integer.parseInt(etxtEndX.getText().toString()));
				intent.putExtra("EndY", Integer.parseInt(etxtEndY.getText().toString()));
				intent.putExtra("File", etxtFile.getText().toString());
				startActivity(intent);
			}
		});
	}
	
	public void onPause() {
		super.onPause();
		SharedPreferences settings = getSharedPreferences("Settings", 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString("StartX", etxtStartX.getText().toString());
		editor.putString("StartY", etxtStartY.getText().toString());
		editor.putString("EndX", etxtEndX.getText().toString());
		editor.putString("EndY", etxtEndY.getText().toString());
		editor.putString("File", etxtFile.getText().toString());
	    editor.commit();
	}
	
	public void onResume() {
		super.onResume();
		buttonConnect.setEnabled(false);
		
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(1000);
					runOnUiThread(new Runnable() {
						public void run() {
							buttonConnect.setEnabled(true);
						}
					});
				} catch (InterruptedException e) { }
			}
		}).start();
	}
}
