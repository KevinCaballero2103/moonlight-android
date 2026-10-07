# Axixi Moonlight ES · Genshin Touch

Versión en español de Axixi Moonlight con los controles desarrollados y probados
para Genshin Impact. Integra todas las mejoras hasta la v12 y añade la interfaz
española y un diseño de Genshin reutilizable en la v13.

## Descargar e instalar

El APK y el diseño exportable están en [Releases](https://github.com/KevinCaballero2103/moonlight-android/releases).
El nombre de la aplicación es **Axixi Moonlight ES**. Esta APK tiene un identificador
independiente (`com.limelight.debug.spanish13`) y puede convivir con las versiones
anteriores. Exporta tu diseño, vuelve a emparejar Sunshine e impórtalo en la nueva app.

Si no aparece en español: **Ajustes → Interfaz y general → Interfaz → Idioma → Español**.
Android 13 o posterior puede abrir el selector de idioma de aplicaciones del sistema.

## Controles incluidos

- Opacidad individual, con herencia de la opacidad global. El 0 % mantiene la entrada.
- Combinaciones con modificadores primero y liberación segura; Ctrl de una sola tecla
  se libera inmediatamente.
- Entrada simultánea con captura de cada dedo y liberación al cancelar, ocultar o salir.
- Ataque + cámara: mantiene clic izquierdo mientras el dedo mueve la mira.
- Tecla + movimiento: mantiene E u otra tecla mientras el dedo apunta.
- Cámara + toque: un toque breve hace clic donde tocaste; arrastrar mueve la cámara.
- Zonas de toque directo para interactuar con el video debajo de los controles.
- Forma rectangular con ancho/alto, o círculo con un único diámetro.
- Niveles 1–99 para superponer controles; un número mayor queda encima.
- Tamaño máximo de al menos 1000 %, ampliado según la diagonal de la pantalla.

## Diseño de Genshin

En el editor de controles de teclado/ratón, pulsa **Genshin**, confirma la carga,
revisa las posiciones y pulsa **Guardar diseño**. El diseño guardado se conserva
hasta que decidas guardar. El botón no aparece en el editor de mandos.

El diseño contiene los 11 controles probados por el usuario: escritorio, clic
izquierdo, clic derecho bloqueado, movimiento WASD, Q, espacio, Z, Ctrl,
Ataque + cámara, Cámara + toque invisible en nivel 1 y E + movimiento.

Los códigos, coordenadas, dimensiones, formas, opacidades y niveles son los del
archivo original. La copia española cambia únicamente los nombres y descripciones.
Son coordenadas en píxeles del dispositivo original; ajusta posiciones y tamaños
si utilizas otra pantalla. No se aplica una transformación automática.

- [Configuración española](presets/genshin/axi_OSC_Keyboard_es.txt)
- [Configuración original](presets/genshin/axi_OSC_Keyboard_original.txt)
- [Guía del diseño](presets/genshin/README.md)

## Compilar y verificar

Java 17, Android SDK 34, Build Tools 34.0.0 y NDK 27.0.12077973.
Clona este fork con sus submódulos e instala esos componentes. Después ejecuta:

```sh
python3 dev/check-spanish-interface.py
sh dev/check-virtual-input.sh
./gradlew --no-daemon -PincludeKishiHaptics=false -PincludeStereo3dAi=false \
  :app:testNonRootDebugUnitTest :app:assembleNonRootDebug
```

El APK se genera en `app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk`.
La variante debug mantiene el empaquetado táctil usado en las v8–v12.
Las extensiones privadas opcionales no son necesarias para estos controles.

La comprobación española revisa recursos, formatos, textos visibles y los datos
del preset. Las pruebas Android cubren el enrutamiento real de los controles con
una conexión simulada. El usuario confirmó la v12 jugando todo el Teatro Imaginario;
la comprobación de la interfaz española en su teléfono se realiza sobre esta v13.

## Origen

Basado en [Axixi2233/moonlight-android](https://github.com/Axixi2233/moonlight-android)
y Moonlight Android. Se mantienen el código, los créditos y la licencia del proyecto.
[README original](README-upstream.md) · [Licencia](LICENSE.txt).
