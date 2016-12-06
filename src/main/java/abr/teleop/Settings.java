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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class Settings extends Activity {
	EditText etxtStartX,etxtStartY, etxtEndX, etxtEndY, etxtFile,
			etxtHeadBearDiff, etxtFwdSpeed, etxtRoadFwdSpeed, etxtTurnSpeed, etxtRoadTurnSpeed,
			etxtMaxCounter, etxtCenterThresh,
			etxtSampleRate, etxtBlurSize, etxtAlpha, etxtBeta, etxtThresh, etxtDilateSize;
	Button buttonConnect;
	CheckBox redRobot, roadFollow, edgeDetect;
	Spinner currentDisplaySpinner;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.controller_connection);
		SharedPreferences settings = getSharedPreferences("Settings", 0);

		etxtStartX = (EditText)findViewById(R.id.etxtStartX);
		etxtStartX.setText(settings.getString("StartX", "5"));
		etxtStartY = (EditText)findViewById(R.id.etxtStartY);
		etxtStartY.setText(settings.getString("StartY", "2"));
		etxtEndX = (EditText)findViewById(R.id.etxtEndX);
		etxtEndX.setText(settings.getString("EndX", "15"));
		etxtEndY = (EditText)findViewById(R.id.etxtEndY);
		etxtEndY.setText(settings.getString("EndY", "16"));
		etxtFile = (EditText)findViewById(R.id.etxtFile);
		etxtFile.setText(settings.getString("File", "map_open_path.txt"));
		redRobot = (CheckBox)findViewById(R.id.redRobot);
		roadFollow = (CheckBox)findViewById(R.id.roadFollow);
		edgeDetect = (CheckBox)findViewById(R.id.edgeDetect);
		etxtHeadBearDiff = (EditText)findViewById(R.id.etxtHeadBearDiff);
		etxtHeadBearDiff.setText(settings.getString("HeadBearDiff", "60"));
		etxtFwdSpeed = (EditText)findViewById(R.id.etxtFwdSpeed);
		etxtFwdSpeed.setText(settings.getString("FwdSpeed", "220"));
		etxtRoadFwdSpeed = (EditText)findViewById(R.id.etxtRoadFwdSpeed);
		etxtRoadFwdSpeed.setText(settings.getString("RoadFwdSpeed", "180"));
		etxtTurnSpeed = (EditText)findViewById(R.id.etxtTurnSpeed);
		etxtTurnSpeed.setText(settings.getString("TurnSpeed", "50"));
		etxtRoadTurnSpeed = (EditText)findViewById(R.id.etxtRoadTurnSpeed);
		etxtRoadTurnSpeed.setText(settings.getString("RoadTurnSpeed", "120"));
		currentDisplaySpinner = (Spinner)findViewById(R.id.currentDisplaySpinner);
		etxtMaxCounter = (EditText)findViewById(R.id.etxtMaxCounter);
		etxtMaxCounter.setText(settings.getString("MaxCounter", "1"));
		etxtCenterThresh = (EditText)findViewById(R.id.etxtCenterThresh);
		etxtCenterThresh.setText(settings.getString("CenterThresh", "0.05"));
		etxtSampleRate = (EditText)findViewById(R.id.etxtSampleRate);
		etxtSampleRate.setText(settings.getString("SampleRate", "4"));
		etxtBlurSize = (EditText)findViewById(R.id.etxtBlurSize);
		etxtBlurSize.setText(settings.getString("BlurSize", "3.0"));
		etxtAlpha = (EditText)findViewById(R.id.etxtAlpha);
		etxtAlpha.setText(settings.getString("Alpha", "2.0"));
		etxtBeta = (EditText)findViewById(R.id.etxtBeta);
		etxtBeta.setText(settings.getString("Beta", "-2.0"));
		etxtThresh = (EditText)findViewById(R.id.etxtThresh);
		etxtThresh.setText(settings.getString("BinaryThresh", "135"));
		etxtDilateSize = (EditText)findViewById(R.id.etxtDilateSize);
		etxtDilateSize.setText(settings.getString("DilateSize", "15"));

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
				intent.putExtra("redRobot", redRobot.isChecked());
				intent.putExtra("roadFollow", roadFollow.isChecked());
				intent.putExtra("edgeDetect", edgeDetect.isChecked());
				intent.putExtra("HeadBearDiff", Float.parseFloat(etxtHeadBearDiff.getText().toString()));
				intent.putExtra("FwdSpeed", Integer.parseInt(etxtFwdSpeed.getText().toString()));
				intent.putExtra("RoadFwdSpeed", Integer.parseInt(etxtRoadFwdSpeed.getText().toString()));
				intent.putExtra("TurnSpeed", Integer.parseInt(etxtTurnSpeed.getText().toString()));
				intent.putExtra("RoadTurnSpeed", Integer.parseInt(etxtRoadTurnSpeed.getText().toString()));
				intent.putExtra("selectedCurrentDisplay", currentDisplaySpinner.getSelectedItem().toString());
				intent.putExtra("MaxCounter", Integer.parseInt(etxtMaxCounter.getText().toString()));
				intent.putExtra("CenterThresh", Double.parseDouble(etxtCenterThresh.getText().toString()));
				intent.putExtra("SampleRate", Integer.parseInt(etxtSampleRate.getText().toString()));
				intent.putExtra("BlurSize", Double.parseDouble(etxtBlurSize.getText().toString()));
				intent.putExtra("Alpha", Double.parseDouble(etxtAlpha.getText().toString()));
				intent.putExtra("Beta", Double.parseDouble(etxtBeta.getText().toString()));
				intent.putExtra("BinaryThresh", Double.parseDouble(etxtThresh.getText().toString()));
				intent.putExtra("DilateSize", Integer.parseInt(etxtDilateSize.getText().toString()));
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
		editor.putString("HeadBearDiff", etxtHeadBearDiff.getText().toString());
		editor.putString("FwdSpeed", etxtFwdSpeed.getText().toString());
		editor.putString("RoadFwdSpeed", etxtRoadFwdSpeed.getText().toString());
		editor.putString("TurnSpeed", etxtTurnSpeed.getText().toString());
		editor.putString("RoadTurnSpeed", etxtRoadTurnSpeed.getText().toString());
		editor.putString("MaxCounter", etxtMaxCounter.getText().toString());
		editor.putString("CenterThresh", etxtCenterThresh.getText().toString());
		editor.putString("SampleRate", etxtSampleRate.getText().toString());
		editor.putString("BlurSize", etxtBlurSize.getText().toString());
		editor.putString("Alpha", etxtAlpha.getText().toString());
		editor.putString("Beta", etxtBeta.getText().toString());
		editor.putString("BinaryThresh", etxtThresh.getText().toString());
		editor.putString("DilateSize", etxtDilateSize.getText().toString());
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
