package ux.justsadnyx.droiddeck.terms;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TermsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setPadding(48, 48, 48, 48);
        tv.setTextSize(15f);
        tv.setTextColor(Color.parseColor("#E0E0E0"));
        tv.setBackgroundColor(Color.parseColor("#121212"));
        tv.setLineSpacing(0, 1.3f);
        tv.setText(getTermsText());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(tv);
        scroll.setBackgroundColor(Color.parseColor("#121212"));
        setContentView(scroll);
    }

    private String getTermsText() {
        return "DroidDeck — Terms of Service & License\n\n" +
                "Last updated: August 2026\n\n" +
                "═══════════════════════════════════════\n" +
                "TERMS OF SERVICE\n" +
                "═══════════════════════════════════════\n\n" +
                "1. ACCEPTANCE\n" +
                "By using DroidDeck, you agree to these terms. If you do not agree, do not use the app.\n\n" +
                "2. DESCRIPTION\n" +
                "DroidDeck is a free, open-source Android toolkit providing device monitoring, file management, app management, network utilities, and system controls.\n\n" +
                "3. USE AT YOUR OWN RISK\n" +
                "DroidDeck is provided \"as is\" without warranty. The developers are not liable for any damage, data loss, or security issues arising from use of this app.\n\n" +
                "4. PERMISSIONS\n" +
                "DroidDeck requests permissions to provide its features. We do not collect, transmit, or store any personal data.\n\n" +
                "5. UPDATES\n" +
                "DroidDeck may check for updates via GitHub. No personal information is sent during this process.\n\n" +
                "6. THIRD-PARTY SERVICES\n" +
                "DroidDeck uses GitHub for update checking and distribution. Their terms apply.\n\n" +
                "═══════════════════════════════════════\n" +
                "MIT LICENSE\n" +
                "═══════════════════════════════════════\n\n" +
                "Copyright (c) 2026 justsadnyx\n\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n" +
                "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n" +
                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n\n" +
                "═══════════════════════════════════════\n" +
                "Source: github.com/justsadnyx-ux/DroidDeck\n" +
                "═══════════════════════════════════════";
    }
}
