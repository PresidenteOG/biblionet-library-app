// Archivo: src/main/java/cat/copernic/biblionet/ui/view/admin/AdminPanelScreen.kt
package cat.copernic.biblionet.ui.view.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.auth.Usuario
import cat.copernic.biblionet.data.model.library.Biblioteca
import cat.copernic.biblionet.ui.viewmodel.user.AdminViewModel
import cat.copernic.biblionet.ui.viewmodel.admin.AdminQuizViewModel

enum class AdminSection { USERS, LIBRARIES, QUIZ }

@Composable
fun AdminPanelScreen(
    navController: NavHostController,
    usuarios: List<Usuario>,
    bibliotecas: List<Biblioteca>,
    librosCount: Int,
    adminViewModel: AdminViewModel,
    onAddLibrary: () -> Unit,
    onBack: () -> Unit,
    onEditUser: (Usuario) -> Unit,
    onEditLib: (Biblioteca) -> Unit,
    onManageQuiz: (String?) -> Unit
) {
    var selectedSection by remember { mutableStateOf(AdminSection.USERS) }
    val tabs = listOf(AdminSection.USERS, AdminSection.LIBRARIES, AdminSection.QUIZ)

    var expandUsers by remember { mutableStateOf(false) }
    var expandLibs by remember { mutableStateOf(false) }

    var searchUserQuery by remember { mutableStateOf("") }
    var searchLibQuery by remember { mutableStateOf("") }

    val quizViewModel: AdminQuizViewModel = viewModel()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE0E3E8))) {
        Column {
            AdminHeader(onBack = onBack, libros = librosCount, users = usuarios.size, libs = bibliotecas.size)
            ManagementTabs(tabs = tabs, selectedSection = selectedSection, onTabSelected = { selectedSection = it })

            if (selectedSection == AdminSection.QUIZ) {
                AdminQuizInlineList(quizViewModel, adminViewModel, onNavigateToForm = { onManageQuiz(it) })
            } else {
                val isExpanded = if (selectedSection == AdminSection.USERS) expandUsers else expandLibs

                ManagementListContainer(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    title = if (selectedSection == AdminSection.USERS) stringResource(R.string.gestion_roles) else stringResource(R.string.gestion_bibliotecas),
                    isExpanded = isExpanded,
                    searchQuery = if (selectedSection == AdminSection.USERS) searchUserQuery else searchLibQuery,
                    onSearchQueryChange = { if (selectedSection == AdminSection.USERS) searchUserQuery = it else searchLibQuery = it },
                    searchPlaceholder = if (selectedSection == AdminSection.USERS) stringResource(R.string.buscar_usuario) else stringResource(R.string.buscar_biblioteca),
                    onViewAllClick = {
                        if (selectedSection == AdminSection.USERS) expandUsers = !expandUsers else expandLibs = !expandLibs
                    }
                ) {
                    if (selectedSection == AdminSection.USERS) {
                        val filteredUsers = usuarios.filter { (it.nombre ?: "").contains(searchUserQuery, true) || (it.email ?: "").contains(searchUserQuery, true) }
                        UserList(if (expandUsers) filteredUsers else filteredUsers.take(3)) { usuario -> onEditUser(usuario) }
                    } else {
                        val filteredLibs = bibliotecas.filter { (it.nombre ?: "").contains(searchLibQuery, true) || (it.direccion ?: "").contains(searchLibQuery, true) }
                        LibraryList(if (expandLibs) filteredLibs else filteredLibs.take(3), onEditLib)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (selectedSection == AdminSection.LIBRARIES) {
                AddActionButton(text = stringResource(R.string.add_new_library), onClick = onAddLibrary)
            }
        }
    }
}

@Composable
fun AdminQuizInlineList(quizViewModel: AdminQuizViewModel, adminViewModel: AdminViewModel, onNavigateToForm: (String?) -> Unit) {
    val context = LocalContext.current
    var preguntaABorrar by remember { mutableStateOf<String?>(null) }
    var searchQuizQuery by remember { mutableStateOf("") }

    var showResetDialog by remember { mutableStateOf(false) }
    var afectarRachas by remember { mutableStateOf(false) } // NUEVO ESTADO

    LaunchedEffect(Unit) { quizViewModel.cargarPreguntas() }

    if (preguntaABorrar != null) {
        AlertDialog(
            onDismissRequest = { preguntaABorrar = null },
            title = { Text(stringResource(R.string.delete_question_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_question_desc)) },
            confirmButton = {
                Button(onClick = { quizViewModel.eliminarPregunta(preguntaABorrar!!); preguntaABorrar = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))) { Text(stringResource(R.string.delete), color = Color.White) }
            },
            dismissButton = { OutlinedButton(onClick = { preguntaABorrar = null }) { Text(stringResource(R.string.cancelar), color = Color(0xFF1A237E)) } }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.confirm_reset_quiz_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.confirm_reset_quiz_desc))
                    Spacer(modifier = Modifier.height(16.dp))

                    // SWITCH PARA DECIDIR SI AFECTA A LAS RACHAS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = afectarRachas,
                            onCheckedChange = { afectarRachas = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1A237E), checkedTrackColor = Color(0xFF1A237E).copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.afectar_rachas), fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                    }
                    Text(
                        text = if (afectarRachas) stringResource(R.string.afectar_rachas_si) else stringResource(R.string.afectar_rachas_no),
                        fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        // PASAMOS EL ESTADO DEL SWITCH A LA BASE DE DATOS
                        adminViewModel.reiniciarQuizGlobalmente(afectarRachas) { exito, errorMsg ->
                            if (exito) {
                                Toast.makeText(context, context.getString(R.string.quiz_reiniciado_exito), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Fallo: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) { Text(stringResource(R.string.reiniciar_todos), color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.reiniciar_todos).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.gestion_preguntas), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E2A78))
            IconButton(onClick = { onNavigateToForm(null) }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Añadir", tint = Color(0xFF1A237E), modifier = Modifier.size(32.dp))
            }
        }

        OutlinedTextField(
            value = searchQuizQuery,
            onValueChange = { searchQuizQuery = it },
            placeholder = { Text(stringResource(R.string.buscar_pregunta)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
        )

        Surface(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)), color = Color.White, shadowElevation = 2.dp) {
            if (quizViewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF1E2A78)) }
            } else {
                val filteredQuiz = quizViewModel.preguntas.filter { (it.texto ?: "").contains(searchQuizQuery, true) }
                if (filteredQuiz.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay preguntas", color = Color.Gray) }
                } else {
                    LazyColumn {
                        items(filteredQuiz) { pregunta ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).clickable { onNavigateToForm(pregunta.id) }, verticalAlignment = Alignment.CenterVertically) {
                                Text(pregunta.texto, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                IconButton(onClick = { preguntaABorrar = pregunta.id }) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminHeader(onBack: () -> Unit, libros: Int, users: Int, libs: Int) {
    Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(Color(0xFF1E2A78), shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)).padding(horizontal = 20.dp, vertical = 30.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                Text(stringResource(R.string.panel_admin), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernStatCard("BOOKS", libros.toString(), Icons.Default.MenuBook, Color(0xFF90CAF9), Modifier.weight(1f))
                ModernStatCard("USERS", users.toString(), Icons.Default.Group, Color(0xFF4ADE80), Modifier.weight(1f))
                ModernStatCard("LIBS", libs.toString(), Icons.Default.Storage, Color(0xFFFFB74D), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ManagementTabs(tabs: List<AdminSection>, selectedSection: AdminSection, onTabSelected: (AdminSection) -> Unit) {
    TabRow(selectedTabIndex = tabs.indexOf(selectedSection), containerColor = Color.Transparent, contentColor = Color(0xFF1E2A78), divider = {}) {
        tabs.forEach { section ->
            Tab(
                selected = selectedSection == section, onClick = { onTabSelected(section) },
                text = { Text(when (section) { AdminSection.USERS -> stringResource(R.string.usuarios); AdminSection.LIBRARIES -> stringResource(R.string.bibliotecas); AdminSection.QUIZ -> stringResource(R.string.quiz_diario) }, fontWeight = FontWeight.Bold) }
            )
        }
    }
}

@Composable
fun ManagementListContainer(
    modifier: Modifier = Modifier,
    title: String,
    isExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    onViewAllClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E2A78))
            TextButton(onClick = onViewAllClick) { Text(if (isExpanded) stringResource(R.string.ocultar) else stringResource(R.string.ver_todo), color = Color(0xFF1E2A78)) }
        }

        if (isExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )
        }

        val surfaceModifier = if (isExpanded) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth()

        Surface(modifier = surfaceModifier.clip(RoundedCornerShape(20.dp)), color = Color.White, shadowElevation = 2.dp) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA)).padding(16.dp, 8.dp)) {
                    Text(stringResource(R.string.nombre_col), Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.info_col), Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.acc_col), Modifier.weight(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                content()
            }
        }
    }
}

