import static org.junit.jupiter.api.Assertions.*;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import modelo.Carta;
import modelo.DragonBallApiService;
import modelo.EstadoJuego;
import modelo.HistorialPartida;
import modelo.InspectorReglasUtil;
import modelo.Jugador;
import org.junit.jupiter.api.Test;

class PruebasModeloTest {
    @Test
    void parseaPersonajesConRazaImagenYKiDeApi() throws Exception {
        String json = "{\"items\":["
                + "{\"id\":1,\"name\":\"Goku\",\"race\":\"Saiyan\",\"image\":\"https://cdn.test/goku.webp\",\"ki\":\"60 Septillion\"},"
                + "{\"id\":2,\"name\":\"Vegeta\",\"race\":\"Saiyan\",\"image\":\"https://cdn.test/vegeta.webp\",\"ki\":\"54 Septillion\"},"
                + "{\"id\":3,\"name\":\"Piccolo\",\"race\":\"Namekian\",\"image\":\"https://cdn.test/piccolo.webp\",\"ki\":\"2 Billion\"},"
                + "{\"id\":4,\"name\":\"Frieza\",\"race\":\"Frieza Race\",\"image\":\"https://cdn.test/frieza.webp\",\"ki\":\"530 Billion\"},"
                + "{\"id\":5,\"name\":\"Krillin\",\"race\":\"Human\",\"image\":\"https://cdn.test/krillin.webp\",\"ki\":\"100 Million\"}]}";
        List<Carta> cartas = DragonBallApiService.parsearRespuesta(json);
        assertEquals(5, cartas.size());
        assertEquals("Saiyan", cartas.get(0).getRaza());
        assertEquals("https://cdn.test/goku.webp", cartas.get(0).getImagenUrl());
        assertEquals("60 Septillion", cartas.get(0).getPoderApi());
        assertTrue(cartas.get(0).getEscalaPoder().compareTo(cartas.get(1).getEscalaPoder()) > 0);
        assertTrue(cartas.get(0).getEscalaPoder().compareTo(cartas.get(2).getEscalaPoder()) > 0);
    }

    @Test
    void introspectorAsignaPropiedadesDeCarta() {
        Carta carta = InspectorReglasUtil.crearCartaDesdeApi(
                "Gohan", "Saiyan", "https://cdn.test/gohan.webp", "2 Trillion", new BigDecimal("2000000000000"));
        assertEquals("Gohan", carta.getNombre());
        assertEquals("Saiyan", carta.getRaza());
        assertEquals("2 Trillion", carta.getPoderApi());
        assertFalse(InspectorReglasUtil.inyectarPropiedadDinamica(carta, "propiedadInexistente", "x"));
    }

    @Test
    void entidadesCumplenLaFormaJavaBeanYSerializable() throws Exception {
        validarBean(Carta.class);
        validarBean(Jugador.class);
        validarBean(EstadoJuego.class);
        validarBean(modelo.JavaBeans.class);
        validarBean(HistorialPartida.class);
        assertTrue(Serializable.class.isAssignableFrom(Carta.class));
        assertTrue(Serializable.class.isAssignableFrom(Jugador.class));
        assertTrue(Serializable.class.isAssignableFrom(EstadoJuego.class));
        assertTrue(Serializable.class.isAssignableFrom(HistorialPartida.class));
    }

    private void validarBean(Class<?> tipo) throws Exception {
        tipo.getDeclaredConstructor();
        PropertyDescriptor[] propiedades = Introspector.getBeanInfo(tipo, Object.class).getPropertyDescriptors();
        assertTrue(propiedades.length > 0, tipo.getSimpleName() + " no expone propiedades JavaBean");
        for (PropertyDescriptor p : propiedades) {
            assertNotNull(p.getReadMethod(), "Falta getter de " + p.getName());
            assertNotNull(p.getWriteMethod(), "Falta setter de " + p.getName());
        }
    }
}
