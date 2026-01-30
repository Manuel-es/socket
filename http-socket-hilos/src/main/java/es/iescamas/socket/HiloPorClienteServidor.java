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
 * Servidor FINAL con:
 * - Mejora 1: Ruta dinámica /nombre/
 * - Mejora 2: Diseño CSS (Card)
 * - Mejora 3: Control de Error 404
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
            long time = System.currentTimeMillis();
            String fecha = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date(time));

            // --- VARIABLES PARA LA RESPUESTA ---
            String status = "200 OK"; // Estado por defecto (se cambiará si hay error)
            String titulo = "Servidor Web Java";
            String mensajePrincipal = "Bienvenido al servidor concurrente.";
            String subtitulo = "Ruta actual: " + path;
            String colorFondo = "coral"; // Color por defecto

            // --- LÓGICA DE RUTAS (Mejoras 1, 2 y 3) ---

            if (path.equals("/")) {
                // Caso Inicio
                titulo = "Página de Inicio";
                mensajePrincipal = "Usa la ruta /nombre/TuNombre para probar.";
            
            } else if (path.startsWith("/nombre/")) {
                // MEJORA 1: Ruta dinámica
                String nombre = path.substring(8);
                titulo = "¡Hola " + nombre + "!";
                mensajePrincipal = "Saludo generado dinámicamente.";
                subtitulo = "Conexión correcta";
            
            } else {
                // MEJORA 3: ERROR 404 (Si no es ni Inicio ni Nombre)
                status = "404 Not Found";
                titulo = "Error 404";
                mensajePrincipal = "<span style='color:red; font-weight:bold;'>¡Página no encontrada!</span>";
                subtitulo = "La ruta '" + path + "' no existe en este servidor.";
                colorFondo = "#444"; // Cambiamos el fondo a gris oscuro para dar miedo
            }

            // --- CSS (Mejora 2) ---
            String css = "body { font-family: 'Segoe UI', sans-serif; "
                       + "background-color: " + colorFondo + "; margin: 0; display: flex; justify-content: center; "
                       + "align-items: center; height: 100vh; transition: background 0.5s; }"
                       + ".card { background: white; padding: 40px; border-radius: 15px; "
                       + "box-shadow: 0 10px 25px rgba(0,0,0,0.3); text-align: center; width: 400px; }"
                       + "h1 { color: #333; margin-bottom: 10px; }"
                       + "p { color: #666; line-height: 1.6; }"
                       + ".tag { display: inline-block; background: #eee; color: #555; "
                       + "padding: 5px 10px; border-radius: 5px; font-size: 0.8em; margin-top: 15px; }";

            String body = "<html>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<title>" + titulo + "</title>"
                    + "<style>" + css + "</style>"
                    + "</head>"
                    + "<body>"
                    + "  <div class='card'>"
                    + "    <h1>" + titulo + "</h1>"
                    + "    <p>" + mensajePrincipal + "</p>"
                    + "    <p><em>" + subtitulo + "</em></p>"
                    + "    <div class='tag'>Hilo: " + Thread.currentThread().getName() + "</div><br>"
                    + "    <small style='color:#ccc; display:block; margin-top:20px;'>Hora: " + fecha + "</small>"
                    + "    <br><a href='/'>Volver al inicio</a>"
                    + "  </div>"
                    + "</body></html>";

            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            // --- CABECERAS HTTP (Usando la variable status) ---
            String headers =
                    "HTTP/1.1 " + status + "\r\n" +  // <--- AQUÍ SE APLICA EL 200 O EL 404
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(bodyBytes);
            out.flush();

            System.out.println("[" + Thread.currentThread().getName() + "] " + path + " -> " + status);
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
