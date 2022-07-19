package com.flleeppyy.serverinstaller.Guis;

import com.flleeppyy.serverinstaller.JComponents.ModpackList;
import com.flleeppyy.serverinstaller.Json.ModpackInfo;
import com.flleeppyy.serverinstaller.MainGui;
import com.flleeppyy.serverinstaller.ModpackApi;
import com.flleeppyy.serverinstaller.ServerInstaller;
import com.flleeppyy.serverinstaller.Utils.ModpackProcessing;
import com.flleeppyy.serverinstaller.Utils.ImageUtils;
import org.apache.hc.client5.http.fluent.Request;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.prefs.Preferences;

public class InstallModpack {
    static JFrame mainFrame;
    static Path serverFolder;
    static JPanel mainPanel;

    static ModpackInfo selectedPack;

    static JPanel topSection;
    static JPanel browseBar;
    static JTextField pathField;

    static JPanel centerSection;
    static ModpackList modpackList;
    static JTextPane selectedModpackDescription;

    static String descriptionTemplate;

    static ModpackServerInstaller modpackServerInstaller;

    static {
        try {
            descriptionTemplate = new String(
                    Files.readAllBytes(
                        Paths.get(
                            Objects.requireNonNull(ServerInstaller.class.getResource("/descriptionTemplate.html"))
                                    .toURI()
                        )
                    )
                );
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    static JPanel bottomSection;
    static JPanel buttonsBox;
    static JButton refreshButton;
    static JComboBox<String> versionSelection;
    static JButton okButton;
    static JButton cancelButton;



    static ModpackInfo[] modpacks;

    public static void InstallNewPack() throws IOException {
        mainFrame = new JFrame("Install a new modpack server");
        mainFrame.setResizable(false);
        mainFrame.setLayout(new BorderLayout());

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        /** Begin top section **/
        {
            topSection = new JPanel(new BorderLayout(0,0));

            String browseLabelText = "Select an EMPTY folder to install the server to";
            JLabel serverLocationLabel = new JLabel(browseLabelText);

            browseBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));

            JButton browseButton = new JButton("Browse...");
            pathField = new JTextField(90);
            pathField.setEditable(false);

            pathField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        browseServerFolder();
                    }
                }
            });

            browseButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    browseServerFolder();
                }
            });

            topSection.add(serverLocationLabel, BorderLayout.NORTH);
            browseBar.add(browseButton);
            browseBar.add(Box.createHorizontalStrut(4));
            browseBar.add(pathField);

            topSection.add(browseBar, BorderLayout.CENTER);

        }
        /** End top section **/


        /** Begin Center section **/
        {
            centerSection = new JPanel(new BorderLayout(4,4));
            centerSection.setBorder(BorderFactory.createEmptyBorder(4,0,4,0));

            // Main dimensions of the boxes. These will make up most of the width of the window
            Dimension preferredSize = new Dimension(400, 520);

            modpackList = new ModpackList();
            modpackList.setPreferredSize(preferredSize);

            selectedModpackDescription = new JTextPane();
            selectedModpackDescription.setText("Please select a modpack from the left.");
            selectedModpackDescription.setEditable(false);
//            selectedModpackDescription.setPreferredSize(preferredSize);
            selectedModpackDescription.setPreferredSize(preferredSize);
            selectedModpackDescription.setEditorKit(new HTMLEditorKit() {

                final ViewFactory viewFactory = new HTMLFactory() {

                    @Override
                    public View create(Element elem) {
                        AttributeSet attrs = elem.getAttributes();
                        Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
                        Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
                        if (o instanceof HTML.Tag) {
                            HTML.Tag kind = (HTML.Tag) o;
                            if (kind == HTML.Tag.IMPLIED) {
                                return new javax.swing.text.html.ParagraphView(elem);
                            }
                        }
                        return super.create(elem);
                    }
                };

                @Override
                public ViewFactory getViewFactory() {
                    return this.viewFactory;
                }
            });
            selectedModpackDescription.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    System.out.println(e.getSourceElement());
                    if (e.getURL() != null)
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            try {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } catch (IOException | URISyntaxException ex) {
                                ex.printStackTrace();
                            }
                        }
                }
            });

            selectedModpackDescription.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            selectedModpackDescription.setFont(pathField.getFont());
            selectedModpackDescription.setContentType("text/html; charset=utf8");

            modpackList.addListSelectionListener(e -> {
                selectedPack = (ModpackInfo) modpackList.getSelectedValue();
                if (selectedPack != null) {
                    String packText = descriptionTemplate.replace("%body%", ModpackProcessing.descriptionGenerator(selectedPack))
                            .replace("%style%", "")
                            .replace("%head%","")
                            .replace("%width%",(preferredSize.width - 100) + "");
                    selectedModpackDescription.setText(
                            packText
                    );
                }
            });
            JScrollPane selectedModpackDescriptionScroll = new JScrollPane(selectedModpackDescription);
            selectedModpackDescriptionScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            selectedModpackDescriptionScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            selectedModpackDescriptionScroll.setPreferredSize(preferredSize);

            centerSection.add(modpackList, BorderLayout.WEST);
//            centerSection.add(selectedModpackDescription, BorderLayout.EAST);
            centerSection.add(selectedModpackDescriptionScroll, BorderLayout.EAST);
        }
        /** End Center section **/


        /** Begin Bottom section **/
        {
            bottomSection = new JPanel(new BorderLayout());
            bottomSection.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

            buttonsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2,0));
            Dimension size = new Dimension(70,21);

            versionSelection = new JComboBox<>();
            System.out.println(versionSelection.getHeight());
            versionSelection.setPreferredSize(new Dimension(80, versionSelection.getHeight()));
            versionSelection.setEnabled(false);
