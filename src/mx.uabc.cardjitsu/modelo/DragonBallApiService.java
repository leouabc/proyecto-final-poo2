package modelo;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente único de Dragon Ball API compartido por el servidor y el cliente.
 * El ki se conserva tal como lo entrega la API y se normaliza solo para comparar
 * correctamente los valores durante el combate.
 */
public final class DragonBallApiService {
    private static final String ENDPOINT = "https://dragonball-api.com/api/characters?page=1&limit=100";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static volatile List<Carta> cachePersonajes;

    private DragonBallApiService() { }

    /** Descarga una sola vez la colección completa y reutiliza la caché. */
    public static List<Carta> obtenerPersonajes() throws IOException {
        List<Carta> disponibles = cachePersonajes;
        if (disponibles != null && !disponibles.isEmpty()) return copiarLista(disponibles);

        synchronized (DragonBallApiService.class) {
            disponibles = cachePersonajes;
            if (disponibles != null && !disponibles.isEmpty()) return copiarLista(disponibles);

            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET().build();
            final HttpResponse<String> response;
            try {
                response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("La consulta a Dragon Ball API fue interrumpida.", ex);
            }
            if (response.statusCode() != 200) {
                throw new IOException("Dragon Ball API respondió HTTP " + response.statusCode() + ".");
            }

            List<Carta> personajes = parsearRespuesta(response.body());
            if (personajes.size() < 5) {
                throw new IOException("Dragon Ball API no devolvió al menos cinco personajes con nombre, raza, imagen y ki.");
            }
            cachePersonajes = Collections.unmodifiableList(personajes);
            return copiarLista(cachePersonajes);
        }
    }

    /** Consulta un personaje individual por su identificador oficial de la API. */
    public static Carta obtenerPersonajePorId(int id) throws IOException {
        if (id <= 0) throw new IllegalArgumentException("El ID debe ser positivo.");
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://dragonball-api.com/api/characters/" + id))
                .timeout(Duration.ofSeconds(15)).header("Accept", "application/json").GET().build();
        final HttpResponse<String> response;
        try {
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("La consulta a Dragon Ball API fue interrumpida.", ex);
        }
        if (response.statusCode() != 200) throw new IOException("Dragon Ball API respondió HTTP " + response.statusCode());
        Carta carta = parsearPersonajeIndividual(response.body());
        if (carta == null) throw new IOException("La API no devolvió todos los datos necesarios del personaje.");
        return carta;
    }

    public static Carta parsearPersonajeIndividual(String json) {
        return json == null ? null : parsearPersonaje(json);
    }

