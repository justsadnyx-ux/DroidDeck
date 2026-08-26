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

        checkBtn.setOnClickListener(btn -> checkForUpdate(false));
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
                .setMessage("DroidDeck — Terms of Service\n\n" +
                        "Last updated: August 2026\n\n" +
                        "1. ACCEPTANCE OF TERMS\n" +
                        "By using DroidDeck, you agree to these terms. If you do not agree, do not use the app.\n\n" +
                        "2. DESCRIPTION OF SERVICE\n" +
                        "DroidDeck is a free, open-source Android toolkit. It provides device monitoring, file management, app management, network utilities, and system controls.\n\n" +
                        "3. USE AT YOUR OWN RISK\n" +
                        "DroidDeck is provided \"as is\" without warranty. You are responsible for your use of the app. The developers are not liable for any damage, data loss, or security issues.\n\n" +
                        "4. PERMISSIONS\n" +
                        "DroidDeck requests permissions to provide its features (storage access, camera for torch, notifications, etc.). We do not collect, transmit, or store any personal data.\n\n" +
                        "5. OPEN SOURCE\n" +
                        "DroidDeck is open source software. You may view, modify, and distribute the source code under the MIT License.\n\n" +
                        "6. UPDATES\n" +
                        "DroidDeck may check for updates via GitHub. No personal information is sent during this process.\n\n" +
                        "7. THIRD-PARTY SERVICES\n" +
                        "DroidDeck uses GitHub (api.github.com, github.com) for update checking and distribution. Their terms apply when you interact with their services.\n\n" +
                        "8. TERMINATION\n" +
                        "You may stop using DroidDeck at any time. Uninstalling the app removes all local data.\n\n" +
                        "9. CHANGES TO TERMS\n" +
                        "These terms may be updated. Continued use constitutes acceptance of any changes.\n\n" +
                        "10. CONTACT\n" +
                        "Report issues at https://github.com/justsadnyx-ux/DroidDeck/issues")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLicense() {
        new AlertDialog.Builder(requireContext())
                .setTitle("MIT License")
                .setMessage("MIT License\n\nCopyright (c) 2026 justsadnyx\n\n" +
                        "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n" +
                        "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n" +
                        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void checkForUpdate(boolean silent) {
        checkBtn.setEnabled(false);
        status.setText("Checking for updates...");

        exec.execute(() -> {
            try {
                String json = fetchUrl("https://api.github.com/repos/justsadnyx-ux/DroidDeck/releases/latest");
                if (json == null) throw new Exception("Network error");

                String tagName = extractJsonString(json, "tag_name");
                String version = tagName != null ? tagName.replaceFirst("^v", "") : null;
                if (version == null) throw new Exception("No version found");

                String currentVersion = "?";
                try {
                    currentVersion = requireContext().getPackageManager()
                            .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                } catch (Exception ignored) {}

                if (isSameOrNewer(version, currentVersion)) {
                    String cv = currentVersion;
                    main.post(() -> {
                        status.setText("Up to date (v" + cv + ")");
                        checkBtn.setEnabled(true);
                    });
                    return;
                }

                String apkUrl = findApkUrl(json);
                if (apkUrl == null) throw new Exception("Release has no APK");
                downloadAndInstall(apkUrl, version);

            } catch (Exception e) {
                main.post(() -> {
                    status.setText("Update check failed: " + e.getMessage());
                    checkBtn.setEnabled(true);
                });
            }
        });
    }

    private void downloadAndInstall(String urlStr, String version) {
        main.post(() -> {
            status.setText("Downloading v" + version + "...");
            dlBar.setVisibility(View.VISIBLE);
            dlBar.setProgress(0);
        });

        try {
            File cacheDir = new File(requireContext().getCacheDir(), "updates");
            cacheDir.mkdirs();
            File apkFile = new File(cacheDir, "DroidDeck-" + version + ".apk");
            if (apkFile.exists()) apkFile.delete();

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
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

            main.post(() -> {
                status.setText("Downloaded. Installing...");
                dlBar.setVisibility(View.GONE);

                if (!requireContext().getPackageManager().canRequestPackageInstalls()) {
                    pendingApkPath = apkFile.getAbsolutePath();
                    Toast.makeText(requireContext(), "Allow installs from this source first.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + requireContext().getPackageName())));
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
            conn.setRequestProperty("User-Agent", "DroidDeck-Android");
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

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    private String findApkUrl(String json) {
        int idx = 0;
        while (true) {
            int nameIdx = json.indexOf("\"name\"", idx);
            if (nameIdx < 0) break;
            String name = extractJsonString(json.substring(nameIdx), "name");
            if (name != null && name.endsWith(".apk")) {
                int dlIdx = json.indexOf("\"browser_download_url\"", nameIdx);
                if (dlIdx >= 0) {
                    return extractJsonString(json.substring(dlIdx), "browser_download_url");
                }
            }
            idx = nameIdx + 10;
        }
        return null;
    }

    private boolean isSameOrNewer(String a, String b) {
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
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
