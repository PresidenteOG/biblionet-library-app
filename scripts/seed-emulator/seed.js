// Seeds the local Firebase Auth + Firestore emulators with demo data for
// BiblioNET (books, branches, inventory, users, a loan). Emulator-only —
// never touches a real Firebase project. Run `firebase emulators:start`
// first, then `node seed.js` from this folder.

process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
process.env.FIREBASE_AUTH_EMULATOR_HOST = "127.0.0.1:9099";

const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");

initializeApp({ projectId: "biblionet-emulator-demo" });

const auth = getAuth();
const db = getFirestore();

const DEMO_USERS = [
  { email: "lector@biblionet.demo", password: "demo1234", nombre: "Ana Lector", rol: "READER" },
  { email: "bibliotecario@biblionet.demo", password: "demo1234", nombre: "Marc Bibliotecari", rol: "LIBRARIAN" },
  { email: "admin@biblionet.demo", password: "demo1234", nombre: "Núria Admin", rol: "ADMIN" },
];

const BIBLIOTECAS = [
  {
    id: "biblio-central",
    nombre: "Biblioteca Central",
    direccion: "Carrer Major 12, Barcelona",
    telefono: "931234567",
    accesibilidad: true,
    email: "central@biblionet.demo",
    descripcion: "La sede principal de la red BiblioNET.",
    foto_url: "",
    latitud: 41.3874,
    longitud: 2.1686,
    horario: { lunes: ["09:00-20:00"], sabado: ["10:00-14:00"] },
  },
  {
    id: "biblio-gracia",
    nombre: "Biblioteca de Gràcia",
    direccion: "Plaça de la Vila 3, Barcelona",
    telefono: "931234568",
    accesibilidad: false,
    email: "gracia@biblionet.demo",
    descripcion: "Sucursal de barrio con sala infantil.",
    foto_url: "",
    latitud: 41.4036,
    longitud: 2.1568,
    horario: { lunes: ["10:00-19:00"] },
  },
];

const CATEGORIAS = [
  { categoriaId: "cat-fantasia", nombre: "Fantasía", descripcion: "Mundos imaginarios y magia.", imagenUrl: "" },
  { categoriaId: "cat-ciencia-ficcion", nombre: "Ciencia Ficción", descripcion: "Futuro, tecnología y especulación.", imagenUrl: "" },
  { categoriaId: "cat-historia", nombre: "Historia", descripcion: "Hechos y personajes del pasado.", imagenUrl: "" },
];

const LIBROS = [
  {
    libro_id: "libro-el-nombre-del-viento",
    titulo: "El nombre del viento",
    autor: "Patrick Rothfuss",
    isbn: "9788401352836",
    descripcion_corta: "La historia de Kvothe, contada por él mismo.",
    descripcion_larga: "Un músico y mago legendario narra su propia leyenda, desde su infancia en una compañía itinerante hasta sus años en la Universidad.",
    imagen_url: "",
    creado_por: "seed-script",
    categoria_id: "cat-fantasia",
    idioma: "es",
    calificacion: "4.7",
    estrellas: 4.7,
  },
  {
    libro_id: "libro-fundacion",
    titulo: "Fundación",
    autor: "Isaac Asimov",
    isbn: "9788445071638",
    descripcion_corta: "El imperio galáctico se desmorona.",
    descripcion_larga: "Hari Seldon predice la caída del Imperio Galáctico y funda la Fundación para acortar la era de barbarie que seguirá.",
    imagen_url: "",
    creado_por: "seed-script",
    categoria_id: "cat-ciencia-ficcion",
    idioma: "es",
    calificacion: "4.6",
    estrellas: 4.6,
  },
  {
    libro_id: "libro-sapiens",
    titulo: "Sapiens: De animales a dioses",
    autor: "Yuval Noah Harari",
    isbn: "9788499926223",
    descripcion_corta: "Una breve historia de la humanidad.",
    descripcion_larga: "Un recorrido por la evolución humana, desde la revolución cognitiva hasta la revolución científica.",
    imagen_url: "",
    creado_por: "seed-script",
    categoria_id: "cat-historia",
    idioma: "es",
    calificacion: "4.5",
    estrellas: 4.5,
  },
];

