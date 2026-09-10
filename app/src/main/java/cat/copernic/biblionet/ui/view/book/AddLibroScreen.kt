package cat.copernic.biblionet.ui.view.book

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.copernic.biblionet.R
import cat.copernic.biblionet.ui.viewmodel.book.BookViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLibroScreen(
    libroId: String? = null,
    viewModel: BookViewModel = viewModel(),
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var isUploadingByCloudinary by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var expandedCategories by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(libroId) {
        if (libroId != null && libroId != "null") {
            viewModel.loadLibro(libroId)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingByCloudinary = true
            viewModel.uploadBookImage(context, it) { url ->
                if (url == null) {
                    Toast.makeText(context, context.getString(R.string.error_upload_cloudinary), Toast.LENGTH_SHORT).show()
                }
                isUploadingByCloudinary = false
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetSuccess()
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A237E))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, stringResource(R.string.atras), tint = Color.White)
            }
            Text(
                text = if (libroId == null || libroId == "null") stringResource(R.string.new_book) else stringResource(R.string.edit_book),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = if (uiState.imagen_url.isNotEmpty()) uiState.imagen_url else null,
                        contentDescription = stringResource(R.string.cover), modifier = Modifier
                            .width(150.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1A237E), RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F4F8)), contentScale = ContentScale.Crop)

                    if (isUploadingByCloudinary) {
                        CircularProgressIndicator(color = Color(0xFF1A237E))
                    }

                    IconButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 12.dp)
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A237E))
                            .border(2.dp, Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, stringResource(R.string.select_cover_photo), tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                BookInput(stringResource(R.string.title), uiState.titulo, viewModel::onTituloChange, Icons.Default.Title)
                BookInput(stringResource(R.string.author), uiState.autor, viewModel::onAutorChange, Icons.Default.Person)
                BookInput(stringResource(R.string.isbn), uiState.isbn, viewModel::onIsbnChange, Icons.Default.QrCode)

                // --- SELECTOR DE CATEGORÍA MEJORADO ---
                ExposedDropdownMenuBox(
                    expanded = expandedCategories,
                    onExpandedChange = { expandedCategories = !expandedCategories },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    val currentCategoryName = uiState.categorias.find { it.categoriaId == uiState.categoria_id }?.nombre ?: stringResource(R.string.seleccionar_categoria)
                    
                    OutlinedTextField(
                        value = currentCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category_label)) },
                        leadingIcon = { Icon(Icons.Default.Category, null, tint = Color(0xFF1A237E)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategories) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategories,
                        onDismissRequest = { expandedCategories = false }
                    ) {
                        uiState.categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.nombre) },
                                onClick = {
                                    viewModel.onCategoriaChange(categoria.categoriaId)
                                    expandedCategories = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                BookInput(stringResource(R.string.language), uiState.idioma, viewModel::onIdiomaChange, Icons.Default.Translate)
                BookInput(stringResource(R.string.short_description), uiState.descripcion_corta, viewModel::onDescCortaChange, Icons.Default.ShortText)
                BookInput(stringResource(R.string.long_description), uiState.descripcion_larga, viewModel::onDescLargaChange, Icons.Default.Description, false)

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState.error != null) {
                    Text(uiState.error!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
                }

                Button(
                    onClick = {
                        if (uiState.libro_id == null) viewModel.saveLibro()
                        else viewModel.updateLibro(uiState.libro_id!!, "")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                    enabled = !uiState.isLoading && !isUploadingByCloudinary
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text(if (uiState.libro_id == null) stringResource(R.string.save_book) else stringResource(R.string.update_book), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun BookInput(label: String, value: String, onValueChange: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF1A237E)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine
    )
}
