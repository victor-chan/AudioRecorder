package com.example.audiorecorder;

import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

/**
 * Created by chenkai on 2017/1/6.
 */

public class AudioRecorder extends Thread {

    private static final String TAG  = "AudioRecorder";
    
    public static final int FORMAT_8_BIT = 8;
    public static final int FORMAT_16_BIT = 16;

    public static final int MSG_OK                    = 0;
    public static final int MSG_INVALID_FORMAT        = 1;
    public static final int MSG_HARDWARE_UNAVAILABLE  = 2;
    public static final int MSG_ILLEGAL_ARGUMENT      = 3;
    public static final int MSG_READ_ERROR            = 4;
    public static final int MSG_WRITE_ERROR           = 5;
    public static final int MSG_AMPLITUDES            = 6;

    private final Handler mHandler;
    private AmpableEncoder mEncoder;
    public boolean mShouldRun;

    private boolean mShouldRecord = false;

    private double mDuration;

    
	private String mOutFile;

	private int mSampleRate;

	private int mChannelConfig;

	private int mFormat;

    public AudioRecorder(Handler handler, String outFilePath) {
        mHandler = handler;
        mOutFile = outFilePath;
    }
    
    public void resumeRecording()
    {
      mShouldRecord = true;
    }

    /**
     * return file name with absolute path, without extension
     */
    public String getOutputFile() {
    	return mOutFile;
    }
    
    public int getSampleRate() {
    	return mSampleRate;
    }
    
    public int getChannel() {
    	return mChannelConfig;
    }
    
    public int getFormat() {
    	return mFormat;
    }

    public void pauseRecording()
    {
      mShouldRecord = false;
    }
    
    
    public void startRecording() {
    	start();
    	resumeRecording();
    }

    public void stopRecording() {
    	pauseRecording();
    	mShouldRun = false;
    	interrupt();
    	try {
    		join();
    	} catch (InterruptedException ex) {
    	      // pass
    	}
    }


    public boolean isRecording()
    {
      return mShouldRun && mShouldRecord;
    }

    public static int mapChannelConfig(int channelConfig)
    {
        switch (channelConfig) {
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                return 1;

            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
                return 2;

            default:
                return 0;
        }
    }


