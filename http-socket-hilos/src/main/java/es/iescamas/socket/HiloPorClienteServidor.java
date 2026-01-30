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
 * Servidor TCP con diseño original + MEJORA 1 (Ruta /nombre/).
 */
public class HiloPorClienteServidor implements Runnable {

    protected int serverPort = 9001;
    protected ServerSocket serversocket = null;
    protected boolean isStopped;
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

                // Creamos un hilo nuevo por cada cliente
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

            // 1) Leer la primera línea
            String requestLine = br.readLine();
            if (requestLine == null || requestLine.isBlank()) return;

            String path = "/";
            if (requestLine.startsWith("GET ")) {
                int start = 4;
                int end = requestLine.indexOf(' ', start);
                if (end > start) 
                    path = requestLine.substring(start, end);
            }

            // 2) Favicon
            if ("/favicon.ico".equals(path)) {
                serveFavicon(out);
                return;
            }

            // 3) Datos del cliente
            String clientIp = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort(); 
            String remote = clientSocket.getRemoteSocketAddress().toString(); 

            long time = System.currentTimeMillis();
            String fecha = new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(new Date(time));

           
            String mensajeTitulo = "<h3 style='color:blue;'>Servidor OK</h3>";
            
            if (path.startsWith("/nombre/")) {
                String nombre = path.substring(8); 
                mensajeTitulo = "<h1 style='color:blue;'>Hola " + nombre + "</h1>";
            }
           
            String body = "<html>"
                    + "<head>"
                    + "<link rel='icon' href='/favicon.ico'>"
                    + "<title>Programación de Servicios y Procesos</title>"
                    + "</head>"
                    + "<body style='background-color: coral;'>"  
                    + mensajeTitulo                             
                    + "<p>Path: " + path + "</p>"
                    + "<p>Server: " + fecha + "</p>"
                    + "<p>Hilo: " + Thread.currentThread().getName() + "</p>"
                    + "<p>Cliente IP: " + clientIp + "</p>"
                    + "<p>Cliente puerto: " + clientPort + "</p>"
                    + "<p>Remote: " + remote + "</p>"
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
                out.flush();
                return;
            }

            byte[] iconBytes = iconStream.readAllBytes();

            String headers =
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/x-icon\r\n" +
                    "Content-Length: " + iconBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(iconBytes);
            out.flush();
        }
    }

    private synchronized boolean isStopped() {
        return isStopped;
    }

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
            if (this.serversocket != null) {
                this.serversocket.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
