package org.luaj.vm2;

public interface IComparable {

    /**
     * Equals: Perform equality comparison with another value including metatag processing using
     * {@link LuaConstants#EQ}.
     *
     * @param val The value to compare with.
     * @return {@link LuaBoolean#TRUE} if values are comparable and {@code (this == rhs)},
     *         {@link LuaBoolean#FALSE} if comparable but not equal, {@link LuaValue} if metatag processing
     *         occurs.
     * @see #eq_b(LuaValue)
     */
    LuaValue eq(LuaValue val);

    /**
     * Equals: Perform equality comparison with another value including metatag processing using
     * {@link LuaConstants#EQ}, and return java boolean
     *
     * @param val The value to compare with.
     * @return true if values are comparable and {@code (this == rhs)}, false if comparable but not equal,
     *         result converted to java boolean if metatag processing occurs.
     * @see #eq(LuaValue)
     */
    boolean eq_b(LuaValue val);

    /**
     * Notquals: Perform inequality comparison with another value including metatag processing using
     * {@link LuaConstants#EQ}.
     *
     * @param val The value to compare with.
     * @return {@link LuaBoolean#TRUE} if values are comparable and {@code (this != rhs)},
     *         {@link LuaBoolean#FALSE} if comparable but equal, inverse of {@link LuaValue} converted to
     *         {@link LuaBoolean} if metatag processing occurs.
     * @see #eq(LuaValue)
     */
    LuaValue neq(LuaValue val);

    /**
     * Notquals: Perform inequality comparison with another value including metatag processing using
     * {@link LuaConstants#EQ}.
     *
     * @param val The value to compare with.
     * @return true if values are comparable and {@code (this != rhs)}, false if comparable but equal, inverse
     *         of result converted to boolean if metatag processing occurs.
     * @see #eq_b(LuaValue)
     */
    boolean neq_b(LuaValue val);

    /**
     * Less than: Perform numeric or string comparison with another value of unknown type, including metatag
     * processing, and returning {@link LuaValue}.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this < rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LT} metatag is defined.
     * @see #gteq_b(LuaValue)
     */
    LuaValue lt(LuaValue rhs);

    /**
     * Less than: Perform numeric comparison with another value of double type, including metatag processing,
     * and returning {@link LuaValue}.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this < rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if this is not a number and no {@link LuaConstants#LT} metatag is defined.
     * @see #gteq_b(double)
     */
    LuaValue lt(double rhs);

    /**
     * Less than: Perform numeric comparison with another value of int type, including metatag processing, and
     * returning {@link LuaValue}.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this < rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if this is not a number and no {@link LuaConstants#LT} metatag is defined.
     * @see #gteq_b(int)
     */
    LuaValue lt(int rhs);

    /**
     * Less than: Perform numeric or string comparison with another value of unknown type, including metatag
     * processing, and returning java boolean.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this < rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LT} metatag is defined.
     * @see #gteq(LuaValue)
     */
    boolean lt_b(LuaValue rhs);

    /**
     * Less than: Perform numeric comparison with another value of int type, including metatag processing, and
     * returning java boolean.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this < rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if this is not a number and no {@link LuaConstants#LT} metatag is defined.
     * @see #gteq(int)
     */
    boolean lt_b(int rhs);

    /**
     * Less than: Perform numeric or string comparison with another value of unknown type, including metatag
     * processing, and returning java boolean.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this < rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LT} metatag is defined.
     * @see #gteq(LuaValue)
     */
    boolean lt_b(double rhs);

    /**
     * Less than or equals: Perform numeric or string comparison with another value of unknown type, including
     * metatag processing, and returning {@link LuaValue}.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this <= rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LE} metatag is defined.
     * @see #gteq_b(LuaValue)
     */
    LuaValue lteq(LuaValue rhs);

    /**
     * Less than or equals: Perform numeric comparison with another value of double type, including metatag
     * processing, and returning {@link LuaValue} .
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this <= rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if this is not a number and no {@link LuaConstants#LE} metatag is defined.
     * @see #gteq_b(double)
     */
    LuaValue lteq(double rhs);

    /**
     * Less than or equals: Perform numeric comparison with another value of int type, including metatag
     * processing, and returning {@link LuaValue}.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this <= rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if this is not a number and no {@link LuaConstants#LE} metatag is defined.
     * @see #gteq_b(int)
     */
    LuaValue lteq(int rhs);

    /**
     * Less than or equals: Perform numeric or string comparison with another value of unknown type, including
     * metatag processing, and returning java boolean.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this <= rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LE} metatag is defined.
     * @see #gteq(LuaValue)
     */
    boolean lteq_b(LuaValue rhs);

    /**
     * Less than or equals: Perform numeric comparison with another value of int type, including metatag
     * processing, and returning java boolean.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this <= rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if this is not a number and no {@link LuaConstants#LE} metatag is defined.
     * @see #gteq(int)
     */
    boolean lteq_b(int rhs);

