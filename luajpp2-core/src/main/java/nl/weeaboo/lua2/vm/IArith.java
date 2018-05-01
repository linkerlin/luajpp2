package nl.weeaboo.lua2.vm;

interface IArith {

    /**
     * Unary minus: return negative value {@code (-this)} as defined by lua unary minus operator
     *
     * @return boolean inverse as {@link LuaBoolean} if boolean or nil, numeric inverse as {@LuaNumber} if
     *         numeric, or metatag processing result if {@link LuaConstants#META_UNM} metatag is defined
     * @throws LuaError if {@code this} is not a table or string, and has no {@link LuaConstants#META_UNM} metatag
     */
    public LuaValue neg();

    /**
     * Add: Perform numeric add operation with another value including metatag processing.
     * <p>
     * Each operand must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     *
     * @param rhs The right-hand-side value to perform the add with
     * @return value of {@code (this + rhs)} if both are numeric, or {@link LuaValue} if metatag processing
     *         occurs
     * @throws LuaError if either operand is not a number or string convertible to number, and neither has the
     *         {@link LuaConstants#META_ADD} metatag defined
     */
    LuaValue add(LuaValue rhs);

    /**
     * Add: Perform numeric add operation with another value of double type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #add(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the add with
     * @return value of {@code (this + rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #add(LuaValue)
     */
    LuaValue add(double rhs);

    /**
     * Add: Perform numeric add operation with another value of int type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #add(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the add with
     * @return value of {@code (this + rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #add(LuaValue)
     */
    LuaValue add(int rhs);

    /**
     * Subtract: Perform numeric subtract operation with another value of unknown type, including metatag
     * processing.
     * <p>
     * Each operand must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     *
     * @param rhs The right-hand-side value to perform the subtract with
     * @return value of {@code (this - rhs)} if both are numeric, or {@link LuaValue} if metatag processing
     *         occurs
     * @throws LuaError if either operand is not a number or string convertible to number, and neither has the
     *         {@link LuaConstants#META_SUB} metatag defined
     */
    LuaValue sub(LuaValue rhs);

    /**
     * Subtract: Perform numeric subtract operation with another value of double type without metatag
     * processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #sub(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the subtract with
     * @return value of {@code (this - rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #sub(LuaValue)
     */
    LuaValue sub(double rhs);

    /**
     * Subtract: Perform numeric subtract operation with another value of int type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #sub(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the subtract with
     * @return value of {@code (this - rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #sub(LuaValue)
     */
    LuaValue sub(int rhs);

    /**
     * Reverse-subtract: Perform numeric subtract operation from an int value without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #sub(LuaValue)} must be used
     *
     * @param lhs The left-hand-side value from which to perform the subtraction
     * @return value of {@code (lhs - this)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #sub(LuaValue)
     * @see #sub(double)
     * @see #sub(int)
     */
    LuaValue subFrom(double lhs);

    /**
     * Reverse-subtract: Perform numeric subtract operation from a double value without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #sub(LuaValue)} must be used
     *
     * @param lhs The left-hand-side value from which to perform the subtraction
     * @return value of {@code (lhs - this)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #sub(LuaValue)
     * @see #sub(double)
     * @see #sub(int)
     */
    LuaValue subFrom(int lhs);

    /**
     * Multiply: Perform numeric multiply operation with another value of unknown type, including metatag
     * processing.
     * <p>
     * Each operand must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     *
     * @param rhs The right-hand-side value to perform the multiply with
     * @return value of {@code (this * rhs)} if both are numeric, or {@link LuaValue} if metatag processing
     *         occurs
     * @throws LuaError if either operand is not a number or string convertible to number, and neither has the
     *         {@link LuaConstants#META_MUL} metatag defined
     */
    LuaValue mul(LuaValue rhs);

    /**
     * Multiply: Perform numeric multiply operation with another value of double type without metatag
     * processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #mul(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the multiply with
     * @return value of {@code (this * rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #mul(LuaValue)
     */
    LuaValue mul(double rhs);

    /**
     * Multiply: Perform numeric multiply operation with another value of int type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #mul(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the multiply with
     * @return value of {@code (this * rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #mul(LuaValue)
     */
    LuaValue mul(int rhs);

