// Archivo: cat/copernic/biblionet/ui/view/inventory/InventarioListScreen.kt
package cat.copernic.biblionet.ui.view.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.inventory.Inventario // IMPORT AÑADIDO
import cat.copernic.biblionet.ui.viewmodel.book.BookViewModel
import cat.copernic.biblionet.ui.viewmodel.inventory.InventarioViewModel
import cat.copernic.biblionet.utils.GlobalState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioListScreen(
    inventarioViewModel: InventarioViewModel = viewModel(),
    bookViewModel: BookViewModel = viewModel(),
    onNavigateToCrear: () -> Unit,
    onNavigateToDetalle: (String) -> Unit,
    onBack: () -> Unit
) {
    val inventario = inventarioViewModel.inventario
    val isLoading = inventarioViewModel.isLoading
    val nombresLibros by bookViewModel.libroNombres.collectAsState()
    val usuarioActual = GlobalState.usuario

    LaunchedEffect(inventario) {
        if (inventario.isNotEmpty()) {
            bookViewModel.cargarNombresMasivos(inventario.map { it.libro_id })
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.inventory), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.atras), tint = Color.White) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A237E))
        )

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF1A237E))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.items_count, inventario.size), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Button(onClick = onNavigateToCrear, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.add))
                            }
                        }
                    }

                    items(inventario.filter { it.biblioteca_id == usuarioActual?.biblioteca_id }) { item ->
                        InventarioCard(
                            item = item,
                            tituloLibro = nombresLibros[item.libro_id] ?: stringResource(R.string.loading),
                            onClick = { onNavigateToDetalle(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InventarioCard(item: Inventario, tituloLibro: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tituloLibro, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E), maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.location_format, item.pasilloEstanteria), fontSize = 13.sp, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = RoundedCornerShape(8.dp), color = if (item.disponible) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)) {
                    Text(
                        text = if (item.disponible) stringResource(R.string.disponible) else stringResource(R.string.status_out_of_stock),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (item.disponible) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${item.stock_disponible}/${item.numero_copias}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}