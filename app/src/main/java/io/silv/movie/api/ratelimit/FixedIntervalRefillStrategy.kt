/*
 * Copyright 2012-2014 Brandon Beck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.silv.movie.api.ratelimit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * A token bucket refill strategy that will provide N tokens for a token bucket to consume every T units of time.
 * The tokens are refilled in bursts rather than at a fixed rate.  This refill strategy will never allow more than
 * N tokens to be consumed during a window of time T.
 */
internal class FixedIntervalRefillStrategy(
    private val ticker: Ticker,
    private val numTokensPerPeriod: Long,
    private val periodNanos: Long,
) : TokenBucket.RefillStrategy {


    private val mutex = Mutex()

    private var lastRefillTime: Long = -periodNanos
    private var nextRefillTime: Long = -periodNanos


    override suspend fun refill(): Long {
        return mutex.withLock {
            val now =  ticker.read()
            if (now < nextRefillTime) {
                return 0
            }
            // We now know that we need to refill the bucket with some tokens, the question is how many.  We need to count how
            // many periods worth of tokens we've missed.
            val numPeriods = ((now - lastRefillTime) / periodNanos).coerceAtLeast(0)

            // Move the last refill time forward by this many periods.
            lastRefillTime += numPeriods * periodNanos

            // ...and we'll refill again one period after the last time we refilled.
            nextRefillTime = lastRefillTime + periodNanos

            numPeriods * numTokensPerPeriod
        }
    }

    override fun getNanosUntilNextRefill(): Long {
        val now = ticker.read()
        return (nextRefillTime - now).coerceAtLeast(0)
    }
}