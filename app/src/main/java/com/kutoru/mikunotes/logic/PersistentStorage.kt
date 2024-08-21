package com.kutoru.mikunotes.logic

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PersistentStorage(context: Context) {

    private val storage: SharedPreferences
    init {
        storage = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
    }

    var domain: String?
        get() {
            return storage.getString(STORAGE_KEY_DOMAIN, null)
        }
        set(value) {
            storage.edit {
                putString(STORAGE_KEY_DOMAIN, value)
            }
        }

    var port: Int?
        get() {
            return if (storage.contains(STORAGE_KEY_PORT)) {
                 storage.getInt(STORAGE_KEY_PORT, 0)
            } else {
                null
            }
        }
        set(value) {
            storage.edit {
                if (value != null) {
                    putInt(STORAGE_KEY_PORT, value)
                } else {
                    remove(STORAGE_KEY_PORT)
                }
            }
        }

    var isSecure: Boolean?
        get() {
            return if (storage.contains(STORAGE_KEY_IS_SECURE)) {
                storage.getBoolean(STORAGE_KEY_IS_SECURE, false)
            } else {
                null
            }
        }
        set(value) {
            storage.edit {
                if (value != null) {
                    putBoolean(STORAGE_KEY_IS_SECURE, value)
                } else {
                    remove(STORAGE_KEY_IS_SECURE)
                }
            }
        }

    var accessCookie: String?
        get() {
            return storage.getString(STORAGE_KEY_ACCESS_TOKEN, null)
        }
        set(value) {
            storage.edit {
                putString(STORAGE_KEY_ACCESS_TOKEN, value)
            }
        }

    var refreshCookie: String?
        get() {
            return storage.getString(STORAGE_KEY_REFRESH_TOKEN, null)
        }
        set(value) {
            storage.edit {
                putString(STORAGE_KEY_REFRESH_TOKEN, value)
            }
        }

    var email: String?
        get() {
            return storage.getString(STORAGE_KEY_EMAIL, null)
        }
        set(value) {
            storage.edit {
                putString(STORAGE_KEY_EMAIL, value)
            }
        }
}
