package app.quranichorizons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quranichorizons.data.SurahAvailability

@Composable
fun StatusChip(a: SurahAvailability) {
    val (label, bg) = when (a) {
        SurahAvailability.READY -> "جاهزة" to Color(0xFF2E7D57)
        SurahAvailability.PREVIEW -> "معاينة — قيد المراجعة" to Color(0xFF8A6A20)
        SurahAvailability.IN_PREPARATION -> "قيد الإعداد" to Color(0xFF33407A)
    }
    Box(Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
