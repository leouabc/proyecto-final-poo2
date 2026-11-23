import com.cardjitsu.modelo.JavaBeans;
import com.cardjitsu.protocolo.MensajeProtocolo;
import com.cardjitsu.protocolo.TipoAccion;
import com.cardjitsu.red.ClienteSocket;
import com.cardjitsu.red.ManejadorEventosCliente;
import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class RankingController {

    @FXML
    private Button btnRegresar;

    @FXML
    private TableView<FilaRanking> TablaGuerreros;

    @FXML
    private TableColumn<FilaRanking, Number> ColPos;

    @FXML
    private TableColumn<FilaRanking, String> ColGuerrero;

    @FXML
    private TableColumn<FilaRanking, Number> colVictorias;

    @FXML
    private TableColumn<FilaRanking, String> colRangos;

    @FXML
    private Label lblEstadoRanking;

    private ClienteSocket clienteSocket;
    private JavaBeans jugadorActual;
    private final ObservableList<FilaRanking> filas = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        ColPos.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPosicion()));
        ColGuerrero.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGuerrero()));
        colVictorias.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getVictorias()));
        colRangos.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRango()));

        TablaGuerreros.setItems(filas);
        ManejadorEventosCliente.setRankingController(this);
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    public void setJugadorActual(JavaBeans jugadorActual) {
        this.jugadorActual = jugadorActual;
    }

    public void actualizarRanking(List<JavaBeans> jugadoresOrdenados) {
        Platform.runLater(() -> {
            filas.clear();
            if (jugadoresOrdenados != null) {
                for (int i = 0; i < jugadoresOrdenados.size(); i++) {
                    JavaBeans j = jugadoresOrdenados.get(i);
                    filas.add(new FilaRanking(i + 1, j.getNombre(), j.getVictorias(), j.getRango()));
                }
            }
            if (lblEstadoRanking != null) {
                lblEstadoRanking.setText("¡SIGUE ESFORZANDOTE COMO NUNCA!");
            }
        });
    }

    public void regresarAlLobby() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cardjitsu/vista/lobby.fxml"));
                Parent root = loader.load();
                LobbyController lobbyController = loader.getController();
                lobbyController.setClienteSocket(clienteSocket);
                if (jugadorActual != null) {
                    lobbyController.setJugadorActual(jugadorActual);
                }
                Stage stage = (Stage) btnRegresar.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                btnRegresar.setDisable(false);
                if (lblEstadoRanking != null) {
                    lblEstadoRanking.setText("Error al regresar al lobby.");
                }
            }
        });
    }

    @FXML
    private void onClickRegresar(ActionEvent event) {
        btnRegresar.setDisable(true);
        if (clienteSocket != null) {
            MensajeProtocolo mensaje = new MensajeProtocolo();
            mensaje.setTipo(TipoAccion.VOLVER_LOBBY);
            clienteSocket.enviar(mensaje);
        } else {
            regresarAlLobby();
        }
    }

    public static class FilaRanking {
        private final int posicion;
        private final String guerrero;
        private final int victorias;
        private final String rango;

        public FilaRanking(int posicion, String guerrero, int victorias, String rango) {
            this.posicion = posicion;
            this.guerrero = guerrero;
            this.victorias = victorias;
            this.rango = rango;
        }

        public int getPosicion() {
            return posicion;
        }

        public String getGuerrero() {
            return guerrero;
        }

        public int getVictorias() {
            return victorias;
        }

        public String getRango() {
            return rango;
        }
    }
}