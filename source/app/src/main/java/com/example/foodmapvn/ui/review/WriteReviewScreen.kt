package com.example.foodmapvn.ui.review

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.Sand

@Composable
fun WriteReviewRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WriteReviewViewModel = viewModel(factory = WriteReviewViewModel.Factory),
) {
    val place by viewModel.placeDetail.collectAsStateWithLifecycle()
    val rating by viewModel.rating.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val priceText by viewModel.priceText.collectAsStateWithLifecycle()
    val imageUris by viewModel.selectedImageUris.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addImageUri(uri)
        }
    }

    WriteReviewScreen(
        place = place,
        rating = rating,
        content = content,
        priceText = priceText,
        imageUris = imageUris,
        uiState = uiState,
        onRatingChange = viewModel::setRating,
        onContentChange = { viewModel.content.value = it },
        onPriceTextChange = { viewModel.priceText.value = it },
        onAddPhoto = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemovePhoto = viewModel::removeImageUri,
        onSubmit = { viewModel.submitReview(onSuccess = onDone) },
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewScreen(
    place: PlaceDetail?,
    rating: Int,
    content: String,
    priceText: String,
    imageUris: List<Uri>,
    uiState: ReviewUiState,
    onRatingChange: (Int) -> Unit,
    onContentChange: (String) -> Unit,
    onPriceTextChange: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viết đánh giá") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (place == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Đang tải thông tin quán...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                place.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                place.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            // Rating Stars
            Text("Đánh giá của bạn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(5) { index ->
                    val star = index + 1
                    Icon(
                        painter = painterResource(if (star <= rating) R.drawable.ic_star else R.drawable.ic_star_border),
                        contentDescription = "Sao $star",
                        tint = Amber,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onRatingChange(star) }
                            .padding(4.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // Content input
            Text("Nội dung nhận xét", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                placeholder = { Text("Món ăn có chuẩn vị không? Không gian quán thế nào?...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(20.dp))

            // Price input
            Text("Giá tham khảo bạn đã trả (đồng)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = onPriceTextChange,
                placeholder = { Text("Ví dụ: 35000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(20.dp))

            // Photo Uploader
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hình ảnh đính kèm (${imageUris.size}/5)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                if (imageUris.size < 5) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Sand)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .clickable(onClick = onAddPhoto),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painterResource(R.drawable.ic_add),
                                    contentDescription = "Thêm ảnh",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("Thêm ảnh", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                items(imageUris) { uri ->
                    Box(modifier = Modifier.size(80.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onRemovePhoto(uri) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_remove),
                                contentDescription = "Xóa ảnh",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            if (uiState is ReviewUiState.Error) {
                Text(
                    uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onSubmit,
                enabled = uiState !is ReviewUiState.Submitting,
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState is ReviewUiState.Submitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.progressText)
                } else {
                    Text("Gửi đánh giá", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
