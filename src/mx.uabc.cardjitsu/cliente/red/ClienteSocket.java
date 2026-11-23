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
    private static boolean escuchando = false;

    private static final Map<String, String> usuariosRegistrado = new HashMap<>();

    @FXML
    public void initialize() {
        if (cliente == null) {

        }
        try {
            cliente = new Socket("localhost", 35557);
            salida = new ObjectOutputStream(cliente.getOutputStream());
            entrada = new ObjectInputStream(cliente.getInputStream());

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @FXML 
    public void onClickRegistrarse(ActionEvent e) {
        usuario = txtNombre.getText().trim();
        String password = txtContra.getText().trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos Vacios", "Algunos campos estan vacios");
            return;
        }
        try {
            jugador = new JavaBeans();
            jugador.setNombre(usuario);
            jugador.setContra(password);

            // Solo envías la petición al servidor (sin cambiar de pantalla aquí)
            MensajeProtocolo msg = new MensajeProtocolo(TipoAccion.REGISTRO, jugador);
            salida.writeObject(msg);
            salida.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onClickEntrarTorneo(ActionEvent e) {
    usuario = txtNombre.getText().trim();
    String password = txtContra.getText().trim();

        if (usuariosRegistrado.containsKey(usuario) && usuariosRegistrado.get(usuario).equals(password)) {
            cambiarPantalla(e, "/cliente/gui/lobby.fxml");
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al iniciar el juego", "Intentalo de nuevo, un error a ocurrido.");
        }
    }
    private void cambiarPantalla(ActionEvent event, String rutaFxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(rutaFxml));
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