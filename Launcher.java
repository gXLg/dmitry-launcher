package com.dmitry;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import java.util.concurrent.*;
import org.json.*;
import org.apache.tools.tar.*;

import javax.swing.*;
import java.awt.Font;
import java.awt.Image;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Desktop;

import java.io.OutputStream;
import java.util.stream.Collectors;
import java.lang.module.ModuleDescriptor.Version;

public class Launcher {

  private static final File MINECRAFT_DIR = new File(System.getProperty("user.home"), ".dmitry-launcher");

  private static JTextArea outputArea;
  private static JTextField inputField;
  private static PipedOutputStream pipedOut = new PipedOutputStream();
  private static List<Process> children = new ArrayList<>();

  public static void main(String[] args) throws Exception {
    MINECRAFT_DIR.mkdirs();

    if (args.length < 2) {
      runLauncher();
    } else {
      runConsole(args[0], args[1]);
    }
  }

  private static void runLauncher() {
    JFrame frame = new JFrame("Dmitry Launcher");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(600, 500);
    
    JPanel title = new JPanel();
    title.setLayout(new FlowLayout(FlowLayout.LEFT));
    ImageIcon icon = new ImageIcon(Launcher.class.getResource("/images/icon.png"));
    ImageIcon scaled = new ImageIcon(icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH));
    JLabel label = new JLabel("Dmitry Launcher", scaled, JLabel.LEFT);
    title.setFont(new Font("SansSerif", Font.BOLD, 26));
    title.add(label);

    if (Desktop.isDesktopSupported()) {
      JButton launcherButton = new JButton("Open launcher folder");
      Desktop desktop = Desktop.getDesktop();
      launcherButton.addActionListener(e -> {
        try { desktop.open(MINECRAFT_DIR); } catch (Exception ignored) {}
      });
      title.add(launcherButton);
    }

    JPanel main = new JPanel(new BorderLayout());
    main.setBorder(BorderFactory.createEmptyBorder(4, 16, 16, 16));
    main.add(title, BorderLayout.NORTH);

    
    JPanel content = new JPanel();
    content.setLayout(new GridLayout(1, 2));
    
    // CLIENT
    JPanel panel1 = new JPanel();
    panel1.setLayout(new BorderLayout());
    panel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JLabel header1 = new JLabel("Client");
    header1.setFont(new Font("SansSerif", Font.BOLD, 16));
    panel1.add(header1, BorderLayout.NORTH);

    // Scrollable list of profiles
    JPanel buttonList1 = new JPanel();
    buttonList1.setLayout(new BoxLayout(buttonList1, BoxLayout.Y_AXIS));
    File clientProfilesDir = new File(MINECRAFT_DIR, "profiles");
    clientProfilesDir.mkdirs();
    File[] profiles1 = clientProfilesDir.listFiles();
    Set<String> profilesSet1 = new HashSet<>();
    if (profiles1 != null) {
      for (int i = 0; i < profiles1.length; i++) {
        profilesSet1.add(profiles1[i].getName());
        if (!profiles1[i].isDirectory()) continue;
        buttonList1.add(createLaunchButton("client", profiles1[i].getName(), frame));
      }
    }
    JScrollPane scrollPane1 = new JScrollPane(buttonList1);
    scrollPane1.setPreferredSize(new Dimension(400, 250));
    panel1.add(scrollPane1, BorderLayout.CENTER);

    // New profile form
    JPanel formPanel1 = new JPanel();
    formPanel1.setAlignmentX(Component.LEFT_ALIGNMENT);
    formPanel1.setLayout(new BoxLayout(formPanel1, BoxLayout.Y_AXIS));
    formPanel1.add(Box.createVerticalStrut(10));
    formPanel1.add(new JLabel("New profile"));

    JTextField clientName = new JTextField();
    JTextField clientVersion = new JTextField();
    JTextField clientMods = new JTextField();
    formPanel1.add(makeLabeledField("Name:", clientName));
    formPanel1.add(makeLabeledField("Version:", clientVersion));
    formPanel1.add(makeLabeledField("Mods:", clientMods));

    JButton createButton1 = new JButton("Create");
    createButton1.setAlignmentX(Component.LEFT_ALIGNMENT);
    formPanel1.add(Box.createVerticalStrut(5));
    formPanel1.add(createButton1);

    panel1.add(formPanel1, BorderLayout.SOUTH);
    content.add(panel1);

