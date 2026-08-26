package ux.justsadnyx.droiddeck;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdatesFragment extends Fragment {

    private TextView status;
    private ProgressBar dlBar;
    private MaterialButton checkBtn;
    private String pendingApkPath;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_updates, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        status = v.findViewById(R.id.u_status);
        dlBar = v.findViewById(R.id.u_dl_bar);
        checkBtn = v.findViewById(R.id.u_check);

        TextView version = v.findViewById(R.id.u_version);
        try {
            String vName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            version.setText("Installed: v" + vName);
        } catch (Exception e) {
            version.setText("Installed: v?");
        }

        MaterialButton releasesBtn = v.findViewById(R.id.u_releases);
        MaterialButton termsBtn = v.findViewById(R.id.u_terms);
        MaterialButton licenseBtn = v.findViewById(R.id.u_license);

        checkBtn.setOnClickListener(btn -> checkForUpdate());
        releasesBtn.setOnClickListener(btn -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/justsadnyx-ux/DroidDeck/releases")));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Cannot open browser", Toast.LENGTH_SHORT).show();
            }
        });

        termsBtn.setOnClickListener(btn -> showTerms());
        licenseBtn.setOnClickListener(btn -> showLicense());

        if (savedInstanceState != null) {
            pendingApkPath = savedInstanceState.getString("pending_apk");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pendingApkPath != null && getContext() != null
                && getContext().getPackageManager().canRequestPackageInstalls()) {
            String path = pendingApkPath;
            pendingApkPath = null;
            promptInstall(path);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingApkPath != null) outState.putString("pending_apk", pendingApkPath);
    }

    private void showTerms() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Terms of Service")
                .setMessage("DroidDeck \u2014 Terms of Service\n\n" +
                        "Last updated: August 2026\nVersion: 1.0\n\n" +
                        "1. ACCEPTANCE OF TERMS\n" +
                        "By downloading, installing, or using DroidDeck (\u201cthe App\u201d), you agree to be bound by these Terms of Service. If you do not agree, do not use the App.\n\n" +
                        "2. DESCRIPTION OF SERVICE\n" +
                        "DroidDeck is a free, open-source Android toolkit that provides device monitoring, file management, app management, network utilities, and system controls. The App runs entirely on your device.\n\n" +
                        "3. USE AT YOUR OWN RISK\n" +
                        "The App is provided \u201cas is\u201d without warranty of any kind. You are solely responsible for your use of the App, including any modifications it makes to system settings (brightness, etc.) or actions it performs (app disable, uninstall, etc.). The developers are not liable for any damage, data loss, or security issues.\n\n" +
                        "4. PERMISSIONS\n" +
                        "The App requests permissions to provide its features:\n" +
                        "\u2022 Storage: file browsing and sharing\n" +
                        "\u2022 Camera: flashlight control\n" +
                        "\u2022 Notifications: update alerts and server status\n" +
                        "\u2022 Network: IP lookup, ping, DNS, web server\n" +
                        "\u2022 Write Settings: brightness control\n" +
                        "\u2022 Install packages: self-update and terms APK install\n\n" +
                        "No personal data is collected, transmitted, or stored by the App.\n\n" +
                        "5. AUTO-UPDATE CHECKS\n" +
                        "The App periodically checks GitHub for new versions. Only the App version number is compared \u2014 no personal information is transmitted.\n\n" +
                        "6. WEB FILE SERVER\n" +
                        "The optional web file server feature shares your device storage over the local network. Use it only on trusted networks. The developers are not responsible for unauthorized access.\n\n" +
                        "7. OPEN SOURCE\n" +
                        "DroidDeck is released under the MIT License. Source code is available on GitHub.\n\n" +
                        "8. TERMINATION\n" +
                        "You may stop using the App at any time by uninstalling it. All local data will be removed.\n\n" +
                        "9. CHANGES\n" +
                        "These terms may be updated. Continued use constitutes acceptance of changes.\n\n" +
                        "10. CONTACT\n" +
                        "https://github.com/justsadnyx-ux/DroidDeck/issues")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLicense() {
        new AlertDialog.Builder(requireContext())
                .setTitle("MIT License")
                .setMessage("Copyright (c) 2026 justsadnyx\n\n" +
                        "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n" +
                        "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n" +
                        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void checkForUpdate() {
        checkBtn.setEnabled(false);
        status.setText("Checking for updates...");
        dlBar.setVisibility(View.GONE);

        exec.execute(() -> {
            try {
                String json = fetchUrl("https://api.github.com/repos/justsadnyx-ux/DroidDeck/releases/latest");
                if (json == null) throw new Exception("Network error \u2014 check your connection");

                String tagName = extractBetween(json, "\"tag_name\":\"", "\"");
                if (tagName == null) throw new Exception("Could not parse release data");
                String latestVersion = tagName.replaceFirst("^v", "");

                String currentVersion = "?";
                try {
                    currentVersion = requireContext().getPackageManager()
                            .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                } catch (Exception ignored) {}

                if (isNewer(latestVersion, currentVersion)) {
                    String apkUrl = findApkUrl(json);
                    String finalCurrentVersion = currentVersion;
                    if (apkUrl == null) {
                        main.post(() -> {
                            status.setText("v" + latestVersion + " available (current: v" + finalCurrentVersion + ")\nNo downloadable APK found.");
                            checkBtn.setEnabled(true);
                        });
                        return;
                    }
                    String fv = latestVersion;
                    main.post(() -> {
                        status.setText("v" + fv + " available \u2014 downloading...");
                        dlBar.setVisibility(View.VISIBLE);
                        dlBar.setProgress(0);
                    });
                    downloadApk(apkUrl, latestVersion);
                } else {
                    String cv = currentVersion;
                    main.post(() -> {
                        status.setText("Up to date \u2014 v" + cv + " is the latest version.");
                        checkBtn.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                main.post(() -> {
                    status.setText("Update check failed: " + e.getMessage());
                    checkBtn.setEnabled(true);
                });
            }
        });
    }

    private void downloadApk(String urlStr, String version) {
        try {
            File cacheDir = new File(requireContext().getCacheDir(), "updates");
            cacheDir.mkdirs();
            File apkFile = new File(cacheDir, "DroidDeck-" + version + ".apk");
            if (apkFile.exists()) apkFile.delete();

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "DroidDeck-Android/1.0");
            conn.connect();

            if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());

            long total = conn.getContentLength();
            InputStream in = conn.getInputStream();
            OutputStream out = new FileOutputStream(apkFile);

            byte[] buf = new byte[65536];
            long downloaded = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                downloaded += n;
                if (total > 0) {
                    int pct = (int) (downloaded * 100 / total);
                    main.post(() -> dlBar.setProgress(pct));
                }
            }
            out.close();
            in.close();
            conn.disconnect();

            String fv = version;
            main.post(() -> {
                status.setText("Downloaded v" + fv + ". Tap to install.");
                dlBar.setVisibility(View.GONE);

                if (!requireContext().getPackageManager().canRequestPackageInstalls()) {
                    pendingApkPath = apkFile.getAbsolutePath();
                    Toast.makeText(requireContext(), "Allow installs from this source first.", Toast.LENGTH_LONG).show();
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + requireContext().getPackageName())));
                    } catch (Exception ignored) {}
                    return;
                }
                promptInstall(apkFile.getAbsolutePath());
            });
        } catch (Exception e) {
            main.post(() -> {
                status.setText("Download failed: " + e.getMessage());
                dlBar.setVisibility(View.GONE);
                checkBtn.setEnabled(true);
            });
        }
    }

    private void promptInstall(String path) {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", new File(path));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Install failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String fetchUrl(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "DroidDeck-Android/1.0");
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractBetween(String json, String start, String end) {
        int i = json.indexOf(start);
        if (i < 0) return null;
        i += start.length();
        int j = json.indexOf(end, i);
        if (j < 0) return null;
        return json.substring(i, j);
    }

    private String findApkUrl(String json) {
        int searchFrom = 0;
        while (true) {
            int nameIdx = json.indexOf("\"name\"", searchFrom);
            if (nameIdx < 0) break;
            String name = extractBetween(json.substring(nameIdx), "\"", "\"");
            if (name != null && name.endsWith(".apk")) {
                int dlIdx = json.indexOf("\"browser_download_url\"", nameIdx);
                if (dlIdx >= 0) {
                    String url = extractBetween(json.substring(dlIdx), "\"browser_download_url\":\"", "\"");
                    if (url != null) return url;
                }
            }
            searchFrom = nameIdx + 6;
        }
        return null;
    }

    private boolean isNewer(String a, String b) {
        try {
            String[] aa = a.split("\\.");
            String[] bb = b.split("\\.");
            int len = Math.max(aa.length, bb.length);
            for (int i = 0; i < len; i++) {
                int ai = i < aa.length ? Integer.parseInt(aa[i]) : 0;
                int bi = i < bb.length ? Integer.parseInt(bb[i]) : 0;
                if (ai > bi) return true;
                if (ai < bi) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
