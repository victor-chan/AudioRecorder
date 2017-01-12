package com.example.audiorecorder;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavEncoder implements Encoder
{
	
	private int mBps;

	private int mChannels;

	private int mSampleRate;

	private String mFileName;
	
	private DataOutputStream mDataOutStream;
	
	public WavEncoder(String fileName, int sampleRate, int channels, int bps) {
		mFileName = fileName;
		mSampleRate = sampleRate;
		mChannels = channels;
		mBps = bps;
		try {
			mDataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int encode(ByteBuffer data, int length)
	{
		if (mDataOutStream != null) {
			try {
				if (mBps == AudioRecorder.FORMAT_16_BIT) {
					short[] shortArray = new short[data.remaining() / 2];
					data.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
					for (int i = 0; i < shortArray.length; i++) {
						mDataOutStream.writeShort(shortArray[i]);
					}
				} /*else if (mBps == AudioRecorder.FORMAT_8_BIT) {
					byte[] ba = new byte[data.remaining()];
					data.get(ba);
					mDataOutStream.write(ba, 0, ba.length);
				}*/
				return length;
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		return 0;
	}

	@Override
	public void flush()
	{
		if (mDataOutStream != null) {
			try {
				mDataOutStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void release()
	{
		if (mDataOutStream != null) {
			try {
				mDataOutStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			//TODO: Can use randomaccessfile insert?
			rawToWave(new File(mFileName), new File(mFileName + ".wav"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void rawToWave(final File rawFile, final File waveFile) throws IOException {

		byte[] rawData = new byte[(int) rawFile.length()];
		DataInputStream input = null;
		try {
			input = new DataInputStream(new FileInputStream(rawFile));
			input.read(rawData);
		} finally {
			if (input != null) {
				input.close();
			}
		}

		DataOutputStream output = null;
		try {
			output = new DataOutputStream(new FileOutputStream(waveFile));
			// WAVE header
			// see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
			writeString(output, "RIFF"); // chunk id
			writeInt(output, 36 + rawData.length); // chunk size
			writeString(output, "WAVE"); // format
			writeString(output, "fmt "); // subchunk 1 id
			writeInt(output, 16); // subchunk 1 size
			writeShort(output, (short) 1); // audio format (1 = PCM)
			writeShort(output, (short) 1); // number of channels
			writeInt(output, mSampleRate); // sample rate
			writeInt(output, mSampleRate * 2); // byte rate
			writeShort(output, (short) 2); // block align
			writeShort(output, (short) 16); // bits per sample
			writeString(output, "data"); // subchunk 2 id
			writeInt(output, rawData.length); // subchunk 2 size
			// Audio data (conversion big endian -> little endian)
			short[] shorts = new short[rawData.length / 2];
			ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
			ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
			for (short s : shorts) {
				bytes.putShort(s);
			}
			output.write(bytes.array());
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}
	
	private void writeInt(final DataOutputStream output, final int value) throws IOException {
		output.write(value >> 0);
		output.write(value >> 8);
		output.write(value >> 16);
		output.write(value >> 24);
	}

	private void writeShort(final DataOutputStream output, final short value) throws IOException {
		output.write(value >> 0);
		output.write(value >> 8);
	}

	private void writeString(final DataOutputStream output, final String value) throws IOException {
		for (int i = 0; i < value.length(); i++) {
			output.write(value.charAt(i));
		}
	}
}
