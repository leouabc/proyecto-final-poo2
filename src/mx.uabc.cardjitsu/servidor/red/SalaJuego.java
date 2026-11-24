package servidor.red;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import modelo.Carta;
import modelo.DragonBallApiService;
import modelo.EstadoJuego;
import modelo.HistorialPartida;
import modelo.InspectorReglasUtil;
import modelo.Jugador;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;
import servidor.datos.RepositorioJugadores;

public class SalaJuego {
    private static final Queue<HiloCliente> colaEspera = new LinkedList<>();
    private static final Map<String, HiloCliente> salasPendientes = new LinkedHashMap<>();
    private static final Map<String, String> nombresSalas = new LinkedHashMap<>();
    private static final Random RANDOM = new Random();
    private static volatile String ultimoErrorSala;

    private final HiloCliente jugador1;
    private final HiloCliente jugador2;
    private final List<Carta> personajesDisponibles;
    private List<Carta> mano1;
    private List<Carta> mano2;
    private Integer indiceJugado1;
    private Integer indiceJugado2;
    private int rondasGanadas1;
    private int rondasGanadas2;
    private int numeroRonda;
    private boolean partidaActiva;
    private final RepositorioJugadores repositorio;
    private boolean resultadoRegistrado;
    private String codigoSala;
    private final EstadoJuego estadoCompartido = new EstadoJuego();
    private final Jugador estadoJugador1 = new Jugador();
    private final Jugador estadoJugador2 = new Jugador();
    private Carta ultimaCarta1;
    private Carta ultimaCarta2;

    public static synchronized String crearSalaPrivada(HiloCliente creador) {
        if (creador == null || !creador.isConectado() || creador.getJugadorActual() == null
                || creador.getSalaActual() != null || salasPendientes.containsValue(creador)) return null;
        removerDeCola(creador);
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (salasPendientes.containsKey(codigo));
        salasPendientes.put(codigo, creador);
        nombresSalas.put(codigo, "Sala de " + creador.getJugadorActual().getNombre());
        return codigo;
    }

    public static boolean unirseSalaPrivada(HiloCliente invitado, String codigo) {
        ultimoErrorSala = null;
        if (invitado == null || codigo == null) return false;
        String clave = codigo.trim().toUpperCase();
        HiloCliente creador;
        synchronized (SalaJuego.class) {
            if (!invitado.isConectado() || invitado.getJugadorActual() == null
                    || invitado.getSalaActual() != null) return false;
            creador = salasPendientes.get(clave);
            if (creador == null || !creador.isConectado() || creador == invitado) return false;
        }

        final List<Carta> pool;
        try {
            pool = DragonBallApiService.obtenerPersonajes();
        } catch (IOException ex) {
            ultimoErrorSala = "No se pudo consultar Dragon Ball API: " + ex.getMessage();
            synchronized (SalaJuego.class) {
                if (salasPendientes.get(clave) == creador) {
                    enviarErrorSala(creador, clave, ultimoErrorSala);
                    enviarErrorSala(invitado, clave, ultimoErrorSala);
                }
            }
            return true;
        }

        synchronized (SalaJuego.class) {
            if (salasPendientes.get(clave) != creador || !creador.isConectado()
                    || !invitado.isConectado() || invitado.getSalaActual() != null) return false;
            salasPendientes.remove(clave);
            String nombre = nombresSalas.remove(clave);
            notificarEntradaSala(creador, clave, nombre);
            notificarEntradaSala(invitado, clave, nombre);
            SalaJuego sala = new SalaJuego(creador, invitado, pool);
            sala.codigoSala = clave;
            return true;
        }
    }

    public static synchronized String getUltimoErrorSala() {
        return ultimoErrorSala;
    }

    private static void notificarEntradaSala(HiloCliente jugador, String codigo, String nombre) {
        MensajeProtocolo aviso = new MensajeProtocolo(TipoAccion.UNIRSE_SALA);
        aviso.setExito(true);
        aviso.setSalaCodigo(codigo);
        aviso.setSalaNombre(nombre == null ? "Sala privada" : nombre);
        aviso.setMensaje("Rival conectado. Preparando combate...");
        jugador.enviar(aviso);
    }

