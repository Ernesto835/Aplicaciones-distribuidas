package ec.edu.uteq.distribuidas.udp;

import java.net.*;
import java.util.Scanner;

/**
 * Cliente UDP: envía un datagrama y espera respuesta con
 * timeout de 3 segundos.
 */
public class ClienteUDP {

    private static final String HOST = "localhost";
    private static final int PUERTO = 9001;
    private static final int TIMEOUT = 3000; // milisegundos
    private static final int TAM_BUF = 1024;

    public static void main(String[] args) throws Exception {
        // En UDP, el socket del cliente no necesita un puerto específico en el constructor
        try (DatagramSocket socket = new DatagramSocket()) {
            // Configurar el tiempo máximo de espera para recibir datos
            socket.setSoTimeout(TIMEOUT);

            InetAddress servidor = InetAddress.getByName(HOST);
            Scanner sc = new Scanner(System.in);

            System.out.println("Cliente UDP conectado a " + HOST + ":" + PUERTO);
            System.out.println("Escribe un mensaje (o 'salir' para terminar):");

            while (sc.hasNextLine()) {
                System.out.print("> ");
                String linea = sc.nextLine().trim();

                if (linea.equalsIgnoreCase("salir")) {
                    break;
                }

                if (linea.isEmpty()) {
                    continue;
                }

                // Preparar y enviar el paquete
                byte[] datos = linea.getBytes();
                DatagramPacket pkt = new DatagramPacket(datos, datos.length, servidor, PUERTO);
                socket.send(pkt);

                // Preparar buffer para la respuesta
                byte[] buf = new byte[TAM_BUF];
                DatagramPacket resp = new DatagramPacket(buf, buf.length);

                try {
                    // Intentar recibir la respuesta del servidor
                    socket.receive(resp);
                    String mensajeServidor = new String(resp.getData(), 0, resp.getLength());
                    System.out.println("Servidor: " + mensajeServidor);

                } catch (SocketTimeoutException e) {
                    // Se dispara si el servidor no responde antes del tiempo definido en TIMEOUT
                    System.out.println("Error: No se recibió respuesta en " + TIMEOUT + " ms (Timeout)");
                }
            }
        }
        System.out.println("Cliente finalizado.");
    }
}