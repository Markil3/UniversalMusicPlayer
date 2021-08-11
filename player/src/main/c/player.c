/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */
#include "interface.h"
#include "player.h"

play_item_t play_list_head = {
    0,         /* play_id */
    SA_CLEAR,  /* stop_flag */
    NULL,      /* prev_item */
    NULL,      /* next_item */
    NULL       /* mutex */
};

static jobject currentFile = NULL;
static HeaderData currentHeader;

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
JNIEXPORT jint JNICALL Java_edu_regis_universeplayer_localPlayer_LocalPlayer_setCurrentFile(JNIEnv *env, jobject obj, jobject stream, jshort numChannels, jshort bitsPerSample, jint sampleRate)
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

JNIEXPORT void JNICALL Java_edu_regis_universeplayer_localPlayer_LocalPlayer_save(JNIEnv *env, jobject obj, jstring location)
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

JNIEXPORT void JNICALL Java_edu_regis_universeplayer_localPlayer_LocalPlayer_playSong(JNIEnv *env, jobject obj)
{
    printf("Playing\n");
    return;
}

JNIEXPORT void JNICALL Java_edu_regis_universeplayer_localPlayer_LocalPlayer_pauseSong(JNIEnv *env, jobject obj)
{
    printf("Pausing\n");
    return;
}

JNIEXPORT jboolean JNICALL Java_edu_regis_universeplayer_localPlayer_LocalPlayer_isSongPaused(JNIEnv *env, jobject obj)
{
    printf("Is Paused?\n");
    return JNI_FALSE;
}