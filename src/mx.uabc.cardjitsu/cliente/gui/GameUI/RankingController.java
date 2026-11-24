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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class RankingController {

    @FXML
    private Label lblTituloRanking;

    @FXML
    private TableView<FilaRanking> tvRanking;

    @FXML private TableColumn<FilaRanking, Integer> colPosicion;
    @FXML private TableColumn<FilaRanking, String> colGuerrero;
    @FXML private TableColumn<FilaRanking, Integer> colVictorias;
    @FXML private TableColumn<FilaRanking, String> colRango;

    @FXML
    private Button btnActualizar;

    @FXML
    private Button btnVolver;

    private ClienteSocket clienteSocket;
    private JavaBeans jugadorActual;
    private final ObservableList<FilaRanking> filasRanking = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        configurarTabla();
    }

    public void activar() {
        ManejadorEventosCliente.registrarRanking(this);
    }

    private void configurarTabla() {
        if (tvRanking != null) {
            if (colPosicion != null) colPosicion.setCellValueFactory(new PropertyValueFactory<>("posicion"));
            if (colGuerrero != null) colGuerrero.setCellValueFactory(new PropertyValueFactory<>("guerrero"));
            if (colVictorias != null) colVictorias.setCellValueFactory(new PropertyValueFactory<>("victorias"));
            if (colRango != null) colRango.setCellValueFactory(new PropertyValueFactory<>("rango"));
            tvRanking.setItems(filasRanking);
            tvRanking.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tvRanking.setPlaceholder(new Label("Aún no hay guerreros registrados."));
        }
    }

    public Parent crearVistaRespaldo() {
        lblTituloRanking = new Label("RANKING DEL TORNEO");
        lblTituloRanking.getStyleClass().add("ranking-titulo");

        tvRanking = new TableView<>();
        tvRanking.getStyleClass().add("ranking-tabla");
        colPosicion = new TableColumn<>("Posición");
        colGuerrero = new TableColumn<>("Guerrero");
        colVictorias = new TableColumn<>("Victorias");
        colRango = new TableColumn<>("Rango");
        colPosicion.setCellValueFactory(new PropertyValueFactory<>("posicion"));
        colGuerrero.setCellValueFactory(new PropertyValueFactory<>("guerrero"));
        colVictorias.setCellValueFactory(new PropertyValueFactory<>("victorias"));
        colRango.setCellValueFactory(new PropertyValueFactory<>("rango"));
        colPosicion.setPrefWidth(100);
        colGuerrero.setPrefWidth(250);
        colVictorias.setPrefWidth(130);
        colRango.setPrefWidth(260);
        tvRanking.getColumns().setAll(colPosicion, colGuerrero, colVictorias, colRango);
        tvRanking.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tvRanking.setItems(filasRanking);
        tvRanking.setPlaceholder(new Label("Aún no hay guerreros registrados."));
        VBox.setVgrow(tvRanking, Priority.ALWAYS);

        btnActualizar = new Button("ACTUALIZAR");
        btnActualizar.getStyleClass().add("ranking-boton");
        btnActualizar.setOnAction(event -> solicitarRanking());
        btnVolver = new Button("REGRESAR");
        btnVolver.getStyleClass().add("ranking-boton-secundario");
        btnVolver.setOnAction(this::onClickVolver);

        HBox acciones = new HBox(10, btnActualizar, btnVolver);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        HBox encabezado = new HBox(14, lblTituloRanking, new javafx.scene.layout.Region(), acciones);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(encabezado.getChildren().get(1), Priority.ALWAYS);

        Label subtitulo = new Label("Los guerreros con más victorias encabezan la clasificación.");
        subtitulo.getStyleClass().add("ranking-subtitulo");
        Label seccion = new Label("MEJORES GUERREROS");
        seccion.getStyleClass().add("ranking-seccion");
        VBox superior = new VBox(8, encabezado, subtitulo);
        VBox contenido = new VBox(14, seccion, tvRanking);
        VBox.setVgrow(tvRanking, Priority.ALWAYS);
        BorderPane root = new BorderPane();
        root.setTop(superior);
        root.setCenter(contenido);
        root.setPadding(new Insets(24, 28, 24, 28));
        root.getStyleClass().add("ranking-root");
        java.net.URL css = getClass().getResource("/cliente/gui/ranking.css");
        if (css != null) root.getStylesheets().add(css.toExternalForm());
        return root;
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    public void solicitarRanking() {
        if (btnActualizar != null) btnActualizar.setDisable(true);
        MensajeProtocolo mensaje = new MensajeProtocolo(TipoAccion.RANKING);
        asegurarConexion().enviar(mensaje);
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

    public void mostrarError(String mensaje) {
        Platform.runLater(() -> {
            filasRanking.clear();
            if (btnActualizar != null) btnActualizar.setDisable(false);
            if (tvRanking != null) {
                tvRanking.setPlaceholder(new Label(
                        mensaje == null || mensaje.isBlank() ? "No fue posible cargar el ranking." : mensaje));
            }
            if (lblTituloRanking != null) lblTituloRanking.setText("RANKING DEL TORNEO");
        });
    }

    public void actualizarRanking(List<JavaBeans> jugadoresOrdenados) {
        Platform.runLater(() -> {
            filasRanking.clear();
            if (jugadoresOrdenados != null) {
                for (int i = 0; i < jugadoresOrdenados.size(); i++) {
                    JavaBeans jugador = jugadoresOrdenados.get(i);
                    if (jugador == null) continue;
                    filasRanking.add(new FilaRanking(i + 1,
                            jugador.getNombre() == null ? "Guerrero" : jugador.getNombre(),
                            jugador.getVictorias(),
                            jugador.getRango() == null ? "Sin rango" : jugador.getRango()));
                }
            }
            if (btnActualizar != null) btnActualizar.setDisable(false);
        });
    }

    public static final class FilaRanking {
        private final javafx.beans.property.IntegerProperty posicion = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.StringProperty guerrero = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.IntegerProperty victorias = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.StringProperty rango = new javafx.beans.property.SimpleStringProperty();
        public FilaRanking(int posicion, String guerrero, int victorias, String rango) {
            this.posicion.set(posicion); this.guerrero.set(guerrero);
            this.victorias.set(victorias); this.rango.set(rango);
        }
        public int getPosicion() { return posicion.get(); }
        public String getGuerrero() { return guerrero.get(); }
        public int getVictorias() { return victorias.get(); }
        public String getRango() { return rango.get(); }
        public javafx.beans.property.IntegerProperty posicionProperty() { return posicion; }
        public javafx.beans.property.StringProperty guerreroProperty() { return guerrero; }
        public javafx.beans.property.IntegerProperty victoriasProperty() { return victorias; }
        public javafx.beans.property.StringProperty rangoProperty() { return rango; }
    }

    @FXML
    private void onClickActualizar(ActionEvent event) {
        solicitarRanking();
    }

    @FXML
    private void onClickVolver(ActionEvent event) {
        if (btnVolver != null) {
            btnVolver.setDisable(true);
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/lobby.fxml"));
            Parent root = loader.load();
            LobbyController lobbyController = loader.getController();
            lobbyController.setClienteSocket(asegurarConexion());
            if (jugadorActual != null) {
                lobbyController.setJugadorActual(jugadorActual);
            }
            Node origen = event != null && event.getSource() instanceof Node ? (Node) event.getSource() : btnVolver;
            Stage stage = (Stage) origen.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dragon Ball Jujitsu");
        } catch (IOException e) {
            if (btnVolver != null) {
                btnVolver.setDisable(false);
            }
        }
    }
}