    public static int mapFormat(int format)
    {
        switch (format) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return FORMAT_8_BIT;

            case AudioFormat.ENCODING_PCM_16BIT:
                return FORMAT_16_BIT;

            default:
                return 0;
        }
    }

    public Amplitudes getAmplitudes()
    {
        if (null == mEncoder) {
            return null;
        }

        Amplitudes amp = new Amplitudes();
        amp.mPosition = (long) mDuration;
        amp.mPeak = mEncoder.getMaxAmplitude();
        amp.mAverage = mEncoder.getAverageAmplitude();

        return amp;
    }

    @Override
    public void run() {
        // Determine audio config to use.
        final int sampleRates[] = { 96000, 48000, 44100, 22050, 11025 };
        final int configs[] = { AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.CHANNEL_CONFIGURATION_STEREO };
        final int formats[] = { AudioFormat.ENCODING_PCM_16BIT/*, AudioFormat.ENCODING_PCM_8BIT */};

        mSampleRate = -1;
        mChannelConfig = -1;
        mFormat = -1;

        int bufsize = AudioRecord.ERROR_BAD_VALUE;
        AudioRecord recorder = null;

        boolean found = false;
        for (int x = 0 ; !found && x < formats.length ; ++x) {
            mFormat = formats[x];

            for (int y = 0 ; !found && y < sampleRates.length ; ++y) {
                mSampleRate = sampleRates[y];

                for (int z = 0 ; !found && z < configs.length ; ++z) {
                    mChannelConfig = configs[z];

                    bufsize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mFormat);

                    // Handle invalid configs
                    if (AudioRecord.ERROR_BAD_VALUE == bufsize) {
                        continue;
                    }
                    if (AudioRecord.ERROR == bufsize) {
                        Log.e(TAG, "Unable to query hardware!");
                        mHandler.obtainMessage(MSG_HARDWARE_UNAVAILABLE).sendToTarget();
                        return;
                    }

                    try {
                        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate,
                                mChannelConfig, mFormat, bufsize);
                    } catch (IllegalArgumentException ex) {
                        recorder = null;
                        continue;
                    }

                    // Got a valid config.
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            Log.e(TAG, "Sample rate, channel config or format not supported!");
            mHandler.obtainMessage(MSG_INVALID_FORMAT).sendToTarget();
            return;
        }
        Log.d(TAG, "Using: " + mFormat + "/" + mChannelConfig + "/" + mSampleRate);

        mShouldRun = true;
        boolean oldShouldRecord = false;

        try {
            // Initialize variables for calculating the recording duration.
            int mappedFormat = mapFormat(mFormat);
            int mappedChannels = mapChannelConfig(mChannelConfig);
            int bytesPerSecond = mSampleRate * (mappedFormat / 8) * mappedChannels;

            mEncoder = EncoderFactory.getEncoder(mOutFile, mSampleRate, mappedChannels, mappedFormat);
            
            // Start recording loop
            mDuration = 0.0;
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufsize);
            while (mShouldRun) {
                // Toggle recording state, if necessary
                if (mShouldRecord != oldShouldRecord) {
                    // State changed! Let's see what we are supposed to do.
                    if (mShouldRecord) {
                        // Log.d(LTAG, "Start recording!");
                        recorder.startRecording();
                    }
                    else {
                        // Log.d(LTAG, "Stop recording!");
                        recorder.stop();
                        mEncoder.flush();
                    }
                    oldShouldRecord = mShouldRecord;
                }

                // If we're supposed to be recording, read data.
                if (mShouldRecord) {
                    int result = recorder.read(buffer, bufsize);
                    switch (result) {
                        case AudioRecord.ERROR_INVALID_OPERATION:
                            Log.e(TAG, "Invalid operation.");
                            mHandler.obtainMessage(MSG_READ_ERROR).sendToTarget();
                            break;

                        case AudioRecord.ERROR_BAD_VALUE:
                            Log.e(TAG, "Bad value.");
                            mHandler.obtainMessage(MSG_READ_ERROR).sendToTarget();
                            break;

                        default:
                            if (result > 0) {
                                // Compute time recorded
                                double read_ms = (1000.0 * result) / bytesPerSecond;
                                mDuration += read_ms;

                                //long start = System.currentTimeMillis();
                                int writeResult = mEncoder.encode(buffer, result);
                                if (writeResult != result) {
                                    Log.e(TAG, "Attempted to write " + result
                                            + " but only wrote " + writeResult);
                                    mHandler.obtainMessage(MSG_WRITE_ERROR).sendToTarget();
                                }
                                else {
                                    Amplitudes amp = getAmplitudes();
                                    mHandler.obtainMessage(MSG_AMPLITUDES, amp).sendToTarget();
                                }
                                //long end = System.currentTimeMillis();
                                //Log.d(LTAG, "Write of " + result + " bytes took " + (end - start) + " msec.");
                            }
                    }
                }
            }

            recorder.release();
            mEncoder.release();
            mEncoder = null;

        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "Illegal argument: " + ex.getMessage());
            mHandler.obtainMessage(MSG_ILLEGAL_ARGUMENT, ex.getMessage()).sendToTarget();
        }
    }

    /***************************************************************************
     * Simple class for reporting measured Amplitudes to user of FLACRecorder
     **/
    public static class Amplitudes
    {
        public long   mPosition;
        public float  mPeak;
        public float  mAverage;


        public Amplitudes()
        {
        }


        public Amplitudes(Amplitudes other)
        {
            mPosition = other.mPosition;
            mPeak = other.mPeak;
            mAverage = other.mAverage;
        }


        public String toString()
        {
            return String.format("%dms: %f/%f", mPosition, mAverage, mPeak);
        }


        public void accumulate(Amplitudes other)
        {
            // Position is simple; the overall position is the sum of
            // both positions.
            long oldPos = mPosition;
            mPosition += other.mPosition;

            // The higher peak is the overall peak.
            if (other.mPeak > mPeak) {
                mPeak = other.mPeak;
            }

            // Average is more complicated, because it needs to be weighted
            // on the time it took to calculate it.
            float weightedOld = mAverage * (oldPos / (float) mPosition);
            float weightedNew = other.mAverage * (other.mPosition / (float) mPosition);
            mAverage = weightedOld + weightedNew;
        }
    }
}
