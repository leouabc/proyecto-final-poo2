package modelo;

import java.io.Serializable;

public class EstadoJuego implements Serializable {
    private static final long serialVersionUID = 1L;

    private String idPartida;
    private String faseActual;
    private Jugador jugador1;
    private Jugador jugador2;
    private String ganadorRonda;

    public EstadoJuego() {
    }

    public String getIdPartida() { return idPartida; }
    public void setIdPartida(String idPartida) { this.idPartida = idPartida; }

    public String getFaseActual() { return faseActual; }
    public void setFaseActual(String faseActual) { this.faseActual = faseActual; }

    public Jugador getJugador1() { return jugador1; }
    public void setJugador1(Jugador jugador1) { this.jugador1 = jugador1; }

    public Jugador getJugador2() { return jugador2; }
    public void setJugador2(Jugador jugador2) { this.jugador2 = jugador2; }

    public String getGanadorRonda() { return ganadorRonda; }
    public void setGanadorRonda(String ganadorRonda) { this.ganadorRonda = ganadorRonda; }
}
