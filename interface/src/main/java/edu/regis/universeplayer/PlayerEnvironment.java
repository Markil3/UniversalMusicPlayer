package edu.regis.universeplayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;

import edu.regis.universeplayer.browserCommands.QueryFuture;
import edu.regis.universeplayer.data.Queue;
import edu.regis.universeplayer.data.SongProvider;
import edu.regis.universeplayer.gui.Interface;
import edu.regis.universeplayer.player.PlayerManager;

/**
 * A centralized spot to link up all of the components.
 *
 * @author William Hubbard
 * @version 0.1
 */
public class PlayerEnvironment
{
    private static final Logger logger = LoggerFactory
            .getLogger(PlayerEnvironment.class);
    private static final ResourceBundle langs = ResourceBundle
            .getBundle("lang.interface", Locale.getDefault());

    /**
     * Initializes the various components, setting up listeners as needed.
     */
    public static void main(String[] args)
    {
        /*
         * Add this just in case of a crash or something. It won't work if the
         * program is forcibly terminated by the OS, but it could be helpful
         * otherwise.
         */
        logger.info("Starting application");

        Queue queue = Queue.getInstance();
        PlayerManager playback = PlayerManager.getPlayers();
        queue.addSongChangeListener(queue1 -> {
            QueryFuture<Void> command = PlayerManager.getPlayers()
                                                     .stopSong();
            try
            {
                if (command != null && !command.getConfirmation()
                                               .wasSuccessful())
                {
                    if (Interface.getInstance() != null)
                    {
                        JOptionPane
                                .showMessageDialog(Interface.getInstance(),
                                        command.getConfirmation()
                                               .getError(),
                                        command.getConfirmation()
                                               .getMessage(),
                                        JOptionPane.ERROR_MESSAGE);
                    }
                    logger.error("Could not run command",
                            command.getConfirmation().getError());
                }
                else
                {
                    command =
                            PlayerManager.getPlayers()
                                         .playSong(queue1.getCurrentSong());
                    if (!command.getConfirmation().wasSuccessful())
                    {
                        if (Interface.getInstance() != null)
                        {
                            JOptionPane.showMessageDialog(Interface
                                            .getInstance(),
                                    command.getConfirmation().getError(),
                                    command.getConfirmation().getMessage(),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                        logger.error("Could not run command",
                                command.getConfirmation().getError());
                    }
                    else
                    {
                    }
                }
            }
            catch (ExecutionException | InterruptedException e)
            {
                logger.error("Could not get current playback status", e);
                if (Interface.getInstance() != null)
                {
                    JOptionPane
                            .showMessageDialog(Interface.getInstance(), e
                                            .getMessage()
                                    , langs
                                            .getString(
                                                    "error.command"), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        playback.addPlaybackListener(status -> {
            switch (status.getInfo().getStatus())
            {
            case FINISHED -> Queue.getInstance().skipNext();
            }
        });

        Interface inter = new Interface();
        inter.setSize(700, 500);
        SongProvider.INSTANCE.addUpdateListener(inter);
        inter.setVisible(true);
    }
}
