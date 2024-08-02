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
            return storage.getString(DOMAIN_STORAGE_KEY, null)
        }
        set(value) {
            storage.edit {
                putString(DOMAIN_STORAGE_KEY, value)
            }
        }

    var port: Int?
        get() {
            return if (storage.contains(PORT_STORAGE_KEY)) {
                 storage.getInt(PORT_STORAGE_KEY, 0)
            } else {
                null
            }
        }
        set(value) {
            storage.edit {
                if (value != null) {
                    putInt(PORT_STORAGE_KEY, value)
                } else {
                    remove(PORT_STORAGE_KEY)
                }
            }
        }

    var isSecure: Boolean?
        get() {
            return if (storage.contains(IS_SECURE_STORAGE_KEY)) {
                storage.getBoolean(IS_SECURE_STORAGE_KEY, false)
            } else {
                null
            }
        }
        set(value) {
            storage.edit {
                if (value != null) {
                    putBoolean(IS_SECURE_STORAGE_KEY, value)
                } else {
                    remove(IS_SECURE_STORAGE_KEY)
                }
            }
        }

    var accessCookie: String?
        get() {
            return storage.getString(ACCESS_TOKEN_STORAGE_KEY, null)
        }
        set(value) {
            storage.edit {
                putString(ACCESS_TOKEN_STORAGE_KEY, value)
            }
        }

    var refreshCookie: String?
        get() {
            return storage.getString(REFRESH_TOKEN_STORAGE_KEY, null)
        }
        set(value) {
            storage.edit {
                putString(REFRESH_TOKEN_STORAGE_KEY, value)
            }
        }
}
