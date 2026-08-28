package ux.justsadnyx.droiddeck;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fm;
    private Fragment activeFragment;
    private final DashboardFragment dashboardFragment = new DashboardFragment();
    private final FilesFragment filesFragment = new FilesFragment();
    private final AppsFragment appsFragment = new AppsFragment();
    private final ToolsFragment toolsFragment = new ToolsFragment();
    private final UpdatesFragment updatesFragment = new UpdatesFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Prefs.isOnboardingDone(this)) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        // Mandatory terms gate: require acceptance before entering the app.
        if (!Prefs.areTermsAccepted(this)) {
            startActivity(new Intent(this, TermsSetupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        UpdateChecker.schedule(this);

        fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.fragment_host, updatesFragment, "updates").hide(updatesFragment)
                    .add(R.id.fragment_host, toolsFragment, "tools").hide(toolsFragment)
                    .add(R.id.fragment_host, appsFragment, "apps").hide(appsFragment)
                    .add(R.id.fragment_host, filesFragment, "files").hide(filesFragment)
                    .add(R.id.fragment_host, dashboardFragment, "dashboard")
                    .commit();
            activeFragment = dashboardFragment;
        } else {
            activeFragment = fm.findFragmentById(R.id.fragment_host);
        }

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_terminal) {
                startActivity(new Intent(this, TerminalActivity.class));
                // Keep current fragment visible; return to it when terminal closes
                return true;
            }
            Fragment target;
            if (id == R.id.nav_dashboard) target = dashboardFragment;
            else if (id == R.id.nav_files) target = filesFragment;
            else if (id == R.id.nav_apps) target = appsFragment;
            else if (id == R.id.nav_tools) target = toolsFragment;
            else if (id == R.id.nav_updates) target = updatesFragment;
            else return false;

            if (target == activeFragment) return true;
            fm.beginTransaction().hide(activeFragment).show(target).commit();
            activeFragment = target;
            return true;
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.nav_dashboard);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("open_updates", false)) {
            BottomNavigationView nav = findViewById(R.id.bottom_nav);
            if (nav != null) {
                nav.setSelectedItemId(R.id.nav_updates);
            }
        }
    }
}
