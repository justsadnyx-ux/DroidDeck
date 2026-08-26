package ux.justsadnyx.droiddeck;

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
        setContentView(R.layout.activity_main);

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
    }
}
