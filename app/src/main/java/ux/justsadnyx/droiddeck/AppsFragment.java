package ux.justsadnyx.droiddeck;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppsFragment extends Fragment {

    private AppAdapter adapter;
    private List<AppEntry> allApps = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        RecyclerView list = v.findViewById(R.id.a_list);
        EditText search = v.findViewById(R.id.a_search);

        adapter = new AppAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { filter(s.toString()); }
        });

        loadApps();
    }

    private void loadApps() {
        PackageManager pm = requireContext().getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(
                PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
        allApps.clear();
        for (PackageInfo pi : packages) {
            ApplicationInfo ai = pi.applicationInfo;
            if (ai == null) continue;
            String label = String.valueOf(pm.getApplicationLabel(ai));
            long size = 0;
            try {
                size = new File(ai.sourceDir).length();
            } catch (Exception ignored) {}
            boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean enabled = ai.enabled;
            allApps.add(new AppEntry(ai.packageName, label, pi.versionName, size, ai.sourceDir, system, enabled));
        }
        Collections.sort(allApps, (a, b) -> {
            if (a.system != b.system) return a.system ? 1 : -1;
            return a.label.compareToIgnoreCase(b.label);
        });
        filter("");
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase(Locale.US);
        List<AppEntry> shown = new ArrayList<>();
        for (AppEntry e : allApps) {
            if (q.isEmpty() || e.label.toLowerCase(Locale.US).contains(q) || e.pkg.contains(q))
                shown.add(e);
        }
        adapter.setItems(shown);
    }

    private void showActions(AppEntry entry) {
        PackageManager pm = requireContext().getPackageManager();
        boolean isSystemApp = entry.system;

        List<String> actions = new ArrayList<>();
        actions.add("Open app");
        actions.add("App info");
        actions.add("Export APK");
        if (!isSystemApp) actions.add("Uninstall");
        if (!isSystemApp) actions.add("Force stop");
        actions.add(entry.enabled ? "Disable app" : "Enable app");

        CharSequence[] arr = actions.toArray(new CharSequence[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle(entry.label)
                .setMessage(entry.pkg + "\nv" + entry.version + " · " + Util.humanSize(entry.size)
                        + (isSystemApp ? "\nSystem app" : "")
                        + (entry.enabled ? "" : "\nDisabled"))
                .setItems(arr, (d, which) -> {
                    String action = arr[which].toString();
                    if (action.equals("Open app")) launch(entry.pkg);
                    else if (action.equals("App info")) openDetails(entry.pkg);
                    else if (action.equals("Export APK")) exportApk(entry);
                    else if (action.equals("Uninstall")) uninstall(entry.pkg);
                    else if (action.equals("Force stop")) forceStop(entry.pkg);
                    else if (action.equals("Disable app")) toggleEnabled(entry, false);
                    else if (action.equals("Enable app")) toggleEnabled(entry, true);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void launch(String pkg) {
        Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent == null) {
            Toast.makeText(requireContext(), "This app cannot be launched", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openDetails(String pkg) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not open app info", Toast.LENGTH_SHORT).show();
        }
    }

    private void uninstall(String pkg) {
        try {
            startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Uninstaller unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void forceStop(String pkg) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requireContext().getSystemService(android.app.ActivityManager.class).killBackgroundProcesses(pkg);
            } else {
                android.app.ActivityManager am = (android.app.ActivityManager) requireContext()
                        .getSystemService(Context.ACTIVITY_SERVICE);
                am.killBackgroundProcesses(pkg);
            }
            Toast.makeText(requireContext(), "Force stopped " + pkg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Force stop failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleEnabled(AppEntry entry, boolean enable) {
        try {
            PackageManager pm = requireContext().getPackageManager();
            pm.setApplicationEnabledSetting(entry.pkg,
                    enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            entry.enabled = enable;
            Toast.makeText(requireContext(),
                    enable ? entry.label + " enabled" : entry.label + " disabled",
                    Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot " + (enable ? "enable" : "disable")
                    + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportApk(AppEntry entry) {
        File source = new File(entry.apkPath);
        if (!source.exists()) {
            Toast.makeText(requireContext(), "APK not found", Toast.LENGTH_SHORT).show();
            return;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        String name = entry.label.replaceAll("[^A-Za-z0-9._-]", "_") + "-" + stamp + ".apk";
        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
                    Uri uri = requireContext().getContentResolver()
                            .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new IOException("MediaStore insert failed");
                    try (InputStream in = new FileInputStream(source);
                         OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                        if (out == null) throw new IOException("Stream unavailable");
                        copy(in, out);
                    }
                } else {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists()) dir.mkdirs();
                    File dest = new File(dir, name);
                    try (InputStream in = new FileInputStream(source);
                         OutputStream out = new FileOutputStream(dest)) {
                        copy(in, out);
                    }
                }
                postToast("Saved " + name + " to Downloads");
            } catch (Exception e) {
                postToast("Export failed: " + e.getMessage());
            }
        }).start();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private void postToast(String message) {
        main.post(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
    }

    static class AppEntry {
        final String pkg;
        final String label;
        final String version;
        final long size;
        final String apkPath;
        final boolean system;
        boolean enabled;

        AppEntry(String pkg, String label, String version, long size, String apkPath, boolean system, boolean enabled) {
            this.pkg = pkg;
            this.label = label;
            this.version = version == null ? "?" : version;
            this.size = size;
            this.apkPath = apkPath;
            this.system = system;
            this.enabled = enabled;
        }
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

        private final List<AppEntry> items = new ArrayList<>();
        private PackageManager pm;

        void setItems(List<AppEntry> apps) {
            items.clear();
            items.addAll(apps);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            pm = parent.getContext().getPackageManager();
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            AppEntry entry = items.get(position);
            h.name.setText(entry.label);
            String meta = entry.pkg + " · v" + entry.version + " · " + Util.humanSize(entry.size);
            if (entry.system) meta += " · system";
            h.meta.setText(meta);
            h.itemView.setAlpha(entry.enabled ? 1.0f : 0.5f);
            try {
                h.icon.setImageDrawable(pm.getApplicationIcon(entry.pkg));
            } catch (Exception e) {
                h.icon.setImageResource(R.drawable.ic_apps);
            }
            h.itemView.setOnClickListener(btn -> showActions(entry));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView meta;

            Holder(View v) {
                super(v);
                icon = v.findViewById(R.id.ia_icon);
                name = v.findViewById(R.id.ia_name);
                meta = v.findViewById(R.id.ia_meta);
            }
        }
    }
}
