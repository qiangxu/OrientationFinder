package com.example.orientaion_finder;

import com.example.orientaion_finder.R;
import com.example.orientaion_finder.MainActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.view.View;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import com.examle.orientation_finder.representation.Matrixf4x4;
import com.examle.orientation_finder.representation.Quaternion;
public class Rotationvectora extends Activity implements SensorEventListener {
	 
	
	private SensorManager mSensorManager;
	private Sensor rotation; 
	
    protected final Matrixf4x4 currentOrientationRotationMatrix= new Matrixf4x4();
    
    private String location = "0";
    private static final String SAMPLES_DIR = Environment.getExternalStorageDirectory() + File.separator + "orientation_samples";	
	private static final String TAG = "Recording Orientation Samples";	 

	private FileWriter mRotationLogFileWriter; 
	private boolean mIsSampling = false;
	
	private long timestamp = 0;
    private long old_time_stamp = 0;
   
    protected final Quaternion currentOrientationQuaternion =  new Quaternion();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_rotationvectora);
			
			Intent intent = getIntent();
			
			location = (String) intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			
			
			if (null == (rotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)))
				finish();
		
			old_time_stamp = System.currentTimeMillis();   
			this.startSampling();
		}
		
			// Register listener
	@Override
	protected void onResume() {
				super.onResume();
				mSensorManager.registerListener(this, rotation,SensorManager.SENSOR_DELAY_NORMAL);
		}
			
			// Unregister listener
	@Override
	protected void onPause() {
				super.onPause();
				finishRecordSamples(); 	
		}        
			
	protected void onStop()
		{       super.onStop(); 
				finishRecordSamples(); 
		}
			
	protected void onDestroy()
		{   	super.onDestroy();
				finishRecordSamples();			   
		}
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	        // Not doing anything
	    }
	
	public void onSensorChanged(SensorEvent event) {
		synchronized (mRotationLogFileWriter)
		{   long deltaT = event.timestamp;  
		    if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
			{
			  timestamp = old_time_stamp;
			  deltaT -= timestamp;
			  old_time_stamp = event.timestamp;	  
		  	
	          SensorManager.getRotationMatrixFromVector(currentOrientationRotationMatrix.matrix, event.values);
	          float[] orient = new float[3];
	          
	          if(this.isSampling()) {
	        	  SensorManager.getOrientation(currentOrientationRotationMatrix.matrix, orient);
	        	 
					try {		
						mRotationLogFileWriter.write("" + timestamp + "," + deltaT + "," + orient[0] +  ","  + orient[1] + ","  + orient[2]);
						for(int i = 0 ; i < currentOrientationRotationMatrix.matrix.length ; i++)
	        	        {  mRotationLogFileWriter.write ("," + currentOrientationRotationMatrix.matrix[i]);
	        	        }
						  mRotationLogFileWriter.write("\n");
						} 
					catch (IOException e) {
						Log.e(TAG, "Log file write for Rotation Vector failed!!!\n", e);
						e.printStackTrace();
						throw new RuntimeException(e);
						}
	             }    
		    }
	    }
	}
	public void startSampling()
    {	try {
			String r = (String) (DateFormat.format("yyyy-MM-dd-hh-mm-ss", new java.util.Date()) );
			String logFileBaseName = "Loc_" + location + "_" + r;
			final File parent = new File(SAMPLES_DIR);
			final File app = new File(SAMPLES_DIR, logFileBaseName + ".magnet.csv");
			if(!parent.mkdirs())
			 {System.err.println("could not create directories");}
			mRotationLogFileWriter = new FileWriter(app);
		  } catch (IOException e) {
			Log.e(TAG, "Creating and opening log files failed!", e);
			e.printStackTrace();
			throw new RuntimeException(e);
		}           
		                                                                                                                  
	    mIsSampling = true;
    }    

	public void stopSampling(View view)
	{   mIsSampling = false;
	   try {
			mRotationLogFileWriter.flush();
			mRotationLogFileWriter.close();
		   } catch (IOException e) {
			Log.e(TAG, "Flushing and closing log files failed!" , e);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	    finishRecordSamples();
	    finish();
	}

	public boolean isSampling()
	{  	return mIsSampling; 
	}
	
    public void finishRecordSamples() {
		   mSensorManager.unregisterListener(this);
	}
}


