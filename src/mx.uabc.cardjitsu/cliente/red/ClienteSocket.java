package cliente.red;

import cliente.gui.GameUI.LobbyController;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class ClienteSocket {
    private static final String HOST = System.getProperty("cardjitsu.host", "localhost");
    private static final int PUERTO = Integer.getInteger("cardjitsu.port", 35557);

    @FXML private TextField txtNombre;
    @FXML private PasswordField txtContra;
    @FXML private Button btnRegistrar;
    @FXML private Button btnEntrar;
    @FXML private Label txtInfo;

    private JavaBeans jugador = new JavaBeans();
    private Socket cliente;
    private ObjectInputStream entrada;
    private ObjectOutputStream salida;

    @FXML
    public void initialize() {
        boolean conectado = conectar();
        if (txtInfo != null) txtInfo.setText(conectado
                ? "Conectado al servidor. Registra tu cuenta o inicia sesión."
                : "Servidor no disponible. Comprueba que esté iniciado.");
    }

    public synchronized boolean conectar() {
        if (cliente != null && cliente.isConnected() && !cliente.isClosed()) return true;
        try {
            cliente = new Socket(HOST, PUERTO);
            cliente.setKeepAlive(true);
            salida = new ObjectOutputStream(cliente.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(cliente.getInputStream());
            ManejadorEventosCliente.setClienteSocketActual(this);
            ManejadorEventosCliente.escucharServidor(entrada);
            return true;
        } catch (IOException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Sin conexión",
                    "No fue posible conectar con el servidor. Verifica que esté iniciado.");
            cerrarConexion();
            return false;
        }
    }

    public synchronized void enviar(MensajeProtocolo mensaje) {
        if (mensaje == null) return;
        try {
            if (salida == null || cliente == null || cliente.isClosed()) {
                if (!conectar()) {
                    bloquearAcceso(false);
                    if (txtInfo != null) txtInfo.setText("No hay conexión con el servidor.");
                    return;
                }
            }
            salida.reset();
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de conexión",
                    "No se pudo enviar la solicitud al servidor.");
            bloquearAcceso(false);
            if (txtInfo != null) txtInfo.setText("Se perdió la conexión con el servidor.");
            cerrarConexion();
        }
    }

    public JavaBeans getJugador() { return jugador; }

    @FXML
    public void onClickRegistrarse(ActionEvent event) {
        String nombre = txtNombre == null ? "" : txtNombre.getText().trim();
        String password = txtContra == null ? "" : txtContra.getText();
        if (!validarCredenciales(nombre, password)) return;
        MensajeProtocolo solicitud = new MensajeProtocolo(TipoAccion.REGISTRO);
        solicitud.setNombreUsuario(nombre);
        solicitud.setContrasena(password);
        bloquearAcceso(true);
        if (txtInfo != null) txtInfo.setText("Enviando registro...");
        enviar(solicitud);
    }

    @FXML
    public void onClickEntrarTorneo(ActionEvent event) {
        String nombre = txtNombre == null ? "" : txtNombre.getText().trim();
        String password = txtContra == null ? "" : txtContra.getText();
        if (!validarCredenciales(nombre, password)) return;
        MensajeProtocolo solicitud = new MensajeProtocolo(TipoAccion.LOGIN);
        solicitud.setNombreUsuario(nombre);
        solicitud.setContrasena(password);
        bloquearAcceso(true);
        if (txtInfo != null) txtInfo.setText("Validando credenciales...");
        enviar(solicitud);
    }

    private void bloquearAcceso(boolean bloqueado) {
        if (btnRegistrar != null) btnRegistrar.setDisable(bloqueado);
        if (btnEntrar != null) btnEntrar.setDisable(bloqueado);
    }

    private boolean validarCredenciales(String nombre, String password) {
        if (nombre.isEmpty() || password.trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos",
                    "Escribe tu nombre de guerrero y una contraseña.");
            return false;
        }
        if (nombre.length() < 3 || nombre.length() > 24 || !nombre.matches("[a-zA-Z0-9_]+")) {
            mostrarAlerta(Alert.AlertType.WARNING, "Nombre no válido",
                    "El nombre debe tener entre 3 y 24 caracteres: letras, números o guion bajo.");
            return false;
        }
        if (password.length() < 4 || password.length() > 72) {
            mostrarAlerta(Alert.AlertType.WARNING, "Contraseña no válida",
                    "La contraseña debe tener entre 4 y 72 caracteres.");
            return false;
        }
        return true;
    }

    public void procesarRespuestaAcceso(MensajeProtocolo respuesta) {
        Platform.runLater(() -> {
            if (respuesta.getTipo() == TipoAccion.REGISTRO) {
                bloquearAcceso(false);
                if (txtInfo != null) txtInfo.setText(respuesta.getMensaje() == null ? "" : respuesta.getMensaje());
                if (respuesta.isExito()) {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Registro exitoso",
                            "Tu cuenta fue creada. Ahora puedes iniciar sesión.");
                    if (txtContra != null) txtContra.clear();
                } else {
                    mostrarAlerta(Alert.AlertType.ERROR, "No se pudo registrar",
                            respuesta.getMensaje());
                }
                return;
            }
            if (respuesta.getTipo() == TipoAccion.LOGIN) {
                bloquearAcceso(false);
                if (!respuesta.isExito() || respuesta.getJugador() == null) {
                    if (txtInfo != null) txtInfo.setText(respuesta.getMensaje() == null ? "Acceso denegado." : respuesta.getMensaje());
                    mostrarAlerta(Alert.AlertType.ERROR, "Acceso denegado", respuesta.getMensaje());
                    return;
                }
                jugador = respuesta.getJugador();
                abrirLobby();
            }
        });
    }

    private void abrirLobby() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/lobby.fxml"));
            Parent root = loader.load();
            LobbyController controller = loader.getController();
            controller.setClienteSocket(this);
            controller.setJugadorActual(jugador);
            Stage stage = txtNombre != null && txtNombre.getScene() != null
                    ? (Stage) txtNombre.getScene().getWindow() : null;
            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (IOException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el lobby: " + ex.getMessage());
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje == null ? "" : mensaje);
        alerta.show();
    }

    public synchronized void cerrarConexion() {
        try { if (entrada != null) entrada.close(); } catch (IOException ignored) {}
        try { if (salida != null) salida.close(); } catch (IOException ignored) {}
        try { if (cliente != null) cliente.close(); } catch (IOException ignored) {}
        entrada = null; salida = null; cliente = null;
    }
}
