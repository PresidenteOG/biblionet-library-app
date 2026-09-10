// Archivo: cat/copernic/biblionet/NavigationWrapper.kt
package cat.copernic.biblionet

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LibraryBooks
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.firebase.firestore.FirebaseFirestore

// App Imports
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.auth.Usuario
import cat.copernic.biblionet.data.model.book.Categoria
import cat.copernic.biblionet.ui.view.admin.AdminPanelScreen
import cat.copernic.biblionet.ui.view.admin.AdminSection
import cat.copernic.biblionet.ui.view.admin.ChangeUserRoleScreen
import cat.copernic.biblionet.ui.view.auth.InicioLoginScreen
import cat.copernic.biblionet.ui.view.auth.LoginScreen
import cat.copernic.biblionet.ui.view.auth.RegistroScreen
import cat.copernic.biblionet.ui.view.book.AddLibroScreen
import cat.copernic.biblionet.ui.view.book.DetailsLibroScreen
import cat.copernic.biblionet.ui.view.book.GestionCategoriasScreen
import cat.copernic.biblionet.ui.view.book.AllCategoriasScreen
import cat.copernic.biblionet.ui.view.book.MisLibrosScreen
import cat.copernic.biblionet.ui.view.inicio.InicioScreen
import cat.copernic.biblionet.ui.view.inventory.InventarioDetailScreen
import cat.copernic.biblionet.ui.view.inventory.InventarioFormScreen
import cat.copernic.biblionet.ui.view.inventory.InventarioListScreen
import cat.copernic.biblionet.ui.view.inventory.MapScreen
import cat.copernic.biblionet.ui.view.library.AddBibliotecaScreen
import cat.copernic.biblionet.ui.view.library.BibliotecarioPanelScreen
import cat.copernic.biblionet.ui.view.library.EditBibliotecaScreen
import cat.copernic.biblionet.ui.view.library.SolicitarReservaScreen
import cat.copernic.biblionet.ui.view.profile.EditarPerfilScreen
import cat.copernic.biblionet.ui.view.profile.PerfilScreen
import cat.copernic.biblionet.ui.viewmodel.user.AdminViewModel
import cat.copernic.biblionet.ui.viewmodel.user.ProfileViewModel
import cat.copernic.biblionet.ui.viewmodel.user.UserViewModel
import cat.copernic.biblionet.ui.viewmodel.book.CategoriasViewModel
import cat.copernic.biblionet.ui.view.settings.AboutScreen
import cat.copernic.biblionet.ui.view.transaction.QuizScreen
import cat.copernic.biblionet.ui.viewmodel.admin.AdminQuizListScreen
import cat.copernic.biblionet.ui.viewmodel.admin.AdminQuizFormScreen
import cat.copernic.biblionet.ui.view.transaction.GestionReservasScreen