    /**
     * Raise to power: Raise this value to a power including metatag processing.
     * <p>
     * Each operand must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     *
     * @param rhs The power to raise this value to
     * @return value of {@code (this ^ rhs)} if both are numeric, or {@link LuaValue} if metatag processing
     *         occurs
     * @throws LuaError if either operand is not a number or string convertible to number, and neither has the
     *         {@link LuaConstants#META_POW} metatag defined
     */
    LuaValue pow(LuaValue rhs);

    /**
     * Raise to power: Raise this value to a power of double type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #pow(LuaValue)} must be used
     *
     * @param rhs The power to raise this value to
     * @return value of {@code (this ^ rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #pow(LuaValue)
     */
    LuaValue pow(double rhs);

    /**
     * Raise to power: Raise this value to a power of int type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #pow(LuaValue)} must be used
     *
     * @param rhs The power to raise this value to
     * @return value of {@code (this ^ rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #pow(LuaValue)
     */
    LuaValue pow(int rhs);

    /**
     * Reverse-raise to power: Raise another value of double type to this power without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #pow(LuaValue)} must be used
     *
     * @param lhs The left-hand-side value which will be raised to this power
     * @return value of {@code (lhs ^ this)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #pow(LuaValue)
     * @see #pow(double)
     * @see #pow(int)
     */
    LuaValue powWith(double lhs);

    /**
     * Reverse-raise to power: Raise another value of double type to this power without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #pow(LuaValue)} must be used
     *
     * @param lhs The left-hand-side value which will be raised to this power
     * @return value of {@code (lhs ^ this)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #pow(LuaValue)
     * @see #pow(double)
     * @see #pow(int)
     */
    LuaValue powWith(int lhs);

    /**
     * Divide: Perform numeric divide operation by another value of unknown type, including metatag
     * processing.
     * <p>
     * Each operand must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     *
     * @param rhs The right-hand-side value to perform the divulo with
     * @return value of {@code (this / rhs)} if both are numeric, or {@link LuaValue} if metatag processing
     *         occurs
     * @throws LuaError if either operand is not a number or string convertible to number, and neither has the
     *         {@link LuaConstants#META_DIV} metatag defined
     */
    LuaValue div(LuaValue rhs);

    /**
     * Divide: Perform numeric divide operation by another value of double type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #div(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the divulo with
     * @return value of {@code (this / rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #div(LuaValue)
     */
    LuaValue div(double rhs);

    /**
     * Divide: Perform numeric divide operation by another value of int type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #div(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the divulo with
     * @return value of {@code (this / rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #div(LuaValue)
     */
    LuaValue div(int rhs);

    /**
     * Reverse-divide: Perform numeric divide operation into another value without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #div(LuaValue)} must be used
     *
     * @param lhs The left-hand-side value which will be divided by this
     * @return value of {@code (lhs / this)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #div(LuaValue)
     * @see #div(double)
     * @see #div(int)
     */
    LuaValue divInto(double lhs);

    /**
     * Modulo: Perform numeric modulo operation with another value of unknown type, including metatag
     * processing.
     * <p>
     * Each operand must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     *
     * @param rhs The right-hand-side value to perform the modulo with
     * @return value of {@code (this % rhs)} if both are numeric, or {@link LuaValue} if metatag processing
     *         occurs
     * @throws LuaError if either operand is not a number or string convertible to number, and neither has the
     *         {@link LuaConstants#META_MOD} metatag defined
     */
    LuaValue mod(LuaValue rhs);

    /**
     * Modulo: Perform numeric modulo operation with another value of double type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #mod(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the modulo with
     * @return value of {@code (this % rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #mod(LuaValue)
     */
    LuaValue mod(double rhs);

    /**
     * Modulo: Perform numeric modulo operation with another value of int type without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #mod(LuaValue)} must be used
     *
     * @param rhs The right-hand-side value to perform the modulo with
     * @return value of {@code (this % rhs)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #mod(LuaValue)
     */
    LuaValue mod(int rhs);

    /**
     * Reverse-modulo: Perform numeric modulo operation from another value without metatag processing
     * <p>
     * {@code this} must derive from {@link LuaNumber} or derive from {@link LuaString} and be convertible to
     * a number
     * <p>
     * For metatag processing {@link #mod(LuaValue)} must be used
     *
     * @param lhs The left-hand-side value which will be modulo'ed by this
     * @return value of {@code (lhs % this)} if this is numeric
     * @throws LuaError if {@code this} is not a number or string convertible to number
     * @see #mod(LuaValue)
     * @see #mod(double)
     * @see #mod(int)
     */
    LuaValue modFrom(double lhs);

}