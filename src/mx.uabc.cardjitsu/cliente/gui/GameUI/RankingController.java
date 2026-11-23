package cliente.gui.GameUI;

import cliente.red.ClienteSocket;
import cliente.red.ManejadorEventosCliente;
import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class RankingController {

    @FXML
    private Label lblTituloRanking;

    @FXML
    private ListView<String> lvRanking;

    @FXML
    private Button btnActualizar;

    @FXML
    private Button btnVolver;

    private ClienteSocket clienteSocket;
    private JavaBeans jugadorActual;
    private final ObservableList<String> filasRanking = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        ManejadorEventosCliente.registrarRanking(this);
        if (lvRanking != null) {
            lvRanking.setItems(filasRanking);
        }
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    public void setJugadorActual(JavaBeans jugadorActual) {
        this.jugadorActual = jugadorActual;
    }

    private ClienteSocket asegurarConexion() {
        if (clienteSocket == null) {
            clienteSocket = new ClienteSocket();
            clienteSocket.initialize();
        }
        return clienteSocket;
    }

    public void actualizarRanking(List<JavaBeans> jugadoresOrdenados) {
        Platform.runLater(() -> {
            filasRanking.clear();
            if (jugadoresOrdenados == null || jugadoresOrdenados.isEmpty()) {
                filasRanking.add("No hay guerreros registrados en el ranking todavía.");
                if (btnActualizar != null) {
                    btnActualizar.setDisable(false);
                }
                return;
            }

            for (int i = 0; i < jugadoresOrdenados.size(); i++) {
                JavaBeans jugador = jugadoresOrdenados.get(i);
                String nombre = (jugador != null && jugador.getNombre() != null)
                        ? jugador.getNombre()
                        : "Guerrero";
                String rango = (jugador != null && jugador.getRango() != null)
                        ? jugador.getRango()
                        : "Sin rango";
                filasRanking.add((i + 1) + ". " + nombre + " — Rango: " + rango);
            }

            if (btnActualizar != null) {
                btnActualizar.setDisable(false);
            }
        });
    }

    @FXML
    private void onClickActualizar(ActionEvent event) {
        if (btnActualizar != null) {
            btnActualizar.setDisable(true);
        }
        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.RANKING);
        socket.enviar(mensaje);
    }

    @FXML
    private void onClickVolver(ActionEvent event) {
        if (btnVolver != null) {
            btnVolver.setDisable(true);
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"));
            Parent root = loader.load();
            LobbyController lobbyController = loader.getController();
            lobbyController.setClienteSocket(asegurarConexion());
            if (jugadorActual != null) {
                lobbyController.setJugadorActual(jugadorActual);
            }
            Stage stage = (Stage) btnVolver.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            if (btnVolver != null) {
                btnVolver.setDisable(false);
            }
        }
    }
}