# Reporte técnico — Dragon Ball Card-Jitsu Multijugador

## 1. Arquitectura

La aplicación conserva una arquitectura cliente-servidor. El servidor escucha conexiones TCP en el puerto `35557`; cada cliente conectado es atendido por un `HiloCliente` independiente. Los mensajes se serializan mediante `ObjectInputStream` y `ObjectOutputStream` usando `MensajeProtocolo` y `TipoAccion`.

El cliente está construido con JavaFX y FXML. Los controladores se separan por pantalla: acceso, lobby, espera, combate, ranking e historial. `ManejadorEventosCliente` recibe las respuestas del socket en un hilo lector y entrega las actualizaciones a los controladores activos mediante `Platform.runLater`.

## 2. API y cartas

`DragonBallApiService` consulta `https://dragonball-api.com/api/characters?page=1&limit=100`. Cada carta se crea desde los campos `name`, `race`, `image` y `ki` entregados por la API. No se sustituyen los poderes por `maxKi` ni por valores locales. El texto original de `ki` se conserva para mostrarlo y se normaliza a `BigDecimal` únicamente para comparar escalas como millones, billones y septillones.

En la interfaz, el retrato individual de `image` se coloca debajo del PNG transparente elegido por raza. Los PNG locales son marcos decorativos; no se usan como retrato del personaje. Los retratos se representan en `WebView` para admitir recursos WebP. El módulo `javafx.web` debe estar disponible en ejecución.

## 3. Mecánica implementada

Cada jugador recibe cinco personajes. En cada ronda, el jugador selecciona una carta y confirma la jugada. El servidor espera las dos selecciones, compara el `ki` normalizado, asigna el punto al mayor poder y repone las cartas utilizadas. El primer jugador que gana tres rondas gana la partida. Un empate de poder no entrega punto.

La raza se muestra como atributo descriptivo de la carta. No modifica el cálculo de ganador. No se agregan elementos de fuego, agua o planta.

## 4. Persistencia DB4O

`ManejadorDB4O` administra el contenedor persistente. `RepositorioJugadores` protege las operaciones con un lock compartido porque el contenedor DB4O se comparte entre varios hilos de cliente.

Se persisten:
- perfiles y contraseñas protegidas mediante PBKDF2;
- victorias, derrotas, puntos y rango;
- historial de cada encuentro, con fecha, participantes, ganador, perdedor, marcador de rondas, motivo de finalización y los últimos personajes/poderes jugados.

La actualización de ambos perfiles y el registro histórico se realizan antes de un único `commit`. El ranking y el historial se consultan desde DB4O.

## 5. JavaBeans e introspección

`Carta`, `Jugador`, `EstadoJuego`, `JavaBeans` e `HistorialPartida` son serializables y cuentan con constructor vacío y propiedades con getters/setters. `InspectorReglasUtil` usa `Introspector.getBeanInfo`, `PropertyDescriptor` y reflexión para asignar dinámicamente los campos de API a `Carta` y para sincronizar las propiedades del `EstadoJuego` durante una partida.

## 6. Concurrencia y sockets

Cada conexión dispone de su propio `HiloCliente`. Los cambios en la cola de matchmaking y el mapa de salas se protegen mediante sincronización de clase; la sala sincroniza la aceptación de jugadas. La descarga HTTP del catálogo se realiza fuera del monitor global, evitando mantener bloqueadas otras búsquedas mientras se consulta el servicio remoto. El acceso a DB4O usa un bloqueo global compartido.

## 7. Pruebas incluidas

`src/test/java/PruebasModeloTest.java` comprueba el mapeo de JSON de ejemplo, lectura de `ki`, comparación de escalas e introspección JavaBeans. `src/test/java/RepositorioJugadoresTest.java` prueba registro, inicio de sesión, actualización de estadísticas y persistencia/consulta del historial usando una base DB4O temporal.

Ejecutar con:

```bash
mvn test
```

## 8. Límites de la verificación

Las pruebas automatizadas de modelo y DB4O no sustituyen una demostración visual. Antes de entregar, ejecutar servidor y dos clientes con JavaFX SDK 21, acceso a Internet, validar carga real de retratos y probar matchmaking, chat, ranking, revancha por abandono e historial desde ambas cuentas.

La rúbrica PDF recibida describe Pokémon Card-Jitsu y reglas por elementos/colores. Esta versión es la adaptación Dragon Ball solicitada por el equipo; esa diferencia debe ser autorizada por el docente si se evaluará la mecánica contra el PDF literal.
