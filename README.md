# MoovAR 🚆

Aplicación Android nativa y moderna para consultar el estado del servicio, horarios en vivo y seguimiento GPS de la red ferroviaria metropolitana (Trenes Argentinos / SOFSE).

Diseñada bajo los lineamientos de **Material 3 Expressive**, con soporte completo de tema dinámico (Material You) y optimizada para máxima velocidad y eficiencia.

---

## ✨ Características

- 🟢 **Estado de Líneas en Vivo:** Estado operativo de las líneas metropolitanas (Roca, Mitre, Sarmiento, San Martín, Belgrano Sur, Tren de la Costa).
- ⏱️ **Próximas Salidas en Tiempo Real:** Estimaciones minuto a minuto por estación origen y destino consultando la API oficial de SOFSE.
- 🗺️ **Seguimiento GPS en Mapa:** Ubicación satelital en vivo de las formaciones en movimiento sobre OpenStreetMap (osmdroid), con un pin temático estilo Google Maps.
- ⭐ **Estaciones y Recorridos Favoritos:** Guardado rápido de estaciones individuales o trayectos frecuentes para acceso inmediato.
- 🎨 **Material 3 Expressive & Monet:** Soporte de tema claro/oscuro dinámico y **Themed Icon** adaptable en Android 13+.
- ⚡ **Ultra Liviana:** Compilación de producción con R8 y optimización de recursos (~4.3 MB).

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Arquitectura:** Clean Architecture + MVI / MVVM modularizada
- **Inyección de Dependencias:** Dagger Hilt
- **Persistencia Local:** Room Database
- **Networking:** Retrofit + OkHttp + Kotlinx Serialization
- **Mapas:** OpenStreetMap (`osmdroid`)

---

## 📦 Instalación

Podés descargar la última APK lista para instalar desde la sección de **[Releases](https://github.com/nicodiaz101/MoovAR/releases)**.

---

## 📄 Licencia

Este proyecto está bajo la Licencia **[MIT](LICENSE)**.
Los íconos y símbolos vectoriales se basan en **Google Material Symbols** bajo licencia **Apache 2.0**.
