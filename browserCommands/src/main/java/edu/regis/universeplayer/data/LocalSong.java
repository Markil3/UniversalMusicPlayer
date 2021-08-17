/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import java.io.File;

/**
 * This song represents a song found on the local file system.
 */
public class LocalSong extends Song
{
    public File file;
    public String type;
    public String codec;
}
