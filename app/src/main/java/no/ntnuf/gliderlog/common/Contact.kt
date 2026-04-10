package no.ntnuf.gliderlog.common

import java.io.Serializable

open class Contact : Serializable {
    companion object {
        private const val serialVersionUID: Long = 3L
    }

    var name: String? = null
    var self: String? = null
    var hasAccount: Boolean = false
    var customerNumber: Int = 0
    var supplierNumber: Int = 0
    var email: String? = null

    override fun toString(): String {
        return name ?: ""
    }
}

