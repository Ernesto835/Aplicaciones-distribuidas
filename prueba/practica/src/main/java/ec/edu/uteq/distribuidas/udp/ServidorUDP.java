package ec.edu.uteq.distribuidas.udp;

import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servidor UDP sin conexión.
 * Recibe datagramas de hasta 1024 bytes y responde en el
 * mismo socket. No tiene estado entre paquetes.
 */
public class ServidorUDP {

    private static final int PUERTO = 9001;
    private static final int TAM_BUF = 1024;

    public static void main(String[] args) throws Exception {
        // El DatagramSocket se abre en el puerto específico para escuchar
        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            System.out.println("Servidor UDP escuchando en puerto " + PUERTO);

            byte[] buffer = new byte[TAM_BUF];

            while (true) {
                // Preparar el paquete para recibir datos
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);

                // BLOQUEA hasta recibir un datagrama
                socket.receive(paquete);

                // Procesar el mensaje recibido
                String mensaje = new String(paquete.getData(), 0, paquete.getLength());
                String hora = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

                System.out.println("[" + hora + "] Recibido de "
                        + paquete.getAddress() + ":" + paquete.getPort()
                        + " -> " + mensaje);

                // Preparar la respuesta (ACK + mensaje original + hora)
                String respuesta = "ACK:" + mensaje + " @" + hora;
                byte[] resp = respuesta.getBytes();

                // Crear el paquete de respuesta dirigido a la IP y puerto del remitente
                DatagramPacket respPkt = new DatagramPacket(
                        resp,
                        resp.length,
                        paquete.getAddress(),
                        paquete.getPort()
                );

                // Enviar la respuesta
                socket.send(respPkt);
            }
        }
    }
}