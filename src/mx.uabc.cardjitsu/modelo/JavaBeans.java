package modelo;

import java.io.Serializable;

public class JavaBeans implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nombre;
    private String contra;
    private String rango;
    private int victorias;
    private int derrotas;
    private int puntos;

    public JavaBeans() {
    }

    public JavaBeans(String nombre, String contra) {
        this.nombre = nombre;
        this.contra = contra;
        this.rango = "Guerrero Clase Baja";
        this.victorias = 0;
        this.derrotas = 0;
        this.puntos = 0;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContra() {
        return contra;
    }

    public void setContra(String contra) {
        this.contra = contra;
    }

    public String getRango() {
        return rango;
    }

    public void setRango(String rango) {
        this.rango = rango;
    }

    public int getVictorias() {
        return victorias;
    }

    public void setVictorias(int victorias) {
        this.victorias = victorias;
    }

    public int getDerrotas() {
        return derrotas;
    }

    public void setDerrotas(int derrotas) {
        this.derrotas = derrotas;
    }

    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }
}
