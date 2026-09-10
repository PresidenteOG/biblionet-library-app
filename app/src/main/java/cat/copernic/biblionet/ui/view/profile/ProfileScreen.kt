// Archivo: src/main/java/cat/copernic/biblionet/ui/view/profile/ProfileScreen.kt
package cat.copernic.biblionet.ui.view.profile

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.copernic.biblionet.R
import cat.copernic.biblionet.data.model.auth.Usuario
import cat.copernic.biblionet.data.model.auth.Role
import cat.copernic.biblionet.ui.viewmodel.user.ProfileViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import androidx.compose.ui.draw.shadow
import cat.copernic.biblionet.utils.GlobalState

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val points: Int,
    val avatarUrl: String,
    val isCurrentUser: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    usuario: Usuario,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var currentLang by remember { mutableStateOf(prefs.getString("app_lang", "es") ?: "es") }

    // SOLUCIÓN AL QUIZ EN TIEMPO REAL CON FIREBASE DIRECTO
    var isQuizCompleted by remember { mutableStateOf(false) }

    DisposableEffect(usuario.uid) {
        val db = FirebaseFirestore.getInstance()
        val registration = db.collection("usuarios").document(usuario.uid)
            .addSnapshotListener { doc, _ ->
                val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val lastDate = doc?.getString("ultima_partida_str") ?: ""
                isQuizCompleted = (today == lastDate)
            }
        onDispose { registration.remove() }
    }

    var leaderboardTab by remember { mutableStateOf("Semanal") }
    var isLoadingUsers by remember { mutableStateOf(true) }
    var allUsersData by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("usuarios").get()
            .addOnSuccessListener { snapshot ->
                allUsersData = snapshot.documents
                isLoadingUsers = false
            }
            .addOnFailureListener {
                isLoadingUsers = false
            }
    }

    val leaderboardData = remember(allUsersData, leaderboardTab) {
        if (allUsersData.isEmpty()) return@remember emptyList()

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val week = cal.get(Calendar.WEEK_OF_YEAR)

        val fieldToRead = when (leaderboardTab) {
            "Semanal" -> "puntos_semana_${year}_${week}"
            "Mensual" -> "puntos_mes_${year}_${month}"
            "Anual" -> "puntos_ano_${year}"
            else -> "puntos_totales"
        }

        allUsersData.map { doc ->
            val pts = doc.getLong(fieldToRead)?.toInt() ?: 0
            val nom = doc.getString("nombre") ?: "Anónimo"
            val pic = doc.getString("foto_url") ?: ""
            val uid = doc.id
            LeaderboardEntry(0, nom, pts, pic, isCurrentUser = (uid == usuario.uid))
        }
            .filter { it.points > 0 }
            .sortedByDescending { it.points }
            .take(10)
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(id = R.string.seleccionar_idioma), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LanguageOption("Castellano (Español)", "es", isSelected = currentLang == "es") { code ->
                        profileViewModel.actualizarIdioma(context, code)
                        currentLang = code
                        showLanguageDialog = false
                        reiniciarApp(context)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    LanguageOption("Català (Catalán)", "ca", isSelected = currentLang == "ca") { code ->
                        profileViewModel.actualizarIdioma(context, code)
                        currentLang = code
                        showLanguageDialog = false
                        reiniciarApp(context)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    LanguageOption("English (Inglés)", "en", isSelected = currentLang == "en") { code ->
                        profileViewModel.actualizarIdioma(context, code)
                        currentLang = code
                        showLanguageDialog = false
                        reiniciarApp(context)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.perfil), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF1A237E)) },
                actions = { IconButton(onClick = { showSettingsSheet = true }) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.configuracion), tint = Color(0xFF1A237E)) } },
                modifier = Modifier.shadow(elevation = 4.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)).verticalScroll(scrollState)) {

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    AsyncImage(
                        model = usuario.foto_url,
                        contentDescription = "Foto",
                        modifier = Modifier.size(120.dp).clip(CircleShape).border(4.dp, Color(0xFFFFB300), CircleShape).background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Surface(color = Color(0xFF1A237E), shape = RoundedCornerShape(12.dp), modifier = Modifier.offset(y = 8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.nivel_lector), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = usuario.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))

                val ahora = com.google.firebase.Timestamp.now()
                val sdfAnio = SimpleDateFormat("yyyy", Locale.getDefault())
                val sdfCompleto = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                if (usuario.bloqueado_hasta != null && usuario.bloqueado_hasta!! > ahora) {
                    val fechaDesbloqueo = sdfCompleto.format(usuario.bloqueado_hasta!!.toDate())
                    Text(
                        text = String.format(stringResource(R.string.cuenta_bloqueada), fechaDesbloqueo),
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val anio = sdfAnio.format(usuario.fecha_registro.toDate())
                    Text(
                        text = String.format(stringResource(R.string.miembro_desde), anio),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatBox(value = "${usuario.puntos_totales}", label = stringResource(R.string.puntos), modifier = Modifier.weight(1f))
                    StatBox(value = "${profileViewModel.librosLeidosCount}", label = stringResource(R.string.libros), modifier = Modifier.weight(1f))
                    StatBox(value = "${usuario.racha}", label = stringResource(R.string.racha), modifier = Modifier.weight(1f))
                }

                val quizBgColor = if (isQuizCompleted) Color(0xFF94A3B8) else Color(0xFF1A237E)
                val quizIconBgColor = if (isQuizCompleted) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                val quizAccentColor = if (isQuizCompleted) Color(0xFFE2E8F0) else Color(0xFFFFB300)

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate("Quiz") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = quizBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isQuizCompleted) 0.dp else 8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).background(quizIconBgColor, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = quizAccentColor, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(R.string.quiz_diario), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(if (isQuizCompleted) stringResource(R.string.modo_practica) else stringResource(R.string.juega_gana_puntos), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                        Box(modifier = Modifier.size(36.dp).background(quizAccentColor, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(if (isQuizCompleted) Icons.Default.Check else Icons.Default.PlayArrow, contentDescription = null, tint = if (isQuizCompleted) Color.White else Color(0xFF1A237E))
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.clasificacion), fontWeight = FontWeight.Bold, color = Color(0xFF1A237E), fontSize = 18.sp)

                        val tabsMap = mapOf(
                            "Semanal" to stringResource(R.string.semanal),
                            "Mensual" to stringResource(R.string.mensual),
                            "Anual" to stringResource(R.string.anual),
                            "Total" to stringResource(R.string.total)
                        )

                        Row(modifier = Modifier.background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp)).padding(2.dp)) {
                            listOf("Semanal", "Mensual", "Anual", "Total").forEach { internalKey ->
                                val isSelected = leaderboardTab == internalKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .clickable { leaderboardTab = internalKey }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tabsMap[internalKey] ?: internalKey,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = if (isSelected) Color(0xFF1A237E) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoadingUsers) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF1A237E))
                    } else if (leaderboardData.isEmpty()) {
                        Text(stringResource(R.string.nadie_puntos), color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp))
                    } else {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Column {
                                leaderboardData.forEachIndexed { index, item ->
                                    Row(modifier = Modifier.fillMaxWidth().background(if (item.isCurrentUser) Color(0xFFEFF6FF) else Color.Transparent).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "#${item.rank}", fontWeight = FontWeight.Bold, color = if (index < 3) Color(0xFFFFB300) else Color(0xFF94A3B8), modifier = Modifier.width(32.dp))
                                        AsyncImage(model = item.avatarUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray), contentScale = ContentScale.Crop)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                            if (item.isCurrentUser) Text(" ${stringResource(R.string.tu)}", fontSize = 10.sp, color = Color(0xFF3B82F6), modifier = Modifier.padding(start = 4.dp))
                                        }
                                        Text("${item.points}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF475569))
                                    }
                                    if (index < leaderboardData.size - 1) HorizontalDivider(color = Color(0xFFF8FAFC))
                                }
                            }
                        }
                    }
                }

                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(28.dp)) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.cerrar_sesion), color = Color(0xFFE11D48), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }, sheetState = sheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.configuracion), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E), modifier = Modifier.padding(bottom = 16.dp))

                SettingsItem(Icons.Default.Edit, stringResource(R.string.editar_perfil)) {
                    showSettingsSheet = false
                    onNavigate("EditarPerfil")
                }

                if (GlobalState.esAdmin()) {
                    SettingsItem(Icons.Default.AdminPanelSettings, stringResource(R.string.panel_administracion)) {
                        showSettingsSheet = false
                        onNavigate("AdminPanel")
                    }
                }
                if (GlobalState.esAdmin() || GlobalState.esBibliotecario()) {
                    SettingsItem(Icons.Default.Assignment,stringResource(R.string.panel_biblioteca)) {
                        showSettingsSheet = false
                        onNavigate("BibliotecarioPanel")
                    }
                }
                SettingsItem(Icons.Default.Language, stringResource(R.string.idioma)) {
                    showSettingsSheet = false
                    showLanguageDialog = true
                }
                SettingsItem(Icons.Default.Info, stringResource(R.string.about_biblionet)) {
                    showSettingsSheet = false
                    onNavigate("about")
                }
            }
        }
    }
}

fun reiniciarApp(context: Context) {
    val activity = context as? Activity
    Handler(Looper.getMainLooper()).postDelayed({
        activity?.recreate()
    }, 150)
}

@Composable
fun LanguageOption(label: String, code: String, isSelected: Boolean, onClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick(code) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Translate, contentDescription = null, tint = if (isSelected) Color(0xFF1A237E) else Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color(0xFF1A237E) else Color.Black)
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = Color(0xFF1A237E))
        }
    }
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        }
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = Color(0xFFF8F9FA), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1A237E))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
        }
    }
}