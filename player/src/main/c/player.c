/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */
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

static jobject currentFile = NULL;
static HeaderData currentHeader;

/**
 * This function is used to initialize the static reference to java.io.InputStream class.
 *
 * @param env - A reference to the JVM.
 * @param audio - An InputStream instance
 * @return A reference to the InputStream class.
 */
jclass getInputStreamClass(JNIEnv *env)
{
    if (InputStream == 0 )
    {
        jclass tempClass = (*env)->FindClass(env, "java/io/InputStream");
        if (tempClass == 0)
        {
            fprintf(stderr, "Could not find class java/io/InputStream");
            return 0;
        }
        InputStream = (*env)->NewGlobalRef(env, tempClass);
    }
    return InputStream;
}

/**
 * This function is used to initialize the static reference to the AudioStream class.
 *
 * @param env - A reference to the JVM.
 * @param audio - An AudioStream instance
 * @return A reference to the AudioStream class.
 */
jclass getAudioStreamClass(JNIEnv *env, jobject audio)
{
    if (AudioStream == 0 )
    {
        jclass tempClass = (*env)->GetObjectClass(env, audio);
        if (tempClass == 0)
        {
            fprintf(stderr, "Could not find class edu/regis/universeplayer_localPlayer/AudioFile");
            return 0;
        }
        AudioStream = (*env)->NewGlobalRef(env, tempClass);
    }
    return AudioStream;
}

jclass getHeaderClass(JNIEnv *env)
{
    if (Header == 0)
    {
        jclass tempClass = (*env)->FindClass(env, "wave/WavHeader");
        if (tempClass == 0)
        {
            fprintf(stderr, "Could not find wave/WavHeader");
            return 0;
        }
        Header = (*env)->NewGlobalRef(env, tempClass);
    }
    return Header;
}

/**
 * This function is used to initialize the static reference to AudioStream's getHeader method.
 *
 * @param env - A reference to the JVM.
 * @param audio - An AudioStream instance
 * @return A reference to the getHeader method.
 */
jmethodID getHeaderMethod(JNIEnv *env, jobject audio)
{
    if (AudioStream_getHeader == 0 )
    {
        jclass tempClass = getAudioStreamClass(env, audio);
        if (tempClass == 0)
        {
            return 0;
        }
        AudioStream_getHeader = (*env)->GetMethodID(env, tempClass, "getHeader", "()Lwave/WavHeader;");
    }
    return AudioStream_getHeader;
}

/**
 * This function is used to initialize the static reference to AudioStream's getByteStream method.
 *
 * @param env - A reference to the JVM.
 * @param audio - An AudioStream instance
 * @return A reference to the getByteStream method.
 */
jmethodID getByteStreamMethod(JNIEnv *env, jobject audio)
{
    if (AudioStream_getByteStream == 0 )
    {
        jclass tempClass = getAudioStreamClass(env, audio);
        if (tempClass == 0)
        {
            return 0;
        }
        AudioStream_getByteStream = (*env)->GetMethodID(env, tempClass, "getByteStream", "()[B");
    }
    return AudioStream_getByteStream;
}

/**
 * This function is used to initialize the static references to AudioStream's read methods.
 *
 * @param env - A reference to the JVM.
 * @param audio - An AudioStream instance
 * @return A reference to the getByteStream method.
 */
jmethodID getReadMethod(JNIEnv *env, jobject audio)
{
    jclass tempClass;
    if (AudioStream_readInt == 0)
    {
        tempClass = getAudioStreamClass(env, audio);
        if (tempClass == 0)
        {
            fprintf(stderr, "Could not find AudioStream class");
        }
        else
        {
            AudioStream_readInt = (*env)->GetMethodID(env, tempClass, "read", "()I");
            printf("Found class AudioStream");
        }
    }
    if (InputStream_readBuffer == 0 )
    {
        tempClass = getInputStreamClass(env);
        if (tempClass == 0)
        {
            fprintf(stderr, "Could not find InputStream class");
        }
        else
        {
            InputStream_readBuffer = (*env)->GetMethodID(env, tempClass, "read", "([B)I");
            printf("Found class java/io/InputStream");
        }
    }
    return AudioStream_readInt;
}

/**
 * This function is used to initialize the static reference to WavHeader's various methods.
 *
 * @param env - A reference to the JVM.
 */
