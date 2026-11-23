package cliente.gui.GameUI;

import cliente.red.ClienteSocket;
import cliente.red.ManejadorEventosCliente;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import modelo.HistorialPartida;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

/** Vista de consulta del historial de partidas persistido en DB4O. */
public class HistorialController {
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    @FXML private Label lblEstado;
    @FXML private TableView<FilaHistorial> tvHistorial;
    @FXML private TableColumn<FilaHistorial, String> colFecha;
    @FXML private TableColumn<FilaHistorial, String> colRival;
    @FXML private TableColumn<FilaHistorial, String> colResultado;
    @FXML private TableColumn<FilaHistorial, String> colRondas;
    @FXML private TableColumn<FilaHistorial, String> colMotivo;

    private ClienteSocket clienteSocket;
    private JavaBeans jugadorActual;
    private final ObservableList<FilaHistorial> filas = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (tvHistorial != null) {
            colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
            colRival.setCellValueFactory(new PropertyValueFactory<>("rival"));
            colResultado.setCellValueFactory(new PropertyValueFactory<>("resultado"));
            colRondas.setCellValueFactory(new PropertyValueFactory<>("rondas"));
            colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
            tvHistorial.setItems(filas);
            tvHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tvHistorial.setPlaceholder(new Label("Todavía no hay partidas registradas."));
        }
    }

    public void activar() {
        ManejadorEventosCliente.registrarHistorial(this);
    }

    public void setClienteSocket(ClienteSocket socket) { this.clienteSocket = socket; }
    public void setJugadorActual(JavaBeans jugador) { this.jugadorActual = jugador; }

    public void solicitarHistorial() {
        if (lblEstado != null) lblEstado.setText("Consultando DB4O...");
        if (clienteSocket == null) {
            mostrarError("No existe una conexión activa con el servidor.");
            return;
        }
        clienteSocket.enviar(new MensajeProtocolo(TipoAccion.HISTORIAL));
    }

    public void actualizarHistorial(List<HistorialPartida> historial) {
        Platform.runLater(() -> {
            filas.clear();
            String actual = jugadorActual == null ? "" : jugadorActual.getNombre();
            if (historial != null) {
                for (HistorialPartida registro : historial) {
                    if (registro == null) continue;
                    boolean soyJugador1 = actual.equalsIgnoreCase(registro.getJugador1() == null ? "" : registro.getJugador1());
                    String rival = soyJugador1 ? registro.getJugador2() : registro.getJugador1();
                    String resultado = actual.equalsIgnoreCase(registro.getGanador() == null ? "" : registro.getGanador())
                            ? "VICTORIA" : "DERROTA";
                    String rondas = soyJugador1
                            ? registro.getRondasJugador1() + " - " + registro.getRondasJugador2()
                            : registro.getRondasJugador2() + " - " + registro.getRondasJugador1();
                    filas.add(new FilaHistorial(
                            FORMATO_FECHA.format(Instant.ofEpochMilli(registro.getFechaEpochMillis())),
                            rival == null ? "Desconocido" : rival,
                            resultado,
                            rondas,
                            registro.getMotivoFinalizacion() == null ? "FINALIZADA" : registro.getMotivoFinalizacion()));
                }
            }
            if (lblEstado != null) lblEstado.setText(filas.isEmpty()
                    ? "Todavía no tienes partidas guardadas." : "Partidas registradas: " + filas.size());
        });
    }

    public void mostrarError(String texto) {
        Platform.runLater(() -> {
            filas.clear();
            if (lblEstado != null) lblEstado.setText(texto == null ? "No fue posible cargar el historial." : texto);
        });
    }

    @FXML
    private void onClickVolver(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/lobby.fxml"));
            Parent root = loader.load();
            LobbyController lobby = loader.getController();
            lobby.setClienteSocket(clienteSocket);
            lobby.setJugadorActual(jugadorActual);
            Node origen = event != null && event.getSource() instanceof Node ? (Node) event.getSource() : tvHistorial;
            Stage stage = (Stage) origen.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dragon Ball Card-Jitsu");
        } catch (IOException ex) {
            mostrarError("No se pudo regresar al Lobby: " + ex.getMessage());
        }
    }

    public static final class FilaHistorial {
        private final String fecha;
        private final String rival;
        private final String resultado;
        private final String rondas;
        private final String motivo;
        public FilaHistorial(String fecha, String rival, String resultado, String rondas, String motivo) {
            this.fecha = fecha; this.rival = rival; this.resultado = resultado;
            this.rondas = rondas; this.motivo = motivo;
        }
        public String getFecha() { return fecha; }
        public String getRival() { return rival; }
        public String getResultado() { return resultado; }
        public String getRondas() { return rondas; }
        public String getMotivo() { return motivo; }
    }
}
