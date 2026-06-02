package com.uteq.prueba;

import java.io.Serializable;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo {
        AUTENTICAR, OPERACION, HEARTBEAT, ELECTION, ANSWER, COORDINATOR, RESPUESTA_OK, RECHAZADO
    }

    private Tipo tipo;
    private int idEmisor;
    private int relojLamport;
    private String contenido;

    public Mensaje(Tipo tipo, int idEmisor, int relojLamport, String contenido) {
        this.tipo = tipo;
        this.idEmisor = idEmisor;
        this.relojLamport = relojLamport;
        this.contenido = contenido;
    }

    public Tipo getTipo() { return tipo; }
    public int getIdEmisor() { return idEmisor; }
    public int getRelojLamport() { return relojLamport; }
    public void setRelojLamport(int relojLamport) { this.relojLamport = relojLamport; }
    public String getContenido() { return contenido; }

    @Override
    public String toString() {
        return String.format("[Tipo: %s | De: N%d | Lamport: %d | Contenido: %s]",
                tipo, idEmisor, relojLamport, contenido);
    }
}