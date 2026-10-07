# Interfaz española y diseño Genshin · v13

La v13 parte de la v12 completa, que el usuario confirmó jugando todo el Teatro
Imaginario de Genshin. Añade la traducción de Axixi a los recursos españoles de
Moonlight y un diseño opcional con los 11 controles del archivo suministrado.

## Traducción

- Los textos de menús, preferencias, editor y layouts se resuelven mediante recursos
  Android. Los valores internos, códigos de teclas y claves de preferencias siguen
  siendo los existentes.
- La selección de idioma usa el idioma del sistema por defecto y admite Español
  manualmente. Las tarjetas permiten varias líneas para las etiquetas españolas.
- Se traducen las funciones de v9–v12: Tecla + movimiento, Cámara + toque,
  diámetro circular, ancho/alto rectangular, niveles y tamaños ampliados.
- Los nombres personalizados, PC/juegos y colaboradores se conservan. Los registros
  técnicos y las notas remotas de versiones mantienen su contenido original.

## Diseño opcional

El botón Genshin aparece en el editor de teclado/ratón. Su diálogo permite cargar
el archivo incluido en memoria; el archivo guardado solo cambia al pulsar Guardar
diseño. Cancelar conserva los controles editados. No aparece en el editor de mandos.

El archivo original se conserva byte por byte en `presets/genshin`. La copia española
y el asset de la aplicación cambian únicamente `name` y `desc`; se mantienen códigos,
coordenadas, tamaños, opacidad, niveles, modos y formas. No se supone una resolución
de pantalla ni se reescala automáticamente el diseño.

## Verificación reproducible

`dev/check-spanish-interface.py` revisa XML, duplicados, cobertura de recursos,
argumentos de formato y la igualdad de todos los datos del preset salvo sus etiquetas.
`dev/check-virtual-input.sh` ejecuta 18 comprobaciones de entrada, 14 de Ataque + cámara
y 9 de Cámara + toque. La compilación ejecuta además la suite JVM/Robolectric completa.

Las pruebas nuevas se ejecutan en Android 9 y Android 14 simulados. Cubren los 11
controles y sus vistas reales, geometría, teclas, opacidad, niveles, carga sin guardar,
confirmación/cancelación, etiquetas españolas y ausencia del preset en mandos.

El workflow `axixi-spanish.yml` compila el APK, ejecuta las comprobaciones, verifica
la firma y publica APK, configuración y SHA-256 únicamente si todo pasa.

## Comprobación en el teléfono

1. Instala Axixi Moonlight ES, empareja Sunshine y selecciona Español en Ajustes →
   Interfaz y general → Interfaz → Idioma si el sistema no lo activa.
2. Revisa Ajustes y el menú del juego en horizontal/vertical, comprobando que las
   opciones españolas se leen y que los nombres personalizados se conservan.
3. En el editor, carga Genshin, comprueba las posiciones y guarda cuando estén bien.
   También puedes importar un diseño anterior directamente.
4. Comprueba opacidad, forma circular/rectangular, tamaño, nivel, bloqueo y códigos;
   exporta, reimporta y verifica que se conservan.
5. Juega con joystick + Ctrl, ataque cargado, E apuntada, cámara y toques breves
   simultáneos. La entrada conserva la implementación de v12.

La APK usa `com.limelight.debug.spanish13`, independiente de las anteriores. La v12
está probada en el teléfono; la interfaz española y la carga integrada se comprueban
físicamente al instalar esta v13.
