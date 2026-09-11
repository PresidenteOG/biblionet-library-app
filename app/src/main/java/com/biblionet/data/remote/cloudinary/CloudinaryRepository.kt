package com.biblionet.data.remote.cloudinary

import android.content.Context
import android.net.Uri

/**
 * Repositorio encargado de gestionar las operaciones relacionadas con Cloudinary.
 *
 * Esta clase actúa como intermediario entre la capa de datos y el servicio
 * que realiza la subida de imágenes a Cloudinary.
 *
 * @property service Servicio encargado de realizar las operaciones de subida de imágenes.
 */
class CloudinaryRepository(
    private val service: CloudinaryService = CloudinaryService()
) {

    /**
     * Sube una imagen de perfil a Cloudinary.
     *
     * @param context Contexto de la aplicación necesario para acceder al contenido de la imagen.
     * @param imageUri URI de la imagen que se desea subir.
     * @param onResult Callback que devuelve la URL de la imagen subida si la operación tiene éxito,
     * o `null` en caso de error.
     */
    fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        onResult: (String?) -> Unit
    ) {
        service.uploadImage(context, imageUri, onResult)
    }
}