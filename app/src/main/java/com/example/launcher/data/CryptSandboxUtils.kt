package com.example.launcher.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

object CryptSandboxUtils {
    fun getEncryptedFile(context: Context, file: File): EncryptedFile {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    fun writeTextEncrypted(context: Context, file: File, text: String) {
        try {
            if (file.exists()) {
                file.delete()
            }
            val encryptedFile = getEncryptedFile(context, file)
            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(text.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to plain write in case of Keystore failure on custom ROMs / emulators
            try {
                file.writeText(text)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun readTextEncrypted(context: Context, file: File): String {
        if (!file.exists()) return ""
        try {
            val encryptedFile = getEncryptedFile(context, file)
            encryptedFile.openFileInput().use { inputStream ->
                return String(inputStream.readBytes(), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to plain read if crypto fails or it was written in plain text
            try {
                return file.readText()
            } catch (ex: Exception) {
                return ""
            }
        }
    }
}