@Composable
fun UserList(usuarios: List<Usuario>, onEdit: (Usuario) -> Unit) {
    LazyColumn {
        items(usuarios) { usuario ->
            GenericAdminRow(title = usuario.nombre, subtitle = usuario.email, onEdit = { onEdit(usuario) })
        }
    }
}

@Composable
fun LibraryList(bibliotecas: List<Biblioteca>, onEdit: (Biblioteca) -> Unit) {
    val sinTelTxt = stringResource(R.string.sin_tel)
    LazyColumn {
        items(bibliotecas) { biblio ->
            val telefonoTexto = if (biblio.telefono.isBlank()) sinTelTxt else biblio.telefono
            GenericAdminRow(title = biblio.nombre, subtitle = telefonoTexto, onEdit = { onEdit(biblio) })
        }
    }
}

@Composable
fun GenericAdminRow(title: String, subtitle: String, onEdit: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(2f), fontSize = 14.sp,fontWeight = FontWeight.Bold)
        Text(subtitle, Modifier.weight(1.5f), fontSize = 13.sp, color = Color.Gray, maxLines = 1)
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = Color(0xFF1E2A78)) }
    }
}

@Composable
fun AddActionButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A78))) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ModernStatCard(title: String, value: String, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(100.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF283593))) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }
    }
}