/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.data;

import edu.regis.universeplayer.PlaybackInfo;
import edu.regis.universeplayer.player.Player;

import java.util.EventObject;

public class PlaybackEvent extends EventObject
{
    private final PlaybackInfo info;
    
    public PlaybackEvent(Player<? extends Song> player, PlaybackInfo playbackInfo)
    {
        super(player);
        this.info = playbackInfo;
    }
    
    public PlaybackInfo getInfo()
    {
        return info;
    }
}
