package com.example.foodmapvn.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.R
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.ui.navigation.Routes
import com.example.foodmapvn.ui.theme.FoodmapvnTheme
import com.example.foodmapvn.ui.theme.SplashBg
import com.example.foodmapvn.ui.theme.SplashCircle
import com.example.foodmapvn.ui.theme.SplashWave
import kotlinx.coroutines.delay

class SplashViewModel(private val areaRepository: AreaRepository) : ViewModel() {
    /** Lần đầu (chưa chọn khu vực) -> màn Giới thiệu -> Chọn vùng; đã có khu vực -> vào thẳng app. */
    fun nextRoute(): String =
        if (areaRepository.selectedArea.value == null) Routes.ONBOARDING else Routes.MAIN

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SplashViewModel(appContainer().areaRepository) }
        }
    }
}

@Composable
fun SplashRoute(
    onFinished: (nextRoute: String) -> Unit,
    viewModel: SplashViewModel = viewModel(factory = SplashViewModel.Factory),
) {
    LaunchedEffect(Unit) {
        delay(SPLASH_MILLIS)
        onFinished(viewModel.nextRoute())
    }
    SplashScreen()
}

private const val SPLASH_MILLIS = 900L

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    // Figma "Splash": nền kem, logo FoodMap trong vòng tròn kem nhạt, sóng màu đào ở đáy
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBg),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val wave = Path().apply {
                moveTo(0f, h * 0.905f)
                cubicTo(w * 0.25f, h * 0.85f, w * 0.55f, h * 0.90f, w, h * 0.82f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wave, SplashWave)
        }
        Box(
            Modifier
                .size(270.dp)
                .clip(CircleShape)
                .background(SplashCircle),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_foodmap),
                contentDescription = "FoodMap",
                modifier = Modifier.width(210.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SplashPreview() {
    FoodmapvnTheme { SplashScreen() }
}