const INVENTARIO = [
  { id: "inv-1", libro_id: "libro-el-nombre-del-viento", biblioteca_id: "biblio-central", numero_copias: 4, stock_disponible: 2, pasilloEstanteria: "A3", disponible: true },
  { id: "inv-2", libro_id: "libro-fundacion", biblioteca_id: "biblio-central", numero_copias: 3, stock_disponible: 3, pasilloEstanteria: "B1", disponible: true },
  { id: "inv-3", libro_id: "libro-sapiens", biblioteca_id: "biblio-gracia", numero_copias: 2, stock_disponible: 0, pasilloEstanteria: "C2", disponible: false },
  { id: "inv-4", libro_id: "libro-el-nombre-del-viento", biblioteca_id: "biblio-gracia", numero_copias: 2, stock_disponible: 1, pasilloEstanteria: "A1", disponible: true },
];

async function ensureAuthUser(u) {
  try {
    const existing = await auth.getUserByEmail(u.email);
    return existing.uid;
  } catch (e) {
    const created = await auth.createUser({
      email: u.email,
      password: u.password,
      displayName: u.nombre,
    });
    return created.uid;
  }
}

async function main() {
  console.log("Seeding BiblioNET emulator data...");

  const uids = {};
  for (const u of DEMO_USERS) {
    const uid = await ensureAuthUser(u);
    uids[u.rol] = uid;
    await db.collection("usuarios").doc(uid).set({
      uid,
      nombre: u.nombre,
      email: u.email,
      rol: u.rol,
      telefono: "600000000",
      biblioteca_id: "biblio-central",
      foto_url: "",
      idioma: "es",
      fecha_registro: Timestamp.now(),
      favoritos: [],
      ultima_partida: null,
      bloqueado_hasta: null,
      puntos_totales: u.rol === "READER" ? 120 : 0,
      forceLogout: false,
      libros_leidos: u.rol === "READER" ? 6 : 0,
      racha: u.rol === "READER" ? 3 : 0,
    });
    console.log(`  auth+usuarios: ${u.email} (${u.rol}) -> ${uid}`);
  }

  for (const b of BIBLIOTECAS) {
    const { id, ...data } = b;
    await db.collection("bibliotecas").doc(id).set(data);
  }
  console.log(`  bibliotecas: ${BIBLIOTECAS.length}`);

  for (const c of CATEGORIAS) {
    await db.collection("categorias").doc(c.categoriaId).set(c);
  }
  console.log(`  categorias: ${CATEGORIAS.length}`);

  for (const l of LIBROS) {
    await db.collection("libros").doc(l.libro_id).set({
      ...l,
      fecha_creacion: Timestamp.now(),
    });
  }
  console.log(`  libros: ${LIBROS.length}`);

  for (const inv of INVENTARIO) {
    const { id, ...data } = inv;
    await db.collection("inventario").doc(id).set(data);
  }
  console.log(`  inventario: ${INVENTARIO.length}`);

  // One active loan and one pending reservation, so the loan screen has content.
  await db.collection("prestamos").doc("prestamo-demo-activo").set({
    prestamoid: "prestamo-demo-activo",
    usuario_id: uids.READER,
    libro_id: "libro-el-nombre-del-viento",
    isbn: "9788401352836",
    fecha_salida: Date.now() - 3 * 24 * 60 * 60 * 1000,
    biblioteca_id: "biblio-central",
    fecha_devolucion: Date.now() + 11 * 24 * 60 * 60 * 1000,
    fecha_entregado: null,
    estado: "ACEPTADO",
  });
  await db.collection("prestamos").doc("prestamo-demo-pendiente").set({
    prestamoid: "prestamo-demo-pendiente",
    usuario_id: uids.READER,
    libro_id: "libro-fundacion",
    isbn: "9788445071638",
    fecha_salida: Date.now(),
    biblioteca_id: "biblio-central",
    fecha_devolucion: 0,
    fecha_entregado: null,
    estado: "PENDIENTE",
  });
  console.log("  prestamos: 2 (1 activo, 1 pendiente)");

  console.log("Done. Demo logins (password: demo1234):");
  for (const u of DEMO_USERS) console.log(`  ${u.rol}: ${u.email}`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
