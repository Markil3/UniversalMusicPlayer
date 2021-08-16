/*
 * Copyright (c) 2021 William Hubbard. All Rights Reserved.
 */

package edu.regis.universeplayer.player;

import edu.regis.universeplayer.browser.InternetSong;
import edu.regis.universeplayer.data.InternetSongProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This dialog is used to add songs
 */
public class InternetSongDialog extends JDialog
{
    private static final Logger logger = LoggerFactory.getLogger(InternetSongDialog.class);
    
    private final JTextField urlBox;
    private final JTextField titleBox;
    private final JTextField albumBox;
    private final JTextField artistBox;
    private final JTextField genreBox;
    
    public InternetSongDialog(Frame owner)
    {
        super(owner, true);
    
        JLabel label;
        GridBagConstraints c = new GridBagConstraints();
        this.getContentPane().setLayout(new GridBagLayout());
        
        int y = 0;
        
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 2, 5, 2);
        
        this.getContentPane().add(label = new JLabel("URL"), c);
        this.urlBox = new JTextField();
        this.urlBox.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                try
                {
                    new URL(((JTextField) e.getSource()).getText());
                    ((JTextField) e.getSource()).setForeground(Color.BLACK);
                }
                catch (MalformedURLException ex)
                {
                    ((JTextField) e.getSource()).requestFocusInWindow();
                    ((JTextField) e.getSource()).setForeground(Color.RED);
                }
            }
        });
        c.gridy = ++y;
        this.getContentPane().add(this.urlBox, c);
        label.setLabelFor(this.urlBox);
    
        c.gridx = 0;
        c.gridy = ++y;
        this.getContentPane().add(label = new JLabel("Song Title"), c);
        this.titleBox = new JTextField();
        c.gridy = ++y;
        this.getContentPane().add(this.titleBox, c);
        label.setLabelFor(this.titleBox);
    
        c.gridx = 0;
        c.gridy = ++y;
        this.getContentPane().add(label = new JLabel("Album"), c);
        this.albumBox = new JTextField();
        c.gridy = ++y;
        this.getContentPane().add(this.albumBox, c);
        label.setLabelFor(this.albumBox);
    
        c.gridx = 0;
        c.gridy = ++y;
        this.getContentPane().add(label = new JLabel("Artists"), c);
        this.artistBox = new JTextField();
        c.gridy = ++y;
        this.getContentPane().add(this.artistBox, c);
        label.setLabelFor(this.artistBox);
    
        c.gridx = 0;
        c.gridy = ++y;
        this.getContentPane().add(label = new JLabel("Genre"), c);
        this.genreBox = new JTextField();
        c.gridy = ++y;
        this.getContentPane().add(this.genreBox, c);
        label.setLabelFor(this.genreBox);
        
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = ++y;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> this.dispose());
        this.getContentPane().add(cancelButton, c);
        c.gridx = 1;
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> this.confirm());
        this.getContentPane().add(addButton, c);
        
        this.pack();
    }
    
    private void confirm()
    {
        URL url;
        if (this.urlBox.getText().isEmpty())
        {
            this.urlBox.requestFocusInWindow();
            this.urlBox.setForeground(Color.RED);
            return;
        }
        try
        {
            url = new URL(this.urlBox.getText());
        }
        catch (MalformedURLException e)
        {
            this.urlBox.requestFocusInWindow();
            return;
        }
        
        for (Component comp: this.getContentPane().getComponents())
        {
            comp.setEnabled(false);
        }
    
        SwingUtilities.invokeLater(() -> {
            Future<InternetSong> future = InternetSongProvider.getInstance().addSong(url, this.titleBox.getText(), this.albumBox.getText(), this.artistBox.getText(), this.genreBox.getText());
            InternetSong song = null;
            try
            {
                song = future.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                logger.error("Could not evaluate song", e);
                JOptionPane.showMessageDialog(InternetSongDialog.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            finally
            {
                if (song != null)
                {
                    dispose();
                }
                else
                {
                    for (Component comp: this.getContentPane().getComponents())
                    {
                        comp.setEnabled(true);
                    }
                }
            }
        });
    }
}
