#include "interface.h"

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