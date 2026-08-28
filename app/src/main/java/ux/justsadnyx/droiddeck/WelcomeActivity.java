package ux.justsadnyx.droiddeck;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private int currentPage = 0;
    private LinearLayout dots;
    private TextView[] dotViews;
    private FrameLayout cardContainer;

    private final String[] titles = {
        "Welcome to DroidDeck",
        "Your phone, your tools",
        "Always up to date"
    };

    private final String[] descriptions = {
        "A pocket toolkit for your Android device. Monitor your phone, manage files, control settings, and share storage over Wi-Fi.",
        "Flashlight, vibration, brightness, ping, hashing — all the essentials in one place. No bloat, no ads, just tools that work.",
        "Auto-updates straight from GitHub. We check in the background so you never miss a fix or a new feature."
    };

    private final int[] icons = {
        R.drawable.ic_dashboard,
        R.drawable.ic_tools,
        R.drawable.ic_updates
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        cardContainer = findViewById(R.id.welcome_card_container);
        dots = findViewById(R.id.welcome_dots);
        Button nextBtn = findViewById(R.id.welcome_next);
        Button skipBtn = findViewById(R.id.welcome_skip);

        dotViews = new TextView[titles.length];
        for (int i = 0; i < titles.length; i++) {
            dotViews[i] = new TextView(this);
            dotViews[i].setText("●");
            dotViews[i].setTextSize(14);
            dotViews[i].setPadding(8, 0, 8, 0);
            dots.addView(dotViews[i]);
        }

        showPage(0);

        nextBtn.setOnClickListener(v -> {
            if (currentPage < titles.length - 1) {
                currentPage++;
                showPage(currentPage);
            } else {
                finishOnboarding();
            }
        });

        skipBtn.setOnClickListener(v -> finishOnboarding());
    }

    private void showPage(int index) {
        View page = getLayoutInflater().inflate(R.layout.item_welcome_page, cardContainer, false);
        ((TextView) page.findViewById(R.id.wp_title)).setText(titles[index]);
        ((TextView) page.findViewById(R.id.wp_desc)).setText(descriptions[index]);
        page.findViewById(R.id.wp_icon).setBackgroundResource(icons[index]);

        cardContainer.removeAllViews();
        cardContainer.addView(page);
        page.setAlpha(0f);
        page.setTranslationY(40f);
        page.animate().alpha(1f).translationY(0f).setDuration(350)
                .setInterpolator(new AccelerateDecelerateInterpolator()).start();

        Button nextBtn = findViewById(R.id.welcome_next);
        nextBtn.setText(index == titles.length - 1 ? "Get started" : "Next");

        for (int i = 0; i < dotViews.length; i++) {
            dotViews[i].setTextColor(getResources().getColor(
                    i == index ? R.color.violet : R.color.outline, getTheme()));
        }
    }

    private void finishOnboarding() {
        Prefs.setOnboardingDone(this);
        // Go straight to the mandatory Terms agreement + companion app setup.
        startActivity(new Intent(this, TermsSetupActivity.class));
        finish();
    }
}
