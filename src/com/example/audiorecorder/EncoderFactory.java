package com.example.audiorecorder;

public class EncoderFactory {

	public static AmpableEncoder getEncoder(String fileName, int sampleRate, int channels, int bps) {
		FLACEncoder flacEncoder = new FLACEncoder(fileName, sampleRate, channels, bps);
		WavEncoder wavEncoder = new WavEncoder(fileName, sampleRate, channels, bps);
		CompositeEncoder encoder = new CompositeEncoder();
		encoder.addEncoder(flacEncoder);
		encoder.addEncoder(wavEncoder);
		return encoder;
	}

}
