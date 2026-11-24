package com.vidrierapp.presentation.visit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vidrierapp.R
import java.io.File

const val PHOTO_THUMBNAILS_TAG = "photo_thumbnails"

@Composable
fun PhotoThumbnailsRow(
    photoPaths: List<String>,
    modifier: Modifier = Modifier,
    thumbnailSize: Dp = 96.dp,
    onRemovePhoto: ((String) -> Unit)? = null
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(photoPaths, key = { _, path -> path }) { index, path ->
            Box(modifier = Modifier.size(thumbnailSize)) {
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(R.string.visit_photo_thumbnail_desc, index + 1),
                    modifier = Modifier
                        .size(thumbnailSize)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                if (onRemovePhoto != null) {
                    IconButton(
                        onClick = { onRemovePhoto(path) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.visit_photo_delete_desc, index + 1)
                        )
                    }
                }
            }
        }
    }
}
