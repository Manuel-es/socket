package es.iescamas.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Servidor con diseño ajustado (Estilo académico básico).
 * - Mejora 1: Ruta dinámica
 * - Mejora 2: HTML con CSS básico
 */
public class HiloPorClienteServidor implements Runnable {

    protected int serverPort = 9001;
    protected ServerSocket serversocket = null;
    protected boolean isStopped = false;
    protected Thread runningThread = null;

    public HiloPorClienteServidor(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();

        while (!isStopped()) {
            try {
                Socket clientSocket = this.serversocket.accept();
                new Thread(() -> {
                    try {
                        processClientRequest(clientSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, "client-" + clientSocket.getPort()).start();

            } catch (IOException e) {
                if (isStopped()) {
                    System.out.println("Server stopped.");
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
        }
        System.out.println("Server Stopped");
    }

    private void processClientRequest(Socket clientSocket) throws IOException {
        try (clientSocket;
             InputStream in = clientSocket.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
             OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = br.readLine();
            if (requestLine == null || requestLine.isBlank()) return;

            String path = "/";
            if (requestLine.startsWith("GET ")) {
                int start = 4;
                int end = requestLine.indexOf(' ', start);
                if (end > start) 
                    path = requestLine.substring(start, end);
            }

            if ("/favicon.ico".equals(path)) {
                serveFavicon(out);
                return;
            }

            // Datos técnicos
            String clientIp = clientSocket.getInetAddress().getHostAddress();
            long time = System.currentTimeMillis();
            String fecha = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date(time));

            // --- VARIABLES DE CONTENIDO ---
            String titulo = "Servidor Web Java";
            String mensajePrincipal = "Bienvenido al servidor concurrente.";
            String subtitulo = "Ruta: " + path;

            // MEJORA 1: Detección de nombre
            if (path.startsWith("/nombre/")) {
                String nombre = path.substring(8);
                titulo = "Hola " + nombre;
                mensajePrincipal = "¡Te saludo desde el servidor!";
                subtitulo = "Nombre detectado correctamente";
            }

            // --- MEJORA 2: HTML/CSS (VERSIÓN SENCILLA) ---
            // Un CSS más básico, escolar y claro.
            String css = "body { background-color: coral; font-family: sans-serif; text-align: center; margin-top: 50px; }"
                       + "div { background-color: white; border: 2px solid black; width: 60%; margin: 0 auto; padding: 20px; }"
                       + "h1 { color: blue; text-decoration: underline; }"
                       + "p { font-size: 18px; }"
                       + ".info { color: gray; font-size: 14px; border-top: 1px solid black; padding-top: 10px; margin-top: 20px;}";

            String body = "<html>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<title>" + titulo + "</title>"
                    + "<style>" + css + "</style>"
                    + "</head>"
                    + "<body>"
                    + "  <div>" // Caja simple
                    + "    <h1>" + titulo + "</h1>"
                    + "    <p>" + mensajePrincipal + "</p>"
                    + "    <p><b>Estado:</b> " + subtitulo + "</p>"
                    + "    <p class='info'>"
                    + "       Hilo: " + Thread.currentThread().getName() + "<br>"
                    + "       Fecha: " + fecha
                    + "    </p>"
                    + "  </div>"
                    + "</body></html>";

            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            String headers =
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(bodyBytes);
            out.flush();

            System.out.println("[" + Thread.currentThread().getName() + "] " + requestLine);
        }
    }

    private void serveFavicon(OutputStream out) throws IOException {
        try (InputStream iconStream = HiloPorClienteServidor.class.getResourceAsStream("/favicon.ico")) {
            if (iconStream == null) {
                out.write(("HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                return;
            }
            byte[] iconBytes = iconStream.readAllBytes();
            String headers = "HTTP/1.1 200 OK\r\nContent-Type: image/x-icon\r\nContent-Length: " + iconBytes.length + "\r\n\r\n";
            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(iconBytes);
        }
    }

    private synchronized boolean isStopped() { return isStopped; }

    private void openServerSocket() {
        try {
            this.serversocket = new ServerSocket(this.serverPort);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot open port " + serverPort, ex);
        }
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            if (this.serversocket != null) this.serversocket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
