import com.cardjitsu.modelo.JavaBeans;
import com.cardjitsu.protocolo.MensajeProtocolo;
import com.cardjitsu.protocolo.TipoAccion;
import com.cardjitsu.red.ClienteSocket;
import com.cardjitsu.red.ManejadorEventosCliente;
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
        LvSalas.setItems(salasDisponibles);
        lblEstadodeSala.setText("");
        ManejadorEventosCliente.setLobbyController(this);
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    public void setJugadorActual(JavaBeans jugadorActual) {
        this.jugadorActual = jugadorActual;
        if (jugadorActual != null) {
            lblGuerrero.setText("Guerrero: " + jugadorActual.getNombre());
            lblRango.setText("Rango: " + jugadorActual.getRango());
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
            lblEstadodeSala.setText(estado);
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
                lblEstadodeSala.setText("Error al abrir la sala de espera.");
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
                rankingController.actualizarRanking(jugadoresOrdenados);
                Stage stage = (Stage) btnRanking.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                bloquearControles(false);
                lblEstadodeSala.setText("Error al abrir el ranking.");
            }
        });
    }

    @FXML
    private void onClickCrear(ActionEvent event) {
        if (clienteSocket == null) {
            lblEstadodeSala.setText("No hay conexión con el servidor.");
            return;
        }

        bloquearControles(true);
        lblEstadodeSala.setText("Creando sala...");

        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.CREAR_SALA);
        clienteSocket.enviar(mensaje);
    }

    @FXML
    private void onClickUnirse(ActionEvent event) {
        String salaSeleccionada = LvSalas.getSelectionModel().getSelectedItem();
        if (salaSeleccionada == null || salaSeleccionada.isBlank()) {
            lblEstadodeSala.setText("Selecciona una sala de la lista.");
            return;
        }

        if (clienteSocket == null) {
            lblEstadodeSala.setText("No hay conexión con el servidor.");
            return;
        }

        bloquearControles(true);
        lblEstadodeSala.setText("Uniéndose a: " + salaSeleccionada);

        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.UNIRSE_SALA);
        mensaje.setDato(salaSeleccionada);
        clienteSocket.enviar(mensaje);
    }

    @FXML
    private void onClickRanking(ActionEvent event) {
        if (clienteSocket == null) {
            lblEstadodeSala.setText("No hay conexión con el servidor.");
            return;
        }

        bloquearControles(true);
        lblEstadodeSala.setText("Cargando ranking...");

        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.SOLICITAR_RANKING);
        clienteSocket.enviar(mensaje);
    }

    private void bloquearControles(boolean bloqueado) {
        btnCrearSalas.setDisable(bloqueado);
        btnUnirse.setDisable(bloqueado);
        btnRanking.setDisable(bloqueado);
        LvSalas.setDisable(bloqueado);
    }
}