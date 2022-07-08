package com.flleeppyy.serverinstaller;

import com.flleeppyy.serverinstaller.Guis.InstallModpack;
import com.flleeppyy.serverinstaller.Guis.UpdateModpack;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class MainGui {
    // Genuinely
    private static final Gson g = new GsonBuilder().setPrettyPrinting().create();

    static JFrame startMenuFrame;

    // Launches the GUI and gets stuff done and yeah
    public static void Launch() {
        startMenu();
    }

    private static void test() {
        JFrame penis = new JFrame("penis penis");

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(4,4));

        JButton PENIS = new JButton("PENISPENISPENISPENISPENISPENISPENISPENIS");
        JButton PENIS2 = new JButton("PENISPENISPENISPENISPENISPENISPENISPENIS");
        JButton PENISTOP = new JButton("PENISPENISPENISPENISPENISPENISPENISPENISPENISPENISPENISPENISPENISPENIS");

        contentPanel.add(PENIS, BorderLayout.WEST);
        contentPanel.add(PENIS2, BorderLayout.EAST);
        contentPanel.add(PENISTOP, BorderLayout.NORTH);

        penis.add(contentPanel);
        penis.pack();
        penis.setVisible(true);
    }

    private static void startMenu() {
        startMenuFrame = new JFrame("Server Installer");

        Dimension size = new Dimension(240,150);
        Dimension buttonSize = new Dimension(130,50);

//        startMenuFrame.setSize(size);
//        startMenuFrame.setMaximumSize(size);
//        startMenuFrame.setMinimumSize(size);
        startMenuFrame.setResizable(false);
        startMenuFrame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,5,10,5));

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ee) {
                System.err.println("something has gone horribly wrong, how did you do this?");
                ee.printStackTrace();
                System.exit(-2);
            }
        }

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        /** Begin top section */
        JPanel topSection = new JPanel();
        topSection.setLayout(new GridLayout(2,1,4,0));
        topSection.setMaximumSize(new Dimension(120,36));

        JLabel title = new JLabel("Welcome!", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(20.0f));
        JLabel description = new JLabel("Please pick an option", SwingConstants.CENTER);

        topSection.add(title);
        topSection.add(description);
        /** End top section */

        /** Begin Button section */
        JButton installModpackButton = new JButton("<html><center>Install a new modpack server</center></html>");
        installModpackButton.setPreferredSize(buttonSize);
        installModpackButton.setMargin(new Insets(0,0,0,0));
        installModpackButton.addActionListener(
            e -> {
                startMenuFrame.dispose();
                try {
                    InstallModpack.InstallNewPack();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        );
        JButton updateModpackButton = new JButton("<html><center>Update modpack server</center></html>");
        updateModpackButton.setPreferredSize(buttonSize);
        installModpackButton.setMargin(new Insets(0,0,0,0));

        buttonsPanel.add(installModpackButton);
        buttonsPanel.add(updateModpackButton);

        /** End Button section */

        mainPanel.add(topSection, BorderLayout.NORTH);
        mainPanel.add(buttonsPanel, BorderLayout.CENTER);

        startMenuFrame.add(mainPanel);

        startMenuFrame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        startMenuFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        startMenuFrame.pack();
        startMenuFrame.setLocationRelativeTo(null);

        startMenuFrame.setVisible(true);
    }

    static void exit() {
        startMenuFrame.dispose();
        System.exit(0);
    }



}
