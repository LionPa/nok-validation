package io.lionpa.nok.validation

class ValidationResultList {

    var errors: ArrayList<String>? = null

    constructor()

    fun add(message: String) {
        if (errors == null) {
            errors = ArrayList(1)
        }
        errors!!.add(message)
    }

    fun list() : List<String> {
        if (errors == null) return emptyList()

        return errors!!
    }
}