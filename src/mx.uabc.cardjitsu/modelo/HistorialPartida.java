package modelo;

import java.io.Serializable;

public class HistorialPartida implements Serializable {
    private static final long serialVersionUID = 1L;

    private String idPartida;
    private long fechaEpochMillis;
    private String jugador1;
    private String jugador2;
    private String ganador;
    private String perdedor;
    private String motivoFinalizacion;
    private int rondasJugador1;
    private int rondasJugador2;
    private String personajeJugador1;
    private String kiJugador1;
    private String personajeJugador2;
    private String kiJugador2;

    public HistorialPartida() { }

    public String getIdPartida() { return idPartida; }
    public void setIdPartida(String idPartida) { this.idPartida = idPartida; }
    public long getFechaEpochMillis() { return fechaEpochMillis; }
    public void setFechaEpochMillis(long fechaEpochMillis) { this.fechaEpochMillis = fechaEpochMillis; }
    public String getJugador1() { return jugador1; }
    public void setJugador1(String jugador1) { this.jugador1 = jugador1; }
    public String getJugador2() { return jugador2; }
    public void setJugador2(String jugador2) { this.jugador2 = jugador2; }
    public String getGanador() { return ganador; }
    public void setGanador(String ganador) { this.ganador = ganador; }
    public String getPerdedor() { return perdedor; }
    public void setPerdedor(String perdedor) { this.perdedor = perdedor; }
    public String getMotivoFinalizacion() { return motivoFinalizacion; }
    public void setMotivoFinalizacion(String motivoFinalizacion) { this.motivoFinalizacion = motivoFinalizacion; }
    public int getRondasJugador1() { return rondasJugador1; }
    public void setRondasJugador1(int rondasJugador1) { this.rondasJugador1 = rondasJugador1; }
    public int getRondasJugador2() { return rondasJugador2; }
    public void setRondasJugador2(int rondasJugador2) { this.rondasJugador2 = rondasJugador2; }
    public String getPersonajeJugador1() { return personajeJugador1; }
    public void setPersonajeJugador1(String personajeJugador1) { this.personajeJugador1 = personajeJugador1; }
    public String getKiJugador1() { return kiJugador1; }
    public void setKiJugador1(String kiJugador1) { this.kiJugador1 = kiJugador1; }
    public String getPersonajeJugador2() { return personajeJugador2; }
    public void setPersonajeJugador2(String personajeJugador2) { this.personajeJugador2 = personajeJugador2; }
    public String getKiJugador2() { return kiJugador2; }
    public void setKiJugador2(String kiJugador2) { this.kiJugador2 = kiJugador2; }
}
