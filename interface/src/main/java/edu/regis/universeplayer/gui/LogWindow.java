package edu.regis.universeplayer.gui;

import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import edu.regis.universeplayer.ListAppender;

public class LogWindow extends JFrame
{
    private static final Logger logger = LoggerFactory.getLogger(LogWindow.class);
    private static final ResourceBundle langs = ResourceBundle
            .getBundle("lang.interface", Locale.getDefault());

    private final HashMap<String, JComponent> filters = new HashMap<>();
    private final Box filterBox;
    private final Box logBox;
    private final JScrollPane centerView;
    private final JMenu loggers;
    private int lastLogRecord = 0;
    private Thread logUpdaterThread;
    private boolean active = true;

    public LogWindow()
    {
        super();

        this.setTitle(langs.getString("logs.title"));

        FlowLayout layout = new FlowLayout();
        JPanel header = new JPanel();
        header.setLayout(layout);

        this.filterBox = Box.createHorizontalBox();
        header.add(filterBox);

        JPopupMenu filterMenu = new JPopupMenu("Filters");
        JButton filterButton = new JButton("\u25BC");
        filterButton.addMouseListener(new MouseAdapter()
        {
            /**
             * {@inheritDoc}
             *
             * @param e
             */
            @Override
            public void mouseClicked(MouseEvent e)
            {
                filterMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        header.add(filterButton);

        JMenu levels = new JMenu(langs.getString("logs.levels"));
        filterMenu.add(levels);
        JMenuItem levelTrace = new JMenuItem(langs.getString("logs.levels.trace"));
        levelTrace.addActionListener(e -> addFilter("levels.trace"));
        levels.add(levelTrace);
        JMenuItem levelDebug = new JMenuItem(langs.getString("logs.levels.debug"));
        levelDebug.addActionListener(e -> addFilter("levels.debug"));
        levels.add(levelDebug);
        JMenuItem levelInfo = new JMenuItem(langs.getString("logs.levels.info"));
        levelInfo.addActionListener(e -> addFilter("levels.info"));
        levels.add(levelInfo);
        JMenuItem levelWarning = new JMenuItem(langs.getString("logs.levels.warning"));
        levelWarning.addActionListener(e -> addFilter("levels.warning"));
        levels.add(levelWarning);
        JMenuItem levelError = new JMenuItem(langs.getString("logs.levels.error"));
        levelError.addActionListener(e -> addFilter("levels.error"));
        levels.add(levelError);

        this.loggers = new JMenu(langs.getString("logs.loggers"));
        filterMenu.add(this.loggers);

        this.getContentPane().add(header, BorderLayout.PAGE_START);

        this.logBox = Box.createVerticalBox();

        this.centerView = new JScrollPane(this.logBox);
        this.getContentPane().add(centerView, BorderLayout.CENTER);

        this.addWindowListener(new WindowAdapter()
        {
            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             *
             * @param e
             */
            @Override
            public void windowClosing(WindowEvent e)
            {
                active = false;
            }
        });

        this.logUpdaterThread = new Thread(() -> {
            while (active)
            {
                if (ListAppender.getLogEvents().size() > this.lastLogRecord)
                {
                    this.lastLogRecord = ListAppender.getLogEvents().size();
                    SwingUtilities.invokeLater(() -> this.resetLogs());
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        logger.error("Could not sleep", e);
                    }
                }
            }
            /*
             * Stop
             */
        });
        this.logUpdaterThread.start();
    }

    private void addFilter(String filter)
    {
        if (!this.filters.containsKey(filter))
        {
            Box filterBox = Box.createHorizontalBox();
            if (langs.containsKey("logs." + filter))
            {
                filterBox.add(new JLabel(langs.getString("logs." + filter)));
            }
            else
            {
                filterBox.add(new JLabel(filter.substring(filter.indexOf('.') + 1)));
            }

            filterBox.add(new JButton(new AbstractAction("X")
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    removeFilter(filter);
                }
            }));
            this.filters.put(filter, filterBox);
            this.filterBox.add(filterBox);
            this.resetLogs();
            this.filterBox.revalidate();
            this.filterBox.repaint();
        }
    }

