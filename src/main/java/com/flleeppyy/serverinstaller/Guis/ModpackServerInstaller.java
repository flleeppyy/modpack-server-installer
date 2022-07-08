package com.flleeppyy.serverinstaller.Guis;

import com.flleeppyy.serverinstaller.Adoptium;
import com.flleeppyy.serverinstaller.Json.ModpackInfo;
import com.flleeppyy.serverinstaller.Json.ModpackVersionSpec;
import com.flleeppyy.serverinstaller.ModpackApi;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.apache.hc.client5.http.fluent.Request;
import org.ini4j.Ini;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class ModpackServerInstaller {
    Path serverPath;
    ModpackInfo modpack;

    JFrame mainFrame;
    JLabel currentOperationLabel;
    JProgressBar progressBar;
    int setProgressCalls = 0;
    JButton abortButton;
    boolean gui = false;

    Path tempFolder;

    public ModpackServerInstaller(Path serverPath) {
        this.serverPath = serverPath;
    }

    public int installModpack(ModpackInfo modpackInfo, String version, boolean ignoreEmpty, boolean gui) throws IOException {
        try {
            if (gui) {
                initMainFrame();
                this.gui = true;
            }
            return _installModpack(modpackInfo,version,ignoreEmpty);
        } catch (IOException e) {
            e.printStackTrace();
            if (gui) {
                JOptionPane.showMessageDialog(null, "An error occurred installing the pack:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                killFrame();
            }
            return 1;
        }
    }
    private int _installModpack(ModpackInfo modpackInfo, String version, boolean ignoreEmpty) throws IOException {
        // Re-fetch modpack for various reasons
        setOperation("Initialization (1/4)","Fetching modpack", 1);

        modpack = ModpackApi.getModpack(modpackInfo.name);

        setOperation("Initialization (2/4)","Checking version", 1);
        {
            if (!Arrays.asList(modpack.versions).contains(version)) {
                StringBuilder yep = new StringBuilder();
                for (int i = 0; i < modpack.versions.length; i++) {
                    if (i == modpack.versions.length -1 ) {
                        yep.append(modpack.versions[i]);
                    } else {
                        yep.append(modpack.versions[i]).append(", ");
                    }
                }
                String error = "Invalid version. Available versions: " + yep;
                throw new IllegalArgumentException(error);
            }
        }

        setOperation("Initialization (3/4)", "Checking server path", 1);
        checkPath(ignoreEmpty);

        setOperation("Initialization (4/4)", "Getting version spec", 1);
        ModpackVersionSpec versionSpec = ModpackApi.getModpackVersionSpec(modpack.name, version);

        setOperation("Downloads (1/4)", "Making temp folder",1);
        generateTempFolder();
        File packFilePath = tempFolder.resolve(versionSpec.filename).toFile();

        setOperation("Downloads (2/4)", "Downloading modpack file",1);
        {
            if (Objects.equals(versionSpec.filetype, "zip")) {
                URL packURL = new URL(versionSpec.url);
                ReadableByteChannel readableByteChannel = Channels.newChannel(packURL.openStream());
                try (FileOutputStream fos = new FileOutputStream(packFilePath.toString())) {
                    fos.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                }

                if (modpack.server.java.javaArgs == null) {
                    ZipFile zipFile = new ZipFile(packFilePath);
                    FileHeader fileHeader = zipFile.getFileHeader("instance.cfg");
                    InputStream inputStream = zipFile.getInputStream(fileHeader);
                    String[] jvmArgs = new Ini(inputStream).get("JvmArgs", String.class).trim().split(" ");
                }

            }
        }

        setOperation("Downloads (3/4)", "Downloading Log4jPatcher",1);
        {
            Path log4jAgentPath = Files.createDirectory(serverPath.resolve("javaAgents"));
            InputStream log4jStream = Request.get("https://github.com/CreeperHost/Log4jPatcher/releases/download/v1.0.1/Log4jPatcher-1.0.1.jar")
                .execute()
                .returnContent()
                .asStream();

            OutputStream outStream = new FileOutputStream(log4jAgentPath.toFile());

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = log4jStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        }

        setOperation("Downloads (4/4)", "Downloading Adoptium JRE");
        {
            Adoptium.GetLatestReleaseAssets(modpack.server.java);
        }

    }

    void generateTempFolder() {
        tempFolder = serverPath.resolve(Paths.get("temp" + UUID.randomUUID().toString().split("-")[0]));
    }

    boolean deleteTempFolder()  {
        try {
            Files.delete(tempFolder);
            return true;
        } catch (IOException e) {
           return false;
        }
    }

    void checkPath(boolean ignoreEmpty) throws IOException {
        if (!Files.isWritable(serverPath) || !Files.isReadable(serverPath)) {
            String error = "The selected path is not read/writable.\n" + serverPath.toString();
            if (gui)
                JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
            throw new IOException(error);
        }

        if (!Files.isDirectory(serverPath)) {
            String error = "The selected path is not a valid directory\n" + serverPath.toString();
            if (gui)
                JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
            throw new InvalidPathException(serverPath.toString(), error);
        }

        if (!ignoreEmpty && Files.list(serverPath).findAny().isPresent()) {
            String error = "Folder is not empty\n" + serverPath.toString();
            if (gui)
                JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
            throw new IOException(error);
        }
    }

    /// Swing

    void initMainFrame() {
        if (mainFrame != null) {
            mainFrame.dispose();
        }

        mainFrame = new JFrame("Installing modpacks server...");
        mainFrame.setLayout(new BorderLayout(8,8));
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setPreferredSize(new Dimension(400, 150));
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setAlwaysOnTop(true);
        mainFrame.setResizable(false);

        currentOperationLabel = new JLabel("Starting...");
        currentOperationLabel.setHorizontalAlignment(JLabel.CENTER);

        progressBar = new JProgressBar();
        progressBar.setToolTipText("Progress");
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        mainFrame.add(currentOperationLabel, BorderLayout.NORTH);
        mainFrame.add(progressBar, BorderLayout.SOUTH);
        mainFrame.pack();

        mainFrame.setVisible(true);
    }

    void killFrame() {
        mainFrame.dispose();
    }

    void setOperation(String category, String operation) {
        this.currentOperationLabel.setText(category + ": " + operation);
    }

    void setOperation(String category, String operation, int increaseProgress) {
        setOperation(category, operation);
        increaseProgress();
    }

    void increaseProgress() {
        progressBar.setMaximum(setProgressCalls);
        progressBar.setValue(progressBar.getValue() + 1);
    }

    void generateBatchFile() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
