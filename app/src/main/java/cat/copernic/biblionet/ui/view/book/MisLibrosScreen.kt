package cat.copernic.biblionet.ui.view.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.auth.Usuario
import cat.copernic.biblionet.ui.components.InfoPrestamoCard
import cat.copernic.biblionet.ui.viewmodel.book.MisLibrosViewModel
import cat.copernic.biblionet.ui.viewmodel.user.UserViewModel

enum class MisLibrosTab { RESERVAS, FAVORITOS, HISTORIAL }

@Composable
fun MisLibrosScreen(
    onBack: () -> Unit,
    onNavigateToDetalle: (String) -> Unit,
    viewModel: MisLibrosViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val usuario = userViewModel.usuario ?: Usuario(nombre = "Usuario")
    var selectedTab by remember { mutableStateOf(MisLibrosTab.FAVORITOS) }

    // Disparador de carga real según la pestaña seleccionada
    LaunchedEffect(selectedTab) {
        viewModel.cargarDatosSegunTab(selectedTab)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFDFDFD))
        ) {
            // 1. Cabecera con soporte multi-idioma
            MisLibrosHeaderSection(usuario = usuario, onBack = onBack)

            // 2. Barra de búsqueda con selector de modo (Sync/Manual)
            MisLibrosSearchBarSection(viewModel = viewModel)

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Sección de pestañas (Tabs)
            MisLibrosTabsSection(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Lista de resultados dinámica
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1A237E))
                }
            } else if (viewModel.prestamosAMostrar.isEmpty()) {
                EmptyStateMessage(selectedTab)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1), // Diseño de lista vertical
                    contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp), // Espacio para el nav fijo
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.prestamosAMostrar) { item ->
                        InfoPrestamoCard(
                            libro = item.libro,
                            estado = item.estado,
                            fecha = item.fecha,
                            onClick = { onNavigateToDetalle(item.libro.libro_id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MisLibrosHeaderSection(usuario: Usuario, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF1A237E))
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = stringResource(id = R.string.personal_library),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E)
            )
            Text(
                text = stringResource(id = R.string.manage_books, usuario.nombre),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun MisLibrosTabsSection(
    selectedTab: MisLibrosTab,
    onTabSelected: (MisLibrosTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabItem(
            label = stringResource(id = R.string.tab_reservations),
            icon = Icons.Default.Bookmark,
            isSelected = selectedTab == MisLibrosTab.RESERVAS,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(MisLibrosTab.RESERVAS) }
        )
        TabItem(
            label = stringResource(id = R.string.tab_favorites),
            icon = Icons.Default.Favorite,
            isSelected = selectedTab == MisLibrosTab.FAVORITOS,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(MisLibrosTab.FAVORITOS) }
        )
        TabItem(
            label = stringResource(id = R.string.tab_history),
            icon = Icons.Default.History,
            isSelected = selectedTab == MisLibrosTab.HISTORIAL,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(MisLibrosTab.HISTORIAL) }
        )
    }
}

@Composable
fun TabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(35.dp) // Un poco más de altura para legibilidad
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF1A237E) else Color(0xFFF1F4F8))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else Color(0xFF1A237E)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF1A237E)
            )
        }
    }
}

@Composable
fun MisLibrosSearchBarSection(viewModel: MisLibrosViewModel) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = { Text(stringResource(id = R.string.filter_library)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    // Botón manual: solo aparece si Sync está desactivado y hay texto
                    if (!viewModel.isRealTimeSearch && viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.ejecutarBusquedaManual() }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF1A237E))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1A237E),
                    unfocusedContainerColor = Color(0xFFF1F4F8),
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de alternancia Sync (Diseño Cuadrado Robusto)
            IconButton(
                onClick = { viewModel.onToggleSearchMode() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (viewModel.isRealTimeSearch) Color(0xFF1A237E) else Color(0xFFF1F4F8))
            ) {
                Icon(
                    imageVector = if (viewModel.isRealTimeSearch) Icons.Default.Sync else Icons.Default.SyncDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (viewModel.isRealTimeSearch) Color.White else Color(0xFF1A237E)
                )
            }
        }

        // Texto informativo del modo de búsqueda
        Text(
            text = if (viewModel.isRealTimeSearch)
                stringResource(id = R.string.real_time_active)
            else
                stringResource(id = R.string.manual_mode),
            fontSize = 10.sp,
            color = if (viewModel.isRealTimeSearch) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        )
    }
}

@Composable
fun EmptyStateMessage(tab: MisLibrosTab) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when(tab) {
            MisLibrosTab.RESERVAS -> Icons.Default.BookmarkBorder
            MisLibrosTab.FAVORITOS -> Icons.Default.FavoriteBorder
            MisLibrosTab.HISTORIAL -> Icons.Default.History
        }
        val messageRes = when(tab) {
            MisLibrosTab.RESERVAS -> R.string.empty_reservations
            MisLibrosTab.FAVORITOS -> R.string.empty_favorites
            MisLibrosTab.HISTORIAL -> R.string.empty_history
        }

        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = messageRes),
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}