void getHeaderMethods(JNIEnv *env)
{
    jclass tempClass = getHeaderClass(env);
    if (tempClass == 0)
    {
        return;
    }
    if (Header_getChunkId == 0)
    {
        Header_getChunkId = (*env)->GetMethodID(env, tempClass, "getChunkID", "()[B");
        if (Header_getChunkId == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getChunkID\n");
	    }
    }
    if (Header_getChunkSize == 0)
    {
        Header_getChunkSize = (*env)->GetMethodID(env, tempClass, "getChunkSize", "()I");
        if (Header_getChunkSize == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getChunkSize\n");
	    }
    }
	if (Header_getFormat == 0)
    {
        Header_getFormat = (*env)->GetMethodID(env, tempClass, "getFormat", "()[B");
	    if (Header_getFormat == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getFormat\n");
	    }
    }
	if (Header_getSubChunk1ID == 0)
	{
	    Header_getSubChunk1ID = (*env)->GetMethodID(env, tempClass, "getSubChunk1ID", "()[B");
	    if (Header_getSubChunk1ID == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getSubChunk1ID\n");
	    }
	}
	if (Header_getSubChunk1Size == 0)
	{
	    Header_getSubChunk1Size = (*env)->GetMethodID(env, tempClass, "getSubChunk1Size", "()I");
	    if (Header_getSubChunk1Size == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getSubChunk1Size\n");
	    }
	}
	if (Header_getAudioFormat == 0)
	{
	    Header_getAudioFormat = (*env)->GetMethodID(env, tempClass, "getAudioFormat", "()S");
	    if (Header_getAudioFormat == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getAudioFormat\n");
	    }
	}
	if (Header_getNumChannels == 0)
	{
	    Header_getNumChannels = (*env)->GetMethodID(env, tempClass, "getNumChannels", "()S");
	    if (Header_getNumChannels == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getNumChannels\n");
	    }
	}
	if (Header_getSampleRate == 0)
	{
	    Header_getSampleRate = (*env)->GetMethodID(env, tempClass, "getSampleRate", "()I");
	    if (Header_getSampleRate == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getSampleRate\n");
	    }
	}
	if (Header_getByteRate == 0)
	{
	    Header_getByteRate = (*env)->GetMethodID(env, tempClass, "getByteRate", "()I");
	    if (Header_getByteRate == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getByteRate\n");
	    }
	}
	if (Header_getBlockAlign == 0)
	{
	    Header_getBlockAlign = (*env)->GetMethodID(env, tempClass, "getBlockAlign", "()S");
	    if (Header_getBlockAlign == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getBlockAlign\n");
	    }
	}
	if (Header_getBitsPerSample == 0)
	{
	    Header_getBitsPerSample = (*env)->GetMethodID(env, tempClass, "getBitsPerSample", "()S");
	    if (Header_getBitsPerSample == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getBitsPerSample\n");
	    }
	}
	if (Header_getSubChunk2ID == 0)
	{
	    Header_getSubChunk2ID = (*env)->GetMethodID(env, tempClass, "getSubChunk2ID", "()[B");
	    if (Header_getSubChunk2ID == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getSubChunk2ID\n");
	    }
	}
	if (Header_getSubChunk2Size == 0)
	{
	    Header_getSubChunk2Size = (*env)->GetMethodID(env, tempClass, "getSubChunk2Size", "()I");
	    if (Header_getSubChunk2Size == 0)
	    {
	        fprintf(stderr, "Could not find wave/WavHeader#getSubChunk2Size\n");
	    }
	}
}

/**
 * Obtains an audio stream's header.
 * @param env - A reference to the JVM.
 * @param stream - The audio stream to obtain the header from
 * @return The header information.
 */
jobject getHeader(JNIEnv *env, jobject stream)
{
    if (AudioStream_getHeader == 0)
    {
        getHeaderMethod(env, stream);
        if (AudioStream_getHeader == 0)
        {
            return 0;
        }
    }
    jobject returnValue = (*env)->CallObjectMethod(env, stream, AudioStream_getHeader);
    return returnValue;
}

/**
 * Obtains the original raw byte data for an audio stream's header.
 * @param env - A reference to the JVM.
 * @param stream - The audio stream to obtain the header from.
 * @return The header information.
 */
jbyteArray getByteStream(JNIEnv *env, jobject stream)
{
    if (AudioStream_getByteStream == 0)
    {
        getByteStreamMethod(env, stream);
        if (AudioStream_getByteStream == 0)
        {
            return 0;
        }
    }
    jbyteArray returnValue = (jbyteArray) (*env)->CallObjectMethod(env, stream, AudioStream_getByteStream);
    return returnValue;
}

/**
 * Reads a single integer from the audio stream.
 * @param env - A reference to the JVM.
 * @param stream - The audio stream to obtain the header from.
 * @return The integer read.
 */
int readInt(JNIEnv *env, jobject stream)
{
    if (AudioStream_readInt == 0)
    {
        getReadMethod(env, stream);
        if (AudioStream_readInt == 0)
        {
            return 0;
        }
    }
    jint returnValue = (*env)->CallIntMethod(env, stream, AudioStream_readInt);
    return (int) returnValue;
}

