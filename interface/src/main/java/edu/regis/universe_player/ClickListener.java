/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universe_player;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A utility class designed to shortcut adding click listeners to AWT objects.
 *
 * @author William Hubbard
 * @version 0.1
 */
public interface ClickListener extends MouseListener
{
    default void mousePressed(MouseEvent var1)
    {
    }

    default void mouseReleased(MouseEvent var1)
    {
    }

    default void mouseEntered(MouseEvent var1)
    {
    }

    default void mouseExited(MouseEvent var1)
    {
    }
}