    private void removeFilter(String filter)
    {
        JComponent component = this.filters.remove(filter);
        if (component != null)
        {
            component.getParent().remove(component);
            this.resetLogs();
            this.filterBox.revalidate();
            this.filterBox.repaint();
        }
    }

    private synchronized void resetLogs()
    {
        JScrollBar scroll = this.centerView.getVerticalScrollBar();
        boolean bottom = scroll.getModel().getValue() >= scroll.getModel().getMaximum() - scroll
                .getModel().getExtent();
        this.logBox.removeAll();
        this.loggers.removeAll();
        ListAppender.getLogEvents().stream().map(LogEvent::getLoggerName).distinct().forEach(name -> {
            JMenuItem loggerItem = new JMenuItem(name);
            loggerItem.addActionListener(e -> addFilter("loggers." + name));
            this.loggers.add(loggerItem);
        });
        ListAppender.getLogEvents().stream().filter(event -> {
            Set<String> filters = this.filters.keySet();
            if (filters.stream().anyMatch(filter -> filter.startsWith("levels")))
            {
                switch (event.getLevel().name())
                {
                case "TRACE":
                    if (!filters.stream().anyMatch(f -> f.equals("levels.trace")))
                    {
                        return false;
                    }
                    break;
                case "DEBUG":
                    if (!filters.stream().anyMatch(f -> f.equals("levels.debug")))
                    {
                        return false;
                    }
                    break;
                case "WARN":
                    if (!filters.stream().anyMatch(f -> f.equals("levels.warning")))
                    {
                        return false;
                    }
                    break;
                case "ERROR":
                case "FATAL":
                    if (!filters.stream().anyMatch(f -> f.equals("levels.error")))
                    {
                        return false;
                    }
                    break;
                default:
                    if (!filters.stream().anyMatch(f -> f.equals("levels.info")))
                    {
                        return false;
                    }
                    break;
                }
            }
            if (filters.stream().anyMatch(f -> f.startsWith("loggers")))
            {
                String loggerName = "loggers." + event.getLoggerName();
                if (!filters.stream().anyMatch(f -> f.equals(loggerName)))
                {
                    return false;
                }
            }
            return true;
        }).sorted(Comparator.comparingLong(LogEvent::getNanoTime)).forEach(event -> {
            Formatter form = new Formatter();
            JLabel label = new JLabel(form
                    .format("%tT - %s - %s", event.getNanoTime(), event.getLoggerName(), event
                            .getMessage().getFormattedMessage()).toString());
            form = new Formatter();
            label.setToolTipText(form.format("(%s#%s:%d)", Optional
                    .ofNullable(event.getSource()).map(StackTraceElement::getClassName)
                    .orElse(""), Optional
                    .ofNullable(event.getSource()).map(StackTraceElement::getMethodName)
                    .orElse(""), Optional
                    .ofNullable(event.getSource()).map(StackTraceElement::getLineNumber).orElse(-1))
                                     .toString());
            switch (event.getLevel().name())
            {
            case "TRACE":
                label.setForeground(Color.GRAY);
                break;
            case "DEBUG":
                label.setForeground(Color.BLUE);
                break;
            case "WARN":
                label.setForeground(Color.ORANGE);
                break;
            case "ERROR":
            case "FATAL":
                label.setForeground(Color.RED);
                break;
            default:
                label.setForeground(Color.BLACK);
                break;
            }
            logBox.add(label);
        });
        this.logBox.revalidate();
        this.logBox.repaint();
        if (bottom)
        {
            SwingUtilities.invokeLater(() -> {
                scroll.setValue(scroll.getMaximum());
            });
        }
    }

    public static void main(String[] args)
    {
        LogWindow window = new LogWindow();
        window.pack();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}
