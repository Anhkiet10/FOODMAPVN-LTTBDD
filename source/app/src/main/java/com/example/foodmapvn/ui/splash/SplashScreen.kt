package com.example.foodmapvn.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay

/** Quyết định màn tiếp theo: chưa có khu vực -> Chọn vùng (xin quyền vị trí); đã có -> vào thẳng app. */
class SplashViewModel(private val areaRepository: AreaRepository) : ViewModel() {
    fun nextRoute(): String =
        if (areaRepository.selectedArea.value == null) Routes.REGION_SELECT else Routes.MAIN

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_place),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Food Map VN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Đặc sản đúng vị · Giá minh bạch",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        )
    }
}

@Preview
@Composable
private fun SplashPreview() {
    FoodmapvnTheme { SplashScreen() }
}