/**
 * Fills a provided buffer with input from the audio stream.
 * @param env - A reference to the JVM.
 * @param stream - The audio stream to obtain the header from.
 * @param buffer - The buffer to read information to
 * @param bufferSize - The size of the buffer
 * @return How much information was actually read.
 */
int readBuffer(JNIEnv *env, jobject stream, char* buffer, int bufferSize)
{
    if (InputStream_readBuffer == 0)
    {
        getReadMethod(env, stream);
        if (InputStream_readBuffer == 0)
        {
            fprintf(stderr, "readBuffer method not found\n");
            return 0;
        }
        if (AudioStream_readInt == 0)
        {
            fprintf(stderr, "readInt method not found\n");
            return 0;
        }
    }

    jbyteArray jbuffer = (*env)->NewByteArray(env, bufferSize);
    jint returnValue = (*env)->CallIntMethod(env, stream, InputStream_readBuffer, jbuffer);
    jbyte *jbufferEl = (*env)->GetByteArrayElements(env, jbuffer, 0);
    for (int i = 0; i < returnValue; i++)
    {
        buffer[i] = jbufferEl[i];
    }
    (*env)->ReleaseByteArrayElements(env, jbuffer, jbufferEl, 0);
    return returnValue;
}

/**
 * Converts a WavHeader into something more easily accessible by native code.
 *
 * @param env - The JVM.
 * @param header - The header to read.
 * @param headerStruct - The header to write the data to.
 */
HeaderData readHeader(JNIEnv *env, jobject header, HeaderData *headerStruct)
{
    if (Header == 0)
    {
        getHeaderMethods(env);
    }
    headerStruct->numChannels = (short) (*env)->CallShortMethod(env, header, Header_getNumChannels);
    headerStruct->bytesPerChannel = (short) (*env)->CallShortMethod(env, header, Header_getBitsPerSample);
    headerStruct->sampleRate = (int) (*env)->CallIntMethod(env, header, Header_getSampleRate);
}

/**
 * Sets the file currently being used
 */
JNIEXPORT jint JNICALL Java_edu_regis_universeplayer_localPlayer_Player_setCurrentFile(JNIEnv *env, jobject obj, jobject stream, jshort numChannels, jshort bitsPerSample, jint sampleRate)
{
    jobject header;
    if (currentFile != NULL)
    {
        (*env)->DeleteLocalRef(env, currentFile);
    }
    currentFile = (*env)->NewGlobalRef(env, stream);
    header = getHeader(env, stream);
    readHeader(env, header, &currentHeader);

    return updatePlayer(env, obj, stream, numChannels, bitsPerSample, sampleRate);
}

JNIEXPORT void JNICALL Java_edu_regis_universeplayer_localPlayer_Player_save(JNIEnv *env, jobject obj, jstring location)
{
    const int BUFFER_SIZE = 256;
	/*
	 * Windows doesn't seem to like using even a constant for determining array
	 * size.
	 */
    char buffer[256];
    int read;

    FILE *output;

    char buf[128];
    const char *locationPath = (*env)->GetStringUTFChars(env, location, 0);
    output = fopen(locationPath, "w");

    if (output == NULL)
    {
        fprintf(stderr, "File %s can't be opened\n", locationPath);
        return;
    }

    // Obtains header data
    jbyteArray headerData = getByteStream(env, currentFile);
    jsize headerLength = (*env)->GetArrayLength(env, headerData);
    jbyte* header = (*env)->GetByteArrayElements(env, headerData, 0);
    char headerBuff[256];
    for (int i = 0; i < headerLength; i++)
    {
        headerBuff[i] = header[i];
    }
    fwrite(headerBuff, headerLength, 1, output);
    (*env)->ReleaseByteArrayElements(env, headerData, header, 0);

    // Writes the actual file
    read = readBuffer(env, currentFile, headerBuff, BUFFER_SIZE);
    while (read > 0)
    {
        fwrite(headerBuff, read, 1, output);
        read = readBuffer(env, currentFile, headerBuff, BUFFER_SIZE);
    }

    fclose(output);
    (*env)->ReleaseStringUTFChars(env, location, locationPath);

    return;
}

JNIEXPORT void JNICALL Java_edu_regis_universeplayer_localPlayer_Player_play(JNIEnv *env, jobject obj)
{
    printf("Playing\n");
    return;
}

JNIEXPORT void JNICALL Java_edu_regis_universeplayer_localPlayer_Player_pause(JNIEnv *env, jobject obj)
{
    printf("Pausing\n");
    return;
}

JNIEXPORT jboolean JNICALL Java_edu_regis_universeplayer_localPlayer_Player_isPaused(JNIEnv *env, jobject obj)
{
    printf("Is Paused?\n");
    return JNI_FALSE;
}