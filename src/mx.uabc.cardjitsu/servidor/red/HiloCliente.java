package servidor.red;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;
import servidor.datos.RepositorioJugadores;

public class HiloCliente implements Runnable {
    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private RepositorioJugadores repositorio;
    private JavaBeans jugadorActual;
    private SalaJuego salaActual;
    private TextArea areaServidor;
    private String ipCliente;
    private volatile boolean conectado;

    public HiloCliente(Socket socket, TextArea areaServidor) {
        this.socket = socket;
        this.areaServidor = areaServidor;
        this.ipCliente = socket.getInetAddress().getHostAddress();
        this.repositorio = new RepositorioJugadores();
        this.conectado = true;
    }

    @Override
    public void run() {
        try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            while (conectado) {
                Object objeto = entrada.readObject();
                if (objeto instanceof MensajeProtocolo) {
                    procesarMensaje((MensajeProtocolo) objeto);
                }
            }
        } catch (Exception e) {
            desconectar();
        }
    }

    private void procesarMensaje(MensajeProtocolo mensaje) {
        TipoAccion accion = mensaje.getAccion();

        if (accion == TipoAccion.REGISTRO) {
            JavaBeans datos = (JavaBeans) mensaje.getDatos();
            boolean registrado = repositorio.registrarJugador(datos.getNombre(), datos.getContra());
            enviar(new MensajeProtocolo(TipoAccion.REGISTRO, registrado));
            registrarLog("Registro de " + datos.getNombre() + ": " + (registrado ? "OK" : "Ya existe"));

        } else if (accion == TipoAccion.LOGIN) {
            JavaBeans datos = (JavaBeans) mensaje.getDatos();
            JavaBeans jugador = repositorio.iniciarSesion(datos.getNombre(), datos.getContra());
            if (jugador != null) {
                this.jugadorActual = jugador;
                registrarLog("Guerrero conectado: " + jugador.getNombre());
            }
            enviar(new MensajeProtocolo(TipoAccion.LOGIN, jugador));

        } else if (accion == TipoAccion.RANKING) {
            enviar(new MensajeProtocolo(TipoAccion.RANKING, repositorio.obtenerRanking()));

        } else if (accion == TipoAccion.BUSCAR_PARTIDA) {
            SalaJuego.emparejarJugador(this);
            registrarLog((jugadorActual != null ? jugadorActual.getNombre() : ipCliente) + " buscando partida...");

        } else if (accion == TipoAccion.JUGAR_CARTA) {
            if (salaActual != null) {
                String carta = (String) mensaje.getDatos();
                salaActual.registrarJugada(this, carta);
            }

        } else if (accion == TipoAccion.DESCONECTAR) {
            desconectar();
        }
    }

    public synchronized void enviar(MensajeProtocolo mensaje) {
        try {
            if (salida != null && !socket.isClosed()) {
                salida.reset();
                salida.writeObject(mensaje);
                salida.flush();
            }
        } catch (IOException e) {
            desconectar();
        }
    }

    private void registrarLog(String texto) {
        if (areaServidor != null) {
            Platform.runLater(() -> areaServidor.appendText(texto + "\n"));
        }
    }

    public void setSalaActual(SalaJuego salaActual) {
        this.salaActual = salaActual;
    }

    public JavaBeans getJugadorActual() {
        return jugadorActual;
    }

    public void desconectar() {
        if (!conectado) {
            return;
        }
        conectado = false;
        SalaJuego.removerDeCola(this);
        if (salaActual != null) {
            salaActual.notificarAbandono(this);
        }
        registrarLog("El cliente " + ipCliente + " se desconecto!");
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
