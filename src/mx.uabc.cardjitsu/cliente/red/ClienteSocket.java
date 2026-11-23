package cliente.red;

import cliente.gui.GameUI.LobbyController;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;

public class ClienteSocket {

    @FXML
    private TextField txtNombre;

    @FXML
    private PasswordField txtContra;

    private JavaBeans jugador = new JavaBeans();

    private static String usuario;
    private static Socket cliente;
    private static ObjectInputStream entrada;
    private static ObjectOutputStream salida;
    private static final Map<String, String> usuariosRegistrado = new HashMap<>();

    @FXML
    public void initialize() {
        if (cliente != null && !cliente.isClosed()) {
            return;
        }
        try {
            cliente = new Socket("localhost", 35557);
            salida = new ObjectOutputStream(cliente.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(cliente.getInputStream());
            ManejadorEventosCliente.setClienteSocketActual(this);
            ManejadorEventosCliente.escucharServidor(entrada);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void enviar(MensajeProtocolo mensaje) {
        if (mensaje == null) {
            return;
        }
        try {
            if (salida != null && cliente != null && !cliente.isClosed()) {
                salida.reset();
                salida.writeObject(mensaje);
                salida.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JavaBeans getJugador() {
        return jugador;
    }

    @FXML
    public void onClickRegistrarse(ActionEvent e) {
        usuario = txtNombre.getText().trim();
        String password = txtContra.getText().trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos Vacios", "Algunos campos estan vacios");
            return;
        }
        if (usuariosRegistrado.containsKey(usuario)) {
            mostrarAlerta(Alert.AlertType.ERROR, "El usuario ya existe!", "El usuario que ingresaste, ya existe");
        } else {
            usuariosRegistrado.put(usuario, password);
            jugador.setContra(password);
            jugador.setNombre(usuario);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Registro exitoso", "Tu cuenta se creo!\n Bienvenid@ a Dragon Ball Jujitsu");
            txtContra.clear();
            txtNombre.clear();
        }
    }

    @FXML
    public void onClickEntrarTorneo(ActionEvent e) {
        usuario = txtNombre.getText().trim();
        String password = txtContra.getText().trim();

        if (usuariosRegistrado.containsKey(usuario) && usuariosRegistrado.get(usuario).equals(password)) {
            jugador.setNombre(usuario);
            jugador.setContra(password);
            cambiarPantalla(e, "/cliente/gui/lobby.fxml");
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al iniciar el juego", "Intentalo de nuevo, un error a ocurrido.");
        }
    }

    private void cambiarPantalla(ActionEvent event, String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof LobbyController) {
                LobbyController lobby = (LobbyController) controller;
                lobby.setClienteSocket(this);
                lobby.setJugadorActual(jugador);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo cargar la pantalla del torneo.");
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}