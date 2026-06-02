package com.uteq.prueba;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    private static final String TOKEN_CORRECTO = "TOKEN-SECRETO-123";
    private static final int PUERTO_COORDINADOR_INICIAL = 8083; // Conecta a N3 de entrada

    public static void main(String[] args) {
        System.out.println("=== CLIENTE DEL REGISTRO DISTRIBUIDO ===");
        Scanner scanner = new Scanner(System.in);

        System.out.print("Ingrese el token de seguridad para operar: ");
        String tokenIngresado = scanner.nextLine();

        // 1. Fase de Autenticación (Parte E)
        if (!autenticarCliente(tokenIngresado)) {
            System.out.println("[RECHAZADO] Acceso denegado. Cerrando cliente.");
            return;
        }
        System.out.println("[OK] Autenticación aprobada.");

        // 2. Fase de envío de operaciones (Parte A y B)
        while (true) {
            System.out.println("\nEscriba la operación de logística (o 'salir'):");
            String operacion = scanner.nextLine();
            if ("salir".equalsIgnoreCase(operacion)) break;

            System.out.print("¿A qué puerto del Nodo desea enviar la operación? (8081, 8082, 8083): ");
            int puertoDestino = Integer.parseInt(scanner.nextLine());

            enviarOperacion(puertoDestino, operacion);
        }
        scanner.close();
    }

    private static boolean autenticarCliente(String token) {
        try (Socket socket = new Socket("localhost", PUERTO_COORDINADOR_INICIAL);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Enviamos con reloj inicial 0
            Mensaje msgAuth = new Mensaje(Mensaje.Tipo.AUTENTICAR, 99, 0, token);
            oos.writeObject(msgAuth);

            Mensaje respuesta = (Mensaje) ois.readObject();
            return respuesta.getTipo() == Mensaje.Tipo.RESPUESTA_OK;

        } catch (Exception e) {
            System.err.println("[ERR] No se pudo conectar al nodo para autenticar: " + e.getMessage());
            return false;
        }
    }

    private static void enviarOperacion(int puerto, String operacion) {
        try (Socket socket = new Socket("localhost", puerto);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Envía la operación al nodo
            Mensaje msgOp = new Mensaje(Mensaje.Tipo.OPERACION, 99, 1, operacion);
            oos.writeObject(msgOp);
            System.out.println("[CLIENTE] Enviando: " + operacion);

            Mensaje respuesta = (Mensaje) ois.readObject();
            System.out.println("[NODO RESPONDE] -> " + respuesta.getContenido() + " | Reloj de Confirmación: " + respuesta.getRelojLamport());

        } catch (Exception e) {
            System.err.println("[ERR] Falló el envío al nodo en el puerto " + puerto + ". Posiblemente caído.");
        }
    }
}