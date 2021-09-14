package edu.regis.universeplayer.browserCommands;

/**
 * A debugging command that instructs the browser to throw an error.
 */
public class CommandError implements BrowserCommand
{
    private boolean forward;

    /**
     * Creates an error command.
     *
     * @param forward - Determines where the error should be thrown. If true, it
     *                will be thrown from the website tab. If false, it will be
     *                thrown by the addon background script.
     */
    public CommandError(boolean forward)
    {
        this.forward = forward;
    }

    /**
     * Checks whether the error will be thrown from the foreground or background
     * script.
     *
     * @return True if the error will be thrown from the foreground script,
     * false if it will be thrown from the background.
     */
    public boolean isForeground()
    {
        return this.forward;
    }

    /**
     * Obtains the name of the command.
     *
     * @return The command name.
     */
    @Override
    public String getCommandName()
    {
        return "error";
    }
}
