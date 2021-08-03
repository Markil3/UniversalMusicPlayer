/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import com.wordpress.tips4java.ScrollablePanel;
import edu.regis.universeplayer.ClickListener;
import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Formatter;

public class QueueList extends JPanel implements Queue.SongChangeListener, Queue.QueueChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(QueueList.class);
    
    private final JScrollPane scroll;
    final JPanel header;
    final ScrollablePanel songList;
    private int currentHighlight = 0;
    
    private JButton clearButton;
    
    public QueueList()
    {
        GridBagConstraints c = new GridBagConstraints();
        
        this.setLayout(new BorderLayout());
        this.setFocusable(true);
//        this.setFocusCycleRoot(true);
        
        header = new JPanel(new GridBagLayout());
//        header.setFocusCycleRoot(true);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        header.add(new JLabel(" ".repeat(20) + "Queue" + " ".repeat(20)), c);
        this.clearButton = new JButton("Clear");
        this.clearButton.addActionListener(e -> {
            Queue.getInstance().clear();
        });
        c.gridy = 1;
        c.gridwidth = 1;
        header.add(clearButton, c);
        header.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                int index = -1;
                Component[] children = ((Container) e.getComponent()).getComponents();
                for (int i = 0, l = children.length; index == -1 && i < l; i++)
                {
                    if (children[i] == e.getOppositeComponent())
                    {
                        index = i;
                    }
                }
                /*
                 * Only auto-switch focus if the previous component was not from
                 * within.
                 */
                if (index == -1)
                {
                    clearButton.requestFocusInWindow();
                }
            }
        });
        this.add(header, BorderLayout.NORTH);
        
        this.songList = new ScrollablePanel(new GridLayout(0, 2));
        /*
         * Don't focus on here until we have elements.
         */
        this.songList.setFocusable(false);
        this.songList.setFocusCycleRoot(true);
        this.songList.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                int index = -1;
                Component[] children = ((Container) e.getComponent()).getComponents();
                for (int i = 0, l = children.length; index == -1 && i < l; i++)
                {
                    if (children[i] == e.getOppositeComponent())
                    {
                        index = i;
                    }
                }
                /*
                 * Only auto-switch focus if the previous component was not from
                 * within.
                 */
                if (index == -1)
                {
                    songList.getComponents()[0].requestFocusInWindow();
                }
                else
                {
                    header.requestFocusInWindow();
                }
            }
        });
        this.songList.setScrollableWidth(ScrollablePanel.ScrollableSizeHint.FIT);
        
        this.scroll = new JScrollPane(this.songList);
//        this.scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
//        this.scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(this.scroll, BorderLayout.CENTER);
        
        this.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                int index = -1;
                Component[] children = ((Container) e.getComponent()).getComponents();
                for (int i = 0, l = children.length; index == -1 && i < l; i++)
                {
                    if (children[i] == e.getOppositeComponent())
                    {
                        index = i;
                    }
                }
                /*
                 * Only auto-switch focus if the previous component was not from
                 * within.
                 */
                if (index == -1)
                {
                    header.requestFocusInWindow();
                }
            }
        });
    }
    
    @Override
    public void onSongChange(Queue queue)
    {
        if (this.currentHighlight >= 0)
        {
            this.songList.getComponent(this.currentHighlight * 2).setForeground(Color.BLACK);
        }
        this.currentHighlight = queue.getCurrentIndex();
        if (this.currentHighlight >= 0)
        {
            this.songList.getComponent(this.currentHighlight * 2).setForeground(Color.BLUE);
        }
    }
    
    @Override
    public void onQueueChange(Queue queue)
    {
        Formatter dateForm;
        JButton songLabel;
        JLabel durationLabel;
        GridBagConstraints c;
        this.songList.removeAll();
        int i = 0;
        for (Song song : queue)
        {
            songLabel = new JButton(song.title);
            songLabel.setFocusPainted(true);
            songLabel.setMargin(new Insets(0, 0, 0, 0));
            songLabel.setContentAreaFilled(false);
            songLabel.setBorderPainted(false);
            songLabel.setOpaque(false);
            songLabel.setHorizontalAlignment(JLabel.LEFT);
            int songIndex = i;
            songLabel.addMouseListener((ClickListener) e -> {
                if (e.getClickCount() == 2)
                {
                    queue.skipToSong(songIndex);
                }
            });
            dateForm = new Formatter();
            durationLabel = new JLabel(dateForm.format("%1$tM:%1$tS", song.duration).toString());
            durationLabel.setHorizontalAlignment(JLabel.RIGHT);
            durationLabel.addMouseListener((ClickListener) e -> {
                if (e.getClickCount() == 2)
                {
                    queue.skipToSong(songIndex);
                }
            });
            dateForm.close();
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = i;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 0, 5);
            c.fill = GridBagConstraints.HORIZONTAL;
            this.songList.add(songLabel);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = i;
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(0, 0, 0, 0);
            this.songList.add(durationLabel);
            
            songLabel.setForeground(Color.BLACK);
            i++;
        }
        this.songList.setFocusable(queue.size() > 0);
        this.currentHighlight = queue.getCurrentIndex();
        if (this.currentHighlight >= 0)
        {
            this.songList.getComponent(queue.getCurrentIndex() * 2).setForeground(Color.BLUE);
        }
        this.validate();
    }
}