    createButton1.addActionListener(e -> {
        String name = clientName.getText().strip();
        String version = clientVersion.getText().strip();
        String mods = clientMods.getText().replace("\\s+", "");
        if (name.length() == 0 || version.length() == 0) return;
        if (profilesSet1.contains(name)) return;

        try {
          File profileDir = new File(clientProfilesDir, name);
          profileDir.mkdirs();
          JSONArray loaderData = readJsonArray("https://meta.fabricmc.net/v2/versions/loader/");
          String fabricLoaderVersion = null;
          for (int i = 0; i < loaderData.length(); i++) {
            JSONObject entry = loaderData.getJSONObject(i);
            if (entry.getBoolean("stable")) {
              fabricLoaderVersion = entry.getString("version");
              break;
            }
          }
          Files.write(new File(profileDir, "version.txt").toPath(), (version + " " + fabricLoaderVersion + " " + mods).getBytes());
          
          buttonList1.add(createLaunchButton("client", name, frame));
          buttonList1.revalidate();
          buttonList1.repaint();
          
          clientName.setText("");
          clientVersion.setText("");
          clientMods.setText("");
        } catch (Exception ignored) {}
    });

    // SERVER
    JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout());
    panel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JLabel header2 = new JLabel("Server");
    header2.setFont(new Font("SansSerif", Font.BOLD, 16));
    panel2.add(header2, BorderLayout.NORTH);

    // Scrollable list of profiles
    JPanel buttonList2 = new JPanel();
    buttonList2.setLayout(new BoxLayout(buttonList2, BoxLayout.Y_AXIS));
    File serverProfilesDir = new File(MINECRAFT_DIR, "servers");
    serverProfilesDir.mkdirs();
    File[] profiles2 = serverProfilesDir.listFiles();
    Set<String> profilesSet2 = new HashSet<>();
    if (profiles2 != null) {
      for (int i = 0; i < profiles2.length; i++) {
        profilesSet2.add(profiles2[i].getName());
        if (!profiles2[i].isDirectory()) continue;
        buttonList2.add(createLaunchButton("server", profiles2[i].getName(), frame));
      }
    }
    JScrollPane scrollPane2 = new JScrollPane(buttonList2);
    scrollPane2.setPreferredSize(new Dimension(400, 250));
    panel2.add(scrollPane2, BorderLayout.CENTER);

    // New profile form
    JPanel formPanel2 = new JPanel();
    formPanel2.setAlignmentX(Component.LEFT_ALIGNMENT);
    formPanel2.setLayout(new BoxLayout(formPanel2, BoxLayout.Y_AXIS));
    formPanel2.add(Box.createVerticalStrut(10));
    formPanel2.add(new JLabel("New profile"));

    JTextField serverName = new JTextField();
    JTextField serverVersion = new JTextField();
    JTextField serverTunnel = new JTextField();
    formPanel2.add(makeLabeledField("Name:", serverName));
    formPanel2.add(makeLabeledField("Version:", serverVersion));
    formPanel2.add(makeLabeledField("Tunnel:", serverTunnel));

    JButton createButton2 = new JButton("Create");
    createButton2.setAlignmentX(Component.LEFT_ALIGNMENT);
    formPanel2.add(Box.createVerticalStrut(5));
    formPanel2.add(createButton2);

    panel2.add(formPanel2, BorderLayout.SOUTH);
    content.add(panel2);

    createButton2.addActionListener(e -> {
        String name = serverName.getText();
        String version = serverVersion.getText();
        String tunnel = serverTunnel.getText();
        if (name.length() == 0 || version.length() == 0) return;
        if (profilesSet2.contains(name)) return;

        try {

          if (version.equals("bridge")) {
            name += " (bridge)";
            File profileDir = new File(serverProfilesDir, name);
            profileDir.mkdirs();
            Files.write(new File(profileDir, "version.txt").toPath(), ("bridge - " + tunnel).getBytes());

          } else {
            File profileDir = new File(serverProfilesDir, name);
            profileDir.mkdirs();
            JSONArray loaderData = readJsonArray("https://meta.fabricmc.net/v2/versions/loader/");
            String fabricLoaderVersion = null;
            for (int i = 0; i < loaderData.length(); i++) {
              JSONObject entry = loaderData.getJSONObject(i);
              if (entry.getBoolean("stable")) {
                fabricLoaderVersion = entry.getString("version");
                break;
              }
            }
            Files.write(new File(profileDir, "version.txt").toPath(), (version + " " + fabricLoaderVersion + " " + tunnel).getBytes());
          }
          
          buttonList2.add(createLaunchButton("server", name, frame));
          buttonList2.revalidate();
          buttonList2.repaint();
          
          serverName.setText("");
          serverVersion.setText("");
          serverTunnel.setText("");
        } catch (Exception ignored) {}
    });

    main.add(content, BorderLayout.CENTER);
    frame.setContentPane(main);
    frame.setVisible(true);
  }

  private static JPanel makeLabeledField(String labelText, JTextField field) {
    JPanel fieldPanel = new JPanel(new BorderLayout());
    JLabel label = new JLabel(labelText);

    fieldPanel.add(label, BorderLayout.NORTH);
    fieldPanel.add(field, BorderLayout.CENTER);
    fieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
    return fieldPanel;
  }
  
  private static JButton createLaunchButton(String launcher, String profile, JFrame frame) {
    JButton button = new JButton(profile);
    button.setAlignmentX(Component.LEFT_ALIGNMENT);
    button.addActionListener(e -> {
      try { launchWithArgs(launcher, profile); } catch (Exception ignored) {}
    });
    if (launcher.equals("client")) {
      button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (SwingUtilities.isRightMouseButton(e)) try { showEditPopup(profile, frame); } catch (Exception ignored) {}
        }
      });
    }
    return button;
  }
  
  private static void showEditPopup(String profile, JFrame parent) throws Exception {
    JDialog dialog = new JDialog(parent, "Edit mods", true);
    dialog.setSize(400, 250);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JTextArea inputArea = new JTextArea(5, 40);
    inputArea.setLineWrap(true);
    inputArea.setWrapStyleWord(true);

    File clientProfilesDir = new File(MINECRAFT_DIR, "profiles");
    File profileDir = new File(clientProfilesDir, profile);
    String[] data = new String(Files.readAllBytes(new File(profileDir, "version.txt").toPath())).trim().split(" ");
    inputArea.setText(data.length > 2 ? data[2] : "");
    JScrollPane scrollPane = new JScrollPane(inputArea);

    JButton submitButton = new JButton("Edit");
    submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    submitButton.setMaximumSize(new Dimension(150, 40));
    submitButton.setPreferredSize(new Dimension(150, 40));

    submitButton.addActionListener(e -> {
      String mods = inputArea.getText().replace("\\s+", "");
      try {
        Files.write(new File(profileDir, "version.txt").toPath(), (data[0] + " " + data[1] + " " + mods).getBytes());
      } catch (Exception ignored) {}
      dialog.dispose();
    });

    panel.add(scrollPane);
    panel.add(Box.createVerticalStrut(10));
    panel.add(submitButton);

    dialog.add(panel);
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
  }

  private static void launchWithArgs(String launcher, String profile) throws Exception {
    String jar = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
    if (getPlatformOSName() == "windows") {
      new ProcessBuilder("cmd", "/c", "start", "/b", "java", "-jar", jar, launcher, profile).start();
    } else {
      new ProcessBuilder("sh", "-c", "nohup java -jar '" + jar + "' '" + launcher + "' '" + profile + "' </dev/null &>/dev/null &").start();
    }
  }

  private static void runConsole(String launcher, String profile) throws Exception {
    File profilesDir = new File(MINECRAFT_DIR, launcher.equals("client") ? "profiles" : "servers");
    profilesDir.mkdirs();
    File profileDir = new File(profilesDir, profile);
    profileDir.mkdirs();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      for (Process p : children) {
        p.destroy();
      }
    }));

    SwingUtilities.invokeLater(() -> runGUI(launcher, profile));

    System.setIn(new PipedInputStream(pipedOut));

    // Redirect output
    System.setOut(new PrintStream(new TextAreaOutputStream()));
    System.setErr(new PrintStream(new TextAreaOutputStream()));

    run(launcher, profile);
  }

  private static void runGUI(String launcher, String profile) {
    File profilesDir = new File(MINECRAFT_DIR, launcher.equals("client") ? "profiles" : "servers");
    File profileDir = new File(profilesDir, profile);

    JFrame frame = new JFrame("Dmitry Launcher (" + launcher + ")");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 800);

    outputArea = new JTextArea();
    outputArea.setEditable(false);
    outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

    JScrollPane scrollPane = new JScrollPane(outputArea);

    PipedOutputStream pipedOutOld = pipedOut;

    inputField = new JTextField();
    inputField.addActionListener(e -> {
      String text = inputField.getText() + "\n"; // simulate Enter key
      try {
        pipedOut.write(text.getBytes());
        pipedOut.flush();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      inputField.setText("");

      outputArea.append(text);
      outputArea.setCaretPosition(outputArea.getDocument().getLength());
    });

    inputField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        // When input field gets focus, set caret to end
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
      }
    });

    JPanel title = new JPanel();
    title.setLayout(new FlowLayout(FlowLayout.LEFT));
    ImageIcon icon = new ImageIcon(Launcher.class.getResource("/images/icon.png"));
    ImageIcon scaled = new ImageIcon(icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH));
    JLabel label = new JLabel("Dmitry Launcher", scaled, JLabel.LEFT);
    title.setFont(new Font("SansSerif", Font.BOLD, 26));
    title.add(label);

    if (Desktop.isDesktopSupported()) {
      JButton launcherButton = new JButton("Open profile folder");
      Desktop desktop = Desktop.getDesktop();
      launcherButton.addActionListener(e -> {
        try { desktop.open(profileDir); } catch (Exception ignored) {}
      });
      title.add(launcherButton);
    }

    JPanel main = new JPanel(new BorderLayout());
    main.setBorder(BorderFactory.createEmptyBorder(4, 16, 16, 16));
    main.add(title, BorderLayout.NORTH);
    main.add(scrollPane, BorderLayout.CENTER);
    main.add(inputField, BorderLayout.SOUTH);

    frame.setContentPane(main);
    frame.setVisible(true);
    SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
  }

  private static void run(String launcher, String profile) throws Exception {
    // Get latest installer version
    JSONArray installerData = readJsonArray("https://meta.fabricmc.net/v2/versions/installer");
    String fabricInstallerVersion = null;
    for (int i = 0; i < installerData.length(); i++) {
      JSONObject entry = installerData.getJSONObject(i);
      if (entry.getBoolean("stable")) {
        fabricInstallerVersion = entry.getString("version");
        break;
      }
    }

    if (fabricInstallerVersion == null) {
      System.out.println("Could not find a stable Fabric installer version.");
      return;
    }

    String fabricInstallerUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/" + fabricInstallerVersion + "/fabric-installer-" + fabricInstallerVersion + ".jar";
    File fabricInstallerPath = new File(MINECRAFT_DIR, "fabric-installer-" + fabricInstallerVersion + ".jar");

    download(fabricInstallerUrl, fabricInstallerPath);

    Scanner scanner = new Scanner(System.in);
    if (launcher.equals("client")) {
      launchClient(scanner, fabricInstallerPath, profile);
    } else if (launcher.equals("server")) {
      launchServer(scanner, fabricInstallerPath, profile);
    }
  }

  private static void download(String urlString, File path) throws IOException {
    path.getParentFile().mkdirs();
    if (path.exists()) {
      System.out.println("Exists " + path.getAbsolutePath());
      return;
    }
    System.out.println("Downloading " + urlString + " -> " + path.getAbsolutePath());
    path.getParentFile().mkdirs();
    URL url = URI.create(urlString).toURL();
    try (InputStream in = url.openStream()) {
      Files.copy(in, path.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static JSONArray readJsonArray(String urlString) throws IOException, JSONException, URISyntaxException {
    return new JSONArray(readUrl(urlString));
  }

  private static JSONObject readJsonObject(String urlString) throws IOException, JSONException, URISyntaxException {
    return new JSONObject(readUrl(urlString));
  }

  private static String readUrl(String urlString) throws IOException, URISyntaxException {
    if (urlString.startsWith("file:")) {
      return new String(Files.readAllBytes(new File(new URI(urlString)).toPath()));
    }

    URL url = URI.create(urlString).toURL();
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
      return builder.toString();
    }
  }

  private static void launchClient(Scanner scanner, File fabricInstallerPath, String profileName) throws Exception {
    File profilesDir = new File(MINECRAFT_DIR, "profiles");
    File assetsDir = new File(MINECRAFT_DIR, "assets");
    assetsDir.mkdirs();

    String uuid;
    File uuidPath = new File(MINECRAFT_DIR, "uuid.txt");
    if (uuidPath.exists()) {
      uuid = new String(Files.readAllBytes(uuidPath.toPath())).trim();
    } else {
      uuid = UUID.randomUUID().toString();
      Files.write(uuidPath.toPath(), uuid.getBytes());
    }

    String playerName;
    File playerNamePath = new File(MINECRAFT_DIR, "playername.txt");
    if (playerNamePath.exists()) {
      playerName = new String(Files.readAllBytes(playerNamePath.toPath())).trim();
    } else {
      System.out.println("What should be your Minecraft playername?");
      System.out.print("> ");
      playerName = scanner.nextLine().trim();
      Files.write(playerNamePath.toPath(), playerName.getBytes());
    }

    File profileDir = new File(profilesDir, profileName);
    File librariesDir = new File(profileDir, "libraries");
    File versionsDir = new File(profileDir, "versions");
    File modsDir = new File(profileDir, "mods");

    librariesDir.mkdirs();
    versionsDir.mkdirs();
    modsDir.mkdirs();

    String[] data = new String(Files.readAllBytes(new File(profileDir, "version.txt").toPath())).trim().split(" ");
    String minecraftVersion = data[0];
    String fabricLoaderVersion = data[1];
    String mods = data.length > 2 ? data[2] : "";

    String versionId = "fabric-loader-" + fabricLoaderVersion + "-" + minecraftVersion;
    File fabricVersionDir = new File(versionsDir, versionId);
    File vanillaVersionDir = new File(versionsDir, minecraftVersion);

    // Install Fabric if needed
    if (!fabricVersionDir.exists()) {
      ProcessBuilder pb = new ProcessBuilder(
        "java", "-jar", fabricInstallerPath.getAbsolutePath(),
        "client",
        "-dir", profileDir.getAbsolutePath(),
        "-mcversion", minecraftVersion,
        "-loader", fabricLoaderVersion,
        "-noprofile"
      );
      Process p = pb.start();
      children.add(p);
      pipeChild(p);
    }

    // Download vanilla Minecraft version
    File versionJsonPath = new File(vanillaVersionDir, minecraftVersion + ".json");
    JSONObject versionJson = null;
    if (!vanillaVersionDir.exists()) {
      JSONObject versionManifest = readJsonObject("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
      JSONArray versions = versionManifest.getJSONArray("versions");
      String versionUrl = null;
      for (int i = 0; i < versions.length(); i++) {
        if (versions.getJSONObject(i).getString("id").equals(minecraftVersion)) {
          versionUrl = versions.getJSONObject(i).getString("url");
          break;
        }
      }
      if (versionUrl == null) {
        System.out.println("Could not find the official Minecraft version " + minecraftVersion);
        return;
      }
      vanillaVersionDir.mkdirs();
      download(versionUrl, versionJsonPath);
      versionJson = readJsonObject(versionJsonPath.toURI().toString());
      download(versionJson.getJSONObject("downloads").getJSONObject("client").getString("url"), new File(vanillaVersionDir, minecraftVersion + ".jar"));
    } else {
      versionJson = readJsonObject(versionJsonPath.toURI().toString());
    }

    // Download libraries
    JSONArray libraries = versionJson.getJSONArray("libraries");
    for (int i = 0; i < libraries.length(); i++) {
      JSONObject lib = libraries.getJSONObject(i);
      if (!checkRules(lib.optJSONArray("rules"))) continue;

      JSONObject artifact = lib.getJSONObject("downloads").getJSONObject("artifact");
      String artifactUrl = artifact.getString("url");
      String artifactPath = artifact.getString("path");
      download(artifactUrl, new File(librariesDir, artifactPath));

      if (lib.has("downloads") && lib.getJSONObject("downloads").has("classifiers")) {
        String osName = getPlatformOSName();
        JSONObject classifiers = lib.getJSONObject("downloads").getJSONObject("classifiers");
        if (lib.has("natives") && lib.getJSONObject("natives").has(osName)) {
          String nativeKey = lib.getJSONObject("natives").getString(osName);
          JSONObject nativeObj = classifiers.getJSONObject(nativeKey);
          String nativeUrl = nativeObj.getString("url");
          String nativePath = nativeObj.getString("path");
          download(nativeUrl, new File(librariesDir, nativePath));
        }
      }
    }

    // Download assets
    String assetsIndexName = versionJson.getString("assets");
    File assetsFile = new File(new File(assetsDir, "indexes"), assetsIndexName + ".json");
    download(versionJson.getJSONObject("assetIndex").getString("url"), assetsFile);

    JSONObject assetsJson = readJsonObject(assetsFile.toURI().toString());
    JSONObject objects = assetsJson.getJSONObject("objects");
    File objectsDir = new File(assetsDir, "objects");

    for (String key : objects.keySet()) {
      JSONObject asset = objects.getJSONObject(key);
      String hash = asset.getString("hash");
      File assetPath = new File(new File(objectsDir, hash.substring(0, 2)), hash);
      download("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash, assetPath);
    }
    // Download mods
    if (!downloadMod("fabric-api", modsDir, minecraftVersion)) return;
    if (!downloadMod("modflared", modsDir, minecraftVersion)) {
      System.out.println("You won't be able to play on a remote cloudflare server in this version");
    }
    downloadMod("modmenu", modsDir, minecraftVersion);

    if (mods.length() > 1) {
      for (String mod : mods.split(",")) {
        downloadMod(mod, modsDir, minecraftVersion);
      }
    }

    // Create classpath
    Map<String, Version> newest = new HashMap<>();
    Map<String, Path> paths = new HashMap<>();
    Files.walk(librariesDir.toPath())
        .filter(path -> path.toString().endsWith(".jar"))
        .forEach(path -> {
          String[] parts = path.toString().replace("\\", "/").split("/");

          String sversion = parts[parts.length - 2];
          Version version = Version.parse(sversion);
          String key = String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 2)) + "/" + parts[parts.length - 1].replace(sversion, "X");

          if (newest.containsKey(key)) {
            Version old = newest.get(key);
            if (old.compareTo(version) > 0) return;
          }
          newest.put(key, version);
          paths.put(key, path);
        });
    List<String> classpathList = new ArrayList<>(paths.values().stream().map(Path::toString).collect(Collectors.toList()));

    classpathList.add(new File(vanillaVersionDir, minecraftVersion + ".jar").getAbsolutePath());
    String classpath = String.join(File.pathSeparator, classpathList);

    // Setup arguments
    Map<String, String> substitutes = new HashMap<>();
    substitutes.put("auth_player_name", playerName);
    substitutes.put("version_name", versionId);
    substitutes.put("game_directory", profileDir.getAbsolutePath());
    substitutes.put("assets_root", assetsDir.getAbsolutePath());
    substitutes.put("assets_index_name", assetsIndexName);
    substitutes.put("auth_uuid", uuid);
    substitutes.put("auth_access_token", "access-token");
    substitutes.put("clientid", "fake-client-id");
    substitutes.put("auth_xuid", "");
    substitutes.put("user_type", "legacy");
    substitutes.put("version_type", "release");
    substitutes.put("launcher_name", "dmitry-launcher");
    substitutes.put("launcher_version", "1.0");
    substitutes.put("natives_directory", new File(profileDir, "natives").getAbsolutePath());
    substitutes.put("classpath", classpath);

    JSONObject fabricConfig = readJsonObject(new File(new File(profileDir, "versions/" + versionId), versionId + ".json").toURI().toString());

    List<String> jvmArgs = new ArrayList<>();
    List<String> gameArgs = new ArrayList<>();

    List<JSONArray> jvmAndGameArrays = Arrays.asList(
        mergeArrays(fabricConfig.getJSONObject("arguments").getJSONArray("jvm"), versionJson.getJSONObject("arguments").getJSONArray("jvm")),
        mergeArrays(fabricConfig.getJSONObject("arguments").getJSONArray("game"), versionJson.getJSONObject("arguments").getJSONArray("game"))
    );

    for (int k = 0; k < 2; k++) {
      JSONArray array = jvmAndGameArrays.get(k);
      List<String> targetList = (k == 0) ? jvmArgs : gameArgs;

      for (int i = 0; i < array.length(); i++) {
        Object obj = array.get(i);
        if (obj instanceof JSONObject) {
          JSONObject argObj = (JSONObject) obj;
          if (targetList != gameArgs && checkRules(argObj.optJSONArray("rules"))) {
            Object val = argObj.get("value");
            if (val instanceof JSONArray) {
              JSONArray valArr = (JSONArray) val;
              for (int j = 0; j < valArr.length(); j++) {
                targetList.add(substitute(valArr.getString(j), substitutes));
              }
            } else if (val instanceof String) {
              targetList.add(substitute((String) val, substitutes));
            }
          }
        } else if (obj instanceof String) {
          targetList.add(substitute((String) obj, substitutes));
        }
      }
    }

    System.out.println("Launching with JVM options:");
    System.out.println(jvmArgs);
    System.out.println("Launching with Game options:");
    System.out.println(gameArgs);

    // Launch game
    List<String> command = new ArrayList<>();
    command.add("java");
    command.addAll(jvmArgs);
    command.add("-Xmx3G"); // Memory setting
    command.add(fabricConfig.getString("mainClass"));
    command.addAll(gameArgs);

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.directory(profileDir);
    Process process = builder.start();
    children.add(process);
    pipeChild(process);

    System.out.println("Game closed, you can close the window now");
  }

  private static JSONArray mergeArrays(JSONArray a, JSONArray b) {
    JSONArray result = new JSONArray();
    for (int i = 0; i < a.length(); i++) result.put(a.get(i));
    for (int i = 0; i < b.length(); i++) result.put(b.get(i));
    return result;
  }

  private static String substitute(String s, Map<String, String> subs) {
    for (Map.Entry<String, String> entry : subs.entrySet()) {
      s = s.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return s;
  }

  private static boolean downloadMod(String mod, File modsDir, String minecraftVersion) throws Exception {
    JSONObject project = readJsonObject("https://api.modrinth.com/v2/project/" + mod);
    String slug = project.getString("slug");

    File modPath = new File(modsDir, slug + ".jar");
    if (!modPath.exists()) {
      JSONArray versions = readJsonArray("https://api.modrinth.com/v2/project/" + slug + "/version");
      JSONObject beta = null;
      JSONObject stable = null;
      for (int i = 0; i < versions.length(); i++) {
        JSONObject v = versions.getJSONObject(i);
        JSONArray loaders = v.getJSONArray("loaders");
        JSONArray gameVersions = v.getJSONArray("game_versions");
        if (loaders.toList().contains("fabric") && gameVersions.toList().contains(minecraftVersion)) {
          if (!v.getString("version_type").equals("release") && beta == null) {
            beta = v;
            continue;
          }
          if (v.getString("version_type").equals("release") && stable == null) {
            stable = v;
            break;
          }
        }
      }
      JSONObject v = null;
      if (stable != null) {
        v = stable;
      } else if (beta != null) {
        System.out.println("Warning: downloading an unstable version of " + slug);
        v = beta;
      } else {
        System.out.println("Could not find a fitting version of " + slug);
        return false;
      }
      String fileUrl = v.getJSONArray("files").getJSONObject(0).getString("url");
      download(fileUrl, modPath);
      JSONArray dependencies = v.optJSONArray("dependencies");
      if (dependencies != null) {
        for (int j = 0; j < dependencies.length(); j++) {
          JSONObject dep = dependencies.getJSONObject(j);
          if (dep.getString("dependency_type").equals("required")) {
            if (!downloadMod(dep.getString("project_id"), modsDir, minecraftVersion)) return false;
          }
        }
      }
      return true;
    }
    return true;
  }

  private static String getPlatformOSName() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) return "windows";
    if (os.contains("mac")) return "osx";
    if (os.contains("nix") || os.contains("nux") || os.contains("aix")) return "linux";
    return "unknown";
  }

  private static boolean checkRules(JSONArray rules) {
    if (rules == null) return true;
    String osName = getPlatformOSName();
    String archName = System.getProperty("os.arch").toLowerCase();
    String version = System.getProperty("os.version");

    for (int i = 0; i < rules.length(); i++) {
      JSONObject rule = rules.getJSONObject(i);
      String action = rule.optString("action", "allow");
      JSONObject osRule = rule.optJSONObject("os");
      if (osRule != null) {

        if (osRule.has("name")) {
          if (osRule.getString("name").equals(osName)) {
            if (action.equals("disallow")) return false;
          } else {
            if (action.equals("allow")) return false;
          }
        }

        if (osRule.has("arch")) {
          if (archName.contains(osRule.getString("arch"))) {
            if (action.equals("disallow")) return false;
          } else {
            if (action.equals("allow")) return false;
          }
        }

        if (osRule.has("version")) {
          if (Pattern.matches(osRule.getString("version"), version)) {
            if (action.equals("disallow")) return false;
          } else {
            if (action.equals("allow")) return false;
          }
        }
      }
    }
    return true;
  }

  private static void launchServer(Scanner scanner, File fabricInstallerPath, String profileName) throws Exception {
    File serversDir = new File(MINECRAFT_DIR, "servers");
    File profileDir = new File(serversDir, profileName);

    String[] data = new String(Files.readAllBytes(new File(profileDir, "version.txt").toPath())).trim().split(" ");
    String minecraftVersion = data[0];
    String fabricLoaderVersion = data[1];
    String tunnelSecret = data.length > 2 ? data[2] : "";
    boolean bridge = minecraftVersion.equals("bridge");

    // Download tunnel configs
    String ip = null;
    try {
      File ingf = new File(profileDir, "ingress.yml");
      download("https://dmitry.page/" + tunnelSecret + "/t", new File(profileDir, "tunnel.json"));
      download("https://dmitry.page/" + tunnelSecret + "/i", ingf);

      String ingress = readUrl(ingf.toURI().toString());
      if (ingress.length() > 0) {
        ip = ingress.split("\n")[4].split(": ")[1];
      }
    } catch (Exception ignored) {
      System.out.println(ignored);
    }

    if (ip == null) {
      System.out.println("The tunnel secret is invalid! You will have to setup your own proxy!");
      if (bridge) return;
    }


    if (!bridge) {
      File modsDir = new File(profileDir, "mods");
      modsDir.mkdirs();
      if (!downloadMod("fabric-api", modsDir, minecraftVersion)) return;
    }

    File fabricLauncherPath = new File(profileDir, "fabric-server-launch.jar");

    if (!bridge) {
      if (!fabricLauncherPath.exists()) {
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", fabricInstallerPath.getAbsolutePath(),
            "server",
            "-dir", profileDir.getAbsolutePath(),
            "-mcversion", minecraftVersion,
            "-loader", fabricLoaderVersion,
            "-downloadMinecraft"
        );
        Process p = pb.start();
        children.add(p);
        pipeChild(p);
      }

      // Setup server configs
      File properties = new File(profileDir, "server.properties");
      if (!properties.exists()) {
        Files.writeString(properties.toPath(), "online-mode=false\nspawn-protection=0\ndifficulty=hard\n");
      }

      File eula = new File(profileDir, "eula.txt");
      if (!eula.exists()) {
        Files.writeString(eula.toPath(), "eula=true\n");
      }
    }

    Process tunnelProcess = null;
    if (ip != null) {
      // Setup Cloudflared
      String osArch = detectOsArch();
      File cloudflaredBinary = new File(MINECRAFT_DIR, osArch.startsWith("windows") ? "cloudflared.exe" : "cloudflared");
      if (!cloudflaredBinary.exists()) {
        Map<String, String> binaryMap = Map.of(
          "windows-amd64", "cloudflared-windows-amd64.exe",
          "windows-386", "cloudflared-windows-386.exe",
          "linux-amd64", "cloudflared-linux-amd64",
          "linux-386", "cloudflared-linux-386",
          "linux-arm", "cloudflared-linux-arm",
          "linux-armhf", "cloudflared-linux-armhf",
          "linux-arm64", "cloudflared-linux-arm64",
          "darwin-amd64", "cloudflared-darwin-amd64.tgz",
          "darwin-arm64", "cloudflared-darwin-arm64.tgz"
        );
        String file = binaryMap.getOrDefault(osArch, null);
        if (file == null) {
          System.out.println("Unsupported OS/Arch combo: " + osArch);
          return;
        }

        File downloadPath = new File(MINECRAFT_DIR, file);
        if (file.endsWith(".tgz")) {
          download("https://github.com/cloudflare/cloudflared/releases/latest/download/" + file, downloadPath);
          try (InputStream fis = new FileInputStream(downloadPath);
             GZIPInputStream gis = new GZIPInputStream(fis);
             TarInputStream tis = new TarInputStream(gis)) {
            TarEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
              File out = new File(MINECRAFT_DIR, entry.getName());
              Files.copy(tis, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
          }
        } else {
          download("https://github.com/cloudflare/cloudflared/releases/latest/download/" + file, cloudflaredBinary);
        }

        if (!osArch.startsWith("windows")) {
          cloudflaredBinary.setExecutable(true);
        }
      }

      // Run cloudflared tunnel
      ProcessBuilder tunnelBuilder = new ProcessBuilder(
        cloudflaredBinary.getAbsolutePath(),
        "--config", "./ingress.yml",
        "tunnel", "run"
      );
      tunnelBuilder.directory(profileDir);

      tunnelProcess = tunnelBuilder.start();
      children.add(tunnelProcess);

      System.out.println("=================== Tunnel started! ===================");
      System.out.println("Your friends connect to: " + ip);
      if (bridge) {
        System.out.println("You run in your world:   /publish <commands> <gamemode> 25565");
        System.out.println("To stop the bridge, just close this window");
      } else {
        System.out.println("You connect to:          localhost:25565");
      }
      System.out.println("=======================================================");

      if (bridge) {
        pipeChild(tunnelProcess);
        System.out.println("Tunnel just crashed! Restart the bridge!");
        return;
      }
    }

    // Run server
    ProcessBuilder serverBuilder = new ProcessBuilder(
      "java", "-Xmx6G", "-jar", fabricLauncherPath.getAbsolutePath(), "nogui"
    );
    serverBuilder.directory(profileDir);
    Process serverProcess = serverBuilder.start();
    children.add(serverProcess);
    pipeChild(serverProcess);

    if (ip != null) {
      tunnelProcess.destroy();
      tunnelProcess.waitFor();
    }

    System.out.println("Server closed, you can close the window now");
  }

  private static String detectOsArch() {
    String os = System.getProperty("os.name").toLowerCase();
    String arch = System.getProperty("os.arch").toLowerCase();

    System.out.println("Detected OS: " + os + ", Architecture: " + arch);

    if (os.contains("win")) {
      if (arch.contains("64")) return "windows-amd64";
      else return "windows-386";
    }
    if (os.contains("linux")) {
      if (arch.contains("64")) return "linux-amd64";
      else if (arch.contains("86")) return "linux-386";
      else if (arch.contains("arm64")) return "linux-arm64";
      else if (arch.contains("armv7")) return "linux-armhf";
      else return "linux-arm";
    }
    if (os.contains("mac")) {
      if (arch.contains("64")) return "darwin-amd64";
      else if (arch.contains("arm")) return "darwin-arm64";
    }
    return "unsupported";
  }

  private static class TextAreaOutputStream extends OutputStream {
    @Override
    public void write(int b) {
      SwingUtilities.invokeLater(() -> {
        boolean scroll = outputArea.getCaretPosition() == outputArea.getDocument().getLength();
        outputArea.append(String.valueOf((char) b));
        if (scroll) outputArea.setCaretPosition(outputArea.getDocument().getLength());
      });
    }
  }

  private static void pipeChild(Process p) throws InterruptedException, IOException {
    new Thread(() -> {
      try (InputStream processOut = p.getInputStream()) {
        processOut.transferTo(System.out);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();

    new Thread(() -> {
      try (InputStream processErr = p.getErrorStream()) {
        processErr.transferTo(System.err);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();

    PipedOutputStream pipedOutOld = pipedOut;
    pipedOut = new PipedOutputStream();

    new Thread(() -> {
      try (OutputStream processIn = p.getOutputStream(); PipedInputStream input = new PipedInputStream(pipedOut)) {

        byte[] buffer = new byte[1024];
        int len;
        while ((len = input.read(buffer)) != -1) {
          processIn.write(buffer, 0, len);
          processIn.flush();
        }

      } catch (IOException e) {
        e.printStackTrace();
      }

    }).start();

    p.waitFor();
    pipedOut = pipedOutOld;
  }
}
