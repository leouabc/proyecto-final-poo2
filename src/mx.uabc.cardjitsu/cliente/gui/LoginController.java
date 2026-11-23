import com.cardjitsu.modelo.JavaBeans;
import com.cardjitsu.protocolo.MensajeProtocolo;
import com.cardjitsu.protocolo.TipoAccion;
import com.cardjitsu.red.ClienteSocket;
import com.cardjitsu.red.ManejadorEventosCliente;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField txtNombre;

    @FXML
    private PasswordField txtContra;

    @FXML
    private Button btnRegistrar;

    @FXML
    private Button btnEntrar;

    @FXML
    private Label txtInfo;

    private ClienteSocket clienteSocket;

    @FXML
    private void initialize() {
        txtInfo.setText("");
        ManejadorEventosCliente.setLoginController(this);
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    @FXML
    private void onClickRegistrarse(ActionEvent event) {
        String nombre = txtNombre.getText().trim();
        String contra = txtContra.getText();

        if (!validarCredenciales(nombre, contra)) {
            return;
        }

        enviarAccionAuth(TipoAccion.REGISTRO, nombre, contra, "Registrando guerrero...");
    }

    @FXML
    private void onClickEntrarTorneo(ActionEvent event) {
        String nombre = txtNombre.getText().trim();
        String contra = txtContra.getText();

        if (!validarCredenciales(nombre, contra)) {
            return;
        }

        enviarAccionAuth(TipoAccion.LOGIN, nombre, contra, "Verificando credenciales...");
    }

    private void enviarAccionAuth(TipoAccion accion, String nombre, String contra, String mensajeEspera) {
        if (clienteSocket == null) {
            txtInfo.setText("No hay conexión con el servidor.");
            return;
        }

        establecerCargando(true);
        txtInfo.setText(mensajeEspera);

        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(accion);
        mensaje.setNombreUsuario(nombre);
        mensaje.setContrasena(contra);

        clienteSocket.enviar(mensaje);
    }

    public void mostrarRespuestaAuth(boolean exito, String mensaje, JavaBeans jugador) {
        Platform.runLater(() -> {
            txtInfo.setText(mensaje);

            if (!exito) {
                establecerCargando(false);
                return;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"));
                Parent root = loader.load();
                LobbyController lobbyController = loader.getController();
                lobbyController.setClienteSocket(clienteSocket);
                lobbyController.setJugadorActual(jugador);
                Stage stage = (Stage) btnEntrar.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                establecerCargando(false);
                txtInfo.setText("Error al cargar el lobby.");
            }
        });
    }

    private boolean validarCredenciales(String nombre, String contra) {
        if (nombre.isEmpty() || contra.isEmpty()) {
            txtInfo.setText("Ingresa tu nombre y contraseña.");
            return false;
        }
        return true;
    }

    private void establecerCargando(boolean cargando) {
        btnRegistrar.setDisable(cargando);
        btnEntrar.setDisable(cargando);
        txtNombre.setDisable(cargando);
        txtContra.setDisable(cargando);
    }
}