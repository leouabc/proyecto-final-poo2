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
        ManejadorEventosCliente.setEsperaController(this);
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
            lblNombreDeSala.setText("Nombre de sala: " + nombreSala);
            lblCodigoSala.setText("Código de sala: " + codigoSala);
            btnCancelar.setDisable(false);
        });
    }

    @FXML
    private void onClickBotonCancelar(ActionEvent event) {
        btnCancelar.setDisable(true);
        if (clienteSocket != null) {
            MensajeProtocolo mensaje = new MensajeProtocolo();
            mensaje.setTipo(TipoAccion.SALIR_SALA);
            mensaje.setDato(codigoSala);
            clienteSocket.enviar(mensaje);
        }
    }

    public void onPartidaLista(int rondaInicial, String nombreRival, int puntosIniciales) {
        Platform.runLater(() -> {
            btnCancelar.setDisable(true);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("partida.fxml"));
                Parent root = loader.load();
                PartidaController partidaController = loader.getController();
                partidaController.setClienteSocket(clienteSocket);
                partidaController.setJugadorActual(jugadorActual);
                partidaController.actualizarCabecera(rondaInicial, nombreRival, puntosIniciales);
                Stage stage = (Stage) btnCancelar.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                btnCancelar.setDisable(false);
            }
        });
    }

    public void onSalaCancelada() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"));
                Parent root = loader.load();
                LobbyController lobbyController = loader.getController();
                lobbyController.setClienteSocket(clienteSocket);
                if (jugadorActual != null) {
                    lobbyController.setJugadorActual(jugadorActual);
                }
                Stage stage = (Stage) btnCancelar.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                btnCancelar.setDisable(false);
            }
        });
    }
}