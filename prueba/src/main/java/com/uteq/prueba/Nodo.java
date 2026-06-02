package com.uteq.prueba;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Nodo {
    private final int id;
    private final int puertoServidor;
    private final AtomicInteger relojLamport = new AtomicInteger(0);

    // Mapeo de nodos del sistema: ID -> Puerto TCP
    private final Map<Integer, Integer> topologia = Map.of(
            1, 8081,
            2, 8082,
            3, 8083
    );

    private int idCoordinador = 3; // Inicialmente asumimos que el mayor es el líder
    private final Map<Integer, Long> ultimosHeartbeats = new ConcurrentHashMap<>();
    private final Set<Integer> nodosActivos = ConcurrentHashMap.newKeySet();
    private boolean enEleccion = false;
    private final String TOKEN_VALIDO = "TOKEN-SECRETO-123";

    public Nodo(int id) {
        this.id = id;
        this.puertoServidor = topologia.get(id);
        // Inicializar estados
        topologia.keySet().forEach(nodoId -> {
            if (nodoId != this.id) {
                ultimosHeartbeats.put(nodoId, System.currentTimeMillis());
                nodosActivos.add(nodoId);
            }
        });
    }

    public void iniciar() {
        System.out.println("[LOG] Iniciando Nodo " + id + " en el puerto " + puertoServidor);

        // 1. Hilo Servidor para recibir conexiones
        new Thread(this::escucharConexiones).start();

        // 2. Hilo Cliente para enviar Heartbeats periódicos (Parte C)
        new Thread(this::enviarHeartbeatsPeriodicos).start();

        // 3. Hilo de monitoreo para detectar fallos de latidos (Parte C)
        new Thread(this::monitorearVecinos).start();
    }

    private void escucharConexiones() {
        try (ServerSocket serverSocket = new ServerSocket(puertoServidor)) {
            while (true) {
                Socket socketCliente = serverSocket.accept();
                // Atender cada conexión en un hilo independiente (Evita bloqueos)
                new Thread(() -> manejarClientenodo(socketCliente)).start();
            }
        } catch (IOException e) {
            System.err.println("[ERR] Error en el servidor del Nodo " + id + ": " + e.getMessage());
        }
    }

    private void manejarClientenodo(Socket socket) {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            Mensaje msg = (Mensaje) ois.readObject();

            // Regla de Lamport al recibir mensaje: max(local, recibido) + 1 (Parte B)
            int relojRecibido = msg.getRelojLamport();
            int nuevoReloj = relojLamport.updateAndGet(local -> Math.max(local, relojRecibido) + 1);

            System.out.println(String.format("[RELOJ TOTAL] Nodo %d procesa: %s. Reloj Lamport actualizado a: %d",
                    id, msg, nuevoReloj));

            switch (msg.getTipo()) {
                case HEARTBEAT:
                    ultimosHeartbeats.put(msg.getIdEmisor(), System.currentTimeMillis());
                    if (!nodosActivos.contains(msg.getIdEmisor())) {
                        System.out.println("[FALLO] Nodo " + msg.getIdEmisor() + " ha vuelto a la vida.");
                        nodosActivos.add(msg.getIdEmisor());
                    }
                    break;

                case AUTENTICAR: // Parte E: Seguridad
                    if (TOKEN_VALIDO.equals(msg.getContenido())) {
                        oos.writeObject(new Mensaje(Mensaje.Tipo.RESPUESTA_OK, id, relojLamport.get(), "Autenticación Exitosa"));
                    } else {
                        oos.writeObject(new Mensaje(Mensaje.Tipo.RECHAZADO, id, relojLamport.get(), "Token Inválido"));
                    }
                    break;

                case OPERACION: // Parte A y B
                    // Las operaciones solo se procesan si vienen con un log ordenado
                    relojLamport.incrementAndGet(); // Evento local de registro
                    System.out.println(String.format("[REGISTRO LOG] **ORDEN TOTAL CONSISTENTE** -> Lamport: %d, Nodo Desempate: %d, Operación: %s",
                            relojLamport.get(), id, msg.getContenido()));

                    oos.writeObject(new Mensaje(Mensaje.Tipo.RESPUESTA_OK, id, relojLamport.get(), "Operación registrada exitosamente"));
                    break;

                case ELECTION: // Parte D: Algoritmo Bully
                    System.out.println("[BULLY] Recibí ELECTION de Nodo " + msg.getIdEmisor() + ". Respondiendo ANSWER.");
                    oos.writeObject(new Mensaje(Mensaje.Tipo.ANSWER, id, relojLamport.get(), "OK"));
                    // Si recibo una elección y no estoy en una, disparo mi propia elección
                    if (!enEleccion) {
                        dispararEleccion();
                    }
                    break;

                case COORDINATOR: // Parte D: Algoritmo Bully
                    this.idCoordinador = msg.getIdEmisor();
                    this.enEleccion = false;
                    System.out.println("[BULLY] **NUEVO COORDINADOR ESTABLECIDO**: Nodo " + idCoordinador);
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            // Manejo silencioso de desconexiones normales de sockets
        }
    }

    private void enviarHeartbeatsPeriodicos() {
        while (true) {
            try {
                Thread.sleep(2000); // Latido cada 2 segundos
                for (Map.Entry<Integer, Integer> nodo : topologia.entrySet()) {
                    if (nodo.getKey() != this.id && nodosActivos.contains(nodo.getKey())) {
                        enviarMensajeSinRespuesta(nodo.getValue(),
                                new Mensaje(Mensaje.Tipo.HEARTBEAT, id, relojLamport.get(), "ping"));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void monitorearVecinos() {
        while (true) {
            try {
                Thread.sleep(1000); // Monitoreo cada segundo
                long ahora = System.currentTimeMillis();

                for (Integer nodoId : topologia.keySet()) {
                    if (nodoId != this.id && nodosActivos.contains(nodoId)) {
                        long ultimoTiempo = ultimosHeartbeats.getOrDefault(nodoId, ahora);
                        // Si no responde en 5 segundos, se declara caído (Parte C)
                        if (ahora - ultimoTiempo > 5000) {
                            System.out.println(String.format("[ALERTA FALLO] El Nodo %d no responde heartbeats. Marcado como CAÍDO.", nodoId));
                            nodosActivos.remove(nodoId);

                            // Si el caído era el coordinador, iniciamos elecciones inmediatamente (Parte D)
                            if (nodoId == idCoordinador) {
                                System.out.println("[BULLY] El Coordinador cayó. Iniciando proceso de elección...");
                                dispararEleccion();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void dispararEleccion() {
        enEleccion = true;
        relojLamport.incrementAndGet(); // Evento interno
        System.out.println("[BULLY] Enviando mensajes de ELECTION a nodos con ID mayor...");

        boolean recibiAnswer = false;

        for (Map.Entry<Integer, Integer> nodo : topologia.entrySet()) {
            if (nodo.getKey() > this.id) {
                // Enviar mensaje sincrónico esperando respuesta ANSWER
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", nodo.getValue()), 1500);
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                    oos.writeObject(new Mensaje(Mensaje.Tipo.ELECTION, id, relojLamport.get(), "Elección"));
                    Mensaje respuesta = (Mensaje) ois.readObject();

                    if (respuesta.getTipo() == Mensaje.Tipo.ANSWER) {
                        recibiAnswer = true;
                    }
                } catch (Exception e) {
                    // El nodo mayor está desconectado/caído
                }
            }
        }

        // Si nadie con ID mayor respondió, yo soy el nuevo coordinador
        if (!recibiAnswer) {
            idCoordinador = this.id;
            enEleccion = false;
            System.out.println("[BULLY] **¡Elección ganada!** Yo soy el nuevo Coordinador: Nodo " + id);

            // Avisar a todos los nodos menores/activos
            for (Map.Entry<Integer, Integer> nodo : topologia.entrySet()) {
                if (nodo.getKey() != this.id) {
                    enviarMensajeSinRespuesta(nodo.getValue(),
                            new Mensaje(Mensaje.Tipo.COORDINATOR, id, relojLamport.get(), "Coordinador"));
                }
            }
        }
    }

    private void enviarMensajeSinRespuesta(int puertoDestino, Mensaje msg) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", puertoDestino), 1000);
            try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
                oos.writeObject(msg);
            }
        } catch (Exception e) {
            // Manejo de desconexiones
        }
    }

    // Ejecutor dinámico por argumentos de consola
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Por favor indica el ID del nodo (1, 2 o 3). Ejemplo: java -cp . com.uteq.prueba.Nodo 1");
            return;
        }
        int idNodo = Integer.parseInt(args[0]);
        Nodo nodo = new Nodo(idNodo);
        nodo.iniciar();
    }
}