    /**
     * Less than or equals: Perform numeric comparison with another value of double type, including metatag
     * processing, and returning java boolean.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this <= rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if this is not a number and no {@link LuaConstants#LE} metatag is defined.
     * @see #gteq(double)
     */
    boolean lteq_b(double rhs);

    /**
     * Greater than: Perform numeric or string comparison with another value of unknown type, including
     * metatag processing, and returning {@link LuaValue}.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this > rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LE} metatag is defined.
     * @see #gteq_b(LuaValue)
     */
    LuaValue gt(LuaValue rhs);

    /**
     * Greater than: Perform numeric comparison with another value of double type, including metatag
     * processing, and returning {@link LuaValue}.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this > rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if this is not a number and no {@link LuaConstants#LE} metatag is defined.
     * @see #gteq_b(double)
     */
    LuaValue gt(double rhs);

    /**
     * Greater than: Perform numeric comparison with another value of int type, including metatag processing,
     * and returning {@link LuaValue}.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this > rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if this is not a number and no {@link LuaConstants#LE} metatag is defined.
     * @see #gteq_b(int)
     */
    LuaValue gt(int rhs);

    /**
     * Greater than: Perform numeric or string comparison with another value of unknown type, including
     * metatag processing, and returning java boolean.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this > rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LE} metatag is defined.
     * @see #gteq(LuaValue)
     */
    boolean gt_b(LuaValue rhs);

    /**
     * Greater than: Perform numeric comparison with another value of int type, including metatag processing,
     * and returning java boolean.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this > rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if this is not a number and no {@link LuaConstants#LE} metatag is defined.
     * @see #gteq(int)
     */
    boolean gt_b(int rhs);

    /**
     * Greater than: Perform numeric or string comparison with another value of unknown type, including
     * metatag processing, and returning java boolean.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this > rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LE} metatag is defined.
     * @see #gteq(LuaValue)
     */
    boolean gt_b(double rhs);

    /**
     * Greater than or equals: Perform numeric or string comparison with another value of unknown type,
     * including metatag processing, and returning {@link LuaValue}.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this >= rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LT} metatag is defined.
     * @see #gteq_b(LuaValue)
     */
    LuaValue gteq(LuaValue rhs);

    /**
     * Greater than or equals: Perform numeric comparison with another value of double type, including metatag
     * processing, and returning {@link LuaValue} .
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this >= rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if this is not a number and no {@link LuaConstants#LT} metatag is defined.
     * @see #gteq_b(double)
     */
    LuaValue gteq(double rhs);

    /**
     * Greater than or equals: Perform numeric comparison with another value of int type, including metatag
     * processing, and returning {@link LuaValue}.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return {@link LuaBoolean#TRUE} if {@code (this >= rhs)}, {@link LuaBoolean#FALSE} if not, or
     *         {@link LuaValue} if metatag processing occurs
     * @throws LuaError if this is not a number and no {@link LuaConstants#LT} metatag is defined.
     * @see #gteq_b(int)
     */
    LuaValue gteq(int rhs);

    /**
     * Greater than or equals: Perform numeric or string comparison with another value of unknown type,
     * including metatag processing, and returning java boolean.
     * <p>
     * To be comparable, both operands must derive from {@link LuaString} or both must derive from
     * {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this >= rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if either both operands are not a strings or both are not numbers and no
     *         {@link LuaConstants#LT} metatag is defined.
     * @see #gteq(LuaValue)
     */
    boolean gteq_b(LuaValue rhs);

    /**
     * Greater than or equals: Perform numeric comparison with another value of int type, including metatag
     * processing, and returning java boolean.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this >= rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if this is not a number and no {@link LuaConstants#LT} metatag is defined.
     * @see #gteq(int)
     */
    boolean gteq_b(int rhs);

    /**
     * Greater than or equals: Perform numeric comparison with another value of double type, including metatag
     * processing, and returning java boolean.
     * <p>
     * To be comparable, this must derive from {@link LuaNumber}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @return true if {@code (this >= rhs)}, false if not, and boolean interpreation of result if metatag
     *         processing occurs.
     * @throws LuaError if this is not a number and no {@link LuaConstants#LT} metatag is defined.
     * @see #gteq(double)
     */
    boolean gteq_b(double rhs);

    /**
     * Perform string comparison with another value of any type using string comparison based on byte values.
     * <p>
     * Only strings can be compared, meaning each operand must derive from {@link LuaString}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @returns int < 0 for {@code (this < rhs)}, int > 0 for {@code (this > rhs)}, or 0 when same string.
     * @throws LuaError if either operand is not a string
     */
    int strcmp(LuaValue rhs);

    /**
     * Perform string comparison with another value known to be a {@link LuaString} using string comparison
     * based on byte values.
     * <p>
     * Only strings can be compared, meaning each operand must derive from {@link LuaString}.
     *
     * @param rhs The right-hand-side value to perform the comparison with
     * @returns int < 0 for {@code (this < rhs)}, int > 0 for {@code (this > rhs)}, or 0 when same string.
     * @throws LuaError if this is not a string
     */
    int strcmp(LuaString rhs);

}