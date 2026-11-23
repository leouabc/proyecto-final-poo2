package servidor.red;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;
import servidor.datos.RepositorioJugadores;

public class SalaJuego {
    private static final Queue<HiloCliente> colaEspera = new LinkedList<>();
    private static final String[] CARTAS_DISPONIBLES = {
        "Androides", "Angel", "DiosDestruccion", "Humanos-Corpo",
        "Majin", "Namekiano", "RazaFreezer", "Saiyan"
    };
    private static final int[] PODER_CARTAS = {
        3, 8, 7, 1, 5, 2, 4, 6
    };

    private HiloCliente jugador1;
    private HiloCliente jugador2;
    private List<Integer> mano1;
    private List<Integer> mano2;
    private Integer indiceJugado1;
    private Integer indiceJugado2;
    private int rondasGanadas1;
    private int rondasGanadas2;
    private int numeroRonda;
    private boolean partidaActiva;
    private boolean revancha1;
    private boolean revancha2;
    private RepositorioJugadores repositorio;
    private Random random;

    public SalaJuego(HiloCliente jugador1, HiloCliente jugador2) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.repositorio = new RepositorioJugadores();
        this.random = new Random();

        jugador1.setSalaActual(this);
        jugador2.setSalaActual(this);

        iniciarNuevaPartida(false);
    }

    private void iniciarNuevaPartida(boolean esRevancha) {
        this.rondasGanadas1 = 0;
        this.rondasGanadas2 = 0;
        this.numeroRonda = 1;
        this.indiceJugado1 = null;
        this.indiceJugado2 = null;
        this.revancha1 = false;
        this.revancha2 = false;
        this.partidaActiva = true;

        this.mano1 = generarManoInicial();
        this.mano2 = generarManoInicial();

        TipoAccion tipoInicio = esRevancha ? TipoAccion.REVANCHA_INICIADA : TipoAccion.INICIAR_PARTIDA;

        MensajeProtocolo msg1 = new MensajeProtocolo();
        msg1.setTipo(tipoInicio);
        msg1.setJugador(jugador2.getJugadorActual());
        msg1.setMano(obtenerNombresMano(mano1));
        msg1.setRonda(numeroRonda);

        MensajeProtocolo msg2 = new MensajeProtocolo();
        msg2.setTipo(tipoInicio);
        msg2.setJugador(jugador1.getJugadorActual());
        msg2.setMano(obtenerNombresMano(mano2));
        msg2.setRonda(numeroRonda);

        jugador1.enviar(msg1);
        jugador2.enviar(msg2);
    }

    private List<Integer> generarManoInicial() {
        List<Integer> mano = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mano.add(random.nextInt(CARTAS_DISPONIBLES.length));
        }
        return mano;
    }

    private List<String> obtenerNombresMano(List<Integer> mano) {
        List<String> nombres = new ArrayList<>();
        for (int idx : mano) {
            nombres.add(CARTAS_DISPONIBLES[idx]);
        }
        return nombres;
    }

    public static synchronized void emparejarJugador(HiloCliente cliente) {
        if (colaEspera.contains(cliente)) {
            return;
        }
        if (colaEspera.isEmpty()) {
            colaEspera.add(cliente);
        } else {
            HiloCliente oponente = colaEspera.poll();
            if (oponente != null && oponente != cliente) {
                new SalaJuego(oponente, cliente);
            } else {
                colaEspera.add(cliente);
            }
        }
    }

    public static synchronized void removerDeCola(HiloCliente cliente) {
        colaEspera.remove(cliente);
    }

    public synchronized void registrarJugada(HiloCliente jugador, int indiceCarta) {
        if (!partidaActiva || indiceCarta < 0 || indiceCarta >= 5) {
            return;
        }
        if (jugador == jugador1 && indiceJugado1 == null) {
            indiceJugado1 = indiceCarta;
        } else if (jugador == jugador2 && indiceJugado2 == null) {
            indiceJugado2 = indiceCarta;
        }

        if (indiceJugado1 != null && indiceJugado2 != null) {
            evaluarRonda();
        }
    }

    private void evaluarRonda() {
        int idCarta1 = mano1.get(indiceJugado1);
        int idCarta2 = mano2.get(indiceJugado2);

        String nombreCarta1 = CARTAS_DISPONIBLES[idCarta1];
        String nombreCarta2 = CARTAS_DISPONIBLES[idCarta2];

        int poder1 = PODER_CARTAS[idCarta1];
        int poder2 = PODER_CARTAS[idCarta2];

        int ganadorRonda = 0;
        if (poder1 > poder2) {
            ganadorRonda = 1;
            rondasGanadas1++;
        } else if (poder2 > poder1) {
            ganadorRonda = 2;
            rondasGanadas2++;
        }

        mano1.set(indiceJugado1, random.nextInt(CARTAS_DISPONIBLES.length));
        mano2.set(indiceJugado2, random.nextInt(CARTAS_DISPONIBLES.length));

        numeroRonda++;

        MensajeProtocolo res1 = new MensajeProtocolo();
        res1.setTipo(TipoAccion.RESULTADO_RONDA);
        res1.setCartaPropia(nombreCarta1);
        res1.setCartaRival(nombreCarta2);
        res1.setExito(ganadorRonda == 1);
        res1.setMensaje(ganadorRonda == 0 ? "Empate" : (ganadorRonda == 1 ? "Ganaste la ronda" : "Perdiste la ronda"));
        res1.setRonda(numeroRonda);
        res1.setMano(obtenerNombresMano(mano1));

        MensajeProtocolo res2 = new MensajeProtocolo();
        res2.setTipo(TipoAccion.RESULTADO_RONDA);
        res2.setCartaPropia(nombreCarta2);
        res2.setCartaRival(nombreCarta1);
        res2.setExito(ganadorRonda == 2);
        res2.setMensaje(ganadorRonda == 0 ? "Empate" : (ganadorRonda == 2 ? "Ganaste la ronda" : "Perdiste la ronda"));
        res2.setRonda(numeroRonda);
        res2.setMano(obtenerNombresMano(mano2));

        jugador1.enviar(res1);
        jugador2.enviar(res2);

        indiceJugado1 = null;
        indiceJugado2 = null;

        if (rondasGanadas1 == 3 || rondasGanadas2 == 3) {
            finalizarPartida(rondasGanadas1 == 3 ? jugador1 : jugador2, rondasGanadas1 == 3 ? jugador2 : jugador1);
        }
    }

    private void finalizarPartida(HiloCliente ganador, HiloCliente perdedor) {
        partidaActiva = false;
        if (ganador.getJugadorActual() != null) {
            repositorio.actualizarResultadoPartida(ganador.getJugadorActual().getNombre(), true);
        }
        if (perdedor.getJugadorActual() != null) {
            repositorio.actualizarResultadoPartida(perdedor.getJugadorActual().getNombre(), false);
        }

        MensajeProtocolo msgGanador = new MensajeProtocolo();
        msgGanador.setTipo(TipoAccion.FIN_PARTIDA);
        msgGanador.setExito(true);
        msgGanador.setMensaje("¡GANASTE EL COMBATE!");

        MensajeProtocolo msgPerdedor = new MensajeProtocolo();
        msgPerdedor.setTipo(TipoAccion.FIN_PARTIDA);
        msgPerdedor.setExito(false);
        msgPerdedor.setMensaje("DERROTA EN EL TORNEO");

        ganador.enviar(msgGanador);
        perdedor.enviar(msgPerdedor);
    }

    public synchronized void solicitarRevancha(HiloCliente solicitante) {
        if (partidaActiva) {
            return;
        }
        if (solicitante == jugador1) {
            revancha1 = true;
        } else if (solicitante == jugador2) {
            revancha2 = true;
        }
        if (revancha1 && revancha2) {
            iniciarNuevaPartida(true);
        }
    }

    public synchronized void notificarAbandono(HiloCliente quienSalio) {
        if (partidaActiva) {
            HiloCliente restante = (quienSalio == jugador1) ? jugador2 : jugador1;
            finalizarPartida(restante, quienSalio);
        }
        jugador1.setSalaActual(null);
        jugador2.setSalaActual(null);
    }
}
