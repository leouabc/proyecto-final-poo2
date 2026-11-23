package modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Jugador implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nombreUsuario;
    private int cinturonNivel;
    private List<Carta> cartasMano;

    public Jugador() {
        this.cartasMano = new ArrayList<>();
    }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public int getCinturonNivel() { return cinturonNivel; }
    public void setCinturonNivel(int cinturonNivel) { this.cinturonNivel = cinturonNivel; }

    public List<Carta> getCartasMano() { return cartasMano; }
    public void setCartasMano(List<Carta> cartasMano) { this.cartasMano = cartasMano; }
}
