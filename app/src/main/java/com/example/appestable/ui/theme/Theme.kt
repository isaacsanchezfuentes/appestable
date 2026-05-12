package com.example.appestable.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 🎨 Esquema de colores para modo oscuro (negro elegante con dorado)
private val DarkColorScheme = darkColorScheme(
    primary       = Color(0xFFCDA15E), // Dorado elegante (acento principal)
    onPrimary     = Color.Black,
    secondary     = Color(0xFFA3833F), // Dorado más oscuro (hover/activo)
    onSecondary   = Color.Black,
    background    = Color.Black,       // Negro profundo para fondo principal
    onBackground  = Color(0xFFF2F2F2), // Blanco suave para texto
    surface       = Color(0xFF1F1F1F), // Gris oscuro para superficies (text fields, etc.)
    onSurface     = Color(0xFFF2F2F2), // Blanco suave para texto en superficies
    error         = Color(0xFFCF6679), // Rojo suave para errores
    onError       = Color.Black
)

// 💡 Esquema de colores para modo claro (opcional, ajustado para consistencia)
private val LightColorScheme = lightColorScheme(
    primary       = Color(0xFF6200EE),
    onPrimary     = Color.White,
    secondary     = Color(0xFF03DAC5),
    onSecondary   = Color.Black,
    background    = Color(0xFFFFFBFE),
    onBackground  = Color(0xFF1C1B1F),
    surface       = Color(0xFFFFFBFE),
    onSurface     = Color(0xFF1C1B1F),
    error         = Color(0xFFB00020),
    onError       = Color.White
)

@Composable
fun AppestableTheme(
    darkTheme: Boolean = true, // Fuerza modo oscuro por defecto
    dynamicColor: Boolean = true, // Android 12+ soporte dinámico
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}