//            versionSelection.setVisible(true);

            okButton = new JButton("OK");
            okButton.setPreferredSize(size);
            cancelButton = new JButton("Cancel");
            cancelButton.setPreferredSize(size);

            cancelButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
//                    super.mouseClicked(e);
                    returnToMainMenu();
                }
            });

            okButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
//                    super.mouseClicked(e);
                    String validFolder = validateServerFolder();
                    if (validFolder != null) {
                        JOptionPane.showMessageDialog(null, validFolder, "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    modpackServerInstaller = new ModpackServerInstaller(serverFolder);
                    try {
                        modpackServerInstaller.installModpack(selectedPack,"1.0.0-alpha.5",true,true);
                    } catch (IOException ex) {
                        ex.printStackTrace();

                    }
                }
            });

            buttonsBox.add(versionSelection);
            buttonsBox.add(okButton);
            buttonsBox.add(cancelButton);

            // refresh button
            refreshButton = new JButton("Refresh");
            refreshButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    refreshModpackList();
                    resetModpackSelection();
                }
            });

            bottomSection.add(refreshButton, BorderLayout.WEST);

            bottomSection.add(buttonsBox, BorderLayout.EAST);
        }
        /** End Bottom section **/

        // Add the shit to the main panel

        mainPanel.add(topSection, BorderLayout.NORTH);
        mainPanel.add(centerSection, BorderLayout.CENTER);
        mainPanel.add(bottomSection, BorderLayout.SOUTH);

        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                returnToMainMenu();
            }
        });
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                returnToMainMenu();
            }
        });


        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.pack();

        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        resetModpackSelection();
        refreshModpackList();
    }

    static boolean isRefreshing = false;
    static Thread refreshThread;
     static synchronized void refreshModpackList() {
        refreshThread = new Thread(() -> {
            selectedModpackDescription.setText("Loading...");
            if (isRefreshing) {
                return;
            }
            isRefreshing = true;
            refreshButton.setEnabled(false);

            try {
                modpacks = ModpackApi.getModpacks();
                int iconFails = 0;
                for (ModpackInfo modpack : modpacks) {
                    try {
                        byte[] rawIcon = Request.get(modpack.icon).execute().returnContent().asBytes();
                        byte[] newIcon = ImageUtils.resizeToFitSquareDim(rawIcon, 64);
                        modpack.setIconBytes(newIcon);
                    } catch (IOException e) {
                        iconFails++;
                    }
                }
                if (iconFails > 0) {
                    JOptionPane.showMessageDialog(null, "Failed to load icons for " + iconFails + " packs", "Warning", JOptionPane.WARNING_MESSAGE);
                }
                modpackList.setListData(modpacks);
                modpackList.repaint();
                resetModpackSelection();
                updateVersionBox();
            } catch (NullPointerException e) {
                JOptionPane.showMessageDialog(null, "The program has encountered an error on the level of which\nwe cannot fix/figure out how to fix. This is a common occurrence\nThe installer will now exit.", "Fatal error", JOptionPane.ERROR_MESSAGE);
                System.exit(42069);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Failed to load packs.\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            isRefreshing = false;
            refreshButton.setEnabled(true);
        });

        refreshThread.start();
    }

    static boolean isResetting = false;
    static synchronized void resetModpackSelection() {
        if (isResetting)
            return;
        isResetting = true;
        modpackList.clearSelection();
        selectedModpackDescription.setText("Please select a modpack from the left");
        isResetting = false;
    }

    static String validateServerFolder() {
        if (serverFolder.toString().length() < 1)
            return "Not valid path";

        if (!Files.isDirectory(serverFolder))
            return "Folder is not a directory";


        if (!Files.isWritable(serverFolder))
            return "Folder is not writable";

        return null;
    }

    static boolean isFolderEmpty() {
        try {
            if (Files.list(serverFolder).findAny().isPresent()) {
                System.out.println("Folder is not empty\n");
                return false;
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error checking folder contents\n");
           JOptionPane.showMessageDialog(null, "Error checking folder contents\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    static void updateVersionBox() {
        if (selectedPack != null) {
                versionSelection.removeAllItems();
            for (String version : selectedPack.versions) {
                versionSelection.addItem(version);
            }
        }
    }

    private static boolean shiftDown = false;
    static void browseServerFolder() {
        JFileChooser filePicker = new JFileChooser(Preferences.userRoot().get("lastBrowsedFolder", ""));
        filePicker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // doesnt FUCKING WORK
//        filePicker.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyPressed(KeyEvent e) {
//                super.keyPressed(e);
//                if (e.isShiftDown()) {
//                    shiftDown = true;
//                }
//            }
//
//            @Override
//            public void keyReleased(KeyEvent e) {
//                super.keyReleased(e);
//                if (!e.isShiftDown()) {
//                    shiftDown = false;
//                }
//            }
//        });

        int e = filePicker.showOpenDialog(null);
        if (e == JFileChooser.APPROVE_OPTION) {
            setServerFolderPath(filePicker.getSelectedFile().toPath());
        }
    }

    static void setServerFolderPath(Path path) {
        serverFolder = path;
        if (!isFolderEmpty()) {
            JOptionPane.showMessageDialog(null, "The folder you selected is not empty.\nPlease be sure you want to install into this folder.\nThis is useful if you're recovering from a\nfailed server install, or predownloaded stuff.", "Error", JOptionPane.WARNING_MESSAGE);
        }
        pathField.setText(path.toString());
    }
    
    static void returnToMainMenu() {
        mainFrame.dispose();
        refreshThread.interrupt();
        MainGui.Launch();
    }
}