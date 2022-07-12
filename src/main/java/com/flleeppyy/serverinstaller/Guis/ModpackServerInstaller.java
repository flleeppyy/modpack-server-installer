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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
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
            if (modpack.forgeVersion != null) {
                Forge.ForgeVersion forgeVersion = Forge.getRecommendedVersion(modpack.minecraftVersion);

                if (forgeVersion == null) {
                    throw new IOException("No recommended version found");
                }
                // Get Installer
                String installerUrl = Forge.Utils.getInstallerUrl(forgeVersion);
                if (installerUrl == null) {
                    // TODO: Implement recovery section for GUI and CLI, telling the user to download the installer manually, and rename it to forgeinstaller.jar
                    throw new IOException("No installer found");
                }
                // Download installer
                InputStream installerStream = Request.get(installerUrl).execute().returnContent().asStream();
                Path installerJar = Files.createTempFile("forgeinstaller", ".jar");
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
            Path installerJar = serverPath.resolve("forgeinstaller.jar");

            int method = 0;

            if (method == 0) {
                // Run installer
                ProcessBuilder pb = new ProcessBuilder(
                        "java",
                        "-jar",
                        installerJar.toString(),
                        "--installServer",
                        "--installDir",
                        serverPath.toString()
                );

                // Log to file
                Path logPath = Files.createFile(serverPath.resolve("forgeinstaller.log"));
                pb.redirectError(logPath.toFile());
                pb.redirectOutput(logPath.toFile());
                Process process = pb.start();

                // Wait for installer to finish
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    throw new IOException("Installer interrupted");
                }

                // Check for errors
                if (process.exitValue() != 0) {
                    throw new IOException("Installer failed");
                }
            } else if (method == 1) {
                try (URLClassLoader ucl = URLClassLoader.newInstance(new URL[]{
                        ModpackServerInstaller.class.getProtectionDomain().getCodeSource().getLocation(),
                        installerJar.toUri().toURL()
                }, getParentClassLoader())) {
                    Class<?> installer = ucl.loadClass("com.flleeppyy.serverinstaller.Installer.Guis");
                    if (!(boolean) installer.getMethod("install", File.class, File.class).invoke(null, serverPath.toFile(), installerJar.toFile())) {
                        throw new IOException("Installer failed");
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }

        // Extract modpack
        setOperation("Installation (2/3)", "Beating your ass in the quote retweets",1);
        {

        }

        setOperation("Installation (3/3)", "Setting up batch files",1 );
        {
            if (modpack.server.java.javaArgs == null) {
                ZipFile zipFile = new ZipFile(packFilePath);
                FileHeader fileHeader = zipFile.getFileHeader("instance.cfg");
                InputStream inputStream = zipFile.getInputStream(fileHeader);
                String[] jvmArgs = new Ini(inputStream).get("JvmArgs", String.class).trim().split(" ");

                zipFile.close();

                // Join args together
                StringBuilder sb = new StringBuilder();
                for (String arg : jvmArgs) {
                    sb.append(arg).append(" ");
                }
                modpack.server.java.javaArgs = sb.toString();

                // Save to pack file
            }
        }


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