    private static void enviarErrorSala(HiloCliente jugador, String codigo, String texto) {
        MensajeProtocolo aviso = new MensajeProtocolo(TipoAccion.UNIRSE_SALA);
        aviso.setExito(false);
        aviso.setSalaCodigo(codigo);
        aviso.setMensaje(texto);
        jugador.enviar(aviso);
    }

    public static synchronized List<String> listarSalas() {
        List<String> resultado = new ArrayList<>();
        for (Map.Entry<String, String> sala : nombresSalas.entrySet()) {
            HiloCliente creador = salasPendientes.get(sala.getKey());
            if (creador != null && creador.isConectado()) {
                resultado.add(sala.getValue() + " | Código: " + sala.getKey());
            }
        }
        return resultado;
    }

    public static synchronized boolean cancelarSalaPendiente(HiloCliente cliente) {
        String codigo = null;
        for (Map.Entry<String, HiloCliente> entrada : salasPendientes.entrySet()) {
            if (entrada.getValue() == cliente) {
                codigo = entrada.getKey();
                break;
            }
        }
        if (codigo == null) return false;
        salasPendientes.remove(codigo);
        nombresSalas.remove(codigo);
        return true;
    }

    private SalaJuego(HiloCliente jugador1, HiloCliente jugador2, List<Carta> pool) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.personajesDisponibles = new ArrayList<>();
        for (Carta carta : pool) this.personajesDisponibles.add(carta.copia());
        this.repositorio = new RepositorioJugadores();
        jugador1.setSalaActual(this);
        jugador2.setSalaActual(this);
        jugador1.setPuedePedirRevancha(false);
        jugador2.setPuedePedirRevancha(false);
        iniciarPartida();
    }

    private void iniciarPartida() {
        rondasGanadas1 = 0;
        rondasGanadas2 = 0;
        numeroRonda = 1;
        indiceJugado1 = null;
        indiceJugado2 = null;
        partidaActiva = true;
        resultadoRegistrado = false;
        mano1 = generarManoInicial();
        mano2 = generarManoInicial();
        inicializarEstadoCompartido();
        sincronizarEstado("SELECCION_CARTA", null);

        MensajeProtocolo msg1 = new MensajeProtocolo(TipoAccion.INICIAR_PARTIDA);
        msg1.setJugador(jugadorPublico(jugador2.getJugadorActual()));
        msg1.setMano(obtenerNombresMano(mano1));
        msg1.setCartasMano(copiarMano(mano1));
        msg1.setRonda(numeroRonda);

        MensajeProtocolo msg2 = new MensajeProtocolo(TipoAccion.INICIAR_PARTIDA);
        msg2.setJugador(jugadorPublico(jugador1.getJugadorActual()));
        msg2.setMano(obtenerNombresMano(mano2));
        msg2.setCartasMano(copiarMano(mano2));
        msg2.setRonda(numeroRonda);

        jugador1.enviar(msg1);
        jugador2.enviar(msg2);
    }

    private void inicializarEstadoCompartido() {
        estadoCompartido.setIdPartida(UUID.randomUUID().toString());
        estadoJugador1.setNombreUsuario(nombreJugador(jugador1));
        estadoJugador2.setNombreUsuario(nombreJugador(jugador2));
        estadoJugador1.setCartasMano(copiarMano(mano1));
        estadoJugador2.setCartasMano(copiarMano(mano2));
        InspectorReglasUtil.inyectarPropiedadDinamica(estadoCompartido, "jugador1", estadoJugador1);
        InspectorReglasUtil.inyectarPropiedadDinamica(estadoCompartido, "jugador2", estadoJugador2);
    }

    private void sincronizarEstado(String fase, String ganadorRonda) {
        InspectorReglasUtil.inyectarPropiedadDinamica(estadoCompartido, "faseActual", fase);
        InspectorReglasUtil.inyectarPropiedadDinamica(estadoCompartido, "ganadorRonda", ganadorRonda);
        estadoJugador1.setCartasMano(copiarMano(mano1 == null ? Collections.emptyList() : mano1));
        estadoJugador2.setCartasMano(copiarMano(mano2 == null ? Collections.emptyList() : mano2));
    }

    private String nombreJugador(HiloCliente cliente) {
        return cliente != null && cliente.getJugadorActual() != null
                ? cliente.getJugadorActual().getNombre() : "Jugador desconectado";
    }

    private JavaBeans jugadorPublico(JavaBeans original) {
        JavaBeans publico = new JavaBeans();
        if (original == null) return publico;
        publico.setNombre(original.getNombre());
        publico.setRango(original.getRango());
        publico.setVictorias(original.getVictorias());
        publico.setDerrotas(original.getDerrotas());
        publico.setPuntos(original.getPuntos());
        return publico;
    }

    private List<Carta> generarManoInicial() {
        List<Carta> mezclados = new ArrayList<>();
        for (Carta carta : personajesDisponibles) mezclados.add(carta.copia());
        Collections.shuffle(mezclados, RANDOM);
        return new ArrayList<>(mezclados.subList(0, 5));
    }

    private Carta cartaAleatoria() {
        return personajesDisponibles.get(RANDOM.nextInt(personajesDisponibles.size())).copia();
    }

    private List<Carta> copiarMano(List<Carta> mano) {
        List<Carta> copia = new ArrayList<>(mano.size());
        for (Carta carta : mano) copia.add(carta.copia());
        return copia;
    }

    private List<String> obtenerNombresMano(List<Carta> mano) {
        List<String> nombres = new ArrayList<>(mano.size());
        for (Carta carta : mano) nombres.add(carta.getNombre());
        return nombres;
    }

    public static void emparejarJugador(HiloCliente cliente) {
        synchronized (SalaJuego.class) {
            if (cliente == null || !cliente.isConectado() || cliente.getJugadorActual() == null
                    || cliente.getSalaActual() != null || colaEspera.contains(cliente)) return;
        }

        final List<Carta> pool;
        try {
            pool = DragonBallApiService.obtenerPersonajes();
        } catch (IOException ex) {
            MensajeProtocolo error = new MensajeProtocolo(TipoAccion.BUSCAR_PARTIDA);
            error.setExito(false);
            error.setMensaje("No se pudo cargar el catálogo desde Dragon Ball API: " + ex.getMessage());
            cliente.enviar(error);
            return;
        }

        synchronized (SalaJuego.class) {
            if (!cliente.isConectado() || cliente.getJugadorActual() == null
                    || cliente.getSalaActual() != null || colaEspera.contains(cliente)) return;

            cliente.setPuedePedirRevancha(false);
            HiloCliente oponente = null;
            while (!colaEspera.isEmpty() && oponente == null) {
                HiloCliente candidato = colaEspera.poll();
                if (candidato != null && candidato.isConectado()
                        && candidato.getSalaActual() == null && candidato != cliente) {
                    oponente = candidato;
                }
            }
            if (oponente == null) {
                colaEspera.add(cliente);
                enviarEstadoCola(cliente, "Buscando rival. Espera a que otro jugador se conecte...");
                return;
            }

            enviarEstadoCola(oponente, "Rival encontrado. Preparando combate...");
            enviarEstadoCola(cliente, "Rival encontrado. Preparando combate...");
            new SalaJuego(oponente, cliente, pool);
        }
    }

    private static void enviarEstadoCola(HiloCliente cliente, String texto) {
        MensajeProtocolo respuesta = new MensajeProtocolo(TipoAccion.BUSCAR_PARTIDA);
        respuesta.setExito(true);
        respuesta.setMensaje(texto);
        cliente.enviar(respuesta);
    }

    public static synchronized void removerDeCola(HiloCliente cliente) {
        colaEspera.remove(cliente);
        cancelarSalaPendiente(cliente);
    }

    public synchronized void registrarJugada(HiloCliente jugador, int indiceCarta) {
        if (!partidaActiva || jugador == null || indiceCarta < 0
                || (jugador != jugador1 && jugador != jugador2)) return;
        List<Carta> mano = jugador == jugador1 ? mano1 : mano2;
        if (mano == null || indiceCarta >= mano.size()) return;
        if (jugador == jugador1 && indiceJugado1 == null) indiceJugado1 = indiceCarta;
        else if (jugador == jugador2 && indiceJugado2 == null) indiceJugado2 = indiceCarta;
        else return;

        if (indiceJugado1 != null && indiceJugado2 != null) {
            sincronizarEstado("RESOLVIENDO_RONDA", null);
            evaluarRonda();
        } else {
            sincronizarEstado("ESPERANDO_JUGADA_RIVAL", null);
        }
    }

    private void evaluarRonda() {
        Carta carta1 = mano1.get(indiceJugado1).copia();
        Carta carta2 = mano2.get(indiceJugado2).copia();
        ultimaCarta1 = carta1.copia();
        ultimaCarta2 = carta2.copia();
        int ganadorRonda = determinarGanadorRonda(carta1, carta2);
        if (ganadorRonda == 1) rondasGanadas1++;
        else if (ganadorRonda == 2) rondasGanadas2++;

        mano1.set(indiceJugado1, cartaAleatoria());
        mano2.set(indiceJugado2, cartaAleatoria());
        numeroRonda++;
        sincronizarEstado("SELECCION_CARTA", ganadorRonda == 0 ? "EMPATE"
                : (ganadorRonda == 1 ? nombreJugador(jugador1) : nombreJugador(jugador2)));

        MensajeProtocolo res1 = new MensajeProtocolo(TipoAccion.RESULTADO_RONDA);
        res1.setCartaPropia(carta1.getNombre());
        res1.setCartaRival(carta2.getNombre());
        res1.setCartaPropiaDatos(carta1.copia());
        res1.setCartaRivalDatos(carta2.copia());
        res1.setExito(ganadorRonda == 1);
        res1.setMensaje(textoResultado(ganadorRonda, 1));
        res1.setRonda(numeroRonda);
        res1.setMano(obtenerNombresMano(mano1));
        res1.setCartasMano(copiarMano(mano1));

        MensajeProtocolo res2 = new MensajeProtocolo(TipoAccion.RESULTADO_RONDA);
        res2.setCartaPropia(carta2.getNombre());
        res2.setCartaRival(carta1.getNombre());
        res2.setCartaPropiaDatos(carta2.copia());
        res2.setCartaRivalDatos(carta1.copia());
        res2.setExito(ganadorRonda == 2);
        res2.setMensaje(textoResultado(ganadorRonda, 2));
        res2.setRonda(numeroRonda);
        res2.setMano(obtenerNombresMano(mano2));
        res2.setCartasMano(copiarMano(mano2));

        jugador1.enviar(res1);
        jugador2.enviar(res2);
        indiceJugado1 = null;
        indiceJugado2 = null;

        if (rondasGanadas1 == 3 || rondasGanadas2 == 3) {
            finalizarPartida(rondasGanadas1 == 3 ? jugador1 : jugador2,
                    rondasGanadas1 == 3 ? jugador2 : jugador1);
        }
    }

    private String textoResultado(int ganador, int jugador) {
        if (ganador == 0) return "Empate: ambos personajes tienen el mismo poder.";
        return ganador == jugador ? "Ganaste la ronda por escala de poder." : "Perdiste la ronda por escala de poder.";
    }

    private int determinarGanadorRonda(Carta carta1, Carta carta2) {
        BigDecimal poder1 = carta1.getEscalaPoder();
        BigDecimal poder2 = carta2.getEscalaPoder();
        int comparacion = poder1.compareTo(poder2);
        return comparacion > 0 ? 1 : (comparacion < 0 ? 2 : 0);
    }

    private void finalizarPartida(HiloCliente ganador, HiloCliente perdedor) {
        finalizarPartida(ganador, perdedor, "VICTORIA POR RONDAS");
    }

    private void finalizarPartida(HiloCliente ganador, HiloCliente perdedor, String motivo) {
        if (resultadoRegistrado || ganador == null || perdedor == null) return;
        resultadoRegistrado = true;
        partidaActiva = false;
        sincronizarEstado("FINALIZADA", nombreJugador(ganador));
        try {
            HistorialPartida historial = new HistorialPartida();
            historial.setIdPartida(codigoSala == null || codigoSala.isBlank()
                    ? UUID.randomUUID().toString() : codigoSala + "-" + UUID.randomUUID());
            historial.setFechaEpochMillis(System.currentTimeMillis());
            historial.setJugador1(nombreJugador(jugador1));
            historial.setJugador2(nombreJugador(jugador2));
            historial.setGanador(nombreJugador(ganador));
            historial.setPerdedor(nombreJugador(perdedor));
            historial.setMotivoFinalizacion(motivo == null ? "FINALIZADA" : motivo);
            historial.setRondasJugador1(rondasGanadas1);
            historial.setRondasJugador2(rondasGanadas2);
            if (ultimaCarta1 != null) {
                historial.setPersonajeJugador1(ultimaCarta1.getNombre());
                historial.setKiJugador1(ultimaCarta1.getPoderApi());
            }
            if (ultimaCarta2 != null) {
                historial.setPersonajeJugador2(ultimaCarta2.getNombre());
                historial.setKiJugador2(ultimaCarta2.getPoderApi());
            }
            if (!repositorio.registrarResultadoCompleto(historial)) {
                System.err.println("El resultado no se guardó: no se encontraron ambos perfiles o los datos del historial no son válidos.");
            }
        } catch (RuntimeException ex) {
            System.err.println("No se pudieron guardar las estadísticas y el historial: " + ex.getMessage());
            ex.printStackTrace();
        }

        MensajeProtocolo msgGanador = new MensajeProtocolo(TipoAccion.FIN_PARTIDA);
        msgGanador.setExito(true);
        msgGanador.setMensaje("¡GANASTE EL COMBATE!");
        MensajeProtocolo msgPerdedor = new MensajeProtocolo(TipoAccion.FIN_PARTIDA);
        msgPerdedor.setExito(false);
        msgPerdedor.setMensaje("DERROTA EN EL TORNEO");
        ganador.enviar(msgGanador);
        perdedor.enviar(msgPerdedor);
    }

    public synchronized void reenviarChat(HiloCliente remitente, String texto) {
        if (!partidaActiva || texto == null || texto.isBlank()) return;
        HiloCliente destinatario;
        if (remitente == jugador1) destinatario = jugador2;
        else if (remitente == jugador2) destinatario = jugador1;
        else return;
        if (destinatario == null || !destinatario.isConectado()) return;

        MensajeProtocolo mensaje = new MensajeProtocolo(TipoAccion.CHAT);
        mensaje.setNombreUsuario(remitente.getJugadorActual() != null
                ? remitente.getJugadorActual().getNombre() : "Jugador");
        mensaje.setMensaje(texto.length() > 300 ? texto.substring(0, 300) : texto);
        destinatario.enviar(mensaje);
    }

    public synchronized void notificarAbandono(HiloCliente quienSalio) {
        if (quienSalio != jugador1 && quienSalio != jugador2) return;
        HiloCliente restante = quienSalio == jugador1 ? jugador2 : jugador1;
        if (partidaActiva) finalizarPartida(restante, quienSalio, "ABANDONO DEL RIVAL");

        if (restante != null && restante.isConectado()) {
            restante.setPuedePedirRevancha(true);
            MensajeProtocolo aviso = new MensajeProtocolo(TipoAccion.RIVAL_ABANDONO);
            aviso.setExito(true);
            aviso.setNombreUsuario(quienSalio.getJugadorActual() != null
                    ? quienSalio.getJugadorActual().getNombre() : "El rival");
            aviso.setMensaje("Tu rival abandonó la sala. Ahora puedes buscar otro combate con el botón de revancha.");
            restante.enviar(aviso);
        }

        quienSalio.setPuedePedirRevancha(false);
        quienSalio.setSalaActual(null);
        if (restante != null) restante.setSalaActual(null);
    }
}
