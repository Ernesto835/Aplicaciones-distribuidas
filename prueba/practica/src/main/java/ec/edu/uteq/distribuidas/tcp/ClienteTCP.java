package ec.edu.uteq.distribuidas.tcp;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente TCP interactivo.
 * Envía comandos al servidor y muestra la respuesta.
 */
public class ClienteTCP {

    private static final String HOST = "127.0.0.1";
    private static final int PUERTO = 9000;

    public static void main(String[] args) {
        System.out.println("Intentando conectar con el servidor en " + HOST + ":" + PUERTO + "...");

        // Uso de try-with-resources para asegurar el cierre automático de sockets y flujos
        try (Socket socket = new Socket(HOST, PUERTO);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             Scanner teclado = new Scanner(System.in)) {

            System.out.println("Conectado exitosamente.");
            System.out.println("Comandos disponibles: HORA | ECO <mensaje> | SALIR");
            System.out.println("--------------------------------------------------");

            String comando;
            while (true) {
                System.out.print("> ");
                comando = teclado.nextLine().trim();

                if (comando.isEmpty()) {
                    continue;
                }

                // Enviar el comando al servidor
                salida.println(comando);

                // Leer la respuesta del servidor
                String respuesta = entrada.readLine();

                if (respuesta == null) {
                    System.out.println("El servidor cerró la conexión.");
                    break;
                }

                System.out.println("Servidor: " + respuesta);

                // Si el comando fue SALIR, rompemos el bucle para finalizar el programa
                if (comando.equalsIgnoreCase("SALIR")) {
                    break;
                }
            }

        } catch (UnknownHostException e) {
            System.err.println("No se pudo encontrar el host: " + HOST);
        } catch (IOException e) {
            System.err.println("Error de E/S al conectar con el servidor: " + e.getMessage());
        }

        System.out.println("Conexión cerrada.");
    }
}