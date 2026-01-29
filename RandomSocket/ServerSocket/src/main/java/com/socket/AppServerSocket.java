package com.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Servidor con Contador de Intentos, Modo Debug y Salida controlada.
 */
public class AppServerSocket 
{
    private final static int PORT = 7777;
    private static int numGen;
    
    public static void main( String[] args ) 
    {
        // Generar número (del 1 al 10, según tu código original)
        numGen = (new Random()).nextInt(10) + 1;
        
        // MODO DEBUG: Ver el número en la consola del servidor
        System.out.println("DEBUG - El número secreto es: " + numGen);
        
        try {
            ServerSocket srvSock = new ServerSocket(PORT);
            System.out.println("ServerSocket en puerto: " + PORT);

            // Abrimos el socket y escuchamos
            Socket client = srvSock.accept();
            mostrarInfoCliente(client);
            
            // Para mandar datos al cliente >>>
            PrintWriter salida = new PrintWriter(client.getOutputStream(), true);
            // Para recibir datos al cliente <<<
            BufferedReader entrada = new BufferedReader(new InputStreamReader(client.getInputStream()));
        
            String datoRec, datoEnv;
            
            int intentos = 0;
            
            while((datoRec = entrada.readLine()) != null) {
  
                intentos++;

                datoEnv = checkNumero(datoRec);
                
                if (!datoEnv.equals("Adios")) {
                    datoEnv = "[Intento " + intentos + "] " + datoEnv;
                }
                
                salida.println(datoEnv);
                
                if (datoRec.trim().equalsIgnoreCase("SALIR")) {
                    System.out.println("Cliente desconectado.");
                }
            }
            
        } catch(IOException e) {
            System.err.println("Problemas en el socket");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void mostrarInfoCliente(Socket client) {
            // Obtener la IP del cliente
            InetAddress clientAddress = client.getInetAddress();
            String clientIP = clientAddress.getHostAddress();
            String hostName = clientAddress.getHostName();
        
            System.out.println("IP:" + clientIP + ", HostName: "+ hostName);
    }

    private static String checkNumero(String datoRec) {
        // Primero miramos si quiere salir
        if (datoRec.trim().equalsIgnoreCase("SALIR")) {
            return "Adios";
        }

        try {
            int numero = Integer.parseInt(datoRec);
            
            if(numero > numGen) {
                return "El número es MENOR que el número mágico"; // Corregido: Si mi numero es mayor, el secreto es menor
            } else if(numero < numGen) {
                return "El número es MAYOR que el número mágico"; // Corregido: Si mi numero es menor, el secreto es mayor
            } else {
                return "¡CORRECTO! Ha adivinado el número";
            }
            
        } catch(NumberFormatException e) {
            return "ERROR: Por favor, introduzca un número";
        }
    }
}
