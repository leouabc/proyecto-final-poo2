package modelo;

import java.io.Serializable;
import java.math.BigDecimal;

/** Carta construida con los datos reales que entrega Dragon Ball API. */
public class Carta implements Serializable {
    private static final long serialVersionUID = 2L;

    private String nombre;
    private String raza;
    private String imagenUrl;
    /** Texto original del campo ki devuelto por la API (por ejemplo, "60 Septillion"). */
    private String poderApi;
    /** Valor normalizado para comparar poderes sin perder la escala indicada por la API. */
    private BigDecimal escalaPoder = BigDecimal.ZERO;

    public Carta() { }

    public Carta(String nombre, String raza, String imagenUrl, String poderApi, BigDecimal escalaPoder) {
        this.nombre = nombre;
        this.raza = raza;
        this.imagenUrl = imagenUrl;
        this.poderApi = poderApi;
        this.escalaPoder = escalaPoder == null ? BigDecimal.ZERO : escalaPoder;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getPoderApi() { return poderApi; }
    public void setPoderApi(String poderApi) { this.poderApi = poderApi; }

    public BigDecimal getEscalaPoder() { return escalaPoder == null ? BigDecimal.ZERO : escalaPoder; }
    public void setEscalaPoder(BigDecimal escalaPoder) {
        this.escalaPoder = escalaPoder == null ? BigDecimal.ZERO : escalaPoder;
    }

    public Carta copia() {
        return new Carta(nombre, raza, imagenUrl, poderApi, getEscalaPoder());
    }
}
