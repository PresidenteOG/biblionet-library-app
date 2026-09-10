// Archivo: cat/copernic/biblionet/ui/view/book/CategoriaListScreen.kt
package cat.copernic.biblionet.ui.view.book

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.book.Categoria
import cat.copernic.biblionet.ui.components.ImagePicker
import cat.copernic.biblionet.ui.view.admin.AddActionButton
import cat.copernic.biblionet.ui.view.admin.ManagementListContainer
import cat.copernic.biblionet.ui.view.admin.ModernStatCard
import coil.compose.AsyncImage

@Composable
fun GestionCategoriasScreen(
    categorias: List<Categoria>,
    isUploading: Boolean = false,
    onBack: () -> Unit,
    onSaveCategoria: (Categoria) -> Unit,
    onDeleteCategoria: (String) -> Unit,
    onViewAll: () -> Unit,
    onUploadImage: (android.content.Context, Uri, (String) -> Unit) -> Unit = { _, _, _ -> }
) {
    var showDialog by remember { mutableStateOf(false) }
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E3E8))
    ) {
        Column {
            CategoriasHeader(
                onBack = onBack,
                totalCategorias = categorias.size
            )

            // CONTENEDOR DE LISTA ACTUALIZADO CON PARÁMETROS FANTASMA
            ManagementListContainer(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                title = stringResource(R.string.gestion_categorias),
                isExpanded = false,
                searchQuery = "", // No se usa aquí, lo dejamos vacío
                onSearchQueryChange = {}, // Acción vacía
                searchPlaceholder = "", // Vacío
                onViewAllClick = onViewAll
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    items(categorias.take(5)) { cat ->
                        CategoriaAdminRow(
                            categoria = cat,
                            onEdit = {
                                categoriaSeleccionada = cat
                                showDialog = true
                            },
                            onDelete = { onDeleteCategoria(cat.categoriaId) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AddActionButton(
                text = stringResource(R.string.add_new_category),
                onClick = {
                    categoriaSeleccionada = null
                    showDialog = true
                }
            )
        }

        if (showDialog) {
            CategoriaFormDialog(
                categoria = categoriaSeleccionada,
                isUploading = isUploading,
                onDismiss = { showDialog = false },
                onUploadImage = { uri, callback -> onUploadImage(context, uri, callback) },
                onConfirm = { nombre, desc, imgUrl ->
                    val catParaGuardar = if (categoriaSeleccionada != null) {
                        categoriaSeleccionada!!.copy(nombre = nombre, descripcion = desc, imagenUrl = imgUrl)
                    } else {
                        Categoria(nombre = nombre, descripcion = desc, imagenUrl = imgUrl)
                    }
                    onSaveCategoria(catParaGuardar)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun CategoriasHeader(onBack: () -> Unit, totalCategorias: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                Color(0xFF1E2A78),
                shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
            )
            .padding(horizontal = 20.dp, vertical = 30.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.atras), tint = Color.White)
                }
                Text(
                    stringResource(R.string.categorias_label),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ModernStatCard(
                title = stringResource(R.string.categorias_label).uppercase(),
                value = totalCategorias.toString(),
                icon = Icons.Default.Category,
                accentColor = Color(0xFFFFB74D),
                modifier = Modifier.width(160.dp)
            )
        }
    }
}

@Composable
fun CategoriaAdminRow(
    categoria: Categoria,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (categoria.imagenUrl.isNotEmpty()) categoria.imagenUrl else null,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray),
            contentScale = ContentScale.Crop,
            error = null
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = categoria.nombre,
            modifier = Modifier.weight(2f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (categoria.descripcion.isEmpty()) stringResource(R.string.no_description_short) else categoria.descripcion,
            modifier = Modifier.weight(1.5f),
            fontSize = 13.sp,
            color = Color.Gray,
            maxLines = 1
        )

        Row(
            modifier = Modifier.weight(0.8f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Edit, null, tint = Color(0xFF1E2A78), modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CategoriaFormDialog(
    categoria: Categoria?,
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onUploadImage: (Uri, (String) -> Unit) -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(categoria?.descripcion ?: "") }
    var imagenUrl by remember { mutableStateOf(categoria?.imagenUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (categoria == null) stringResource(R.string.add_category_title) else stringResource(R.string.edit_category_title),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E2A78)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ImagePicker(
                    imageUrl = imagenUrl,
                    imageUri = selectedImageUri,
                    isUploading = isUploading,
                    onImageSelected = { uri ->
                        selectedImageUri = uri
                        onUploadImage(uri) { url ->
                            imagenUrl = url
                        }
                    },
                    onImageUploaded = { url -> imagenUrl = url },
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text(stringResource(R.string.category_desc_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nombre.isNotBlank()) onConfirm(nombre, descripcion, imagenUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A78)),
                enabled = !isUploading,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar), color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(20.dp)

    )
    Spacer(modifier = Modifier.height(20.dp))
}