# Matriz de cumplimiento de la rúbrica

La especificación recibida se titula **Pokémon Card-Jitsu Multijugador**. Este repositorio conserva la adaptación solicitada por el equipo: **Dragon Ball Card-Jitsu**, personajes obtenidos de Dragon Ball API, raza visible y comparación del poder `ki`. Por ello, los apartados técnicos pueden completarse, pero la mecánica temática no es idéntica a la redacción del PDF.

| Criterio del PDF | Situación en esta versión | Evidencia dentro del proyecto |
|---|---|---|
| Interfaz JavaFX/FXML (20) | Implementada | Vistas FXML para acceso, lobby, espera, partida, ranking e historial; CSS separado; tabla de ranking e historial; cartas formadas por retrato API más marco PNG transparente. |
| Persistencia DB4O (20) | Implementada para perfil, estadísticas e historial | `RepositorioJugadores`, `ManejadorDB4O`, `HistorialPartida`; actualización de estadísticas y registro histórico en una misma operación de persistencia; consulta de ranking e historial. |
| Concurrencia y sockets (15) | Implementada en la arquitectura; requiere prueba en red real | `ServidorSocket`, `HiloCliente`, `SalaJuego`, `ClienteSocket`; un hilo por cliente, locks compartidos para sala/cola y DB4O; descarga de catálogo fuera del monitor global de matchmaking. |
| JavaBeans e introspección (15) | Integrada | `Carta`, `Jugador`, `EstadoJuego`, `JavaBeans` e `HistorialPartida` tienen constructor sin argumentos, propiedades encapsuladas, getters/setters y Serializable. `InspectorReglasUtil` usa `Introspector` y `PropertyDescriptor` en el mapeo real de API y el estado de partida. |
| Mecánicas del PDF (15) | **Adaptación, no equivalencia literal** | El proyecto compara el campo `ki` de Dragon Ball API y conserva las razas. No usa el triángulo Fuego/Agua/Planta, colores ni condición de victoria por elementos que exige el PDF de Pokémon. Se mantiene así para respetar la decisión del equipo. |
| Reporte técnico y demo (15) | Documentación preparada; demo pendiente de ejecutarse en dos equipos/clientes | `README.md`, `REPORTE_TECNICO.md`, esta matriz y pruebas automatizadas de modelo/DB4O. Las capturas y la demostración en vivo deben generarse al ejecutar la aplicación. |

## Pendiente externo al código

Si el docente exige literalmente Pokémon y las reglas descritas en el PDF, el criterio de mecánicas no puede marcarse como cumplido mientras el juego use Dragon Ball, razas y KI. Para cambiar eso se necesitaría autorización para modificar la temática o una versión paralela con las reglas de Pokémon.

La verificación final de JavaFX, descarga real de imágenes, sockets entre dos clientes y prueba visual de los FXML debe hacerse en el equipo de entrega con JDK 21 y JavaFX SDK 21, porque requiere el runtime gráfico y acceso a Internet.
