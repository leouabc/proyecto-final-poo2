package mx.uabc.cardjitsu.modelo;

import java.io.Serializable;

public class Carta implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String nombre;
    private String elemento; // Fuego, Agua, Nieve
    private int poder;
    private String color;

    public Carta() {
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getElemento() { return elemento; }
    public void setElemento(String elemento) { this.elemento = elemento; }

    public int getPoder() { return poder; }
    public void setPoder(int poder) { this.poder = poder; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
