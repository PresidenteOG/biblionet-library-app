// Archivo: src/main/java/cat/copernic/biblionet/ui/view/admin/ChangeUserRoleScreen.kt
package cat.copernic.biblionet.ui.view.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.auth.Role
import cat.copernic.biblionet.ui.viewmodel.user.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeUserRoleScreen(
    initialUid: String,
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val roles = Role.entries
    val bibliotecas = adminViewModel.bibliotecas
    var biblioSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(initialUid) {
        adminViewModel.selectUser(initialUid)
    }

    val selectedUser = adminViewModel.usuarios.find { it.uid == adminViewModel.selectedUid }
    val filteredBibliotecas = bibliotecas.filter { 
        it.nombre.contains(biblioSearchQuery, ignoreCase = true) || 
        it.direccion.contains(biblioSearchQuery, ignoreCase = true) 
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.gestion_roles), 
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color(0xFF1A237E))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A237E)
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // INFO CARD DEL USUARIO
            if (selectedUser != null) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = CircleShape,
                                color = Color(0xFF1A237E).copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person, 
                                        contentDescription = null, 
                                        tint = Color(0xFF1A237E),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.editando_usuario), 
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    selectedUser.nombre, 
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A237E)
                                )
                                Text(
                                    selectedUser.email, 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Seleccionar Rol", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, 
                    color = Color(0xFF1A237E),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // LISTA DE ROLES
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp
                ) {
                    Column {
                        roles.forEachIndexed { index, role ->
                            val isSelected = adminViewModel.selectedRole == role.name
                            RoleOptionItem(
                                roleName = role.name,
                                isSelected = isSelected,
                                onClick = { adminViewModel.selectRole(role.name) }
                            )
                            if (index < roles.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    thickness = 0.5.dp,
                                    color = Color(0xFFF0F0F0)
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN DE BIBLIOTECA (Solo si es Bibliotecario)
            if (adminViewModel.selectedRole == Role.LIBRARIAN.name) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Biblioteca Asociada", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF1A237E)
                        )
                        Text(
                            "${filteredBibliotecas.size} sedes",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Gray
                        )
                    }
                }
                
                item {
                    // BUSCADOR DE BIBLIOTECAS
                    OutlinedTextField(
                        value = biblioSearchQuery,
                        onValueChange = { biblioSearchQuery = it },
                        placeholder = { Text("Buscar sede...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            focusedBorderColor = Color(0xFF1A237E)
                        ),
                        singleLine = true
                    )
                }

                items(filteredBibliotecas) { bib ->
                    val isSelected = adminViewModel.selectedBibliotecaId == bib.id
                    BibliotecaSelectionCard(
                        nombre = bib.nombre,
                        direccion = bib.direccion,
                        isSelected = isSelected,
                        onClick = { adminViewModel.onBibliotecaSelected(bib.id) }
                    )
                }

                if (filteredBibliotecas.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron sedes", color = Color.Gray)
                        }
                    }
                }
            }

            // BOTÓN DE ACCIÓN
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { adminViewModel.saveRoleChange { onBack() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A237E),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    enabled = adminViewModel.selectedUid != null && adminViewModel.selectedRole != null
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.guardar_cambios).uppercase(), 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RoleOptionItem(
    roleName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) Color(0xFF1A237E).copy(alpha = 0.05f) else Color.Transparent,
        animationSpec = tween(300), label = ""
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 20.dp)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF1A237E) else Color.LightGray,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = roleName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF1A237E) else Color.DarkGray
        )
    }
}

@Composable
fun BibliotecaSelectionCard(
    nombre: String,
    direccion: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        if (isSelected) Color(0xFFE8EAF6) else Color.White, label = ""
    )
    val borderColor by animateColorAsState(
        if (isSelected) Color(0xFF1A237E) else Color.Transparent, label = ""
    )
    val elevation by animateDpAsState(
        if (isSelected) 4.dp else 0.dp, label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else BorderStroke(1.dp, Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFF1A237E) else Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business, 
                    contentDescription = null, 
                    tint = if (isSelected) Color.White else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nombre, 
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF1A237E) else Color.Black
                )
                Text(
                    direccion, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle, 
                    contentDescription = null, 
                    tint = Color(0xFF1A237E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
