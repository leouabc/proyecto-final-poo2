package servidor.red;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import modelo.JavaBeans;
import modelo.HistorialPartida;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;
import servidor.datos.RepositorioJugadores;

public class HiloCliente implements Runnable {
    private Socket socket;
    private volatile ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private RepositorioJugadores repositorio;
    private volatile JavaBeans jugadorActual;
    private volatile SalaJuego salaActual;
    private TextArea areaServidor;
    private String ipCliente;
    private volatile boolean conectado;
    private volatile boolean puedePedirRevancha;

    public HiloCliente(Socket socket, TextArea areaServidor) {
        this.socket = socket;
        this.areaServidor = areaServidor;
        this.ipCliente = socket.getInetAddress().getHostAddress();
        this.repositorio = new RepositorioJugadores();
        this.conectado = true;
    }

    @Override
    public void run() {
        try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            while (conectado) {
                Object objeto = entrada.readObject();
                if (objeto instanceof MensajeProtocolo) {
                    procesarMensaje((MensajeProtocolo) objeto);
                }
            }
        } catch (Exception e) {
            desconectar();
        }
    }

    private void procesarMensaje(MensajeProtocolo mensaje) {
        if (mensaje == null || mensaje.getTipo() == null) {
            return;
        }
        TipoAccion tipo = mensaje.getTipo();

        if (tipo == TipoAccion.REGISTRO) {
            String nombre = mensaje.getNombreUsuario();
            String contra = mensaje.getContrasena();
            boolean registrado = nombre != null && contra != null
                    && repositorio.registrarJugador(nombre, contra);
            JavaBeans jugador = registrado ? repositorio.buscarPorNombre(nombre) : null;

            MensajeProtocolo respuesta = new MensajeProtocolo();
            respuesta.setTipo(TipoAccion.REGISTRO);
            respuesta.setExito(registrado);
            respuesta.setMensaje(registrado ? "Registro exitoso" : "El guerrero ya existe");
            respuesta.setJugador(jugador == null ? null : jugadorPublico(jugador));
            if (registrado) {
                this.jugadorActual = jugador;
            }
            enviar(respuesta);
            registrarLog("Registro de " + nombre + ": " + (registrado ? "OK" : "Ya existe"));

        } else if (tipo == TipoAccion.LOGIN) {
            String nombre = mensaje.getNombreUsuario();
            String contra = mensaje.getContrasena();
            JavaBeans jugador = repositorio.iniciarSesion(nombre, contra);
            boolean exito = (jugador != null);

            MensajeProtocolo respuesta = new MensajeProtocolo();
            respuesta.setTipo(TipoAccion.LOGIN);
            respuesta.setExito(exito);
            respuesta.setMensaje(exito ? "Bienvenido al torneo" : "Usuario o contraseña incorrectos");
            respuesta.setJugador(jugador == null ? null : jugadorPublico(jugador));
            if (exito) {
                this.jugadorActual = jugador;
                registrarLog("Guerrero conectado: " + jugador.getNombre());
            }
            enviar(respuesta);

        } else if (tipo == TipoAccion.RANKING) {
            if (jugadorActual == null) {
                MensajeProtocolo error = new MensajeProtocolo(TipoAccion.RANKING);
                error.setMensaje("Inicia sesión para consultar el ranking.");
                enviar(error);
                return;
            }
            MensajeProtocolo respuesta = new MensajeProtocolo(TipoAccion.RANKING);
            try {
                respuesta.setRanking(repositorio.obtenerRanking());
                respuesta.setExito(true);
                respuesta.setMensaje("Ranking actualizado.");
            } catch (RuntimeException ex) {
                respuesta.setExito(false);
                respuesta.setMensaje("No se pudo consultar el ranking: " + ex.getMessage());
                registrarLog("Error al consultar ranking de " + jugadorActual.getNombre() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
            enviar(respuesta);

        } else if (tipo == TipoAccion.HISTORIAL) {
            MensajeProtocolo respuesta = new MensajeProtocolo(TipoAccion.HISTORIAL);
            if (jugadorActual == null) {
                respuesta.setExito(false);
                respuesta.setMensaje("Inicia sesión para consultar tu historial.");
            } else {
                try {
                    List<HistorialPartida> historial = repositorio.obtenerHistorialJugador(jugadorActual.getNombre());
                    respuesta.setHistorial(historial);
                    respuesta.setExito(true);
                    respuesta.setMensaje(historial.isEmpty()
                            ? "Todavía no tienes partidas registradas." : "Historial actualizado.");
                } catch (RuntimeException ex) {
                    respuesta.setExito(false);
                    respuesta.setMensaje("No se pudo consultar el historial: " + ex.getMessage());
                    registrarLog("Error al consultar historial: " + ex.getMessage());
                }
            }
            enviar(respuesta);

        } else if (tipo == TipoAccion.LISTAR_SALAS) {
            MensajeProtocolo respuesta = new MensajeProtocolo(TipoAccion.LISTAR_SALAS);
            respuesta.setSalasDisponibles(SalaJuego.listarSalas());
            respuesta.setExito(true);
            enviar(respuesta);

        } else if (tipo == TipoAccion.CREAR_SALA) {
            if (jugadorActual == null) {
                MensajeProtocolo error = new MensajeProtocolo(tipo);
                error.setMensaje("Debes iniciar sesión para crear una sala.");
                enviar(error);
                return;
            }
            String codigo = SalaJuego.crearSalaPrivada(this);
            MensajeProtocolo respuesta = new MensajeProtocolo(tipo);
            respuesta.setExito(codigo != null);
            respuesta.setSalaCodigo(codigo);
            respuesta.setSalaNombre(codigo == null ? null : "Sala de " + jugadorActual.getNombre());
            respuesta.setMensaje(codigo == null ? "No se pudo crear la sala." : "Comparte este código con tu rival: " + codigo);
            enviar(respuesta);

        } else if (tipo == TipoAccion.UNIRSE_SALA) {
            boolean unido = SalaJuego.unirseSalaPrivada(this, mensaje.getSalaCodigo());
            MensajeProtocolo respuesta = new MensajeProtocolo(tipo);
            if (!unido) {
                respuesta.setExito(false);
                respuesta.setMensaje("No existe esa sala o ya no está disponible.");
                enviar(respuesta);
            }

        } else if (tipo == TipoAccion.BUSCAR_PARTIDA) {
            if (jugadorActual == null) {
                MensajeProtocolo error = new MensajeProtocolo(TipoAccion.BUSCAR_PARTIDA);
                error.setExito(false);
                error.setMensaje("Debes iniciar sesión antes de buscar partida.");
                enviar(error);
                return;
            }
            if (salaActual != null) {
                MensajeProtocolo error = new MensajeProtocolo(tipo);
                error.setExito(false);
                error.setMensaje("Ya estás participando en una partida.");
                enviar(error);
                return;
            }
            SalaJuego.emparejarJugador(this);
            registrarLog(jugadorActual.getNombre() + " buscando combate...");

        } else if (tipo == TipoAccion.JUGAR_CARTA) {
            SalaJuego sala = salaActual;
            if (sala != null) {
                sala.registrarJugada(this, mensaje.getIndiceCarta());
            }

        } else if (tipo == TipoAccion.CHAT) {
            SalaJuego sala = salaActual;
            if (sala != null && mensaje.getMensaje() != null
                    && !mensaje.getMensaje().isBlank()) {
                sala.reenviarChat(this, mensaje.getMensaje().trim());
            }

        } else if (tipo == TipoAccion.PEDIR_REVANCHA) {
            // La revancha solo se habilita en el servidor cuando el rival abandona.
            if (puedePedirRevancha && salaActual == null) {
                SalaJuego.emparejarJugador(this);
            } else {
                MensajeProtocolo error = new MensajeProtocolo(TipoAccion.PEDIR_REVANCHA);
                error.setExito(false);
                error.setMensaje("La revancha solo está disponible cuando tu rival abandona la partida.");
                enviar(error);
            }
        } else if (tipo == TipoAccion.SALIR_PARTIDA || tipo == TipoAccion.CANCELAR_ESPERA) {
            SalaJuego.removerDeCola(this);
            if (salaActual != null) salaActual.notificarAbandono(this);
            salaActual = null;
            if (tipo == TipoAccion.CANCELAR_ESPERA) {
                MensajeProtocolo respuesta = new MensajeProtocolo(TipoAccion.CANCELAR_ESPERA);
                respuesta.setExito(true);
                respuesta.setMensaje("Búsqueda cancelada.");
                enviar(respuesta);
            }
        } else if (tipo == TipoAccion.DESCONECTAR) {
            desconectar();
        }
    }

    private JavaBeans jugadorPublico(JavaBeans original) {
        JavaBeans publico = new JavaBeans();
        publico.setNombre(original.getNombre());
        publico.setRango(original.getRango());
        publico.setVictorias(original.getVictorias());
        publico.setDerrotas(original.getDerrotas());
        publico.setPuntos(original.getPuntos());
        return publico;
    }

    public synchronized void enviar(MensajeProtocolo mensaje) {
        try {
            if (salida != null && !socket.isClosed()) {
                salida.reset();
                salida.writeObject(mensaje);
                salida.flush();
            }
        } catch (IOException e) {
            desconectar();
        }
    }

    private void registrarLog(String texto) {
        if (areaServidor != null) {
            Platform.runLater(() -> areaServidor.appendText(texto + "\n"));
        }
    }

    public synchronized void setSalaActual(SalaJuego salaActual) {
        this.salaActual = salaActual;
    }

    public void setPuedePedirRevancha(boolean puedePedirRevancha) {
        this.puedePedirRevancha = puedePedirRevancha;
    }

    public boolean puedePedirRevancha() {
        return puedePedirRevancha;
    }

    public boolean isConectado() {
        return conectado && socket != null && !socket.isClosed();
    }

    public SalaJuego getSalaActual() { return salaActual; }

    public JavaBeans getJugadorActual() {
        return jugadorActual;
    }

    public void desconectar() {
        if (!conectado) {
            return;
        }
        conectado = false;
        SalaJuego.removerDeCola(this);
        if (salaActual != null) {
            salaActual.notificarAbandono(this);
        }
        registrarLog("El cliente " + ipCliente + " se desconecto!");
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
