package ux.justsadnyx.droiddeck;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ToolsFragment extends Fragment {

    private TextView serverStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tools, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        MaterialSwitch serverSwitch = v.findViewById(R.id.t_server_switch);
        serverStatus = v.findViewById(R.id.t_server_status);
        MaterialButton flashBtn = v.findViewById(R.id.t_flash);
        MaterialButton vibrateBtn = v.findViewById(R.id.t_vibrate);
        MaterialButton sosBtn = v.findViewById(R.id.t_sos);
        Slider brightness = v.findViewById(R.id.t_brightness);
        MaterialButton brightnessPerm = v.findViewById(R.id.t_brightness_perm);
        EditText pingHost = v.findViewById(R.id.t_ping_host);
        MaterialButton pingBtn = v.findViewById(R.id.t_ping);
        TextView pingOut = v.findViewById(R.id.t_ping_out);
        EditText hashInput = v.findViewById(R.id.t_hash_input);
        MaterialButton hashMd5 = v.findViewById(R.id.t_hash_md5);
        MaterialButton hashSha = v.findViewById(R.id.t_hash_sha);
        TextView hashOut = v.findViewById(R.id.t_hash_out);
        MaterialButton wifiBtn = v.findViewById(R.id.t_wifi);
        MaterialButton btBtn = v.findViewById(R.id.t_bluetooth);
        MaterialButton displayBtn = v.findViewById(R.id.t_display);
        MaterialButton batteryBtn = v.findViewById(R.id.t_battery);
        MaterialButton appsBtn = v.findViewById(R.id.t_apps);
        MaterialButton notifPerm = v.findViewById(R.id.t_notif_perm);
        MaterialButton clipBtn = v.findViewById(R.id.t_clip_clear);
        MaterialButton shareBtn = v.findViewById(R.id.t_share_report);
        MaterialButton vibrateLong = v.findViewById(R.id.t_vibrate_long);

        updateServerStatus();
        serverSwitch.setOnCheckedChangeListener((btn, checked) -> {
            Context ctx = requireContext();
            Intent intent = new Intent(ctx, HttpServerService.class);
            if (checked) {
                if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 42);
                }
                ContextCompat.startForegroundService(ctx, intent);
            } else {
                ctx.stopService(intent);
            }
            serverStatus.postDelayed(this::updateServerStatus, 800);
        });

        flashBtn.setOnClickListener(btn -> toggleTorch());
        vibrateBtn.setOnClickListener(btn -> vibrate(300));
        vibrateLong.setOnClickListener(btn -> vibrate(2000));
        sosBtn.setOnClickListener(btn -> sosPattern());

        int currentBrightness = Settings.System.getInt(requireContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 128);
        brightness.setValue(Math.min(currentBrightness, 255));
        brightness.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser || !Settings.System.canWrite(requireContext())) return;
            Settings.System.putInt(requireContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, (int) value);
        });
        brightnessPerm.setOnClickListener(btn -> {
            if (!Settings.System.canWrite(requireContext())) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:" + requireContext().getPackageName())));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Write settings unavailable", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Permission already granted", Toast.LENGTH_SHORT).show();
            }
        });

        pingBtn.setOnClickListener(btn -> {
            String host = pingHost.getText().toString().trim();
            if (host.isEmpty()) host = "1.1.1.1";
            ping(host, pingOut);
        });

        hashMd5.setOnClickListener(btn -> computeHash(hashInput.getText().toString(), "MD5", hashOut));
        hashSha.setOnClickListener(btn -> computeHash(hashInput.getText().toString(), "SHA-256", hashOut));

        wifiBtn.setOnClickListener(btn -> openPanel(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS));
        btBtn.setOnClickListener(btn -> open(null, Settings.ACTION_BLUETOOTH_SETTINGS));
        displayBtn.setOnClickListener(btn -> open(null, Settings.ACTION_DISPLAY_SETTINGS));
        batteryBtn.setOnClickListener(btn -> open(null, Settings.ACTION_BATTERY_SAVER_SETTINGS));
        appsBtn.setOnClickListener(btn -> open(null, Settings.ACTION_APPLICATION_SETTINGS));

        notifPerm.setOnClickListener(btn -> {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 43);
            } else {
                Toast.makeText(requireContext(), "Notifications allowed", Toast.LENGTH_SHORT).show();
            }
        });

        clipBtn.setOnClickListener(btn -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("", ""));
                Toast.makeText(requireContext(), "Clipboard cleared", Toast.LENGTH_SHORT).show();
            }
        });

        shareBtn.setOnClickListener(btn -> shareReport());
    }

    private void shareReport() {
        try {
            String report = Util.deviceReport(requireContext());
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, report);
            startActivity(Intent.createChooser(intent, "Share device report"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPanel(String panelAction, String fallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startActivity(new Intent(panelAction));
                return;
            } catch (Exception ignored) {}
        }
        open(null, fallback);
    }

    private void open(String data, String action) {
        try {
            Intent intent = new Intent(action);
            if (data != null) intent.setData(Uri.parse(data));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Setting unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateServerStatus() {
        boolean running = HttpServerService.isRunning();
        String ips = Util.localIpAddresses();
        if (running) {
            if (ips.equals("No local IP found")) {
                serverStatus.setText("Running — connect to Wi-Fi, then browse to http://<phone-ip>:" + HttpServerService.PORT);
            } else {
                String firstIp = ips.split("\n")[0].trim().split("\\s+")[0];
                serverStatus.setText("Running — open on your PC:\nhttp://" + firstIp + ":" + HttpServerService.PORT
                        + "\nServes your phone's storage to any browser on this network.");
            }
        } else {
            serverStatus.setText("Off — flip the switch to share files with any PC on your Wi-Fi.");
        }
    }

    private void toggleTorch() {
        try {
            CameraManager cm = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
            for (String id : cm.getCameraIdList()) {
                Boolean hasFlash = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    boolean newState = !torchOn;
                    cm.setTorchMode(id, newState);
                    torchOn = newState;
                    Toast.makeText(requireContext(), torchOn ? "Flashlight ON" : "Flashlight OFF", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(requireContext(), "No flashlight found", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Torch failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean torchOn = false;

    private void vibrate(long ms) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) requireContext()
                    .getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) {
            Toast.makeText(requireContext(), "No vibrator", Toast.LENGTH_SHORT).show();
            return;
        }
        vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void sosPattern() {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) requireContext()
                    .getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) {
            Toast.makeText(requireContext(), "No vibrator", Toast.LENGTH_SHORT).show();
            return;
        }
        long[] pattern = {
            0, 200, 100, 200, 100, 200,    // S: dot dot dot
            200, 600, 100,                  // O: dash dash dash
            600, 200, 100, 200, 100, 200,   // S: dot dot dot
            200
        };
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        Toast.makeText(requireContext(), "SOS pattern", Toast.LENGTH_SHORT).show();
    }

    private void ping(final String host, final TextView output) {
        output.setText("Pinging " + host + "...");
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"ping", "-c", "4", "-W", "2", host});
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                reader.close();
                process.waitFor();
            } catch (Exception e) {
                sb.append("ping failed: ").append(e.getMessage());
            }
            String result = sb.length() == 0 ? "No response" : sb.toString().trim();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> output.setText(result));
        }).start();
    }

    private void computeHash(String text, String algorithm, TextView output) {
        if (text.isEmpty()) {
            output.setText("Enter text first");
            return;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashed = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            output.setText(algorithm + ": " + sb);
        } catch (Exception e) {
            output.setText("Hash failed: " + e.getMessage());
        }
    }
}
