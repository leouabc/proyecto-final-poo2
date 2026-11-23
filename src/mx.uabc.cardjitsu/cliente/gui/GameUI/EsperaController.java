package cliente.gui.GameUI;

import cliente.red.ClienteSocket;
import cliente.red.ManejadorEventosCliente;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import modelo.JavaBeans;
import modelo.Carta;
import java.util.List;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class EsperaController {

    @FXML
    private Label lblNombreDeSala;

    @FXML
    private Label lblCodigoSala;

    @FXML
    private Button btnCancelar;

    private ClienteSocket clienteSocket;
    private JavaBeans jugadorActual;
    private String nombreSala;
    private String codigoSala;

    @FXML
    private void initialize() {
        ManejadorEventosCliente.registrarEspera(this);
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    public void setJugadorActual(JavaBeans jugadorActual) {
        this.jugadorActual = jugadorActual;
    }

    public void setDatosSala(String nombreSala, String codigoSala) {
        this.nombreSala = nombreSala;
        this.codigoSala = codigoSala;
        Platform.runLater(() -> {
            if (lblNombreDeSala != null) {
                lblNombreDeSala.setText("Nombre de sala: " + nombreSala);
            }
            if (lblCodigoSala != null) {
                lblCodigoSala.setText("Código de sala: " + codigoSala);
            }
            if (btnCancelar != null) {
                btnCancelar.setDisable(false);
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
    private void onClickBotonCancelar(ActionEvent event) {
        if (btnCancelar != null) {
            btnCancelar.setDisable(true);
        }
        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.CANCELAR_ESPERA);
        mensaje.setMensaje(codigoSala);
        socket.enviar(mensaje);
    }

    public void mostrarEstado(String estado) {
        Platform.runLater(() -> {
            if (lblNombreDeSala != null && estado != null && !estado.isBlank()) {
                lblNombreDeSala.setText(estado);
            }
        });
    }

    public void onPartidaLista(int rondaInicial, String nombreRival, int puntosIniciales) {
        onPartidaLista(rondaInicial, nombreRival, puntosIniciales, null);
    }

    public void onPartidaLista(int rondaInicial, String nombreRival, int puntosIniciales, List<Carta> mano) {
        Platform.runLater(() -> {
            if (btnCancelar != null) {
                btnCancelar.setDisable(true);
            }
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/partida.fxml"));
                Parent root = loader.load();
                PartidaController partidaController = loader.getController();
                partidaController.setClienteSocket(clienteSocket);
                partidaController.setJugadorActual(jugadorActual);
                partidaController.actualizarCabecera(rondaInicial, nombreRival, puntosIniciales);
                partidaController.renderizarMano(mano);
                Stage stage = (Stage) btnCancelar.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                if (btnCancelar != null) {
                    btnCancelar.setDisable(false);
                }
            }
        });
    }

    public void onSalaCancelada() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/lobby.fxml"));
                Parent root = loader.load();
                LobbyController lobbyController = loader.getController();
                lobbyController.setClienteSocket(clienteSocket);
                if (jugadorActual != null) {
                    lobbyController.setJugadorActual(jugadorActual);
                }
                Stage stage = (Stage) btnCancelar.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                if (btnCancelar != null) {
                    btnCancelar.setDisable(false);
                }
            }
        });
    }
}