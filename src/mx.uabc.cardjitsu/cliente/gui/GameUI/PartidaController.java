package cliente.gui.GameUI;

import cliente.gui.GameUI.LobbyController;
import cliente.red.ClienteSocket;
import cliente.red.ManejadorEventosCliente;
import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class PartidaController {

    @FXML
    private Label lblRonda;

    @FXML
    private Label lblRival;

    @FXML
    private Label lblPuntos;

    @FXML
    private VBox VboxCartasRival;

    @FXML
    private VBox VboxTusCartasGanadas;

    @FXML
    private Label lblResultado;

    @FXML
    private VBox CartaTuyaLanzada;

    @FXML
    private VBox cartaEnemiga;

    @FXML
    private Button btnRegresar;

    @FXML
    private Button btnRevancha;

    @FXML
    private VBox carta1;

    @FXML
    private VBox carta2;

    @FXML
    private VBox carta3;

    @FXML
    private VBox carta4;

    @FXML
    private VBox carta5;

    @FXML
    private Button btnConfirmar;

    private ClienteSocket clienteSocket;
    private JavaBeans jugadorActual;
    private List<VBox> slotsMano;
    private int indiceSeleccionado = -1;
    private boolean turnoConfirmado = false;

    @FXML
    private void initialize() {
        slotsMano = List.of(carta1, carta2, carta3, carta4, carta5);
        for (int i = 0; i < slotsMano.size(); i++) {
            final int indice = i;
            VBox slot = slotsMano.get(i);
            slot.setOnMouseClicked(e -> seleccionarCarta(indice));
        }
        btnConfirmar.setDisable(true);
        btnRevancha.setDisable(true);
        lblResultado.setText("Resultado: Selecciona una carta para jugar.");
        ManejadorEventosCliente.setPartidaController(this);
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    public void setJugadorActual(JavaBeans jugadorActual) {
        this.jugadorActual = jugadorActual;
    }

    public void actualizarCabecera(int ronda, String nombreRival, int puntos) {
        Platform.runLater(() -> {
            lblRonda.setText("RONDA: " + ronda);
            lblRival.setText("RIVAL: " + nombreRival);
            lblPuntos.setText("PUNTOS: " + puntos);
        });
    }

    public void renderizarMano(List<Node> vistasCartas) {
        Platform.runLater(() -> {
            limpiarSeleccion();
            turnoConfirmado = false;
            for (int i = 0; i < slotsMano.size(); i++) {
                VBox slot = slotsMano.get(i);
                slot.getChildren().clear();
                slot.setDisable(false);
                if (vistasCartas != null && i < vistasCartas.size()) {
                    slot.getChildren().add(vistasCartas.get(i));
                }
            }
        });
    }

    public void mostrarResultadoRonda(Node vistaCartaPropia, Node vistaCartaRival, String textoResultado,
            Node cartaGanadaPropia, Node cartaGanadaRival) {
        Platform.runLater(() -> {
            CartaTuyaLanzada.getChildren().clear();
            cartaEnemiga.getChildren().clear();

            if (vistaCartaPropia != null) {
                CartaTuyaLanzada.getChildren().add(vistaCartaPropia);
            }
            if (vistaCartaRival != null) {
                cartaEnemiga.getChildren().add(vistaCartaRival);
            }
            if (cartaGanadaPropia != null) {
                VboxTusCartasGanadas.getChildren().add(cartaGanadaPropia);
            }
            if (cartaGanadaRival != null) {
                VboxCartasRival.getChildren().add(cartaGanadaRival);
            }

            lblResultado.setText("Resultado: " + textoResultado);
            limpiarSeleccion();
            turnoConfirmado = false;
        });
    }

    public void habilitarFinPartida(String mensajeFinal) {
        Platform.runLater(() -> {
            lblResultado.setText("Resultado: " + mensajeFinal);
            btnConfirmar.setDisable(true);
            btnRevancha.setDisable(false);
            for (VBox slot : slotsMano) {
                slot.setDisable(true);
            }
        });
    }

    public void reiniciarParaRevancha(int rondaInicial, String nombreRival, int puntosIniciales, List<Node> nuevaMano) {
        Platform.runLater(() -> {
            VboxTusCartasGanadas.getChildren().clear();
            VboxCartasRival.getChildren().clear();
            CartaTuyaLanzada.getChildren().clear();
            cartaEnemiga.getChildren().clear();
            btnRevancha.setDisable(true);
            lblResultado.setText("Resultado: Revancha iniciada. Selecciona una carta.");
            actualizarCabecera(rondaInicial, nombreRival, puntosIniciales);
            renderizarMano(nuevaMano);
        });
    }

    public void regresarAlLobby() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"));
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
                lblResultado.setText("Resultado: Error al regresar al lobby.");
            }
        });
    }

    private void seleccionarCarta(int indice) {
        if (turnoConfirmado) {
            return;
        }
        for (int i = 0; i < slotsMano.size(); i++) {
            VBox slot = slotsMano.get(i);
            slot.getStyleClass().remove("carta-seleccionada");
            if (i == indice && !slot.getChildren().isEmpty()) {
                slot.getStyleClass().add("carta-seleccionada");
                indiceSeleccionado = indice;
            }
        }
        btnConfirmar.setDisable(indiceSeleccionado == -1);
    }

    private void limpiarSeleccion() {
        indiceSeleccionado = -1;
        btnConfirmar.setDisable(true);
        for (VBox slot : slotsMano) {
            slot.getStyleClass().remove("carta-seleccionada");
        }
    }

    @FXML
    private void onClickConfirmar(ActionEvent event) {
        if (indiceSeleccionado < 0 || turnoConfirmado) {
            return;
        }
        if (clienteSocket == null) {
            lblResultado.setText("Resultado: Sin conexión con el servidor.");
            return;
        }

        turnoConfirmado = true;
        btnConfirmar.setDisable(true);
        for (VBox slot : slotsMano) {
            slot.setDisable(true);
        }
        lblResultado.setText("Resultado: Carta enviada, esperando rival...");

        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.JUGAR_CARTA);
        mensaje.setIndiceCarta(indiceSeleccionado);
        clienteSocket.enviar(mensaje);
    }

    @FXML
    private void onClickRegresar(ActionEvent event) {
        btnRegresar.setDisable(true);
        if (clienteSocket != null) {
            MensajeProtocolo mensaje = new MensajeProtocolo();
            mensaje.setTipo(TipoAccion.SALIR_PARTIDA);
            clienteSocket.enviar(mensaje);
        } else {
            regresarAlLobby();
        }
    }

    @FXML
    private void onClickRevancha(ActionEvent event) {
        btnRevancha.setDisable(true);
        lblResultado.setText("Resultado: Solicitando revancha...");
        if (clienteSocket != null) {
            MensajeProtocolo mensaje = new MensajeProtocolo();
            mensaje.setTipo(TipoAccion.PEDIR_REVANCHA);
            clienteSocket.enviar(mensaje);
        }
    }
}