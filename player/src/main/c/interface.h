#ifndef INTERFACE_H
#define INTERFACE_H

#include <jni.h>
#include <stdio.h>

typedef struct {
    short numChannels;
    short bytesPerChannel;
    int sampleRate;
} HeaderData;

static jclass InputStream;
static jclass AudioStream;
static jclass Header;
static jmethodID AudioStream_getHeader;
static jmethodID AudioStream_getByteStream;
static jmethodID AudioStream_readInt;
static jmethodID InputStream_readBuffer;
static jmethodID Header_getChunkId;
static jmethodID Header_getChunkSize;
static jmethodID Header_getFormat;
static jmethodID Header_getSubChunk1ID;
static jmethodID Header_getSubChunk1Size;
static jmethodID Header_getAudioFormat;
static jmethodID Header_getNumChannels;
static jmethodID Header_getSampleRate;
static jmethodID Header_getByteRate;
static jmethodID Header_getBlockAlign;
static jmethodID Header_getBitsPerSample;
static jmethodID Header_getSubChunk2ID;
static jmethodID Header_getSubChunk2Size;

/**
 * This function is used to initialize the static reference to java.io.InputStream class.
 *
 * @param env - A reference to the JVM.
 * @param audio - An InputStream instance
 * @return A reference to the InputStream class.
 */
jclass getInputStreamClass(JNIEnv *env);

/**
 * This function is used to initialize the static reference to the AudioStream class.
 *
 * @param env - A reference to the JVM.
 * @param audio - An AudioStream instance
 * @return A reference to the AudioStream class.
 */
jclass getAudioStreamClass(JNIEnv *env, jobject audio);

jclass getHeaderClass(JNIEnv *env);

/**
 * This function is used to initialize the static reference to AudioStream's getHeader method.
 *
 * @param env - A reference to the JVM.
 * @param audio - An AudioStream instance
 * @return A reference to the getHeader method.
 */
jmethodID getHeaderMethod(JNIEnv *env, jobject audio);

/**
 * This function is used to initialize the static reference to AudioStream's getByteStream method.
 *
 * @param env - A reference to the JVM.
 * @param audio - An AudioStream instance
 * @return A reference to the getByteStream method.
 */
jmethodID getByteStreamMethod(JNIEnv *env, jobject audio);

/**
 * This function is used to initialize the static references to AudioStream's read methods.
 *
 * @param env - A reference to the JVM.
 * @param audio - An AudioStream instance
 * @return A reference to the getByteStream method.
 */
jmethodID getReadMethod(JNIEnv *env, jobject audio);

/**
 * This function is used to initialize the static reference to WavHeader's various methods.
 *
 * @param env - A reference to the JVM.
 */
void getHeaderMethods(JNIEnv *env);

#endif /* INTERFACE_H */