package ux.justsadnyx.droiddeck;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpServerService extends Service {

    public static final int PORT = 8080;
    private static final String CHANNEL_ID = "droiddeck_server";
    private static final int NOTIFICATION_ID = 7;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private Thread acceptThread;

    public static boolean isRunning() {
        return RUNNING.get();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
        startServer();
    }

    @Override
    public void onDestroy() {
        RUNNING.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "File Server",
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("DroidDeck file server")
                .setContentText("Sharing files on port " + PORT)
                .setOngoing(true)
                .setContentIntent(pending)
                .build();
    }

    private synchronized void startServer() {
        if (RUNNING.get()) return;
        RUNNING.set(true);

        File webRoot = Environment.getExternalStorageDirectory();

        acceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                while (RUNNING.get() && !serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    handle(client, webRoot);
                }
            } catch (Exception e) {
                RUNNING.set(false);
            }
        }, "DroidDeck-HttpServer");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private static String htmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void handle(final Socket client, final File webRoot) {
        new Thread(() -> {
            OutputStream out = null;
            try {
                client.setSoTimeout(10000);
                byte[] buf = new byte[8192];
                java.io.InputStream in = client.getInputStream();
                int read = in.read(buf);
                if (read <= 0) {
                    closeQuietly(client);
                    return;
                }
                String request = new String(buf, 0, read, StandardCharsets.ISO_8859_1);
                String firstLine = request.split("\r\n")[0];
                String[] parts = firstLine.split(" ");
                String method = parts.length > 0 ? parts[0] : "GET";
                String rawPath = parts.length > 1 ? parts[1] : "/";

                int queryIdx = rawPath.indexOf('?');
                if (queryIdx >= 0) rawPath = rawPath.substring(0, queryIdx);

                String path = URLDecoder.decode(rawPath, "UTF-8");
                out = client.getOutputStream();

                File target = new File(webRoot, path).getCanonicalFile();
                String rootCanonical = webRoot.getCanonicalFile().getAbsolutePath();
                if (!target.getAbsolutePath().startsWith(rootCanonical)) {
                    writeStatus(out, 403, "Forbidden");
                    finish(out, method);
                    closeQuietly(client);
                    return;
                }

                if (target.isDirectory()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
                      .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                      .append("<title>DroidDeck · ").append(htmlEscape(target.getName())).append("</title>")
                      .append("<style>body{font-family:'Segoe UI',system-ui,sans-serif;background:#1a1a1a;color:#eee;")
                      .append("max-width:820px;margin:32px auto;padding:0 16px}h1{color:#a78bfa;font-weight:650}")
                      .append("ul{list-style:none;padding:0}li{padding:9px 12px;border-radius:8px;margin:3px 0;background:#242424}")
                      .append("li:hover{background:#2d2d2d}a{color:#22d3ee;text-decoration:none}")
                      .append(".dir{font-weight:600}.size{float:right;color:#888;font-size:.85em}</style></head>")
                      .append("<body><h1>📱 DroidDeck</h1><p>").append(htmlEscape(path)).append("</p><ul>");
                    File[] kids = target.listFiles();
                    if (!path.equals("/")) {
                        sb.append("<li class=\"dir\"><a href=\"../\">⬆️ .. (parent)</a></li>");
                    }
                    if (kids != null) {
                        java.util.Arrays.sort(kids, (a, b) -> {
                            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                            return a.getName().compareToIgnoreCase(b.getName());
                        });
                        for (File kid : kids) {
                            String name = htmlEscape(kid.getName());
                            String href = path.endsWith("/") ? path + kid.getName() : path + "/" + kid.getName();
                            String sizeLabel = kid.isDirectory() ? "" :
                                    "<span class=\"size\">" + Util.humanSize(kid.length()) + "</span>";
                            sb.append("<li class=\"").append(kid.isDirectory() ? "dir" : "").append("\">")
                              .append("<a href=\"").append(href).append(kid.isDirectory() ? "/" : "").append("\">")
                              .append(kid.isDirectory() ? "📁 " : "📄 ").append(name).append("</a>").append(sizeLabel).append("</li>");
                        }
                    } else {
                        sb.append("<li>(empty or inaccessible)</li>");
                    }
                    sb.append("</ul></body></html>");
                    byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
                    writeStatus(out, 200, body.length + "", "text/html; charset=utf-8");
                    if (!method.equalsIgnoreCase("HEAD")) out.write(body);
                } else if (target.exists() && target.isFile()) {
                    long length = target.length();
                    writeStatus(out, 200, length + "", mimeFor(target));
                    if (!method.equalsIgnoreCase("HEAD")) {
                        FileInputStream fis = new FileInputStream(target);
                        byte[] buf2 = new byte[65536];
                        int n;
                        while ((n = fis.read(buf2)) > 0) out.write(buf2, 0, n);
                        fis.close();
                    }
                } else {
                    writeStatus(out, 404, "Not Found", "text/html");
                    if (!method.equalsIgnoreCase("HEAD"))
                        out.write("<h1>404</h1>".getBytes(StandardCharsets.UTF_8));
                }
                finish(out, method);
            } catch (Exception ignored) {
            } finally {
                closeQuietly(client);
            }
        }, "DroidDeck-Conn").start();
    }

    private void writeStatus(OutputStream out, int code, String contentLengthHeader,
                             String contentType) throws Exception {
        String reason = code == 200 ? "OK" : code == 404 ? "Not Found" : code == 403 ? "Forbidden" : "Error";
        String headers = "HTTP/1.1 " + code + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + contentLengthHeader + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }

    private void writeStatus(OutputStream out, int code, String reason) throws Exception {
        writeStatus(out, code, "-1", "text/plain");
    }

    private void finish(OutputStream out, String method) throws Exception {
        if (!method.equalsIgnoreCase("HEAD") && out != null) out.flush();
    }

    private static String mimeFor(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(java.util.Locale.US);
        String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime == null ? "application/octet-stream" : mime;
    }

    private static void closeQuietly(Socket socket) {
        try { socket.close(); } catch (Exception ignored) {}
    }
}
