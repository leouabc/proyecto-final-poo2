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
        TipoAccion tipo = mensaje.getTipo();

        if (tipo == TipoAccion.REGISTRO) {
            String nombre = mensaje.getNombreUsuario();
            String contra = mensaje.getContrasena();
            boolean registrado = repositorio.registrarJugador(nombre, contra);
            JavaBeans jugador = registrado ? repositorio.buscarPorNombre(nombre) : null;

            MensajeProtocolo respuesta = new MensajeProtocolo();
            respuesta.setTipo(TipoAccion.REGISTRO);
            respuesta.setExito(registrado);
            respuesta.setMensaje(registrado ? "Registro exitoso" : "El guerrero ya existe");
            respuesta.setJugador(jugador);
            if (registrado) {
                this.jugadorActual = jugador;
            }
            enviar(respuesta);
            registrarLog("Registro de " + nombre + ": " + (registrado ? "OK" : "Ya existe"));

        } else if (tipo == TipoAccion.LOGIN) {
            String nombre = mensaje.getNombreUsuario();
            String contra = mensaje.getContrasena();
            JavaBeans jugador = repositorio.iniciarSesion(nombre, contra);
            boolean exito = (jugador != null);

            MensajeProtocolo respuesta = new MensajeProtocolo();
            respuesta.setTipo(TipoAccion.LOGIN);
            respuesta.setExito(exito);
            respuesta.setMensaje(exito ? "Bienvenido al torneo" : "Usuario o contraseña incorrectos");
            respuesta.setJugador(jugador);
            if (exito) {
                this.jugadorActual = jugador;
                registrarLog("Guerrero conectado: " + jugador.getNombre());
            }
            enviar(respuesta);

        } else if (tipo == TipoAccion.RANKING) {
            MensajeProtocolo respuesta = new MensajeProtocolo();
            respuesta.setTipo(TipoAccion.RANKING);
            respuesta.setRanking(repositorio.obtenerRanking());
            enviar(respuesta);

        } else if (tipo == TipoAccion.CREAR_SALA || tipo == TipoAccion.UNIRSE_SALA || tipo == TipoAccion.BUSCAR_PARTIDA) {
            SalaJuego.emparejarJugador(this);
            registrarLog((jugadorActual != null ? jugadorActual.getNombre() : ipCliente) + " buscando combate...");

        } else if (tipo == TipoAccion.JUGAR_CARTA) {
            if (salaActual != null) {
                salaActual.registrarJugada(this, mensaje.getIndiceCarta());
            }

        } else if (tipo == TipoAccion.PEDIR_REVANCHA) {
            if (salaActual != null) {
                salaActual.solicitarRevancha(this);
            }

        } else if (tipo == TipoAccion.SALIR_PARTIDA || tipo == TipoAccion.CANCELAR_ESPERA) {
            SalaJuego.removerDeCola(this);
            if (salaActual != null) {
                salaActual.notificarAbandono(this);
            }
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
