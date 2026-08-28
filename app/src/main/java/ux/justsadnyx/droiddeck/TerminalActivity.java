package ux.justsadnyx.droiddeck;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TerminalActivity extends AppCompatActivity {

    private TextView output;
    private EditText input;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        output = findViewById(R.id.terminal_output);
        input = findViewById(R.id.term_input);
        output.setMovementMethod(new ScrollingMovementMethod());
        appendLine("DroidDeck Terminal");
        appendLine("Type a command and press Run. Working dir: /");
        appendLine("");

        MaterialButton runBtn = findViewById(R.id.term_run);
        MaterialButton clearBtn = findViewById(R.id.term_clear);

        runBtn.setOnClickListener(v -> runCommand());
        clearBtn.setOnClickListener(v -> output.setText(""));

        input.setOnEditorActionListener((v, actionId, event) -> {
            runCommand();
            return true;
        });
        input.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                runCommand();
                return true;
            }
            return false;
        });
    }

    private void runCommand() {
        String cmd = input.getText().toString().trim();
        if (cmd.isEmpty()) return;
        input.setText("");
        appendLine("$ " + cmd);

        exec.execute(() -> {
            try {
                Process p;
                // Try with su first if available? No - run as app shell by default.
                p = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd});
                BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                StringBuilder sb = new StringBuilder();
                String line;
                boolean any = false;
                while ((line = stdout.readLine()) != null) {
                    sb.append(line).append("\n");
                    any = true;
                }
                StringBuilder err = new StringBuilder();
                while ((line = stderr.readLine()) != null) {
                    err.append(line).append("\n");
                    any = true;
                }
                int exit = p.waitFor();
                final int code = exit;
                final StringBuilder out = sb;
                final StringBuilder er = err;
                main.post(() -> {
                    if (out.length() > 0) appendLine(out.toString().trim());
                    if (er.length() > 0) appendColored(er.toString().trim(), "#FFB86C");
                    appendLine(""); // blank line after output
                    appendColored("Process exited with code " + code, "#6C5CE7");
                    appendLine("");
                });
            } catch (Exception e) {
                main.post(() -> appendColored("Error: " + e.getMessage(), "#FF6B6B"));
            }
        });
    }

    private void appendLine(String text) {
        output.append(text + "\n");
        autoScroll();
    }

    private void appendColored(String text, String color) {
        // Simple colored output handling via android.text.Html
        android.text.Spanned sp = android.text.Html.fromHtml("<font color=\"" + color + "\">" + escapeHtml(text) + "</font>");
        output.append(sp);
        output.append("\n");
        autoScroll();
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void autoScroll() {
        final TextView tv = output;
        tv.post(() -> {
            // Scroll parent ScrollView to bottom
            View parent = tv.getParent() instanceof View ? (View) tv.getParent() : null;
            if (parent != null) {
                // find ScrollView
                android.widget.ScrollView sv = findScrollView(tv);
                if (sv != null) sv.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private android.widget.ScrollView findScrollView(View v) {
        android.view.ViewParent p = v.getParent();
        while (p != null) {
            if (p instanceof android.widget.ScrollView) return (android.widget.ScrollView) p;
            p = p.getParent();
        }
        return null;
    }
}
