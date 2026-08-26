package ux.justsadnyx.droiddeck;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        TextView device = v.findViewById(R.id.d_device);
        TextView androidVer = v.findViewById(R.id.d_android);
        TextView cpu = v.findViewById(R.id.d_cpu);
        TextView screen = v.findViewById(R.id.d_screen);
        TextView ram = v.findViewById(R.id.d_ram);
        ProgressBar ramBar = v.findViewById(R.id.d_ram_bar);
        TextView storage = v.findViewById(R.id.d_storage);
        ProgressBar storageBar = v.findViewById(R.id.d_storage_bar);
        TextView battery = v.findViewById(R.id.d_battery);
        TextView uptime = v.findViewById(R.id.d_uptime);
        TextView ipLocal = v.findViewById(R.id.d_ip_local);
        TextView ipPublic = v.findViewById(R.id.d_ip_public);
        MaterialButton refresh = v.findViewById(R.id.d_refresh);

        device.setText(Build.MANUFACTURER + " " + Build.MODEL);
        String patch = Build.VERSION.SECURITY_PATCH != null ? " · security " + Build.VERSION.SECURITY_PATCH : "";
        androidVer.setText("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")" + patch);
        cpu.setText(Util.cpuInfo());

        int densityDpi = getResources().getDisplayMetrics().densityDpi;
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        screen.setText(w + " × " + h + " px · " + densityDpi + " dpi");

        ipLocal.setText(Util.localIpAddresses());
        Util.fetchPublicIp(ipPublic::setText);

        refresh.setOnClickListener(btn -> {
            ipLocal.setText(Util.localIpAddresses());
            Util.fetchPublicIp(ipPublic::setText);
            updateLive(ram, ramBar, storage, storageBar, battery, uptime);
        });

        updateLive(ram, ramBar, storage, storageBar, battery, uptime);
    }

    private void updateLive(TextView ram, ProgressBar ramBar, TextView storage, ProgressBar storageBar,
                            TextView battery, TextView uptime) {
        Context ctx = requireContext();

        long totalRam = Util.totalRam(ctx);
        long availRam = Util.availRam(ctx);
        long usedRam = totalRam - availRam;
        ram.setText(Util.humanSize(usedRam) + " used of " + Util.humanSize(totalRam));
        ramBar.setProgress((int) (usedRam * 100 / Math.max(totalRam, 1)));

        long totalSt = Util.storageTotal();
        long freeSt = Util.storageFree();
        long usedSt = totalSt - freeSt;
        storage.setText(Util.humanSize(usedSt) + " used of " + Util.humanSize(totalSt)
                + " (" + Util.humanSize(freeSt) + " free)");
        storageBar.setProgress((int) (usedSt * 100 / Math.max(totalSt, 1)));

        int level = Util.batteryLevel(ctx);
        float temp = Util.batteryTemp(ctx);
        battery.setText(level + "% charged · " + temp + " °C");

        uptime.setText("Up for " + Util.humanDuration(SystemClock.elapsedRealtime()));
    }
}
