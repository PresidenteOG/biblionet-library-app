package com.biblionet.data.remote.cloudinary

import android.content.Context
import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

/**
 * Servicio encargado de gestionar la subida de imágenes a Cloudinary.
 *
 * Esta clase utiliza la API REST de Cloudinary junto con OkHttp
 * para subir imágenes desde la aplicación y obtener la URL pública
 * segura de la imagen almacenada en la nube.
 */
class CloudinaryService {

    /** Nombre del cloud configurado en Cloudinary. */
    private val cloudName = "die6u09pk"

    /** Upload preset configurado en Cloudinary para permitir subidas sin firma (unsigned). */
    private val uploadPreset = "perfil_unsigned"

    /** Cliente HTTP reutilizable para realizar las peticiones a la API de Cloudinary. */
    private val client = OkHttpClient()

    /**
     * Sube una imagen a Cloudinary.
     *
     * El método convierte la imagen referenciada por el `Uri` en bytes,
     * crea una petición multipart y la envía a la API de Cloudinary.
     * Si la subida es correcta, se devuelve la URL segura (`secure_url`)
     * de la imagen alojada en Cloudinary.
     *
     * @param context Contexto necesario para acceder al `ContentResolver`.
     * @param imageUri URI de la imagen que se desea subir.
     * @param onResult Callback que devuelve:
     * - la URL segura de la imagen si la subida es exitosa
     * - `null` si ocurre algún error durante el proceso.
     */
    fun uploadImage(
        context: Context,
        imageUri: Uri,
        onResult: (String?) -> Unit
    ) {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        if (inputStream == null) {
            onResult(null)
            return
        }

        val bytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "profile.jpg",
                RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
            )
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            /**
             * Se ejecuta cuando ocurre un error de red durante la petición.
             */
            override fun onFailure(call: Call, e: IOException) {
                onResult(null)
            }

            /**
             * Se ejecuta cuando se recibe una respuesta del servidor.
             * Si la respuesta es válida, se extrae la URL segura de la imagen.
             */
            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        onResult(null)
                        return
                    }

                    val bodyString = resp.body?.string()
                    if (bodyString != null) {
                        try {
                            val json = JSONObject(bodyString)
                            val imageUrl = json.getString("secure_url")
                            onResult(imageUrl)
                        } catch (e: Exception) {
                            onResult(null)
                        }
                    } else {
                        onResult(null)
                    }
                }
            }
        })
    }
}