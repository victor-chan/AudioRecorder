package com.example.audiorecorder;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CompositeEncoder implements AmpableEncoder {
	
	private ArrayList<Encoder> mEncoders;
	private IAmplitude mAmpableEncoder;
	
	public CompositeEncoder() {
		
	}
	
	public void addEncoder(Encoder encoder) {
		if (mEncoders == null) {
			mEncoders = new ArrayList<>();
		}
		
		mEncoders.add(encoder);
		if (encoder instanceof IAmplitude) {
			mAmpableEncoder =  (IAmplitude) encoder;
		}
	}
	
//	private IAmplitude getAmplitedeableEncoder() {
//		if (mEncoders != null) {
//			for (Encoder encoder : mEncoders) {
//				if (encoder instanceof IAmplitude) {
//					return (IAmplitude) encoder;
//				}
//			}
//		}
//		return null;
//	}

	@Override
	public float getMaxAmplitude() {
		if (mAmpableEncoder != null) {
			return mAmpableEncoder.getMaxAmplitude();
		}
		return 0;
	}

	@Override
	public float getAverageAmplitude() {
		if (mAmpableEncoder != null) {
			return mAmpableEncoder.getAverageAmplitude();
		}
		return 0;
	}

	@Override
	public int encode(ByteBuffer data, int length) {
		int len = 0;
		if (mEncoders != null) {
			for (Encoder encoder : mEncoders) {
				int encodedLen = encoder.encode(data, length);
				if (len == 0) {
					len = encodedLen;
				}
			}
		}
		return len;
	}

	@Override
	public void flush() {
		if (mEncoders != null) {
			for (Encoder encoder : mEncoders) {
				encoder.flush();
			}
		}
	}

	@Override
	public void release() {
		if (mEncoders != null) {
			for (Encoder encoder : mEncoders) {
				encoder.release();
			}
		}
	}

}
