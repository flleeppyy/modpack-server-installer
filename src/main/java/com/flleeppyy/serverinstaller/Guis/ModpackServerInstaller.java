package com.flleeppyy.serverinstaller.Guis;

import com.flleeppyy.serverinstaller.Adoptium;
import com.flleeppyy.serverinstaller.Json.ModpackInfo;
import com.flleeppyy.serverinstaller.Json.ModpackVersionSpec;
import com.flleeppyy.serverinstaller.ModpackApi;
import com.github.zafarkhaja.semver.Version;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.hc.client5.http.fluent.Request;
import org.ini4j.Ini;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    // TODO: Reimplement progress shit to be easier
    private int _installModpack(ModpackInfo modpackInfo, String version, boolean ignoreEmpty) throws IOException {
        // Re-fetch modpack for various reasons
        setOperation("Initialization (1/4)","Fetching modpack", 1);

        modpack = ModpackApi.getModpack(modpackInfo.name);
        if (modpack.server == null) {
            throw new IOException("Modpack doesn't support server installation");
        }

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

        setOperation("Downloads (1/6)", "Making temp folder",1);
        generateTempFolder();
        File packFilePath = tempFolder.resolve(versionSpec.filename).toFile();

        setOperation("Downloads (2/6)", "Downloading modpack file",1);
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

        setOperation("Downloads (3/6)", "Downloading Log4jPatcher",1);
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

        setOperation("Downloads (4/6)", "Downloading JRE");
        {
            String arch = System.getProperty("os.arch");
            String os = System.getProperty("os.name");

            Adoptium.Binary binary;
            if (modpack.server.java == null) {
                // lets just, uhhh cheeseburger
                binary = Adoptium.GetLatestAssets(getJavaReleaseForMinecraftVersion(modpack.minecraftVersion), arch, os, "jre").Binary;
            } else {
                binary = Adoptium.GetReleaseAssetByVer(modpack.server.java.adoptium.releaseVersion, arch, os, "jre").Binaries[0];
            }

            Path jrePath = Files.createDirectory(serverPath.resolve("jre"));
            InputStream jreStream = Request.get(binary.Package.Link).execute().returnContent().asStream();
            if (binary.Package.Link.endsWith(".tar.gz")) {
                // tar.gz
                try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(jreStream))) {
                    TarArchiveEntry entry;
                    while ((entry = tarIn.getNextTarEntry()) != null) {
                        if (entry.isDirectory()) {
                            Files.createDirectories(jrePath.resolve(entry.getName()));
                        } else {
                            Files.copy(tarIn, jrePath.resolve(entry.getName()));
                        }
                    }
                }
            } else if (binary.Package.Link.endsWith(".zip")) {
                // zip
                try (ZipInputStream zipIn = new ZipInputStream(jreStream)) {
                    ZipEntry entry;
                    while ((entry = zipIn.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            Files.createDirectories(jrePath.resolve(entry.getName()));
                        } else {
                            Files.copy(zipIn, jrePath.resolve(entry.getName()));
                        }
                    }
                }
            } else if (binary.Package.Link.endsWith(".msi")) {
                // Shouldn't be possible, but just in case
                throw new IOException("MSI is not supported");
            } else {
                // unknown
                throw new IllegalArgumentException("Unknown binary type: " + binary.Package.Link);
            }
        }

        // TODO: Add fabric support lmao
        setOperation("Downloads (5/6)", "Downloading Forge");
        {

        }

        setOperation("Downloads (6/6)", "Downloading Minecraft Server");
        {

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

    void generateBatchFile(String relativeJavaPath) throws IOException {
        byte[] blep = Files.readAllBytes(Paths.get(ModpackServerInstaller.class.getResource("/start.bat").getPath()));

        StringBuilder sb = new StringBuilder();
        for (byte b : blep) {
            sb.append((char) b);
        }

        String args = "";
        if (modpack.server.java.javaArgs != null) {
            args = modpack.server.java.javaArgs;
        }


        String javaPath = serverPath.resolve(relativeJavaPath).toString();
        // dont forget agent for log4j
        sb.replace("%ARGUMENTSTEMPLATE%", javaPath + " -javaagent:javaAgents/Log4jPatcher-1.0.0.jar " + args);
    }

    // silly little hack because I'm actually fucking lazy
    int getJavaReleaseForMinecraftVersion(String version) {
        Version semver = new Version.Builder(version).build();

        // Minecraft 1.17 and newer uses 17
        if (semver.getMajorVersion() >= 1 && semver.getMinorVersion() >= 17) {
            return 17;
        }
        // Minecraft 1.16 and older uses 8
        else if (semver.getMajorVersion() >= 1 && semver.getMinorVersion() <= 16) {
            return 8;
        } else {
            return 8;
        }

    }
}
