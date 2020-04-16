/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.utils.crypto.Primes;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by kaylukas on 18/06/2018.
 */
public class PrimeWorker implements Runnable {

    private BlockingQueue<BigInteger> primeChannel;
    private int bitLength;
    private Semaphore primesNeeded;
    PrimeWorker(Semaphore primesNeeded, BlockingQueue<BigInteger> primeChannel, int bitLength) {
        this.primesNeeded = primesNeeded;
        this.primeChannel = primeChannel;
        this.bitLength = bitLength;
    }

    @Override
    public void run() {
        BigInteger prime;
        do {
            prime = BigInteger.probablePrime(bitLength, new SecureRandom());
        } while (primesNeeded.tryAcquire() && primeChannel.offer(prime));
    }
}
