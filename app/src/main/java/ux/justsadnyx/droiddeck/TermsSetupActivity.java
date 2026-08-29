package ux.justsadnyx.droiddeck;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TermsSetupActivity extends AppCompatActivity {

    public static final String TERMS_PACKAGE = "ux.justsadnyx.droiddeck.terms";
    public static final String ACTION_SHOW_TERMS = "ux.justsadnyx.droiddeck.terms.SHOW_TERMS";
    private static final int REQ_INSTALL_PERM = 100;

    private TextView progress;
    private android.widget.ProgressBar progressBar;
    private MaterialButton acceptBtn;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean installing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_setup);

        TextView body = findViewById(R.id.terms_body);
        body.setText(TermsTextProvider.getFullText());

        progress = findViewById(R.id.terms_progress);
        progressBar = findViewById(R.id.terms_progressbar);
        acceptBtn = findViewById(R.id.accept_btn);

        // Enable Accept once the user has scrolled through the terms (mandatory read gate).
        androidx.core.widget.NestedScrollView scroll = findViewById(R.id.terms_scroll);
        body.post(() -> {
            scroll.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
                View content = v.getChildAt(0);
                if (content != null && v.getScrollY() >= (content.getHeight() - v.getHeight())) {
                    acceptBtn.setEnabled(true);
                }
            });
        });

        acceptBtn.setOnClickListener(v -> onAccept());

        MaterialButton openStandalone = findViewById(R.id.setup_open_terms);
        openStandalone.setOnClickListener(v -> openOnlineTerms());

        MaterialButton decline = findViewById(R.id.setup_decline);
        decline.setOnClickListener(v -> onDecline());

        // Show inline "I have read" gate: accept starts as disabled, enabled by a confirm dialog
        // to reinforce mandatory acceptance.
    }

    private void onAccept() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm acceptance")
                .setMessage("I have read and agree to the DroidDeck Terms of Service and MIT License shown above. Do you accept?")
                .setPositiveButton("Yes, I accept", (d, w) -> {
                    Prefs.setTermsAccepted(this);
                    acceptBtn.setEnabled(false);
                    startCompanionInstall();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void onDecline() {
        new AlertDialog.Builder(this)
                .setTitle("Decline terms")
                .setMessage("You must accept the Terms of Service to use DroidDeck. Do you want to exit?")
                .setPositiveButton("Exit", (d, w) -> finish())
                .setNegativeButton("Review again", null)
                .show();
    }

    private void openOnlineTerms() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/justsadnyx-ux/DroidDeck")));
        } catch (Exception e) {
            toast("Cannot open browser");
        }
    }

    private void startCompanionInstall() {
        if (installing) return;
        installing = true;

        // If terms already installed and same version, skip.
        if (isPackageInstalled(TERMS_PACKAGE)) {
            Prefs.setTermsInstalled(this, true);
            proceedToMain();
            return;
        }

        // Ensure unknown-source permission on Android 8+ (silently supported on older APIs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            progress.setVisibility(View.VISIBLE);
            progress.setText(R.string.terms_install_need_perm);
            installing = false;
            try {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())), REQ_INSTALL_PERM);
            } catch (Exception e) {
                goRequestInstall();
            }
            return;
        }

        doBackgroundInstall();
    }

    private void goRequestInstall() {
        new AlertDialog.Builder(this)
                .setMessage("Please allow installs from this source (\"DroidDeck\") to set up the companion Terms app.")
                .setPositiveButton("Open settings", (d, w) -> {
                    try {
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + getPackageName())), REQ_INSTALL_PERM);
                    } catch (Exception e2) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setOnDismissListener(d -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            && !getPackageManager().canRequestPackageInstalls()) {
                        toast("Install source not enabled. Terms companion must be installed.");
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_INSTALL_PERM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && getPackageManager().canRequestPackageInstalls()) {
                doBackgroundInstall();
            } else {
                installing = false;
                toast("Please enable installs to continue setup.");
            }
        }
    }

    private void doBackgroundInstall() {
        progress.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progress.setText(R.string.terms_installing);
        acceptBtn.setEnabled(false);

        exec.execute(() -> {
            try {
                File apkFile = extractTermsApk();
                if (apkFile == null) {
                    main.post(() -> {
                        progress.setText("Could not extract the bundled companion app.");
                        progressBar.setVisibility(View.GONE);
                        installing = false;
                    });
                    return;
                }
                Uri uri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                main.post(() -> {
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        progress.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        toast("Install failed: " + e.getMessage());
                        installing = false;
                    }
                });
            } catch (Exception e) {
                main.post(() -> {
                    progress.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    toast("Extract failed: " + e.getMessage());
                    installing = false;
                });
            }
        });
    }

    private File extractTermsApk() {
        try {
            InputStream in = getAssets().open("droiddeck-terms.apk");
            File cacheDir = new File(getCacheDir(), "apks");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File apkFile = new File(cacheDir, "droiddeck-terms.apk");
            OutputStream out = new FileOutputStream(apkFile);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.close();
            in.close();
            return apkFile;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isPackageInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void proceedToMain() {
        Prefs.setTermsInstalled(this, true);
        Prefs.setTermsInstallRequested(this, true);
        Prefs.setTermsAccepted(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If the companion app just got installed (user returned from install screen), proceed.
        if (Prefs.areTermsAccepted(this) && isPackageInstalled(TERMS_PACKAGE)) {
            proceedToMain();
        }
    }
    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
    }

    @Override
    @android.annotation.SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        // Blocks back so terms must be accepted
        onDecline();
    }
}