    /** Método público para probar el mapeo JSON sin depender de Internet. */
    public static List<Carta> parsearRespuesta(String json) throws IOException {
        if (json == null || json.isBlank()) throw new IOException("La respuesta de Dragon Ball API está vacía.");
        int itemsKey = json.indexOf("\"items\"");
        int inicio = itemsKey < 0 ? -1 : json.indexOf('[', itemsKey);
        if (inicio < 0) throw new IOException("La respuesta de Dragon Ball API no contiene la lista 'items'.");

        int fin = encontrarFinArray(json, inicio);
        if (fin < 0) throw new IOException("La lista de personajes recibida está incompleta.");

        String items = json.substring(inicio + 1, fin);
        List<Carta> personajes = new ArrayList<>();
        int profundidad = 0;
        int inicioObjeto = -1;
        boolean dentroCadena = false;
        boolean escape = false;

        for (int i = 0; i < items.length(); i++) {
            char c = items.charAt(i);
            if (dentroCadena) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') dentroCadena = false;
                continue;
            }
            if (c == '"') {
                dentroCadena = true;
            } else if (c == '{') {
                if (profundidad++ == 0) inicioObjeto = i;
            } else if (c == '}') {
                profundidad--;
                if (profundidad == 0 && inicioObjeto >= 0) {
                    Carta carta = parsearPersonaje(items.substring(inicioObjeto, i + 1));
                    if (carta != null) personajes.add(carta);
                    inicioObjeto = -1;
                }
            }
        }
        return personajes;
    }

    private static int encontrarFinArray(String json, int inicio) {
        int profundidad = 0;
        boolean dentroCadena = false;
        boolean escape = false;
        for (int i = inicio; i < json.length(); i++) {
            char c = json.charAt(i);
            if (dentroCadena) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') dentroCadena = false;
                continue;
            }
            if (c == '"') dentroCadena = true;
            else if (c == '[') profundidad++;
            else if (c == ']' && --profundidad == 0) return i;
        }
        return -1;
    }

    private static Carta parsearPersonaje(String objeto) {
        String nombre = campo(objeto, "name");
        String raza = campo(objeto, "race");
        String imagen = campo(objeto, "image");
        // El poder de la carta procede únicamente del campo ki de Dragon Ball API.
        // No se sustituye por maxKi ni por valores predeterminados locales.
        String ki = campo(objeto, "ki");
        if (nombre == null || esDesconocido(raza) || imagen == null || esDesconocido(ki)) return null;
        if (!(imagen.startsWith("https://") || imagen.startsWith("http://"))) return null;
        BigDecimal valor = convertirEscala(ki);
        if (valor == null) return null;
        return InspectorReglasUtil.crearCartaDesdeApi(
                nombre.trim(), raza.trim(), imagen.trim(), ki.trim(), valor);
    }

    private static String campo(String json, String clave) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(clave)
                + "\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"|(-?\\d+(?:\\.\\d+)?))");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return null;
        String valor = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        return desescapar(valor);
    }

    private static String desescapar(String valor) {
        if (valor == null) return null;
        StringBuilder salida = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c != '\\' || i + 1 >= valor.length()) {
                salida.append(c);
                continue;
            }
            char siguiente = valor.charAt(++i);
            switch (siguiente) {
                case '"': salida.append('"'); break;
                case '\\': salida.append('\\'); break;
                case '/': salida.append('/'); break;
                case 'b': salida.append('\b'); break;
                case 'f': salida.append('\f'); break;
                case 'n': salida.append('\n'); break;
                case 'r': salida.append('\r'); break;
                case 't': salida.append('\t'); break;
                case 'u':
                    if (i + 4 < valor.length()) {
                        try {
                            salida.append((char) Integer.parseInt(valor.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException ex) { salida.append('u'); }
                    } else salida.append('u');
                    break;
                default: salida.append(siguiente);
            }
        }
        return salida.toString();
    }

    private static boolean esDesconocido(String valor) {
        return valor == null || valor.isBlank() || "unknown".equalsIgnoreCase(valor.trim())
                || "n/a".equalsIgnoreCase(valor.trim());
    }

    /** Convierte valores como "60 Septillion" a un número comparable sin alterar el texto mostrado. */
    private static BigDecimal convertirEscala(String texto) {
        if (esDesconocido(texto)) return null;
        String limpio = texto.trim().toLowerCase(Locale.ROOT).replace(",", "");
        Matcher matcher = Pattern.compile("^([+-]?\\d+(?:\\.\\d+)?)\\s*([a-z]+)?$").matcher(limpio);
        if (!matcher.matches()) return null;
        BigDecimal base;
        try {
            base = new BigDecimal(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
        String unidad = matcher.group(2) == null ? "" : matcher.group(2);
        int exponente;
        switch (unidad) {
            case "k": case "thousand": case "thousands": exponente = 3; break;
            case "m": case "million": case "millions": exponente = 6; break;
            case "b": case "billion": case "billions": exponente = 9; break;
            case "t": case "trillion": case "trillions": exponente = 12; break;
            case "quadrillion": case "quadrillions": exponente = 15; break;
            case "quintillion": case "quintillions": exponente = 18; break;
            case "sextillion": case "sextillions": exponente = 21; break;
            case "septillion": case "septillions": exponente = 24; break;
            case "octillion": case "octillions": exponente = 27; break;
            case "nonillion": case "nonillions": exponente = 30; break;
            case "decillion": case "decillions": exponente = 33; break;
            case "": exponente = 0; break;
            default: return null;
        }
        return base.scaleByPowerOfTen(exponente);
    }

    private static List<Carta> copiarLista(List<Carta> originales) {
        List<Carta> copia = new ArrayList<>(originales.size());
        for (Carta carta : originales) copia.add(carta.copia());
        return copia;
    }
}
