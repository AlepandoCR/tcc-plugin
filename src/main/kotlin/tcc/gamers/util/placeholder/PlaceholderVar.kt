package tcc.gamers.util.placeholder

import java.util.function.Supplier

/**
 * A typed wrapper for a variable that can be exposed to PlaceholderAPI.
 *
 *
 * This class allows storing primitive wrappers or objects, dynamically
 * resolving their string representation when requested by PAPI.
 *
 * @param <T> The type of the value held by this variable.
</T> */
class PlaceholderVar<T> {
    val identifier: String
    private var valueSupplier: Supplier<T?>

    /**
     * Constructs a placeholder variable with a dynamic value supplier.
     *
     * @param identifier    The PAPI identifier (e.g., "taxi_speed").
     * @param valueSupplier The supplier yielding the current value.
     */
    constructor(identifier: String, valueSupplier: Supplier<T?>) {
        this.identifier = identifier
        this.valueSupplier = valueSupplier
    }

    /**
     * Constructs a placeholder variable with a static initial value.
     *
     * @param identifier The PAPI identifier.
     * @param initial    The initial static value.
     */
    constructor(identifier: String, initial: T?) {
        this.identifier = identifier
        this.valueSupplier = Supplier { initial }
    }

    /**
     * Updates the supplier for this variable. Useful for changing static
     * values into dynamic ones at runtime.
     *
     * @param valueSupplier The new value supplier.
     */
    fun setSupplier(valueSupplier: Supplier<T?>) {
        this.valueSupplier = valueSupplier
    }

    var value: T?
        /**
         * Retrieves the current value directly.
         *
         * @return The typed value.
         */
        get() = valueSupplier.get()
        /**
         * Sets a new static value for this variable.
         *
         * @param value The new value.
         */
        set(value) {
            this.valueSupplier = Supplier { value }
        }

    val asString: String
        /**
         * Gets the string representation of the value for PlaceholderAPI.
         * Formats decimal numbers to prevent long floating-point strings if needed.
         *
         * @return The stringified value, or empty string if null.
         */
        get() {
            val `val` = this.value ?: return ""


            // Optional: Custom formatting for specific types (like rounding doubles)
            if (`val` is Double || `val` is Float) {
                return String.format("%.2f", (`val` as Number).toDouble())
            }

            return `val`.toString()
        }
}