@Composable
fun NavigationWrapper(navHostController: NavHostController) {
    val context = LocalContext.current
    val navBackStackEntry by navHostController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userViewModel: UserViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()
    val categoriasViewModel: CategoriasViewModel = viewModel()

    val usuarioGlobal = userViewModel.usuario
    val isLoadingGlobal = userViewModel.isLoading

    // Quiz Popup State
    var showQuizPopup by remember { mutableStateOf(false) }
    var quizPopupMessage by remember { mutableStateOf("") }

    val authRoutes = listOf("InicioLogin", "Login", "Registro")

    // --- REDIRECCIÓN GLOBAL POR CIERRE DE SESIÓN FORZADO ---
    LaunchedEffect(usuarioGlobal, isLoadingGlobal) {
        if (!isLoadingGlobal && usuarioGlobal == null && currentRoute !in authRoutes) {
            navHostController.navigate("InicioLogin") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // SISTEMA DE NOTIFICACIONES DEL QUIZ MEJORADO
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastNotifiedDate = prefs.getString("last_daily_notification", "")

        // Notificación diaria del quiz
        if (today != lastNotifiedDate) {
            quizPopupMessage = context.getString(R.string.nuevo_dia_quiz)
            showQuizPopup = true
            prefs.edit().putString("last_daily_notification", today).apply()
        }

        // Listener para alertas en tiempo real del admin
        FirebaseFirestore.getInstance().collection("sistema").document("alertas")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val timestampObj = snapshot.getTimestamp("timestamp")
                val isFresh = if (timestampObj == null) {
                    true
                } else {
                    val timestamp = timestampObj.toDate().time
                    val now = System.currentTimeMillis()
                    (now - timestamp < 15000) // 15 segundos de margen
                }

                if (isFresh) {
                    val msg = snapshot.getString("mensaje") ?: "¡Quiz actualizado!"
                    val lastMsgTime = prefs.getLong("last_alert_time", 0L)
                    val now = System.currentTimeMillis()

                    // Evitar notificaciones duplicadas
                    if (now - lastMsgTime > 5000) {
                        quizPopupMessage = msg
                        showQuizPopup = true

                        // EL ADMIN HA RESETEADO: DESTRUIR CONTINUIDAD DEL QUIZ
                        prefs.edit().apply {
                            putLong("last_alert_time", now)
                            remove("ongoing_quiz_date")
                            remove("ongoing_quiz_index")
                            remove("ongoing_quiz_score")
                            remove("question_end_time")
                        }.apply()

                        // Mostrar también un Toast para asegurar visibilidad
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }

        // Verificación periódica de cambio de día
        while(true) {
            kotlinx.coroutines.delay(60000) // Revisar cada minuto
            val nuevoDia = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
            val lastNotif = prefs.getString("last_daily_notification", "")
            if (nuevoDia != lastNotif) {
                quizPopupMessage = context.getString(R.string.nuevo_dia_quiz)
                showQuizPopup = true
                prefs.edit().putString("last_daily_notification", nuevoDia).apply()
            }
        }
    }

    // Rutas donde NO se muestra la barra inferior
    val hiddenBottomBarRoutes = setOf(
        "EditarPerfil", "AdminPanel", "addLibraryScreen", "changeUserRole/{uid}",
        "CrearBiblioteca", "GestionCategorias", "editLibrary/{bibliotecaId}",
        "InventarioForm", "InventarioDetail/{inventarioId}", "AllCategorias",
        "books/form?libroId={libroId}", "LibroDetail/{libro_id}", "BibliotecarioPanel",
        "Quiz", "AdminQuizList", "AdminQuizForm?id={id}", "InventarioList", "about",
        "SolicitarReserva/{libro_id}", "GestionReservas"
    )

    val shouldShowBottomBar = currentRoute != null &&
            currentRoute !in authRoutes &&
            currentRoute !in hiddenBottomBarRoutes &&
            !currentRoute.startsWith("InventarioDetail") &&
            !currentRoute.startsWith("AdminQuizForm") &&
            !currentRoute.startsWith("LibroDetail") &&
            !currentRoute.startsWith("books/form") &&
            !currentRoute.startsWith("editLibrary") &&
            !currentRoute.startsWith("changeUserRole")

    Scaffold(
        containerColor = Color(0xFFFDFDFD)
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {

            NavHost(
                navController = navHostController,
                startDestination = "InicioLogin"
            ) {

                composable("InicioLogin") {
                    InicioLoginScreen(
                        onNavegarLogin = { navHostController.navigate("Login") },
                        onNavegarRegistro = { navHostController.navigate("Registro") }
                    )
                }

                composable("Login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navHostController.navigate("Home") {
                                popUpTo("InicioLogin") { inclusive = true }
                            }
                        },
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable("Registro") {
                    RegistroScreen(
                        onRegisterSuccess = {
                            navHostController.navigate("Home") {
                                popUpTo("InicioLogin") { inclusive = true }
                            }
                        },
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable("Home") {
                    if (isLoadingGlobal) {
                        LoadingBox()
                    } else if (usuarioGlobal != null) {
                        InicioScreen(
                            usuario = usuarioGlobal,
                            navController = navHostController,
                            onProfileClick = { navHostController.navigate("Perfil") }
                        )
                    } else {
                        // Fallback para usuario nulo
                        InicioScreen(
                            usuario = Usuario(nombre = "Usuario"),
                            navController = navHostController,
                            onProfileClick = { navHostController.navigate("Perfil") }
                        )
                    }
                }

                composable("Mapa") {
                    MapScreen(
                        onNavigateToBookDetails = { libroId ->
                            navHostController.navigate("LibroDetail/$libroId")
                        }
                    )
                }

                composable("MisLibros") {
                    MisLibrosScreen(
                        onBack = { navHostController.popBackStack() },
                        onNavigateToDetalle = { id -> navHostController.navigate("LibroDetail/$id") }
                    )
                }

                composable(
                    route = "books/form?libroId={libroId}",
                    arguments = listOf(navArgument("libroId") { type = NavType.StringType; nullable = true; defaultValue = null })
                ) { backStackEntry ->
                    val libroId = backStackEntry.arguments?.getString("libroId")
                    AddLibroScreen(
                        libroId = if (libroId == "null") null else libroId,
                        onSuccess = { navHostController.popBackStack() },
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable(
                    route = "LibroDetail/{libro_id}",
                    arguments = listOf(navArgument("libro_id") { type = NavType.StringType })
                ) { backStackEntry ->
                    val libroId = backStackEntry.arguments?.getString("libro_id") ?: ""
                    DetailsLibroScreen(
                        libroId = libroId,
                        onBackClick = { navHostController.popBackStack() },
                        onReservarClick = { navHostController.navigate("SolicitarReserva/$libroId") }
                    )
                }

                composable("BibliotecarioPanel") {
                    BibliotecarioPanelScreen(
                        onNavigateToCrear = { navHostController.navigate("books/form?libroId=null") },
                        onNavigateToDetalle = { id -> navHostController.navigate("LibroDetail/$id") },
                        onNavigateToEditar = { id -> navHostController.navigate("books/form?libroId=$id") },
                        onNavigateToCategorias = { navHostController.navigate("GestionCategorias") },
                        onNavigateToInventario = { navHostController.navigate("InventarioList") },
                        onNavigateToReservas = { navHostController.navigate("GestionReservas") },
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable("InventarioList") {
                    InventarioListScreen(
                        onNavigateToCrear = { navHostController.navigate("InventarioForm") },
                        onNavigateToDetalle = { id -> navHostController.navigate("InventarioDetail/$id") },
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable("InventarioForm") {
                    InventarioFormScreen(
                        onBack = { navHostController.popBackStack() },
                        onSave = { navHostController.popBackStack() }
                    )
                }

                composable(
                    route = "InventarioDetail/{inventarioId}",
                    arguments = listOf(navArgument("inventarioId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("inventarioId") ?: ""
                    InventarioDetailScreen(
                        inventarioId = id,
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable("GestionCategorias") {
                    GestionCategoriasScreen(
                        categorias = categoriasViewModel.categorias,
                        isUploading = categoriasViewModel.isUploading,
                        onBack = { navHostController.popBackStack() },
                        onSaveCategoria = { categoria -> categoriasViewModel.guardarCategoria(categoria) },
                        onDeleteCategoria = { id -> categoriasViewModel.eliminarCategoria(id) },
                        onViewAll = { navHostController.navigate("AllCategorias") },
                        onUploadImage = { ctx, uri, callback -> categoriasViewModel.uploadImage(ctx, uri, callback) }
                    )
                }

                composable("AllCategorias") {
                    AllCategoriasScreen(
                        categorias = categoriasViewModel.categorias,
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable("AdminPanel") {
                    AdminPanelScreen(
                        navController = navHostController, usuarios = adminViewModel.usuarios, bibliotecas = adminViewModel.bibliotecas,
                        librosCount = adminViewModel.librosCount, adminViewModel = adminViewModel,
                        onAddLibrary = { navHostController.navigate("CrearBiblioteca") }, onBack = { navHostController.popBackStack() },
                        onEditUser = { usuario -> navHostController.navigate("changeUserRole/${usuario.uid}") },
                        onEditLib = { biblioteca -> navHostController.navigate("editLibrary/${biblioteca.id}") },
                        onManageQuiz = { id -> navHostController.navigate("AdminQuizForm?id=${id ?: "null"}") }
                    )
                }


                composable("AdminQuizList") {
                    AdminQuizListScreen(
                        onNavigateBack = { navHostController.popBackStack() },
                        onNavigateToForm = { id ->
                            if (id == null) navHostController.navigate("AdminQuizForm?id=null")
                            else navHostController.navigate("AdminQuizForm?id=$id")
                        }
                    )
                }

                composable(
                    route = "AdminQuizForm?id={id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = "null" })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")
                    AdminQuizFormScreen(
                        preguntaId = if (id == "null") null else id,
                        onNavigateBack = { navHostController.popBackStack() }
                    )
                }

                composable("CrearBiblioteca") {
                    val localCtx = LocalContext.current
                    AddBibliotecaScreen(
                        onSuccess = {
                            navHostController.popBackStack()
                            Toast.makeText(localCtx, "¡Biblioteca guardada!", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable(
                    route = "editLibrary/{bibliotecaId}",
                    arguments = listOf(navArgument("bibliotecaId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val idSeleccionado = backStackEntry.arguments?.getString("bibliotecaId") ?: ""
                    EditBibliotecaScreen(
                        bibliotecaId = idSeleccionado,
                        onSuccess = { navHostController.popBackStack() },
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable(
                    route = "changeUserRole/{uid}",
                    arguments = listOf(navArgument("uid") { type = NavType.StringType })
                ) { backStackEntry ->
                    val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                    ChangeUserRoleScreen(
                        initialUid = uid,
                        adminViewModel = adminViewModel,
                        onBack = { navHostController.popBackStack() }
                    )
                }

                composable("Perfil") {
                    if (isLoadingGlobal) {
                        LoadingBox()
                    } else if (usuarioGlobal != null) {
                        PerfilScreen(
                            usuario = usuarioGlobal,
                            onNavigate = { ruta -> navHostController.navigate(ruta) },
                            onLogout = {
                                userViewModel.logout()
                                navHostController.navigate("InicioLogin") { popUpTo(0) { inclusive = true } }
                            }
                        )
                    } else {
                        LoadingBox()
                    }
                }

                composable("EditarPerfil") {
                    val profileViewModel: ProfileViewModel = viewModel()
                    val usuario = profileViewModel.usuario
                    if (usuario != null) {
                        EditarPerfilScreen(
                            usuario = usuario,
                            onBack = { navHostController.popBackStack() },
                            onSave = { nombre, telefono, fotoUrl ->
                                profileViewModel.actualizarPerfil(nombre, telefono, fotoUrl)
                                navHostController.popBackStack()
                            }
                        )
                    } else {
                        LoadingBox()
                    }
                }

                composable("Quiz") {
                    QuizScreen(onNavigateBack = { navHostController.popBackStack() })
                }

                composable("about") {
                    AboutScreen(onBack = { navHostController.popBackStack() })
                }

                composable(
                    route = "SolicitarReserva/{libro_id}",
                    arguments = listOf(navArgument("libro_id") { type = NavType.StringType })
                ) { backStackEntry ->
                    val libroId = backStackEntry.arguments?.getString("libro_id") ?: ""
                    SolicitarReservaScreen(
                        libroId = libroId,
                        onBackClick = { navHostController.popBackStack() },
                        onReservaConfirmada = {
                            navHostController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("reserva_ok", true)
                            navHostController.popBackStack()
                        }
                    )
                }

                composable("GestionReservas") {
                    GestionReservasScreen(onBack = { navHostController.popBackStack() })
                }
            }

            // Popup del Quiz (mejorado)
            if (showQuizPopup && currentRoute !in authRoutes) {
                AlertDialog(
                    onDismissRequest = { showQuizPopup = false },
                    title = {
                        Text(
                            "¡Aviso del Quiz!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A237E)
                        )
                    },
                    text = {
                        Text(
                            quizPopupMessage,
                            fontSize = 16.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showQuizPopup = false
                                navHostController.navigate("Quiz")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A237E)
                            )
                        ) {
                            Text("¡Ir a Jugar!", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showQuizPopup = false }) {
                            Text("Más tarde", color = Color.Gray)
                        }
                    }
                )
            }

            // Barra de navegación inferior
            if (shouldShowBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    BiblioNavigationBar(navController = navHostController, currentRoute = currentRoute)
                }
            }
        }
    }
}

@Composable
fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF1A237E))
    }
}

@Composable
fun BiblioNavigationBar(navController: NavHostController, currentRoute: String?) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .height(72.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                NavigationTabData("Home", Icons.Default.Home, stringResource(R.string.nav_inicio)),
                NavigationTabData("Mapa", Icons.Default.Map, stringResource(R.string.nav_mapa)),
                NavigationTabData("MisLibros", Icons.Default.LibraryBooks, stringResource(R.string.nav_libros)),
                NavigationTabData("Perfil", Icons.Default.Person, stringResource(R.string.nav_perfil))
            )

            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .width(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFF1A237E) else Color.Transparent)
                        .clickable {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color.White else Color(0xFF9E9E9E),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

data class NavigationTabData(val route: String, val icon: ImageVector, val label: String)