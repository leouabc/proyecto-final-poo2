package protocolo;

import java.io.Serializable;
import java.util.List;
import modelo.JavaBeans;

public class MensajeProtocolo implements Serializable {
    private static final long serialVersionUID = 1L;

    private TipoAccion tipo;
    private String nombreUsuario;
    private String contrasena;
    private boolean exito;
    private String mensaje;
    private JavaBeans jugador;
    private List<JavaBeans> ranking;
    private int indiceCarta;
    private List<String> mano;
    private int ronda;
    private String cartaPropia;
    private String cartaRival;

    public MensajeProtocolo() {
    }

    public MensajeProtocolo(TipoAccion tipo) {
        this.tipo = tipo;
    }

    public TipoAccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoAccion tipo) {
        this.tipo = tipo;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public JavaBeans getJugador() {
        return jugador;
    }

    public void setJugador(JavaBeans jugador) {
        this.jugador = jugador;
    }

    public List<JavaBeans> getRanking() {
        return ranking;
    }

    public void setRanking(List<JavaBeans> ranking) {
        this.ranking = ranking;
    }

    public int getIndiceCarta() {
        return indiceCarta;
    }

    public void setIndiceCarta(int indiceCarta) {
        this.indiceCarta = indiceCarta;
    }

    public List<String> getMano() {
        return mano;
    }

    public void setMano(List<String> mano) {
        this.mano = mano;
    }

    public int getRonda() {
        return ronda;
    }

    public void setRonda(int ronda) {
        this.ronda = ronda;
    }

    public String getCartaPropia() {
        return cartaPropia;
    }

    public void setCartaPropia(String cartaPropia) {
        this.cartaPropia = cartaPropia;
    }

    public String getCartaRival() {
        return cartaRival;
    }

    public void setCartaRival(String cartaRival) {
        this.cartaRival = cartaRival;
    }
}
