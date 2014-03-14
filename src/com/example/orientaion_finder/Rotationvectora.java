package com.example.orientaion_finder;

import com.example.orientaion_finder.R;
import com.example.orientaion_finder.MainActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TimerTask;

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
	private Sensor acceleration;
	private Sensor magneticfield;
	private Sensor gyroscope;
	
	  // angular speeds from gyro
    private float[] gyro = new float[3];
 
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
 
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
    private float[] realgyroOrientation = {0.f,0.f,0.f};
    // magnetic field vector
    private float[] magnet = new float[3];
 
    // accelerometer vector
    private float[] accel = new float[3];
 
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
    private float[] realaccMagOrientation = {0.f,0.f,0.f};
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];
 
    private float[] prevgyroOrientation = new float[3];
    
    // orientation angles from accel and magnet
    private float[] prevaccMagOrientation = new float[3];
 
    // final orientation angles from sensor fusion
    private float[] prevfusedOrientation = new float[3];
    
    private float correlation = 0.f;
    private float magnetic_angle_change = 0.f;
 
    private float alpha = 0.49f;
    private float beta = 0.49f;
    private float gamma = 0.02f;
   
    private static final float CORRELATION_THRESHOLD = 0.16f;
    private static final float MAGNETIC_ANGLE_CHANGE_THRESHOLD = 0.1f;
    private boolean magnetic_data = false;
    private boolean accMagOri_data = false;
     
    // accelerometer
    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
	private float timestamp;
	private boolean initState = true;
    
	public static final int TIME_CONSTANT = 30;
	public static final float FILTER_COEFFICIENT = 0.98f;
	
	private long mLastGyroTimestamp= 0;
	
    private String location = "0";
    private static final String SAMPLES_DIR = Environment.getExternalStorageDirectory() + File.separator + "orientation_samples";	
	private static final String TAG = "Recording Orientation Samples";	 

	private FileWriter mRotationLogFileWriter;
	private FileWriter mOrientationLogFileWriter;
	private boolean mIsSampling = false;
	
    private long old_time_stamp = 0;
  	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_rotationvectora);
			Intent intent = getIntent();
			location = (String) intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			acceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			magneticfield = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			old_time_stamp = System.currentTimeMillis();   
			
			    gyroOrientation[0] = 0.0f;
		        gyroOrientation[1] = 0.0f;
		        gyroOrientation[2] = 0.0f;
		 
		        // initialise gyroMatrix with identity matrix
		        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
		        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
		        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
		 
		    this.startSampling();
		}
		
			// Register listener
	@Override
	protected void onResume() {
				super.onResume();
				mSensorManager.registerListener(this, acceleration,SensorManager.SENSOR_DELAY_NORMAL);
				mSensorManager.registerListener(this, magneticfield,SensorManager.SENSOR_DELAY_NORMAL);
				mSensorManager.registerListener(this, gyroscope ,SensorManager.SENSOR_DELAY_NORMAL);
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
		synchronized (this)
		{   long deltaT = event.timestamp;  
		   switch(event.sensor.getType()){ 
		   case Sensor.TYPE_ACCELEROMETER:
			 { System.arraycopy(event.values,0,accel,0,3);
			   System.out.println("Acc");
			   if(magnetic_data){
			   calculateAccMagOrientation();}	
			   break;
		   }
		   case Sensor.TYPE_MAGNETIC_FIELD:
			 { System.out.println("Mag");
			   if(!magnetic_data)
			      { magnetic_data = true;
			      }
			   System.arraycopy(event.values,0,magnet,0,3);
			   break;
		   }
		   case Sensor.TYPE_GYROSCOPE:
			 { System.arraycopy(event.values,0,gyro,0,3);
			   System.out.println("Gyr");
			   if (mLastGyroTimestamp == 0) {
					mLastGyroTimestamp = old_time_stamp;
				}
			   deltaT -= mLastGyroTimestamp;
			   mLastGyroTimestamp = event.timestamp;
			   gyroFunction(deltaT,gyro);
			   break;
		   }
		} 
		if(this.isSampling()) {
    		try {		
				mRotationLogFileWriter.write(""+ deltaT + "," + accel[0] +  ","  + accel[1] + ","  + accel[2]+ "," + magnet[0] +  ","  + magnet[1] + ","  + magnet[2]+ "," + gyro[0] +  "," + gyro[1] + "," + gyro[2]+"\n");	
    		} 
			catch (IOException e) {
				Log.e(TAG, "Log file write for Rotation Vector failed!!!\n", e);
				e.printStackTrace();
				throw new RuntimeException(e);
	    	}
		 }
	  }
	}
	
	// calculates orientation angles from accelerometer and magnetometer output
	public void calculateAccMagOrientation() {
	    if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
	        SensorManager.getOrientation(rotationMatrix, accMagOrientation);
	    }
	    if(accMagOri_data == false)
	    {accMagOri_data = true;}
	    System.out.println(accMagOrientation[0]);
	}
	// This function is borrowed from the Android reference
		// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
		// It calculates a rotation vector from the gyroscope angular speed values.
	private void getRotationVectorFromGyro(float[] gyroValues,
	            float[] deltaRotationVector,
	            float timeFactor)
		{
			float[] normValues = new float[3];
			
			// Calculate the angular speed of the sample
			float omegaMagnitude =
			(float)Math.sqrt(gyroValues[0] * gyroValues[0] +
			gyroValues[1] * gyroValues[1] +
			gyroValues[2] * gyroValues[2]);
			
			// Normalize the rotation vector if it's big enough to get the axis
			if(omegaMagnitude > EPSILON) {
			normValues[0] = gyroValues[0] / omegaMagnitude;
			normValues[1] = gyroValues[1] / omegaMagnitude;
			normValues[2] = gyroValues[2] / omegaMagnitude;
			}
			
			// Integrate around this axis with the angular speed by the timestep
			// in order to get a delta rotation from this sample over the timestep
			// We will convert this axis-angle representation of the delta rotation
			// into a quaternion before turning it into the rotation matrix.
			float thetaOverTwo = omegaMagnitude * timeFactor;
			float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
			deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
			deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
			deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
			deltaRotationVector[3] = cosThetaOverTwo;
		}

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(long deltaT, float[] gyro) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (!accMagOri_data)
        {   System.out.println("Return");
            System.out.println(accMagOrientation[0]);
            return;
        }
        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }
     
        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];   
        final float dT = (deltaT) * NS2S;
        getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        // measurement done, save current time for next interval        
        // convert rotation vector into rotation matrix
        
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
     
        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);
     
        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
        System.out.println(gyroOrientation[0]);
        calculateFusedOrientation();                
    }
    
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];
     
        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);
     
        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
     
        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
     
        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
     
        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
  
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
     
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
     
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
     
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return result;
    }
    public void calculateFusedOrientation() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;            
            /*
             * Fix for 179° <--> -179° transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
             * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
             */
            // azimuth
            realgyroOrientation[0] =  gyroOrientation[0] ;
            realgyroOrientation[1] =  gyroOrientation[1] ;
            realgyroOrientation[2] =  gyroOrientation[2] ;
            realaccMagOrientation[0] = accMagOrientation[0] ;
            realaccMagOrientation[1] = accMagOrientation[1] ;
            realaccMagOrientation[2] = accMagOrientation[2] ;
            							            				
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
            	gyroOrientation[0] += 2.0 * Math.PI;
            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            	accMagOrientation[0] += 2.0 * Math.PI;
            }
            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
            	gyroOrientation[0] += 2.0 * Math.PI;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            	accMagOrientation[0] += 2.0 * Math.PI;
            }
            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
            	gyroOrientation[0] += 2.0 * Math.PI;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            	accMagOrientation[0] += 2.0 * Math.PI;
            }
            
            if(prevgyroOrientation == null)
            { prevgyroOrientation[0] = 0.f;
              prevgyroOrientation[1] = 0.f;
              prevgyroOrientation[2] = 0.f;
            }
            if(prevaccMagOrientation == null)
            { prevaccMagOrientation[0] = 0.f;
              prevaccMagOrientation[1] = 0.f;
              prevaccMagOrientation[2] = 0.f;
            }
            
            correlation = (float) Math.abs((gyroOrientation[0]-accMagOrientation[0])%(2*Math.PI));
            magnetic_angle_change =(float) Math.abs((accMagOrientation[0] - prevaccMagOrientation[0])%(2*Math.PI));
            	
            if(correlation <= CORRELATION_THRESHOLD && magnetic_angle_change <= MAGNETIC_ANGLE_CHANGE_THRESHOLD)
            {	alpha = 0.49f; beta= 0.49f; gamma = 0.02f;             	
            }
            else if(correlation <= CORRELATION_THRESHOLD && magnetic_angle_change > MAGNETIC_ANGLE_CHANGE_THRESHOLD)
            {  alpha = 0.f; beta= 0.98f; gamma = 0.02f;
            }
            else if(correlation > CORRELATION_THRESHOLD && magnetic_angle_change <= MAGNETIC_ANGLE_CHANGE_THRESHOLD)
            {  alpha = 1.0f; beta= 0.f; gamma = 0.f;
            }
            else if(correlation > CORRELATION_THRESHOLD && magnetic_angle_change > MAGNETIC_ANGLE_CHANGE_THRESHOLD)
            {  alpha = 0.50f; beta= 0.50f; gamma=0.f;
            }
            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
                    
        	fusedOrientation[0] = alpha*prevfusedOrientation[0] + beta*gyroOrientation[0] + gamma*accMagOrientation[0];
        	fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
       // 	fusedOrientation[0] += (fusedOrientation[0] < -Math.PI) ? 2.0 * Math.PI : 0;
        	prevfusedOrientation[0] = fusedOrientation[0];
        	prevgyroOrientation[0] = gyroOrientation[0];
        	prevaccMagOrientation[0] = accMagOrientation[0]; 
        	
        	fusedOrientation[1] = alpha*prevfusedOrientation[1] + beta*gyroOrientation[1] + gamma*accMagOrientation[1];
        	fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
        	//	fusedOrientation[1] += (fusedOrientation[1] < -Math.PI) ? 2.0 * Math.PI : 0;
        	prevfusedOrientation[1] = fusedOrientation[1];
        	prevgyroOrientation[1] = gyroOrientation[1];
        	prevaccMagOrientation[1] = accMagOrientation[1]; 
        	
        	fusedOrientation[2] = alpha*prevfusedOrientation[2] + beta*gyroOrientation[2] + gamma*accMagOrientation[2];
        	fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
        	//fusedOrientation[2] += (fusedOrientation[2] < -Math.PI) ? 2.0 * Math.PI : 0;
        	prevfusedOrientation[2] = fusedOrientation[2];
        	prevgyroOrientation[2] = gyroOrientation[2];
        	prevaccMagOrientation[2] = accMagOrientation[2]; 
      
        	if(this.isSampling()) {
        		try {		
    				mOrientationLogFileWriter.write( realaccMagOrientation[0] +  ","  + realaccMagOrientation[1] + ","  + realaccMagOrientation[2]+ "," + realgyroOrientation[0] +  ","  + realgyroOrientation[1] + ","  + realgyroOrientation[2]+ "," + fusedOrientation[0] +  "," + fusedOrientation[1] + "," + fusedOrientation[2]+"\n");	
        		} 
    			catch (IOException e) {
    				Log.e(TAG, "Log file write for Rotation Vector failed!!!\n", e);
    				e.printStackTrace();
    				throw new RuntimeException(e);
    	    	}
    		 }
    	  
        	
//        	gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            //System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);            
      //      update sensor output in GUI
      //      mHandler.post(updateOreintationDisplayTask);
    }
    
	public void startSampling()
    {	try {
			String r = (String) (DateFormat.format("yyyy-MM-dd-hh-mm-ss", new java.util.Date()) );
			String logFileBaseName = "Loc_" + location + "_" + r;
			final File parent = new File(SAMPLES_DIR);
			final File app = new File(SAMPLES_DIR, logFileBaseName + ".sensor.csv");
			final File pro = new File(SAMPLES_DIR, logFileBaseName + ".orient.csv");
			if(!parent.mkdirs())
			 {System.err.println("could not create directories");}
			mRotationLogFileWriter = new FileWriter(app);
			mOrientationLogFileWriter = new FileWriter(pro);
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
			mOrientationLogFileWriter.flush();
			mOrientationLogFileWriter.close();
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


