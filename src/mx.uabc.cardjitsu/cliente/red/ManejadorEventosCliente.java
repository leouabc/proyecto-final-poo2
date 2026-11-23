package cliente.red;

import cliente.gui.GameUI.EsperaController;
import cliente.gui.GameUI.HistorialController;
import cliente.gui.GameUI.LobbyController;
import cliente.gui.GameUI.PartidaController;
import cliente.gui.GameUI.RankingController;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import modelo.Carta;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class ManejadorEventosCliente extends Application {

    private static volatile LobbyController lobbyActivo;
    private static volatile EsperaController esperaActiva;
    private static volatile PartidaController partidaActiva;
    private static volatile RankingController rankingActivo;
    private static volatile HistorialController historialActivo;
    private static volatile ClienteSocket clienteSocketActual;
    // INICIAR_PARTIDA puede llegar mientras JavaFX todavía está cargando la vista de espera.
    private static volatile MensajeProtocolo inicioPartidaPendiente;

    public static void registrarLobby(LobbyController controller) {
        lobbyActivo = controller;
        if (controller != null) {
            esperaActiva = null; partidaActiva = null; rankingActivo = null; historialActivo = null;
            inicioPartidaPendiente = null;
        }
    }

    public static void registrarEspera(EsperaController controller) {
        esperaActiva = controller;
        if (controller != null) {
            lobbyActivo = null; partidaActiva = null; rankingActivo = null; historialActivo = null;
            MensajeProtocolo pendiente = inicioPartidaPendiente;
            inicioPartidaPendiente = null;
            if (pendiente != null) {
                Platform.runLater(() -> entregarInicioPartida(controller, pendiente));
            }
        }
    }

    public static void registrarPartida(PartidaController controller) {
        partidaActiva = controller;
        if (controller != null) { lobbyActivo = null; esperaActiva = null; rankingActivo = null; historialActivo = null; }
    }

    public static void registrarRanking(RankingController controller) {
        rankingActivo = controller;
        if (controller != null) { lobbyActivo = null; esperaActiva = null; partidaActiva = null; historialActivo = null; }
    }

    public static void registrarHistorial(HistorialController controller) {
        historialActivo = controller;
        if (controller != null) {
            lobbyActivo = null; esperaActiva = null; partidaActiva = null; rankingActivo = null;
        }
    }

    public static void setClienteSocketActual(ClienteSocket socket) {
        clienteSocketActual = socket;
    }

    public static ClienteSocket getClienteSocketActual() {
        return clienteSocketActual;
    }

    public static void escucharServidor(ObjectInputStream entrada) {
        Thread hiloEscucha = new Thread(() -> {
            try {
                while (entrada != null) {
                    Object obj = entrada.readObject();
                    if (obj instanceof MensajeProtocolo) {
                        despacharMensaje((MensajeProtocolo) obj);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                Platform.runLater(() -> {
                    String aviso = "Se perdió la conexión con el servidor. Reinicia el servidor e inicia sesión de nuevo.";
                    if (lobbyActivo != null) lobbyActivo.mostrarEstado(aviso);
                    if (rankingActivo != null) rankingActivo.mostrarError(aviso);
                    if (historialActivo != null) historialActivo.mostrarError(aviso);
                    if (esperaActiva != null) esperaActiva.mostrarEstado(aviso);
                    if (partidaActiva != null) partidaActiva.mostrarMensajeEstado(aviso);
                });
            }
        });
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }

    private static void despacharMensaje(MensajeProtocolo msg) {
        if (msg == null || msg.getTipo() == null) {
            return;
        }
        TipoAccion tipo = msg.getTipo();

        if (tipo == TipoAccion.LOGIN || tipo == TipoAccion.REGISTRO) {
            if (clienteSocketActual != null) clienteSocketActual.procesarRespuestaAcceso(msg);
        } else if (tipo == TipoAccion.BUSCAR_PARTIDA) {
            if (!msg.isExito()) {
                if (lobbyActivo != null) lobbyActivo.mostrarEstado(msg.getMensaje());
                else if (esperaActiva != null) esperaActiva.mostrarEstado(msg.getMensaje());
            } else if (lobbyActivo != null) {
                // Solo se abre la espera cuando realmente comienza una búsqueda.
                lobbyActivo.onBusquedaPartida(msg.getMensaje());
            } else if (esperaActiva != null) {
                // El jugador que ya estaba en cola permanece en su espera.
                esperaActiva.mostrarEstado(msg.getMensaje());
            }
        } else if (tipo == TipoAccion.CREAR_SALA || tipo == TipoAccion.UNIRSE_SALA) {
            if (lobbyActivo != null) {
                if (msg.isExito()) lobbyActivo.onSalaCreadaOUnida(
                        msg.getSalaNombre() == null ? "Sala privada" : msg.getSalaNombre(),
                        msg.getSalaCodigo() == null ? "" : msg.getSalaCodigo());
                else lobbyActivo.mostrarEstado(msg.getMensaje());
            }
        } else if (tipo == TipoAccion.HISTORIAL) {
            if (historialActivo != null) {
                if (msg.isExito()) historialActivo.actualizarHistorial(msg.getHistorial());
                else historialActivo.mostrarError(msg.getMensaje());
            } else if (lobbyActivo != null) {
                lobbyActivo.mostrarEstado("La respuesta del historial llegó sin una pantalla activa.");
            }
        } else if (tipo == TipoAccion.LISTAR_SALAS) {
            if (lobbyActivo != null) lobbyActivo.actualizarSalas(msg.getSalasDisponibles());
        } else if (tipo == TipoAccion.CANCELAR_ESPERA) {
            if (esperaActiva != null) esperaActiva.onSalaCancelada();
        } else if (tipo == TipoAccion.RANKING) {
            if (!msg.isExito() || msg.getRanking() == null) {
                if (rankingActivo != null) rankingActivo.mostrarError(msg.getMensaje());
                else if (lobbyActivo != null) lobbyActivo.mostrarEstado(msg.getMensaje());
            } else if (rankingActivo != null) {
                rankingActivo.actualizarRanking(msg.getRanking());
            } else if (lobbyActivo != null) {
                lobbyActivo.mostrarEstado("El ranking se recibió, pero la pantalla no está activa. Vuelve a intentarlo.");
            }
        } else if (tipo == TipoAccion.INICIAR_PARTIDA) {
            EsperaController espera = esperaActiva;
            if (espera != null) entregarInicioPartida(espera, msg);
            else inicioPartidaPendiente = msg;
        } else if (tipo == TipoAccion.RESULTADO_RONDA) {
            if (partidaActiva != null) {
                Carta propia = msg.getCartaPropiaDatos();
                Carta rival = msg.getCartaRivalDatos();
                partidaActiva.mostrarResultadoRonda(
                        propia, rival, msg.getMensaje(), msg.getRonda(),
                        0, msg.getCartasMano(), msg.isExito());
            }
        } else if (tipo == TipoAccion.FIN_PARTIDA) {
            if (partidaActiva != null) partidaActiva.habilitarFinPartida(msg.getMensaje());
        } else if (tipo == TipoAccion.RIVAL_ABANDONO) {
            if (partidaActiva != null) partidaActiva.habilitarRevanchaPorAbandono(msg.getMensaje());
        } else if (tipo == TipoAccion.PEDIR_REVANCHA) {
            if (partidaActiva != null) partidaActiva.mostrarMensajeEstado(msg.getMensaje());
        } else if (tipo == TipoAccion.CHAT) {
            if (partidaActiva != null) {
                partidaActiva.recibirMensajeChat(msg.getNombreUsuario(), msg.getMensaje());
            }
        }
    }

    private static void entregarInicioPartida(EsperaController espera, MensajeProtocolo msg) {
        if (espera == null || msg == null) return;
        String nombreRival = msg.getJugador() != null ? msg.getJugador().getNombre() : "Rival";
        espera.onPartidaLista(msg.getRonda(), nombreRival, 0, msg.getCartasMano());
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/login.fxml"));
        Scene scene = new Scene(loader.load());

        Object controller = loader.getController();
        if (controller instanceof ClienteSocket) {
            ClienteSocket socketController = (ClienteSocket) controller;
            clienteSocketActual = socketController;
        }

        stage.setTitle("Dragon Ball Jujitsu");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(ManejadorEventosCliente.class, args);
    }
}