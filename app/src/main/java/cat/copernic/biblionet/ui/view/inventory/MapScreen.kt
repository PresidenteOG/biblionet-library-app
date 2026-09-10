// Archivo: src/main/java/cat/copernic/biblionet/ui/view/inventory/MapScreen.kt
package cat.copernic.biblionet.ui.view.inventory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.library.Biblioteca
import cat.copernic.biblionet.ui.viewmodel.inventory.MapLibro
import cat.copernic.biblionet.ui.viewmodel.inventory.MapViewModel
import coil.compose.AsyncImage
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onNavigateToBookDetails: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val bibliotecasAEnsenyar = viewModel.filteredBibliotecas
    val selectedBiblio = viewModel.selectedBiblioteca
    val selectedLibro = viewModel.selectedLibro

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
        if (!hasLocationPermission) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    var searchTextFieldValue by remember { mutableStateOf(TextFieldValue(viewModel.searchQuery)) }
    var showFullCatalog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.searchQuery) {
        if (viewModel.searchQuery != searchTextFieldValue.text) {
            searchTextFieldValue = searchTextFieldValue.copy(text = viewModel.searchQuery)
        }
    }

    LaunchedEffect(selectedBiblio) {
        if (selectedBiblio == null) {
            showFullCatalog = false
        }
    }

    val ubicacionBuscadaTexto = stringResource(R.string.ubicacion_buscada)
    val obteniendoGps = stringResource(R.string.obteniendo_gps)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(9.0)
                    controller.setCenter(viewModel.userLocation)
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapViewRef = mapView
                mapView.overlays.removeAll { it is Marker }

                if (hasLocationPermission && !mapView.overlays.any { it is MyLocationNewOverlay }) {
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
                    locationOverlay.enableMyLocation()
                    locationOverlay.runOnFirstFix {
                        val miUbicacionReal = locationOverlay.myLocation
                        if (miUbicacionReal != null && viewModel.searchedLocationMarker == null) {
                            mapView.post {
                                mapView.controller.animateTo(miUbicacionReal)
                                mapView.controller.setZoom(15.0)
                                viewModel.updateUserLocation(miUbicacionReal)
                            }
                        }
                    }
                    mapView.overlays.add(locationOverlay)
                }

                viewModel.searchedLocationMarker?.let { loc ->
                    val yellowMarker = Marker(mapView)
                    yellowMarker.position = loc
                    yellowMarker.title = ubicacionBuscadaTexto
                    val yellowIcon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)?.mutate()
                    yellowIcon?.setColorFilter(android.graphics.Color.parseColor("#FFC107"), android.graphics.PorterDuff.Mode.SRC_IN)
                    if (yellowIcon != null) yellowMarker.icon = yellowIcon
                    yellowMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(yellowMarker)
                }

                bibliotecasAEnsenyar.forEach { biblio ->
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(biblio.latitud, biblio.longitud)
                    marker.title = biblio.nombre
                    val redIcon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)?.mutate()
                    redIcon?.setColorFilter(android.graphics.Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
                    if (redIcon != null) marker.icon = redIcon
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.setOnMarkerClickListener { _, _ -> viewModel.onBibliotecaSelected(biblio); true }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = searchTextFieldValue,
                        onValueChange = { newValue ->
                            searchTextFieldValue = newValue
                            viewModel.onSearchQueryChanged(newValue.text, context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.buscar_calle_ciudad)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchTextFieldValue.text.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchTextFieldValue = TextFieldValue("")
                                    viewModel.onSearchQueryChanged("", context)
                                    focusManager.clearFocus()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF1A237E),
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    AnimatedVisibility(visible = viewModel.addressSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(viewModel.addressSuggestions) { address ->
                                    val fullAddress = address.getAddressLine(0)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            viewModel.selectSuggestion(address)
                                            mapViewRef?.controller?.animateTo(GeoPoint(address.latitude, address.longitude))
                                            mapViewRef?.controller?.setZoom(16.0)
                                            focusManager.clearFocus()
                                        }.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(fullAddress, fontSize = 14.sp, color = Color.Black)
                                    }
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.toggleFilterVisibility() },
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = if (viewModel.isFilterVisible) Color(0xFF1A237E) else Color.Gray)
                }
            }

            AnimatedVisibility(visible = viewModel.isFilterVisible) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.filtro_distancia_km), fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                        OutlinedTextField(
                            value = viewModel.maxDistanceInput,
                            onValueChange = { if (it.length <= 5) viewModel.onMaxDistanceInputChanged(it) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text(stringResource(R.string.ej_20_vacio)) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            trailingIcon = { Text("km", color = Color.Gray, modifier = Modifier.padding(end = 16.dp)) }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 110.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FloatingActionButton(
                    onClick = {
                        val locationOverlay = mapViewRef?.overlays?.filterIsInstance<MyLocationNewOverlay>()?.firstOrNull()
                        val miUbicacion = locationOverlay?.myLocation
                        if (miUbicacion != null) {
                            mapViewRef?.controller?.animateTo(miUbicacion)
                            mapViewRef?.controller?.setZoom(17.0)
                            viewModel.updateUserLocation(miUbicacion)
                        } else {
                            Toast.makeText(context, obteniendoGps, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                    containerColor = Color(0xFF1A237E),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                }
            }

            AnimatedVisibility(visible = selectedBiblio == null && viewModel.closestBibliotecas.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(viewModel.closestBibliotecas) { biblio ->
                        val distanciaExacta = viewModel.calcularDistancia(biblio)

                        ClosestLibraryCard(
                            biblioteca = biblio,
                            distanciaKm = distanciaExacta,
                            onClick = {
                                viewModel.onBibliotecaSelected(biblio)
                                mapViewRef?.controller?.animateTo(GeoPoint(biblio.latitud, biblio.longitud))
                                mapViewRef?.controller?.setZoom(16.0)
                            }
                        )
                    }
                }
            }
        }

        if (selectedBiblio != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    viewModel.clearSelection()
                    showFullCatalog = false
                },
                containerColor = Color.White,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = showFullCatalog)
            ) {
                if (!showFullCatalog) {
                    LibraryProfileContent(
                        biblioteca = selectedBiblio,
                        libros = viewModel.librosDeBiblioteca,
                        isLoadingBooks = viewModel.isLoadingBooks,
                        onClose = {
                            viewModel.clearSelection()
                            showFullCatalog = false
                        },
                        onBookClick = { libro -> viewModel.onLibroSelected(libro) },
                        onViewMore = { showFullCatalog = true },
                        viewModel = viewModel
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { showFullCatalog = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color(0xFF1A237E))
                            }
                            Text(
                                text = "${stringResource(R.string.library_catalog)} - ${selectedBiblio.nombre}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A237E),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = {
                                viewModel.clearSelection()
                                showFullCatalog = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cerrar), tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(viewModel.librosDeBiblioteca) { libro ->
                                BookCard(libro, modifier = Modifier.clickable {
                                    viewModel.onLibroSelected(libro)
                                })
                            }
                        }
                    }
                }
            }
        }

        if (selectedLibro != null) {
            AlertDialog(
                onDismissRequest = { viewModel.onLibroSelected(null) },
                confirmButton = {
                    Button(
                        onClick = {
                            val idToNavigate = selectedLibro.id
                            viewModel.onLibroSelected(null)

                            if (idToNavigate.isNotBlank()) {
                                onNavigateToBookDetails(idToNavigate)
                            } else {
                                Toast.makeText(context, "Error al cargar los datos del libro", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                    ) {
                        Text("Ver Detalles")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onLibroSelected(null) }) {
                        Text(stringResource(R.string.cerrar), color = Color.Gray)
                    }
                },
                title = { Text(selectedLibro.titulo, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(String.format(stringResource(R.string.autor_formato), selectedLibro.autor), color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        val statusColor = if (selectedLibro.disponible) Color(0xFF22C55E) else Color(0xFFEF4444)
                        Text(if (selectedLibro.disponible) stringResource(R.string.disponible) else stringResource(R.string.no_disponible), color = statusColor, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// --- TARJETA DE BIBLIOTECA CERCANA REDISEÑADA Y BALANCEADA ---
@Composable
fun ClosestLibraryCard(biblioteca: Biblioteca, distanciaKm: Double, onClick: () -> Unit) {
    val context = LocalContext.current
    val msgCopiado = stringResource(R.string.direccion_copiada)

    Card(
        modifier = Modifier
            .width(260.dp) // Más ancha para que respiren los textos
            .height(100.dp) // Más alta para que quepan las 3 filas holgadamente
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp), // Mayor margen interno
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto proporcionada al nuevo alto
            if (biblioteca.fotoUrl.isNotEmpty()) {
                AsyncImage(
                    model = biblioteca.fotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF1A237E)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Datos agrupados a la derecha y centrados verticalmente
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(start = 12.dp), // Separación entre foto y textos
                verticalArrangement = Arrangement.Center
            ) {
                // Título
                Text(
                    text = biblioteca.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Dirección Copiable
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Dirección", biblioteca.direccion)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, msgCopiado, Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = biblioteca.direccion,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Color.Gray, modifier = Modifier.size(10.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Kilómetros
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color(0xFF1A237E), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.1f km".format(distanciaKm),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A237E)
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryProfileContent(
    biblioteca: Biblioteca,
    libros: List<MapLibro>,
    isLoadingBooks: Boolean,
    onClose: () -> Unit,
    onBookClick: (MapLibro) -> Unit,
    onViewMore: () -> Unit,
    viewModel: MapViewModel
) {
    val context = LocalContext.current
    val msgCopiado = stringResource(R.string.direccion_copiada)

    val (estadoTexto, estaAbierto) = viewModel.obtenerEstadoApertura(biblioteca, context)
    val colorEstado = if (estaAbierto) Color(0xFF22C55E) else Color(0xFFEF4444)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (biblioteca.fotoUrl.isNotEmpty()) {
                AsyncImage(
                    model = biblioteca.fotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1A237E)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = biblioteca.nombre + if (biblioteca.accesibilidad) " ♿" else "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Dirección", biblioteca.direccion)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, msgCopiado, Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(biblioteca.direccion, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Color.Gray, modifier = Modifier.size(12.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (biblioteca.telefono.isNotBlank()) biblioteca.telefono else "Teléfono no disponible",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (biblioteca.email.isNotBlank()) biblioteca.email else "Correo electrónico no disponible",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colorEstado))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = estadoTexto, color = colorEstado, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.catalogo_destacado), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoadingBooks) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1A237E)) }
        } else if (libros.isEmpty()) {
            Text(stringResource(R.string.no_libros_disponibles), color = Color.Gray)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(libros.take(3)) { libro ->
                    BookCard(libro, modifier = Modifier.clickable { onBookClick(libro) })
                }

                if (libros.size > 3) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onViewMore() }
                                .padding(horizontal = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE8EAF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.view_more), tint = Color(0xFF1A237E))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.view_more), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(libro: MapLibro, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(90.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = libro.portadaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (libro.disponible) Color(0xFF22C55E) else Color(0xFFEF4444))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = libro.titulo,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}