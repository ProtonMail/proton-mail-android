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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by kaylukas on 18/06/2018.
 */

public class PrimeGenerator {

    private int workerThreads;

    public PrimeGenerator(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public PrimeGenerator() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public BigInteger[] generatePrimes(int bitLength, int n) {
        BlockingQueue<BigInteger> primeChannel = new ArrayBlockingQueue<>(n);
        Semaphore primesNeeded = new Semaphore(n);

        // The workers will race to find primes.
        Thread[] workers = new Thread[workerThreads];
        for (int i = 0; i < workerThreads; i++) {
            workers[i] = new Thread(new PrimeWorker(primesNeeded, primeChannel, bitLength));
            workers[i].start();
        }

        BigInteger[] primes = new BigInteger[n];
        for (int i = 0; i < n; i++) {
            try {
                primes[i] = primeChannel.take();
            } catch (InterruptedException e) {
                return null;
            }
        }

        // They will automatically stop generating primes.
        for (int i = 0; i < workerThreads; i++) {
            workers[i].setPriority(Thread.MIN_PRIORITY);
        }

        return primes;
    }

}
