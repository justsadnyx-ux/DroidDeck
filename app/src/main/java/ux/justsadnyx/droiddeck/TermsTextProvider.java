package ux.justsadnyx.droiddeck;

public class TermsTextProvider {

    public static String getFullText() {
        StringBuilder sb = new StringBuilder();
        sb.append("DroidDeck \u2014 Terms of Service & License\n\n");
        sb.append("Last updated: August 2026\nVersion: 1.1\n\n");
        sb.append(divider()).append("\n");
        sb.append("TERMS OF SERVICE\n");
        sb.append(divider()).append("\n\n");

        sb.append("1. ACCEPTANCE OF TERMS\n\n");
        sb.append("By downloading, installing, or using DroidDeck (the \"App\"), you agree to be bound by these Terms of Service. If you do not agree, do not use the App.\n\n");

        sb.append("2. DESCRIPTION OF SERVICE\n\n");
        sb.append("DroidDeck is a free, open-source Android toolkit providing device monitoring, file management, app management, a built-in terminal, network utilities, and system controls. The App runs entirely on your device and does not connect to any external servers except for optional update checks.\n\n");

        sb.append("3. USE AT YOUR OWN RISK\n\n");
        sb.append("The App is provided \"as is\" without warranty of any kind, express or implied. The built-in terminal allows you to run arbitrary commands on your device. You are solely responsible for any commands you execute and any modifications made to system settings or apps. The developers are not liable for any damage, data loss, or security issues arising from use of the App.\n\n");

        sb.append("4. PERMISSIONS\n\n");
        sb.append("The App requests the following permissions:\n\n");
        sb.append("  \u2022 Storage access: File browsing, management, and sharing.\n");
        sb.append("  \u2022 Camera: Flashlight/torch control.\n");
        sb.append("  \u2022 Notifications: Update alerts and web server status.\n");
        sb.append("  \u2022 Network access: IP lookup, ping, DNS, web file server, update checks.\n");
        sb.append("  \u2022 Write Settings: Screen brightness control.\n");
        sb.append("  \u2022 Install packages: Self-update and terms APK install.\n");
        sb.append("  \u2022 Foreground service: Background web file server.\n");
        sb.append("  \u2022 Vibrate: Vibration alerts and SOS pattern.\n\n");
        sb.append("No personal data is collected, transmitted, or stored by the App. All data remains on your device.\n\n");

        sb.append("5. AUTO-UPDATE CHECKS\n\n");
        sb.append("The App periodically checks GitHub (api.github.com) for new versions. During this check, only the App's current version number is sent. No personal information, device identifiers, or usage data is transmitted. You may disable automatic update checks in the Update tab.\n\n");

        sb.append("6. TERMINAL\n\n");
        sb.append("The built-in terminal runs shell commands on your device with the same privileges as the App. On rooted devices it may request superuser access. Only run commands you understand. See Section 3.\n\n");

        sb.append("7. WEB FILE SERVER\n\n");
        sb.append("The App includes an optional web file server that shares your device storage over the local network. When enabled, any device on the same Wi-Fi network can browse and download files. Use this only on trusted, private networks.\n\n");

        sb.append("8. OPEN SOURCE\n\n");
        sb.append("DroidDeck is released under the MIT License. Source code is available at https://github.com/justsadnyx-ux/DroidDeck.\n\n");

        sb.append("9. DATA COLLECTION\n\n");
        sb.append("DroidDeck does NOT:\n");
        sb.append("  \u2022 Collect any personal data\n");
        sb.append("  \u2022 Transmit data to third-party servers (except GitHub for updates)\n");
        sb.append("  \u2022 Use analytics or tracking\n");
        sb.append("  \u2022 Display advertisements\n");
        sb.append("  \u2022 Require account creation or login\n\n");

        sb.append("10. TERMINATION\n\n");
        sb.append("You may stop using the App at any time by uninstalling it. All locally stored data will be removed.\n\n");

        sb.append("11. LIMITATION OF LIABILITY\n\n");
        sb.append("In no event shall the developers be held liable for any indirect, incidental, special, consequential, or punitive damages resulting from your use of the App.\n\n");

        sb.append("12. CONTACT\n\n");
        sb.append("For questions or issues: https://github.com/justsadnyx-ux/DroidDeck/issues\n\n\n");

        sb.append(divider()).append("\n");
        sb.append("MIT LICENSE\n");
        sb.append(divider()).append("\n\n");

        sb.append("Copyright (c) 2026 justsadnyx\n\n");
        sb.append("Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n");
        sb.append("The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n");
        sb.append("THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n\n");
        sb.append(divider()).append("\n");
        sb.append("Source: github.com/justsadnyx-ux/DroidDeck\n");
        sb.append(divider());
        return sb.toString();
    }

    private static String divider() {
        return "═══════════════════════════════════════";
    }
}
