package com.example.audiorecorder;

import java.io.File;
import java.lang.ref.WeakReference;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.audiorecorder.AudioRecorder.Amplitudes;
import com.example.audiorecorder.PCMPlayer.PlayerListener;

public class MainActivity extends ActionBarActivity {

	private static class UIHandler extends Handler {
		
		private static final float AMP_THRESHOLD = 0.2f;
		
		private Runnable mAutoStopRecordRunnable = null;

		private WeakReference<MainActivity> mMainActRef;

		public UIHandler(MainActivity act) {
			mMainActRef = new WeakReference<>(act);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case AudioRecorder.MSG_AMPLITUDES:
				Amplitudes amp = (Amplitudes) msg.obj;
				MainActivity act = mMainActRef.get();
				if (act != null) {
					act.setAmplitude((int) (amp.mPeak * 100));
				}
				
				shouldStopRecording(amp);
				break;
			}
		}
		
		private void shouldStopRecording(Amplitudes amp) {
			if (amp.mPeak < AMP_THRESHOLD) {
				if (mAutoStopRecordRunnable == null) {
					mAutoStopRecordRunnable = new Runnable() {
						
						@Override
						public void run() {
							MainActivity act = mMainActRef.get();
							if (act != null) {
								act.stopRecord();
							}
						}
					};
					postDelayed(mAutoStopRecordRunnable, 3000);
				}
			} else {
				if (mAutoStopRecordRunnable != null) {
					removeCallbacks(mAutoStopRecordRunnable);
					mAutoStopRecordRunnable = null;
				}
			}
		}
	};
	
	private ProgressBar mProgressBar;
	private Button mPlayBtn;
	private Button mRecordBtn;

	private PCMPlayer mPCMPlayer;
	private AudioRecorder mRecorder;

	public void setAmplitude(int progress) {
		mProgressBar.setProgress(progress);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		long now = System.currentTimeMillis();
        String outFile = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	outFile = new File(Environment.getExternalStorageDirectory(), String.valueOf(now)).getAbsolutePath();
        } else {
        	outFile = getFileStreamPath(String.valueOf(now)).getAbsolutePath();
        }
		mRecorder = new AudioRecorder(new UIHandler(this), outFile);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		mPlayBtn = (Button) findViewById(R.id.play);
		mPlayBtn.setEnabled(false);
		mRecordBtn = (Button) findViewById(R.id.record);
		
		mRecordBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mRecorder.isRecording()) {
					stopRecord();
				} else {
					startRecord();
				}
			}
		});
		mPlayBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				playPCM();
			}
		});
	}

	private void finishPlay() {
		mPCMPlayer = null;
		mPlayBtn.post(new Runnable() {

			@Override
			public void run() {
				mPlayBtn.setEnabled(true);
			}
		});
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}

	private void startRecord() {
		mRecorder.start();
		mRecorder.resumeRecording();
		mRecordBtn.setText("stop");
	}

	void stopRecord() {
		mRecorder.stopRecording();
		mPlayBtn.setEnabled(true);
		mRecordBtn.setEnabled(false);
		mRecordBtn.setText("record");
	}

	private void playPCM() {
		if (mRecorder == null) {
			return;
		}
		mPlayBtn.setEnabled(false);
		File file = new File(mRecorder.getOutputFile());
		if (!file.exists()) {
			Toast.makeText(MainActivity.this, "文件不存在",
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (mPCMPlayer == null) {
			mPCMPlayer = new PCMPlayer(MainActivity.this.getApplicationContext(), file.getAbsolutePath(),
						mRecorder.getSampleRate(), mRecorder.getChannel(), mRecorder.getFormat());
			mPCMPlayer.setListener(new PlayerListener() {

				@Override
				public void onFinished() {
					finishPlay();
				}

				@Override
				public void onError() {
					finishPlay();
				}
			});
			mPCMPlayer.startPlayback();
		}
	}
}
