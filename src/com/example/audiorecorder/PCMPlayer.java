
package com.example.audiorecorder;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class PCMPlayer extends Thread {
	
	/***************************************************************************
	 * Private constants
	 **/
	// Log ID
	private static final String LTAG = "PCMPlayer";

	// Sleep time (in msec) when playback is paused. Will be interrupted, so
	// this can be arbitrarily large.
	private static final int PAUSED_SLEEP_TIME = 10 * 60 * 1000;

	/***************************************************************************
	 * Public data
	 **/
	// Flag that keeps the thread running when true.
	public volatile boolean mShouldRun;

	/***************************************************************************
	 * Listener that informs the user of errors and end of playback.
	 **/
	public static abstract class PlayerListener {
		public abstract void onError();

		public abstract void onFinished();
	}

	/***************************************************************************
	 * Private data
	 **/
	// Context in which this object was created
	private Context mContext;

	// Audio track
	private AudioTrack mAudioTrack;

	// File path for the output file.
	private String mPath;

	// Flag; determines whether playback is paused or not.
	private volatile boolean mPaused;


	// Listener.
	private PlayerListener mListener;

	private int mFormat;

	private int mChannel;

	private int mSampleRate;

	public PCMPlayer(Context context, String path, int sampleRate, int channel,
			int format) {
		mContext = context;
		mPath = path;
		mSampleRate = sampleRate;
		mChannel = channel;
		mFormat = format;

		mShouldRun = true;
		mPaused = true;
	}

	public void startPlayback() {
		start();
		resumePlayback();
	}
	public void pausePlayback() {
		mPaused = true;
		interrupt();
	}

	public void resumePlayback() {
		mPaused = false;
		interrupt();
	}

	public void setListener(PlayerListener listener) {
		mListener = listener;
	}

	public void run() {
		int bufsize = AudioTrack.getMinBufferSize(mSampleRate, mChannel,
				mFormat);

		BufferedInputStream inStream = null;
		// Create AudioTrack.
		try {
			mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					mSampleRate, mChannel, mFormat, bufsize,
					AudioTrack.MODE_STREAM);
			mAudioTrack.play();

			byte[] tmpbuf = new byte[bufsize];

			inStream = new BufferedInputStream(new FileInputStream(mPath));

			while (mShouldRun) {
				try {
					// If we're paused, just sleep the thread
					if (mPaused) {
						sleep(PAUSED_SLEEP_TIME);
						continue;
					}

					int read = inStream.read(tmpbuf, 0, tmpbuf.length);
					if (read <= 0) {
						break;
					}
					short[] sa = new short[read / 2];
					ByteBuffer.wrap(tmpbuf, 0, read)
							.order(ByteOrder.BIG_ENDIAN).asShortBuffer()
							.get(sa);
					mAudioTrack.write(sa, 0, sa.length);
				} catch (InterruptedException ex) {
					// We'll pass through to the next iteration. If mShouldRun
					// has
					// been set to false, the thread will terminate. If mPause
					// has
					// been set to true, we'll sleep in the next interation.
				} catch (IOException e) {
					e.printStackTrace();
					if (null != mListener) {
						mListener.onError();
					}
				}
			}

			if (null != mListener) {
				mListener.onFinished();
			}

		} catch (IllegalArgumentException ex) {
			Log.e(LTAG, "Could not initialize AudioTrack.");

			if (null != mListener) {
				mListener.onError();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			if (null != mListener) {
				mListener.onError();
			}
		} finally {
			release();
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void release() {
		if (mAudioTrack != null) {
			mAudioTrack.stop();
			mAudioTrack.release();
			mAudioTrack = null;
		}
	}
}
