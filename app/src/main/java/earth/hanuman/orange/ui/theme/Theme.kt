package earth.hanuman.orange.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OrangePrimary = Color(0xFFFF7A1A)
private val OrangeDeep = Color(0xFFCC5A00)

private val LightColors = lightColorScheme(
    primary = OrangePrimary,
    secondary = OrangeDeep,
)

private val DarkColors = darkColorScheme(
    primary = OrangePrimary,
    secondary = OrangeDeep,
)

@Composable
fun OrangeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
