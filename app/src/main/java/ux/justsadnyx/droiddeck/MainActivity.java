package ux.justsadnyx.droiddeck;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) return show(new DashboardFragment());
            if (id == R.id.nav_files) return show(new FilesFragment());
            if (id == R.id.nav_apps) return show(new AppsFragment());
            if (id == R.id.nav_tools) return show(new ToolsFragment());
            return false;
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private boolean show(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.setReorderingAllowed(true);
        tx.replace(R.id.fragment_host, fragment);
        tx.commit();
        return true;
    }
}
