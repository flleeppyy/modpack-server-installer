// Some code inherited from TeamKun/ForgeCLI
//MIT License
//
//Copyright (c) 2021 TeamKun., Kamesuta
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

// I believe this is how licenses work?

package com.flleeppyy.serverinstaller.Guis;

import com.flleeppyy.serverinstaller.Adoptium;
import com.flleeppyy.serverinstaller.Json.ModpackInfo;
import com.flleeppyy.serverinstaller.Json.ModpackVersionSpec;
import com.flleeppyy.serverinstaller.MetaPolyMC.Forge;
import com.flleeppyy.serverinstaller.ModpackApi;
import com.flleeppyy.serverinstaller.Utils.OtherUtils;
import com.flleeppyy.serverinstaller.Utils.PolyCfgParser;
import com.github.zafarkhaja.semver.Version;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.hc.client5.http.fluent.Request;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModpackServerInstaller {
    Path serverPath;
    ModpackInfo modpack;

    JFrame mainFrame;
    JLabel currentOperationLabel;
    JTextPane logTextPane;
    JProgressBar progressBar;
    JButton abortButton;
    int setProgressCalls = 16;
    boolean gui = false;
    Thread modpackThread;

    Path tempFolder;

    public ModpackServerInstaller(Path serverPath) {
        this.serverPath = serverPath;
    }

    public void installModpack(ModpackInfo modpackInfo, String version, boolean ignoreEmpty, boolean gui) throws IOException {
        try {
            if (gui) {
                initMainFrame();
                this.gui = true;
                Thread modpackThread = new Thread(() -> {
                    try {
                        int result = _installModpack(modpackInfo, version, ignoreEmpty);

                        if (result == 0) {
                            JOptionPane.showMessageDialog(mainFrame, "Modpack installed successfully!");
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, "Modpack installation failed!");
                        }

                    } catch (Exception e) {
                        balls(e);
                    }
                    mainFrame.setVisible(false);
                    mainFrame.dispose();
                });

            modpackThread.start();

            } else {
                _installModpack(modpackInfo, version, ignoreEmpty);
            }
        } catch (IOException e) {
            balls(e);
        }
    }

    private void balls(Exception e) {
        e.printStackTrace();
        if (gui) {
            JOptionPane.showMessageDialog(null, "An error occurred installing the pack:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            killFrame();
        }
    }
    // TODO: Reimplement progress shit to be easier
    private int _installModpack(ModpackInfo modpackInfo, String version, boolean ignoreEmpty) throws IOException {
        // Re-fetch modpack for various reasons
        setOperation("Initialization (1/4)","Fetching modpack", 1);
        modpack = ModpackApi.getModpack(modpackInfo.id.toString());
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
        ModpackVersionSpec versionSpec = ModpackApi.getModpackVersionSpec(modpack.id.toString(), version);

        setOperation("Downloads (1/6)", "Making temp folder",1);
        generateTempFolder();
        File packFilePath = tempFolder.resolve(versionSpec.filename).toFile();

        setOperation("Downloads (2/6)", "Downloading modpack file",1);
        {
            if (!packFilePath.exists()) {
                if (Objects.equals(versionSpec.filetype, "zip")) {
                    URL packURL = new URL(versionSpec.getUrl());
                    ReadableByteChannel readableByteChannel = Channels.newChannel(packURL.openStream());
                    try (FileOutputStream fos = new FileOutputStream(packFilePath.toString())) {
                        fos.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    }
                }
            } else {
                System.out.println("Pack file already exists, skipping download");
            }
        }

        setOperation("Downloads (3/6)", "Downloading Log4jPatcher",1);
        {
            Path javaAgentsFolder = serverPath.resolve("javaAgents");
            if (!Files.exists(javaAgentsFolder)) {
                Files.createDirectory(javaAgentsFolder);
            }

            Path log4jPatcherPath = javaAgentsFolder.resolve("log4jPatcher.jar");

            if (!Files.exists(log4jPatcherPath)) {
                InputStream log4jStream = Request.get("https://github.com/CreeperHost/Log4jPatcher/releases/download/v1.0.1/Log4jPatcher-1.0.1.jar")
                        .execute()
                        .returnContent()
                        .asStream();

                OutputStream outStream = new FileOutputStream(log4jPatcherPath.toString());

                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = log4jStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            } else {
                System.out.println("Log4jPatcher already exists, skipping download");
            }
        }

        Path jrePath = serverPath.resolve("jre");
        String realOs;
        String os = System.getProperty("os.name").toLowerCase();

        //https://stackoverflow.com/questions/47160990/how-to-determine-32-bit-os-or-64-bit-os-from-java-application
        String arch = System.getenv("PROCESSOR_ARCHITECTURE");
        String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");

        // god fucking dammit this is so stupid
        String realArch = (arch != null && arch.endsWith("64")
                || wow64Arch != null && wow64Arch.endsWith("64")
                ? "64" : "32").equals("64") ? "x64" : "x86";

        // TODO: add more checks for the other OSes or something
        if (os.contains("win")) {
            realOs = "windows";
        } else if (os.contains("mac")) {
            realOs = "macos";
        } else if (os.contains("linux")) {
            realOs = "linux";
        } else {
            throw new IllegalArgumentException("Unsupported OS: " + os);
        }
        setOperation("Downloads (4/6)", "Downloading JRE");
        if (!Files.exists(serverPath.resolve("jre"))) {


            Adoptium.Binary binary;
            if (modpack.server.java == null) {
                // lets just, uhhh cheeseburger

                binary = Adoptium.GetLatestAssets(getJavaReleaseForMinecraftVersion(modpack.minecraftVersion), realArch, realOs, "jre").Binary;
            } else {
                binary = Adoptium.GetReleaseAssetByVer(modpack.server.java.adoptium.releaseVersion, arch, os, "jre").Binaries[0];
            }


            if (!Files.exists(jrePath)) {
                Files.createDirectory(jrePath);
            }

            appendLog("Downloading JRE from " + binary.Package.Link);
            InputStream jreStream = Request.get(binary.Package.Link).execute().returnContent().asStream();

            appendLog("Extracting JRE to " + jrePath);
            if (binary.Package.Link.endsWith(".tar.gz")) {
                // tar.gz
                try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(jreStream))) {
                    TarArchiveEntry entry;
                    while ((entry = tarIn.getNextTarEntry()) != null) {
                        String regex = "(jdk|jre[a-z0-9\\\\-]*)\\\\/(.*)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(entry.getName());

                        Path destEntryPath = entry.getName().matches(regex) ? jrePath.resolve(matcher.group(2)) : jrePath;
                        if (entry.isDirectory()) {
                            Files.createDirectories(destEntryPath);
                        } else {
                            Files.copy(tarIn, destEntryPath);
                            appendLog("Extracting " + destEntryPath);
                        }
                    }
                }
            } else if (binary.Package.Link.endsWith(".zip")) {
                // zip
                try (ZipInputStream zipIn = new ZipInputStream(jreStream)) {
                    ZipEntry entry;
                    while ((entry = zipIn.getNextEntry()) != null) {
                        try {
                        Path destEntryPath = Paths.get(jrePath + "/" + entry.getName().split("/", 2)[1]);
                            if (entry.isDirectory()) {
                                Files.createDirectories(destEntryPath);
                            } else {
                                appendLog("Extracting " + destEntryPath);
                                Files.copy(zipIn, destEntryPath);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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
        Forge.ForgeVersion forgeVersion = Forge.getRecommendedVersion(modpack.minecraftVersion);
        {
            if (modpack.forgeVersion != null) {

                if (forgeVersion == null) {
                    throw new IOException("No recommended version found");
                }
                // Get Installer
                String installerUrl = Forge.Utils.getInstallerUrl(forgeVersion);
                if (installerUrl == null) {
                    // TODO: Implement recovery section for GUI and CLI, telling the user to download the installer manually, and rename it to forge-installer.jar
                    throw new IOException("No installer found");
                }
                appendLog("Downloading Forge installer from URL: " + installerUrl);
                // Download installer
                InputStream installerStream = Request.get(installerUrl).execute().returnContent().asStream();
                Path installerJar = serverPath.resolve("forge-installer.jar");
                Files.copy(installerStream, installerJar, StandardCopyOption.REPLACE_EXISTING);

            }
        }

        setOperation("Downloads (6/6)", "Downloading Minecraft Server");
        {
            // Skipping cause forge downloads it for us yay
        }

        // Installation
        setOperation("Installation (1/3)", "Installing Forge Server", 1);
        {
            // set path for forgeinstaller.jar
            Path installerJar = serverPath.resolve("forge-installer.jar");

//            appendLog("Using method 0 to install forge");
            // Run installer
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-jar",
                    installerJar.toString(),
                    "--installServer",
                    serverPath.toString()
            );

            // Log to file

            Path logPath;
            if (!Files.exists(serverPath.resolve("forge-installer.log"))) {
                logPath = Files.createFile(serverPath.resolve("forge-installer.log"));
            } else {
                logPath = serverPath.resolve("forge-installer.log");
            }
            pb.redirectError(logPath.toFile());
            pb.redirectOutput(logPath.toFile());

            appendLog("Running installer");
            Process process = pb.start();

            // Output stdout to JTextPane
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLog(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            stdoutThread.start();

            // Wait for installer to finish
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                throw new IOException("Forge Installer interrupted");
            }

            System.out.println(process.exitValue());
            // Check for errors
            if (process.exitValue() != 0) {
                // Convert process error stream to string
                StringBuilder error = new StringBuilder();
                InputStream errorStream = process.getErrorStream();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                }

                System.out.println(error);


                throw new IOException("Forge Installer failed");

            }
        }

        // Extract modpack
        setOperation("Installation (2/3)", "Beating your ass in the quote retweets",1);
        {

        }

        setOperation("Installation (3/3)", "Setting up batch files",1 );
        {
            String baseJavaArguments = "";
            if (modpack.server.java == null || modpack.server.java.javaArgs == null) {
                appendLog("Fetching args from instance config file");
                ZipFile zipFile = new ZipFile(packFilePath);
                InputStream is = zipFile.getInputStream(zipFile.getFileHeader("instance.cfg"));
                String jvmArgsRaw = new PolyCfgParser(is).get("JvmArgs");
                zipFile.close();
                if (jvmArgsRaw != null) {
                    baseJavaArguments = jvmArgsRaw;
                } else {
                    appendLog("No Java args detected");
                }
            } else {
                baseJavaArguments = modpack.server.java.javaArgs;
            }

            // Get local start.bat resource as URl
            String startBatContents = OtherUtils.getResource("/start.bat");
            System.out.println(startBatContents);

            StringBuilder arguments = new StringBuilder();

            arguments.append(jrePath).append(realOs.equals("windows") ? "\\bin\\java.exe " : "/bin/java ");

            // Search server folder for jar that starts with forge-1.
            Path forgeJar = null;

            for (Path path : Files.newDirectoryStream(serverPath)) {
                if (path.toString().endsWith(".jar")) {
                    if (path.toString().contains("forge-")) {
                        forgeJar = path;
                        break;
                    }
                }
            }

            if (forgeJar == null) {
                appendLog("Forge jar not found, youll have to insert it manually.");
                arguments.append("-jar ").append("replaceWithForgeServer.jar ");
            } else {
                arguments.append("-jar ").append(forgeJar.toFile().getName()).append(" ");
            }

            arguments.append(baseJavaArguments).append(" -nogui");

            startBatContents = startBatContents.replace("%ARGUMENTSTEMPLATE%", arguments.toString());

            // Write to file
            Path startBatPath = serverPath.resolve("start.bat");
            appendLog("Writing start.bat");
            Files.write(startBatPath, startBatContents.getBytes());
        }

        // Done
        appendLog("Done");
        return 0;
    }

    private static ClassLoader getParentClassLoader() {
        if (!System.getProperty("java.version").startsWith("1.")) {
            try {
                return (ClassLoader) ClassLoader.class.getDeclaredMethod("getPlatformClassLoader").invoke(null);
            } catch (Exception e) {
                System.out.println("No platform classloader: " + System.getProperty("java.version"));
            }
        }
        return null;
    }

    void generateTempFolder() throws IOException {
        tempFolder = serverPath.resolve("serverinstallerTemp");
        if (!tempFolder.toFile().exists()) {
            Files.createDirectories(tempFolder);
        }
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

        mainFrame = new JFrame("Installing modpack server...");
        mainFrame.setLayout(new BorderLayout(8,8));
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setPreferredSize(new Dimension(500, 220));
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setAlwaysOnTop(true);
        mainFrame.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        mainPanel.setLayout(new BorderLayout(4,4));

        currentOperationLabel = new JLabel("Starting...");
        currentOperationLabel.setHorizontalAlignment(JLabel.CENTER);

        progressBar = new JProgressBar();
        progressBar.setName("Progress");
        progressBar.setVisible(true);
        progressBar.setStringPainted(true);
        progressBar.setMaximum(setProgressCalls);

        logTextPane = new JTextPane();
        logTextPane.setPreferredSize(new Dimension(500, 100));
        logTextPane.setEditable(false);

        JScrollPane logScrollPane = new JScrollPane(logTextPane);
        logScrollPane.setPreferredSize(new Dimension(500, 100));
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        mainPanel.add(currentOperationLabel, BorderLayout.NORTH);
        mainPanel.add(logScrollPane, BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);

        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    void appendLog(String text) {
        if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        logTextPane.setText(logTextPane.getText() + text + "\n");
        logTextPane.setCaretPosition(logTextPane.getDocument().getLength());
    }

    void killFrame() {
        mainFrame.dispose();
    }

    void setOperation(String category, String operation) {
        this.currentOperationLabel.setText(category + ": " + operation);
        appendLog(operation);
    }

    void setOperation(String category, String operation, int increaseProgress) {
        setOperation(category, operation);
        increaseProgress();
    }

    void increaseProgress() {
        progressBar.setValue(progressBar.getValue() + 1);
    }

    String generateBatchFile(String relativeJavaPath) throws IOException {
        byte[] blep = Files.readAllBytes(Paths.get(ModpackServerInstaller.class.getResource("/start.bat").getPath()));

        String sb = "";
        for (byte b : blep) {
            sb += ((char) b);
        }

        String args = "";
        if (modpack.server.java.javaArgs != null) {
            args = modpack.server.java.javaArgs;
        }

        String javaPath = serverPath.resolve(relativeJavaPath).toString();
        // dont forget agent for log4j
        return sb.replace("%ARGUMENTSTEMPLATE%", javaPath + " -javaagent:javaAgents/Log4jPatcher-1.0.0.jar " + args);
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
