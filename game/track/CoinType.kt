package cube.run.game.track

/** Coloured collectible families. Higher tiers are rarer in the route cycle and pay more. */
enum class CoinType(val reward: Int, val hue: Float) {
    RED(1, 5f),
    BLUE(2, 212f),
    GREEN(3, 126f),
    PINK(4, 326f),
    MIXED(5, 278f);

    companion object {
        /** A small flat payout nudge; it does not change collectible frequency or box rewards. */
        const val BASE_EARNINGS_MULTIPLIER = 1.10f
    }
}
