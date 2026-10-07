# Diseño de Genshin probado

La configuración original contiene 11 controles. La copia española conserva todos
los valores salvo el texto de los nombres y descripciones. La misma copia se incluye
en el APK, en `assets/config/genshin_touch_es.json`.

| Control | Entrada | Función |
| --- | --- | --- |
| Escritorio | Win + D | Mostrar el escritorio |
| Clic izquierdo | Ratón izquierdo | Clic normal |
| Clic derecho | Ratón derecho, bloqueo | Mantener el clic derecho |
| Movimiento | W, A, S, D | Joystick invisible |
| Q | Q | Habilidad definitiva |
| Saltar | Espacio | Salto |
| Z | Z | Acceso rápido del juego |
| Ctrl | Ctrl izquierdo | Acción asignada a Ctrl |
| Ataque + cámara | Clic izquierdo y movimiento | Ataque con mira |
| Cámara + toque | Movimiento o clic en el punto tocado | Touchpad invisible, nivel 1 |
| E | E y movimiento | Habilidad con mira |

## Uso

Pulsa **Genshin** en el editor de controles, confirma la carga, revisa las posiciones
y guarda. También puedes importar `axi_OSC_Keyboard_es.txt` con el importador habitual.
El original se conserva en `axi_OSC_Keyboard_original.txt`.

Los demás botones usan el nivel 2 por defecto. La zona de Cámara + toque utiliza
nivel 1 para quedar debajo. La opacidad individual de esa zona y del joystick es 0 %.
La opacidad del resto hereda el valor global.

Las coordenadas se conservan tal como se exportaron. La última zona visible llega
al menos a x=2096 y la zona de cámara tiene altura 746 px; esto no define la resolución
exacta de la pantalla original. En otro dispositivo, revisa tamaño y posición.
