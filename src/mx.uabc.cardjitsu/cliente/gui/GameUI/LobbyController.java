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
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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
    private Button btnHistorial;

    @FXML
    private ListView<String> LvSalas;

    @FXML
    private Button btnCrearSalas;

    @FXML
    private Button btnUnirse;

    @FXML
    private Label lblEstadodeSala;

    @FXML private TextField txtCodigoSala;

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
        solicitarSalas();
    }

    private void solicitarSalas() {
        if (clienteSocket == null) return;
        MensajeProtocolo mensaje = new MensajeProtocolo(TipoAccion.LISTAR_SALAS);
        clienteSocket.enviar(mensaje);
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/espera.fxml"));
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


    public void onBusquedaPartida(String estado) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/espera.fxml"));
                Parent root = loader.load();
                EsperaController esperaController = loader.getController();
                esperaController.setClienteSocket(clienteSocket);
                esperaController.setJugadorActual(jugadorActual);
                esperaController.setDatosSala("Emparejamiento rápido", "Automático");
                esperaController.mostrarEstado(estado == null ? "Buscando rival..." : estado);
                Stage stage = obtenerStage();
                if (stage != null) stage.setScene(new Scene(root));
            } catch (IOException ex) {
                mostrarEstado("No se pudo abrir la búsqueda: " + ex.getMessage());
            }
        });
    }

    private Stage obtenerStage() {
        if (btnRanking != null && btnRanking.getScene() != null) {
            return (Stage) btnRanking.getScene().getWindow();
        }
        if (btnCrearSalas != null && btnCrearSalas.getScene() != null) {
            return (Stage) btnCrearSalas.getScene().getWindow();
        }
        return null;
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
        String codigo = txtCodigoSala != null ? txtCodigoSala.getText().trim() : "";
        if (codigo.isEmpty() && (salaSeleccionada == null || salaSeleccionada.isBlank())) {
            mostrarEstado("Escribe un código o selecciona una sala.");
            return;
        }

        ClienteSocket socket = asegurarConexion();
        bloquearControles(true);
        if (lblEstadodeSala != null) {
            lblEstadodeSala.setText("Uniéndose a: " + salaSeleccionada);
        }

        if (codigo.isEmpty() && salaSeleccionada != null) {
            codigo = salaSeleccionada.substring(salaSeleccionada.lastIndexOf("Código:") + 7).trim();
        }
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.UNIRSE_SALA);
        mensaje.setSalaCodigo(codigo);
        socket.enviar(mensaje);
    }

    @FXML
    private void onClickPartidaRapida(ActionEvent event) {
        ClienteSocket socket = asegurarConexion();
        bloquearControles(true);
        if (lblEstadodeSala != null) lblEstadodeSala.setText("Buscando un rival...");
        socket.enviar(new MensajeProtocolo(TipoAccion.BUSCAR_PARTIDA));
    }

    @FXML
    private void onClickActualizarSalas(ActionEvent event) {
        solicitarSalas();
        mostrarEstado("Actualizando salas disponibles...");
    }

    @FXML
    private void onClickHistorial(ActionEvent event) {
        ClienteSocket socket = asegurarConexion();
        Node origen = event != null && event.getSource() instanceof Node
                ? (Node) event.getSource() : btnHistorial;
        Stage stage = origen != null && origen.getScene() != null
                ? (Stage) origen.getScene().getWindow() : obtenerStage();
        if (stage == null) {
            mostrarEstado("No se encontró la ventana para abrir el historial.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/historial.fxml"));
            Parent root = loader.load();
            HistorialController controller = loader.getController();
            controller.setClienteSocket(socket);
            controller.setJugadorActual(jugadorActual);
            stage.setScene(new Scene(root));
            stage.setTitle("Dragon Ball Card-Jitsu - Historial");
            controller.activar();
            controller.solicitarHistorial();
        } catch (Exception ex) {
            mostrarEstado("No se pudo abrir el historial: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onClickRanking(ActionEvent event) {
        ClienteSocket socket = asegurarConexion();
        Node origen = event != null && event.getSource() instanceof Node
                ? (Node) event.getSource() : btnRanking;
        Stage stage = origen != null && origen.getScene() != null
                ? (Stage) origen.getScene().getWindow() : obtenerStage();

        if (stage == null) {
            mostrarEstado("No se encontró la ventana para abrir el ranking.");
            return;
        }
        if (btnRanking != null) btnRanking.setDisable(true);

        RankingController rankingController = null;
        Parent root;
        try {
            java.net.URL recurso = getClass().getResource("/cliente/gui/ranking.fxml");
            if (recurso == null) throw new IOException("No se encontró cliente/gui/ranking.fxml en los recursos.");
            FXMLLoader loader = new FXMLLoader(recurso);
            root = loader.load();
            rankingController = loader.getController();
            if (rankingController == null) throw new IOException("El FXML no creó RankingController.");
            rankingController.setClienteSocket(socket);
            rankingController.setJugadorActual(jugadorActual);
        } catch (Exception errorFxml) {
            rankingController = new RankingController();
            rankingController.setClienteSocket(socket);
            rankingController.setJugadorActual(jugadorActual);
            root = rankingController.crearVistaRespaldo();
            System.err.println("No se pudo cargar ranking.fxml; se abrió la vista de respaldo.");
            errorFxml.printStackTrace();
        }

        try {
            stage.setScene(new Scene(root));
            stage.setTitle("Dragon Ball Jujitsu - Ranking");
            rankingController.activar();
            rankingController.solicitarRanking();
        } catch (Exception ex) {
            ManejadorEventosCliente.registrarLobby(this);
            if (btnRanking != null) btnRanking.setDisable(false);
            mostrarEstado("No se pudo mostrar el ranking: " + ex.getMessage());
            ex.printStackTrace();
        }
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
        if (btnHistorial != null) {
            btnHistorial.setDisable(bloqueado);
        }
        if (LvSalas != null) {
            LvSalas.setDisable(bloqueado);
        }
    }
}