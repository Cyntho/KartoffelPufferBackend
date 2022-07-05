package org.cyntho.fh.models

data class Dish(val dishId: Int,
                val active: Boolean,
                val iconHash: String,
                val name: String,
                val allergies: MutableList<AllergyWrapper>,
                val description: String)

