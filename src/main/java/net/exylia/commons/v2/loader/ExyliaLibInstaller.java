package net.exylia.commons.v2.loader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Puts ExyliaLib on the server for plugins that still run on ExyliaCommons.
 *
 * <p>Commons is shaded into each plugin, so it cannot depend on the library
 * and does not use it. This exists only so that a server running today's
 * plugins ends up with ExyliaLib installed; from there the library's own
 * updater keeps it current, and a plugin migrated later finds it already
 * there.
 *
 * <p>It does nothing when the library is already present, whether loaded as a
 * plugin or merely sitting in {@code plugins/}. There is no version check:
 * updating an existing install is the library's own job, not ours.
 *
 * <p>Runs once per server rather than once per plugin, off the main thread,
 * and never prevents anything from starting: a plugin that does not use the
 * library must not care that a download failed.
 *
 * @since 1.0.1
 */
public final class ExyliaLibInstaller {

    private static final String PLUGIN_NAME = "ExyliaLib";
    private static final String JAR_NAME = "ExyliaLib.jar";

    /**
     * Served from the repository's default branch: it updates with the same
     * push that publishes a release, and points at assets on the same host.
     * The library's own updater reads this exact file.
     */
    private static final String MANIFEST_URL =
        "https://raw.githubusercontent.com/DiGround-s/ExyliaLib/main/lib-manifest.json";

    private static final int TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_TIMEOUT_MS = 30_000;

    /**
     * Commons is shaded, so every plugin carries its own copy of this class and
     * its own copy of this flag. What actually keeps a second plugin from
     * downloading the same jar is the on-disk check inside {@link #install}:
     * the first one to finish leaves the file the others find.
     *
     * <p>This only stops one plugin from starting several threads.
     */
    private static final AtomicBoolean started = new AtomicBoolean();

    private ExyliaLibInstaller() {
        throw new AssertionError("No instances.");
    }

    /**
     * Installs ExyliaLib in the background if the server does not have it.
     *
     * <p>Returns immediately. Failures are logged and otherwise ignored.
     *
     * @param plugin the plugin asking, used for its logger and its data folder
     */
    public static void ensureInstalled(Plugin plugin) {
        if (!started.compareAndSet(false, true)) return;

        // Already running: nothing to do, and nothing to check. Keeping an
        // existing install up to date is the library's own business.
        if (Bukkit.getPluginManager().getPlugin(PLUGIN_NAME) != null) return;

        Thread thread = new Thread(() -> install(plugin.getLogger(), pluginsDir(plugin)),
            "ExyliaLib-Installer");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * The server's {@code plugins/} directory.
     *
     * <p>Derived from the plugin's own data folder rather than from a system
     * property: a server can be started from any working directory, and
     * {@code plugins/} is not always beside it.
     */
    private static Path pluginsDir(Plugin plugin) {
        return plugin.getDataFolder().getAbsoluteFile().toPath().getParent();
    }

    private static void install(Logger log, Path pluginsDir) {
        Path jar = pluginsDir.resolve(JAR_NAME);

        // On disk but not loaded: a sibling plugin staged it earlier this boot,
        // or the admin dropped it in. Either way the next start picks it up.
        if (Files.exists(jar)) return;

        String manifest;
        try {
            manifest = fetch();
        } catch (IOException e) {
            // A server with no outbound network, or GitHub having a bad
            // minute. Nothing is broken: this is not the plugin's own work.
            log.log(Level.WARNING,
                "Could not reach the ExyliaLib manifest — skipping install, will retry next start.");
            return;
        }

        String version = latestVersion(manifest);
        if (version == null) {
            log.warning("The ExyliaLib manifest listed no version — skipping install.");
            return;
        }

        String url = valueOf(manifest, "url", version);
        String sha256 = valueOf(manifest, "sha256", version);
        if (url == null || sha256 == null) {
            log.warning("The ExyliaLib manifest entry for " + version
                + " is incomplete — skipping install.");
            return;
        }

        log.info("Installing ExyliaLib " + version + " — one-time setup...");
        try {
            download(url, sha256, jar);
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not install ExyliaLib: " + e.getMessage(), e);
            return;
        }
        log.info("ExyliaLib " + version + " installed — it will be active after a restart.");
    }

    // ---- manifest ----

    private static String fetch() throws IOException {
        try {
            HttpURLConnection conn = open(MANIFEST_URL, TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new IOException("manifest fetch returned HTTP " + conn.getResponseCode());
            }
            try (InputStream in = conn.getInputStream()) {
                return new String(readAll(in), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Reads {@code latest.major1} from the manifest.
     *
     * <p>Parsed by hand because Commons cannot assume Gson is on the server's
     * classpath, and adding a dependency to install a jar would be a poor
     * trade. The manifest is generated, so its shape is stable.
     */
    static String latestVersion(String json) {
        int latest = json.indexOf("\"latest\"");
        if (latest < 0) return null;
        // Searched after "latest" so a version entry named the same cannot win.
        int key = json.indexOf("\"major1\"", latest);
        if (key < 0) return null;
        return quotedAfter(json, key + "\"major1\"".length());
    }

    /**
     * Reads {@code key} from the manifest entry describing {@code version}.
     *
     * @return the value, or {@code null} if either is missing
     */
    static String valueOf(String json, String key, String version) {
        int entry = json.indexOf('"' + version + '"');
        if (entry < 0) return null;
        int found = json.indexOf('"' + key + '"', entry);
        if (found < 0) return null;
        return quotedAfter(json, found + key.length() + 2);
    }

    /** Returns the next double-quoted string at or after {@code from}. */
    private static String quotedAfter(String json, int from) {
        int colon = json.indexOf(':', from);
        if (colon < 0) return null;
        int open = json.indexOf('"', colon + 1);
        if (open < 0) return null;
        int close = json.indexOf('"', open + 1);
        if (close < 0) return null;
        return json.substring(open + 1, close);
    }

    // ---- download ----

    private static void download(String url, String expectedSha256, Path dest) throws IOException {
        Path dir = dest.getParent();
        Path tmp = Files.createTempFile(dir, "ExyliaLib", ".tmp");
        try {
            HttpURLConnection conn = open(url, DOWNLOAD_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            if (conn.getResponseCode() != 200) {
                throw new IOException("download returned HTTP " + conn.getResponseCode());
            }
            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }

            String actual = sha256(tmp);
            if (!actual.equalsIgnoreCase(expectedSha256)) {
                throw new IOException("SHA-256 mismatch: expected " + expectedSha256
                    + ", got " + actual);
            }

            // Moved into place only once the bytes are known good, so a failed
            // download can never leave a half-written jar the server would try
            // to load on its next start.
            try {
                Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            throw e instanceof IOException io ? io : new IOException(e.getMessage(), e);
        }
    }

    private static HttpURLConnection open(String url, int readTimeout) throws IOException {
        try {
            HttpURLConnection conn =
                (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(readTimeout);
            conn.setRequestProperty("User-Agent", "ExyliaCommons-LibInstaller/1.0");
            return conn;
        } catch (IllegalArgumentException e) {
            throw new IOException("bad URL: " + url, e);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(Files.readAllBytes(file))) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is required by the platform", e);
        }
    }
}
