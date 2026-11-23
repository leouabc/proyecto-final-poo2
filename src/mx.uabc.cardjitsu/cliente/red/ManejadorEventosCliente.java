package cliente.red;

import cliente.gui.GameUI.EsperaController;
import cliente.gui.GameUI.LobbyController;
import cliente.gui.GameUI.PartidaController;
import cliente.gui.GameUI.RankingController;
import java.io.IOException;
import java.io.ObjectInputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class ManejadorEventosCliente extends Application {

    private static LobbyController lobbyActivo;
    private static EsperaController esperaActiva;
    private static PartidaController partidaActiva;
    private static RankingController rankingActivo;
    private static ClienteSocket clienteSocketActual;

    public static void registrarLobby(LobbyController controller) {
        lobbyActivo = controller;
    }

    public static void registrarEspera(EsperaController controller) {
        esperaActiva = controller;
    }

    public static void registrarPartida(PartidaController controller) {
        partidaActiva = controller;
    }

    public static void registrarRanking(RankingController controller) {
        rankingActivo = controller;
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
                    if (lobbyActivo != null) {
                        lobbyActivo.mostrarEstado("Se perdió la conexión con el servidor.");
                    }
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

        if (tipo == TipoAccion.RANKING) {
            if (lobbyActivo != null) {
                lobbyActivo.onRankingCargado(msg.getRanking());
            } else if (rankingActivo != null) {
                rankingActivo.actualizarRanking(msg.getRanking());
            }
        } else if (tipo == TipoAccion.INICIAR_PARTIDA) {
            String nombreRival = (msg.getJugador() != null) ? msg.getJugador().getNombre() : "Rival";
            if (esperaActiva != null) {
                esperaActiva.onPartidaLista(msg.getRonda(), nombreRival, 0);
            }
        } else if (tipo == TipoAccion.REVANCHA_INICIADA) {
            String nombreRival = (msg.getJugador() != null) ? msg.getJugador().getNombre() : "Rival";
            if (partidaActiva != null) {
                partidaActiva.reiniciarParaRevancha(msg.getRonda(), nombreRival, 0, null);
            }
        } else if (tipo == TipoAccion.FIN_PARTIDA) {
            if (partidaActiva != null) {
                partidaActiva.habilitarFinPartida(msg.getMensaje());
            }
        } else if (tipo == TipoAccion.CHAT) {
            if (partidaActiva != null) {
                partidaActiva.recibirMensajeChat(msg.getNombreUsuario(), msg.getMensaje());
            }
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/login.fxml"));
        Scene scene = new Scene(loader.load());

        Object controller = loader.getController();
        if (controller instanceof ClienteSocket) {
            ClienteSocket socketController = (ClienteSocket) controller;
            socketController.initialize();
            clienteSocketActual = socketController;
        }

        stage.setTitle("Dragon Ball Jujitsu");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}