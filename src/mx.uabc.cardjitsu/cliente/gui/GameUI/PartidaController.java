package cliente.gui.GameUI;

import cliente.red.ClienteSocket;
import cliente.red.ManejadorEventosCliente;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modelo.Carta;
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
    private TextArea txtAreaChat;

    @FXML
    private VBox VboxCartasRival;

    @FXML
    private TextField txtMensaje;

    @FXML
    private Button btnEnviar;

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
    private List<Carta> manoActual = new ArrayList<>();
    private final List<VBox> slotsMano = new ArrayList<>();
    private int indiceSeleccionado = -1;
    private boolean jugadaConfirmada;

    @FXML
    private void initialize() {
        ManejadorEventosCliente.registrarPartida(this);
        slotsMano.clear();
        if (carta1 != null) slotsMano.add(carta1);
        if (carta2 != null) slotsMano.add(carta2);
        if (carta3 != null) slotsMano.add(carta3);
        if (carta4 != null) slotsMano.add(carta4);
        if (carta5 != null) slotsMano.add(carta5);

        for (int i = 0; i < slotsMano.size(); i++) {
            final int indice = i;
            VBox slot = slotsMano.get(i);
            slot.setOnMouseClicked(event -> seleccionarCarta(indice));
        }

        if (btnRevancha != null) {
            btnRevancha.setDisable(true);
        }
        if (btnConfirmar != null) {
            btnConfirmar.setDisable(true);
        }
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
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

    @FXML
    private void enviarMensaje(ActionEvent event) {
        if (txtMensaje == null) {
            return;
        }
        String texto = txtMensaje.getText() != null ? txtMensaje.getText().trim() : "";
        if (texto.isEmpty()) {
            return;
        }

        String remitente = (jugadorActual != null && jugadorActual.getNombre() != null && !jugadorActual.getNombre().isBlank())
                ? jugadorActual.getNombre()
                : "Tú";

        recibirMensajeChat(remitente, texto);
        txtMensaje.clear();

        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.CHAT);
        mensaje.setNombreUsuario(remitente);
        mensaje.setMensaje(texto);
        socket.enviar(mensaje);
    }

    public void recibirMensajeChat(String remitente, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        String autor = (remitente != null && !remitente.isBlank()) ? remitente : "Jugador";
        Platform.runLater(() -> {
            if (txtAreaChat != null) {
                txtAreaChat.appendText(autor + ": " + texto + "\n");
            }
        });
    }

    public void actualizarCabecera(int ronda, String nombreRival, int puntos) {
        Platform.runLater(() -> {
            if (lblRonda != null) {
                lblRonda.setText("RONDA: " + ronda);
            }
            if (lblRival != null) {
                lblRival.setText("RIVAL: " + (nombreRival != null ? nombreRival : "Esperando rival"));
            }
            if (lblPuntos != null) {
                lblPuntos.setText("PUNTOS: " + puntos);
            }
        });
    }

    public void renderizarMano(List<Carta> cartas) {
        Platform.runLater(() -> {
            manoActual = (cartas != null) ? new ArrayList<>(cartas) : new ArrayList<>();
            indiceSeleccionado = -1;
            jugadaConfirmada = false;

            if (btnConfirmar != null) {
                btnConfirmar.setDisable(true);
            }

            for (int i = 0; i < slotsMano.size(); i++) {
                VBox slot = slotsMano.get(i);
                slot.getChildren().clear();
                slot.setDisable(false);
                if (i < manoActual.size()) {
                    slot.getChildren().add(crearTarjetaVisual(manoActual.get(i)));
                }
            }
        });
    }

    private void seleccionarCarta(int indice) {
        if (jugadaConfirmada || indice < 0 || indice >= manoActual.size()) {
            return;
        }
        indiceSeleccionado = indice;
        if (btnConfirmar != null) {
            btnConfirmar.setDisable(false);
        }
        if (lblResultado != null) {
            lblResultado.setText("Resultado: Seleccionaste " + formatearCarta(manoActual.get(indice)));
        }
    }

    @FXML
    private void onClickConfirmar(ActionEvent event) {
        if (jugadaConfirmada || indiceSeleccionado < 0 || indiceSeleccionado >= manoActual.size()) {
            return;
        }
        jugadaConfirmada = true;
        if (btnConfirmar != null) {
            btnConfirmar.setDisable(true);
        }
        bloquearMano(true);

        Carta cartaElegida = manoActual.get(indiceSeleccionado);
        if (CartaTuyaLanzada != null) {
            CartaTuyaLanzada.getChildren().setAll(crearTarjetaVisual(cartaElegida));
        }
        if (lblResultado != null) {
            lblResultado.setText("Resultado: Esperando jugada del rival...");
        }

        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.JUGAR_CARTA);
        mensaje.setIndiceCarta(indiceSeleccionado);
        socket.enviar(mensaje);
    }

    public void mostrarResultadoRonda(Carta cartaPropia, Carta cartaRival, String resultadoTexto, int nuevaRonda, int puntosActuales, List<Carta> nuevaMano, boolean ganoRonda) {
        Platform.runLater(() -> {
            if (CartaTuyaLanzada != null) {
                CartaTuyaLanzada.getChildren().clear();
                if (cartaPropia != null) {
                    CartaTuyaLanzada.getChildren().add(crearTarjetaVisual(cartaPropia));
                }
            }
            if (cartaEnemiga != null) {
                cartaEnemiga.getChildren().clear();
                if (cartaRival != null) {
                    cartaEnemiga.getChildren().add(crearTarjetaVisual(cartaRival));
                }
            }
            if (ganoRonda && cartaRival != null && VboxTusCartasGanadas != null) {
                VboxTusCartasGanadas.getChildren().add(crearTarjetaVisual(cartaRival));
            } else if (!ganoRonda && cartaPropia != null && VboxCartasRival != null) {
                VboxCartasRival.getChildren().add(crearTarjetaVisual(cartaPropia));
            }
            if (lblResultado != null) {
                lblResultado.setText("Resultado: " + (resultadoTexto != null ? resultadoTexto : ""));
            }
            actualizarCabecera(nuevaRonda, lblRival != null ? lblRival.getText().replace("RIVAL: ", "") : "", puntosActuales);
            if (nuevaMano != null) {
                renderizarMano(nuevaMano);
            } else {
                jugadaConfirmada = false;
                bloquearMano(false);
            }
        });
    }

    public void habilitarFinPartida(String resumenFinal) {
        Platform.runLater(() -> {
            bloquearMano(true);
            if (btnConfirmar != null) {
                btnConfirmar.setDisable(true);
            }
            if (lblResultado != null && resumenFinal != null) {
                lblResultado.setText("Resultado: " + resumenFinal);
            }
            if (btnRevancha != null) {
                btnRevancha.setDisable(false);
            }
        });
    }

    public void reiniciarParaRevancha(int rondaInicial, String nombreRival, int puntosIniciales, List<Carta> cartasIniciales) {
        Platform.runLater(() -> {
            if (btnRevancha != null) {
                btnRevancha.setDisable(true);
            }
            if (CartaTuyaLanzada != null) {
                CartaTuyaLanzada.getChildren().clear();
            }
            if (cartaEnemiga != null) {
                cartaEnemiga.getChildren().clear();
            }
            if (VboxTusCartasGanadas != null) {
                VboxTusCartasGanadas.getChildren().clear();
            }
            if (VboxCartasRival != null) {
                VboxCartasRival.getChildren().clear();
            }
            if (lblResultado != null) {
                lblResultado.setText("Resultado: ¡Revancha iniciada!");
            }
            actualizarCabecera(rondaInicial, nombreRival, puntosIniciales);
            renderizarMano(cartasIniciales);
        });
    }

    @FXML
    private void onClickRevancha(ActionEvent event) {
        if (btnRevancha != null) {
            btnRevancha.setDisable(true);
        }
        if (lblResultado != null) {
            lblResultado.setText("Resultado: Solicitando revancha...");
        }

        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.PEDIR_REVANCHA);
        socket.enviar(mensaje);
    }

    @FXML
    private void onClickRegresar(ActionEvent event) {
        if (btnRegresar != null) {
            btnRegresar.setDisable(true);
        }

        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.SALIR_PARTIDA);
        socket.enviar(mensaje);

        volverAlLobby();
    }

    public void volverAlLobby() {
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
                if (btnRegresar != null) {
                    btnRegresar.setDisable(false);
                }
            }
        });
    }

    private VBox crearTarjetaVisual(Carta carta) {
        VBox box = new VBox(4);
        String nombre = (carta != null && carta.getNombre() != null) ? carta.getNombre() : "Carta";
        String elemento = (carta != null && carta.getElemento() != null) ? carta.getElemento() : "";
        int poder = (carta != null) ? carta.getPoder() : 0;

        box.getChildren().addAll(
                new Label(nombre),
                new Label(elemento),
                new Label("Poder: " + poder)
        );
        return box;
    }

    private String formatearCarta(Carta carta) {
        if (carta == null) {
            return "Carta";
        }
        String nombre = carta.getNombre() != null ? carta.getNombre() : "Sin nombre";
        String elemento = carta.getElemento() != null ? carta.getElemento() : "";
        return nombre + (elemento.isEmpty() ? "" : " [" + elemento + "]") + " - Poder " + carta.getPoder();
    }

    private void bloquearMano(boolean bloqueada) {
        for (VBox slot : slotsMano) {
            slot.setDisable(bloqueada);
        }
    }
}