package com.ldm.ciberroyale

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.IOException

object CertificateManager {
    fun saveCertificateToDownloads(context: Context): Boolean {
        val inputStream = context.resources
            .openRawResource(R.raw.certificado_ciberroyale)

        // 1) Elegimos la “carpeta” de MediaStore según API:
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            // en APIs antiguas usamos la colección genérica
            MediaStore.Files.getContentUri("external")
        }

        // 2) Preparamos los metadatos:
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Certificado-CiberRoyale.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Environment.DIRECTORY_DOWNLOADS == "Download"
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        // 3) Insertamos y copiamos el flujo:
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)
            ?: run {
                Toast.makeText(context, "Error al crear el fichero.", Toast.LENGTH_SHORT).show()
                return false
            }

        return try {
            resolver.openOutputStream(uri).use { os ->
                inputStream.copyTo(os!!)
            }
            Toast.makeText(context,
                "¡Certificado guardado en Descargas!",
                Toast.LENGTH_LONG).show()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context,
                "Error al guardar el certificado.",
                Toast.LENGTH_SHORT).show()
            false
        }
    }
}
