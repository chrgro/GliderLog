package no.ntnuf.gliderlog.fiken

import no.ntnuf.gliderlog.common.Contact
import org.json.JSONObject

class FikenContact : Contact() {
    companion object {
        private const val serialVersionUID: Long = 1L

        fun createContact(root: JSONObject): FikenContact {
            return FikenContact().apply {
                name = root.optString("name", "")
                hasAccount = true
                customerNumber = root.optInt("customerNumber", 0)
                supplierNumber = root.optInt("supplierNumber", 0)
                val emailValue = root.optString("email", "")
                email = if (emailValue.isBlank()) null else emailValue
            }
        }
    }
}


