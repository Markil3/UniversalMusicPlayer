/* WaveReader.java

   Copyright (c) 2011 Ethan Chen

   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2 of the License, or (at your option) any later version.

   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.intervigil.wave;

import java.io.*;

import com.intervigil.wave.exception.InvalidWaveException;

public class WaveReader {
    private static final int WAV_HEADER_CHUNK_ID = 0x52494646;  // "RIFF"
    private static final int WAV_FORMAT = 0x57415645;  // "WAVE"
    private static final int WAV_FORMAT_CHUNK_ID = 0x666d7420; // "fmt "
    private static final int WAV_DATA_CHUNK_ID = 0x64617461; // "data"
    private static final int STREAM_BUFFER_SIZE = 4096;

    private File mInFile;
    private BufferedInputStream mInStream;

    private int mSampleRate;
    private int mChannels;
    private int mSampleBits;
    private int mFileSize;
    private int mDataSize;


    /**
     * Constructor; initializes WaveReader to read from given file
     *
     * @param path  path to input file
     * @param name  name of input file
     */
    public WaveReader(String path, String name) {
        this.mInFile = new File(path + File.separator + name);
    }

    /**
     * Constructor; initializes WaveReader to read from given file
     *
     * @param file  handle to input file
     */
    public WaveReader(File file) {
        this.mInFile = file;
    }

    /**
     * Constructor; initializes WaveReader to read from given file
     *
     * @param file  handle to input file
     * @author William Hubbard, 7/24/2021
     */
    public WaveReader(InputStream stream) {
        this.mInFile = null;
        this.mInStream = new BufferedInputStream(stream, STREAM_BUFFER_SIZE);
    }

    /**
     * Open WAV file for reading
     *
     * @throws FileNotFoundException if input file does not exist
     * @throws InvalidWaveException if input file is not a valid WAVE file
     * @throws IOException if I/O error occurred during file read
     */
    public void openWave() throws FileNotFoundException, InvalidWaveException, IOException {
        /*
         * Only initializes the stream if needed ~ Change made by William Hubbard on 7/24/2021
         */
        if (this.mInStream == null) {
            FileInputStream fileStream = new FileInputStream(mInFile);
            mInStream = new BufferedInputStream(fileStream, STREAM_BUFFER_SIZE);
        }

        int headerId = readUnsignedInt(mInStream);  // should be "RIFF"
        if (headerId != WAV_HEADER_CHUNK_ID) {
            throw new InvalidWaveException(String.format("Invalid WAVE header chunk ID: %d", headerId));
        }
        mFileSize = readUnsignedIntLE(mInStream);  // length of header
        int format = readUnsignedInt(mInStream);  // should be "WAVE"
        if (format != WAV_FORMAT) {
            throw new InvalidWaveException("Invalid WAVE format");
        }
        
        int formatId = readUnsignedInt(mInStream);  // should be "fmt "
        if (formatId != WAV_FORMAT_CHUNK_ID) {
            throw new InvalidWaveException("Invalid WAVE format chunk ID");
        }
        int formatSize = readUnsignedIntLE(mInStream);
        if (formatSize != 16) {
            
        }
        int audioFormat = readUnsignedShortLE(mInStream);
        if (audioFormat != 1) {
            throw new InvalidWaveException("Not PCM WAVE format");
        }
        mChannels = readUnsignedShortLE(mInStream);
        mSampleRate = readUnsignedIntLE(mInStream);
        int byteRate = readUnsignedIntLE(mInStream);
        int blockAlign = readUnsignedShortLE(mInStream);
        mSampleBits = readUnsignedShortLE(mInStream);
        
        int dataId = readUnsignedInt(mInStream);
        if (dataId != WAV_DATA_CHUNK_ID) {
            throw new InvalidWaveException("Invalid WAVE data chunk ID");
        }
        mDataSize = readUnsignedIntLE(mInStream);
    }

    /**
     * Writes a short to a {@link ByteArrayOutputStream}.
     * @param stream - The stream to write to.
     * @param num - The number to write.
     * @throws IOException
     * @author William Hubbard, 7/24/2021
     * @author java.io.DataOutputStream#writeShort(int)
     */
    private void writeShort(ByteArrayOutputStream stream, short num) throws IOException {
        byte[] buf = new byte[2];
        buf[0] = (byte)(num >>> 8);
        buf[1] = (byte)(num >>> 0);
        stream.write(buf);
    }

    /**
     * Restructures the original wave header for the file as a byte array.
     * @return The original wave header
     * @throws IOException
     * @author William Hubbard, 7/24/2021
     * @author Ethan Chen, com.intervigil.wave.WaveWriter#writeWaveHeader()
     */
    public byte[] getHeaderBytes() throws IOException {
        // rewind to beginning of the file
        ByteArrayOutputStream stream = new ByteArrayOutputStream(44);

        int bytesPerSec = (mSampleBits + 7) / 8;

        stream.write("RIFF".getBytes()); // WAV chunk header
        stream.write(Integer.reverseBytes(36)); // WAV chunk size
        stream.write("WAVE".getBytes()); // WAV format

        stream.write("fmt ".getBytes()); // format subchunk header
        stream.write(Integer.reverseBytes(16)); // format subchunk size
        writeShort(stream, Short.reverseBytes((short) 1)); // audio format
        writeShort(stream, Short.reverseBytes((short) mChannels)); // number of channels
        stream.write(Integer.reverseBytes(mSampleRate)); // sample rate
        stream.write(Integer.reverseBytes(mSampleRate * mChannels * bytesPerSec)); // byte rate
        writeShort(stream, Short.reverseBytes((short) (mChannels * bytesPerSec))); // block align
        writeShort(stream, Short.reverseBytes((short) mSampleBits)); // bits per sample

        stream.write("data".getBytes()); // data subchunk header
        stream.write(Integer.reverseBytes(0)); // data subchunk size

        stream.close();

        return stream.toByteArray();
    }

    /**
     * Get sample rate
     *
     * @return input file's sample rate
     */
    public int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Get number of channels
     *
     * @return number of channels in input file
     */
    public int getChannels() {
        return mChannels;
    }

    /**
     * Get PCM format, S16LE or S8LE
     *
     * @return number of bits per sample
     */
    public int getPcmFormat() {
        return mSampleBits;
    }
    
    /**
     * Get file size
     *
     * @return total input file size in bytes
     */
    public int getFileSize() {
        return mFileSize + 8;
    }

    /**
     * Get input file's audio data size
     * Basically file size without headers included
     *
     * @return audio data size in bytes
     */
    public int getDataSize() {
        return mDataSize;
    }

    /**
     * Get input file length
     *
     * @return length of file in seconds
     */
    public int getLength() {
        if (mSampleRate == 0 || mChannels == 0 || (mSampleBits + 7) / 8 == 0) {
            return 0;
        } else {
            return mDataSize / (mSampleRate * mChannels * ((mSampleBits + 7) / 8));
        }
    }

    /**
     * Read audio data from input file (mono)
     *
     * @param dst  mono audio data output buffer
     * @param numSamples  number of samples to read
     *
     * @return number of samples read
     *
     * @throws IOException if file I/O error occurs
     */
    public int read(short[] dst, int numSamples) throws IOException {
        if (mChannels != 1) {
            return -1;
        }

        byte[] buf = new byte[numSamples * 2];
        int index = 0;
        int bytesRead = mInStream.read(buf, 0, numSamples * 2);

        for (int i = 0; i < bytesRead; i+=2) {
            dst[index] = byteToShortLE(buf[i], buf[i+1]);
            index++;
        }

        return index;
    }

    /**
     * Read audio data from input file (stereo)
     *
     * @param left  left channel audio output buffer
     * @param right  right channel audio output buffer
     * @param numSamples  number of samples to read
     *
     * @return number of samples read
     *
     * @throws IOException if file I/O error occurs
     */
    public int read(short[] left, short[] right, int numSamples) throws IOException {
        if (mChannels != 2) {
            return -1;
        }
        byte[] buf = new byte[numSamples * 4];
        int index = 0;
        int bytesRead = mInStream.read(buf, 0, numSamples * 4);

        for (int i = 0; i < bytesRead; i+=2) {
            short val = byteToShortLE(buf[0], buf[i+1]);
            if (i % 4 == 0) {
                left[index] = val;
            } else {
                right[index] = val;
                index++;
            }
        }

        return index;
    }

    /**
     * Close WAV file. WaveReader object cannot be used again following this call.
     *
     * @throws IOException if I/O error occurred closing filestream
     */
    public void closeWaveFile() throws IOException {
        if (mInStream != null) {
            mInStream.close();
        }
    }
    
    private static short byteToShortLE(byte b1, byte b2) {
        return (short) (b1 & 0xFF | ((b2 & 0xFF) << 8));
    }

    private static int readUnsignedInt(BufferedInputStream in) throws IOException {
        int ret;
        byte[] buf = new byte[4];
        ret = in.read(buf);
        if (ret == -1) {
            return -1;
        } else {
            return (((buf[0] & 0xFF) << 24)
                    | ((buf[1] & 0xFF) << 16)
                    | ((buf[2] & 0xFF) << 8)
                    | (buf[3] & 0xFF));
        }
    }
    
    private static int readUnsignedIntLE(BufferedInputStream in) throws IOException {
        int ret;
        byte[] buf = new byte[4];
        ret = in.read(buf);
        if (ret == -1) {
            return -1;
        } else {
            return (buf[0] & 0xFF
                    | ((buf[1] & 0xFF) << 8)
                    | ((buf[2] & 0xFF) << 16)
                    | ((buf[3] & 0xFF) << 24));
        }
    }
    
    private static short readUnsignedShortLE(BufferedInputStream in) throws IOException {
        int ret;
        byte[] buf = new byte[2];
        ret = in.read(buf, 0, 2);
        if (ret == -1) {
            return -1;
        } else {
            return byteToShortLE(buf[0], buf[1]);
        }
    }
}