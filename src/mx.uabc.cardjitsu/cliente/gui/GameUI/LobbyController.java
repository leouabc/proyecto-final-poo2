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

public class LobbyController {

    @FXML
    private Label lblGuerrero;

    @FXML
    private Label lblRango;

    @FXML
    private Button btnRanking;

    @FXML
    private ListView<String> LvSalas;

    @FXML
    private Button btnCrearSalas;

    @FXML
    private Button btnUnirse;

    @FXML
    private Label lblEstadodeSala;

    private ClienteSocket clienteSocket;
    private JavaBeans jugadorActual;
    private final ObservableList<String> salasDisponibles = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (LvSalas != null) {
            LvSalas.setItems(salasDisponibles);
        }
        if (lblEstadodeSala != null) {
            lblEstadodeSala.setText("");
        }
        ManejadorEventosCliente.registrarLobby(this);
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    public void setJugadorActual(JavaBeans jugadorActual) {
        this.jugadorActual = jugadorActual;
        if (jugadorActual != null) {
            if (lblGuerrero != null) {
                lblGuerrero.setText("Guerrero: " + jugadorActual.getNombre());
            }
            if (lblRango != null) {
                lblRango.setText("Rango: " + jugadorActual.getRango());
            }
        }
    }

    public void actualizarSalas(List<String> salas) {
        Platform.runLater(() -> {
            if (salas != null) {
                salasDisponibles.setAll(salas);
            } else {
                salasDisponibles.clear();
            }
        });
    }

    public void mostrarEstado(String estado) {
        Platform.runLater(() -> {
            if (lblEstadodeSala != null) {
                lblEstadodeSala.setText(estado != null ? estado : "");
            }
            bloquearControles(false);
        });
    }

    public void onSalaCreadaOUnida(String nombreSala, String codigoSala) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("espera.fxml"));
                Parent root = loader.load();
                EsperaController esperaController = loader.getController();
                esperaController.setClienteSocket(clienteSocket);
                esperaController.setJugadorActual(jugadorActual);
                esperaController.setDatosSala(nombreSala, codigoSala);
                Stage stage = (Stage) btnCrearSalas.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                bloquearControles(false);
                if (lblEstadodeSala != null) {
                    lblEstadodeSala.setText("Error al abrir la sala de espera.");
                }
            }
        });
    }

    public void onRankingCargado(List<JavaBeans> jugadoresOrdenados) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ranking.fxml"));
                Parent root = loader.load();
                RankingController rankingController = loader.getController();
                rankingController.setClienteSocket(clienteSocket);
                rankingController.setJugadorActual(jugadorActual);
                rankingController.actualizarRanking(jugadoresOrdenados);
                Stage stage = (Stage) btnRanking.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                bloquearControles(false);
                if (lblEstadodeSala != null) {
                    lblEstadodeSala.setText("Error al abrir el ranking.");
                }
            }
        });
    }

    private ClienteSocket asegurarConexion() {
        if (clienteSocket == null) {
            clienteSocket = new ClienteSocket();
            clienteSocket.initialize();
        }
        return clienteSocket;
    }

    @FXML
    private void onClickCrear(ActionEvent event) {
        ClienteSocket socket = asegurarConexion();
        bloquearControles(true);
        if (lblEstadodeSala != null) {
            lblEstadodeSala.setText("Creando sala...");
        }

        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.CREAR_SALA);
        socket.enviar(mensaje);
    }

    @FXML
    private void onClickUnirse(ActionEvent event) {
        String salaSeleccionada = (LvSalas != null) ? LvSalas.getSelectionModel().getSelectedItem() : null;
        if (salaSeleccionada == null || salaSeleccionada.isBlank()) {
            mostrarEstado("Selecciona una sala de la lista.");
            return;
        }

        ClienteSocket socket = asegurarConexion();
        bloquearControles(true);
        if (lblEstadodeSala != null) {
            lblEstadodeSala.setText("Uniéndose a: " + salaSeleccionada);
        }

        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.UNIRSE_SALA);
        mensaje.setMensaje(salaSeleccionada);
        socket.enviar(mensaje);
    }

    @FXML
    private void onClickRanking(ActionEvent event) {
        ClienteSocket socket = asegurarConexion();
        bloquearControles(true);
        if (lblEstadodeSala != null) {
            lblEstadodeSala.setText("Cargando ranking...");
        }

        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.RANKING);
        socket.enviar(mensaje);
    }

    private void bloquearControles(boolean bloqueado) {
        if (btnCrearSalas != null) {
            btnCrearSalas.setDisable(bloqueado);
        }
        if (btnUnirse != null) {
            btnUnirse.setDisable(bloqueado);
        }
        if (btnRanking != null) {
            btnRanking.setDisable(bloqueado);
        }
        if (LvSalas != null) {
            LvSalas.setDisable(bloqueado);
        }
    }
}